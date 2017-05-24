package com.m87.sam.ui.activity;

import android.app.ActionBar;
import android.support.v4.view.ViewPager;
import android.app.ActionBar;
import android.app.Fragment;
import android.support.v4.view.ViewPager;

public class HomeTabListener implements ActionBar.TabListener {
    // Fragment fragment;
    ViewPager mViewPager;

    public HomeTabListener(ViewPager viewPager) {
        this.mViewPager = viewPager;
    }

    public void onTabSelected(android.app.ActionBar.Tab tab, android.app.FragmentTransaction ft) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    public void onTabUnselected(android.app.ActionBar.Tab tab, android.app.FragmentTransaction ft) {
        //nothing done here
    }

    public void onTabReselected(android.app.ActionBar.Tab tab, android.app.FragmentTransaction ft) {
        // nothing done here
    }
}
