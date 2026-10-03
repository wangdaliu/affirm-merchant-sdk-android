package com.affirm.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Message;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class AffirmWebChromeClient extends WebChromeClient {

    interface MediaPermissionResultCallback {
        void onResult(boolean cameraGranted, boolean audioGranted);
    }

    interface Callbacks {
        void chromeLoadCompleted();

        default void requestWebViewMediaPermissions(
                @NonNull String[] androidPermissions,
                @NonNull MediaPermissionResultCallback resultCallback) {
            resultCallback.onResult(false, false);
        }
    }

    private final Callbacks callback;

    @Nullable
    private PermissionRequest pendingRequest;

    AffirmWebChromeClient(@NonNull Callbacks callback) {
        this.callback = callback;
    }

    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture,
                                  Message resultMsg) {
        final WebView.HitTestResult result = view.getHitTestResult();
        final String data = result.getExtra();
        if (isUserGesture
                && URLUtil.isNetworkUrl(data)
                && result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
            final Context context = view.getContext();
            final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
            context.startActivity(browserIntent);
            return true;
        }
        return false;
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage cm) {
        if (BuildConfig.DEBUG) {
            AffirmLog.d(cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId());
            return true;
        }
        return false;
    }

    @Override
    public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
        new AlertDialog.Builder(view.getContext()).setTitle(R.string.affirm)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> result.cancel())
                .create()
                .show();
        return true;
    }

    @Override
    public void onProgressChanged(WebView view, int progress) {
        if (progress > 99) {
            callback.chromeLoadCompleted();
        }
    }

    @Override
    public void onPermissionRequest(final PermissionRequest request) {
        final String[] androidPermissions =
                AffirmMediaCaptureHelper.toAndroidPermissions(request.getResources());
        if (androidPermissions.length == 0 || pendingRequest != null) {
            finishRequest(request, new String[0]);
            return;
        }

        pendingRequest = request;
        callback.requestWebViewMediaPermissions(androidPermissions,
                (cameraGranted, audioGranted) ->
                        deliverPermissionResult(request, cameraGranted, audioGranted));
    }

    @Override
    public void onPermissionRequestCanceled(PermissionRequest request) {
        if (pendingRequest == request) {
            pendingRequest = null;
        }
    }

    private void deliverPermissionResult(@NonNull PermissionRequest request,
                                         boolean cameraGranted,
                                         boolean audioGranted) {
        if (pendingRequest != request) {
            return;
        }
        pendingRequest = null;
        finishRequest(request, AffirmMediaCaptureHelper.toGrantedWebViewResources(
                request.getResources(), cameraGranted, audioGranted));
    }

    private static void finishRequest(@NonNull PermissionRequest request,
                                      @NonNull String[] granted) {
        try {
            if (granted.length == 0) {
                request.deny();
            } else {
                request.grant(granted);
            }
        } catch (IllegalStateException e) {
            AffirmLog.w("WebView media permission request is no longer valid", e);
        }
    }
}
