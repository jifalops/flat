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
import com.flat.localization.coordinatesystem.LocationAlgorithm;
import com.flat.localization.signal.Signal;
import com.flat.localization.signal.rangingandprocessing.SignalInterpreter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class ActiveItemListActivity extends Activity {
    private static final String TAG = ActiveItemListActivity.class.getSimpleName();
    public static final int SIGNAL_FRAGMENT = 1;
    public static final int ALG_FRAGMENT = 2;
    public static final String EXTRA_ITEM = "extra_item";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        switch(getIntent().getIntExtra(EXTRA_ITEM, SIGNAL_FRAGMENT)) {
            case SIGNAL_FRAGMENT:
                getFragmentManager().beginTransaction().replace(android.R.id.content, new SignalFragment()).commit();
                break;
            case ALG_FRAGMENT:
                getFragmentManager().beginTransaction().replace(android.R.id.content, new AlgorithmFragment()).commit();
                break;
        }
    }

    private static class ActiveToggleItemHolder {
        ImageView dot;
        TextView name;
        TextView desc;
        Switch enabled;
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
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }
    
    private static ActiveToggleItemHolder getHolder(View v, ViewGroup parent, Activity a) {
        ActiveToggleItemHolder holder;
        if (v == null) {
            LayoutInflater inflater = a.getLayoutInflater();
            v = inflater.inflate(R.layout.active_item_toggle, parent, false);

            holder = new ActiveToggleItemHolder();
            holder.dot = (ImageView) v.findViewById(R.id.activityDot);
            holder.name = (TextView) v.findViewById(R.id.name);
            holder.desc = (TextView) v.findViewById(R.id.desc);
            holder.enabled = (Switch) v.findViewById(R.id.enabled);
            holder.count = (TextView) v.findViewById(R.id.count);
            v.setTag(holder);
        } else {
            holder = (ActiveToggleItemHolder) v.getTag();
        }
        return holder;
    }






    public static class SignalFragment extends ListFragment {
        Model model;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            model = Model.getInstance();
            setListAdapter(new SignalAdapter());
        }

        private final Signal.SignalListener signalListener = new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                Signal[] signals = model.getSignals();
                for (int i=0; i<signals.length; ++i) {
                    if (signal == signals[i]) {
                        View container = getViewByPosition(i, getListView());
                        if (container != null) {
                            blink((ImageView) container.findViewById(R.id.activityDot));
                            ((TextView) container.findViewById(R.id.count)).setText(signal.getChangeCount() + "");
                        }
                        break;
                    }
                }
            }
        };

        private class SignalAdapter extends ArrayAdapter<Signal> {
            public SignalAdapter() {
                super(getActivity(), R.layout.active_item_toggle, model.getSignals());
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ActiveToggleItemHolder holder = getHolder(convertView, parent, getActivity());

                final Signal signal = model.getSignals()[position];
                signal.registerListener(signalListener);

                holder.name.setText(signal.getName());

                List<String> processors = new ArrayList<String>();
                for (SignalInterpreter rp : model.getRangingProcessors(signal)) {
                    processors.add(rp.getName());
                }
                holder.desc.setText(TextUtils.join(", ", processors));
                holder.enabled.setChecked(signal.isEnabled());

                holder.enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        sharedPrefs.edit().putBoolean(signal.getName(), isChecked).commit();
                        if (isChecked) {
                            signal.enable(getActivity());
                        } else {
                            signal.disable(getActivity());
                        }
                    }
                });

                return convertView;
            }
        }
    }





    public static class AlgorithmFragment extends ListFragment {
        Model model;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            model = Model.getInstance();
            setListAdapter(new AlgorithmAdapter());
        }

        private final LocationAlgorithm.AlgorithmListener algListener = new LocationAlgorithm.AlgorithmListener() {
            @Override
            public void onApplied(LocationAlgorithm la, Node target, List<Node> references) {
                LocationAlgorithm[] algs = model.getAlgorithms();
                for (int i=0; i<algs.length; ++i) {
                    if (la == algs[i]) {
                        View container = getViewByPosition(i, getListView());
                        if (container != null) {
                            blink((ImageView) container.findViewById(R.id.activityDot));
                            ((TextView) container.findViewById(R.id.desc)).setText("Nodes: " + references.size());
                            ((TextView) container.findViewById(R.id.count)).setText(la.getUseCount() + "");
                        }
                        break;
                    }
                }
            }
        };


        private class AlgorithmAdapter extends ArrayAdapter<LocationAlgorithm> {
            public AlgorithmAdapter() {
                super(getActivity(), R.layout.active_item_toggle, model.getAlgorithms());
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ActiveToggleItemHolder holder = getHolder(convertView, parent, getActivity());

                final LocationAlgorithm alg = model.getAlgorithms()[position];
                alg.registerListener(algListener);

                holder.name.setText(alg.getName());
                holder.enabled.setChecked(alg.isEnabled());

                holder.enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        sharedPrefs.edit().putBoolean(alg.getName(), isChecked).commit();

                        alg.setEnabled(isChecked);
                    }
                });

                return convertView;
            }
        }

    }


















//    private static final Node.NodeListener nodeListener = new Node.NodeListener() {
//        @Override
//        public void onRangePending(Node n, Node.Range r) {
//
//        }
//
//        @Override
//        public void onStatePending(Node n, Node.State s) {
//
//        }
//
//        @Override
//        public void onRangeChanged(Node n, Node.Range r) {
//
//        }
//
//        @Override
//        public void onStateChanged(Node n, Node.State s) {
//
//        }
//    };
//
//    private static final Model.ModelListener modelListener = new Model.ModelListener() {
//        @Override
//        public void onNodeAdded(Node n) {
//            Log.i(TAG, "Node Added: " + n.getId());
//        }
//    };





}