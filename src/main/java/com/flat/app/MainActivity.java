package com.flat.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.flat.R;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new AppServiceFragment()).commit();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_other_tests:
                startActivity(new Intent(this, OtherTestsActivity.class));
                break;
            case R.id.action_settings:
                startActivity(new Intent(this, PrefsActivity.class));
                break;
        }
        return true;
    }
}
