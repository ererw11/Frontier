package com.android.frontier.fill_up;

import java.io.Serializable;
import java.util.HashMap;

public class FillUp implements Serializable {

    private String id;
    private String date;
    private String placeId;
    private String tripMileage;
    private String pricePerGallon;
    private String gallons;


    public FillUp() {
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

    public String getTripMileage() {
        return tripMileage;
    }

    public void setTripMileage(String tripMileage) {
        this.tripMileage = tripMileage;
    }

    public String getPricePerGallon() {
        return pricePerGallon;
    }

    public void setPricePerGallon(String pricePerGallon) {
        this.pricePerGallon = pricePerGallon;
    }

    public String getGallons() {
        return gallons;
    }

    public void setGallons(String gallons) {
        this.gallons = gallons;
    }

    public HashMap<String, String> toFirebaseObject() {
        HashMap<String, String> fill_up = new HashMap<>();
        fill_up.put("id", id);
        fill_up.put("date", date);
        fill_up.put("placeId", placeId);
        fill_up.put("tripMileage", tripMileage);
        fill_up.put("pricePerGallon", pricePerGallon);
        fill_up.put("gallons", gallons);

        return fill_up;
    }
}
