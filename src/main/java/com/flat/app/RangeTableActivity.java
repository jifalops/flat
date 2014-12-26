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
import com.flat.localization.Model;
import com.flat.localization.Node;

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
        Model model;
        ColorStateList colors;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            model = Model.getInstance();
            setListAdapter(new RangeTableAdapter());
        }

        private final Model.ModelListener modelListener = new Model.ModelListener() {
            @Override
            public void onNodeAdded(Node n) {
                ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
            }
        };

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
                            showRange((TextView) container.findViewById(R.id.dist), n);
                            ((TextView) container.findViewById(R.id.count)).setText(n.getRangeHistorySize() + "");
                        }
                        break;
                    }
                }
            }

            @Override
            public void onStateChanged(Node n, Node.State s) {

            }
        };

        private void showRange(TextView tv, Node n) {
            float f;
            if (n.getRange().actual > 0) {
                f = n.getRange().actual;
                tv.setTextColor(Color.RED);
            } else {
                f = n.getRange().dist;
                if (colors != null) tv.setTextColor(colors);
            }
            f = ((int)(f*100))/100f;
            tv.setText(f + "m");
        }

        private class RangeTableAdapter extends ArrayAdapter<Node> {
            public RangeTableAdapter() {
                super(getActivity(), R.layout.range_table_item, model.getNodesCopy());
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

                if (colors == null) colors = holder.dist.getTextColors();

                final Node node = model.getNode(position);


                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

                node.readPrefs(prefs);

                holder.title.setText(node.getName());

                holder.summary.setText(node.getId());
                showRange(holder.dist, node);

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

                        input.setText("" + (node.getActualRangeOverride() > 0 ? node.getActualRangeOverride() : node.getRange().dist));

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
            model.unregisterListener(modelListener);
            for (Node n : model.getNodesCopy()) {
                n.unregisterListener(nodeListener);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            model.registerListener(modelListener);
            for (Node n : model.getNodesCopy()) {
                n.registerListener(nodeListener);
            }
        }

    }


}