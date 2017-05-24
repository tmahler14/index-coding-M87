//**************************************************************************************************
// Copyright (C) 2015 M87, Inc. All Rights Reserved.
// Proprietary & Confidential
//
// This source code and the algorithms implemented therein constitute
// confidential information and may compromise trade secrets of M87, Inc.
//----------------------------------------------------------------------------
// Sam.java - M87 Sample Application
//
// Sample application utilizing the M87 SDK.
//--------------------------------------------------------------------------------------------------

// package
package com.m87.sam.ui.activity;

// import the libs
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;

import com.m87.sam.BuildConfig;
import com.m87.sam.R;
import com.m87.sam.ui.fragment.NeighborsFragment;
import com.m87.sam.ui.util.Controls;
import com.m87.sam.ui.util.Logger;
import com.m87.sdk.*;
import com.m87.sdk.ProximityManager.Status;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/// Sam activity runs everything
public class Sam extends BaseActivity
{
    //**************************************************************************************************
    // Private members
    //--------------------------------------------------------------------------------------------------
    private static final int TID_PUBLISH          = 8701;
    private static final int TID_SUBSCRIBE        = 8702;
    private static final int TID_PUBLISH_CANCEL   = 8703;
    private static final int TID_SUBSCRIBE_CANCEL = 8704;
    private static final int TID_MSG_BROADCAST    = 8705;
    public  static final int TID_MSG_SEND         = 8706;

    private ProximityManager mApi;

    private NeighborsFragment   mNeighborsFragment;
    private ArrayList<Fragment> mFragmentStack;

    private AlertDialog m87InstallDialog;
    private AlertDialog m87InitDialog;
    private AlertDialog m87SdkDisabledDialog;

    private boolean mNoAccessibility;

    private SparseArray<ProximityMessage> msgList = new SparseArray<ProximityMessage>();
    private int msgTid = 1; // 0 is invalid
    private int mSelfProximityEntryId = -1;

    // Custom Stuff
    private Controls customControls = new Controls();

    // DOM Elemants
    private Button initSubscribeButton;
    private Button initPublishButton;
    private Button initTestButton;

    //**************************************************************************************************
    // Private classes
    //--------------------------------------------------------------------------------------------------
    private class SamCallbacks implements ProximityManager.Callbacks {
        @Override
        public void onInitialize(ProximityManager.Status.Initialize status, java.lang.String statusMessage)
        {
            if (status == ProximityManager.Status.Initialize.SUCCESS)
            {
                Logger.debug("Successfully initialized SDK");
                if (m87InitDialog != null)
                {
                    m87InitDialog.cancel();
                    m87InitDialog = null;
                }
            }
        }

