package com.android.frontier.utils;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;

import com.android.frontier.R;
import com.android.frontier.fill_up.FillUp;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.Date;

public class TripUtils {

    // Confirms if the user is signed in, if not sends the user to the sign in page
    public static void confirmSignedIn(Context packageContext, FirebaseUser user) {
        if (user == null) {
            // user is signed out, go to the trip activity to sign in
            Intent intent = SignInActivity.createIntent(packageContext);
            packageContext.startActivity(intent);
        }
    }

    // Creates a String of the total cost of the Fill Up
    public static String getTotalCost(FillUp fillUp) {
        String stringGallons = fillUp.getGallons();
        String stringPricePerGallon = fillUp.getPricePerGallon();
        if (stringGallons == null) {
            stringGallons = "0.0";
        }
        if (stringPricePerGallon == null) {
            stringPricePerGallon = "0.0";
        }
        String totalCostString;
        Double totalCost = Double.parseDouble(stringGallons) * Double.parseDouble(stringPricePerGallon);
        NumberFormat format = NumberFormat.getCurrencyInstance();
        totalCostString = format.format(totalCost);
        return totalCostString;
    }

    // Creates a String of the Miles Per Gallon in Fill Up
    public static String milesPerGallon(FillUp fillUp) {
        String milesPerGallonString;
        if (!((fillUp.getTripMileage() == null) || (fillUp.getGallons() == null))) {
            Double milesPerGallon = Double.parseDouble(fillUp.getTripMileage()) / Double.parseDouble(fillUp.getGallons());
            milesPerGallonString = String.format("%.2f", milesPerGallon);
            return milesPerGallonString;
        } else {
            return "--.--";
        }
    }

    // Creates a String in Currency format
    public static String turnStringToCash(String stringCost) {
        NumberFormat format = NumberFormat.getCurrencyInstance();
        if (stringCost == null) {
            stringCost = "0.0";
        }
        double stringDouble = Double.parseDouble(stringCost);
        return format.format(stringDouble);
    }

    // Formats the Date to MM/DD/YYYY 12:00 AM
    public static String formatDateWithTime(Date date) {
        String dateFormat = "MM/dd/yy h:mm a";
        return DateFormat.format(dateFormat, date).toString();
    }

    // Returns the Resource for the image of the Expense category
    public static int getCategoryImage(String category) {
        int categoryImage = R.drawable.ic_attach_money_white_48dp;

        if (category == null) {
            return categoryImage;
        }
        switch (category) {
            case "Automotive":
                // Automotive
                categoryImage = R.drawable.ic_directions_car_white_48dp;
                break;
            case "Lodging":
                // Lodging
                categoryImage = R.drawable.ic_hotel_white_48dp;
                break;
            case "Meals":
                // Meals
                categoryImage = R.drawable.ic_restaurant_white_48dp;
                break;
            case "Snacks":
                // Snacks
                categoryImage = R.drawable.ic_local_dining_white_48dp;
                break;
            case "Parking":
                // Parking
                categoryImage = R.drawable.ic_local_parking_white_48dp;
                break;
            case "Entertainment":
                // Entertainment
                categoryImage = R.drawable.ic_local_bar_white_48dp;
                break;
            default:
                // default image
                categoryImage = R.drawable.ic_attach_money_white_48dp;
                break;
        }

        return categoryImage;
    }

    // Returns the Integer of the selected category
    public static int getCategoryInt(String category) {
        int categoryInt = 0;

        if (category == null) {
            return categoryInt;
        }
        switch (category) {
            case "Automotive":
                // Automotive
                categoryInt = 0;
                break;
            case "Lodging":
                // Lodging
                categoryInt = 1;
                break;
            case "Meals":
                // Meals
                categoryInt = 2;
                break;
            case "Snacks":
                // Snacks
                categoryInt = 3;
                break;
            case "Parking":
                // Parking
                categoryInt = 4;
                break;
            case "Entertainment":
                // Entertainment
                categoryInt = 5;
                break;
            default:
                // default
                categoryInt = 0;
                break;
        }

        return categoryInt;
    }
}
