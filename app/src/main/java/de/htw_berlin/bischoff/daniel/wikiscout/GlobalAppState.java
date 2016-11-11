package de.htw_berlin.bischoff.daniel.wikiscout;

import android.location.Location;

import org.json.JSONObject;

class GlobalAppState {
    private static GlobalAppState instance = null;

    private Location markersUpdatedAt;
    private JSONObject wikiJson;

    private GlobalAppState() {
        // Exists only to defeat instantiation.
    }

    static GlobalAppState getInstance() {
        if (instance == null) {
            instance = new GlobalAppState();
            System.out.println("============> NEW INSTANCE OF APP STATE");
        }
        return instance;
    }

    Location getMarkersUpdatedAt() {
        return markersUpdatedAt;
    }

    void setMarkersUpdatedAt(Location markersUpdatedAt) {
        this.markersUpdatedAt = markersUpdatedAt;
    }

    JSONObject getWikiJson() {
        return wikiJson;
    }

    void setWikiJson(JSONObject wikiJson) {
        this.wikiJson = wikiJson;
    }
}