        /**
         * Called when the M87 library is done installing
         *
         * @param status
         * @param statusMessage
         */
        public void onInstall(ProximityManager.Status.Install status, java.lang.String statusMessage) {
            if (status == ProximityManager.Status.Install.SUCCESS)
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

        public void onTerminate(ProximityManager.Status.Terminate  status, java.lang.String statusMessage)
        {
        }

        public void onEvent(ProximityManager.Event.Code code, ProximityManager.Event.Status status, java.lang.String message)
        {
            Logger.debug("Event: code(" + code + "), status(" + status + ")");
            if (code == ProximityManager.Event.Code.CONFIG)
            {
                if (status == ProximityManager.Event.Status.CONFIG_ACCESSIBILITY)
                {
                    if (! isAccessibilityOn()) goToAccessibilitySettings();
                }
            }
            else if (code == ProximityManager.Event.Code.MWC_STATE)
            {
                if (status == ProximityManager.Event.Status.MWC_STATE_ENABLED)
                {
                    Logger.debug("SDK has resumed");
                    if (m87SdkDisabledDialog != null)
                    {
                        m87SdkDisabledDialog.cancel();
                        m87SdkDisabledDialog = null;
                    }
                }
                else if (status == ProximityManager.Event.Status.MWC_STATE_DISABLED)
                {
                    Logger.debug("SDK has stopped");
                    NeighborsFragment.neighborList.clear();
                    mNeighborsFragment.display();
                    if (m87InstallDialog == null) waitForSdkEnable();
                }
            }
            else if (code == ProximityManager.Event.Code.UNINSTALL)
            {
                Logger.debug("SDK has been uninstalled");
                actionPop();
                if (mApi != null)
                {
                    mApi.terminate();
                    mApi = null;
                }
                if (m87InstallDialog == null) loadM87();
            }
        }

        public void onMatch(java.lang.String subscribeExpression, ProximityEntry entry, ProximityEntry.State state)
        {
            if (entry == null)
            {
                Logger.debug("entry is null for %s", state.name());
                return;
            }

            Logger.debug("MATCH! : with ="+entry.id()+ " msg : "+subscribeExpression+" metadata : "+entry.metaData());
            String op = "Ignoring";
            switch (state)
            {
                case ADD:
                    op = "Adding";

                    // Add to neighbor array
                    if (entry.isSelf()) mSelfProximityEntryId = entry.id();
                    NeighborsFragment.neighborList.add(entry);
                    NeighborsFragment.neighborListAdapter.notifyDataSetChanged();

                    // If the transmitter, then send init message
                    if (!customControls.isTransmitter && entry.expression().contains("TX")) {
                        sendInitMsg(entry.id());
                    }

                    break;
                case UPDATE:
                    op = "Updating";
                    for (ProximityEntry n : NeighborsFragment.neighborList)
                    {
                        if (n.id() == entry.id())
                        {
                            ProximityEntry.copy(n, entry);
                            break;
                        }
                    }
                    break;
                case DELETE:
                    op = "Deleting";
                    Iterator<ProximityEntry> it = NeighborsFragment.neighborList.iterator();
                    while (it.hasNext())
                    {
                        ProximityEntry n = it.next();
                        if (n.id() == entry.id())
                        {
                            if (entry.isSelf()) mSelfProximityEntryId = -1;
                            it.remove();
                            break;
                        }
                    }
                    break;
            }

            Logger.debug("subExpr(%s) %s Entry : id (%d) expression(%s) metaData(%s) range(%d) metrics(%d) rssi(%d), connStatus(%d)",
                subscribeExpression, op, entry.id(), entry.expression(), entry.metaData(), entry.range(), entry.metrics(), entry.rssi(), entry.connStatus());

            mNeighborsFragment.display();
        }

        public void onReceiveDirectMessage(ProximityMessage obj)
        {
            if (obj == null)
            {
                Logger.debug("entry is null");
                return;
            }

            Logger.debug("Receive Msg Entry: sourceProximityEntryId(%d) destinationProximityEntryId(%d) msg(%s)",
                obj.sourceProximityEntryId(), obj.destinationProximityEntryId(), obj.message());

            Toast msg = Toast.makeText(getApplicationContext(), "Msg received: "+obj.message(), Toast.LENGTH_LONG);
            msg.show();

            //displayMessageReceived(obj);
        }

        public void onSendDirectMessage(int transactionId, ProximityMessage message,
            Status.SendDirectMessage status, java.lang.String statusMessage)
        {
            Logger.debug("tid(" + transactionId + ") Message(" + message.message() + ") Send Direct Message status: " + status);
            if (status == ProximityManager.Status.SendDirectMessage.SUCCESS_ACKED)
            {
                ProximityMessage proxMsg = msgList.get(transactionId);
                if (proxMsg != null)
                {
                    Toast msg = Toast.makeText(getApplicationContext(), "Receieved Ack from "+proxMsg.sourceProximityEntryId(), Toast.LENGTH_LONG);
                    msg.show();
                    //displayMessageAcked(proxMsg);
                    msgList.delete(transactionId);
                }
            }
            if (status != ProximityManager.Status.SendDirectMessage.SUCCESS &&
                status != ProximityManager.Status.SendDirectMessage.SUCCESS_ACKED &&
                !isFinishing()) {
//                AlertDialog.Builder builder = new AlertDialog.Builder(Sam.this);
//                builder.setTitle("Send Direct Message Status").setMessage(status.name());
//                final AlertDialog dialog = builder.create();
//                dialog.show();
            }
        }

        public void onPublish(int transactionId, java.lang.String expression,
            Status.Publish status, java.lang.String statusMessage) {
            Logger.debug("tid(" + transactionId + ") Expr (" + expression + ") Publish status: " + status);

            if (status != ProximityManager.Status.Publish.SUCCESS && !isFinishing()) {
                Toast msg = Toast.makeText(getApplicationContext(), "Published failed!", Toast.LENGTH_LONG);
                msg.show();
            }
        }

        public void onSubscribe(int transactionId, java.lang.String expression,
            Status.Subscribe status, java.lang.String statusMessage)
        {
            Logger.debug("tid(" + transactionId + ") Expr(" + expression + ") Subscribe status: " + status);
            if (status != ProximityManager.Status.Subscribe.SUCCESS && !isFinishing())
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(Sam.this);
                builder.setTitle("Subscribe Status").setMessage(status.name());
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        }

        public void onCancelPublish(int tid, ProximityManager.Status.CancelPublish status, java.lang.String statusMessage)
        {
            Logger.debug("tid(" + tid + ") Publish cancel status: " + status);
            if (status != ProximityManager.Status.CancelPublish.SUCCESS && !isFinishing())
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(Sam.this);
                builder.setTitle("Cancel Publish Status").setMessage(status.name());
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        }

        public void onCancelSubscribe(int tid, ProximityManager.Status.CancelSubscribe status, java.lang.String statusMessage)
        {
            Logger.debug("tid(" + tid + ") Subscribe cancel status: " + status);
            if (status != ProximityManager.Status.CancelSubscribe.SUCCESS && !isFinishing())
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(Sam.this);
                builder.setTitle("Cancel Subscribe Status").setMessage(status.name());
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
    private void showInstallDialog() {
        // Create a dialog to wait for installation
        AlertDialog.Builder builder = new AlertDialog.Builder(Sam.this);
        builder.setMessage(R.string.sdk_install).setTitle(R.string.sdk_install_title).setCancelable(false);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
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

        // Initially take the user to the M87 install page
        mApi.install();

        m87InstallDialog.show();
        m87InstallDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (! mApi.isInstalled()) mApi.install();
                // The M87Action.INSTALL event will dismiss this dialog
            }
        });
    };

