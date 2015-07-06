package com.dragontek.mygpoclient;

import org.apache.http.HttpHost;

public class Locator {
    String _username;
    String _simpleBase;
    String _base;
    HttpHost _targetHost;

    public Locator(String username) {
        this(username, Global.HOST);
    }

    public Locator(String username, String host) {
        this(username, host, Global.VERSION);
    }

    public Locator(String username, String host, int version) {
        this._username = username;
        this._targetHost = new HttpHost(host, 443, "https");

        this._simpleBase = _targetHost.toURI();
        this._base = String.format("%s/api/%s", _simpleBase, version);

    }

    public String clientConfigUri() {
        return Util
                .join(new String[]{this._simpleBase, "clientconfig.json"});
    }

    public String loginUri() {
        // Use SSL on this one!
        return Util.join(new String[]{this._base, "auth", _username,
                "login.json"});
    }

    public String logoutUri() {
        return Util.join(new String[]{this._base, "auth", _username,
                "logout.json"});
    }

    public String subscriptionsUri(String deviceId) {
        return subscriptionsUri(deviceId, "json");
    }

    public String subscriptionsUri(String deviceId, String format) {
        // TODO: Assert format is valid or throw error.
        String filename = String.format("%s.%s", deviceId, format);
        return Util.join(new String[]{this._simpleBase, "subscriptions",
                this._username, filename});
    }

    public String toplistUri() {
        return toplistUri(25);
    }

    public String toplistUri(int count) {
        return toplistUri(count, "json");
    }

    public String toplistUri(int count, String format) {
        // TODO: Assert count and format are valid or throw error.
        String filename = String.format("toplist/%s.%s", count, format);
        return Util.join(new String[]{this._simpleBase, filename});
    }

    public String suggestionsUri() {
        return suggestionsUri(10);
    }

    public String suggestionsUri(int count) {
        return toplistUri(count, "json");
    }

    public String suggestionsUri(int count, String format) {
        // TODO: Assert count and format are valid or throw error.
        String filename = String.format("suggestions/%s.%s", count, format);
        return Util.join(new String[]{this._simpleBase, filename});
    }

    public String searchUri(String query) {
        return searchUri(query, "json");
    }

    public String searchUri(String query, String format) {
        // TODO: Assert format is valid or throw error.
        String filename = String.format("search.%s?q=%s", format, query);
        return Util.join(new String[]{this._simpleBase, filename});
    }

    public String addRemoveSubscriptionsUri(String deviceId) {
        String filename = String.format("%s.json", deviceId);
        return Util.join(new String[]{this._base, "subscriptions",
                this._username, filename});
    }

    public String subscriptionUpdatesUri(String deviceId) {
        return subscriptionUpdatesUri(deviceId, 0);
    }

    public String subscriptionUpdatesUri(String deviceId, long since) {
        String filename = String.format("%s.json?since=%s", deviceId, since);

        return Util.join(new String[]{this._base, "subscriptions",
                this._username, filename});
    }

    public String uploadEpisodeActionsUri() {
        String filename = String.format("%s.json", this._username);
        return Util.join(new String[]{this._base, "episodes", filename});
    }

    public String downloadEpisodeActionsUri(long since, String podcast,
                                            String deviceId) {
        String filename = String.format("%s.json?since=%s",
                this._username, since);
        if (deviceId != null)
            filename += String.format("&device=%s", deviceId);
        if (podcast != null)
            filename += String.format("&podcast=%s", podcast);

        return Util.join(new String[]{this._base, "episodes", filename});
    }

    public String downloadEpisodeActionsUri(long since, String deviceId) {
        return downloadEpisodeActionsUri(since, null, deviceId);
    }

    public String deviceUpdatesUri(long since, String deviceId) {
        String filename = String.format("%s.json?since=%s", deviceId, since);
        return Util.join(new String[]{this._base, "updates", this._username,
                filename});
    }

    public String deviceSettingsUri(String deviceId) {
        String filename = String.format("%s.json", deviceId);
        return Util.join(new String[]{this._base, "devices", this._username,
                filename});
    }

    public String deviceListUri() {
        String filename = String.format("%s.json", this._username);
        return Util.join(new String[]{this._base, "devices", filename});
    }

    public String getPodcastDataUri(String podcastUrl) {
        String filename = String.format("podcast.json?url=%s", podcastUrl);
        return Util.join(new String[]{this._base, "data", filename});
    }

    public String getEpisodeDataUri(String podcastUrl, String episodeUrl) {
        String filename = String.format("episode.json?podcast=%s&url=%s",
                podcastUrl, episodeUrl);
        return Util.join(new String[]{this._base, "data", filename});
    }

    public String favoriteEpisodesUri() {
        String filename = String.format("%s.json", this._username);
        return Util.join(new String[]{this._base, "favorites", filename});
    }

    public String deviceSynchronizationUri() {
        String filename = String.format("%s.json", this._username);
        return Util.join(new String[]{this._base, "sync-devices", filename});
    }

    public String getPodcastListsUri() {
        String filename = String.format("%s.json", this._username);
        return Util.join(new String[]{this._base, "lists", filename});
    }

    public String getPodcastListUri(String listName, String format) {
        String filename = String.format("%s.%s", listName, format);
        return Util.join(new String[]{this._base, "lists", this._username,
                "list", filename});
    }
}
