package com.flat.loggingrequests;

import com.android.volley.Response;
import com.flat.localization.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jacob Phillips (10/2014)
 */
public final class RangingRequest extends AbstractRequest {
    private static final String TAG = RangingRequest.class.getSimpleName();

    static final String URL = "http://10.1.1.11/flat/logging_service.php";

    public static Map<String, String> makeRequest(Node.Range range, Node src, Node dst) {
        RangeParams params = new RangeParams(range, src, dst);
        Map<String, String> p = new HashMap<String, String>();
        p.put("request", params.request);
        p.put("node_id", params.node_id);
        p.put("remote_node_id", params.remote_node_id);
        p.put("signal", params.signal);
        p.put("algorithm", params.algorithm);
        p.put("estimate", String.valueOf(params.estimate));
        p.put("actual", String.valueOf(params.actual));
        p.put("node_time", String.valueOf(params.node_time));
        return p;
    }


    public RangingRequest(Map<String, String> request, Response.Listener listener, Response.ErrorListener errorListener) {
        super(request, listener, errorListener);

    }

    public static final class RangeParams extends RequestParams {
        public final String request = "ranging";
        public String node_id;
        public String remote_node_id;
        public String signal;
        public String algorithm;
        public float estimate;
        public float actual;
        public long node_time;

        public RangeParams(Node.Range r, Node src, Node dst) {
            node_id = src.getId();
            remote_node_id = dst.getId();
            signal = r.signal;
            algorithm = r.algorithm;
            estimate = r.dist;
            actual = r.actual;
            node_time = r.time;
        }
    }
}
