package com.dragontek.mygpoclient.http;

import com.dragontek.mygpoclient.Global;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class HttpClient extends DefaultHttpClient {

    private String _authToken;
    private String _username = null;
    private String _password = null;

    public HttpClient() {
    }

    public HttpClient(String username, String password) {
        this._username = username;
        this._password = password;
    }

    public static String convertStreamToString(InputStream is)
            throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is,
                Charset.forName("UTF-8")));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } finally {
            try {
                is.close();
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    protected HttpUriRequest prepareRequest(String method, String uri,
                                            HttpEntity entity) {
        // TODO: add params to uri if it's a GET instead of a post
        HttpUriRequest request = new HttpGet(uri);

        if (method.equals("POST")) {
            request = new HttpPost(uri);
            ((HttpPost) request).setEntity(entity);
        } else if (method.equals("PUT")) {
            request = new HttpPut(uri);
            ((HttpPut) request).setEntity(entity);
        }

        // Authentication
        if (_username != null && _password != null) {
            Credentials creds = new UsernamePasswordCredentials(_username,
                    _password);
            AuthScope scope = new AuthScope(request.getURI().getHost(), request
                    .getURI().getPort());
            getCredentialsProvider().setCredentials(scope, creds);
        }

        request.addHeader("User-Agent", Global.USER_AGENT);
        return request;
    }

    protected String processResponse(HttpResponse response)
            throws IllegalStateException, IOException {
        String result = null;
        HttpEntity entity = response.getEntity();
        if (Global.DEBUG) {
            System.out.println("RESPONSE");
            for (Header header : response.getAllHeaders()) {
                System.out.println("HEADER: " + header.getName() + " = "
                        + header.getValue());
            }
        }
        if (entity != null) {
            InputStream instream = entity.getContent();
            result = convertStreamToString(instream);
            instream.close();
        }
        return result;
    }

    public String getAuthToken() {
        return this._authToken;
    }

    public void setAuthToken(String token) {
        this._authToken = token;
    }

    protected String request(String method, String uri, HttpEntity data)
            throws IOException {
        HttpUriRequest request = prepareRequest(method, uri, data);
        HttpResponse response = execute(request);

        if (Global.DEBUG) {
            System.out.println(String.format("%s: %s", method, uri));

            if (data != null) {
                System.out.println("DATA:");
                data.writeTo(System.out);
                System.out.println();
            }
            for (Header h : request.getAllHeaders()) {
                System.out.println(String.format("HEADER: %s: %s", h.getName(),
                        h.getValue()));
            }
            for (Cookie c : this.getCookieStore().getCookies()) {
                // if(c.getName().equals("sessionid"));
                // _authToken = c.getValue();
                System.out.println(String.format("COOKIE: %s: %s -- %s",
                        c.getName(), c.getValue(), c.getDomain()));

                System.out.println(response.getStatusLine());
                // System.out.println(response);
            }
        }

        StatusLine s = response.getStatusLine();
        if (s.getStatusCode() == 200) {
            return processResponse(response);
        } else {
            // If we get anything other than 200 throw it so we can handle it
            throw new HttpResponseException(s.getStatusCode(), s.toString());
        }
    }

    public String GET(String uri) throws IOException {
        return request("GET", uri, null);
    }

    public String GET(String uri, HttpEntity data) throws IOException {
        return request("GET", uri, data);
    }

    public String POST(String uri, HttpEntity data) throws IOException {
        return request("POST", uri, data);
    }

    public String PUT(String uri, HttpEntity data) throws IOException {
        return request("PUT", uri, data);
    }
}
