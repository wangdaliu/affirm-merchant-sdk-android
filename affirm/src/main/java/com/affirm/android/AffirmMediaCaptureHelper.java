package com.affirm.android;

import android.Manifest;
import android.webkit.PermissionRequest;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class AffirmMediaCaptureHelper {

    private AffirmMediaCaptureHelper() {
    }

    @NonNull
    static String[] toAndroidPermissions(@Nullable String[] webViewResources) {
        if (webViewResources == null) {
            return new String[0];
        }

        final List<String> permissions = new ArrayList<>();
        for (String resource : webViewResources) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                permissions.add(Manifest.permission.CAMERA);
            } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                permissions.add(Manifest.permission.RECORD_AUDIO);
            }
        }
        return permissions.toArray(new String[0]);
    }

    @NonNull
    static String[] toGrantedWebViewResources(@Nullable String[] webViewResources,
                                            boolean cameraGranted,
                                            boolean audioGranted) {
        if (webViewResources == null) {
            return new String[0];
        }

        final List<String> granted = new ArrayList<>();
        for (String resource : webViewResources) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource) && cameraGranted) {
                granted.add(resource);
            } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)
                    && audioGranted) {
                granted.add(resource);
            }
        }
        return granted.toArray(new String[0]);
    }
}
