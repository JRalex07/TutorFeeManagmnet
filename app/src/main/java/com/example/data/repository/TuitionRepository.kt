package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.StudentDao
import com.example.data.model.*
import com.example.util.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class TuitionRepository(private val context: Context) {

  private val TAG = "TuitionRepository"

  // In-memory reactive state flows reflecting the single source of truth
  private val _students = MutableStateFlow<List<Student>>(emptyList())
  val students: StateFlow<List<Student>> = _students.asStateFlow()

  val studentDao: StudentDao = AppDatabase.getDatabase(context).studentDao()

  private val _feeRecords = MutableStateFlow<List<FeeRecord>>(emptyList())
  val feeRecords: StateFlow<List<FeeRecord>> = _feeRecords.asStateFlow()

  private val _payments = MutableStateFlow<List<Payment>>(emptyList())
  val payments: StateFlow<List<Payment>> = _payments.asStateFlow()

  private val _refunds = MutableStateFlow<List<Refund>>(emptyList())
  val refunds: StateFlow<List<Refund>> = _refunds.asStateFlow()

  private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
  val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

  private val _tuitionProfile = MutableStateFlow(TuitionProfile())
  val tuitionProfile: StateFlow<TuitionProfile> = _tuitionProfile.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  private val _isFirestoreConnected = MutableStateFlow(false)
  val isFirestoreConnected: StateFlow<Boolean> = _isFirestoreConnected.asStateFlow()

  private var firestore: FirebaseFirestore? = null
  private val auth = FirebaseAuth.getInstance()
  private var activeUserId: String? = null

  // Snapshot listeners
  private var studentsListener: ListenerRegistration? = null
  private var feeRecordsListener: ListenerRegistration? = null
  private var paymentsListener: ListenerRegistration? = null
  private var profileListener: ListenerRegistration? = null

  // Duplicate submission guard (Idempotency cache)
  private val recentSubmissions = mutableMapOf<String, Long>()

  init {
    initializeFirestore()
    setupAuthObserver()
  }

  private fun initializeFirestore() {
    try {
      val dbId = context.getString(R.string.firestore_database_id)
      val db = FirebaseFirestore.getInstance(dbId)
      val settings = FirebaseFirestoreSettings.Builder()
        .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
        .build()
      db.firestoreSettings = settings
      firestore = db
      _isFirestoreConnected.value = true
      Log.d(TAG, "Connected to provisioned Firestore database: $dbId")
    } catch (e: Exception) {
      Log.e(TAG, "Error initializing Firestore client", e)
      _isFirestoreConnected.value = false
    }
  }

  private fun setupAuthObserver() {
    auth.addAuthStateListener { firebaseAuth ->
      val user = firebaseAuth.currentUser
      if (user != null) {
        val uid = user.uid
        if (activeUserId != uid) {
          activeUserId = uid
          attachFirestoreListeners(uid)
        }
      } else {
        activeUserId = null
        detachFirestoreListeners()
        _students.value = emptyList()
        _feeRecords.value = emptyList()
        _payments.value = emptyList()
      }
    }
  }

  private fun attachFirestoreListeners(userId: String) {
    detachFirestoreListeners()
    val db = firestore ?: return

    _isLoading.value = true

    // 1. Listen to /users/{userId}/students
    studentsListener = db.collection("users").document(userId).collection("students")
      .addSnapshotListener { snapshot, error ->
        _isLoading.value = false
        if (error != null) {
          Log.e(TAG, "Error listening to students", error)
          return@addSnapshotListener
        }
        if (snapshot != null) {
          val list = snapshot.toObjects(Student::class.java)
          _students.value = list
          // Synchronize local Room database
          CoroutineScope(Dispatchers.IO).launch {
            try {
              studentDao.insertStudents(list)
            } catch (e: Exception) {
              Log.e(TAG, "Room sync error", e)
            }
          }
        }
      }

    // 2. Listen to /users/{userId}/fee_records
    feeRecordsListener = db.collection("users").document(userId).collection("fee_records")
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.e(TAG, "Error listening to fee records", error)
          return@addSnapshotListener
        }
        if (snapshot != null) {
          val list = snapshot.toObjects(FeeRecord::class.java)
          _feeRecords.value = list
        }
      }

    // 3. Listen to /users/{userId}/payments
    paymentsListener = db.collection("users").document(userId).collection("payments")
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.e(TAG, "Error listening to payments", error)
          return@addSnapshotListener
        }
        if (snapshot != null) {
          val list = snapshot.toObjects(Payment::class.java).sortedByDescending { it.createdAt }
          _payments.value = list
        }
      }

    // 4. Listen to /users/{userId} profile
    profileListener = db.collection("users").document(userId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.e(TAG, "Error listening to profile", error)
          return@addSnapshotListener
        }
        if (snapshot != null && snapshot.exists()) {
          val profile = snapshot.toObject(TuitionProfile::class.java)
          if (profile != null) {
            _tuitionProfile.value = profile
          }
        }
      }
  }

  private fun detachFirestoreListeners() {
    studentsListener?.remove()
    studentsListener = null
    feeRecordsListener?.remove()
    feeRecordsListener = null
    paymentsListener?.remove()
    paymentsListener = null
    profileListener?.remove()
    profileListener = null
  }

  fun clearError() {
    _errorMessage.value = null
  }

  private fun requireUserId(): String {
    return auth.currentUser?.uid ?: error("User must be signed in to perform database operations")
  }

  // Idempotency check to protect against accidental rapid clicks
  private fun checkAndRecordDuplicate(key: String, windowMillis: Long = 4000): Boolean {
    val now = System.currentTimeMillis()
    val lastTime = recentSubmissions[key]
    if (lastTime != null && (now - lastTime) < windowMillis) {
      return true
    }
    recentSubmissions[key] = now
    return false
  }

  suspend fun addStudent(student: Student): Result<Student> = withContext(Dispatchers.IO) {
    try {
      val uid = requireUserId()
      val generatedId = if (student.id.isBlank()) UUID.randomUUID().toString() else student.id
      val count = _students.value.size + 1
      val displayId = if (student.studentId.isBlank()) "STU-%s-%03d".format(DateUtils.currentYear(), count) else student.studentId
      
      val newStudent = student.copy(
        id = generatedId,
        studentId = displayId,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
      )

      // Persist to Room local database
      studentDao.insertStudent(newStudent)

      // Write to live Firestore collection
      val db = firestore
      if (db != null) {
        val studentData = mapOf(
          "id" to newStudent.id,
          "userId" to uid,
          "name" to newStudent.name,
          "parentContact" to newStudent.parentContact,
          "monthlyFeeAmount" to newStudent.monthlyFeeAmount,
          "studentId" to newStudent.studentId,
          "fatherName" to newStudent.fatherName,
          "motherName" to newStudent.motherName,
          "dob" to newStudent.dob,
          "gender" to newStudent.gender,
          "phoneNumber" to newStudent.phoneNumber,
          "address" to newStudent.address,
          "joiningDate" to newStudent.joiningDate,
          "status" to newStudent.status.name,
          "notes" to newStudent.notes,
          "feeCycle" to newStudent.feeCycle.name,
          "discount" to newStudent.discount,
          "discountType" to newStudent.discountType.name,
          "feeStartDate" to newStudent.feeStartDate,
          "preferredPaymentDay" to newStudent.preferredPaymentDay,
          "advanceBalance" to newStudent.advanceBalance,
          "studentClass" to newStudent.studentClass,
          "section" to newStudent.section,
          "schoolName" to newStudent.schoolName,
          "subjects" to newStudent.subjects,
          "batch" to newStudent.batch,
          "tuitionTiming" to newStudent.tuitionTiming,
          "currentMonthStatus" to newStudent.currentMonthStatus.name,
          "active" to (newStudent.status == StudentStatus.ACTIVE),
          "createdAt" to System.currentTimeMillis(),
          "updatedAt" to System.currentTimeMillis()
        )

        db.collection("users").document(uid).collection("students").document(newStudent.id).set(studentData)
      }

      val updatedList = _students.value.filterNot { it.id == newStudent.id } + newStudent
      _students.value = updatedList

      // Auto-generate fee records for current period and prior months
      generateInitialFeeRecordsForStudent(uid, newStudent)

      recordAuditLog("STUDENT_ADDED", "Added student ${newStudent.fullName} (${newStudent.studentId})", newStudent.id)

      Result.success(newStudent)
    } catch (e: Exception) {
      Log.e(TAG, "Error adding student", e)
      Result.failure(e)
    }
  }

  suspend fun updateStudent(student: Student): Result<Student> = withContext(Dispatchers.IO) {
    try {
      val uid = requireUserId()
      val current = _students.value.find { it.id == student.id }
        ?: return@withContext Result.failure(Exception("Student not found"))

      val updatedStudent = student.copy(updatedAt = System.currentTimeMillis())
      
      // Update in Room local database
      studentDao.updateStudent(updatedStudent)
      _students.value = _students.value.map { if (it.id == student.id) updatedStudent else it }

      // Update in live Firestore
      val db = firestore
      if (db != null) {
        val studentData = mapOf(
          "id" to updatedStudent.id,
          "userId" to uid,
          "name" to updatedStudent.name,
          "parentContact" to updatedStudent.parentContact,
          "monthlyFeeAmount" to updatedStudent.monthlyFeeAmount,
          "studentId" to updatedStudent.studentId,
          "fatherName" to updatedStudent.fatherName,
          "motherName" to updatedStudent.motherName,
          "dob" to updatedStudent.dob,
          "gender" to updatedStudent.gender,
          "phoneNumber" to updatedStudent.phoneNumber,
          "address" to updatedStudent.address,
          "joiningDate" to updatedStudent.joiningDate,
          "status" to updatedStudent.status.name,
          "notes" to updatedStudent.notes,
          "feeCycle" to updatedStudent.feeCycle.name,
          "discount" to updatedStudent.discount,
          "discountType" to updatedStudent.discountType.name,
          "feeStartDate" to updatedStudent.feeStartDate,
          "preferredPaymentDay" to updatedStudent.preferredPaymentDay,
          "advanceBalance" to updatedStudent.advanceBalance,
          "studentClass" to updatedStudent.studentClass,
          "section" to updatedStudent.section,
          "schoolName" to updatedStudent.schoolName,
          "subjects" to updatedStudent.subjects,
          "batch" to updatedStudent.batch,
          "tuitionTiming" to updatedStudent.tuitionTiming,
          "currentMonthStatus" to updatedStudent.currentMonthStatus.name,
          "active" to (updatedStudent.status == StudentStatus.ACTIVE),
          "updatedAt" to System.currentTimeMillis()
        )
        db.collection("users").document(uid).collection("students").document(student.id).set(studentData)
      }

      recordAuditLog("STUDENT_UPDATED", "Updated profile for ${student.fullName}", student.id)
      Result.success(updatedStudent)
    } catch (e: Exception) {
      Log.e(TAG, "Error updating student", e)
      Result.failure(e)
    }
  }

  suspend fun markStudentLeaving(
    studentId: String,
    leavingDate: String,
    reason: String,
    settlementStatus: SettlementStatus
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val uid = requireUserId()
      val student = _students.value.find { it.id == studentId }
        ?: return@withContext Result.failure(Exception("Student not found"))

      val updated = student.copy(
        status = StudentStatus.LEFT,
        leavingDate = leavingDate,
        leavingReason = reason,
        settlementStatus = settlementStatus,
        updatedAt = System.currentTimeMillis()
      )

      studentDao.updateStudent(updated)
      _students.value = _students.value.map { if (it.id == studentId) updated else it }
      
      firestore?.collection("users")?.document(uid)?.collection("students")?.document(studentId)
        ?.update(
          mapOf(
            "status" to StudentStatus.LEFT.name,
            "active" to false,
            "leavingDate" to leavingDate,
            "leavingReason" to reason,
            "updatedAt" to System.currentTimeMillis()
          )
        )

      recordAuditLog("STUDENT_LEFT", "Student ${student.fullName} marked as left. Settlement: ${settlementStatus.label}", studentId)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteStudent(studentId: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val uid = requireUserId()
      val student = _students.value.find { it.id == studentId }
      studentDao.deleteStudentById(studentId)
      _students.value = _students.value.filterNot { it.id == studentId }

      firestore?.collection("users")?.document(uid)?.collection("students")?.document(studentId)?.delete()
      recordAuditLog("STUDENT_DELETED", "Deleted student ${student?.fullName ?: studentId}", studentId)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun generateInitialFeeRecordsForStudent(userId: String, student: Student) {
    val currentPeriod = DateUtils.currentPeriod()
    val periods = listOf(DateUtils.previousPeriod(currentPeriod), currentPeriod, DateUtils.nextPeriod(currentPeriod))
    val newRecords = mutableListOf<FeeRecord>()

    periods.forEach { period ->
      val baseFee = student.monthlyFee
      val discountAmount = when (student.discountType) {
        DiscountType.FIXED -> student.discount.coerceAtMost(baseFee)
        DiscountType.PERCENTAGE -> (baseFee * (student.discount / 100.0)).coerceAtMost(baseFee)
        DiscountType.NONE -> 0.0
      }
      val netFee = (baseFee - discountAmount).coerceAtLeast(0.0)
      val dueDate = DateUtils.generateDueDate(period, student.preferredPaymentDay)
      val isPast = DateUtils.isOverdue(dueDate)

      val status = if (period < currentPeriod && isPast) {
        FeeStatus.OVERDUE
      } else if (period == currentPeriod) {
        if (isPast) FeeStatus.OVERDUE else FeeStatus.DUE
      } else {
        FeeStatus.NOT_DUE
      }

      val record = FeeRecord(
        id = "FEE_${student.id}_$period",
        studentId = student.id,
        studentName = student.fullName,
        feePeriod = period,
        baseFee = baseFee,
        discount = discountAmount,
        netFee = netFee,
        paidAmount = 0.0,
        remainingAmount = netFee,
        dueDate = dueDate,
        status = status
      )
      newRecords.add(record)

      val recordMap = mapOf(
        "id" to record.id,
        "userId" to userId,
        "studentId" to record.studentId,
        "studentName" to record.studentName,
        "feePeriod" to record.feePeriod,
        "baseFee" to record.baseFee,
        "discount" to record.discount,
        "netFee" to record.netFee,
        "paidAmount" to record.paidAmount,
        "remainingAmount" to record.remainingAmount,
        "totalAmount" to record.netFee,
        "dueDate" to record.dueDate,
        "status" to record.status.name,
        "createdAt" to System.currentTimeMillis(),
        "updatedAt" to System.currentTimeMillis()
      )
      firestore?.collection("users")?.document(userId)?.collection("fee_records")?.document(record.id)?.set(recordMap)
    }

    _feeRecords.value = _feeRecords.value + newRecords
  }

  suspend fun collectFee(
    studentId: String,
    amount: Double,
    paymentDate: String,
    paymentMethod: PaymentMethod,
    transactionReference: String,
    targetPeriod: String?,
    notes: String,
    applyWaiverOrDiscount: Double = 0.0,
    treatExcessAsAdvance: Boolean = true
  ): Result<Payment> = withContext(Dispatchers.IO) {
    if (amount <= 0.0) {
      return@withContext Result.failure(Exception("Payment amount must be greater than zero."))
    }
    val idempotencyKey = "PAY_${studentId}_${amount}_${targetPeriod ?: "AUTO"}_$paymentDate"
    if (checkAndRecordDuplicate(idempotencyKey)) {
      return@withContext Result.failure(Exception("Duplicate payment request blocked. Please check payment history."))
    }

    val uid = requireUserId()
    val student = _students.value.find { it.id == studentId }
      ?: return@withContext Result.failure(Exception("Student not found"))

    try {
      var remainingPaymentToAllocate = amount
      val allocatedPeriods = mutableListOf<String>()
      val allocatedAmounts = mutableMapOf<String, Double>()
      val updatedFeeRecords = _feeRecords.value.toMutableList()

      val candidateRecords = if (!targetPeriod.isNullOrBlank()) {
        var rec = updatedFeeRecords.find { it.studentId == studentId && it.feePeriod == targetPeriod }
        if (rec == null) {
          rec = FeeRecord(
            id = "FEE_${studentId}_$targetPeriod",
            studentId = studentId,
            studentName = student.fullName,
            feePeriod = targetPeriod,
            baseFee = student.monthlyFee,
            netFee = student.monthlyFee,
            remainingAmount = student.monthlyFee,
            dueDate = DateUtils.generateDueDate(targetPeriod, student.preferredPaymentDay),
            status = FeeStatus.DUE
          )
          updatedFeeRecords.add(rec)
        }
        listOf(rec)
      } else {
        updatedFeeRecords
          .filter { it.studentId == studentId && it.remainingAmount > 0 }
          .sortedBy { it.feePeriod }
      }

      for (rec in candidateRecords) {
        if (remainingPaymentToAllocate <= 0) break

        val needed = rec.remainingAmount
        val alloc = needed.coerceAtMost(remainingPaymentToAllocate)
        remainingPaymentToAllocate -= alloc

        val newPaid = rec.paidAmount + alloc
        val newRemaining = (rec.netFee - newPaid).coerceAtLeast(0.0)
        val newStatus = when {
          newRemaining == 0.0 -> FeeStatus.PAID
          newPaid > 0.0 -> FeeStatus.PARTIALLY_PAID
          DateUtils.isOverdue(rec.dueDate) -> FeeStatus.OVERDUE
          else -> FeeStatus.DUE
        }

        val updatedRec = rec.copy(
          paidAmount = newPaid,
          remainingAmount = newRemaining,
          status = newStatus,
          updatedAt = System.currentTimeMillis()
        )

        val idx = updatedFeeRecords.indexOfFirst { it.id == rec.id }
        if (idx >= 0) {
          updatedFeeRecords[idx] = updatedRec
        } else {
          updatedFeeRecords.add(updatedRec)
        }

        allocatedPeriods.add(rec.feePeriod)
        allocatedAmounts[rec.feePeriod] = alloc

        // Write updated fee record to Firestore
        val recData = mapOf(
          "id" to updatedRec.id,
          "userId" to uid,
          "studentId" to updatedRec.studentId,
          "studentName" to updatedRec.studentName,
          "feePeriod" to updatedRec.feePeriod,
          "baseFee" to updatedRec.baseFee,
          "discount" to updatedRec.discount,
          "netFee" to updatedRec.netFee,
          "totalAmount" to updatedRec.netFee,
          "paidAmount" to updatedRec.paidAmount,
          "remainingAmount" to updatedRec.remainingAmount,
          "dueDate" to updatedRec.dueDate,
          "status" to updatedRec.status.name,
          "updatedAt" to System.currentTimeMillis()
        )
        firestore?.collection("users")?.document(uid)?.collection("fee_records")?.document(updatedRec.id)?.set(recData)
      }

      // Handle excess advance
      var newAdvanceBalance = student.advanceBalance
      if (remainingPaymentToAllocate > 0) {
        if (treatExcessAsAdvance) {
          newAdvanceBalance += remainingPaymentToAllocate
          allocatedPeriods.add("ADVANCE")
          allocatedAmounts["ADVANCE"] = remainingPaymentToAllocate
        } else {
          val lastPeriod = candidateRecords.lastOrNull()?.feePeriod ?: DateUtils.currentPeriod()
          val nextP = DateUtils.nextPeriod(lastPeriod)
          val nextRec = FeeRecord(
            id = "FEE_${studentId}_$nextP",
            studentId = studentId,
            studentName = student.fullName,
            feePeriod = nextP,
            baseFee = student.monthlyFee,
            netFee = student.monthlyFee,
            paidAmount = remainingPaymentToAllocate,
            remainingAmount = (student.monthlyFee - remainingPaymentToAllocate).coerceAtLeast(0.0),
            dueDate = DateUtils.generateDueDate(nextP, student.preferredPaymentDay),
            status = if (remainingPaymentToAllocate >= student.monthlyFee) FeeStatus.PAID else FeeStatus.PARTIALLY_PAID
          )
          updatedFeeRecords.add(nextRec)
          allocatedPeriods.add(nextP)
          allocatedAmounts[nextP] = remainingPaymentToAllocate

          val nextMap = mapOf(
            "id" to nextRec.id,
            "userId" to uid,
            "studentId" to nextRec.studentId,
            "studentName" to nextRec.studentName,
            "feePeriod" to nextRec.feePeriod,
            "baseFee" to nextRec.baseFee,
            "netFee" to nextRec.netFee,
            "totalAmount" to nextRec.netFee,
            "paidAmount" to nextRec.paidAmount,
            "remainingAmount" to nextRec.remainingAmount,
            "dueDate" to nextRec.dueDate,
            "status" to nextRec.status.name,
            "updatedAt" to System.currentTimeMillis()
          )
          firestore?.collection("users")?.document(uid)?.collection("fee_records")?.document(nextRec.id)?.set(nextMap)
        }
      }

      // Create Payment Document
      val receiptNum = "REC-%s-%04d".format(
        DateUtils.currentPeriod().replace("-", ""),
        (_payments.value.size + 1)
      )
      val paymentId = "PAY_${System.currentTimeMillis()}_${(100..999).random()}"
      val payment = Payment(
        id = paymentId,
        receiptNumber = receiptNum,
        studentId = studentId,
        studentName = student.fullName,
        studentClass = student.studentClass,
        amount = amount,
        paymentDate = paymentDate,
        paymentMethod = paymentMethod,
        transactionReference = transactionReference,
        allocatedFeePeriods = allocatedPeriods,
        allocatedAmounts = allocatedAmounts,
        status = PaymentRecordStatus.COMPLETED,
        notes = notes,
        balanceAfterPayment = updatedFeeRecords.filter { it.studentId == studentId }.sumOf { it.remainingAmount },
        createdAt = System.currentTimeMillis()
      )

      val paymentMap = mapOf(
        "id" to payment.id,
        "userId" to uid,
        "studentId" to payment.studentId,
        "studentName" to payment.studentName,
        "studentClass" to payment.studentClass,
        "amount" to payment.amount,
        "receiptNumber" to payment.receiptNumber,
        "paymentDate" to payment.paymentDate,
        "paymentMethod" to payment.paymentMethod.name,
        "transactionReference" to payment.transactionReference,
        "allocatedFeePeriods" to payment.allocatedFeePeriods,
        "notes" to payment.notes,
        "status" to payment.status.name,
        "balanceAfterPayment" to payment.balanceAfterPayment,
        "createdAt" to System.currentTimeMillis()
      )
      firestore?.collection("users")?.document(uid)?.collection("payments")?.document(payment.id)?.set(paymentMap)

      // Update student in Firestore & Room
      val updatedStudent = student.copy(
        advanceBalance = newAdvanceBalance,
        updatedAt = System.currentTimeMillis()
      )
      studentDao.updateStudent(updatedStudent)
      _students.value = _students.value.map { if (it.id == studentId) updatedStudent else it }

      firestore?.collection("users")?.document(uid)?.collection("students")?.document(studentId)?.update(
        mapOf(
          "advanceBalance" to newAdvanceBalance,
          "updatedAt" to System.currentTimeMillis()
        )
      )

      _feeRecords.value = updatedFeeRecords
      _payments.value = listOf(payment) + _payments.value

      recordAuditLog("PAYMENT_COLLECTED", "Collected ₹$amount via ${paymentMethod.label} for ${student.fullName} (Receipt: $receiptNum)", studentId)

      Result.success(payment)
    } catch (e: Exception) {
      Log.e(TAG, "Error collecting fee", e)
      Result.failure(e)
    }
  }

  suspend fun waiveFee(feeRecordId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val uid = requireUserId()
      val rec = _feeRecords.value.find { it.id == feeRecordId }
        ?: return@withContext Result.failure(Exception("Fee record not found"))

      val updated = rec.copy(
        remainingAmount = 0.0,
        status = FeeStatus.WAIVED,
        waiverReason = reason,
        updatedAt = System.currentTimeMillis()
      )

      _feeRecords.value = _feeRecords.value.map { if (it.id == feeRecordId) updated else it }
      firestore?.collection("users")?.document(uid)?.collection("fee_records")?.document(feeRecordId)?.update(
        mapOf(
          "remainingAmount" to 0.0,
          "status" to FeeStatus.WAIVED.name,
          "waiverReason" to reason,
          "updatedAt" to System.currentTimeMillis()
        )
      )

      recordAuditLog("FEE_WAIVED", "Waived fee for ${rec.studentName} for ${rec.feePeriod}. Reason: $reason", rec.studentId)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun cancelPayment(paymentId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val uid = requireUserId()
      val payment = _payments.value.find { it.id == paymentId }
        ?: return@withContext Result.failure(Exception("Payment record not found"))

      val updatedPayment = payment.copy(
        status = PaymentRecordStatus.CANCELLED,
        cancellationReason = reason,
        cancelledAt = System.currentTimeMillis()
      )

      _payments.value = _payments.value.map { if (it.id == paymentId) updatedPayment else it }

      firestore?.collection("users")?.document(uid)?.collection("payments")?.document(paymentId)?.update(
        mapOf(
          "status" to PaymentRecordStatus.CANCELLED.name,
          "cancellationReason" to reason,
          "cancelledAt" to System.currentTimeMillis()
        )
      )

      val updatedFeeRecords = _feeRecords.value.toMutableList()
      payment.allocatedAmounts.forEach { (period, amount) ->
        val rec = updatedFeeRecords.find { it.studentId == payment.studentId && it.feePeriod == period }
        if (rec != null) {
          val newPaid = (rec.paidAmount - amount).coerceAtLeast(0.0)
          val newRem = (rec.netFee - newPaid).coerceAtLeast(0.0)
          val newStatus = when {
            newRem == 0.0 -> FeeStatus.PAID
            newPaid > 0.0 -> FeeStatus.PARTIALLY_PAID
            DateUtils.isOverdue(rec.dueDate) -> FeeStatus.OVERDUE
            else -> FeeStatus.DUE
          }
          val modRec = rec.copy(
            paidAmount = newPaid,
            remainingAmount = newRem,
            status = newStatus,
            updatedAt = System.currentTimeMillis()
          )
          val idx = updatedFeeRecords.indexOfFirst { it.id == rec.id }
          if (idx >= 0) updatedFeeRecords[idx] = modRec

          firestore?.collection("users")?.document(uid)?.collection("fee_records")?.document(rec.id)?.update(
            mapOf(
              "paidAmount" to newPaid,
              "remainingAmount" to newRem,
              "status" to newStatus.name,
              "updatedAt" to System.currentTimeMillis()
            )
          )
        }
      }
      _feeRecords.value = updatedFeeRecords

      recordAuditLog("PAYMENT_CANCELLED", "Cancelled payment ${payment.receiptNumber} for ${payment.studentName}. Reason: $reason", payment.studentId)
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Error cancelling payment", e)
      Result.failure(e)
    }
  }

  suspend fun issueRefund(
    studentId: String,
    amount: Double,
    method: PaymentMethod,
    reason: String
  ): Result<Refund> = withContext(Dispatchers.IO) {
    try {
      val uid = requireUserId()
      val student = _students.value.find { it.id == studentId }
        ?: return@withContext Result.failure(Exception("Student not found"))

      val refund = Refund(
        id = "REF_${System.currentTimeMillis()}",
        studentId = studentId,
        studentName = student.fullName,
        amount = amount,
        refundDate = DateUtils.currentDateString(),
        paymentMethod = method,
        reason = reason,
        createdAt = System.currentTimeMillis()
      )

      _refunds.value = listOf(refund) + _refunds.value

      val newAdvance = (student.advanceBalance - amount).coerceAtLeast(0.0)
      val updatedStudent = student.copy(advanceBalance = newAdvance, updatedAt = System.currentTimeMillis())
      studentDao.updateStudent(updatedStudent)
      _students.value = _students.value.map { if (it.id == studentId) updatedStudent else it }

      firestore?.collection("users")?.document(uid)?.collection("students")?.document(studentId)?.update(
        mapOf("advanceBalance" to newAdvance, "updatedAt" to System.currentTimeMillis())
      )

      recordAuditLog("REFUND_ISSUED", "Issued refund of ₹$amount to ${student.fullName}. Reason: $reason", studentId)
      Result.success(refund)
    } catch (e: Exception) {
      Log.e(TAG, "Error issuing refund", e)
      Result.failure(e)
    }
  }

  suspend fun recordReminderSent(
    studentId: String,
    feeRecordId: String?,
    channel: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val uid = requireUserId()
      val now = System.currentTimeMillis()
      val student = _students.value.find { it.id == studentId }

      if (student != null) {
        val updatedStudent = student.copy(lastReminderSentAt = now, updatedAt = now)
        studentDao.updateStudent(updatedStudent)
        _students.value = _students.value.map { if (it.id == studentId) updatedStudent else it }

        firestore?.collection("users")?.document(uid)?.collection("students")?.document(studentId)?.update(
          mapOf("lastReminderSentAt" to now, "updatedAt" to now)
        )
      }

      if (feeRecordId != null) {
        val rec = _feeRecords.value.find { it.id == feeRecordId }
        if (rec != null) {
          val updatedRec = rec.copy(lastReminderSentAt = now, updatedAt = now)
          _feeRecords.value = _feeRecords.value.map { if (it.id == feeRecordId) updatedRec else it }
          firestore?.collection("users")?.document(uid)?.collection("fee_records")?.document(feeRecordId)?.update(
            mapOf("lastReminderSentAt" to now, "updatedAt" to now)
          )
        }
      }

      recordAuditLog(
        "REMINDER_SENT",
        "Dispatched fee reminder to ${student?.fullName ?: studentId} via $channel",
        studentId
      )
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Error recording reminder", e)
      Result.failure(e)
    }
  }

  suspend fun resetToSampleData(): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val s1 = Student(
        id = "STU_SAMPLE_1",
        studentId = "STU-2026-001",
        name = "Aarav Sharma",
        studentClass = "Class 10 - Mathematics",
        monthlyFeeAmount = 1500.0,
        parentContact = "9876543210",
        preferredPaymentDay = 5,
        status = StudentStatus.ACTIVE
      )
      val s2 = Student(
        id = "STU_SAMPLE_2",
        studentId = "STU-2026-002",
        name = "Diya Patel",
        studentClass = "Class 12 - Physics",
        monthlyFeeAmount = 2000.0,
        parentContact = "9812345678",
        preferredPaymentDay = 10,
        status = StudentStatus.ACTIVE
      )
      addStudent(s1)
      addStudent(s2)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun clearAllData(): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val uid = requireUserId()
      val db = firestore
      if (db != null) {
        val userDoc = db.collection("users").document(uid)
        _students.value.forEach { s ->
          userDoc.collection("students").document(s.id).delete()
        }
        _feeRecords.value.forEach { f ->
          userDoc.collection("fee_records").document(f.id).delete()
        }
        _payments.value.forEach { p ->
          userDoc.collection("payments").document(p.id).delete()
        }
      }
      studentDao.deleteAllStudents()
      _students.value = emptyList()
      _feeRecords.value = emptyList()
      _payments.value = emptyList()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateTuitionProfile(profile: TuitionProfile): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val uid = requireUserId()
      _tuitionProfile.value = profile
      
      val profileMap = mapOf(
        "id" to uid,
        "name" to profile.teacherName,
        "tuitionName" to profile.tuitionName,
        "phone" to profile.phone,
        "address" to profile.address,
        "upiId" to profile.upiId,
        "defaultMonthlyFee" to profile.defaultMonthlyFee,
        "defaultDueDay" to profile.defaultDueDay,
        "currencySymbol" to profile.currencySymbol,
        "receiptFooterNote" to profile.receiptFooterNote,
        "updatedAt" to System.currentTimeMillis()
      )
      firestore?.collection("users")?.document(uid)?.set(profileMap)
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Error updating profile", e)
      Result.failure(e)
    }
  }

  private fun recordAuditLog(action: String, description: String, studentId: String?) {
    val log = AuditLog(
      id = "AUDIT_${System.currentTimeMillis()}_${(100..999).random()}",
      action = action,
      description = description,
      studentId = studentId,
      entityId = studentId ?: "",
      timestamp = System.currentTimeMillis()
    )
    _auditLogs.value = listOf(log) + _auditLogs.value
  }
}
