package com.android.frontier.expense;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.android.frontier.R;
import com.android.frontier.utils.SingleFragmentActivity;

public class ExpenseListActivity extends SingleFragmentActivity
        implements ExpenseListFragment.Callbacks, ExpenseFragment.Callbacks {

    public static final String EXTRA_TRIP_ID =
            "com.android.frontier.expense.trip_id";

    public static Intent expenseListIntent(Context packageContext, String tripId) {
        Intent intent = new Intent(packageContext, ExpenseListActivity.class);
        intent.putExtra(EXTRA_TRIP_ID, tripId);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        String tripId = (String) getIntent()
                .getSerializableExtra(EXTRA_TRIP_ID);
        return ExpenseListFragment.newInstance(tripId);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_masterdetail;
    }

    @Override
    public void onExpenseSelected(Expense expense, String tripID) {
        if (findViewById(R.id.detail_fragment_container) == null) {
            Intent intent = ExpenseActivity.newExpenseIntent(this, expense, tripID);
            startActivity(intent);
        } else {
            Fragment newDetail = ExpenseFragment.newExpenseInstance(expense, tripID);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_fragment_container, newDetail)
                    .commit();
        }
    }

    @Override
    public void onExpenseUpdated(Expense expense) {
        ExpenseListFragment listFragment = (ExpenseListFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);
        listFragment.updateUI();
    }
}
