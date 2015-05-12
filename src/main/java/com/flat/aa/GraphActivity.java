package com.flat.aa;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.flat.R;
import com.flat.app.testing.OtherTestsActivity;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Jacob Phillips (05/2015, jphilli85 at gmail)
 */
public class GraphActivity extends Activity {
    GraphView graph;
    Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        graph = new GraphView(this);
        setContentView(graph);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTimer();
    }

    void startTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                graph.post(new Runnable() {
                    @Override
                    public void run() {
                        List<DataPoint> points = new ArrayList<DataPoint>();
                        for (Node n : NodeManager.getInstance().getNodes()) {
                            State s = n.getState();
                            if (s != null) {
                                points.add(new DataPoint(s.pos[0], s.pos[1]));
                            }
                        }

                        graph.removeAllSeries();
                        graph.addSeries(new PointsGraphSeries<DataPoint>(points.toArray(new DataPoint[points.size()])));
                    }
                });
            }
        }, 0, 2000);
    }

    void stopTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_other_tests:
                startActivity(new Intent(this, OtherTestsActivity.class));
                break;
        }
        return true;
    }
}
