package com.dragontek.mygpoclient.json;

import com.google.gson.Gson;

import com.dragontek.mygpoclient.http.HttpClient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;

public class JsonClient extends HttpClient {

    public JsonClient() {
        super();
    }

    public JsonClient(String username, String password) {
        super(username, password);
    }

    public static String encode(Object data) {
        Gson gson = new Gson();
        return gson.toJson(data);
    }

    public static <T> T decode(String data, Class<T> clazz) {
        Gson gson = new Gson();
        return gson.fromJson(data, clazz);
    }

    @Override
    protected HttpUriRequest prepareRequest(String method, String uri,
                                            HttpEntity data) {
        HttpUriRequest request = super.prepareRequest(method, uri, data);
        request.addHeader("Accept", "application/json");
        return request;
    }

    @Override
    protected String processResponse(HttpResponse response)
            throws IllegalStateException, IOException {
        //noinspection UnnecessaryLocalVariable
        String data = super.processResponse(response);
        // return decode(data);
        return data;
    }
}