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
import java.util.List;
import java.util.Map;

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

            List<NodeOptionsPreference.NodeOptions> info = new ArrayList<NodeOptionsPreference.NodeOptions>();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
//            Pattern pat = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})");

            NodeOptionsPreference.NodeOptions no;
            int count = 0;
            for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                if (entry.getKey().startsWith("nodeopts")) {
                    ++count;
                    no = new NodeOptionsPreference.NodeOptions((String) entry.getValue());
                    info.add(no);
                }
            }

            boolean found;
            for (Node n : Model.getInstance().getNodesCopy()) {
                found = false;
                for (NodeOptionsPreference.NodeOptions nops : info) {
                    if (nops.id.equals(n.getId())) {
                        found = true;
                    }
                }
                if (!found) {
                    info.add(new NodeOptionsPreference.NodeOptions("nodeopts" + count, n.getId(), n.getName(), true));
                    ++count;
                }
            }

            for (NodeOptionsPreference.NodeOptions nops : info) {
                nop = new NodeOptionsPreference(getActivity());
                nop.setKey(nops.key);
                opts.addPreference(nop);
            }
        }



    }

}
