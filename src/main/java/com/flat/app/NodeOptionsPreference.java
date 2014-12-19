package com.flat.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.flat.R;
import com.flat.localization.Model;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class NodeOptionsPreference extends Preference {

    private TextView name;
    private Switch enabled;

    public NodeOptionsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NodeOptionsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NodeOptionsPreference(Context context) {
        super(context);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        LayoutInflater li = (LayoutInflater)getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View layout = li.inflate(R.layout.node_options_item, parent, false);

        getPersistedValues();


        ((TextView) layout.findViewById(R.id.nodeId)).setText(getKey());

        layout.findViewById(R.id.nodeInfo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });

        name = (TextView) layout.findViewById(R.id.nodeName);
        enabled = (Switch) layout.findViewById(R.id.nodeIgnoreSwitch);

        name.setText(Model.getInstance().getNode(getKey()).getName());

        getPersistedValues();

        enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveState();
            }
        });

        return layout;
    }

    @Override
    protected void onClick() {
        showDialog();

    }

    private void showDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(getContext());
        b.setMessage("Name");

        final EditText input = new EditText(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        b.setView(input);

        b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (input.length() > 0) {
                    name.setText(input.getText());
                    saveState();
                }
            }
        });

        b.show();
    }



    private void saveState() {
        persistString(enabled.isChecked() + "," + name.getText());
    }

    private void getPersistedValues() {
        String info = getPersistedString("");
        if (!TextUtils.isEmpty(info)) {
            String[] parts = info.split(",", 2);
            if (!TextUtils.isEmpty(parts[0])) {
                enabled.setChecked(Boolean.valueOf(parts[0]));
            }
            if (parts.length > 1) {
                name.setText(parts[1]);
            }
        }
    }
}
