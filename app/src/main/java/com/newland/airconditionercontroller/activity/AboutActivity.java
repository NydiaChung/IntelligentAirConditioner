package com.newland.airconditionercontroller.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import com.newland.airconditionercontroller.R;
import com.newland.airconditionercontroller.base.BaseActivity;

public class AboutActivity extends BaseActivity {
    private static String TAG = "AboutActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        initActionBar();
    }

    private void initActionBar() {
        initHeadView();
        setHeadVisable(true);
        initLeftTitleView("返回");
        setLeftTitleView(true);
        initTitleView("关于我们");
        setRithtTitleViewVisable(false);
    }
}
