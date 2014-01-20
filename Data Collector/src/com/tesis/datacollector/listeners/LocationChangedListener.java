package com.tesis.datacollector.listeners;

import android.location.Location;

public interface LocationChangedListener {
    void handleLocationChanged(Location location);
    void handleGpsOn();
}
