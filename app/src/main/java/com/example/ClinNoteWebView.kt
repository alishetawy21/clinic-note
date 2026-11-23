package com.example

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.ByteArrayInputStream

private const val APP_HOST = "appassets.androidplatform.net"
private const val CLINNOTE_APP_URL = "https://$APP_HOST/clinnote.html"

@Composable
fun ClinNoteScreen(
  viewModel: ClinNoteViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val activity = context as? Activity

  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
  var fullscreenView by remember { mutableStateOf<View?>(null) }
  var fullscreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

  val openSingleDocumentLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      val callback = pendingFileCallback
      pendingFileCallback = null
      callback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
    }

  val openMultipleDocumentsLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
      val callback = pendingFileCallback
      pendingFileCallback = null
      callback?.onReceiveValue(if (uris.isNotEmpty()) uris.toTypedArray() else null)
    }

  val saveDocumentLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { destUri ->
      val export = uiState.pendingExport
      if (destUri != null && export != null) {
        val savedOk =
          runCatching {
            context.contentResolver.openOutputStream(destUri)?.use { out ->
              export.cachedFile.inputStream().use { input -> input.copyTo(out) }
            } != null
          }.getOrDefault(false)
        val message =
          if (savedOk) {
            context.getString(R.string.export_saved_toast, export.fileName)
          } else {
            context.getString(R.string.export_save_failed)
          }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        if (savedOk) viewModel.dismissExportSheet()
      }
    }

  BackHandler {
    when {
      fullscreenView != null -> {
        fullscreenCallback?.onCustomViewHidden()
        fullscreenView = null
        fullscreenCallback = null
        setSystemBarsVisible(activity, true)
      }
      webViewRef?.canGoBack() == true -> {
        webViewRef?.goBack()
      }
      else -> {
        viewModel.setShowExitConfirmation(true)
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      pendingFileCallback?.onReceiveValue(null)
      pendingFileCallback = null
      webViewRef?.destroy()
      webViewRef = null
    }
  }

  Surface(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    color = MaterialTheme.colorScheme.background,
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .let { base ->
            if (fullscreenView == null) base.windowInsetsPadding(WindowInsets.safeDrawing) else base
          },
    ) {
      AndroidView(
        modifier = Modifier.fillMaxSize().testTag("clinnote_webview"),
        factory = { ctx ->
          createClinNoteWebView(
            context = ctx,
            viewModel = viewModel,
            onFileChooser = { callback, acceptTypes, allowMultiple ->
              pendingFileCallback?.onReceiveValue(null)
              pendingFileCallback = callback
              val mimeTypes = resolvePickerMimeTypes(acceptTypes)
              if (allowMultiple) {
                openMultipleDocumentsLauncher.launch(mimeTypes)
              } else {
                openSingleDocumentLauncher.launch(mimeTypes)
              }
            },
            onEnterFullscreen = { view, callback ->
              fullscreenView = view
              fullscreenCallback = callback
              setSystemBarsVisible(activity, false)
            },
            onExitFullscreen = {
              fullscreenView = null
              fullscreenCallback = null
              setSystemBarsVisible(activity, true)
            },
          ).also { created ->
            webViewRef = created
            created.loadUrl(CLINNOTE_APP_URL)
          }
        },
      )

      fullscreenView?.let { customView ->
        AndroidView(
          modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
          factory = { ctx ->
            FrameLayout(ctx).apply {
              (customView.parent as? ViewGroup)?.removeView(customView)
              addView(
                customView,
                FrameLayout.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT,
                  ViewGroup.LayoutParams.MATCH_PARENT,
                ),
              )
            }
          },
        )
      }

      AnimatedVisibility(
        visible = uiState.isLoading && uiState.loadError == null,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        ClinNoteLoadingOverlay()
      }

      uiState.loadError?.let { errorMsg ->
        ClinNoteErrorOverlay(
          errorMessage = errorMsg,
          onRetry = {
            viewModel.setLoadError(null)
            viewModel.setLoading(true)
            webViewRef?.loadUrl(CLINNOTE_APP_URL)
          },
        )
      }
    }
  }

  uiState.activeJsDialog?.let { dialog ->
    ClinNoteJsDialog(
      dialog = dialog,
      onConfirm = { input -> viewModel.confirmJsDialog(input) },
      onDismiss = { viewModel.dismissJsDialog() },
    )
  }

  uiState.pendingExport?.let { exportRequest ->
    ClinNoteExportDialog(
      exportRequest = exportRequest,
      onSaveToDevice = { saveDocumentLauncher.launch(exportRequest.fileName) },
      onShare = {
        ClinNoteBridge.launchShareIntent(context, exportRequest)
        viewModel.dismissExportSheet()
      },
      onDismiss = { viewModel.dismissExportSheet() },
    )
  }

  if (uiState.showExitConfirmation) {
    ClinNoteExitDialog(
      onConfirmExit = {
        viewModel.setShowExitConfirmation(false)
        activity?.finish()
      },
      onDismiss = { viewModel.setShowExitConfirmation(false) },
    )
  }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createClinNoteWebView(
  context: Context,
  viewModel: ClinNoteViewModel,
  onFileChooser: (ValueCallback<Array<Uri>>, Array<String>?, Boolean) -> Unit,
  onEnterFullscreen: (View, WebChromeClient.CustomViewCallback?) -> Unit,
  onExitFullscreen: () -> Unit,
): WebView {
  val bridge =
    ClinNoteBridge(
      context = context.applicationContext,
      onExportReady = { exportRequest -> viewModel.showExportSheet(exportRequest) },
    )

  return WebView(context).apply {
    layoutParams =
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
      )
    isFocusable = true
    isFocusableInTouchMode = true

    settings.apply {
      javaScriptEnabled = true
      domStorageEnabled = true
      @Suppress("DEPRECATION")
      databaseEnabled = true
      allowFileAccess = false
      allowContentAccess = true
      setSupportZoom(false)
      builtInZoomControls = false
      displayZoomControls = false
      useWideViewPort = true
      loadWithOverviewMode = true
      mediaPlaybackRequiresUserGesture = false
      mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    }

    addJavascriptInterface(bridge, ClinNoteBridge.JS_INTERFACE_NAME)

    webViewClient =
      object : WebViewClient() {
        override fun shouldInterceptRequest(
          view: WebView?,
          request: WebResourceRequest?,
        ): WebResourceResponse? {
          val url = request?.url ?: return super.shouldInterceptRequest(view, request)
          if (url.scheme == "https" && url.host == APP_HOST) {
            val path = url.path?.trimStart('/') ?: ""
            return when (path) {
              "", "clinnote.html" -> {
                runCatching {
                  WebResourceResponse(
                    "text/html",
                    "UTF-8",
                    200,
                    "OK",
                    mapOf("Cache-Control" to "no-store"),
                    context.assets.open("clinnote.html"),
                  )
                }.getOrNull()
              }
              "favicon.ico" -> {
                WebResourceResponse(
                  "image/x-icon",
                  null,
                  204,
                  "No Content",
                  emptyMap(),
                  ByteArrayInputStream(ByteArray(0)),
                )
              }
              else -> null
            }
          }
          return super.shouldInterceptRequest(view, request)
        }

        override fun shouldOverrideUrlLoading(
          view: WebView?,
          request: WebResourceRequest?,
        ): Boolean {
          val url = request?.url ?: return false
          if (url.scheme == "https" && url.host == APP_HOST) {
            return false
          }
          if (url.scheme == "http" || url.scheme == "https") {
            runCatching {
              val intent = Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              context.startActivity(intent)
            }
            return true
          }
          return false
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
          super.onPageStarted(view, url, favicon)
          view?.evaluateJavascript(ClinNoteBridge.CAPACITOR_SHIM_JS, null)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
          super.onPageFinished(view, url)
          view?.evaluateJavascript(ClinNoteBridge.CAPACITOR_SHIM_JS, null)
          viewModel.setLoading(false)
        }

        override fun onReceivedError(
          view: WebView?,
          request: WebResourceRequest?,
          error: WebResourceError?,
        ) {
          super.onReceivedError(view, request, error)
          if (request?.isForMainFrame == true) {
            viewModel.setLoadError(
              error?.description?.toString()
                ?: context.getString(R.string.error_load_failed),
            )
          }
        }
      }

    webChromeClient =
      object : WebChromeClient() {
        override fun onJsAlert(
          view: WebView?,
          url: String?,
          message: String?,
          result: JsResult?,
        ): Boolean {
          if (result == null) return false
          viewModel.showJsDialog(JsDialogRequest.Alert(message.orEmpty(), result))
          return true
        }

        override fun onJsConfirm(
          view: WebView?,
          url: String?,
          message: String?,
          result: JsResult?,
        ): Boolean {
          if (result == null) return false
          viewModel.showJsDialog(JsDialogRequest.Confirm(message.orEmpty(), result))
          return true
        }

        override fun onJsPrompt(
          view: WebView?,
          url: String?,
          message: String?,
          defaultValue: String?,
          result: JsPromptResult?,
        ): Boolean {
          if (result == null) return false
          viewModel.showJsDialog(
            JsDialogRequest.Prompt(
              message = message.orEmpty(),
              defaultValue = defaultValue.orEmpty(),
              result = result,
            ),
          )
          return true
        }

        override fun onShowFileChooser(
          webView: WebView?,
          filePathCallback: ValueCallback<Array<Uri>>?,
          fileChooserParams: FileChooserParams?,
        ): Boolean {
          if (filePathCallback == null) return false
          val allowMultiple = fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
          onFileChooser(filePathCallback, fileChooserParams?.acceptTypes, allowMultiple)
          return true
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
          if (view == null) {
            callback?.onCustomViewHidden()
            return
          }
          onEnterFullscreen(view, callback)
        }

        override fun onHideCustomView() {
          onExitFullscreen()
        }
      }
  }
}

