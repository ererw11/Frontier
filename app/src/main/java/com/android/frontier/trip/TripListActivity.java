package com.android.frontier.trip;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.android.frontier.utils.SingleFragmentActivity;

public class TripListActivity extends SingleFragmentActivity {

    public static Intent tripListIntent(Context packageContext) {
        Intent intent = new Intent(packageContext, TripListActivity.class);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        return new TripListFragment();
    }


}
