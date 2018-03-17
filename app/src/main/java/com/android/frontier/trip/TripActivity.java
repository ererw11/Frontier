package com.android.frontier.trip;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.android.frontier.utils.SingleFragmentActivity;

public class TripActivity extends SingleFragmentActivity {

    private static final String EXTRA_TRIP =
            "com.android.frontier.trip.trip_id";

    public static Intent newIntent(Context packageContext, Trip trip) {
        Intent intent = new Intent(packageContext, TripActivity.class);
        intent.putExtra(EXTRA_TRIP, trip);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        Trip trip = (Trip) getIntent()
                .getSerializableExtra(EXTRA_TRIP);
        return TripFragment.newInstance(trip);
    }
}
