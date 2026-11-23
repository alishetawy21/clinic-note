package com.example

import android.net.Uri
import android.webkit.JsPromptResult
import android.webkit.JsResult
import androidx.lifecycle.ViewModel
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed interface JsDialogRequest {
  data class Alert(val message: String, val result: JsResult) : JsDialogRequest

  data class Confirm(val message: String, val result: JsResult) : JsDialogRequest

  data class Prompt(
    val message: String,
    val defaultValue: String,
    val result: JsPromptResult,
  ) : JsDialogRequest
}

data class ExportRequest(
  val fileName: String,
  val mimeType: String,
  val cachedFile: File,
  val contentUri: Uri,
  val sizeBytes: Long,
)

data class ClinNoteUiState(
  val isLoading: Boolean = true,
  val loadError: String? = null,
  val activeJsDialog: JsDialogRequest? = null,
  val pendingExport: ExportRequest? = null,
  val showExitConfirmation: Boolean = false,
)

class ClinNoteViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(ClinNoteUiState())
  val uiState: StateFlow<ClinNoteUiState> = _uiState.asStateFlow()

  fun setLoading(loading: Boolean) {
    _uiState.update { it.copy(isLoading = loading) }
  }

  fun setLoadError(error: String?) {
    _uiState.update { it.copy(loadError = error, isLoading = false) }
  }

  fun showJsDialog(request: JsDialogRequest) {
    val previous = _uiState.value.activeJsDialog
    cancelJsResult(previous)
    _uiState.update { it.copy(activeJsDialog = request) }
  }

  fun confirmJsDialog(promptInput: String? = null) {
    val dialog = _uiState.value.activeJsDialog ?: return
    _uiState.update { it.copy(activeJsDialog = null) }
    when (dialog) {
      is JsDialogRequest.Alert -> dialog.result.confirm()
      is JsDialogRequest.Confirm -> dialog.result.confirm()
      is JsDialogRequest.Prompt -> dialog.result.confirm(promptInput ?: dialog.defaultValue)
    }
  }

  fun dismissJsDialog() {
    val dialog = _uiState.value.activeJsDialog ?: return
    _uiState.update { it.copy(activeJsDialog = null) }
    cancelJsResult(dialog)
  }

  fun showExportSheet(request: ExportRequest) {
    _uiState.update { it.copy(pendingExport = request) }
  }

  fun dismissExportSheet() {
    _uiState.update { it.copy(pendingExport = null) }
  }

  fun setShowExitConfirmation(show: Boolean) {
    _uiState.update { it.copy(showExitConfirmation = show) }
  }

  private fun cancelJsResult(dialog: JsDialogRequest?) {
    when (dialog) {
      is JsDialogRequest.Alert -> dialog.result.confirm()
      is JsDialogRequest.Confirm -> dialog.result.cancel()
      is JsDialogRequest.Prompt -> dialog.result.cancel()
      null -> Unit
    }
  }

  override fun onCleared() {
    cancelJsResult(_uiState.value.activeJsDialog)
    super.onCleared()
  }
}
