package com.flat.app;

import android.app.Activity;
import android.app.ListFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.flat.R;
import com.flat.localization.Model;
import com.flat.localization.Node;
import com.flat.localization.signal.Signal;
import com.flat.localization.signal.rangingandprocessing.SignalInterpreter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class RangeTableActivity extends Activity {
    private static final String TAG = RangeTableActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new RangeTableFragment()).commit();
    }

    private static class Holder {
        ImageView dot;
        TextView name;
        TextView desc;
        TextView estimate;
        TextView actual;
    }

    private static void blink(final ImageView dot) {
        if (dot == null) return;
        dot.setImageResource(R.drawable.green_dot);
        dot.postDelayed(new Runnable() {
            @Override
            public void run() {
                dot.setImageResource(R.drawable.dot);
            }
        }, 400);
    }

    private static  View getViewByPosition(int pos, ListView listView) {
        if (listView == null) return null;
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }
    


    public static class RangeTableFragment extends ListFragment {
        Model model;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            model = Model.getInstance();
            setListAdapter(new RangeTableAdapter());
        }

        private final Node.NodeListener nodeListener = new Node.NodeListener() {
            @Override
            public void onRangePending(Node n, Node.Range r) {

            }

            @Override
            public void onStatePending(Node n, Node.State s) {

            }

            @Override
            public void onRangeChanged(Node n, Node.Range r) {
                List<Node> nodes = model.getNodesCopy();
                for (int i=0; i<nodes.size(); ++i) {
                    if (n == nodes.get(i)) {
                        View container = getViewByPosition(i, getListView());
                        if (container != null) {
                            blink((ImageView) container.findViewById(R.id.activityDot));
                            ((TextView) container.findViewById(R.id.estimate)).setText(n.getRange().dist + "");
                            ((TextView) container.findViewById(R.id.actual)).setText(n.getRange().actual + "");
                        }
                        break;
                    }
                }
            }

            @Override
            public void onStateChanged(Node n, Node.State s) {

            }
        };

        private class RangeTableAdapter extends ArrayAdapter<Node> {
            public RangeTableAdapter() {
                super(getActivity(), R.layout.range_table_item, model.getNodesCopy());
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Holder holder;
                if (convertView == null) {
                    LayoutInflater inflater = getActivity().getLayoutInflater();
                    convertView = inflater.inflate(R.layout.range_table_item, parent, false);

                    holder = new Holder();
                    holder.dot = (ImageView) convertView.findViewById(R.id.activityDot);
                    holder.name = (TextView) convertView.findViewById(R.id.name);
                    holder.desc = (TextView) convertView.findViewById(R.id.desc);
                    holder.estimate = (TextView) convertView.findViewById(R.id.estimate);
                    holder.actual = (TextView) convertView.findViewById(R.id.actual);
                    convertView.setTag(holder);
                } else {
                    holder = (Holder) convertView.getTag();
                }

                final Node node = model.getNode(position);
                //signal.registerListener(signalListener);

                final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

                String info = sharedPrefs.getString(node.getId(), "");
                JSONObject json;
                try {
                    json = new JSONObject(info);
                    // TODO use node.setName() in AppController and read from sharedprefs (also change with prefs)
                    holder.name.setText(json.getString("name"));
                } catch (JSONException e) {
                    holder.name.setText(node.getName());
                }

                holder.desc.setText(node.getId());
                holder.estimate.setText(node.getRange().dist + "");
                holder.actual.setText(node.getRange().actual + "");

                //node.registerListener(nodeListener);

                return convertView;
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            for (Node n : model.getNodesCopy()) {
                n.unregisterListener(nodeListener);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            for (Node n : model.getNodesCopy()) {
                n.registerListener(nodeListener);
            }
        }

    }


}