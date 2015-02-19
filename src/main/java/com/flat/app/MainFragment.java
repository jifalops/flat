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
import com.flat.localization.Node;
import com.flat.localization.NodeManager;
import com.flat.localization.algorithm.Algorithm;
import com.flat.localization.signal.Signal;
import com.flat.util.PersistentIntentService;
import com.flat.util.PersistentIntentServiceFragment;

import java.util.List;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class MainFragment extends PersistentIntentServiceFragment {
    private AppService mService;

    private ImageView signalDot, algDot, rangeDot;
    private TextView signalSummary, algSummary, rangeSummary;
    private boolean shouldEnableService;

    @Override
    public void onServiceConnected(PersistentIntentService service) {
        mService = (AppService) service;
        if (shouldEnableService) {
            service.setEnabled(true);
            onServiceEnabled(true);
            shouldEnableService = false;
        }
    }

    @Override
    protected Class<? extends PersistentIntentService> getServiceClass() {
        return AppService.class;
    }

    @Override
    protected void onServiceEnabled(boolean enabled) {
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
        AppController.getInstance().signalManager.registerListener(signalListener);
        AppController.getInstance().algorithmManager.registerListener(algListener);
        AppController.getInstance().nodeManager.registerListener(nodeManagerListener);
    }

    private void unregisterListeners() {
        AppController.getInstance().signalManager.unregisterListener(signalListener);
        AppController.getInstance().algorithmManager.unregisterListener(algListener);
        AppController.getInstance().nodeManager.unregisterListener(nodeManagerListener);
    }

    private void updateSummaries() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        int count = 0;
        for (Signal s : AppController.getInstance().signalManager.getSignals()) {
            if (prefs.getBoolean(s.getName(), false)) ++count;
        }
        signalSummary.setText(count + " enabled");

        count = 0;
        for (Algorithm la : AppController.getInstance().algorithmManager.getAlgorithms()) {
            if (prefs.getBoolean(la.getName(), false)) ++count;
        }
        algSummary.setText(count + " enabled");

        rangeSummary.setText(AppController.getInstance().nodeManager.getNodeCount() + " nodes in range (this app instance)");
    }



    private final Signal.SignalListener signalListener = new Signal.SignalListener() {
        @Override
        public void onChange(Signal signal, int eventType) {
            blink(signalDot);
        }
    };

    private final Algorithm.AlgorithmListener algListener = new Algorithm.AlgorithmListener() {
        @Override
        public void onApplied(Algorithm la, Node target, List<Node> references) {
            blink(algDot);
        }
    };

    private final NodeManager.NodeManagerListener nodeManagerListener = new NodeManager.NodeManagerListener() {
        @Override
        public void onNodeAdded(Node n) {
            rangeSummary.setText(AppController.getInstance().nodeManager.getNodeCount() + " nodes in range (this app instance)");
        }

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
        shouldEnableService = true;
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
                Intent i = new Intent(getActivity(), BlinkingListActivity.class);
                i.putExtra(BlinkingListActivity.EXTRA_FRAGMENT, BlinkingListActivity.SIGNAL_FRAGMENT);
                startActivity(i);
            }
        });

        layout.findViewById(R.id.algsCont).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), BlinkingListActivity.class);
                i.putExtra(BlinkingListActivity.EXTRA_FRAGMENT, BlinkingListActivity.ALG_FRAGMENT);
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
