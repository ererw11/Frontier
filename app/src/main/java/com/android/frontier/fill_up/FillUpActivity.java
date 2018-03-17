package com.android.frontier.fill_up;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.android.frontier.utils.SingleFragmentActivity;

public class FillUpActivity extends SingleFragmentActivity
        implements FillUpFragment.Callbacks {

    private static final String EXTRA_FILL_UP =
            "com.android.frontier.fill_up.fill_up_extra";
    private static final String EXTRA_TRIP_ID =
            "com.android.frontier.fill_up.trip_id";

    public static Intent newFillUpIntent(Context packageContext, FillUp fillUp, String tripId) {
        Intent intent = new Intent(packageContext, FillUpActivity.class);
        intent.putExtra(EXTRA_FILL_UP, fillUp);
        intent.putExtra(EXTRA_TRIP_ID, tripId);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        FillUp fillUp = (FillUp) getIntent()
                .getSerializableExtra(EXTRA_FILL_UP);
        String tripId = (String) getIntent()
                .getSerializableExtra(EXTRA_TRIP_ID);
        return FillUpFragment.newFillUpInstance(fillUp, tripId);
    }

    @Override
    public void onFillUpUpdated(FillUp fillUp) {

    }
}
