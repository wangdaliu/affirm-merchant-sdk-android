package com.affirm.android;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

abstract class AffirmFragment extends Fragment implements AffirmWebChromeClient.Callbacks {

    protected static final String TAG_PREFIX = "AffirmFragment";

    private static final int REQUEST_MEDIA_PERMISSION = 1;

    AffirmWebView webView;
    private View progressIndicator;
    private AffirmWebChromeClient.MediaPermissionResultCallback pendingMediaCallback;

    abstract void initViews();

    abstract void onAttached();

    protected static void addFragment(FragmentManager fragmentManager, @IdRes int containerViewId,
                                      @NonNull Fragment fragment, @NonNull String tag) {
        fragmentManager
                .beginTransaction()
                .add(containerViewId, fragment, tag)
                .commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();
    }

    protected void removeFragment(@NonNull String tag) {
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager == null) {
            AffirmLog.d("The fragment is getting detached from the Activity");
            return;
        }
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment != null) {
            fragmentManager.beginTransaction().remove(fragment).commitAllowingStateLoss();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.affirm_fragment_webview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        webView = view.findViewById(R.id.webview);
        progressIndicator = view.findViewById(R.id.progressIndicator);

        AffirmUtils.debuggableWebView(getContext());
        initViews();
        onAttached();
    }

    @Override
    public void onDestroyView() {
        pendingMediaCallback = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        webView.destroyWebView();
        webView = null;
        super.onDestroy();
    }

    @Override
    public void chromeLoadCompleted() {
        progressIndicator.setVisibility(View.GONE);
    }

    @Override
    public void requestWebViewMediaPermissions(
            @NonNull String[] androidPermissions,
            @NonNull AffirmWebChromeClient.MediaPermissionResultCallback resultCallback) {
        final Context context = getContext();
        if (context == null
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || hasAllPermissions(context, androidPermissions)) {
            notifyMediaPermission(context, resultCallback);
            return;
        }

        pendingMediaCallback = resultCallback;
        requestPermissions(androidPermissions, REQUEST_MEDIA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != REQUEST_MEDIA_PERMISSION || pendingMediaCallback == null) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        final AffirmWebChromeClient.MediaPermissionResultCallback callback =
                pendingMediaCallback;
        pendingMediaCallback = null;
        notifyMediaPermission(getContext(), callback);
    }

    private static void notifyMediaPermission(
            @Nullable Context context,
            @NonNull AffirmWebChromeClient.MediaPermissionResultCallback resultCallback) {
        if (context == null) {
            resultCallback.onResult(false, false);
            return;
        }
        resultCallback.onResult(
                hasPermission(context, Manifest.permission.CAMERA),
                hasPermission(context, Manifest.permission.RECORD_AUDIO));
    }

    private static boolean hasAllPermissions(@NonNull Context context,
                                             @NonNull String[] permissions) {
        for (String permission : permissions) {
            if (!hasPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasPermission(@NonNull Context context, @NonNull String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }
}