package com.m87.sam.ui.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.m87.sam.R;
import com.m87.sam.ui.util.Controls;

/**
 * Created by tim-azul on 5/22/17.
 */

public class ControlActivity extends AppCompatActivity {
    RadioGroup control_rg_device_type;
    Button initButton;
    EditText et_drop_prob;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        // Init view
        control_rg_device_type = (RadioGroup) findViewById(R.id.radio_group_device_type);

        initButton = (Button) findViewById(R.id.bt_init_controls);

        et_drop_prob = (EditText) findViewById(R.id.et_control_drop_probability);

        // Init radio
        control_rg_device_type.check(R.id.radio_control_receiver);

        // Init button
        initButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setControls();
            }
        });

    };

    /**
     * Set the controls when the init button is clicked
     */
    public void setControls(){

        // Set is transmitter vs. receiver
        switch (control_rg_device_type.getCheckedRadioButtonId()) {
            case R.id.radio_control_transmitter:
                Controls.getInstance().setTransmitter(true);
                break;
            case R.id.radio_control_receiver:
                Controls.getInstance().setTransmitter(false);
                break;
        }

        // Set device probability
        if( !(et_drop_prob.getText().toString().matches("")) ){
            Controls.getInstance().setReceiveProb(Double.valueOf(et_drop_prob.getText().toString()));
        }

        // Set num tests
//        if( !(et_num_tests.getText().toString().matches("")) ){
//            Controls.getInstance().setNumTests(Integer.parseInt(et_num_tests.getText().toString()));
//        }

        // Show message
        String controlMsg = String.format("Controls Initialized!\nType: %s\nAlgo: %s\nDrop Prob: %.2f\nNum Tests: %d",
                Controls.getInstance().getTransmitter(),
                Controls.getInstance().getAlgoType(),
                Controls.getInstance().getReceiveProb(),
                Controls.getInstance().getNumTests()
                );
        Toast msg = Toast.makeText(getApplicationContext(), controlMsg, Toast.LENGTH_LONG);
        msg.show();

    };

    /**
     * On change of the radio buttons
     * @param view
     */
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_control_transmitter:
                if (checked)
                    // Trasmitter
                    break;
            case R.id.radio_control_receiver:
                if (checked)
                    // Receiver
                    break;
        }
    }
}
