package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun sanitizeFileName_preservesSafeNamesAndStripsTraversal() {
    assertEquals("annotated.pdf", ClinNoteBridge.sanitizeFileName("../dir/annotated.pdf"))
    assertEquals("محاضرات_الباطنة.pdf", ClinNoteBridge.sanitizeFileName("محاضرات:الباطنة.pdf"))
    assertEquals("clinnote_export.bin", ClinNoteBridge.sanitizeFileName("   "))
  }

  @Test
  fun inferMimeType_mapsClinNoteExportFormats() {
    assertEquals("application/pdf", ClinNoteBridge.inferMimeType("notes-annotated.pdf"))
    assertEquals("application/zip", ClinNoteBridge.inferMimeType("clinnote-backup-2026-11-23.zip"))
    assertEquals("application/json", ClinNoteBridge.inferMimeType("cards.json"))
    assertEquals("text/tab-separated-values", ClinNoteBridge.inferMimeType("cards.tsv"))
    assertEquals("text/csv", ClinNoteBridge.inferMimeType("export.csv"))
  }
}
