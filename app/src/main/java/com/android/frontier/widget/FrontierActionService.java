package com.android.frontier.widget;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.android.frontier.expense.Expense;
import com.android.frontier.fill_up.FillUp;

public class FrontierActionService extends IntentService {

    public static final String ACTION_UPDATE_WIDGET_FILL_UP = "com.android.frontier.widget.update_fill_up";
    public static final String EXTRA_FILL_UP = "com.android.frontier.extra.fill_up";
    public static final String EXTRA_LOCATION_NAME = "com.android.frontier.extra.location_name";
    public static final String ACTION_UPDATE_WIDGET_EXPENSE = "com.android.frontier.widget.update_expense";
    public static final String EXTRA_EXPENSE = "com.android.frontier.extra.expense";

    public FrontierActionService() {
        super("FrontierActionService");
    }

    // Start the action to update the Fill Up part of the widget
    public static void startActionUpdateFillUp(Context context, FillUp fillUp, String locationName) {
        Intent intent = new Intent(context, FrontierActionService.class);
        intent.putExtra(EXTRA_FILL_UP, fillUp);
        intent.putExtra(EXTRA_LOCATION_NAME, locationName);
        intent.setAction(ACTION_UPDATE_WIDGET_FILL_UP);
        context.startService(intent);
    }

    // Start the action to update the Expense part of the widget
    public static void startActionUpdateExpense(Context context, Expense expense) {
        Intent intent = new Intent(context, FrontierActionService.class);
        intent.putExtra(EXTRA_EXPENSE, expense);
        intent.setAction(ACTION_UPDATE_WIDGET_EXPENSE);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPDATE_WIDGET_FILL_UP.equals(action) &&
                    intent.getSerializableExtra(EXTRA_FILL_UP) != null &&
                    intent.getSerializableExtra(EXTRA_LOCATION_NAME) != null) {
                FillUp fillUp = (FillUp) intent.getSerializableExtra(EXTRA_FILL_UP);
                String name = (String) intent.getSerializableExtra(EXTRA_LOCATION_NAME);
                handleActionUpdateFillUp(fillUp, name);
            } else if (ACTION_UPDATE_WIDGET_EXPENSE.equals(action) &&
                    intent.getSerializableExtra(EXTRA_EXPENSE) != null) {
                Expense expense = (Expense) intent.getSerializableExtra(EXTRA_EXPENSE);
                handleActionUpdateExpense(expense);
            }
        }
    }

    // Send the Fill Up to the widget along with the location name
    private void handleActionUpdateFillUp(FillUp fillUp, String locationName) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, FrontierWidgetProvider.class));
        FrontierWidgetProvider.updateFrontierWidgets(this, appWidgetManager, fillUp, locationName, null, appWidgetIds);
    }

    // Send the Expense to the widget
    private void handleActionUpdateExpense(Expense expense) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, FrontierWidgetProvider.class));
        FrontierWidgetProvider.updateFrontierWidgets(this, appWidgetManager, null, null, expense, appWidgetIds);
    }
}
