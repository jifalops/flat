package com.flat.app;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.flat.localization.Controller;
import com.flat.localization.Model;
import com.flat.localization.Node;
import com.flat.localization.ranging.RangingProcessor;
import com.flat.localization.scheme.LocationAlgorithm;
import com.flat.localization.signal.Signal;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class LoggingActivity extends Activity {
    private static final String TAG = LoggingActivity.class.getSimpleName();

    Model model;
    Controller controller;
    ListView signalsView;
    ListView algsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logging_activity);


        model = Model.getInstance();
        controller = Controller.getInstance(this);

        signalsView = (ListView) findViewById(R.id.signalList);
        algsView = (ListView) findViewById(R.id.algorithmList);

        signalsView.setAdapter(new SignalAdapter());
        algsView.setAdapter(new AlgorithmAdapter());
    }

    private class SignalAdapter extends ArrayAdapter<Signal> {
        public SignalAdapter() {
            super(LoggingActivity.this, R.layout.signal_item, model.getSignals());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            SignalHolder holder = null;

            final Signal signal = model.getSignals()[position];
            signal.registerListener(signalListener);

            if (row == null) {
                LayoutInflater inflater = (LoggingActivity.this).getLayoutInflater();
                row = inflater.inflate(R.layout.signal_item, parent, false);

                holder = new SignalHolder();
                holder.dot = (ImageView) row.findViewById(R.id.dotView);
                holder.name = (TextView) row.findViewById(R.id.name);
                holder.processors = (TextView) row.findViewById(R.id.line2);
                holder.enabled = (Switch) row.findViewById(R.id.enabled);
                row.setTag(holder);
            } else {
                holder = (SignalHolder) row.getTag();
            }


            holder.name.setText(signal.getName());

            List<String> processors = new ArrayList<String>();
            for (RangingProcessor rp : model.getRangingProcessors(signal)) {
                processors.add(rp.getName());
            }
            holder.processors.setText(TextUtils.join(", ", processors));
            holder.enabled.setChecked(signal.isEnabled());

            holder.enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        signal.enable(LoggingActivity.this);
                    } else {
                        signal.disable(LoggingActivity.this);
                    }
                }
            });

            return row;
        }


    }
    private static class SignalHolder {
        ImageView dot;
        TextView name;
        TextView processors;
        Switch enabled;
    }




    private class AlgorithmAdapter extends ArrayAdapter<LocationAlgorithm> {
        public AlgorithmAdapter() {
            super(LoggingActivity.this, R.layout.algorithm_item, model.getAlgorithms());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            AlgorithmHolder holder = null;

            final LocationAlgorithm alg = model.getAlgorithms()[position];
            alg.registerListener(algListener);

            if (row == null) {
                LayoutInflater inflater = (LoggingActivity.this).getLayoutInflater();
                row = inflater.inflate(R.layout.algorithm_item, parent, false);

                holder = new AlgorithmHolder();
                holder.dot = (ImageView) row.findViewById(R.id.dotView);
                holder.name = (TextView) row.findViewById(R.id.name);
                holder.line2 = (TextView) row.findViewById(R.id.line2);
                holder.enabled = (Switch) row.findViewById(R.id.enabled);
                row.setTag(holder);
            } else {
                holder = (AlgorithmHolder) row.getTag();
            }



            holder.name.setText(alg.getName());
            holder.enabled.setChecked(alg.isEnabled());

            holder.enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    alg.setEnabled(isChecked);
                }
            });



            return row;
        }


    }
    private static class AlgorithmHolder {
        ImageView dot;
        TextView name;
        TextView line2;
        Switch enabled;
    }


    private void blink(final ImageView dot) {
        if (dot == null) return;
        dot.setImageResource(R.drawable.green_dot);
        dot.postDelayed(new Runnable() {
            @Override
            public void run() {
                dot.setImageResource(R.drawable.dot);
            }
        }, 400);
    }

    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
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

        }

        @Override
        public void onStateChanged(Node n, Node.State s) {

        }
    };

    private final Model.ModelListener modelListener = new Model.ModelListener() {
        @Override
        public void onNodeAdded(Node n) {
            Log.i(TAG, "Node Added: " + n.getId());
        }
    };

    private final LocationAlgorithm.AlgorithmListener algListener = new LocationAlgorithm.AlgorithmListener() {
        @Override
        public void onApplied(LocationAlgorithm la, Node target, List<Node> references) {
            LocationAlgorithm[] algs = model.getAlgorithms();
            for (int i=0; i<algs.length; ++i) {
                if (la == algs[i]) {
                    View container = getViewByPosition(i, algsView);
                    if (container != null) {
                        blink((ImageView) container.findViewById(R.id.dotView));
                        ((TextView) container.findViewById(R.id.line2)).setText("Nodes: " + references.size());
                    }
                    break;
                }
            }
        }
    };


    private final Signal.SignalListener signalListener = new Signal.SignalListener() {
        @Override
        public void onChange(Signal signal, int eventType) {
            Signal[] signals = model.getSignals();
            for (int i=0; i<signals.length; ++i) {
                if (signal == signals[i]) {
                    View container = getViewByPosition(i, signalsView);
                    if (container != null) {
                        blink((ImageView) container.findViewById(R.id.dotView));
                    }
                    break;
                }
            }
        }
    };
}