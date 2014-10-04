package com.flat.logging;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.flat.util.GsonRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jacob Phillips (10/2014)
 */
public abstract class AbstractRequest<T> extends GsonRequest<T> {
    public static final String URL = "http://10.1.1.1/_/flat/logger.php";

    public AbstractRequest(Class clazz, Map headers, Response.Listener listener, Response.ErrorListener errorListener) {
        super(URL, clazz, headers, listener, errorListener);
    }

    public static abstract class GsonData {
        public String request_checksum;
        public String response_checksum;
        public int response_code;
        public String response;
    }

    protected abstract GsonData getRequest();

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        Map<String, String> p = new HashMap<String, String>();
        p.put("request_checksum", getRequest().request_checksum);
        p.put("response_checksum", getRequest().response_checksum);
        p.put("response_code", String.valueOf(getRequest().response_code));
        p.put("response", getRequest().response);
        return p;
    }
}
