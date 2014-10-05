package com.flat.logging;

import android.content.Context;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jacob Phillips (10/2014)
 */
final class SchemeRequestTest {
    static final String TAG = SchemeRequestTest.class.getSimpleName();

    Map<String, SchemeRequest> requests = new HashMap<String, SchemeRequest>();
    final RequestQueue queue;

    SchemeRequestTest(Context ctx) {
        Log.d(TAG, "Starting queue.");
        queue = Volley.newRequestQueue(ctx);
        queue.start();
    }

    void run(SchemeRequest.SchemeParams... params) {
        Log.d(TAG, "Running unit test.");
        for (SchemeRequest.SchemeParams p : params) {
            queue(p);
        }

        // Remove some requests randomly
        int cancel = (int) (Math.random() * params.length);      // remove 0 to all-1 elements
        for (int i=0; i<cancel; ++i) {
            for (Map.Entry<String, SchemeRequest> entry: requests.entrySet()) {     // in the order of the set
                cancel(entry.getKey());
                requests.remove(entry.getKey());
                break;
            }
        }
    }


    void queue(SchemeRequest.SchemeParams p) {
        if (p == null) {
            p.node_id = "00:00:00:00:00:00";
            p.algorithm = "FSPL";
            p.node_time = System.nanoTime();
            p.request_checksum = "";
            p.response_code = 200;
            p.response = "OK";
            p.response_checksum = "";
            p.db_time = System.nanoTime();
            p.x = 1.0;
            p.y = 2.0;
            p.z = 3.0;
            p.a = 90.0;
            p.b = 45.0;
            p.c = 22.5;
        }

        Response.Listener listener = null;
        Response.ErrorListener errorListener = null;

        final SchemeRequest req = new SchemeRequest(p, null, listener, errorListener);

        listener = new Response.Listener<RangingRequest.RangeParams>() {
            @Override
            public void onResponse(RangingRequest.RangeParams response) {
                Log.d(TAG, req.getTag() + ": " + response.toString());
            }
        };

        errorListener = new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, req.getTag() + " Error: " + error.getMessage());
            }
        };

        requests.put((String) req.getTag(), req);
        queue.add(req);
        Log.d(TAG, "Queued " + req.getTag());
    }

    void cancel(String tag) {
        Log.d(TAG, "Cancelling " + requests.get(tag));
        queue.cancelAll(requests.get(tag));
    }

    void cancelAll() {
        Log.d(TAG, "Cancelling all.");
        queue.cancelAll(null);
    }

    @Override
    protected void finalize() throws Throwable {
        Log.d(TAG, "Stopping queue.");
        queue.stop();
        super.finalize();
    }
}
