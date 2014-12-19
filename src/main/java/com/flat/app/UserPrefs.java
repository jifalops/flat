package com.flat.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Button;

import com.flat.R;
import com.flat.localization.Model;
import com.flat.localization.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class UserPrefs extends PreferenceActivity {




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //setContentView(R.layout.prefs_activity);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();

        // Add a button to the header list.
        if (hasHeaders()) {
            Button button = new Button(this);
            button.setText("Some action");
            setListFooter(button);
        }

    }


    public static class PrefsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            PreferenceScreen opts = (PreferenceScreen) findPreference("node options");

            NodeOptionsPreference nop;

            List<String> ids = new ArrayList<String>();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Pattern pat = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})");


            for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                if (pat.matcher(entry.getKey()).matches()) {
                    ids.add(entry.getKey());
                }
            }

            for (Node n : Model.getInstance().getNodesCopy()) {
                if (!ids.contains(n.getId())) {
                    ids.add(n.getId());
                }
            }

            Collections.sort(ids);
            for (String id : ids) {
                nop = new NodeOptionsPreference(getActivity());
                nop.setKey(id);
                opts.addPreference(nop);
            }
        }



    }

}
