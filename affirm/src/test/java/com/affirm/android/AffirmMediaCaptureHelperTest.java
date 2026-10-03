package com.affirm.android;

import android.Manifest;
import android.webkit.PermissionRequest;

import com.google.common.truth.Truth;

import org.junit.Test;

public class AffirmMediaCaptureHelperTest {

    @Test
    public void toAndroidPermissionsMapsVideoAndAudio() {
        final String[] permissions = AffirmMediaCaptureHelper.toAndroidPermissions(
                new String[]{
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE
                });

        Truth.assertThat(permissions).asList().containsExactly(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO).inOrder();
    }

    @Test
    public void toAndroidPermissionsIgnoresNonMediaResources() {
        final String[] permissions = AffirmMediaCaptureHelper.toAndroidPermissions(
                new String[]{PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID});

        Truth.assertThat(permissions).isEmpty();
    }

    @Test
    public void toAndroidPermissionsHandlesNull() {
        Truth.assertThat(AffirmMediaCaptureHelper.toAndroidPermissions(null)).isEmpty();
    }

    @Test
    public void toGrantedWebViewResourcesFiltersByOsGrant() {
        final String[] requested = new String[]{
                PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                PermissionRequest.RESOURCE_AUDIO_CAPTURE,
                PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID
        };

        Truth.assertThat(AffirmMediaCaptureHelper.toGrantedWebViewResources(
                requested, true, false)).asList().containsExactly(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE);
        Truth.assertThat(AffirmMediaCaptureHelper.toGrantedWebViewResources(
                requested, false, true)).asList().containsExactly(
                PermissionRequest.RESOURCE_AUDIO_CAPTURE);
        Truth.assertThat(AffirmMediaCaptureHelper.toGrantedWebViewResources(
                requested, false, false)).isEmpty();
    }
}
