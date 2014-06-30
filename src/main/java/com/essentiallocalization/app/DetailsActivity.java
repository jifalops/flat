package com.essentiallocalization.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Bundle;

import com.essentiallocalization.util.wifi.ScanResultsFragment;

public class DetailsActivity extends Activity implements BluetoothFragment.BluetoothFragmentListener, ScanResultsFragment.Callback, MovementSensorFragment.Callback {
	private Fragment mDetailsFragment;

    private int mCurrentItem = MainItems.DEFAULT;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		Configuration config = getResources().getConfiguration();
		if ((config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
						> Configuration.SCREENLAYOUT_SIZE_LARGE) {
            finish();
            return;
        }

		mDetailsFragment = null;

		if (savedInstanceState != null) {
            mCurrentItem = MainItems.getItem(savedInstanceState);

            mDetailsFragment = getFragmentManager().getFragment(savedInstanceState,
                    MainItems.getFragmentName(mCurrentItem)
            );

		} else {
            mCurrentItem = MainItems.getItem(getIntent().getExtras());
        }

        if (mDetailsFragment == null) {
            mDetailsFragment = MainItems.getFragment(mCurrentItem);
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
