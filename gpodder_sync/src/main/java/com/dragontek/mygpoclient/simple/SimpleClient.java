package com.dragontek.mygpoclient.simple;

import com.dragontek.mygpoclient.Global;
import com.dragontek.mygpoclient.Locator;
import com.dragontek.mygpoclient.http.HttpClient;
import com.dragontek.mygpoclient.json.JsonClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpResponseException;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Client for the gpodder.net Simple API
 * <p/>
 * This is the API client implementation that provides a java interface to the
 * gpodder.net Simple API.
 *
 * @author jmondragon
 */
public class SimpleClient {
    public static String FORMAT = "json";
    protected Locator _locator;
    protected JsonClient _client;
    protected String _authToken;
    protected Gson _gson;

    public SimpleClient(String username) {
        this(username, null, Global.HOST);
    }

    /**
     * Creates a new Simple API client
     * <p/>
     * Username and password must be specified and are the user's login data for
     * the webservice.
     * <p/>
     * The parameter host is optional and defaults to the main webservice.
     * <p/>
     * The parameter client_class is optional and should not need to be changed
     * in normal use cases. If it is changed, it should provide the same
     * interface as the json.JsonClient class in mygpoclient.
     *
     * @param username
     * @param password
     */
    public SimpleClient(String username, String password) {
        this(username, password, Global.HOST);
    }

    public SimpleClient(String username, String password, String host) {
        this._gson = new Gson();
        this._locator = new Locator(username, host);
        this._client = new JsonClient(username, password);
    }

    public String getAuthToken() {
        return _authToken;
    }

    public void setAuthToken(String authToken) {
        _authToken = authToken;
        CookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie("sessionid",
                _authToken);
        cookie.setDomain("gpodder.net");
        cookieStore.addCookie(cookie);
        this._client.setCookieStore(cookieStore);
    }

    public boolean authenticate(String username, String password) {
        if (username != null && password != null) {
            HttpClient tempClient = new HttpClient(username, password);
            try {
                tempClient.POST(_locator.loginUri(), null);
                for (Cookie c : tempClient.getCookieStore().getCookies()) {
                    if (c.getName().equals("sessionid"))
                        ;
                    _authToken = c.getValue();
                }
            } catch (Exception e) {
                _authToken = null;
            }
        }
        return _authToken != null;
    }

    public Set<String> getSubscriptions(String deviceId) throws IOException,
            AuthenticationException {
        String uri = _locator.subscriptionsUri(deviceId);
        Type collectionType = new TypeToken<HashSet<String>>() {
        }.getType();
        try {
            return _gson.fromJson(_client.GET(uri), collectionType);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 401)
                throw new AuthenticationException(
                        "Unable to authenticate user with Gpodder.net", e);
            else
                throw e;
        }

    }

    public boolean putSubscriptions(String deviceId, List<String> urls)
            throws IOException, AuthenticationException {
        String uri = _locator.subscriptionsUri(deviceId);
        try {
            String response = _client.PUT(uri,
                    new StringEntity(_gson.toJson(urls)));
            return (response.isEmpty());
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 401)
                throw new AuthenticationException(
                        "Unable to authenticate user with Gpodder.net", e);
            else
                throw e;
        }

    }

    public List<Podcast> getSuggestions(int count) throws IOException,
            AuthenticationException {
        String uri = _locator.suggestionsUri(count);
        Type collectionType = new TypeToken<ArrayList<Podcast>>() {
        }.getType();
        try {
            return _gson.fromJson(_client.GET(uri), collectionType);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 401)
                throw new AuthenticationException(
                        "Unable to authenticate user with Gpodder.net", e);
            else
                throw e;
        }

    }
}
