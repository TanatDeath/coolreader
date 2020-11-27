package org.coolreader.geo;

import org.coolreader.CoolReader;
import org.coolreader.crengine.Properties;

import java.util.ArrayList;
import java.util.List;

public class GeoLastData {

    public CoolReader coolReader;
    public static List<MetroLocation> metroLocations = new ArrayList<MetroLocation>();
    public static List<TransportStop> transportStops = new ArrayList<TransportStop>();

    public GeoLastData(org.coolreader.CoolReader cr) {
        this.coolReader = cr;
    }

    public void createGeoListener(Properties settings) {
        // do nothing stub
    }

    public void gpsStart() {

    }

    public void gpsStop() {

    }

    public void netwStart() {

    }

    public void netwStop() {

    }

    public static void loadMetroStations(CoolReader cr, boolean forceReload) {

    }

    public static void loadTransportStops(CoolReader cr, boolean forceReload) {

    }

    public boolean checkLocationPermission(CoolReader cr) {
        return true;
    }

}
