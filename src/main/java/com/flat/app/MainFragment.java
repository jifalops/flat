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

import com.flat.AppController;
import com.flat.R;
import com.flat.localization.LocMan;
import com.flat.localization.node.Node;
import com.flat.localization.NodeManager;
import com.flat.localization.algorithms.Algorithm;
import com.flat.localization.node.NodeRange;
import com.flat.localization.node.NodeState;
import com.flat.localization.node.RemoteNode;
import com.flat.localization.signals.Signal;
import com.flat.util.PersistentIntentService;
import com.flat.util.PersistentIntentServiceFragment;

import java.util.List;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class MainFragment extends PersistentIntentServiceFragment {
    private AppController.AppService mService;

    private ImageView signalDot, algDot, rangeDot;
    private TextView signalSummary, algSummary, rangeSummary;
    private boolean shouldEnableService;

    private LocMan locManager;

    @Override
    public void onServiceConnected(PersistentIntentService service) {
        mService = (AppController.AppService) service;
        if (shouldEnableService) {
            service.setEnabled(true);
            onServiceEnabled(true);
            shouldEnableService = false;
        }
    }

    @Override
    protected Class<? extends PersistentIntentService> getServiceClass() {
        return AppController.AppService.class;
    }

    @Override
    protected void onServiceEnabled(boolean enabled) {
        setPersistent(enabled);
        LocMan.getInstance(getActivity()).setEnabled(enabled);
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
        locManager.getSignalManager().registerListener(signalListener);
        locManager.getAlgorithmManager().registerListener(algListener);
        locManager.getNodeManager().registerListener(nodeManagerListener);
    }

    private void unregisterListeners() {
        locManager.getSignalManager().unregisterListener(signalListener);
        locManager.getAlgorithmManager().unregisterListener(algListener);
        locManager.getNodeManager().unregisterListener(nodeManagerListener);
    }

    private void updateSummaries() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        int count = 0;
        for (Signal s : locManager.getSignalManager().getSignals()) {
            if (prefs.getBoolean(s.getName(), false)) ++count;
        }
        signalSummary.setText(count + " enabled");

        count = 0;
        for (Algorithm la : locManager.getAlgorithmManager().getAlgorithms()) {
            if (prefs.getBoolean(la.getName(), false)) ++count;
        }
        algSummary.setText(count + " enabled");

        rangeSummary.setText(locManager.getNodeManager().getNodeCount() + " nodes in range (this app instance)");
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
        public void onNodeAdded(RemoteNode n) {
            rangeSummary.setText(locManager.getNodeManager().getNodeCount() + " nodes in range (this app instance)");
        }

        @Override
        public void onRangePending(RemoteNode n, NodeRange r) {

        }

        @Override
        public void onStatePending(Node n, NodeState s) {

        }

        @Override
        public void onRangeChanged(RemoteNode n, NodeRange r) {
            blink(rangeDot);
        }

        @Override
        public void onStateChanged(Node n, NodeState s) {

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
        locManager = LocMan.getInstance(getActivity());
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
