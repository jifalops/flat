package com.flat.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.flat.R;
import com.flat.nsd.NsdChatActivity;
import com.flat.sensors.RotationVectorDemo;
import com.flat.util.wifi.ScanResultsFragment;

public class MainActivity extends Activity implements MainFragment.Callbacks,
        BluetoothFragment.BluetoothFragmentListener, ScanResultsFragment.Callback, MovementSensorFragment.Callback {
    private MainFragment mMainFragment;
    private Fragment mDetailsFragment;

    private int mCurrentItem = MainHelper.DEFAULT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mDetailsFragment = null;

        if (savedInstanceState != null) {
            mCurrentItem = MainHelper.getItem(savedInstanceState);

            mMainFragment = (MainFragment) getFragmentManager()
                    .getFragment(savedInstanceState, MainFragment.class.getName());


            mDetailsFragment = getFragmentManager().getFragment(savedInstanceState,
                    MainHelper.getFragmentName(mCurrentItem)
            );
        }

        if (mMainFragment == null) {
            mMainFragment = (MainFragment) getFragmentManager().findFragmentById(R.id.mainFragment);
        }

        if (isDualPane()) {
            if (mDetailsFragment == null) {
                mDetailsFragment = MainHelper.getFragment(mCurrentItem);
            }

            getFragmentManager().beginTransaction()
                    .add(R.id.detailsFragmentWrapper, mDetailsFragment)
                    .commit();

            showDetails(mCurrentItem);
        }


//        if (!RootTools.isAccessGiven()) {
//            Toast.makeText(this, "Root is not available or was denied! App may not function correctly.", Toast.LENGTH_LONG).show();
//        } else if (!RootTools.isBusyboxAvailable()) {
//            RootTools.offerBusyBox(this);
//        }
    }

    @Override
    public void onRequestBluetoothEnabled() {

    }

    @Override
    public void onRequestDiscoverable() {

    }

    @Override
    public void onBluetoothSupported(boolean supported) {
        if (!supported) {
            Toast.makeText(this, getString(R.string.bt_not_supported), Toast.LENGTH_LONG).show();
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(MainHelper.KEY_ITEM, mCurrentItem);
//		if (mMainFragment != null) getSupportFragmentManager()
//			.putFragment(outState, GroupListFragment.class.getName(), mMainFragment);
//		if (mDetailsFragment != null) getSupportFragmentManager()
//			.putFragment(outState, DetailsFragment.class.getName(), mDetailsFragment);
    }

    @Override
    public void showDetails(int item) {
        boolean changed = item != mCurrentItem;
        mCurrentItem = item;

        if (item == MainHelper.ROTATION_VECTOR_DEMO) {
            startActivity(new Intent(this, RotationVectorDemo.class));
            return;
        }

        if (item == MainHelper.NETWORK_SERVICE_DISCOVERY) {
            startActivity(new Intent(this, NsdChatActivity.class));
            return;
        }

        if (isDualPane()) {
            // We can display everything in-place with fragments, so update
            // the list to highlight the selected item and show the data.
            mMainFragment.getListView().setItemChecked(item, true);

            // Check what fragment is currently shown, replace if needed.
            if (mDetailsFragment == null) {
                mDetailsFragment =  getFragmentManager().findFragmentById(MainHelper.getFragmentId(item));
            }
            if (mDetailsFragment == null || changed) {
                // Make new fragment to show this selection.
                mDetailsFragment = MainHelper.getFragment(item);

                // Execute a transaction, replacing any existing fragment
                // with this one inside the frame.
                getFragmentManager().beginTransaction()
                        .replace(R.id.detailsFragmentWrapper, mDetailsFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();
            }

        } else {
            // Otherwise we need to launch a new activity to display
            // the dialog fragment with selected text.
            Intent intent = new Intent();
            intent.setClass(this, DetailsActivity.class);
            intent.putExtra(MainHelper.KEY_ITEM, mCurrentItem);
            startActivity(intent);
        }
    }

    @Override
    public boolean isDualPane() {
        return findViewById(R.id.detailsFragmentWrapper) != null;
    }


}