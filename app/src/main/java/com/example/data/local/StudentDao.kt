package com.example.data.local

import androidx.room.*
import com.example.data.model.Student
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

  @Query("SELECT * FROM students ORDER BY createdAt DESC")
  fun getAllStudents(): Flow<List<Student>>

  @Query("SELECT * FROM students WHERE status = 'ACTIVE' ORDER BY name ASC")
  fun getActiveStudents(): Flow<List<Student>>

  @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
  fun getStudentById(id: String): Flow<Student?>

  @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
  suspend fun getStudentByIdDirect(id: String): Student?

  @Query("""
    SELECT * FROM students 
    WHERE name LIKE '%' || :query || '%' 
       OR studentId LIKE '%' || :query || '%' 
       OR parentContact LIKE '%' || :query || '%'
       OR studentClass LIKE '%' || :query || '%'
       OR batch LIKE '%' || :query || '%'
    ORDER BY name ASC
  """)
  fun searchStudents(query: String): Flow<List<Student>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStudent(student: Student)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStudents(students: List<Student>)

  @Update
  suspend fun updateStudent(student: Student)

  @Delete
  suspend fun deleteStudent(student: Student)

  @Query("DELETE FROM students WHERE id = :id")
  suspend fun deleteStudentById(id: String)

  @Query("DELETE FROM students")
  suspend fun deleteAllStudents()
}
