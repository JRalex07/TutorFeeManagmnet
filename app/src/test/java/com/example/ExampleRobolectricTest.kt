package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.ReminderUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Tuition Fee Manager", appName)
  }

  @Test
  fun `reminder message contains student details and amount`() {
    val message = ReminderUtils.buildReminderMessage(
      studentName = "Rahul Sharma",
      parentContact = "9876543210",
      period = "2026-10",
      amount = "₹1,500",
      dueDate = "2026-10-05",
      tuitionName = "Apex Academy",
      teacherName = "Prof. Sharma",
      upiId = "apex@upi"
    )

    assertTrue(message.contains("Rahul Sharma"))
    assertTrue(message.contains("₹1,500"))
    assertTrue(message.contains("Apex Academy"))
    assertTrue(message.contains("apex@upi"))
  }
}
