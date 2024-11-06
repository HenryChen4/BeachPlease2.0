package com.example.beachplease;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GeoapifyResponse {
    @SerializedName("features")
    private List<Feature> features;

    public List<Feature> getFeatures() {
        return features;
    }

    public static class Feature {
        @SerializedName("properties")
        private Properties properties;

        public Properties getProperties() {
            return properties;
        }
    }

    public static class Properties {
        @SerializedName("name")
        private String name;

        @SerializedName("lon")
        private double longitude;

        @SerializedName("lat")
        private double latitude;

        public String getName() {
            return name;
        }

        public double getLongitude() {
            return longitude;
        }

        public double getLatitude() {
            return latitude;
        }
    }
}

