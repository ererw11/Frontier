package com.android.frontier.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.widget.RemoteViews;

import com.android.frontier.R;
import com.android.frontier.expense.Expense;
import com.android.frontier.expense.ExpenseListActivity;
import com.android.frontier.fill_up.FillUp;
import com.android.frontier.fill_up.FillUpListActivity;
import com.android.frontier.trip.TripActivity;
import com.android.frontier.utils.TripUtils;

import static com.android.frontier.utils.TripUtils.getTotalCost;
import static com.android.frontier.utils.TripUtils.turnStringToCash;

/**
 * Implementation of App Widget functionality.
 */
public class FrontierWidgetProvider extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                @Nullable FillUp fillUp, @Nullable String locationName, @Nullable Expense expense, int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.frontier_widget_provider);

        // Load the most recent Fill Up data
        if (fillUp != null) {
            // Display the placeId
            views.setTextViewText(R.id.widget_fill_up_location_text_view, locationName);
            // Display the price
            views.setTextViewText(R.id.widget_fill_up_total_cost_text_view, getTotalCost(fillUp));
            // Display the date
            views.setTextViewText(R.id.widget_fill_up_date_text_view, fillUp.getDate());
        }

        // Load the most recent Expense data
        if (expense != null) {
            // Display the item
            views.setTextViewText(R.id.widget_expense_item_text_view, expense.getItem());
            // Display the category
            views.setTextViewText(R.id.widget_expense_category_text_view, expense.getCategory());
            // Display the price
            views.setTextViewText(R.id.widget_expense_price_text_view, turnStringToCash(expense.getPrice()));
            // Display the correct category image
            views.setImageViewResource(R.id.widget_expense_image_text_view, TripUtils.getCategoryImage(expense.getCategory()));
        }

        // Create an Intent to launch TripActivity when "Frontier" is clicked
        Intent tripIntent = new Intent(context, TripActivity.class);
        PendingIntent tripPendingIntent = PendingIntent.getActivity(context, 0, tripIntent, 0);
        // Widgets allow click handlers to only launch pending intents
        views.setOnClickPendingIntent(R.id.widget_layout, tripPendingIntent);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);

        // Create an Intent to launch FillUpListActivity when Fill Up is clicked
        Intent fillUpIntent = new Intent(context, FillUpListActivity.class);
        PendingIntent fillUpPendingIntent = PendingIntent.getActivity(context, 1, fillUpIntent, 0);
        // Widgets allow click handlers to only launch pending intents
        views.setOnClickPendingIntent(R.id.widget_fill_up_location_text_view, fillUpPendingIntent);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);

        // Create an Intent to launch ExpenseListActivity when Expense is clicked
        Intent expenseIntent = new Intent(context, ExpenseListActivity.class);
        PendingIntent expensePendingIntent = PendingIntent.getActivity(context, 2, expenseIntent, 0);
        // Widgets allow click handlers to only launch pending intents
        views.setOnClickPendingIntent(R.id.widget_expense_item_text_view, expensePendingIntent);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);

    }

    public static void updateFrontierWidgets(Context context, AppWidgetManager appWidgetManager,
                                             @Nullable FillUp fillUp, @Nullable String locationName, @Nullable Expense expense, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, fillUp, locationName, expense, appWidgetId);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

