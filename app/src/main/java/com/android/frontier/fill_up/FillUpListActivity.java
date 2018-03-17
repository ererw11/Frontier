package com.android.frontier.fill_up;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.android.frontier.R;
import com.android.frontier.utils.SingleFragmentActivity;

public class FillUpListActivity extends SingleFragmentActivity
        implements FillUpListFragment.Callbacks, FillUpFragment.Callbacks {

    private static final String EXTRA_TRIP_ID =
            "com.android.frontier.fill_up.trip_id";

    public static Intent fillUpListIntent(Context packageContext, String tripId) {
        Intent intent = new Intent(packageContext, FillUpListActivity.class);
        intent.putExtra(EXTRA_TRIP_ID, tripId);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        String tripId = (String) getIntent()
                .getSerializableExtra(EXTRA_TRIP_ID);
        return FillUpListFragment.newInstance(tripId);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_masterdetail;
    }

    @Override
    public void onFillUpSelected(FillUp fillUp, String tripId) {
        if (findViewById(R.id.detail_fragment_container) == null) {
            Intent intent = FillUpActivity.newFillUpIntent(this, fillUp, tripId);
            startActivity(intent);
        } else {
            Fragment newDetail = FillUpFragment.newFillUpInstance(fillUp, tripId);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_fragment_container, newDetail)
                    .commit();
        }
    }

    @Override
    public void onFillUpUpdated(FillUp fillUp) {
        FillUpListFragment listFragment = (FillUpListFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);
        listFragment.updateUI();
    }
}
