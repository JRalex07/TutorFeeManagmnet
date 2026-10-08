package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject

class Converters {
  @TypeConverter
  fun fromStudentStatus(value: StudentStatus?): String? = value?.name

  @TypeConverter
  fun toStudentStatus(value: String?): StudentStatus =
    value?.let { runCatching { StudentStatus.valueOf(it) }.getOrNull() } ?: StudentStatus.ACTIVE

  @TypeConverter
  fun fromFeeStatus(value: FeeStatus?): String? = value?.name

  @TypeConverter
  fun toFeeStatus(value: String?): FeeStatus =
    value?.let { runCatching { FeeStatus.valueOf(it) }.getOrNull() } ?: FeeStatus.DUE

  @TypeConverter
  fun fromFeeCycle(value: FeeCycle?): String? = value?.name

  @TypeConverter
  fun toFeeCycle(value: String?): FeeCycle =
    value?.let { runCatching { FeeCycle.valueOf(it) }.getOrNull() } ?: FeeCycle.MONTHLY

  @TypeConverter
  fun fromDiscountType(value: DiscountType?): String? = value?.name

  @TypeConverter
  fun toDiscountType(value: String?): DiscountType =
    value?.let { runCatching { DiscountType.valueOf(it) }.getOrNull() } ?: DiscountType.NONE

  @TypeConverter
  fun fromSettlementStatus(value: SettlementStatus?): String? = value?.name

  @TypeConverter
  fun toSettlementStatus(value: String?): SettlementStatus? =
    value?.let { runCatching { SettlementStatus.valueOf(it) }.getOrNull() }

  @TypeConverter
  fun fromStringList(value: List<String>?): String = value?.joinToString(";;") ?: ""

  @TypeConverter
  fun toStringList(value: String?): List<String> =
    if (value.isNullOrBlank()) emptyList() else value.split(";;").filter { it.isNotBlank() }

  @TypeConverter
  fun fromPaymentList(payments: List<Payment>?): String {
    if (payments.isNullOrEmpty()) return ""
    val jsonArray = JSONArray()
    for (p in payments) {
      val obj = JSONObject().apply {
        put("id", p.id)
        put("receiptNumber", p.receiptNumber)
        put("studentId", p.studentId)
        put("studentName", p.studentName)
        put("studentClass", p.studentClass)
        put("amount", p.amount)
        put("paymentDate", p.paymentDate)
        put("paymentMethod", p.paymentMethod.name)
        put("transactionReference", p.transactionReference)
        put("status", p.status.name)
        put("notes", p.notes)
        put("createdAt", p.createdAt)
      }
      jsonArray.put(obj)
    }
    return jsonArray.toString()
  }

  @TypeConverter
  fun toPaymentList(data: String?): List<Payment> {
    if (data.isNullOrBlank()) return emptyList()
    return try {
      val jsonArray = JSONArray(data)
      val list = mutableListOf<Payment>()
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        list.add(
          Payment(
            id = obj.optString("id", ""),
            receiptNumber = obj.optString("receiptNumber", ""),
            studentId = obj.optString("studentId", ""),
            studentName = obj.optString("studentName", ""),
            studentClass = obj.optString("studentClass", ""),
            amount = obj.optDouble("amount", 0.0),
            paymentDate = obj.optString("paymentDate", ""),
            paymentMethod = runCatching { PaymentMethod.valueOf(obj.optString("paymentMethod", "CASH")) }.getOrDefault(PaymentMethod.CASH),
            transactionReference = obj.optString("transactionReference", ""),
            status = runCatching { PaymentRecordStatus.valueOf(obj.optString("status", "COMPLETED")) }.getOrDefault(PaymentRecordStatus.COMPLETED),
            notes = obj.optString("notes", ""),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
          )
        )
      }
      list
    } catch (_: Exception) {
      emptyList()
    }
  }
}
