package com.flat.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.flat.R;
import com.flat.data.Model;
import com.flat.localization.node.Node;
import com.flat.localization.algorithm.LocationAlgorithm;
import com.flat.localization.signal.Signal;
import com.flat.util.app.PersistentIntentService;
import com.flat.util.app.PersistentIntentServiceFragment;

import java.util.List;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class AppServiceFragment extends PersistentIntentServiceFragment {
    private AppService mService;

    private ImageView signalDot, algDot, rangeDot;
    private TextView signalSummary, algSummary, rangeSummary;

    Model model;


    @Override
    public void onServiceConnected(PersistentIntentService service) {
        mService = (AppService) service;
    }

    @Override
    protected Class<? extends PersistentIntentService> getServiceClass() {
        return AppService.class;
    }

    @Override
    protected void onServiceEnabled(boolean enabled) {
        if (!isBound()) return;
        setPersistent(enabled);
        AppController.getInstance().setEnabled(enabled);
    }



    @Override
    public void onResume() {
        super.onResume();
        registerListeners();
        updateSummaries();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterListeners();
    }

    private void registerListeners() {
        for (Signal s : model.getSignals()) {
            s.registerListener(signalListener);
        }

        for (LocationAlgorithm la: model.getAlgorithms()) {
            la.registerListener(algListener);
        }

        for (Node n : model.getNodesCopy()) {
            n.registerListener(nodeListener);
        }

        model.registerListener(modelListener);

    }

    private void unregisterListeners() {
        for (Signal s : model.getSignals()) {
            s.unregisterListener(signalListener);
        }

        for (LocationAlgorithm la: model.getAlgorithms()) {
            la.unregisterListener(algListener);
        }

        for (Node n : model.getNodesCopy()) {
            n.unregisterListener(nodeListener);
        }

        model.unregisterListener(modelListener);
    }

    private void updateSummaries() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        int count = 0;
        for (Signal s : Model.getInstance().getSignals()) {
            if (prefs.getBoolean(s.getName(), false)) ++count;
        }
        signalSummary.setText(count + " enabled");

        count = 0;
        for (LocationAlgorithm la : Model.getInstance().getAlgorithms()) {
            if (prefs.getBoolean(la.getName(), false)) ++count;
        }
        algSummary.setText(count + " enabled");

        rangeSummary.setText(model.getNodeCount() + " nodes in range (this app instance)");
    }



    private final Signal.SignalListener signalListener = new Signal.SignalListener() {
        @Override
        public void onChange(Signal signal, int eventType) {
            blink(signalDot);
        }
    };

    private final LocationAlgorithm.AlgorithmListener algListener = new LocationAlgorithm.AlgorithmListener() {
        @Override
        public void onApplied(LocationAlgorithm la, Node target, List<Node> references) {
            blink(algDot);
        }
    };

    private final Model.ModelListener modelListener = new Model.ModelListener() {
        @Override
        public void onNodeAdded(Node n) {
            n.registerListener(nodeListener);
            rangeSummary.setText(model.getNodeCount() + " nodes in range (this app instance)");
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
            blink(rangeDot);
        }

        @Override
        public void onStateChanged(Node n, Node.State s) {

        }
    };

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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showPersistControl(false);
        model = Model.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.real_main, container, false);

        signalDot = (ImageView) layout.findViewById(R.id.signalDot);
        algDot = (ImageView) layout.findViewById(R.id.algDot);
        rangeDot = (ImageView) layout.findViewById(R.id.rangetableDot);

        signalSummary = (TextView) layout.findViewById(R.id.signalSummary);
        algSummary = (TextView) layout.findViewById(R.id.algSummary);
        rangeSummary = (TextView) layout.findViewById(R.id.rangetableSummary);


        layout.findViewById(R.id.signalsCont).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), ActiveItemListActivity.class);
                i.putExtra(ActiveItemListActivity.EXTRA_FRAGMENT, ActiveItemListActivity.SIGNAL_FRAGMENT);
                startActivity(i);
            }
        });

        layout.findViewById(R.id.algsCont).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), ActiveItemListActivity.class);
                i.putExtra(ActiveItemListActivity.EXTRA_FRAGMENT, ActiveItemListActivity.ALG_FRAGMENT);
                startActivity(i);
            }
        });

        layout.findViewById(R.id.rangesCont).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), RangeTableActivity.class));
            }
        });

        layout.findViewById(R.id.coordsCont).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent i = new Intent(getActivity(), ActiveItemListActivity.class);
//                i.putExtra(ActiveItemListActivity.EXTRA_FRAGMENT, ActiveItemListActivity.ALG_FRAGMENT);
//                startActivity(i);
            }
        });

        return layout;
    }
}
