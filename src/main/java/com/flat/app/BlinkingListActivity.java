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
import com.flat.localization.AlgorithmManager;
import com.flat.localization.LocMan;
import com.flat.localization.SignalManager;
import com.flat.localization.algorithms.Algorithm;
import com.flat.localization.node.Node;
import com.flat.localization.signals.Signal;
import com.flat.localization.signals.interpreters.SignalInterpreter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class BlinkingListActivity extends Activity {
    private static final String TAG = BlinkingListActivity.class.getSimpleName();
    public static final int SIGNAL_FRAGMENT = 1;
    public static final int ALG_FRAGMENT = 2;
    public static final String EXTRA_FRAGMENT = "extra_item";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        switch(getIntent().getIntExtra(EXTRA_FRAGMENT, SIGNAL_FRAGMENT)) {
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
    


    public static class SignalFragment extends ListFragment {
        SignalManager manager;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            manager = LocMan.getInstance(getActivity()).getSignalManager();
            setListAdapter(new SignalAdapter());
        }

        private final Signal.SignalListener signalListener = new Signal.SignalListener() {
            @Override
            public void onChange(Signal signal, int eventType) {
                Signal[] signals = manager.getSignals();
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
                super(getActivity(), R.layout.active_item_toggle, manager.getSignals());
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ActiveToggleItemHolder holder;
                if (convertView == null) {
                    LayoutInflater inflater = getActivity().getLayoutInflater();
                    convertView = inflater.inflate(R.layout.active_item_toggle, parent, false);

                    holder = new ActiveToggleItemHolder();
                    holder.dot = (ImageView) convertView.findViewById(R.id.activityDot);
                    holder.name = (TextView) convertView.findViewById(R.id.name);
                    holder.desc = (TextView) convertView.findViewById(R.id.desc);
                    holder.enabled = (Switch) convertView.findViewById(R.id.enabled);
                    holder.count = (TextView) convertView.findViewById(R.id.count);
                    convertView.setTag(holder);
                } else {
                    holder = (ActiveToggleItemHolder) convertView.getTag();
                }

                final Signal signal = manager.getSignals()[position];
                //signal.registerListener(signalListener);

                holder.name.setText(signal.getName());

                List<String> processors = new ArrayList<String>();
                for (SignalInterpreter rp : manager.getInterpreters(signal)) {
                    processors.add(rp.getName());
                }
                holder.desc.setText(TextUtils.join(", ", processors));

                final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

                holder.enabled.setChecked(sharedPrefs.getBoolean(signal.getName(), false));

                holder.enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                        sharedPrefs.edit().putBoolean(signal.getName(), isChecked).apply();
                        if (isChecked) {
                            if (LocMan.getInstance(getActivity()).isEnabled()) {
                                signal.enable(getActivity());
                            }
                        } else {
                            signal.disable(getActivity());
                        }
                    }
                });

                holder.count.setText(signal.getChangeCount() + "");

                return convertView;
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            manager.unregisterListener(signalListener);
        }

        @Override
        public void onResume() {
            super.onResume();
            manager.registerListener(signalListener);
        }
    }





    public static class AlgorithmFragment extends ListFragment {
        AlgorithmManager manager;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            manager = LocMan.getInstance(getActivity()).getAlgorithmManager();
            setListAdapter(new AlgorithmAdapter());
        }

        private final Algorithm.AlgorithmListener algListener = new Algorithm.AlgorithmListener() {
            @Override
            public void onApplied(Algorithm la, Node target, List<Node> references) {
                Algorithm[] algs = manager.getAlgorithms();
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


        private class AlgorithmAdapter extends ArrayAdapter<Algorithm> {
            public AlgorithmAdapter() {
                super(getActivity(), R.layout.active_item_toggle, manager.getAlgorithms());
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ActiveToggleItemHolder holder;
                if (convertView == null) {
                    LayoutInflater inflater = getActivity().getLayoutInflater();
                    convertView = inflater.inflate(R.layout.active_item_toggle, parent, false);

                    holder = new ActiveToggleItemHolder();
                    holder.dot = (ImageView) convertView.findViewById(R.id.activityDot);
                    holder.name = (TextView) convertView.findViewById(R.id.name);
                    holder.desc = (TextView) convertView.findViewById(R.id.desc);
                    holder.enabled = (Switch) convertView.findViewById(R.id.enabled);
                    holder.count = (TextView) convertView.findViewById(R.id.count);
                    convertView.setTag(holder);
                } else {
                    holder = (ActiveToggleItemHolder) convertView.getTag();
                }

                final Algorithm alg = manager.getAlgorithms()[position];
                //alg.registerListener(algListener);

                holder.name.setText(alg.getName());

                final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

                holder.enabled.setChecked(sharedPrefs.getBoolean(alg.getName(), false));

                holder.enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        sharedPrefs.edit().putBoolean(alg.getName(), isChecked).apply();

                        if (isChecked) {
                            if (LocMan.getInstance(getActivity()).isEnabled()) {
                                alg.setEnabled(true);
                            }
                        } else {
                            alg.setEnabled(false);
                        }
                    }
                });

                holder.count.setText(alg.getUseCount() + "");

                return convertView;
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            manager.unregisterListener(algListener);
        }

        @Override
        public void onResume() {
            super.onResume();
            manager.registerListener(algListener);
        }
    }
}