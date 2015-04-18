package com.flat.aa;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
* Created by Jacob Phillips.
*/
public final class State {
    public final String id;
    public final float[] pos = new float[3];
    public final float[] angle = new float[3];
    public long time = System.currentTimeMillis();
    public String algorithm;
    State(String id) { this.id = id; }

    static State from(String jsonObject) throws JSONException {
        JSONObject json = new JSONObject(jsonObject);
        State s = new State(json.getString("id"));
        JSONArray array = json.getJSONArray("pos");
        s.pos[0] = (float) array.getDouble(0);
        s.pos[1] = (float) array.getDouble(1);
        s.pos[2] = (float) array.getDouble(2);
        array = json.getJSONArray("angle");
        s.angle[0] = (float) array.getDouble(0);
        s.angle[1] = (float) array.getDouble(1);
        s.angle[2] = (float) array.getDouble(2);
        s.time = json.getLong("time");
        s.algorithm = json.getString("algorithm");
        return s;
    }

    @Override
    public String toString() {
        try {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("pos", new JSONArray(pos));
            json.put("angle", new JSONArray(angle));
            json.put("time", time);
            json.put("algorithm", algorithm);
            return json.toString();
        } catch (JSONException ignored) {}
        return super.toString();
    }
}
