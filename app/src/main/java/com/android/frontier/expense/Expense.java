package com.android.frontier.expense;

import java.io.Serializable;
import java.util.HashMap;

public class Expense implements Serializable {

    private String id;
    private String date;
    private String placeId;
    private String item;
    private String price;
    private String category;

    public Expense() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    HashMap<String, String> toFirebaseObject() {
        HashMap<String, String> expense = new HashMap<>();
        expense.put("id", id);
        expense.put("date", date);
        expense.put("placeId", placeId);
        expense.put("item", item);
        expense.put("price", price);
        expense.put("category", category);

        return expense;
    }
}