package com.dragontek.mygpoclient.pub;

import com.dragontek.mygpoclient.Global;
import com.dragontek.mygpoclient.Locator;
import com.dragontek.mygpoclient.json.JsonClient;
import com.dragontek.mygpoclient.simple.IPodcast;
import com.dragontek.mygpoclient.simple.Podcast;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for the gpodder.net "anonymous" API
 * <p/>
 * This is the API client implementation that provides a Java interface to the
 * parts of the gpodder.net Simple API that don't need user authentication.
 *
 * @author joshua.mondragon
 */
public class PublicClient {
    public static String FORMAT = "json";
    public Locator _locator;
    public JsonClient _client;
    private Gson _gson;

    /**
     * Creates a new Public API client
     */
    public PublicClient() {
        this(Global.HOST);
    }

    /**
     * Creates a new Public API client
     *
     * @param host hostname of the webservice (gpodder.net)
     */
    public PublicClient(String host) {
        this._locator = new Locator(host);
        this._client = new JsonClient();
        this._gson = new Gson();
    }

    /**
     * Get a list of most-subscribed podcasts
     *
     * @param count the amount of podcasts that are returned. The minimum value is
     *              1 and the maximum value is 100.
     * @return Returns a list of {@link ToplistPodcast} objects.
     * @throws IOException
     */
    public List<IPodcast> getToplist(int count) throws IOException {
        String uri = _locator.toplistUri(count);
        Type collectionType = new TypeToken<ArrayList<Podcast>>() {
        }.getType();
        return _gson.fromJson(_client.GET(uri), collectionType);
    }

    /**
     * Get a list of most-subscribed podcasts
     *
     * @return Returns a list of {@link ToplistPodcast} objects.
     * @throws IOException
     */
    public List<IPodcast> getToplist() throws IOException {
        return getToplist(Global.TOPLIST_DEFAULT);
    }

    /**
     * Search for podcasts on the webservice
     *
     * @param query specifies the search query as a string
     * @return Returns a list of Podcast objects.
     * @throws IOException
     */
    public List<IPodcast> searchPodcast(String query) throws IOException {
        Type collectionType = new TypeToken<ArrayList<Podcast>>() {
        }.getType();
        return _gson.fromJson(_client.GET(_locator.searchUri(URLEncoder.encode(
                query, "UTF-8"))), collectionType);
    }

    public Podcast getPodcastData(String podcastUrl) throws IOException {
        return _gson.fromJson(
                _client.GET(_locator.getPodcastDataUri(podcastUrl)),
                Podcast.class);
    }

    public ClientConfig getConfiguration() throws IOException {
        return _gson.fromJson(_client.GET(_locator.clientConfigUri()),
                ClientConfig.class);
    }
}
