package com.m87.sam.ui.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.m87.sam.R;
import com.m87.sam.ui.util.Controls;
import com.m87.sam.ui.util.Logger;
import com.m87.sam.ui.views.GaugeView;

public class TestFragment extends Fragment {
    public GaugeView mGaugeView;
    public Button mTestButton;

    public float degree = -225;
    public float sweepAngleControl = 0;
    public float sweepAngleFirstChart = 1;
    public float sweepAngleSecondChart = 1;
    public float sweepAngleThirdChart = 1;
    public boolean isInProgress = false;
    public boolean resetMode = false;
    public boolean canReset = false;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_test, null);

        // Custom view
        mGaugeView = (GaugeView) mRootView.findViewById(R.id.test_gauge_view);
        mTestButton = (Button) mRootView.findViewById(R.id.button_init_test);

        mGaugeView.setRotateDegree(degree);

        initGauge();


        // Button handlers
        mTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTestDialog();
            }
        });



        return mRootView;
    }

    public void openTestDialog() {
        View testStrView = getActivity().getLayoutInflater().inflate(R.layout.set_test_str, null);
        final EditText testNumTestsInput = (EditText) testStrView.findViewById(R.id.set_test_str_num_test);
        final RadioGroup testStoreResultsRG = (RadioGroup) testStrView.findViewById(R.id.radio_group_test_store_data);
        final RadioGroup testTypeRG = (RadioGroup) testStrView.findViewById(R.id.radio_group_test_test_type);
        final RadioGroup testUseIcRG = (RadioGroup) testStrView.findViewById(R.id.radio_group_test_use_ic);

        testNumTestsInput.setText("1");
        testTypeRG.check(R.id.radio_control_test_type_basic);
        testStoreResultsRG.check(R.id.radio_control_test_store_yes);
        testUseIcRG.check(R.id.radio_control_test_use_ic_yes);


        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.set_test_str)
                .setView(testStrView)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                String exprStr = testNumTestsInput.getText().toString();
                                Boolean sendResults = false;
                                Boolean useIc = true;
                                int testType = 0;

                                switch (testTypeRG.getCheckedRadioButtonId()) {
                                    case R.id.radio_control_test_type_basic:
                                        testType = 0;
                                        break;
                                    case R.id.radio_control_test_type_demo:
                                        testType = 1;
                                        break;
                                }

                                switch (testStoreResultsRG.getCheckedRadioButtonId()) {
                                    case R.id.radio_control_test_store_yes:
                                        sendResults = true;
                                        break;
                                    case R.id.radio_control_test_store_no:
                                        sendResults = false;
                                        break;
                                }

                                switch (testUseIcRG.getCheckedRadioButtonId()) {
                                    case R.id.radio_control_test_use_ic_yes:
                                        useIc = true;
                                        break;
                                    case R.id.radio_control_test_use_ic_no:
                                        useIc = false;
                                        break;
                                }

                                // Set controls
                                Controls.getInstance().setUseIndexCoding(useIc);
                                Controls.getInstance().setTestType(testType);
                                Controls.getInstance().setSendResults(sendResults);


                                // TODO set test and run test
                                if (testType == 0) {
                                    ((HomeActivity) getActivity()).startBasicAlgoTest();
                                } else {
                                    ((HomeActivity) getActivity()).startFinalDemoTest();
                                }


                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.cancel();
                            }
                        })
                .create()
                .show();
    }

    public void resetGauge(){
        degree = -225;
        sweepAngleControl = 0;
        sweepAngleFirstChart = 1;
        sweepAngleSecondChart = 1;
        sweepAngleThirdChart = 1;
        isInProgress = false;
        resetMode = false;
        canReset = false;
    }

    public void initGauge() {
        resetGauge();
        new Thread() {
            public void run() {
                for (int i = 0; i < 300; i++) {
                    try {
                        getActivity().runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                //System.out.println("CHART: degreee = "+degree);
                                sweepAngleControl++;

                                if (sweepAngleControl <= 90) {
                                    sweepAngleFirstChart++;
                                    mGaugeView.setSweepAngleFirstChart(sweepAngleFirstChart);
                                } else if (sweepAngleControl <= 180) {
                                    sweepAngleSecondChart++;
                                    mGaugeView.setSweepAngleSecondChart(sweepAngleSecondChart);
                                } else if (sweepAngleControl <= 270) {
                                    sweepAngleThirdChart++;
                                    mGaugeView.setSweepAngleThirdChart(sweepAngleThirdChart);
                                }

                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (i == 299) {
                        isInProgress = false;
                        canReset = true;
                    }
                }

                // Sweep in the dial only for display
                sweepDial();
            }
        }.start();
    }

    public void sweepDial() {
        new Thread() {
            public void run() {
                for (int i = 0; i < 300; i++) {
                    try {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    degree++;
                                    if (degree < 45) {
                                        mGaugeView.setRotateDegree(degree);
                                    }

                                }
                            });
                            Thread.sleep(3);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                for (int i = 300; i > 0; i--) {
                    try {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    degree--;
                                    if (degree < 45) {
                                        mGaugeView.setRotateDegree(degree);
                                    }
                                }
                            });
                            Thread.sleep(3);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }
}
