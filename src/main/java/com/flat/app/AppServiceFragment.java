package com.flat.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.flat.R;
import com.flat.localization.Model;
import com.flat.localization.Node;
import com.flat.localization.coordinatesystem.LocationAlgorithm;
import com.flat.localization.signal.Signal;
import com.flat.util.app.PersistentIntentService;
import com.flat.util.app.PersistentIntentServiceFragment;

import java.util.List;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class AppServiceFragment extends PersistentIntentServiceFragment {
    private AppService mService;

    private ImageView signalDot, algDot;
    private TextView signalSummary, algSummary;

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
    protected void setServiceEnabled(boolean enabled) {
        if (!isBound()) return;
        setPersistent(enabled);
        Model model = Model.getInstance();
        if (enabled) {
            // TODO enable signals that have their switches on.
        } else {
            for (Signal s : model.getSignals()) {
                s.disable(getActivity());
            }

            for (LocationAlgorithm la: model.getAlgorithms()) {
                la.setEnabled(false);
            }
        }
    }

    private void registerListeners() {
        for (Signal s : model.getSignals()) {
            s.registerListener(signalListener);
        }

        for (LocationAlgorithm la: model.getAlgorithms()) {
            la.registerListener(algListener);
        }
    }

    private void unregisterListeners() {
        for (Signal s : model.getSignals()) {
            s.unregisterListener(signalListener);
        }

        for (LocationAlgorithm la: model.getAlgorithms()) {
            la.unregisterListener(algListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        registerListeners();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterListeners();
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
        signalSummary = (TextView) layout.findViewById(R.id.signalSummary);
        algSummary = (TextView) layout.findViewById(R.id.algSummary);

        int count = 0;
        for (Signal s : Model.getInstance().getSignals()) {
            if (s.isEnabled()) ++count;
        }
        signalSummary.setText(count + " enabled");

        count = 0;
        for (LocationAlgorithm la : Model.getInstance().getAlgorithms()) {
            if (la.isEnabled()) ++count;
        }
        algSummary.setText(count + " enabled");

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

        return layout;
    }
}
