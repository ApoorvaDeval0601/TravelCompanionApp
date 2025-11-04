package com.example.travelapp.network;

import com.google.gson.annotations.SerializedName;

public class Leg {

    @SerializedName("distance")
    private DistanceInfo distance;

    @SerializedName("duration")
    private DurationInfo duration;

    @SerializedName("start_address")
    private String startAddress;

    @SerializedName("end_address")
    private String endAddress;

    public DistanceInfo getDistance() {
        return distance;
    }

    public void setDistance(DistanceInfo distance) {
        this.distance = distance;
    }

    public DurationInfo getDuration() {
        return duration;
    }

    public void setDuration(DurationInfo duration) {
        this.duration = duration;
    }

    public String getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(String startAddress) {
        this.startAddress = startAddress;
    }

    public String getEndAddress() {
        return endAddress;
    }

    public void setEndAddress(String endAddress) {
        this.endAddress = endAddress;
    }
}

class DurationInfo {
    @SerializedName("value")
    private int value; // in seconds

    @SerializedName("text")
    private String text;

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