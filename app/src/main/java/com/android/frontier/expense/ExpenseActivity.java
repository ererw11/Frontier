package com.android.frontier.expense;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.android.frontier.utils.SingleFragmentActivity;

public class ExpenseActivity extends SingleFragmentActivity implements ExpenseFragment.Callbacks {

    private static final String EXTRA_EXPENSE =
            "com.android.frontier.expense.expense_extra";
    private static final String EXTRA_TRIP_ID =
            "com.android.frontier.expense.trip_id";

    public static Intent newExpenseIntent(Context packageContext, Expense expense, String tripId) {
        Intent intent = new Intent(packageContext, ExpenseActivity.class);
        intent.putExtra(EXTRA_EXPENSE, expense);
        intent.putExtra(EXTRA_TRIP_ID, tripId);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        Expense expense = (Expense) getIntent()
                .getSerializableExtra(EXTRA_EXPENSE);
        String tripId = (String) getIntent()
                .getSerializableExtra(EXTRA_TRIP_ID);
        return ExpenseFragment.newExpenseInstance(expense, tripId);
    }

    @Override
    public void onExpenseUpdated(Expense expense) {

    }
}
