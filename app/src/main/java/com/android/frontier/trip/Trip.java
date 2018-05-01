package com.android.frontier.trip;

import java.io.Serializable;

class Trip implements Serializable {

    private String tripName;
    private String id;
    private int numberOfFillUps;
    private int numberOfExpenses;

    public Trip() {
    }

    public String getTripName() {
        return tripName;
    }

    public void setTripName(String tripName) {
        this.tripName = tripName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getNumberOfFillUps() {
        return numberOfFillUps;
    }

    public void setNumberOfFillUps(int numberOfFillUps) {
        this.numberOfFillUps = numberOfFillUps;
    }

    public int getNumberOfExpenses() {
        return numberOfExpenses;
    }

    public void setNumberOfExpenses(int numberOfExpenses) {
        this.numberOfExpenses = numberOfExpenses;
    }
}
