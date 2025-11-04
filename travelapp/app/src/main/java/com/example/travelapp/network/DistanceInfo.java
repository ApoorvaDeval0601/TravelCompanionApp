package com.example.travelapp.network;

import com.google.gson.annotations.SerializedName;

public class DistanceInfo {

    @SerializedName("value")
    private int value; // Distance in meters

    @SerializedName("text")
    private String text; // Human-readable distance (e.g., "1,400 km")

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}