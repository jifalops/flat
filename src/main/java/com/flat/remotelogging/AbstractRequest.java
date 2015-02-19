package com.flat.remotelogging;

import com.android.volley.Response;

import org.json.JSONObject;

import java.util.Map;

/**
 * Created by Jacob Phillips (10/2014)
 */
public abstract class AbstractRequest extends CustomRequest {
    private static final String TAG = AbstractRequest.class.getSimpleName();
    public static final String URL = "http://10.1.1.11/flat/logging_service.php";

    AbstractRequest(Map<String, String> request, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(Method.POST, URL, request, listener, errorListener);
    }


    public static abstract class RequestParams {
        public String request_checksum;
        public String response_checksum;
        public int response_code;
        public String response;
    }
}
