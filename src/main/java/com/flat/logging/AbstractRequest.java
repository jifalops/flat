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
    static final String URL = "http://10.1.1.1/_/flat/logger.php";

    AbstractRequest(Class clazz, Map headers, Response.Listener listener, Response.ErrorListener errorListener) {
        super(URL, clazz, headers, listener, errorListener);
        setTag(this.toString());
    }

    public static abstract class RequestParams {
        public String request_checksum;
        public String response_checksum;
        public int response_code;
        public String response;
    }

    // http://www.androidhive.info/2014/05/android-working-with-volley-library-1/
    abstract RequestParams getRequestParams();

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        Map<String, String> p = new HashMap<String, String>();
        p.put("request_checksum", getRequestParams().request_checksum);
        p.put("response_checksum", getRequestParams().response_checksum);
        p.put("response_code", String.valueOf(getRequestParams().response_code));
        p.put("response", getRequestParams().response);
        return p;
    }
}
