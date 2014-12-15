//package com.flat.loggingrequests;
//
//import com.android.volley.AuthFailureError;
//import com.android.volley.Response;
//
//import java.util.Map;
//
///**
// * Created by Jacob Phillips (10/2014)
// */
//public final class StateRequest extends AbstractRequest {
//    private static final String TAG = StateRequest.class.getSimpleName();
//
//    private final StateParams params;
//
//    public StateRequest(StateParams params, Map headers, Response.Listener listener, Response.ErrorListener errorListener) {
//        super(StateParams.class, headers, listener, errorListener);
//        this.params = params;
//    }
//
//    public static final class StateParams extends RequestParams {
//        public final String request = "state";
//
//        public String node_id;
//        public String remote_node_id;
//        public String algorithm = "";
//        public double x, y, z, a, b, c;
//        public long node_time;
//        public long node_datetime;
//    }
//
//    @Override
//    RequestParams getRequestParams() {
//        return params;
//    }
//
//    @Override
//    protected Map<String, String> getParams() throws AuthFailureError {
//        Map<String, String> p = super.getParams();
//        p.put("request", params.request);
//        p.put("node_id", params.node_id);
//        p.put("remote_node_id", params.remote_node_id);
//        p.put("algorithm", params.algorithm);
//        p.put("x", String.valueOf(params.x));
//        p.put("y", String.valueOf(params.y));
//        p.put("z", String.valueOf(params.z));
//        p.put("a", String.valueOf(params.a));
//        p.put("b", String.valueOf(params.b));
//        p.put("c", String.valueOf(params.c));
//        p.put("node_time", String.valueOf(params.node_time));
//        p.put("node_datetime", String.valueOf(params.node_datetime));
//        return p;
//    }
//}
