package com.ccs.flowybrowser;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;

public class MainActivity extends AppCompatActivity {

    ProgressBar progressBar;
    EditText inputUrl;
    WebView webView;
    ImageButton sendButton, forwardButton, backButton, refreshButton;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private int mOriginalSystemUiVisibility;
    private int mOriginalOrientation;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        inputUrl = (EditText) findViewById(R.id.autoCompleteTextView);
        webView = (WebView) findViewById(R.id.webView);
        sendButton = (ImageButton) findViewById(R.id.sendButton);
        forwardButton = (ImageButton) findViewById(R.id.forwardButton);
        backButton = (ImageButton) findViewById(R.id.backButton);
        refreshButton = (ImageButton) findViewById(R.id.refreshButton);
        webView.setWebViewClient(new myWebClient());

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress == 100)
                    progressBar.setVisibility(View.GONE);
                else
                    progressBar.setVisibility(View.VISIBLE);
            }
        });

        WebSettings webset = webView.getSettings();
        webset.setJavaScriptEnabled(true);
        webset.setJavaScriptCanOpenWindowsAutomatically(true);

        webView.loadUrl("http://www.google.com");

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = inputUrl.getText().toString();

                if (url.contains("http://") || url.contains("https://")) {
                    return;

                } else if (url.contains(" ") || url.contains("")) {
                    url = "https://www.google.com/search?q=" + url;
                } else if (url.contains(".") && !url.contains(" ")) {
                    url = "http://" + url;
                }


                webView.loadUrl(url);
                url = webView.getOriginalUrl();
                inputUrl.setText(webView.getOriginalUrl());
                inputUrl.setText(webView.getUrl());


                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(webView.getWindowToken(), 0);
            }
        });

        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoForward())
                    webView.goForward();
                inputUrl.setCursorVisible(false);
                inputUrl.clearComposingText();
                inputUrl.clearFocus();
                inputUrl.clearAnimation();
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoBack())
                    webView.goBack();
                inputUrl.setCursorVisible(false);
                inputUrl.clearComposingText();
                inputUrl.clearFocus();
                inputUrl.clearAnimation();

            }
        });

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.reload();
                inputUrl.setCursorVisible(false);
                inputUrl.clearComposingText();
                inputUrl.clearFocus();
                inputUrl.clearAnimation();
            }
        });
        inputUrl.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                inputUrl.selectAll();

                return false;
            }
        });
        if (inputUrl.isLongClickable() || inputUrl.isClickable()) {
            inputUrl.selectAll();
        }


    }
}