package com.ccs.flowybrowser;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages pinned sites for the home quick access panel.
 * Uses JSON in SharedPreferences to keep data stable across sessions.
 */
public class PinnedSitesManager {

    private static final String PREFS_NAME = "pinned_sites_data";
    private static final String KEY_PINNED = "pinned_sites";

    private final SharedPreferences prefs;

    public PinnedSitesManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<PinnedSite> getPinnedSites() {
        String json = prefs.getString(KEY_PINNED, null);
        if (json == null) {
            List<PinnedSite> defaults = getDefaultSites();
            savePinnedSites(defaults);
            return defaults;
        }
        return parsePinnedSites(json);
    }

    public void savePinnedSites(List<PinnedSite> sites) {
        prefs.edit().putString(KEY_PINNED, toPinnedSitesJson(sites)).apply();
    }

    public void addPinnedSite(PinnedSite site) {
        List<PinnedSite> sites = getPinnedSites();
        sites.add(site);
        savePinnedSites(sites);
    }

    public void updatePinnedSite(int index, PinnedSite site) {
        List<PinnedSite> sites = getPinnedSites();
        if (index >= 0 && index < sites.size()) {
            sites.set(index, site);
            savePinnedSites(sites);
        }
    }

    public void removePinnedSite(int index) {
        List<PinnedSite> sites = getPinnedSites();
        if (index >= 0 && index < sites.size()) {
            sites.remove(index);
            savePinnedSites(sites);
        }
    }

    private List<PinnedSite> parsePinnedSites(String json) {
        List<PinnedSite> sites = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String url = obj.optString("url", "");
                String title = obj.optString("title", "");
                String iconPath = obj.optString("iconPath", "");
                if (!url.isEmpty()) {
                    sites.add(new PinnedSite(url, title, iconPath));
                }
            }
        } catch (JSONException ignored) {
            // Corrupted data -> fallback to defaults
        }
        return sites;
    }

    private String toPinnedSitesJson(List<PinnedSite> sites) {
        JSONArray array = new JSONArray();
        for (PinnedSite site : sites) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("url", site.url);
                obj.put("title", site.title);
                obj.put("iconPath", site.iconPath);
                array.put(obj);
            } catch (JSONException ignored) {
                // Skip invalid entry
            }
        }
        return array.toString();
    }

    private List<PinnedSite> getDefaultSites() {
        List<PinnedSite> defaults = new ArrayList<>();
        defaults.add(new PinnedSite("https://www.google.com", "Google", ""));
        defaults.add(new PinnedSite("https://www.youtube.com", "YouTube", ""));
        defaults.add(new PinnedSite("https://www.wikipedia.org", "Wikipedia", ""));
        defaults.add(new PinnedSite("https://www.github.com", "GitHub", ""));
        defaults.add(new PinnedSite("https://www.stackoverflow.com", "Stack Overflow", ""));
        defaults.add(new PinnedSite("https://www.reddit.com", "Reddit", ""));
        return defaults;
    }

    public static class PinnedSite {
        public final String url;
        public final String title;
        public final String iconPath;

        public PinnedSite(String url, String title, String iconPath) {
            this.url = url;
            this.title = title;
            this.iconPath = iconPath;
        }
    }
}
