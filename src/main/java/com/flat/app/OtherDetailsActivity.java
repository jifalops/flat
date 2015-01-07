package com.flat.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Bundle;

import com.flat.util.wifi.ScanResultsFragment;

public class OtherDetailsActivity extends Activity implements BluetoothFragment.BluetoothFragmentListener, ScanResultsFragment.Callback, MovementSensorFragment.Callback {
	private Fragment mDetailsFragment;

    private int mCurrentItem = OtherTestsData.DEFAULT;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
		Configuration config = getResources().getConfiguration();
		if ((config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
						> Configuration.SCREENLAYOUT_SIZE_LARGE) {
            finish();
            return;
        }

		mDetailsFragment = null;

		if (savedInstanceState != null) {
            mCurrentItem = OtherTestsData.getItem(savedInstanceState);

            mDetailsFragment = getFragmentManager().getFragment(savedInstanceState,
                    OtherTestsData.getFragmentName(mCurrentItem)
            );

		} else {
            mCurrentItem = OtherTestsData.getItem(getIntent().getExtras());
        }

        if (mDetailsFragment == null) {
            mDetailsFragment = OtherTestsData.getFragment(mCurrentItem);
        }

		if (mDetailsFragment != null) {
            getFragmentManager().beginTransaction()
            .add(android.R.id.content, mDetailsFragment)
            .commit();
        }

    }
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {	
		super.onSaveInstanceState(outState);
//		getSupportFragmentManager().putFragment(outState, DetailsFragment.class.getName(), mDetailsFragment);
	}

    @Override
    public void onRequestBluetoothEnabled() {

    }

    @Override
    public void onRequestDiscoverable() {

    }

    @Override
    public void onBluetoothSupported(boolean supported) {

    }
}
