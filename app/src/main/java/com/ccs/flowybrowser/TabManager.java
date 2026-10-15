package com.ccs.flowybrowser;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Owns Gecko sessions, normal-tab persistence and private-tab isolation. */
public final class TabManager {
    public interface SessionConfigurer { void configure(BrowserTab tab); }

    private static final String PREFS = "browser_tabs";
    private static final String KEY_URLS = "normal_urls";
    private static final String KEY_SELECTED = "selected_normal_url";
    private static final String PRIVATE_CONTEXT_ID = "flowy-private";
    private static final String HOME_URL = "https://search.brave.com/";

    private final GeckoRuntime runtime;
    private final SharedPreferences preferences;
    private final SessionConfigurer configurer;
    private final List<BrowserTab> tabs = new ArrayList<>();
    private BrowserTab current;
    private long nextId = System.currentTimeMillis();

    public TabManager(Context context, GeckoRuntime runtime, SessionConfigurer configurer) {
        this.runtime = runtime;
        this.preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.configurer = configurer;
    }

    public BrowserTab restoreOrCreate() {
        JSONArray urls;
        try { urls = new JSONArray(preferences.getString(KEY_URLS, "[]")); }
        catch (Exception ignored) { urls = new JSONArray(); }
        String selectedUrl = preferences.getString(KEY_SELECTED, "");
        BrowserTab selected = null;
        for (int i = 0; i < urls.length(); i++) {
            String url = urls.optString(i, "");
            if (url.isEmpty()) continue;
            BrowserTab tab = create(false, url, false);
            if (url.equals(selectedUrl)) selected = tab;
        }
        if (tabs.isEmpty()) selected = create(false, HOME_URL, false);
        current = selected != null ? selected : tabs.get(0);
        current.session.setActive(true);
        return current;
    }

    public BrowserTab create(boolean privateMode, String url) {
        return create(privateMode, url, true);
    }

    private BrowserTab create(boolean privateMode, String url, boolean select) {
        GeckoSessionSettings.Builder builder = new GeckoSessionSettings.Builder()
                .usePrivateMode(privateMode)
                .allowJavascript(true);
        if (privateMode) builder.contextId(PRIVATE_CONTEXT_ID);
        GeckoSession geckoSession = new GeckoSession(builder.build());
        BrowserTab tab = new BrowserTab(++nextId, privateMode, geckoSession,
                url == null || url.isEmpty() ? HOME_URL : url);
        configurer.configure(tab);
        geckoSession.open(runtime);
        geckoSession.setActive(false);
        tabs.add(tab);
        if (select) select(tab);
        geckoSession.loadUri(tab.url);
        persist();
        return tab;
    }

    public void select(BrowserTab tab) {
        if (tab == null || !tabs.contains(tab)) return;
        if (current != null && current != tab) current.session.setActive(false);
        current = tab;
        current.session.setActive(true);
        persist();
    }

    public BrowserTab closeCurrent() {
        if (current == null) return create(false, HOME_URL);
        BrowserTab closing = current;
        boolean wasPrivate = closing.privateMode;
        int index = tabs.indexOf(closing);
        tabs.remove(closing);
        closing.session.close();

        BrowserTab replacement = nearestTab(index, wasPrivate);
        if (replacement == null) replacement = create(wasPrivate, HOME_URL, false);
        current = replacement;
        current.session.setActive(true);
        if (wasPrivate && count(true) == 0) {
            runtime.getStorageController().clearDataForSessionContext(PRIVATE_CONTEXT_ID);
        }
        persist();
        return current;
    }

    private BrowserTab nearestTab(int oldIndex, boolean privateMode) {
        if (tabs.isEmpty()) return null;
        int start = Math.min(oldIndex, tabs.size() - 1);
        for (int distance = 0; distance < tabs.size(); distance++) {
            int left = start - distance;
            if (left >= 0 && tabs.get(left).privateMode == privateMode) return tabs.get(left);
            int right = start + distance;
            if (right < tabs.size() && tabs.get(right).privateMode == privateMode) return tabs.get(right);
        }
        return null;
    }

    public BrowserTab first(boolean privateMode) {
        for (BrowserTab tab : tabs) if (tab.privateMode == privateMode) return tab;
        return null;
    }

    public BrowserTab find(GeckoSession session) {
        for (BrowserTab tab : tabs) if (tab.session == session) return tab;
        return null;
    }

    public BrowserTab current() { return current; }

    public List<BrowserTab> tabs(boolean privateMode) {
        List<BrowserTab> result = new ArrayList<>();
        for (BrowserTab tab : tabs) if (tab.privateMode == privateMode) result.add(tab);
        return Collections.unmodifiableList(result);
    }

    public int count(boolean privateMode) {
        int count = 0;
        for (BrowserTab tab : tabs) if (tab.privateMode == privateMode) count++;
        return count;
    }

    public void persist() {
        JSONArray urls = new JSONArray();
        for (BrowserTab tab : tabs) {
            if (!tab.privateMode && tab.url != null && tab.url.startsWith("http")) urls.put(tab.url);
        }
        String selected = current != null && !current.privateMode ? current.url : "";
        preferences.edit().putString(KEY_URLS, urls.toString()).putString(KEY_SELECTED, selected).apply();
    }

    public void closeAll() {
        boolean hadPrivate = false;
        for (BrowserTab tab : new ArrayList<>(tabs)) {
            hadPrivate |= tab.privateMode;
            tab.session.close();
        }
        tabs.clear();
        current = null;
        if (hadPrivate) runtime.getStorageController().clearDataForSessionContext(PRIVATE_CONTEXT_ID);
    }
}
