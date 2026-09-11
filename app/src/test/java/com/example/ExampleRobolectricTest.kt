package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
    assertEquals("Apex Striker Career", appName)
  }

  @Test
  fun `verify demo version and database version`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals(2, com.example.data.AppVersion.CURRENT)
    assertEquals("DEMO", com.example.data.AppVersion.DISPLAY)
    val db = com.example.data.AppDatabase.getDatabase(context, 1)
    val player = db.careerDao().getPlayerSync()
    assertEquals(null, player)
  }
}
