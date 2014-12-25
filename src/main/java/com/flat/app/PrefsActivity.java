package com.flat.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.flat.R;
import com.flat.localization.Model;
import com.flat.localization.Node;
import com.flat.loggingrequests.AbstractRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class PrefsActivity extends PreferenceActivity {




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
    }


    public static class PrefsFragment extends PreferenceFragment {


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            getActivity().setTitle(R.string.action_settings);
//            getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());


            EditTextPreference host = (EditTextPreference) findPreference("log host");
            host.setSummary(sharedPrefs.getString(host.getKey(), AbstractRequest.URL));



            PreferenceScreen nodesScreen = (PreferenceScreen) findPreference("node options");

            Preference description = new Preference(getActivity());
            //description.setTitle("Set a node's name or turn it off to ignore in all calculations");
            description.setSummary("Set a node's name or turn it off to ignore in all calculations");
            nodesScreen.addPreference(description);

            NodeOptionsPreference nodePref;
            final List<String> ids = new ArrayList<String>();

            Pattern macAddr = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})");


            // Add all MACs to the list from SharedPrefs.
            for (Map.Entry<String, ?> entry : sharedPrefs.getAll().entrySet()) {
                if (macAddr.matcher(entry.getKey()).matches()) {
                    ids.add(entry.getKey());
                }
            }

            // Add any other MACs from the current data model.
            for (Node n : Model.getInstance().getNodesCopy()) {
                if (!ids.contains(n.getId())) {
                    ids.add(n.getId());
                }
            }

            // Add each node-options-pref to the node screen
            Collections.sort(ids);
            for (String id : ids) {
                nodePref = new NodeOptionsPreference(getActivity());
                nodePref.setKey(id);
                nodesScreen.addPreference(nodePref);
            }

            nodesScreen.setSummary(ids.size() + " known nodes");
        }



    }

}
