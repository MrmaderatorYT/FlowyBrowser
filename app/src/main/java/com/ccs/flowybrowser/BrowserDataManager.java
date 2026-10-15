package com.ccs.flowybrowser;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages browser data: bookmarks, history, and settings.
 * Uses SharedPreferences with JSON to avoid reflection and reduce crashes.
 */
public class BrowserDataManager {

    private static final String PREFS_NAME = "browser_data";
    private static final String KEY_BOOKMARKS = "bookmarks";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_SETTINGS = "settings";

    private final SharedPreferences prefs;

    public BrowserDataManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ===== BOOKMARKS =====
    public void addBookmark(String url, String title) {
        List<BookmarkItem> bookmarks = getBookmarks();
        // Remove if already exists
        bookmarks.removeIf(bookmark -> bookmark.url.equals(url));
        bookmarks.add(new BookmarkItem(url, title, System.currentTimeMillis()));
        saveBookmarks(bookmarks);
    }

    public void removeBookmark(String url) {
        List<BookmarkItem> bookmarks = getBookmarks();
        bookmarks.removeIf(bookmark -> bookmark.url.equals(url));
        saveBookmarks(bookmarks);
    }

    public boolean isBookmarked(String url) {
        List<BookmarkItem> bookmarks = getBookmarks();
        for (BookmarkItem item : bookmarks) {
            if (item.url.equals(url)) {
                return true;
            }
        }
        return false;
    }

    public List<BookmarkItem> getBookmarks() {
        String json = prefs.getString(KEY_BOOKMARKS, "[]");
        return parseBookmarks(json);
    }

    private void saveBookmarks(List<BookmarkItem> bookmarks) {
        String json = toBookmarksJson(bookmarks);
        prefs.edit().putString(KEY_BOOKMARKS, json).apply();
    }

    // ===== HISTORY =====
    public void addToHistory(String url, String title) {
        List<HistoryItem> history = getHistory();
        // Remove if already exists (move to top)
        history.removeIf(item -> item.url.equals(url));
        history.add(0, new HistoryItem(url, title, System.currentTimeMillis()));
        // Keep only last 100 items
        if (history.size() > 100) {
            history = history.subList(0, 100);
        }
        saveHistory(history);
        // Clean old entries (older than 30 days)
        cleanOldHistory();
    }

    public List<HistoryItem> getHistory() {
        String json = prefs.getString(KEY_HISTORY, "[]");
        return parseHistory(json);
    }

    private void saveHistory(List<HistoryItem> history) {
        String json = toHistoryJson(history);
        prefs.edit().putString(KEY_HISTORY, json).apply();
    }

    private void cleanOldHistory() {
        List<HistoryItem> history = getHistory();
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000); // 30 days
        history.removeIf(item -> item.timestamp < thirtyDaysAgo);
        saveHistory(history);
    }

    // ===== SETTINGS =====
    public BrowserSettings getSettings() {
        String json = prefs.getString(KEY_SETTINGS, null);
        if (json == null) {
            return new BrowserSettings(); // Default settings
        }
        return parseSettings(json);
    }

    public void saveSettings(BrowserSettings settings) {
        String json = toSettingsJson(settings);
        prefs.edit().putString(KEY_SETTINGS, json).apply();
    }

    // ===== JSON HELPERS =====
    private List<BookmarkItem> parseBookmarks(String json) {
        List<BookmarkItem> bookmarks = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                BookmarkItem item = new BookmarkItem(
                        obj.optString("url", ""),
                        obj.optString("title", ""),
                        obj.optLong("timestamp", 0L)
                );
                if (!item.url.isEmpty()) {
                    bookmarks.add(item);
                }
            }
        } catch (JSONException ignored) {
            // Corrupted data -> return empty list
        }
        return bookmarks;
    }

    private String toBookmarksJson(List<BookmarkItem> bookmarks) {
        JSONArray array = new JSONArray();
        for (BookmarkItem item : bookmarks) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("url", item.url);
                obj.put("title", item.title);
                obj.put("timestamp", item.timestamp);
                array.put(obj);
            } catch (JSONException ignored) {
                // Skip invalid item
            }
        }
        return array.toString();
    }

    private List<HistoryItem> parseHistory(String json) {
        List<HistoryItem> history = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                HistoryItem item = new HistoryItem(
                        obj.optString("url", ""),
                        obj.optString("title", ""),
                        obj.optLong("timestamp", 0L)
                );
                if (!item.url.isEmpty()) {
                    history.add(item);
                }
            }
        } catch (JSONException ignored) {
            // Corrupted data -> return empty list
        }
        return history;
    }

    private String toHistoryJson(List<HistoryItem> history) {
        JSONArray array = new JSONArray();
        for (HistoryItem item : history) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("url", item.url);
                obj.put("title", item.title);
                obj.put("timestamp", item.timestamp);
                array.put(obj);
            } catch (JSONException ignored) {
                // Skip invalid item
            }
        }
        return array.toString();
    }

    private BrowserSettings parseSettings(String json) {
        BrowserSettings settings = new BrowserSettings();
        try {
            JSONObject obj = new JSONObject(json);
            settings.searchEngine = obj.optString("searchEngine", settings.searchEngine);
            settings.enableJavaScript = obj.optBoolean("enableJavaScript", settings.enableJavaScript);
            settings.trustedDomains.clear();
            JSONArray domains = obj.optJSONArray("trustedDomains");
            if (domains != null) {
                for (int i = 0; i < domains.length(); i++) {
                    String domain = domains.optString(i, "");
                    if (!domain.isEmpty()) {
                        settings.trustedDomains.add(domain);
                    }
                }
            }
        } catch (JSONException ignored) {
            // Corrupted data -> fallback to defaults
        }
        return settings;
    }

    private String toSettingsJson(BrowserSettings settings) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("searchEngine", settings.searchEngine);
            obj.put("enableJavaScript", settings.enableJavaScript);
            JSONArray domains = new JSONArray();
            for (String domain : settings.trustedDomains) {
                domains.put(domain);
            }
            obj.put("trustedDomains", domains);
        } catch (JSONException ignored) {
            // Ignore and return best-effort JSON
        }
        return obj.toString();
    }

    // ===== DATA CLASSES =====
    public static class BookmarkItem {
        public String url;
        public String title;
        public long timestamp;

        public BookmarkItem() {}

        public BookmarkItem(String url, String title, long timestamp) {
            this.url = url;
            this.title = title;
            this.timestamp = timestamp;
        }
    }

    public static class HistoryItem {
        public String url;
        public String title;
        public long timestamp;

        public HistoryItem() {}

        public HistoryItem(String url, String title, long timestamp) {
            this.url = url;
            this.title = title;
            this.timestamp = timestamp;
        }
    }

    public static class BrowserSettings {
        public String searchEngine = "google"; // google, bing, duckduckgo
        public List<String> trustedDomains = new ArrayList<>(); // Domains where JS is allowed
        public boolean enableJavaScript = false;

        public BrowserSettings() {
            // Add some default trusted domains
            trustedDomains.add("google.com");
            trustedDomains.add("youtube.com");
            trustedDomains.add("wikipedia.org");
        }
    }
}