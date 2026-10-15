package com.ccs.flowybrowser;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Home screen with quick access to frequently visited sites and bookmarks.
 * Similar to Chrome's new tab page with pinned sites.
 */
public class HomeActivity extends AppCompatActivity {

    private LinearLayout quickAccessContainer;
    private BrowserDataManager dataManager;
    private PinnedSitesManager pinnedSitesManager;
    private EditText searchInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        dataManager = new BrowserDataManager(this);
        pinnedSitesManager = new PinnedSitesManager(this);
        initializeViews();
        loadQuickAccessSites();
        setupButtons();
    }

    private void initializeViews() {
        quickAccessContainer = findViewById(R.id.quickAccessContainer);
        searchInput = findViewById(R.id.searchInput);
    }

    private void loadQuickAccessSites() {
        quickAccessContainer.removeAllViews();

        List<PinnedSitesManager.PinnedSite> sites = pinnedSitesManager.getPinnedSites();

        // Show up to 8 pinned sites in grid layout (2 rows x 4 columns)
        int maxQuickAccess = Math.min(sites.size(), 8);
        int itemsAdded = 0;
        boolean addTileAdded = false;

        for (int row = 0; row < 2; row++) {
            LinearLayout rowLayout = createRowLayout();
            quickAccessContainer.addView(rowLayout);

            for (int col = 0; col < 4; col++) {
                if (itemsAdded < maxQuickAccess) {
                    PinnedSitesManager.PinnedSite site = sites.get(itemsAdded);
                    View siteView = createPinnedSiteView(site, itemsAdded);
                    rowLayout.addView(siteView);
                    itemsAdded++;
                } else if (!addTileAdded) {
                    View addTile = createAddTileView();
                    rowLayout.addView(addTile);
                    addTileAdded = true;
                }
            }
        }
    }

    private LinearLayout createRowLayout() {
        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        return rowLayout;
    }

    private View createPinnedSiteView(PinnedSitesManager.PinnedSite site, int index) {
        View siteView = getLayoutInflater().inflate(R.layout.item_quick_access_site, quickAccessContainer, false);

        TextView titleView = siteView.findViewById(R.id.siteTitle);
        TextView siteLetter = siteView.findViewById(R.id.siteLetter);
        ImageView siteIcon = siteView.findViewById(R.id.siteIcon);

        titleView.setText(site.title);
        siteLetter.setText(getInitialLetter(site.title));

        if (!site.iconPath.isEmpty()) {
            File iconFile = new File(site.iconPath);
            if (iconFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(iconFile.getAbsolutePath());
                if (bitmap != null) {
                    siteIcon.setImageBitmap(bitmap);
                    siteLetter.setVisibility(View.GONE);
                }
            }
        }

        siteView.setOnClickListener(v -> openUrl(site.url));
        siteView.setOnLongClickListener(v -> {
            showEditSiteDialog(site, index);
            return true;
        });

        return siteView;
    }

    private View createAddTileView() {
        View addView = getLayoutInflater().inflate(R.layout.item_quick_access_site, quickAccessContainer, false);
        TextView titleView = addView.findViewById(R.id.siteTitle);
        TextView siteLetter = addView.findViewById(R.id.siteLetter);
        ImageView siteIcon = addView.findViewById(R.id.siteIcon);

        titleView.setText("Add site");
        siteLetter.setText("+");
        siteIcon.setImageDrawable(null);
        siteLetter.setVisibility(View.VISIBLE);

        addView.setOnClickListener(v -> showAddSiteDialog());
        return addView;
    }

    private void setupButtons() {
        findViewById(R.id.bookmarksButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, BookmarksActivity.class);
            startActivityForResult(intent, MainActivity.REQUEST_CODE_BOOKMARKS);
        });

        findViewById(R.id.historyButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivityForResult(intent, MainActivity.REQUEST_CODE_HISTORY);
        });

        findViewById(R.id.settingsButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.downloadsButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, DownloadsActivity.class);
            startActivity(intent);
        });

        // Search functionality
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });

        findViewById(R.id.searchGoButton).setOnClickListener(v -> performSearch());
    }

    private void openUrl(String url) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("url", url);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private String getInitialLetter(String title) {
        if (title == null || title.isEmpty()) {
            return "?";
        }
        return title.substring(0, 1).toUpperCase();
    }

    private void showAddSiteDialog() {
        showEditSiteDialog(null, -1);
    }

    private void showEditSiteDialog(PinnedSitesManager.PinnedSite site, int index) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_site, null);
        EditText nameInput = dialogView.findViewById(R.id.siteNameInput);
        EditText urlInput = dialogView.findViewById(R.id.siteUrlInput);

        if (site != null) {
            nameInput.setText(site.title);
            urlInput.setText(site.url);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(site == null ? "Add site" : "Edit site")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String url = urlInput.getText().toString().trim();
                    if (name.isEmpty() || url.isEmpty()) {
                        Toast.makeText(this, "Name and URL required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    url = normalizeUrl(url);
                    fetchFaviconAndSave(name, url, index);
                })
                .setNegativeButton("Cancel", null);

        if (site != null) {
            builder.setNeutralButton("Remove", (dialog, which) -> {
                pinnedSitesManager.removePinnedSite(index);
                loadQuickAccessSites();
            });
        }

        builder.show();
    }

    private String normalizeUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "https://" + url;
        }
        return url;
    }

    private void fetchFaviconAndSave(String name, String url, int index) {
        new FaviconDownloadTask(name, url, index).execute();
    }

    private class FaviconDownloadTask extends AsyncTask<Void, Void, String> {
        private final String name;
        private final String url;
        private final int index;

        FaviconDownloadTask(String name, String url, int index) {
            this.name = name;
            this.url = url;
            this.index = index;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                Uri uri = Uri.parse(url);
                if (uri.getHost() == null) {
                    return "";
                }
                String faviconUrl = uri.getScheme() + "://" + uri.getHost() + "/favicon.ico";
                URL iconUrl = new URL(faviconUrl);
                HttpURLConnection connection = (HttpURLConnection) iconUrl.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "";
                }

                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();

                if (bitmap == null) {
                    return "";
                }

                File dir = new File(getFilesDir(), "icons");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String fileName = "icon_" + System.currentTimeMillis() + ".png";
                File file = new File(dir, fileName);
                FileOutputStream out = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();

                return file.getAbsolutePath();
            } catch (Exception ignored) {
                return "";
            }
        }

        @Override
        protected void onPostExecute(String iconPath) {
            PinnedSitesManager.PinnedSite newSite = new PinnedSitesManager.PinnedSite(url, name, iconPath);
            if (index >= 0) {
                pinnedSitesManager.updatePinnedSite(index, newSite);
            } else {
                pinnedSitesManager.addPinnedSite(newSite);
            }
            loadQuickAccessSites();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            String url = data.getStringExtra("url");
            if (url != null && !url.isEmpty()) {
                openUrl(url);
            }
        }
    }

    private void performSearch() {
        String query = searchInput.getText().toString().trim();
        if (!query.isEmpty()) {
            openUrl(processSearchQuery(query));
        }
    }

    private String processSearchQuery(String query) {
        BrowserDataManager.BrowserSettings settings = dataManager.getSettings();

        // If input contains spaces or no dots, treat as search query
        if (query.contains(" ") || !query.contains(".")) {
            return getSearchUrl(settings.searchEngine) + query.replace(" ", "+");
        }

        // If input doesn't start with http/https, add https
        if (!query.startsWith("http://") && !query.startsWith("https://")) {
            return "https://" + query;
        }

        return query;
    }

    private String getSearchUrl(String engine) {
        switch (engine) {
            case "bing":
                return "https://www.bing.com/search?q=";
            case "duckduckgo":
                return "https://duckduckgo.com/?q=";
            case "google":
            default:
                return "https://www.google.com/search?q=";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh quick access sites when returning to home
        loadQuickAccessSites();
    }
}