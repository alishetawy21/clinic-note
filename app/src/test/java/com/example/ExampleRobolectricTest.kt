package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
  fun `app name matches ClinNote`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ClinNote", appName)
  }

  @Test
  fun `bundled clinnote html asset is readable`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val content = context.assets.open("clinnote.html").bufferedReader().use { it.readText() }
    assertTrue(content.contains("ClinNote"))
  }

  @Test
  fun `bridge sanitizes filenames and infers mime types`() {
    assertEquals("annotated.pdf", ClinNoteBridge.sanitizeFileName("../dir/annotated.pdf"))
    assertEquals("application/pdf", ClinNoteBridge.inferMimeType("notes-annotated.pdf"))
    assertEquals("application/zip", ClinNoteBridge.inferMimeType("clinnote-backup.zip"))
    assertEquals("text/tab-separated-values", ClinNoteBridge.inferMimeType("cards.tsv"))
  }
}
