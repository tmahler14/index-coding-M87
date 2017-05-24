//**************************************************************************************************
// Copyright (C) 2015 M87, Inc. All Rights Reserved.
// Proprietary & Confidential
//
// This source code and the algorithms implemented therein constitute
// confidential information and may compromise trade secrets of M87, Inc.
//--------------------------------------------------------------------------------------------------
package com.m87.sam.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

/**
 * A base activity that handles common functionality in the app.
 */
public abstract class BaseActivity extends ActionBarActivity
{
   @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                if (this instanceof Sam) return false;
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Converts an intent into a {@link Bundle} suitable for use as fragment arguments.
     */
    public static Bundle intentToFragmentArguments(Intent intent)
    {
        Bundle arguments = new Bundle();

        if (intent == null) return arguments;

        final Uri data = intent.getData();
        if (data != null) arguments.putParcelable("_uri", data);

        final Bundle extras = intent.getExtras();
        if (extras != null) arguments.putAll(intent.getExtras());

        return arguments;
    }

    /**
     * Converts a fragment arguments bundle into an intent.
     */
    public static Intent fragmentArgumentsToIntent(Bundle arguments)
    {
        Intent intent = new Intent();
        if (arguments == null) return intent;

        final Uri data = arguments.getParcelable("_uri");
        if (data != null) intent.setData(data);

        intent.putExtras(arguments);
        intent.removeExtra("_uri");

        return intent;
    }
}
