package com.ccs.flowybrowser;

import org.mozilla.geckoview.GeckoSession;

/** Mutable state belonging to one browser tab and its isolated GeckoSession. */
public final class BrowserTab {
    public final long id;
    public final boolean privateMode;
    public final GeckoSession session;
    public String url;
    public String title;
    public boolean canGoBack;
    public boolean canGoForward;
    public boolean loading;
    public int progress;

    BrowserTab(long id, boolean privateMode, GeckoSession session, String url) {
        this.id = id;
        this.privateMode = privateMode;
        this.session = session;
        this.url = url;
        this.title = "";
    }

    public String displayTitle() {
        if (title != null && !title.trim().isEmpty()) return title;
        if (url != null && !url.trim().isEmpty()) return url;
        return privateMode ? "Private tab" : "New tab";
    }
}
