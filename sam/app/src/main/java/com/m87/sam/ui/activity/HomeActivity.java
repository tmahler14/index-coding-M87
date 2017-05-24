package com.m87.sam.ui.activity;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.TabLayout;
import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.m87.sam.BuildConfig;
import com.m87.sam.R;
import com.m87.sam.ui.fragment.NeighborsFragment;
import com.m87.sam.ui.util.Controls;
import com.m87.sam.ui.util.Logger;
import com.m87.sdk.ProximityConfig;
import com.m87.sdk.ProximityEntry;
import com.m87.sdk.ProximityEvent;
import com.m87.sdk.ProximityEvent.EventCode;
import com.m87.sdk.ProximityEvent.EventStatus;
import com.m87.sdk.ProximityLocation;
import com.m87.sdk.ProximityManager;
import com.m87.sdk.ProximityMessage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by tim-azul on 5/22/17.
 */

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = HomeActivity.class.getSimpleName();

    //**************************************************************************************************
    // Private members
    //--------------------------------------------------------------------------------------------------
    private String mTag = "SAM";
    private static final int TID_PUBLISH          = 8701;
    private static final int TID_SUBSCRIBE        = 8702;
    private static final int TID_PUBLISH_CANCEL   = 8703;
    private static final int TID_SUBSCRIBE_CANCEL = 8704;
    private static final int TID_MSG_BROADCAST    = 8705;
    public  static final int TID_MSG_SEND         = 8706;
    public  static final int TID_SET_VALUE        = 8707;
    public  static final int TID_GET_VALUE        = 8708;

    private ProximityManager mApi;

    private HomeFragment mHomeFragment;
    private MessageFragment mMessageFragment;

    private AlertDialog m87InstallDialog;
    private AlertDialog m87InitDialog;
    private AlertDialog m87SdkDisabledDialog;

    private boolean mNoAccessibility;

    private SparseArray<ProximityMessage> msgList = new SparseArray<ProximityMessage>();
    private int msgTid = 1; // 0 is invalid
    private int mSelfProximityEntryId = -1;
    private int mPowerLevel;
    private ProximityLocation mSelfLocation;

    // Private class params
    private Tab homeTab, messageTab;

    private Controls customControls = Controls.getInstance();

    //**************************************************************************************************
    // Private classes
    //--------------------------------------------------------------------------------------------------
    private class SamCallbacks implements ProximityManager.Callbacks {
        @Override
        public void onInitialize(int status, String statusMessage)
        {
            if (status == ProximityManager.InitializeStatus.SUCCESS)
            {
                Logger.debug("Successfully initialized SDK");
                if (m87InitDialog != null)
                {
                    m87InitDialog.cancel();
                    m87InitDialog = null;

                    Toast msg = Toast.makeText(getApplicationContext(), "Successfully started M87", Toast.LENGTH_LONG);
                    msg.show();
                }
            }
            else if (!isFinishing())
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                builder.setTitle("Initialize status").setMessage("Error code " + status
                        + "\nError msg: " + statusMessage);
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        }

        /**
         * Called when the M87 library is done installing
         *
         * @param status
         * @param statusMessage
         */
        public void onInstall(int status, String statusMessage) {
            if (status == ProximityManager.InstallStatus.SUCCESS)
            {
                Logger.debug("Successfully installed SDK");
                if (m87InstallDialog != null)
                {
                    m87InstallDialog.cancel();
                    m87InstallDialog = null;
                }
                startM87();
            }
        };

        @Override
        public void onTerminate(int status, String statusMessage)
        {
        }

        public void onEvent(ProximityEvent event) {
            Logger.debug("Event: code(" + event.getCode() + "), status(" + event.getStatus() + ")");
            int code = event.getCode();
            int status = event.getStatus();

            if (code == EventCode.CONFIG)
            {
                if (status == EventStatus.CONFIG_ACCESSIBILITY)
                {
                    if (! isAccessibilityOn()) goToAccessibilitySettings();
                }
            }
            else if (code == EventCode.MWC_STATE)
            {
                if (status == EventStatus.MWC_STATE_ENABLED)
                {
                    Logger.debug("SDK has resumed");
                    if (m87SdkDisabledDialog != null)
                    {
                        m87SdkDisabledDialog.cancel();
                        m87SdkDisabledDialog = null;
                    }
                }
                else if (status == EventStatus.MWC_STATE_DISABLED)
                {
                    Logger.debug("SDK has stopped");
                    NeighborsFragment.neighborList.clear();
                    if (m87InstallDialog == null) waitForSdkEnable();
                }
            }
            else if (code == EventCode.LOCATION)
            {
                mApi.getValue(TID_GET_VALUE, ProximityManager.Value.Key.LOCATION_UPDATES);
                mApi.getValue(TID_GET_VALUE, ProximityManager.Value.Key.LOCATION_RAW);
                mApi.getValue(TID_GET_VALUE, ProximityManager.Value.Key.LOCATION_NEIGHBORS);
            }
            else if (code == EventCode.UNINSTALL)
            {
                Logger.debug("SDK has been uninstalled");
                if (mApi != null)
                {
                    mApi.terminate();
                    mApi = null;
                }
                if (m87InstallDialog == null) startM87();
            }
        }

        public void onMatch(java.lang.String subscribeExpression, ProximityEntry entry, int matchState)
        {
            if (entry == null)
            {
                Logger.debug("entry is null for %d", matchState);
                return;
            }

            Logger.debug("MATCH! : with ="+entry.getId()+ " msg : "+subscribeExpression+" metadata : "+entry.getMetaData());
            String op = "Ignoring";
            switch (matchState)
            {
                case ProximityManager.MatchState.ADD:
                    op = "Adding";

                    // Add to neighbor array
                    if (entry.isSelf()) mSelfProximityEntryId = entry.getId();
                    NeighborsFragment.neighborList.add(entry);
                    NeighborsFragment.neighborListAdapter.notifyDataSetChanged();

                    // If the transmitter, then send init message
                    if (!customControls.isTransmitter && entry.getExpression().contains("TX")) {
                        sendInitMsg(entry.getId());
                    }

                    break;
                case ProximityManager.MatchState.UPDATE:
                    op = "Updating";
                    for (ProximityEntry n : NeighborsFragment.neighborList)
                    {
                        if (n.getId() == entry.getId())
                        {
                            ProximityEntry.copy(n, entry);
                            break;
                        }
                    }
                    break;
                case ProximityManager.MatchState.DELETE:
                    op = "Deleting";
                    Iterator<ProximityEntry> it = NeighborsFragment.neighborList.iterator();
                    while (it.hasNext())
                    {
                        ProximityEntry n = it.next();
                        if (n.getId() == entry.getId())
                        {
                            if (entry.isSelf()) mSelfProximityEntryId = -1;
                            it.remove();
                            break;
                        }
                    }
                    break;
            }

            Logger.debug("subExpr(%s) %s Entry : id (%d) expression(%s) metaData(%s) range(%d) metrics(%d) rssi(%d), connStatus(%d)",
                    subscribeExpression, op, entry.getId(), entry.getExpression(), entry.getMetaData(), entry.getHopCount(), entry.getMetrics(), entry.getRssi(), entry.getConnStatus());

        }

        public void onReceiveDirectMessage(ProximityMessage obj)
        {
            if (obj == null)
            {
                Logger.debug("entry is null");
                return;
            }

            Logger.debug("Receive Msg Entry: sourceProximityEntryId(%d) destinationProximityEntryId(%d) msg(%s)",
                    obj.getSourceProximityEntryId(), obj.getDestinationProximityEntryId(), obj.getMessage());

            Toast msg = Toast.makeText(getApplicationContext(), "Msg received: "+obj.getMessage(), Toast.LENGTH_LONG);
            msg.show();

            //displayMessageReceived(obj);
        }

        public void onSendDirectMessage(int transactionId, ProximityMessage message,
                                        int status, java.lang.String statusMessage)
        {
            Logger.debug("tid(" + transactionId + ") Message(" + message.getMessage() + ") Send Direct Message status: " + status);
            if (status == ProximityManager.SendDirectMessageStatus.SUCCESS_ACKED)
            {
                ProximityMessage proxMsg = msgList.get(transactionId);
                if (proxMsg != null)
                {
                    Toast msg = Toast.makeText(getApplicationContext(), "Receieved Ack from "+proxMsg.getSourceProximityEntryId(), Toast.LENGTH_LONG);
                    msg.show();
                    //displayMessageAcked(proxMsg);
                    msgList.delete(transactionId);
                }
            }
            if (status != ProximityManager.SendDirectMessageStatus.SUCCESS &&
                    status != ProximityManager.SendDirectMessageStatus.SUCCESS_ACKED &&
                    !isFinishing()) {
//                AlertDialog.Builder builder = new AlertDialog.Builder(Sam.this);
//                builder.setTitle("Send Direct Message Status").setMessage(status.name());
//                final AlertDialog dialog = builder.create();
//                dialog.show();
            }
        }

        public void onPublish(int transactionId, java.lang.String expression,
                              int status, java.lang.String statusMessage) {
            Logger.debug("tid(" + transactionId + ") Expr (" + expression + ") Publish status: " + status);

            if (status != ProximityManager.PublishStatus.SUCCESS && !isFinishing()) {
                Toast msg = Toast.makeText(getApplicationContext(), "Published failed!", Toast.LENGTH_LONG);
                msg.show();
            }
        }

        public void onSubscribe(int transactionId, java.lang.String expression,
                                int status, java.lang.String statusMessage)
        {
            Logger.debug("tid(" + transactionId + ") Expr(" + expression + ") Subscribe status: " + status);
            if (status != ProximityManager.SubscribeStatus.SUCCESS && !isFinishing())
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                builder.setTitle("Subscribe Status").setMessage("Error code " + status
                        + "\nError msg: " + statusMessage);
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        }

        public void onCancelPublish(int tid, int status, java.lang.String statusMessage)
        {
            Logger.debug("tid(" + tid + ") Publish cancel status: " + status);
            if (status != ProximityManager.CancelPublishStatus.SUCCESS && !isFinishing())
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                builder.setTitle("Cancel Publish Status").setMessage("Error code " + status
                        + "\nError msg: " + statusMessage);
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        }

        public void onCancelSubscribe(int tid, int status, java.lang.String statusMessage)
        {
            Logger.debug("tid(" + tid + ") Subscribe cancel status: " + status);
            if (status != ProximityManager.CancelSubscribeStatus.SUCCESS && !isFinishing())
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                builder.setTitle("Cancel Subscribe Status").setMessage("Error code " + status
                        + "\nError msg: " + statusMessage);
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        }

        @Override
        public void onDisableService(int status, String statusMessage) {
            Logger.debug("onDisabledService: status(%d)", status);
        }

        @Override
        public void onConfigChanged(ProximityConfig configInEffect) {
            Logger.debug("onConfigChanged: power level(%d)", configInEffect.getPowerLevel());
            Toast toast = Toast.makeText(HomeActivity.this, "Configuration has changed", Toast.LENGTH_SHORT);
            toast.show();
        }

        @Override
        public void onConfigStatus(ProximityConfig configInEffect, int status, String statusMessage)
        {
            Logger.debug("Config Status: %d, message: %s", status, statusMessage);
            mPowerLevel = configInEffect.getPowerLevel();
            if (isFinishing()) return;
            if (status == ProximityManager.ConfigStatus.SUCCESS)
            {
                Logger.debug("power level = %d", mPowerLevel);
                new AlertDialog.Builder(HomeActivity.this)
                        .setTitle("Current power level: " + mPowerLevel)
                        .setPositiveButton("Set new",
                                new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        displayPowerLevelDialog();
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
            else
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                builder.setTitle("Config Status").setMessage("Error code " + status
                        + "\nError msg: " + statusMessage);
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        }

        @Override
        public void onSetValue(int transactionId, Pair keyValuePair, int status, String statusMessage)
        {
            Logger.debug("onSetValue: key(%d) value(%d) status(%s)", keyValuePair.first, keyValuePair.second, status);
            int key = (Integer) keyValuePair.first;
            if (key == ProximityManager.Value.Key.LOCATION_UPDATES)
            {
                if (status == ProximityManager.Value.Status.SUCCESS && ((Long)keyValuePair.second != 0))
                {
                    mApi.getValue(TID_GET_VALUE, ProximityManager.Value.Key.LOCATION_UPDATES);
                    mApi.getValue(TID_GET_VALUE, ProximityManager.Value.Key.LOCATION_RAW);
                    mApi.getValue(TID_GET_VALUE, ProximityManager.Value.Key.LOCATION_NEIGHBORS);
                }
            }

            if (status != ProximityManager.Value.Status.SUCCESS && !isFinishing())
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                builder.setTitle("Set Value Status").setMessage("Error code " + status);
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        }

        @Override
        public void onGetValue(int transactionId, Pair keyValuePair, int status, String statusMessage)
        {
            Logger.debug("onGetValue: key(%d) status(%s)", keyValuePair.first, status);
            int key = (Integer) keyValuePair.first;
            if (status == ProximityManager.Value.Status.SUCCESS)
            {
                switch (key)
                {
                    case ProximityManager.Value.Key.LOCATION_UPDATES:
                    {
//                        Long value = (Long) keyValuePair.second;
//                        Logger.debug("get LOCATION_UPDATES " + value);
//                        LocationFragment f = getLocationFragment();
//                        if (f != null) f.setLocationUpdates(value != 0);
                        break;
                    }
                    case ProximityManager.Value.Key.LOCATION_RAW:
                    {
//                        List<ProximityLocation> locs  = (List<ProximityLocation>) keyValuePair.second;
//                        mSelfLocation = locs.isEmpty()? null: locs.get(0);
//                        double x = mSelfLocation != null? mSelfLocation.getLocation().getLatitude(): 0;
//                        double y = mSelfLocation != null? mSelfLocation.getLocation().getLongitude(): 0;
//                        double r = mSelfLocation != null? mSelfLocation.getLocation().getAccuracy(): 0;
//                        Logger.debug("get LOCATION_RAW " + x + ", " + y +", " + r);
//                        LocationFragment f = getLocationFragment();
//                        if (f != null) f.refreshData(mSelfLocation);
                        break;
                    }
                    case ProximityManager.Value.Key.LOCATION_NEIGHBORS:
                    {
//                        List<ProximityLocation> locs  = (List<ProximityLocation>) keyValuePair.second;
//                        Logger.debug("get LOCATION_NEIGHBOURS " + locs.size());
//                        LocationFragment f = getLocationFragment();
//                        if (f != null) f.refreshData(mSelfLocation, locs);
                        break;
                    }
                }
            }
            if (status != ProximityManager.Value.Status.SUCCESS && !isFinishing())
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                builder.setTitle("Get Value Status Fails").setMessage("key: " + key + "\nstatus: "
                        + status + "\nmsg: " + statusMessage);
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    };

    //**************************************************************************************************
    // Private methods
    //--------------------------------------------------------------------------------------------------

    /**
     * When the M87 library has not yet been installed, bring up the dialog to install it
     */
    private void showInstallDialog(boolean isUpdate) {
        // Create a dialog to wait for installation
        AlertDialog.Builder builder = new AlertDialog.Builder(Sam.this);
        builder.setMessage(isUpdate? R.string.sdk_update: R.string.sdk_install)
                .setTitle(isUpdate? R.string.sdk_update_title: R.string.sdk_install_title)
                .setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mApi.install();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
                onBackPressed();
            }
        });
        m87InstallDialog = builder.create();
        m87InstallDialog.show();
    };

    /**
     * Loads in the M87 library
     *
     * @return true if the library is installed
     */
    private boolean loadM87()
    {
        mApi = new ProximityManager(this, new HomeActivity.SamCallbacks());

        if (! BuildConfig.isMwcBundled)
        {
            // If the M87 SDK is already installed do nothing
            if (mApi.isInstalled()) return true;
            else {
                // Else install the M87 lib
                showInstallDialog();
                return false;
            }
        }
        return true;
    };

    /**
     * Start the M87 library
     */
    private void startM87() {
        // Give the user the option of enabling the accessibility setting
        if (! isAccessibilityOn() && !mNoAccessibility) goToAccessibilitySettings();

        // Bring up the initializing dialog box
        waitForInit();

        // Init the mApi proximity manager
        if (BuildConfig.isMwcBundled) mApi.initialize("com.m87.sam");
        else                          mApi.initialize(this);
    };

    /**
     * Check if M87 accessibility service is enabled
     *
     * @return true if the service is enabled
     */
    private boolean isAccessibilityOn() {
        int enabled = 0;
        final String service = "/com.m87.mwc.P2pConnectionMonitor";

        boolean accessibilityFound = false;

        try {
            enabled = Settings.Secure.getInt(
                    getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        }
        catch (Exception e) {
        }

        if (enabled != 1) return false;

        String settings = Settings.Secure.getString(
                getApplicationContext().getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

        if (settings != null) {
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
            splitter.setString(settings);
            while (splitter.hasNext())
            {
                if (splitter.next().contains(service)) return true;
            }
        }

        return false;
    };

    /**
     * Bring up the window that asks to enable the accessiblity for P2P connection
     */
    private void goToAccessibilitySettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
        builder.setMessage(R.string.acc_msg).setTitle(R.string.acc_title).setCancelable(false);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // Real override is set below
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if ( ! isAccessibilityOn() ) startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), 0);
                else dialog.dismiss();
            }
        });
    };

    /**
     * Show the "M87 initializing" dialog
     */
    private void waitForInit() {
        if (m87InitDialog == null)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            builder.setMessage(R.string.init_msg).setTitle(R.string.init_title).setCancelable(false);

            m87InitDialog = builder.create();
            m87InitDialog.show();
        }
    };

    private void waitForSdkEnable()
    {
        if (m87SdkDisabledDialog == null)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            builder.setMessage(R.string.sdk_disabled_msg).setTitle(R.string.sdk_disabled_title).setCancelable(false);

            m87SdkDisabledDialog = builder.create();
            m87SdkDisabledDialog.show();
        }
    }

    private void confirmExit()
    {

        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
        builder.setMessage(R.string.confirm_exit);
        builder.setCancelable(false);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                HomeActivity.this.finish();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void getPowerLvl()
    {
        View view = getLayoutInflater().inflate(R.layout.set_power_lvl, null);
        final EditText input = (EditText) view.findViewById(R.id.set_power_lvl_text);

        new AlertDialog.Builder(HomeActivity.this)
                .setTitle("Set power level (current: " + mApi.getPowerLevel() + ")")
                .setView(view)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                String lvlStr = input.getText().toString();
                                if (lvlStr.length() == 0) return;
                                int lvl = Integer.valueOf(lvlStr);
                                if (mApi.setPowerLevel(lvl) != 0)
                                {
                                    Toast toast = Toast.makeText(HomeActivity.this, "Power level out of range!", Toast.LENGTH_SHORT);
                                    toast.show();
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

    private void getExpressionStringPublish()
    {
        View exprStrView = getLayoutInflater().inflate(R.layout.set_expr_str, null);
        final EditText exprStrInput = (EditText) exprStrView.findViewById(R.id.set_expr_str_text);
        final EditText metadataInput = (EditText) exprStrView.findViewById(R.id.set_metadata_text);
        final EditText rangeInput = (EditText) exprStrView.findViewById(R.id.set_range_text);

        new AlertDialog.Builder(HomeActivity.this)
                .setTitle(R.string.set_expr_str)
                .setView(exprStrView)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                String exprStr = exprStrInput.getText().toString();
                                String metadata = metadataInput.getText().toString();
                                String rangeStr = rangeInput.getText().toString();
                                if (exprStr.length() == 0 || rangeStr.length() == 0)
                                {
                                    Toast toast = Toast.makeText(HomeActivity.this, "Expression string and range must be provided", Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                                else
                                {
                                    try
                                    {
                                        int range = Integer.valueOf(rangeStr);
                                        mApi.publish(TID_PUBLISH, exprStr, range, metadata);

                                        Toast toast = Toast.makeText(HomeActivity.this, "Successfully published to: "+exprStr, Toast.LENGTH_SHORT);
                                        toast.show();
                                    }
                                    catch (Exception e)
                                    {
                                        Logger.debug(e.getMessage());
                                    }
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

    private void getExpressionStringCancelPublish()
    {
        View exprStrView = getLayoutInflater().inflate(R.layout.set_expr_str_pub_cancel, null);
        final EditText exprStrInput = (EditText) exprStrView.findViewById(R.id.set_expr_str_text);

        new AlertDialog.Builder(HomeActivity.this)
                .setTitle(R.string.cancel_pub)
                .setView(exprStrView)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                String exprStr = exprStrInput.getText().toString();
                                if (exprStr.length() == 0)
                                {
                                    Toast toast = Toast.makeText(HomeActivity.this, "Expression string must be provided", Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                                else
                                {
                                    mApi.cancelPublish(TID_PUBLISH_CANCEL, exprStr);
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

    private void getExpressionStringSubscribe()
    {
        View exprStrView = getLayoutInflater().inflate(R.layout.set_expr_str_sub, null);
        final EditText exprStrInput = (EditText) exprStrView.findViewById(R.id.set_expr_str_text);
        final EditText rangeInput   = (EditText) exprStrView.findViewById(R.id.set_range_text);

        new AlertDialog.Builder(HomeActivity.this)
                .setTitle(R.string.subscribe)
                .setView(exprStrView)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                String exprStr = exprStrInput.getText().toString();
                                String rangeStr = rangeInput.getText().toString();
                                if (exprStr.length() == 0 || rangeStr.length() == 0)
                                {
                                    Toast toast = Toast.makeText(HomeActivity.this, "Expression string and range must be provided", Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                                else
                                {
                                    try
                                    {
                                        int range = Integer.valueOf(rangeStr);
                                        mApi.subscribe(TID_SUBSCRIBE, exprStr, range);
                                    }
                                    catch (Exception e)
                                    {
                                        Logger.debug(e.getMessage());
                                    }
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

    private void getExpressionStringCancelSubscribe() {
        View exprStrView = getLayoutInflater().inflate(R.layout.set_expr_str_pub_cancel, null);
        final EditText exprStrInput = (EditText) exprStrView.findViewById(R.id.set_expr_str_text);

        mApi.cancelSubscribe(TID_SUBSCRIBE_CANCEL, Controls.SUBSCRIBE_CHANNEL);

        restartConnectionButtons();
    };

    private void displayPowerLevelDialog()
    {
        View view = getLayoutInflater().inflate(R.layout.set_power_lvl, null);
        final EditText input = (EditText) view.findViewById(R.id.set_power_lvl_text);
        new AlertDialog.Builder(HomeActivity.this)
                .setTitle("Set power level 0 - 8 (current: " + mPowerLevel + ")")
                .setView(view)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                String lvlStr = input.getText().toString();
                                if (lvlStr.length() == 0) return;
                                int lvl = Integer.valueOf(lvlStr);
                                ProximityConfig config = new ProximityConfig();
                                config.setPowerLevel(lvl);
                                mApi.setConfigPreference(config);
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

    private void displayMessageReceived(ProximityMessage entry)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
        builder.setTitle(R.string.rcv_msg);

        View dm = getLayoutInflater().inflate(R.layout.rcv_msg, null);
        builder.setView(dm);

        final TextView src = (TextView) dm.findViewById(R.id.rcv_msg_src);
        final TextView dst = (TextView) dm.findViewById(R.id.rcv_msg_dst);
        final TextView msg = (TextView) dm.findViewById(R.id.rcv_msg_msg);

        src.setText(mHomeFragment.findExpressionById(entry.sourceProximityEntryId()));
        dst.setText(mHomeFragment.findExpressionById(entry.destinationProximityEntryId()));
        msg.setText(entry.message());

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void displayMessageAcked(ProximityMessage entry)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
        builder.setTitle(R.string.msg_ack);

        View dm = getLayoutInflater().inflate(R.layout.rcv_msg, null);
        builder.setView(dm);

        final TextView src = (TextView) dm.findViewById(R.id.rcv_msg_src);
        final TextView dst = (TextView) dm.findViewById(R.id.rcv_msg_dst);
        final TextView msg = (TextView) dm.findViewById(R.id.rcv_msg_msg);

        src.setText(mHomeFragment.findExpressionById(entry.sourceProximityEntryId()));
        dst.setText(mHomeFragment.findExpressionById(entry.destinationProximityEntryId()));
        msg.setText(entry.message());

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Loads in the config necessary for the M87 lib to run
     */
    private void loadConfig()
    {
        BufferedReader br = null;

        try
        {
            br = new BufferedReader(new FileReader("/data/local/tmp/mwc.config"));
            String line;
            String delims = "\\s*=\\s*";

            while ((line = br.readLine()) != null)
            {
                Logger.debug("[CONFIG] " + line);

                String[] tokens = line.split(delims);
                if (tokens.length != 2) continue;

                if (tokens[0].equals("SAM_NO_ACCESSIBILITY")) mNoAccessibility = (Integer.parseInt(tokens[1]) != 0);
            }
            if (br != null) br.close();
        }
        catch (Exception e)
        {
            // File not found or other exceptions are ignored
            Logger.error(e.getMessage());
        }
    };

    /**
     * Init the connection protocol
     */
    private void initSubscribe() {
        Logger.debug("INIT: Function launch");

        // Send subscribe
        mApi.subscribe(TID_SUBSCRIBE, Controls.SUBSCRIBE_CHANNEL, Controls.SUBSCRIBE_RANGE);

        Logger.debug("INIT: Successfully subscribed");

        Toast msg = Toast.makeText(getApplicationContext(), "Successfully subscribed to channel "+Controls.SUBSCRIBE_CHANNEL, Toast.LENGTH_SHORT);
        msg.show();

        // Wait a second to flip the buttons
//        new android.os.Handler().postDelayed(
//                new Runnable() {
//                    public void run() {
//                        // Toggle buttons
//                        initSubscribeButton.setVisibility(View.GONE);
//                        initPublishButton.setVisibility(View.VISIBLE);
//                    }
//                }, 1000);

    };

    private void restartConnectionButtons() {
        // Wait a second to flip the buttons
//        new android.os.Handler().postDelayed(
//                new Runnable() {
//                    public void run() {
//                        // Toggle buttons
//                        initSubscribeButton.setVisibility(View.VISIBLE);
//                        initPublishButton.setVisibility(View.GONE);
//                    }
//                }, 1000);
    };

    private void sendInitMsg(int entryId) {
        Logger.debug("SEND INIT MSG");

        Toast msg = Toast.makeText(getApplicationContext(), "TX found: sedning init msg", Toast.LENGTH_SHORT);
        msg.show();
        newMsg(entryId, customControls.deviceId+".init");
    };

    private void cancelInit() {
        mApi.cancelSubscribe(TID_SUBSCRIBE_CANCEL, Controls.SUBSCRIBE_CHANNEL);

        mApi.cancelPublish(TID_PUBLISH_CANCEL, customControls.publishMessage);

        restartConnectionButtons();
    };

    /**
     * Init the connection protocol
     */
    private void initPublish() {
        Logger.debug("PUBLISH: Function launch");

        // Send subscribe
        mApi.publish(TID_PUBLISH, customControls.publishMessage, Controls.SUBSCRIBE_RANGE, null);

        Logger.debug("PUBLISH: Successfully published");

        Toast msg = Toast.makeText(getApplicationContext(), "Published msg:  "+customControls.publishMessage, Toast.LENGTH_LONG);
        msg.show();

        if (this.customControls.isTransmitter) {
            initTest();
        }
    };


    /**
     * Init the test
     * @return
     */
    private void initTest() {
        Toast msg = Toast.makeText(getApplicationContext(), "Initializing test", Toast.LENGTH_LONG);
        msg.show();
    };

    //**************************************************************************************************
    // Public Methods
    //--------------------------------------------------------------------------------------------------
    public ProximityManager getM87Api()
    {
        return mApi;
    }

    /**
     * Send message
     *
     * @param dstProximityEntryId
     * @param msg
     */
    public void newMsg(int dstProximityEntryId, String msg)
    {
        // TODO: Populate sourceExpression from publishing list
        ProximityMessage proxMsg = new ProximityMessage(mSelfProximityEntryId, dstProximityEntryId, msg);
        msgList.append(msgTid, proxMsg);
        mApi.sendDirectMessage(msgTid, proxMsg);
        msgTid++;
    }

    //**************************************************************************************************
    // Activity overrides
    //--------------------------------------------------------------------------------------------------

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHomeFragment = new HomeFragment();
        mMessageFragment = new MessageFragment();

        // Init tab views
        setContentView(R.layout.activity_home_tabs);
        Toolbar toolbar = (Toolbar) findViewById(R.id.home_tab_toolbar);
        setSupportActionBar(toolbar);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.home_tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(""));
        tabLayout.addTab(tabLayout.newTab().setText(""));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        tabLayout.getTabAt(0).setIcon(R.drawable.icon_home_selected);
        tabLayout.getTabAt(1).setIcon(R.drawable.icon_message_selected);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager_home);
        final PagerAdapter adapter = new HomeTabPagerAdapter
                (getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        /******* M87 ********/
        loadConfig();   // Load in the config

        if (loadM87()) startM87();


        // Set broadcast listener for logout (
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction("com.package.ACTION_LOGOUT");
//        registerReceiver(new BroadcastReceiver() {
//
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                //At this point you should start the login activity and finish this one
//                finish();
//            }
//        }, intentFilter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void initControls() {
        Intent intent = new Intent(HomeActivity.this, ControlActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_init_controls:
                initControls();
                return true;
            case R.id.publish:
                Logger.debug("Publish button pressed");
                getExpressionStringPublish();
                return true;
            case R.id.subscribe:
                Logger.debug("Subscribe button pressed");
                getExpressionStringSubscribe();
                return true;
//            case R.id.cancelpublish:
//                Logger.debug("Cancel Publish button pressed");
//                getExpressionStringCancelPublish();
//                return true;
//            case R.id.cancelsubscribe:
//                Logger.debug("Cancel Subscribe button pressed");
//                getExpressionStringCancelSubscribe();
//                return true;
            case R.id.cancelinit:
                Logger.debug("Cancel Init button pressed");
                cancelInit();
                return true;
            case R.id.set_power_lvl:
                Logger.debug("Set power level button pressed");
                //getPowerLvl();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public class HomeTabPagerAdapter extends FragmentStatePagerAdapter {
        int mNumOfTabs;

        public HomeTabPagerAdapter(FragmentManager fm, int NumOfTabs) {
            super(fm);
            this.mNumOfTabs = NumOfTabs;
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    if (mHomeFragment == null) mHomeFragment = new HomeFragment();
                    return mHomeFragment;
                case 1:
                    if (mMessageFragment == null) mMessageFragment = new MessageFragment();
                    return mMessageFragment;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return mNumOfTabs;
        }
    }

    @Override
    protected void onDestroy()
    {
        if (mApi != null)
        {
            Logger.debug("Exiting API");
            NeighborsFragment.neighborList.clear();
            mApi.terminate();
            mApi = null;
        }
        super.onDestroy();
    }

    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    public void onBackPressed()
    {
        confirmExit();
    }

}