    /**
     * Loads in the M87 library
     *
     * @return true if the library is installed
     */
    private boolean loadM87()
    {
        mApi = new ProximityManager(this, new SamCallbacks());

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
     * Push the neighbors fragment on the view
     *
     * @param fr - fragment
     */
    private void actionPush(Fragment fr) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        if (mFragmentStack.size() == 0) ft.add(R.id.fragment_container, fr);
        else                            ft.replace(R.id.fragment_container, fr);

        ft.commit();
    };

    /**
     * Pop a fragment off the top of the fragment stack
     *
     * @return always return false for some reason
     */
    private boolean actionPop() {
        if (mFragmentStack.size() != 0) mFragmentStack.remove(mFragmentStack.size() - 1);

        if (mFragmentStack.size() == 0) return true;

        FragmentTransaction ft = getFragmentManager().beginTransaction();

        ft.replace(R.id.fragment_container, mFragmentStack.get(mFragmentStack.size() - 1));

        ft.commit();

        return false;
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
        AlertDialog.Builder builder = new AlertDialog.Builder(Sam.this);
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
            AlertDialog.Builder builder = new AlertDialog.Builder(Sam.this);
            builder.setMessage(R.string.init_msg).setTitle(R.string.init_title).setCancelable(false);

            m87InitDialog = builder.create();
            m87InitDialog.show();
        }
    };

    private void waitForSdkEnable()
    {
        if (m87SdkDisabledDialog == null)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(Sam.this);
            builder.setMessage(R.string.sdk_disabled_msg).setTitle(R.string.sdk_disabled_title).setCancelable(false);

            m87SdkDisabledDialog = builder.create();
            m87SdkDisabledDialog.show();
        }
    }

    private void confirmExit()
    {
        if (mFragmentStack.size() > 1) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(Sam.this);
        builder.setMessage(R.string.confirm_exit);
        builder.setCancelable(false);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                actionPop();
                Sam.this.finish();
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

        new AlertDialog.Builder(Sam.this)
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
                                    Toast toast = Toast.makeText(Sam.this, "Power level out of range!", Toast.LENGTH_SHORT);
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

        new AlertDialog.Builder(Sam.this)
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
                                    Toast toast = Toast.makeText(Sam.this, "Expression string and range must be provided", Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                                else
                                {
                                    try
                                    {
                                        int range = Integer.valueOf(rangeStr);
                                        mApi.publish(TID_PUBLISH, exprStr, range, metadata);
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

        new AlertDialog.Builder(Sam.this)
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
                                    Toast toast = Toast.makeText(Sam.this, "Expression string must be provided", Toast.LENGTH_SHORT);
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

        new AlertDialog.Builder(Sam.this)
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
                                    Toast toast = Toast.makeText(Sam.this, "Expression string and range must be provided", Toast.LENGTH_SHORT);
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

    private void displayMessageReceived(ProximityMessage entry)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(Sam.this);
        builder.setTitle(R.string.rcv_msg);

        View dm = getLayoutInflater().inflate(R.layout.rcv_msg, null);
        builder.setView(dm);

        final TextView src = (TextView) dm.findViewById(R.id.rcv_msg_src);
        final TextView dst = (TextView) dm.findViewById(R.id.rcv_msg_dst);
        final TextView msg = (TextView) dm.findViewById(R.id.rcv_msg_msg);

        src.setText(mNeighborsFragment.findExpressionById(entry.sourceProximityEntryId()));
        dst.setText(mNeighborsFragment.findExpressionById(entry.destinationProximityEntryId()));
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
        AlertDialog.Builder builder = new AlertDialog.Builder(Sam.this);
        builder.setTitle(R.string.msg_ack);

        View dm = getLayoutInflater().inflate(R.layout.rcv_msg, null);
        builder.setView(dm);

        final TextView src = (TextView) dm.findViewById(R.id.rcv_msg_src);
        final TextView dst = (TextView) dm.findViewById(R.id.rcv_msg_dst);
        final TextView msg = (TextView) dm.findViewById(R.id.rcv_msg_msg);

        src.setText(mNeighborsFragment.findExpressionById(entry.sourceProximityEntryId()));
        dst.setText(mNeighborsFragment.findExpressionById(entry.destinationProximityEntryId()));
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
        new android.os.Handler().postDelayed(
            new Runnable() {
                public void run() {
                    // Toggle buttons
                    initSubscribeButton.setVisibility(View.GONE);
                    initPublishButton.setVisibility(View.VISIBLE);
                }
            }, 1000);

    };

    private void restartConnectionButtons() {
        // Wait a second to flip the buttons
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        // Toggle buttons
                        initSubscribeButton.setVisibility(View.VISIBLE);
                        initPublishButton.setVisibility(View.GONE);
                    }
                }, 1000);
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

        mNeighborsFragment = new NeighborsFragment();
        actionPush(mNeighborsFragment);

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
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        loadConfig();   // Load in the config

        if (loadM87()) startM87();

        Logger.debug("Serial num = "+Controls.serialNumber);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.drawable.ic_launcher);
        }

        // Init global params
        mFragmentStack     = new ArrayList<Fragment>();
        mNeighborsFragment = new NeighborsFragment();

        setContentView(R.layout.activity_home);
        actionPush(mNeighborsFragment);

        // Init buttons

        // subscribe
        this.initSubscribeButton = (Button) findViewById(R.id.button_init_subscribe);
        initSubscribeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                initSubscribe();
            }
        });

        // publish
        this.initPublishButton = (Button) findViewById(R.id.button_init_publish);
        initPublishButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                initPublish();
            }
        });

        // test
        this.initTestButton = (Button) findViewById(R.id.button_init_publish);
        initTestButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                initTest();
            }
        });
    };

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
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {

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
                getPowerLvl();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
