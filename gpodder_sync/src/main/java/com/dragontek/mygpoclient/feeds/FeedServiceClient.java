package com.dragontek.mygpoclient.feeds;

import com.dragontek.mygpoclient.json.JsonClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FeedServiceClient extends JsonClient {

    private static String HOST = "http://feeds.dragontek.com";
    private long last_updated_timestamp = 0;

    public FeedServiceClient() {
        this(HOST);
    }

    public FeedServiceClient(String host) {
        this(host, null, null);
    }

    public FeedServiceClient(String host, String username, String password) {
        super(username, password);
        HOST = host;
    }

    @Override
    public HttpUriRequest prepareRequest(String method, String uri,
                                         HttpEntity data) {
        HttpUriRequest request = super.prepareRequest(method, uri, data);
        request.addHeader("Accept-Encoding", "gzip");
        if (last_updated_timestamp != 0)
            request.addHeader("If-Modified-Since",
                    String.valueOf(last_updated_timestamp / 1000)); // "Tue, 16 Apr 2013 02:52:48 -0000");
        // //new
        // Date(last_updated_timestamp).toGMTString());
        return request;
    }

    public FeedServiceResponse parseFeeds(String[] feed_urls)
            throws ClientProtocolException, IOException {
        return parseFeeds(feed_urls, 0L, false, true, false, 0, null);
    }

    public FeedServiceResponse parseFeeds(String[] feed_urls, long last_modified)
            throws ClientProtocolException, IOException {
        return parseFeeds(feed_urls, last_modified, false, true, false, 0, null);
    }

    public FeedServiceResponse parseFeeds(String[] feed_urls,
                                          long last_modified, boolean strip_html)
            throws ClientProtocolException, IOException {
        return parseFeeds(feed_urls, last_modified, strip_html, true, false, 0,
                null);
    }

    public FeedServiceResponse parseFeeds(String[] feed_urls,
                                          long last_modified, boolean strip_html, boolean use_cache)
            throws ClientProtocolException, IOException {
        return parseFeeds(feed_urls, last_modified, strip_html, use_cache,
                false, 0, null);
    }

    public FeedServiceResponse parseFeeds(String[] feed_urls,
                                          long last_modified, boolean strip_html, boolean use_cache,
                                          boolean inline_logo) throws ClientProtocolException, IOException {
        return parseFeeds(feed_urls, last_modified, strip_html, use_cache,
                inline_logo, 0, null);
    }

    public FeedServiceResponse parseFeeds(String[] feed_urls,
                                          long last_modified, boolean strip_html, boolean use_cache,
                                          boolean inline_logo, int scale_logo)
            throws ClientProtocolException, IOException {
        return parseFeeds(feed_urls, last_modified, strip_html, use_cache,
                inline_logo, scale_logo, null);
    }

    public FeedServiceResponse parseFeeds(String[] feed_urls,
                                          final long last_modified, boolean strip_html, boolean use_cache,
                                          boolean inline_logo, int scale_logo, String logo_format)
            throws ClientProtocolException, IOException {
        List<NameValuePair> args = new ArrayList<>();
        if (strip_html)
            args.add(new BasicNameValuePair("process_text", "strip_html"));
        if (use_cache)
            args.add(new BasicNameValuePair("use_cache", "1"));
        if (inline_logo) {
            args.add(new BasicNameValuePair("inline_logo", "1"));
            args.add(new BasicNameValuePair("scale_logo", Integer
                    .toString(scale_logo)));
        }
        if (logo_format != null && !logo_format.isEmpty())
            args.add(new BasicNameValuePair("logo_format", logo_format));

        for (String feed_url : feed_urls) {
            BasicNameValuePair nvp = new BasicNameValuePair("url", feed_url);
            if (!args.contains(nvp))
                args.add(new BasicNameValuePair("url", feed_url));
        }

        // Set the member variable so we can set the header in prepareRequest
        last_updated_timestamp = last_modified;

        Gson gson = new Gson();
        String response = this.POST(HOST + "/parse", new UrlEncodedFormEntity(
                args, "UTF-8"));

        // if(Global.DEBUG)
        // System.out.println("RESPONSE: " + response);

        // POST
        Type collectionType = new TypeToken<ArrayList<Feed>>() {
        }.getType();
        List<Feed> feeds = gson.fromJson(response, collectionType);
        // GET
        // List<Feed> response = gson.fromJson(this.GET(HOST + "/parse?" +
        // URLEncodedUtils.format(args, "UTF-8")), collectionType);

        return new FeedServiceResponse(feeds, 0L, feed_urls);
    }

}
