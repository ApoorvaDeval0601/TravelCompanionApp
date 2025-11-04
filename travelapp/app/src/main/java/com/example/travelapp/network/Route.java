package com.example.travelapp.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Route {

    @SerializedName("legs")
    private List<Leg> legs;

    @SerializedName("summary")
    private String summary;

    public List<Leg> getLegs() {
        return legs;
    }

    public void setLegs(List<Leg> legs) {
        this.legs = legs;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}