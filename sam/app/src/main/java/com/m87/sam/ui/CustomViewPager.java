//**************************************************************************************************
// Copyright (C) 2015 M87, Inc. All Rights Reserved.
// Proprietary & Confidential
//
// This source code and the algorithms implemented therein constitute
// confidential information and may compromise trade secrets of M87, Inc.
//--------------------------------------------------------------------------------------------------
package com.m87.sam.ui;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class CustomViewPager extends ViewPager
{
    private boolean isPagingEnabled = false;

    public CustomViewPager(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.isPagingEnabled = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (this.isPagingEnabled) return super.onTouchEvent(event);
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event)
    {
        if (this.isPagingEnabled) return super.onInterceptTouchEvent(event);
        return false;
    }

    public void setPagingEnabled(boolean b)
    {
        this.isPagingEnabled = b;
    }
}
