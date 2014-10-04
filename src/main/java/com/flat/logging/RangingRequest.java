package com.flat.logging;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;

import java.util.Map;

/**
 * Created by Jacob Phillips (10/2014)
 */
public final class RangingRequest extends AbstractRequest{
    private static final String TAG = RangingRequest.class.getSimpleName();

    private final RangeParams params;

    public RangingRequest(RangeParams params, Map headers, Response.Listener listener, Response.ErrorListener errorListener) {
        super(RangeParams.class, headers, listener, errorListener);
        this.params = params;
    }

    @Override
    RequestParams getRequestParams() {
        return params;
    }

    public static final class RangeParams extends RequestParams {
        public final String request = "ranging";

        public String node_id;
        public String remote_node_id;
        public String algorithm;
        public double estimate;
        public double actual;
        public long node_time;
        public long db_time;
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        Map<String, String> p = super.getParams();
        p.put("request", params.request);
        p.put("node_id", params.node_id);
        p.put("remote_node_id", params.remote_node_id);
        p.put("algorithm", params.algorithm);
        p.put("estimate", String.valueOf(params.estimate));
        p.put("actual", String.valueOf(params.actual));
        p.put("node_time", String.valueOf(params.node_time));
        p.put("db_time", String.valueOf(params.db_time));
        return p;
    }
}
