package com.flat.logging;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jacob Phillips (10/2014)
 */
public final class SchemeRequest extends AbstractRequest<SchemeRequest.SchemeData> {
    public static final String TAG = SchemeRequest.class.getSimpleName();

    private final SchemeData params;

    public SchemeRequest(SchemeData params) {
        super(SchemeData.class, null, new Response.Listener<SchemeData>() {
            @Override
            public void onResponse(SchemeData response) {
                Log.d(TAG, response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
            }
        });

        this.params = params;
    }

    public static final class SchemeData extends AbstractRequest.GsonData {
        public final String request = "scheme";

        public String node_id = "";
        public String algorithm = "";
        public long node_time;
        public long db_time;
        public double x, y, z, a, b, c;
    }

    @Override
    protected GsonData getRequest() {
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
