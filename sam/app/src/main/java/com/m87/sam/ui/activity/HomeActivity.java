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
import android.os.Handler;
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
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.m87.sam.BuildConfig;
import com.m87.sam.R;
import com.m87.sam.ui.fragment.NeighborsFragment;
import com.m87.sam.ui.pojos.BasicAlgoTest;
import com.m87.sam.ui.pojos.ChatMessage;
import com.m87.sam.ui.pojos.GaussianElimination;
import com.m87.sam.ui.pojos.IndexCoding;
import com.m87.sam.ui.pojos.IndexCodingMessage;
import com.m87.sam.ui.pojos.M87ProximityDevice;
import com.m87.sam.ui.pojos.Matrix;
import com.m87.sam.ui.pojos.ReceiverHandler;
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

    public ProximityManager mApi;

    public TestFragment mTestFragment;
    public HomeFragment mHomeFragment;
    public MessageFragment mMessageFragment;

    private AlertDialog m87InstallDialog;
    private AlertDialog m87InitDialog;
    private AlertDialog m87SdkDisabledDialog;

    private boolean mNoAccessibility;

    private SparseArray<ProximityMessage> msgList = new SparseArray<ProximityMessage>();
    private int msgTid = 1; // 0 is invalid
    private int mSelfProximityEntryId = -1;
    private int mPowerLevel;
    private ProximityLocation mSelfLocation;

    public static String INIT_RECEIVER = "init receiver connection";
    public static String INIT_TRANSMITTER = "init transmitter connection";

    // Private class params
    private Tab homeTab, messageTab;

    private Controls customControls = Controls.getInstance();

    // Tests
    public boolean testRunning = false;
    public BasicAlgoTest basicTest;
    public ReceiverHandler receiverHandler;

    //**************************************************************************************************
    // Private classes
    //--------------------------------------------------------------------------------------------------
    private class SamCallbacks extends ProximityManager.Callbacks {
        @Override
        public void onInitialize(int status, String statusMessage)
        {
            if (status == ProximityManager.InitializeStatus.SUCCESS)
            {
                Logger.debug("Successfully initialized SDK");
//                NeighborsFragment.neighborList.clear();
//                NeighborsFragment f = getNeighboursFragment();
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
        @Override
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

        @Override
        public void onEvent(ProximityEvent event) {
            Logger.debug("Event: code(" + event.getCode() + "), status(" + event.getStatus() + ")");
            int code = event.getCode();
            int status = event.getStatus();

            if (code == EventCode.MWC_STATE)
            {
                if (status == EventStatus.MWC_STATE_ENABLED)
                {
//                    SAM_DEBUG("SDK has resumed");
//                    NeighborsFragment.neighborList.clear();
//                    NeighborsFragment f = getNeighboursFragment();
//                    if (f != null) {
//                        f.display();
//                    }
                    if (m87SdkDisabledDialog != null)
                    {
                        m87SdkDisabledDialog.cancel();
                        m87SdkDisabledDialog = null;
                    }
                }
                else if (status == EventStatus.MWC_STATE_DISABLED)
                {
//                    SAM_DEBUG("SDK has stopped");
//                    NeighborsFragment.neighborList.clear();
//                    NeighborsFragment f = getNeighboursFragment();
//                    if (f != null) {
//                        f.display();
//                    }
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

        @Override
        public void onMatch(java.lang.String subscribeExpression, ProximityEntry entry, int matchState)
        {
            if (entry == null)
            {
                Logger.debug("entry is null for %d", matchState);
                return;
            }

            Logger.debug("MATCH! : with ="+entry.getId()+ " msg : "+subscribeExpression+" metadata : "+entry.getMetaData()+", expression:"+entry.getExpression());
            String op = "Ignoring";
            switch (matchState)
            {
                case ProximityManager.MatchState.ADD:
                    op = "Adding";

                    // Add to neighbor array
                    if (entry.isSelf()) mSelfProximityEntryId = entry.getId();

                    addProximityEvent(entry);

                    if (!customControls.isTransmitter && entry.getExpression().contains("TX")) {
                        Logger.debug("Sending receiver init message");
                        newMsg(entry.getId(), INIT_RECEIVER);
                    }

                    break;
                case ProximityManager.MatchState.UPDATE:
                    op = "Updating";
                    for (M87ProximityDevice n : HomeFragment.neighborList)
                    {
                        if (n.getId() == entry.getId())
                        {
                            M87ProximityDevice.copy(n, entry);
                            break;
                        }
                    }
                    break;
                case ProximityManager.MatchState.DELETE:
                    op = "Deleting";
                    Iterator<M87ProximityDevice> it = HomeFragment.neighborList.iterator();
                    while (it.hasNext())
                    {
                        M87ProximityDevice n = it.next();
                        if (n.getId() == entry.getId())
                        {
                            if (entry.isSelf()) mSelfProximityEntryId = -1;
                            it.remove();
                            break;
                        }
                    }
                    MessageFragment.messageListAdapter.notifyDataSetChanged();
                    break;
            }

            Logger.debug("subExpr(%s) %s Entry : id (%d) expression(%s) metaData(%s) range(%d) metrics(%d) rssi(%d), connStatus(%d)",
                    subscribeExpression, op, entry.getId(), entry.getExpression(), entry.getMetaData(), entry.getHopCount(), entry.getMetrics(), entry.getRssi(), entry.getConnectionStatus());

        }

        @Override
        public void onReceiveMessage(ProximityMessage obj)
        {
            if (obj == null || obj.getMessage() == null || obj.getMessage().length() == 0)
            {
                Logger.debug("entry is null");
                return;
            }

            Logger.debug("Receive Msg Entry: sourceProximityEntryId(%d) destinationProximityEntryId(%d) msg(%s)",
                    obj.getSourceProximityEntryId(), obj.getDestinationProximityEntryId(), obj.getMessage());

            Toast msg = Toast.makeText(getApplicationContext(), "Msg received: "+obj.getMessage(), Toast.LENGTH_SHORT);
            msg.show();

            addChatMessage(new ChatMessage(obj.getSourceProximityEntryId(), obj.getDestinationProximityEntryId(), obj.getMessage(), true));

            // If receiver, and message contains the init trigger, trigger test
            if (customControls.isTransmitter && obj.getMessage().equals(INIT_RECEIVER)) {
                Logger.debug("Sending transmitter init message");
                newMsg(obj.getSourceProximityEntryId(), INIT_TRANSMITTER);
            }

            if (!customControls.isTransmitter && obj.getMessage().contains("test_init")) {
                testRunning = true;
                receiverHandler = new ReceiverHandler(HomeActivity.this);
            }

            if (testRunning) {
                IndexCodingMessage message = IndexCodingMessage.parseMessage(obj.getSourceProximityEntryId(), obj.getMessage());

                if (message != null) {
                    Logger.debug("Test message");
                    Logger.debug("Is transmitter = " + customControls.isTransmitter);
                    Logger.debug("Test type = " + customControls.testType);

                    if (customControls.isTransmitter) {

                        // Handle basic test
                        if (customControls.testType == 0) {
                            basicTest.handleMessage(message);
                        }
                    } else {
                        receiverHandler.handleMessage(message);
                    }
                }
            }
            //displayMessageReceived(obj);
        }

        @Override
        public void onSendMessage(int transactionId, ProximityMessage message,
                                        int status, java.lang.String statusMessage)
        {
            Logger.debug("tid(" + transactionId + ") Message(" + message.getMessage() + ") Send Direct Message status: " + status);
            if (status == ProximityManager.SendMessageStatus.SUCCESS)
            {
                ProximityMessage proxMsg = msgList.get(transactionId);

                addChatMessage(new ChatMessage(message.getSourceProximityEntryId(), message.getDestinationProximityEntryId(), message.getMessage(), false));

                if (proxMsg != null)
                {
//                    Toast msg = Toast.makeText(getApplicationContext(), "Receieved Ack from "+proxMsg.getSourceProximityEntryId(), Toast.LENGTH_LONG);
//                    msg.show();
                    //displayMessageAcked(proxMsg);
                    msgList.delete(transactionId);
                }
            }
            else if (status == ProximityManager.SendMessageStatus.FAILURE_CONN_MONITOR_OFF  && !isFinishing()) {
                // This is likely user turned off the CONNECTION MONITOR after the initial setup.
                // Application has to prompt user to turn it on again, and potentially retry failed
                // messages.
                if (!isConnectionMonitorOn()) {
                    setUpConnectionMonitor();
                }
            } else if (status != ProximityManager.SendMessageStatus.SUCCESS && !isFinishing()) {
//                AlertDialog.Builder builder = new AlertDialog.Builder(Sam.this);
//                builder.setTitle("Send Direct Message Status").setMessage(status.name());
//                final AlertDialog dialog = builder.create();
//                dialog.show();
            }
        }

        @Override
        public void onPublish(int transactionId, java.lang.String expression,
                              int status, java.lang.String statusMessage) {
            Logger.debug("tid(" + transactionId + ") Expr (" + expression + ") Publish status: " + status);
            if (status == ProximityManager.PublishStatus.FAILURE_CONN_MONITOR_OFF && !isFinishing()) {
                // This is likely user turned off the CONNECTION MONITOR after the initial setup.
                // Application has to prompt user to turn it on again, and potentially retry failed
                // publish.
                if (!isConnectionMonitorOn()) {
                    setUpConnectionMonitor();
                }
            }
            else if (status != ProximityManager.PublishStatus.SUCCESS && !isFinishing()) {
                Toast msg = Toast.makeText(getApplicationContext(), "Published failed!", Toast.LENGTH_LONG);
                msg.show();
            }
        }

        @Override
        public void onSubscribe(int transactionId, java.lang.String expression,
                                int status, java.lang.String statusMessage)
        {
            Logger.debug("tid(" + transactionId + ") Expr(" + expression + ") Subscribe status: " + status);
            if (status != ProximityManager.SubscribeStatus.SUCCESS && !isFinishing())
            {
                Logger.debug("Successfully subscribed");
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                builder.setTitle("Subscribe Status").setMessage("Error code " + status
                        + "\nError msg: " + statusMessage);
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        }

        @Override
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

        @Override
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
        public void onConfig(ProximityConfig configInEffect, int status, String statusMessage)
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
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
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
     * Start the M87 library
     */
    private void startM87()
    {

        waitForInit();

        if (mApi == null) {
            mApi = new ProximityManager(this, new HomeActivity.SamCallbacks());
        }
        int r = mApi.initialize(getString(R.string.api_key));

        if (r != ProximityManager.SUCCESS) {
            handleSyncError(r);
        }
    }

    private void handleSyncError(int code)
    {
        String err;
        switch (code)
        {
            case ProximityManager.FAILURE_NOT_INITIALIZED:
                err = "FAILURE_NOT_INITIALIZED";
                break;
            case ProximityManager.FAILURE_UNAVAILABLE:
                err = "FAILURE_UNAVAILABLE";
                break;
            case ProximityManager.FAILURE_NOT_INSTALLED:
                err = "FAILURE_NOT_INSTALLED";
                break;
            case ProximityManager.FAILURE_UPDATE_REQUIRED:
                err = "FAILURE_UPDATE_REQUIRED";
                break;
            default:
                return;
        }

        Toast toast = Toast.makeText(HomeActivity.this, "Handling " + err, Toast.LENGTH_LONG);
        toast.show();

        switch (code)
        {
            case ProximityManager.FAILURE_NOT_INITIALIZED:
                // should never happen, since we always call initialize first
                //
            case ProximityManager.FAILURE_UNAVAILABLE:
                // Connection to MWC service has failed, we can either wait for
                // MWC service to come back, or try re-initialize it on demand.
                //
                Runnable r = new Runnable() {
                    public void run() {
                        startM87();
                    }
                };
                Handler h = new Handler();
                h.postDelayed(r, 5000);
                break;
            case ProximityManager.FAILURE_NOT_INSTALLED:
                // MWC is not installed, prompt user to install MWC
                //
                showInstallDialog(false);
                break;
            case ProximityManager.FAILURE_UPDATE_REQUIRED:
                // MWC version is too low, update is required, prompt user
                //
                showInstallDialog(true);
                break;
            default:
                break;
        }
    }

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

    // Prompt user to turn on P2P Connection Monitor setting, in order to allow M87 to
    // automatically accept peer connections for direct messages
    public void setUpConnectionMonitor() {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
        String label = mApi.getConnectionMonitorName();
        String msg = getString(R.string.acc_msg, label);
        builder.setMessage(msg).setTitle(R.string.acc_title).setCancelable(false);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mApi.setUpConnectionMonitor();
            }
        }).create().show();
    }

    public boolean isConnectionMonitorOn() {
        return mApi.isConnectionMonitorOn();
    }


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
                                        int r = mApi.publish(TID_PUBLISH, exprStr, metadata, true);
                                        if (r != 0) handleSyncError(r);

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
                                    int r = mApi.cancelPublish(TID_PUBLISH_CANCEL, exprStr);
                                    if (r != 0) handleSyncError(r);
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
                                        int r = mApi.subscribe(TID_SUBSCRIBE, exprStr);
                                        if (r != 0) handleSyncError(r);
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

        mApi.cancelSubscribe(TID_SUBSCRIBE_CANCEL, Controls.getInstance().subscribeChannel);

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

        src.setText(mHomeFragment.findExpressionById(entry.getSourceProximityEntryId()));
        dst.setText(mHomeFragment.findExpressionById(entry.getDestinationProximityEntryId()));
        msg.setText(entry.getMessage());

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

        src.setText(mHomeFragment.findExpressionById(entry.getSourceProximityEntryId()));
        dst.setText(mHomeFragment.findExpressionById(entry.getDestinationProximityEntryId()));
        msg.setText(entry.getMessage());

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
     * Init the connection protocol
     */
    public void initSubscribe() {
        Logger.debug("INIT: Function launch");

        // Send subscribe
        mApi.subscribe(TID_SUBSCRIBE, Controls.getInstance().subscribeChannel);

        Logger.debug("INIT: Successfully subscribed");

        Toast msg = Toast.makeText(getApplicationContext(), "Successfully subscribed to channel "+Controls.getInstance().subscribeChannel, Toast.LENGTH_SHORT);
        msg.show();

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
        mApi.cancelSubscribe(TID_SUBSCRIBE_CANCEL, customControls.subscribeMessage);

        mApi.cancelPublish(TID_PUBLISH_CANCEL, customControls.publishMessage);

        restartConnectionButtons();
    };

    /**
     * Init the connection protocol
     */
    public void initPublish() {
        Logger.debug("PUBLISH: Function launch");

        // Send subscribe
        mApi.publish(TID_PUBLISH, customControls.publishMessage, "", true);

        Logger.debug("PUBLISH: Successfully published");

        Toast msg = Toast.makeText(getApplicationContext(), "Published msg:  "+customControls.publishMessage, Toast.LENGTH_LONG);
        msg.show();
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
        int r = mApi.sendMessage(msgTid, proxMsg);
        if (r != 0) handleSyncError(r);
        msgTid++;
    }

    //**************************************************************************************************
    // Activity overrides
    //--------------------------------------------------------------------------------------------------

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTestFragment = new TestFragment();
        mHomeFragment = new HomeFragment();
        mMessageFragment = new MessageFragment();

        // Init tab views
        setContentView(R.layout.activity_home_tabs);
        Toolbar toolbar = (Toolbar) findViewById(R.id.home_tab_toolbar);
        setSupportActionBar(toolbar);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.home_tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(""));
        tabLayout.addTab(tabLayout.newTab().setText(""));
        tabLayout.addTab(tabLayout.newTab().setText(""));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        tabLayout.getTabAt(0).setIcon(R.drawable.icon_test_selected);
        tabLayout.getTabAt(1).setIcon(R.drawable.icon_home_selected);
        tabLayout.getTabAt(2).setIcon(R.drawable.icon_message_selected);

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

        startM87();

//        double[][] matrix = { {1,0,0,0,0,1,0,0,0,0}, {0,1,0,0,0,0,1,0,0,0}, {0,1,1,0,0,0,0,1,0,0} };
//        GaussianElimination.printMatrix(GaussianElimination.run(matrix));

        double[][] matrix2 = { {1,0,0,1,0}, {0,0,1,0,0}, {0,1,0,0,1}, {0,0,1,1,0}, {1,1,0,0,0} };
        Matrix m = new Matrix(matrix2);
        m.show();
        Matrix m2 = IndexCoding.reduceMatrix(m);
        m2.show();

        //Logger.debug(IndexCodingMessage.parseMessage("TX.1.4.0.1.4.78").toString());

        //HomeFragment.neighborList.add(new ProximityEntry());

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
                    if (mTestFragment == null) mTestFragment = new TestFragment();
                    return mTestFragment;
                case 1:
                    if (mHomeFragment == null) mHomeFragment = new HomeFragment();
                    return mHomeFragment;
                case 2:
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
            HomeFragment.neighborList.clear();
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

    /**
     * On change of the radio buttons
     * @param view
     */
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_control_test_store_yes:
                if (checked)

                    break;
            case R.id.radio_control_test_store_no:
                if (checked)

                    break;
            case R.id.radio_control_test_use_ic_yes:
                if (checked)

                    break;
            case R.id.radio_control_test_use_ic_no:
                if (checked)

                    break;
        }
    }

    public void addProximityEvent(ProximityEntry e) {
        if (e.getExpression() != null) {
            HomeFragment.neighborList.add(new M87ProximityDevice(e));
            HomeFragment.neighborListAdapter.notifyDataSetChanged();
        }
    }

    public void addChatMessage(ChatMessage m) {
        if (m.message != null) {
            MessageFragment.messageList.add(m);
            MessageFragment.messageListAdapter.notifyDataSetChanged();
        }
    }

    public void startBasicAlgoTest() {
        // Show toast
        Toast msg = Toast.makeText(getApplicationContext(), "Starting basic algo test", Toast.LENGTH_SHORT);
        msg.show();

        // Create and start test
        testRunning = true;
        basicTest = new BasicAlgoTest(this.getApplicationContext(), this);
        basicTest.run();


    }

    public void startFinalDemoTest() {
        // Show toast
        Toast msg = Toast.makeText(getApplicationContext(), "Starting final demo test", Toast.LENGTH_SHORT);
        msg.show();


    }

}
