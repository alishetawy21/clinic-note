package com.example

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ClinNoteBridge(
  private val context: Context,
  private val onExportReady: (ExportRequest) -> Unit,
) {
  private val mainHandler = Handler(Looper.getMainLooper())
  private val exportDir: File = File(context.cacheDir, "ClinNote").apply { mkdirs() }
  private val activeSessions = ConcurrentHashMap<String, WriteSession>()
  private val completedFiles = ConcurrentHashMap<String, File>()

  private data class WriteSession(val file: File, val stream: FileOutputStream)

  @JavascriptInterface
  fun beginWrite(rawFileName: String): String {
    return try {
      exportDir.mkdirs()
      val safeName = sanitizeFileName(rawFileName)
      val outFile = File(exportDir, safeName)
      val sessionId = UUID.randomUUID().toString()
      activeSessions[sessionId] = WriteSession(outFile, FileOutputStream(outFile, false))
      sessionId
    } catch (_: Exception) {
      ""
    }
  }

  @JavascriptInterface
  fun appendWriteChunk(sessionId: String, base64Chunk: String): Boolean {
    val session = activeSessions[sessionId] ?: return false
    return try {
      if (base64Chunk.isNotEmpty()) {
        val bytes = Base64.decode(base64Chunk, Base64.DEFAULT)
        session.stream.write(bytes)
      }
      true
    } catch (_: Exception) {
      cleanupSession(sessionId)
      false
    }
  }

  @JavascriptInterface
  fun finishWrite(sessionId: String): String {
    val session = activeSessions.remove(sessionId) ?: return ""
    return try {
      session.stream.flush()
      session.stream.close()
      val uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", session.file)
      val uriString = uri.toString()
      completedFiles[uriString] = session.file
      uriString
    } catch (_: Exception) {
      runCatching { session.stream.close() }
      ""
    }
  }

  @JavascriptInterface
  fun presentExportSheet(uriString: String, title: String, @Suppress("UNUSED_PARAMETER") dialogTitle: String) {
    val cachedFile = completedFiles[uriString] ?: return
    val uri = Uri.parse(uriString)
    val safeName = sanitizeFileName(title.ifBlank { cachedFile.name })
    val request =
      ExportRequest(
        fileName = safeName,
        mimeType = inferMimeType(safeName),
        cachedFile = cachedFile,
        contentUri = uri,
        sizeBytes = cachedFile.length(),
      )
    mainHandler.post { onExportReady(request) }
  }

  private fun cleanupSession(sessionId: String) {
    val session = activeSessions.remove(sessionId) ?: return
    runCatching { session.stream.close() }
    runCatching { session.file.delete() }
  }

  companion object {
    const val JS_INTERFACE_NAME = "ClinNoteBridge"

    val CAPACITOR_SHIM_JS =
      """
      (function() {
        if (window.Capacitor && window.Capacitor.__clinNoteNative) return;
        window.Capacitor = {
          __clinNoteNative: true,
          isNativePlatform: function() { return true; },
          Plugins: {
            Filesystem: {
              writeFile: function(opts) {
                return new Promise(function(resolve, reject) {
                  try {
                    var rawPath = String((opts && opts.path) || 'export.bin');
                    var name = rawPath.split('/').pop() || 'export.bin';
                    var b64 = String((opts && opts.data) || '');
                    var sid = ClinNoteBridge.beginWrite(name);
                    if (!sid) {
                      reject(new Error('Could not open export file'));
                      return;
                    }
                    var chunk = 524288;
                    for (var i = 0; i < b64.length; i += chunk) {
                      if (!ClinNoteBridge.appendWriteChunk(sid, b64.slice(i, i + chunk))) {
                        reject(new Error('Failed writing export chunk'));
                        return;
                      }
                    }
                    var uri = ClinNoteBridge.finishWrite(sid);
                    if (!uri) {
                      reject(new Error('Failed finalizing export'));
                      return;
                    }
                    resolve({ uri: uri });
                  } catch (e) {
                    reject(e);
                  }
                });
              }
            },
            Share: {
              share: function(opts) {
                return new Promise(function(resolve, reject) {
                  try {
                    var title = String((opts && opts.title) || 'ClinNote export');
                    var uri = String((opts && opts.url) || '');
                    var dialogTitle = String((opts && opts.dialogTitle) || title);
                    ClinNoteBridge.presentExportSheet(uri, title, dialogTitle);
                    resolve({});
                  } catch (e) {
                    reject(e);
                  }
                });
              }
            }
          }
        };
      })();
      """.trimIndent()

    fun sanitizeFileName(raw: String): String {
      val base = File(raw).name.trim()
      val cleaned = base.replace(Regex("[^A-Za-z0-9._\\-\\u0600-\\u06FF ]"), "_")
      return cleaned.ifBlank { "clinnote_export.bin" }.take(120)
    }

    fun inferMimeType(fileName: String): String {
      return when (fileName.substringAfterLast('.', "").lowercase()) {
        "pdf" -> "application/pdf"
        "zip" -> "application/zip"
        "json" -> "application/json"
        "tsv" -> "text/tab-separated-values"
        "csv" -> "text/csv"
        "txt" -> "text/plain"
        else -> "application/octet-stream"
      }
    }

    fun launchShareIntent(context: Context, exportRequest: ExportRequest) {
      val shareIntent =
        Intent(Intent.ACTION_SEND).apply {
          type = exportRequest.mimeType
          putExtra(Intent.EXTRA_STREAM, exportRequest.contentUri)
          putExtra(Intent.EXTRA_SUBJECT, exportRequest.fileName)
          clipData =
            ClipData.newUri(
              context.contentResolver,
              exportRequest.fileName,
              exportRequest.contentUri,
            )
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      val chooserTitle =
        context.getString(R.string.share_chooser_title, exportRequest.fileName)
      context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }
  }
}