private fun resolvePickerMimeTypes(acceptTypes: Array<String>?): Array<String> {
  val tokens =
    acceptTypes
      .orEmpty()
      .flatMap { it.split(',') }
      .map { it.trim().lowercase() }
      .filter { it.isNotEmpty() }

  if (tokens.isEmpty()) return arrayOf("*/*")
  if (tokens.all { it == "application/pdf" || it == ".pdf" }) {
    return arrayOf("application/pdf")
  }
  if (tokens.all { it.startsWith("image/") || it in setOf(".png", ".jpg", ".jpeg", ".webp") }) {
    return arrayOf("image/*")
  }
  return arrayOf("*/*")
}

private fun setSystemBarsVisible(activity: Activity?, visible: Boolean) {
  val window = activity?.window ?: return
  val controller = WindowCompat.getInsetsController(window, window.decorView)
  controller.systemBarsBehavior =
    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
  if (visible) {
    controller.show(WindowInsetsCompat.Type.systemBars())
  } else {
    controller.hide(WindowInsetsCompat.Type.systemBars())
  }
}

@Composable
private fun ClinNoteLoadingOverlay() {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Description,
        contentDescription = stringResource(R.string.app_name),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(56.dp),
      )
      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
      )
      CircularProgressIndicator(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp),
      )
      Text(
        text = stringResource(R.string.loading_workspace),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.tertiary,
      )
    }
  }
}

@Composable
private fun ClinNoteErrorOverlay(
  errorMessage: String,
  onRetry: () -> Unit,
) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = stringResource(R.string.error_load_failed),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
      )
      Text(
        text = errorMessage,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.tertiary,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Button(
        onClick = onRetry,
        modifier = Modifier.testTag("retry_load_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Refresh,
          contentDescription = stringResource(R.string.retry),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = stringResource(R.string.retry))
      }
    }
  }
}
