package com.flat.logging;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;

import java.util.Map;

/**
 * Created by Jacob Phillips (10/2014)
 */
public final class SchemeRequest extends AbstractRequest {
    private static final String TAG = SchemeRequest.class.getSimpleName();

    private final SchemeParams params;

    public SchemeRequest(SchemeParams params,  Map headers, Response.Listener listener, Response.ErrorListener errorListener) {
        super(SchemeParams.class, headers, listener, errorListener);
        this.params = params;
    }

    public static final class SchemeParams extends RequestParams {
        public final String request = "scheme";

        public String node_id = "";
        public String algorithm = "";
        public long node_time;
        public long db_time;
        public double x, y, z, a, b, c;
    }

    @Override
    RequestParams getRequestParams() {
        return params;
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        Map<String, String> p = super.getParams();
        p.put("request", params.request);
        p.put("node_id", params.node_id);
        p.put("algorithm", params.algorithm);
        p.put("node_time", String.valueOf(params.node_time));
        p.put("db_time", String.valueOf(params.db_time));
        p.put("x", String.valueOf(params.x));
        p.put("y", String.valueOf(params.y));
        p.put("z", String.valueOf(params.z));
        p.put("a", String.valueOf(params.a));
        p.put("b", String.valueOf(params.b));
        p.put("c", String.valueOf(params.c));
        return p;
    }
}
