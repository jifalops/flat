package com.flat.loggingrequests;

import android.content.Context;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * @author Jacob Phillips (01/2015, jphilli85 at gmail)
 */
public class VolleyManager {
    private static final String TAG = VolleyManager.class.getSimpleName();

    private final RequestQueue queue;

    public VolleyManager(Context context) {
        queue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public RequestQueue getRequestQueue() {
        return queue;
    }

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        // set the default tag if tag is empty
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        queue.add(req);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        queue.add(req);
    }

    public void cancelPendingRequests(Object tag) {
        queue.cancelAll(tag);
    }
}
