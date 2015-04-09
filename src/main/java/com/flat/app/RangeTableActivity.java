package com.flat.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.flat.R;
import com.flat.localization.LocalizationManager2;
import com.flat.localization.NodeManager;
import com.flat.localization.node.Node;
import com.flat.localization.node.NodeRange;
import com.flat.localization.node.NodeState;
import com.flat.localization.node.RemoteNode;

import java.util.List;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class RangeTableActivity extends Activity {
    private static final String TAG = RangeTableActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Ranges (this device)");
        getFragmentManager().beginTransaction().replace(android.R.id.content, new RangeTableFragment()).commit();
    }

    private static class Holder {
        ImageView dot;
        TextView title;
        TextView summary;
        TextView dist;
        TextView count;
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
        ColorStateList defaultColor;
        NodeManager nodeManager;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            nodeManager = LocalizationManager2.getInstance(getActivity()).getNodeManager();
            setListAdapter(new RangeTableAdapter());
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
//            setListAdapter(null);
//            getListView().addHeaderView(getActivity().getLayoutInflater().inflate(R.layout.range_table_header, null));
        }

        private final NodeManager.NodeManagerListener nodeManagerListener = new NodeManager.NodeManagerListener() {
            @Override
            public void onNodeAdded(RemoteNode n) {
                // TODO still doesnt redraw the list
                ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
                getListView().invalidate();
            }

            @Override
            public void onRangePending(RemoteNode n, NodeRange r) {

            }

            @Override
            public void onStatePending(Node n, NodeState s) {

            }

            @Override
            public void onRangeChanged(RemoteNode n, NodeRange r) {
                List<RemoteNode> nodes = nodeManager.getNodes();
                for (int i=0; i<nodes.size(); ++i) {
                    if (n == nodes.get(i)) {
                        View container = getViewByPosition(i, getListView());
                        if (container != null) {
                            blink((ImageView) container.findViewById(R.id.activityDot));
                            showRange((TextView) container.findViewById(R.id.dist), n);
                            ((TextView) container.findViewById(R.id.count)).setText(n.getRangeHistorySize() + "");
                        }
                        break;
                    }
                }
            }

            @Override
            public void onStateChanged(Node n, NodeState s) {

            }
        };

        private void showRange(TextView tv, RemoteNode n) {
            if (tv == null || n == null) return;
            float f;
            if (n.getRange().rangeOverride > 0) {
                f = n.getRange().rangeOverride;
                tv.setTextColor(Color.RED);
            } else {
                f = n.getRange().range;
                if (defaultColor != null) tv.setTextColor(defaultColor);
            }
            tv.setText(round(f) + "m");
        }

        private float round(float f) {
            return ((int)(f*100))/100f;
        }

        private class RangeTableAdapter extends ArrayAdapter<RemoteNode> {
            public RangeTableAdapter() {
                super(getActivity(), R.layout.range_table_item, nodeManager.getNodes());
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final Holder holder;
                if (convertView == null) {
                    LayoutInflater inflater = getActivity().getLayoutInflater();
                    convertView = inflater.inflate(R.layout.range_table_item, parent, false);

                    holder = new Holder();
                    holder.dot = (ImageView) convertView.findViewById(R.id.activityDot);
                    holder.title = (TextView) convertView.findViewById(R.id.name);
                    holder.summary = (TextView) convertView.findViewById(R.id.desc);
                    holder.dist = (TextView) convertView.findViewById(R.id.dist);
                    holder.count = (TextView) convertView.findViewById(R.id.count);
                    convertView.setTag(holder);
                } else {
                    holder = (Holder) convertView.getTag();
                }

                if (defaultColor == null) defaultColor = holder.dist.getTextColors();

                final RemoteNode node = nodeManager.getNode(position);


                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

                node.readPrefs(prefs);

                holder.title.setText(node.getName());

                holder.summary.setText(node.getId());
                showRange(holder.dist, node);

                holder.count.setText(node.getRangeHistorySize() + "");

                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder b = new AlertDialog.Builder(getContext());
                        b.setMessage("Name for " + node.getId());

                        final EditText input = new EditText(getContext());
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        input.setLayoutParams(lp);

                        input.setText(node.getName());

                        b.setView(input);

                        b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (input.length() > 0) {
                                    holder.title.setText(input.getText());
                                    node.setName(input.getText().toString());
                                    node.savePrefs(prefs);
                                }
                            }
                        });

                        b.show();
                    }
                });

                convertView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        AlertDialog.Builder b = new AlertDialog.Builder(getContext());
                        b.setMessage("Range override for " + node.getId() + "\n(0 to clear override)");

                        final EditText input = new EditText(getContext());
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        input.setLayoutParams(lp);

                        float f = node.getActualRangeOverride() > 0 ? node.getActualRangeOverride() : node.getRange().range;
                        input.setText(round(f) + "");

                        b.setView(input);

                        b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    node.setActualRangeOverride(Float.valueOf(input.getText().toString()));
                                } catch (Throwable ignored) {
                                }
                            }
                        });

                        b.show();
                        return true;
                    }
                });

                return convertView;
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            nodeManager.unregisterListener(nodeManagerListener);
        }

        @Override
        public void onResume() {
            super.onResume();
            nodeManager.registerListener(nodeManagerListener);
        }

    }


}