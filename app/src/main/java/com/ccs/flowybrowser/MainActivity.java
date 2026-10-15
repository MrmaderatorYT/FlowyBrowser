package com.ccs.flowybrowser;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/** Main browser surface backed by GeckoView and persistent GeckoSession tabs. */
public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_BOOKMARKS = 1;
    public static final int REQUEST_CODE_HISTORY = 2;
    private static final String HOME_URL = "https://search.brave.com/";
    private static GeckoRuntime runtime;

    private EditText urlInput;
    private GeckoView geckoView;
    private ProgressBar progressBar;
    private ImageButton backButton, forwardButton, refreshButton, goButton, menuButton;
    private ImageButton bookmarkButton, homeButton, tabsButton;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BrowserDataManager dataManager;
    private BrowserDataManager.BrowserSettings browserSettings;
    private TabManager tabManager;
    private GeckoSession displayedSession;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dataManager = new BrowserDataManager(this);
        browserSettings = dataManager.getSettings();
        initializeViews();
        setupNavigationDrawer();
        setupClickListeners();
        ensureRuntime();
        tabManager = new TabManager(this, runtime, this::bindSessionDelegates);
        displayTab(tabManager.restoreOrCreate());
    }

    private void ensureRuntime() {
        if (runtime == null) {
            GeckoRuntimeSettings settings = new GeckoRuntimeSettings.Builder().javaScriptEnabled(true).build();
            runtime = GeckoRuntime.create(getApplicationContext(), settings);
        }
    }

    private void initializeViews() {
        urlInput = findViewById(R.id.urlInput);
        geckoView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        backButton = findViewById(R.id.backButton);
        forwardButton = findViewById(R.id.forwardButton);
        refreshButton = findViewById(R.id.refreshButton);
        goButton = findViewById(R.id.goButton);
        menuButton = findViewById(R.id.menuButton);
        bookmarkButton = findViewById(R.id.bookmarkButton);
        homeButton = findViewById(R.id.homeButton);
        tabsButton = findViewById(R.id.tabsButton);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
    }

    private void bindSessionDelegates(BrowserTab tab) {
        tab.session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override public void onPageStart(@NonNull GeckoSession session, @NonNull String url) {
                tab.url = url;
                tab.title = "";
                tab.loading = true;
                if (isCurrent(tab)) refreshChrome(tab);
            }
            @Override public void onPageStop(@NonNull GeckoSession session, boolean success) {
                tab.loading = false;
                tab.progress = 100;
                if (success && !tab.privateMode && tab.url.startsWith("http")) {
                    dataManager.addToHistory(tab.url, tab.displayTitle());
                }
                tabManager.persist();
                if (isCurrent(tab)) refreshChrome(tab);
            }
            @Override public void onProgressChange(@NonNull GeckoSession session, int progress) {
                tab.progress = progress;
                if (isCurrent(tab)) {
                    progressBar.setProgress(progress);
                    progressBar.setVisibility(progress < 100 ? View.VISIBLE : View.GONE);
                }
            }
        });
        tab.session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override public void onTitleChange(@NonNull GeckoSession session, String title) {
                tab.title = title == null ? tab.url : title;
            }
        });
        tab.session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override public void onCanGoBack(@NonNull GeckoSession session, boolean value) {
                tab.canGoBack = value;
                if (isCurrent(tab)) updateNavigationButtons(tab);
            }
            @Override public void onCanGoForward(@NonNull GeckoSession session, boolean value) {
                tab.canGoForward = value;
                if (isCurrent(tab)) updateNavigationButtons(tab);
            }
        });
    }

    private boolean isCurrent(BrowserTab tab) {
        return tabManager != null && tabManager.current() == tab;
    }

    private void displayTab(BrowserTab tab) {
        if (tab == null) return;
        if (displayedSession != null && displayedSession != tab.session) geckoView.releaseSession();
        tabManager.select(tab);
        if (displayedSession != tab.session) {
            geckoView.setSession(tab.session);
            displayedSession = tab.session;
        }
        refreshChrome(tab);
    }

    private void refreshChrome(BrowserTab tab) {
        urlInput.setText(tab.url);
        urlInput.setHint(tab.privateMode ? "Private search or address" : "Search or address");
        progressBar.setProgress(tab.progress);
        progressBar.setVisibility(tab.loading ? View.VISIBLE : View.GONE);
        updateNavigationButtons(tab);
        updateBookmarkButton();
        navigationView.getMenu().findItem(R.id.nav_private)
                .setTitle(tab.privateMode ? "Normal browsing" : "Private browsing");
        tabsButton.setContentDescription("Tabs: " + tabManager.count(tab.privateMode));
    }

    private void updateNavigationButtons(BrowserTab tab) {
        backButton.setEnabled(tab.canGoBack);
        backButton.setAlpha(tab.canGoBack ? 1f : 0.35f);
        forwardButton.setEnabled(tab.canGoForward);
        forwardButton.setAlpha(tab.canGoForward ? 1f : 0.35f);
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            handleNavigationItemSelected(item);
            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void setupClickListeners() {
        goButton.setOnClickListener(v -> navigateFromInput());
        backButton.setOnClickListener(v -> { BrowserTab t = tabManager.current(); if (t.canGoBack) t.session.goBack(); });
        forwardButton.setOnClickListener(v -> { BrowserTab t = tabManager.current(); if (t.canGoForward) t.session.goForward(); });
        refreshButton.setOnClickListener(v -> tabManager.current().session.reload());
        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(navigationView));
        bookmarkButton.setOnClickListener(v -> toggleBookmark());
        homeButton.setOnClickListener(v -> loadUrl(HOME_URL));
        tabsButton.setOnClickListener(v -> showTabsDialog());
        tabsButton.setOnLongClickListener(v -> { createTab(tabManager.current().privateMode); return true; });
        urlInput.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                navigateFromInput();
                return true;
            }
            return false;
        });
    }

    private void showTabsDialog() {
        boolean privateMode = tabManager.current().privateMode;
        List<BrowserTab> tabs = tabManager.tabs(privateMode);
        String[] labels = new String[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) labels[i] = tabs.get(i).displayTitle();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(privateMode ? "Private tabs" : "Tabs")
                .setItems(labels, (d, which) -> displayTab(tabs.get(which)))
                .setPositiveButton("New tab", (d, which) -> createTab(privateMode))
                .setNegativeButton("Close current", (d, which) -> closeCurrentTab())
                .setNeutralButton("Cancel", null)
                .create();
        dialog.show();
    }

    private void createTab(boolean privateMode) {
        displayTab(tabManager.create(privateMode, HOME_URL));
    }

    private void closeCurrentTab() {
        if (displayedSession != null) geckoView.releaseSession();
        displayedSession = null;
        displayTab(tabManager.closeCurrent());
    }

    private void switchMode() {
        boolean targetPrivate = !tabManager.current().privateMode;
        BrowserTab target = tabManager.first(targetPrivate);
        if (target == null) target = tabManager.create(targetPrivate, HOME_URL);
        displayTab(target);
    }

    private void handleNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_private) switchMode();
        else if (id == R.id.nav_bookmarks) startActivityForResult(new Intent(this, BookmarksActivity.class), REQUEST_CODE_BOOKMARKS);
        else if (id == R.id.nav_history) startActivityForResult(new Intent(this, HistoryActivity.class), REQUEST_CODE_HISTORY);
        else if (id == R.id.nav_downloads) startActivity(new Intent(this, DownloadsActivity.class));
        else if (id == R.id.nav_settings) startActivity(new Intent(this, SettingsActivity.class));
    }

    private void navigateFromInput() {
        String input = urlInput.getText().toString().trim();
        if (input.isEmpty()) return;
        loadUrl(processInput(input));
        InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (keyboard != null && getCurrentFocus() != null) keyboard.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    private String processInput(String input) {
        if (input.contains(" ") || !input.contains(".")) {
            try { return searchUrl() + URLEncoder.encode(input, "UTF-8"); }
            catch (UnsupportedEncodingException ignored) { return searchUrl() + input.replace(" ", "+"); }
        }
        return input.startsWith("http://") || input.startsWith("https://") ? input : "https://" + input;
    }

    private String searchUrl() {
        switch (browserSettings.searchEngine) {
            case "bing": return "https://www.bing.com/search?q=";
            case "google": return "https://www.google.com/search?q=";
            default: return "https://duckduckgo.com/?q=";
        }
    }

    private void loadUrl(String url) { tabManager.current().session.loadUri(url); }

    private void toggleBookmark() {
        BrowserTab tab = tabManager.current();
        if (tab.url == null || tab.url.isEmpty()) return;
        if (dataManager.isBookmarked(tab.url)) {
            dataManager.removeBookmark(tab.url);
            Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show();
        } else {
            dataManager.addBookmark(tab.url, tab.displayTitle());
            Toast.makeText(this, "Bookmark added", Toast.LENGTH_SHORT).show();
        }
        updateBookmarkButton();
    }

    private void updateBookmarkButton() {
        BrowserTab tab = tabManager == null ? null : tabManager.current();
        bookmarkButton.setSelected(tab != null && tab.url != null && dataManager.isBookmarked(tab.url));
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            String url = data.getStringExtra("url");
            if (url != null && !url.isEmpty()) loadUrl(url);
        }
    }

    @Override public void onBackPressed() {
        BrowserTab tab = tabManager.current();
        if (tab.canGoBack) tab.session.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (tabManager != null) {
            tabManager.persist();
            if (isFinishing()) {
                if (displayedSession != null) geckoView.releaseSession();
                tabManager.closeAll();
            }
        }
        displayedSession = null;
        super.onDestroy();
    }
}
