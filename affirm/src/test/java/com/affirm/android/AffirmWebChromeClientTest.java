package com.affirm.android;

import android.webkit.PermissionRequest;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

@RunWith(MockitoJUnitRunner.class)
public class AffirmWebChromeClientTest {

    @Mock
    AffirmWebChromeClient.Callbacks callbacks;
    @Mock
    WebView webview;

    @InjectMocks
    AffirmWebChromeClient affirmWebChromeClient;

    @Test
    public void onProgressChangedTest() {
        affirmWebChromeClient.onProgressChanged(webview, 80);
        Mockito.verify(callbacks, never()).chromeLoadCompleted();
    }

    @Test
    public void onProgressChangedWithOneHundredTest() {
        affirmWebChromeClient.onProgressChanged(webview, 100);
        Mockito.verify(callbacks).chromeLoadCompleted();
    }

    @Test
    public void onPermissionRequestGrantsWhenHostApproves() {
        final PermissionRequest request = mockMediaRequest(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                PermissionRequest.RESOURCE_AUDIO_CAPTURE);
        final AffirmWebChromeClient client =
                new AffirmWebChromeClient(new ImmediateMediaCallbacks(true, true));

        client.onPermissionRequest(request);

        final ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        Mockito.verify(request).grant(captor.capture());
        Mockito.verify(request, never()).deny();
        org.junit.Assert.assertArrayEquals(new String[]{
                PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                PermissionRequest.RESOURCE_AUDIO_CAPTURE
        }, captor.getValue());
    }

    @Test
    public void onPermissionRequestDeniesWhenHostRejects() {
        final PermissionRequest request = mockMediaRequest(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE);
        final AffirmWebChromeClient client =
                new AffirmWebChromeClient(new ImmediateMediaCallbacks(false, false));

        client.onPermissionRequest(request);

        Mockito.verify(request).deny();
        Mockito.verify(request, never()).grant(any(String[].class));
    }

    @Test
    public void onPermissionRequestGrantsOnlyApprovedResources() {
        final PermissionRequest request = mockMediaRequest(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                PermissionRequest.RESOURCE_AUDIO_CAPTURE);
        final AffirmWebChromeClient client =
                new AffirmWebChromeClient(new ImmediateMediaCallbacks(true, false));

        client.onPermissionRequest(request);

        final ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        Mockito.verify(request).grant(captor.capture());
        org.junit.Assert.assertArrayEquals(new String[]{
                PermissionRequest.RESOURCE_VIDEO_CAPTURE
        }, captor.getValue());
    }

    @Test
    public void onPermissionRequestDeniesNonMediaResources() {
        final PermissionRequest request = mockMediaRequest(
                PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID);

        affirmWebChromeClient.onPermissionRequest(request);

        Mockito.verify(request).deny();
        Mockito.verify(callbacks, never()).requestWebViewMediaPermissions(
                any(String[].class),
                any(AffirmWebChromeClient.MediaPermissionResultCallback.class));
    }

    @Test
    public void onPermissionRequestDeniesWhenAnotherRequestIsPending() {
        final PermissionRequest first = mockMediaRequest(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE);
        final PermissionRequest second = mockMediaRequest(
                PermissionRequest.RESOURCE_AUDIO_CAPTURE);
        final HoldingMediaCallbacks holdingCallbacks = new HoldingMediaCallbacks();
        final AffirmWebChromeClient client = new AffirmWebChromeClient(holdingCallbacks);

        client.onPermissionRequest(first);
        client.onPermissionRequest(second);

        Mockito.verify(second).deny();
        Mockito.verify(first, never()).deny();
        holdingCallbacks.callback.onResult(true, false);
        final ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        Mockito.verify(first).grant(captor.capture());
        org.junit.Assert.assertArrayEquals(new String[]{
                PermissionRequest.RESOURCE_VIDEO_CAPTURE
        }, captor.getValue());
    }

    @Test
    public void onPermissionRequestCanceledDropsPendingGrant() {
        final PermissionRequest request = mockMediaRequest(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE);
        final HoldingMediaCallbacks holdingCallbacks = new HoldingMediaCallbacks();
        final AffirmWebChromeClient client = new AffirmWebChromeClient(holdingCallbacks);

        client.onPermissionRequest(request);
        client.onPermissionRequestCanceled(request);
        holdingCallbacks.callback.onResult(true, true);

        Mockito.verify(request, never()).grant(any(String[].class));
        Mockito.verify(request, never()).deny();
    }

    private static PermissionRequest mockMediaRequest(String... resources) {
        final PermissionRequest request = Mockito.mock(PermissionRequest.class);
        Mockito.when(request.getResources()).thenReturn(resources);
        return request;
    }

    private static final class ImmediateMediaCallbacks
            implements AffirmWebChromeClient.Callbacks {
        private final boolean cameraGranted;
        private final boolean audioGranted;

        ImmediateMediaCallbacks(boolean cameraGranted, boolean audioGranted) {
            this.cameraGranted = cameraGranted;
            this.audioGranted = audioGranted;
        }

        @Override
        public void chromeLoadCompleted() {
        }

        @Override
        public void requestWebViewMediaPermissions(
                @NonNull String[] androidPermissions,
                @NonNull AffirmWebChromeClient.MediaPermissionResultCallback resultCallback) {
            resultCallback.onResult(cameraGranted, audioGranted);
        }
    }

    private static final class HoldingMediaCallbacks
            implements AffirmWebChromeClient.Callbacks {
        AffirmWebChromeClient.MediaPermissionResultCallback callback;

        @Override
        public void chromeLoadCompleted() {
        }

        @Override
        public void requestWebViewMediaPermissions(
                @NonNull String[] androidPermissions,
                @NonNull AffirmWebChromeClient.MediaPermissionResultCallback resultCallback) {
            this.callback = resultCallback;
        }
    }
}