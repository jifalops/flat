//package com.flat.loggingrequests;
//
//import android.content.Context;
//import android.util.Log;
//
//import com.android.volley.RequestQueue;
//import com.android.volley.Response;
//import com.android.volley.VolleyError;
//import com.android.volley.VolleyLog;
//import com.android.volley.toolbox.Volley;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * Created by Jacob Phillips (10/2014)
// */
//public final class RangingRequestTest {
//    static final String TAG = RangingRequestTest.class.getSimpleName();
//
//    Map<String, RangingRequest> requests = new HashMap<String, RangingRequest>();
//    final RequestQueue queue;
//
//    public RangingRequestTest(Context ctx) {
//        Log.d(TAG, "Starting queue.");
//        queue = Volley.newRequestQueue(ctx);
//        queue.start();
//    }
//
//    public void run(RangingRequest.RangeParams... params) {
//        Log.d(TAG, "Running unit test.");
//        for (RangingRequest.RangeParams p : params) {
//            queue(p);
//        }
//
//        // Remove some requests randomly
//        int cancel = (int) (Math.random() * params.length);      // remove 0 to all-1 elements
//        for (int i=0; i<cancel; ++i) {
//            for (Map.Entry<String, RangingRequest> entry: requests.entrySet()) {     // in the order of the set
//                cancel(entry.getKey());
//                requests.remove(entry.getKey());
//                break;
//            }
//        }
//    }
//    public void run() { run(null); }
//
//
//    void queue(RangingRequest.RangeParams p) {
//        if (p == null) {
//            p.node_id = "00:00:00:00:00:00";
//            p.actual = 0.1f;
//            p.estimate = 0.2f;
//            p.algorithm = "FSPL";
//            p.node_time = System.nanoTime();
//            p.remote_node_id = "00:00:00:00:00:11";
//            p.request_checksum = "";
//            p.response_code = 200;
//            p.response = "OK";
//            p.response_checksum = "";
//        }
//
//        Response.Listener listener = null;
//        Response.ErrorListener errorListener = null;
//
//        final RangingRequest req = new RangingRequest(p, null, listener, errorListener);
//
//        listener = new Response.Listener<RangingRequest.RangeParams>() {
//            @Override
//            public void onResponse(RangingRequest.RangeParams response) {
//                Log.d(TAG, req.getTag() + ": " + response.toString());
//            }
//        };
//
//        errorListener = new Response.ErrorListener() {
//
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                VolleyLog.d(TAG, req.getTag() + " Error: " + error.getMessage());
//            }
//        };
//
//        requests.put((String) req.getTag(), req);
//        queue.add(req);
//        Log.d(TAG, "Queued " + req.getTag());
//    }
//
//    public void cancel(String tag) {
//        Log.d(TAG, "Cancelling " + requests.get(tag));
//        queue.cancelAll(requests.get(tag));
//    }
//
//    public void cancelAll() {
//        Log.d(TAG, "Cancelling all.");
//        queue.cancelAll(null);
//    }
//
//    @Override
//    protected void finalize() throws Throwable {
//        Log.d(TAG, "Stopping queue.");
//        queue.stop();
//        super.finalize();
//    }
//}
