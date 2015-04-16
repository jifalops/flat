package com.flat.localization.node;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Jacob Phillips.
 */
public class RangeTable {
    private Map<String, Entry> table = new HashMap<String, Entry>();
    public Set<Entry> getEntries() { return (HashSet<Entry>) table.values(); }
    public Entry getEntry(String bssid) { return table.get(bssid); }
    public Entry putEntry(Entry tableEntry) { return table.put(tableEntry.bssid, tableEntry); }

    public RangeTable() {}
    public RangeTable(String jsonArray) {
        try {
            JSONArray json = new JSONArray(jsonArray);
            for (int i = 0; i < json.length(); ++i) {
                putEntry(new Entry(json.getString(i)));
            }
        } catch (JSONException ignored) {}
    }

    @Override
    public String toString() {
//        try {
            JSONArray json = new JSONArray();
            for (Entry e : getEntries()) {
                json.put(e.toString());
            }
            return json.toString();
//        } catch (JSONException ignored) {}
//        return super.toString();
    }


    public static final class Entry {
        public String bssid;
        public String ssid;
        public int rssi;
        public int freq;
        public long time;
        public float range;
        public String algorithm;
        public float rangeOverride;

        public Entry() {
        }

        public Entry(String jsonObject) {
            try {
                JSONObject json = new JSONObject(jsonObject);
                bssid = json.getString("bssid");
                ssid = json.getString("ssid");
                rssi = json.getInt("rssi");
                freq = json.getInt("freq");
                time = json.getLong("time");
                range = (float) json.getDouble("range");
                algorithm = json.getString("algorithm");
                rangeOverride = (float) json.getDouble("rangeOverride");
            } catch (JSONException ignored) {
            }
        }

        @Override
        public String toString() {
            try {
                JSONObject json = new JSONObject();
                json.put("bssid", bssid);
                json.put("ssid", ssid);
                json.put("rssi", rssi);
                json.put("freq", freq);
                json.put("time", time);
                json.put("range", range);
                json.put("algorithm", algorithm);
                json.put("rangeOverride", rangeOverride);
                return json.toString();
            } catch (JSONException ignored) {
            }
            return super.toString();
        }

        public Compact compact() {
            Compact c = new Compact();
            c.bssid = bssid;
            c.range = range;
            c.time = time;
            return c;
        }

        public static final class Compact {
            public String bssid;
            public float range;
            public long time;

            public Compact() {
            }

            public Compact(String jsonArray) {
                try {
                    JSONArray json = new JSONArray(jsonArray);
                    bssid = json.getString(0);
                    range = (float) json.getDouble(1);
                    time = json.getLong(2);
                } catch (JSONException ignored) {
                }
            }

            @Override
            public String toString() {
                try {
                    JSONArray json = new JSONArray();
                    json.put(bssid);
                    json.put(range);
                    json.put(time);
                    return json.toString();
                } catch (JSONException ignored) {
                }
                return super.toString();
            }
        }
    }
}
