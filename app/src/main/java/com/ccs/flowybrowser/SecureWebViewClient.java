package com.ccs.flowybrowser;

import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.net.Uri;

/**
 * Secure WebViewClient with proper URL handling and progress tracking.
 * Designed for maximum security and minimal resource usage.
 */
public class SecureWebViewClient extends WebViewClient {

    private final ProgressBar progressBar;
    private final EditText urlInput;

    /**
     * Constructor with UI components for progress and URL updates.
     * @param progressBar Progress bar to show loading state
     * @param urlInput URL input field to update with current URL
     */
    public SecureWebViewClient(ProgressBar progressBar, EditText urlInput) {
        this.progressBar = progressBar;
        this.urlInput = urlInput;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        // Let WebView handle normal navigation. Re-loading here causes redirect
        // loops and breaks POST requests. Only intercept unsupported schemes.
        Uri uri = request.getUrl();
        return !isWebUrl(uri);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // Legacy API support for older Android versions
        return !isWebUrl(Uri.parse(url));
    }

    private boolean isWebUrl(Uri uri) {
        if (uri == null || uri.getScheme() == null) return false;
        String scheme = uri.getScheme().toLowerCase(java.util.Locale.US);
        return "http".equals(scheme) || "https".equals(scheme);
    }

    @Override
    public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
        // Update URL input with current page URL
        if (urlInput != null) {
            urlInput.setText(url);
        }

        // Show progress bar when loading starts
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        // Hide progress bar when loading completes
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        super.onPageFinished(view, url);
    }
}
