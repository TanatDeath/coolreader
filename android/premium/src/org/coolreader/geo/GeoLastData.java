package org.coolreader.geo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.coolreader.crengine.Properties;
import org.coolreader.crengine.Settings;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.StrUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GeoLastData {

    public static final Logger log = L.create("geo");

    public static Double MIN_DIST_NEAR = 100D; // meters
    public static Double MIN_DIST_STATION_SIGNAL1 = 200D; // meters
    public static Double MIN_DIST_STATION_SIGNAL2 = 700D; // meters
    public static Double MIN_DIST_STOP_SIGNAL1 = 100D; // meters
    public static Double MIN_DIST_STOP_SIGNAL2 = 400D; // meters
    // geo work - system objects
    public CoolReader coolReader;
    public ProviderLocationTracker gps = null;
    public ProviderLocationTracker netw = null;
    public LocationTracker.LocationUpdateListener geoListener = null;
    public LocationTracker.LocationUpdateListener netwListener = null;
    public static List<MetroLocation> metroLocations = new ArrayList<MetroLocation>();
    public static List<TransportStop> transportStops = new ArrayList<TransportStop>();
    // logic
    public Double lastLon = 0D;
    public Double lastLat = 0D;
    public MetroStation lastStationBefore = null;
    public MetroStation lastStation = null;
    public MetroStation tempStation = null; //!!!! temp
    public TransportStop tempStop = null; //!!!! temp
    public Double lastStationDist = -1D;
    public boolean near2signalled = false;
    public boolean near1signalled = false;
    public TransportStop lastStopBefore = null;
    public TransportStop lastStop = null;
    public Double lastStopDist = -1D;

    public GeoLastData(org.coolreader.CoolReader cr) {
        this.coolReader = cr;
    }

    public void setLastStation(Double lon, Double lat, MetroStation lastSt) {
        lastStationBefore = lastStation;
        lastStation = lastSt;
        lastStationDist = -1D;
        if (lastSt!=null)
            lastStationDist = geoDistance2(lat, lastSt.lat, lastSt.lat_m, lon, lastSt.lon, lastSt.lon_m,0D, 0D);
    }

    public void setLastStop(Double lon, Double lat, TransportStop lastSt) {
        lastStopBefore = lastStop;
        lastStop = lastSt;
        lastStopDist = -1D;
        if (lastSt!=null)
            lastStopDist = geoDistance(lat, lastSt.lat, lon, lastSt.lon,0D, 0D);
    }

    public void updateDistance(Double lon, Double lat) {
        if (lastStation!=null)
            lastStationDist = geoDistance2(lat, lastStation.lat, lastStation.lat_m, lon, lastStation.lon, lastStation.lon_m,0D, 0D);
        if (lastStop!=null)
            lastStopDist = geoDistance(lat, lastStop.lat, lon, lastStop.lon,0D, 0D);
    }

    public void checkSingnalled(boolean bSameStation, boolean bSameStop) {
        if (near1signalled || near2signalled) return; // if everything already done
        boolean bStationInDist1 = (lastStationDist>0) && (lastStationDist < MIN_DIST_STATION_SIGNAL1);
        boolean bStationInDist2 = (lastStationDist>0) && (lastStationDist < MIN_DIST_STATION_SIGNAL2);
        boolean bStopInDist1 = (lastStopDist>0) && (lastStopDist < MIN_DIST_STOP_SIGNAL1);
        boolean bStopInDist2 = (lastStopDist>0) && (lastStopDist < MIN_DIST_STOP_SIGNAL2);
        boolean bNeedSignal1 = bStationInDist1 || bStopInDist1;
        boolean bNeedSignal2 = bStationInDist2 || bStopInDist2;
        if (bNeedSignal1) {
            if (!near1signalled) doSignal(bSameStation, bSameStop);
            near1signalled = true;
            near2signalled = true;
        }
        if (bNeedSignal2 && (!bNeedSignal1)) {
            if (!near2signalled) doSignal(bSameStation, bSameStop);
            near2signalled = true;
        }
    }

    public void doSignal(final boolean bSameStation, final boolean bSameStop) {
        String s = "";
        if (lastStation != null) s = lastStation.name + " (" + lastStationDist.intValue() + "m)";
        if (lastStop != null) s = s + "; " + lastStop.name + " (" + lastStopDist.intValue() + "m), "+lastStop.routeNumbers;
        if (s.startsWith(";")) s=s.substring(2);
        if (!StrUtils.isEmptyStr(s)) {
            final String sF = s;
            BackgroundThread.instance().postBackground(new Runnable() {
                @Override
                public void run() {
                    BackgroundThread.instance().postGUI(new Runnable() {
                        @Override
                        public void run() {
                            coolReader.showGeoToast(sF, lastStation, lastStop, lastStationDist, lastStopDist, lastStationBefore,
                                    bSameStation, bSameStop);
                        }
                    }, 500);
                }
            });
        }
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     *
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     * @returns Distance in Meters
     */
    public static double geoDistance(double lat1, double lat2, double lon1,
                                     double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    public static double geoDistance2(double lat1, double lat2, double lat3, double lon1,
                                     double lon2, double lon3, double el1, double el2) {
        double res1 = geoDistance(lat1, lat2, lon1, lon2, el1, el2);
        double res2 = geoDistance(lat1, lat3, lon1, lon3, el1, el2);
        if (res1 < res2) return res1;
        return res2;
    }


    public MetroStation getClosestStation(Double lon, Double lat, MetroStation exceptS) {
        double dist = -1;
        double closestDist = -1;
        MetroStation closestStation = null;
        for (MetroLocation ml: metroLocations) {
            for (MetroLine mll: ml.metroLines) {
                for (MetroStation ms: mll.metroStations) {
                    if (exceptS!=null)
                        if (ms.equals(exceptS)) continue;
                    dist = geoDistance2(lat, ms.lat, ms.lat_m, lon, ms.lon, ms.lon_m,0D, 0D);
                    if ((dist<closestDist)||(closestDist<0)) {
                        closestDist = dist;
                        closestStation = ms;
                    }
                }
            }
        }
        return closestStation;
    }

    public TransportStop getClosestStop(Double lon, Double lat, TransportStop exceptS) {
        double dist = -1;
        double closestDist = -1;
        TransportStop closestStop = null;
        for (TransportStop ts: transportStops) {
            if (exceptS!=null)
                if (ts.equals(exceptS)) continue;
            dist = geoDistance(lat, ts.lat, lon, ts.lon,0D, 0D);
            if ((dist<closestDist)||(closestDist<0)) {
                closestDist = dist;
                closestStop = ts;
            }
        }
        return closestStop;
    }

    public boolean isNearClosestStation(Double lon, Double lat, MetroStation closestStation, MetroStation closeStation) {
        if ((closestStation != null) && (closeStation != null)) {
            Double dist = geoDistance2(lat, closestStation.lat, closestStation.lat_m, lon, closestStation.lon, closestStation.lon_m, 0D, 0D);
            Double stationsDist = geoDistance2(closestStation.lat, closeStation.lat, closeStation.lat_m,
                    closestStation.lon, closeStation.lon, closeStation.lon_m, 0D, 0D);
            Double stationsDist2 = geoDistance2(closestStation.lat_m, closeStation.lat_m, closeStation.lat_m,
                    closestStation.lon, closeStation.lon, closeStation.lon_m, 0D, 0D);
            if (stationsDist2<stationsDist) stationsDist = stationsDist2;
            if ((dist<stationsDist) || (dist<MIN_DIST_NEAR)) {
                return true;
            }
        }
        return false;
    }

    public boolean isNearClosestStop(Double lon, Double lat, TransportStop closestStop, TransportStop closeStop) {
        if ((closestStop != null) && (closeStop != null)) {
            Double dist = geoDistance(lat, closestStop.lat, lon, closestStop.lon,0D, 0D);
            Double stopsDist = geoDistance(closestStop.lat, closeStop.lat,
                    closestStop.lon, closeStop.lon, 0D, 0D);
            if ((dist<stopsDist) || (dist<MIN_DIST_NEAR)) {
                return true;
            }
        }
        return false;
    }

    public void geoUpdateCoords(Location oldLoc, long oldTime, Location newLoc,
                                long newTime) {
        final Double longitude = newLoc.getLongitude();
        final Double latitude = newLoc.getLatitude();
        final Double llastLon = lastLon;
        final Double llastLat = lastLat;
        //Double height = newLoc.getAltitude();
        //Float accuracy = newLoc.getAccuracy();
        boolean bChanged = false;
        if ((longitude!=null)&&(latitude!=null)) {
            if ((!longitude.equals(lastLon))||(!latitude.equals(lastLat))) {
                bChanged = true;
                lastLon = longitude;
                lastLat = latitude;
            }
        }
        if (bChanged) {
            MetroStation closestStationRaw = getClosestStation(lastLon, lastLat, null);
            MetroStation closeStationRaw = getClosestStation(lastLon, lastLat, closestStationRaw);
            TransportStop closestStopRaw = getClosestStop(lastLon, lastLat, null);
            TransportStop closeStopRaw = getClosestStop(lastLon, lastLat, closestStopRaw);
            boolean bNearMetro = isNearClosestStation(lastLon, lastLat, closestStationRaw, closeStationRaw);
            MetroStation closestStation = null;
            if (bNearMetro) closestStation = closestStationRaw;
            boolean bNearStop = isNearClosestStop(lastLon, lastLat, closestStopRaw, closeStopRaw);
            TransportStop closestStop = null;
            if (bNearStop) closestStop = closestStopRaw;
            boolean bNearObject = bNearMetro || bNearStop;
            if (bNearObject) {
                boolean bSameStation = false;
                if ((lastStation == null) && (closestStation == null)) bSameStation = true;
                if ((!bSameStation) && (lastStation != null) && (closestStation != null))
                    bSameStation = lastStation.equals(closestStation);
                boolean bSameStop = false;
                if ((lastStop == null) && (closestStop == null)) bSameStop = true;
                if ((!bSameStop) && (lastStop != null) && (closestStop != null))
                    bSameStop = lastStop.equals(closestStop);
                boolean bNearObjectChanged = (!bSameStation) || (!bSameStop);
                if (bNearObjectChanged) {
                    if (!bSameStation) setLastStation(lastLon, lastLat, closestStation);
                    if (!bSameStop) setLastStop(lastLon, lastLat, closestStop);
                    near1signalled = false;
                    near2signalled = false;
                    updateDistance(lastLon, lastLat);
                    checkSingnalled(bSameStation, bSameStop);
                } else {
                    updateDistance(lastLon, lastLat);
                    checkSingnalled(false, false);
                }
            }
        }
    }

    public static MetroStation getPrevNextStation(MetroStation ms, boolean isPrev) {
        if (ms != null) {
            for (MetroLocation ml: metroLocations)
                for (MetroLine mln: ml.metroLines)
                    for (int i=0; i < mln.metroStations.size(); i++) {
                        if (ms.equals(mln.metroStations.get(i))) {
                            if ((isPrev) && (i>0)) return mln.metroStations.get(i-1);
                            if ((!isPrev) && (i<mln.metroStations.size()-1)) return mln.metroStations.get(i+1);
                        }
                    }
        }
        return null;
    }

    public static String getStationHexColor(MetroStation ms) {
        if (ms != null) {
            for (MetroLocation ml: metroLocations)
                for (MetroLine mln: ml.metroLines)
                    for (MetroStation mst: mln.metroStations) {
                        if (ms.equals(mst)) return mln.hexColor;
                    }
        }
        return null;
    }

    public static void loadMetroStations(CoolReader cr, boolean forceReload) {
        if (!forceReload)
            if (cr.geoLastData.metroLocations != null)
                if (!cr.geoLastData.metroLocations.isEmpty()) return;
        String s ="";
        try {
            InputStream is = cr.getResources().openRawResource(R.raw.metro_coords);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String str = "";
            while ((str = reader.readLine()) != null) s=s + " " +str;
            is.close();
        } catch (Exception e) {
            log.e("load metro stations file", e);
        }
        if (cr.geoLastData.metroLocations == null) cr.geoLastData.metroLocations = new ArrayList<MetroLocation>();
        cr.geoLastData.metroLocations.clear();
        try {
            JSONArray jsonA = new JSONArray(s);
            if (jsonA != null) {
                int i = 0;
                while (i < jsonA.length()) {
                    JSONObject jso = (JSONObject) jsonA.get(i);
                    MetroLocation metroLocation = new MetroLocation();
                    if (jso.has("id")) { metroLocation.id = jso.getInt("id"); }
                    if (jso.has("name")) { metroLocation.name = jso.getString("name"); }
                    if (jso.has("url")) { metroLocation.url = jso.getString("url"); }
                    ArrayList<MetroLine> metroLines = null;
                    if (jso.has("lines")) {
                        JSONArray jsonL = jso.getJSONArray("lines");
                        metroLines = new ArrayList<MetroLine>();
                        int j = 0;
                        while (j < jsonL.length()) {
                            JSONObject jsoL = (JSONObject) jsonL.get(j);
                            MetroLine metroLine = new MetroLine();
                            if (jsoL.has("id")) { metroLine.id = jsoL.getInt("id"); }
                            if (jsoL.has("hex_color")) { metroLine.hexColor = jsoL.getString("hex_color"); }
                            if (jsoL.has("name")) { metroLine.name = jsoL.getString("name"); }
                            ArrayList<MetroStation> metroStations = null;
                            if (jsoL.has("stations")) {
                                JSONArray jsonS = jsoL.getJSONArray("stations");
                                metroStations = new ArrayList<MetroStation>();
                                int k = 0;
                                while (k < jsonS.length()) {
                                    JSONObject jsoS = (JSONObject) jsonS.get(k);
                                    MetroStation metroStation = new MetroStation();
                                    if (jsoS.has("id")) { metroStation.id = jsoS.getDouble("id"); }
                                    if (jsoS.has("name")) {
                                        metroStation.name = jsoS.getString("name");
                                    } else
                                        metroStation.name = "";
                                    if (jsoS.has("alias")) {
                                        metroStation.alias = jsoS.getString("alias");
                                    } else
                                        metroStation.alias = metroStation.name;
                                    if (metroStation.name.contains("оровиц")) cr.geoLastData.tempStation = metroStation; // !!!!
                                    if (jsoS.has("lat")) { metroStation.lat = jsoS.getDouble("lat"); }
                                    if (jsoS.has("lng")) { metroStation.lon = jsoS.getDouble("lng"); }
                                    if (jsoS.has("lat_m")) {
                                        metroStation.lat_m = jsoS.getDouble("lat_m");
                                    } else metroStation.lat_m = metroStation.lat;
                                    if (jsoS.has("lng_m")) {
                                        metroStation.lon_m = jsoS.getDouble("lng_m");
                                    } else metroStation.lon_m = metroStation.lon;
                                    if (jsoS.has("order")) { metroStation.order = jsoS.getInt("order"); }
                                    metroStations.add(metroStation);
                                    k++;
                                }
                                metroLine.metroStations = metroStations;
                            }
                            metroLines.add(metroLine);
                            j++;
                        }
                    }
                    metroLocation.metroLines = metroLines;
                    ArrayList<MetroInterchange> metroInterchanges = null;
                    if (jso.has("interchanges")) {
                        JSONArray jsonL = jso.getJSONArray("interchanges");
                        metroInterchanges = new ArrayList<MetroInterchange>();
                        int j = 0;
                        while (j < jsonL.length()) {
                            JSONObject jsoL = (JSONObject) jsonL.get(j);
                            if (jsoL.has("interchange")) {
                                MetroInterchange mi = new MetroInterchange();
                                JSONArray jsL = jsoL.getJSONArray("interchange");
                                int jj = 0;
                                while (jj < jsL.length()) {
                                    String st = "";
                                    JSONObject o = (JSONObject) jsL.get(jj);
                                    if (o.has("station")) st = o.getString("station");
                                    if ((jj==0) && (!StrUtils.isEmptyStr(st))) mi.name1 = st;
                                    if ((jj==1) && (!StrUtils.isEmptyStr(st))) mi.name2 = st;
                                    jj++;
                                }
                                if ((!StrUtils.isEmptyStr(mi.name1))&&(!StrUtils.isEmptyStr(mi.name2)))
                                    metroInterchanges.add(mi);
                            }
                            //metroLines.add(metroLine);
                            j++;
                        }
                    }
                    metroLocation.metroInterchanges = metroInterchanges;
                    cr.geoLastData.metroLocations.add(metroLocation);
                    i++;
                }
            }
        } catch (JSONException e) {
            log.e("parse metro stations file", e);
        }
    }

    public static void loadTransportStops(CoolReader cr, boolean forceReload) {
        if (!forceReload)
            if (cr.geoLastData.transportStops != null)
                if (!cr.geoLastData.transportStops.isEmpty()) return;
        if (cr.geoLastData.transportStops == null) cr.geoLastData.transportStops = new ArrayList<TransportStop>();
        cr.geoLastData.transportStops.clear();
        String sT ="";
        try {
            InputStream is = cr.getResources().openRawResource(R.raw.data_398);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "windows-1251"));
            String str = "";
            while ((str = reader.readLine()) != null) sT=sT + " " +str;
            is.close();
        } catch (Exception e) {
            log.e("load transport stops file", e);
        }
        try {
            JSONArray jsonA = new JSONArray(sT);
            if (jsonA != null) {
                int i = 0;
                while (i < jsonA.length()) {
                    JSONObject jso = (JSONObject) jsonA.get(i);
                    TransportStop transportStop = new TransportStop();
                    if (jso.has("Street")) { transportStop.street = jso.getString("Street"); }
                    if (jso.has("Name")) { transportStop.name = jso.getString("Name"); }
                    if (jso.has("Latitude_WGS84")) { transportStop.lat = jso.getDouble("Latitude_WGS84"); }
                    if (jso.has("Longitude_WGS84")) { transportStop.lon = jso.getDouble("Longitude_WGS84"); }
                    if (jso.has("District")) { transportStop.district = jso.getString("District"); }
                    if (jso.has("RouteNumbers")) { transportStop.routeNumbers = jso.getString("RouteNumbers"); }
                    cr.geoLastData.transportStops.add(transportStop);
                    cr.geoLastData.tempStop = transportStop;
                    i++;
                }
            }
        } catch (JSONException e) {
            log.e("parse transport stops file", e);
        }
    }

    public static String getStationColor(MetroLocation ml, String ms) {
        for (MetroLine mln: ml.metroLines)
            for (int i=0; i < mln.metroStations.size(); i++) {
                if ((mln.metroStations.get(i).name.equals(ms))||
                        (mln.metroStations.get(i).alias.equals(ms))) {
                        return mln.hexColor;
                }
            }
        return "";
    }

    public static List<String> getStationInterchangeColors(MetroStation ms) {
        ArrayList<String> res = new ArrayList<String>();
        for (MetroLocation ml: metroLocations)
            for (MetroLine mln: ml.metroLines)
                for (int i=0; i < mln.metroStations.size(); i++) {
                    if (ms.equals(mln.metroStations.get(i))) {
                        if (ml.metroInterchanges != null) {
                            for (MetroInterchange mi: ml.metroInterchanges) {
                                String stI = "";
                                if ((mi.name1.equals(ms.name))||(mi.name1.equals(ms.alias)))
                                    stI = mi.name2;
                                if ((mi.name2.equals(ms.name))||(mi.name2.equals(ms.alias)))
                                    stI= mi.name1;
                                if (!StrUtils.isEmptyStr(stI)) {
                                    String sCol = getStationColor(ml, stI);
                                    if (!StrUtils.isEmptyStr(sCol)) {
                                        if (!res.contains(sCol)) res.add(sCol);
                                    }
                                }
                            }
                        }
                    }
                }
        return res;
    }

    public void createGeoListener(Properties settings) {
        this.gps = new ProviderLocationTracker(coolReader,
                ProviderLocationTracker.ProviderType.GPS);
        this.geoListener = new LocationTracker.LocationUpdateListener() {
            @Override
            public void onUpdate(Location oldLoc, long oldTime, Location newLoc,
                                 long newTime) {
                geoUpdateCoords(oldLoc, oldTime, newLoc, newTime);
            }

        };

        this.netw = new ProviderLocationTracker(coolReader,
                ProviderLocationTracker.ProviderType.NETWORK);

        this.netwListener = new LocationTracker.LocationUpdateListener() {
            @Override
            public void onUpdate(Location oldLoc, long oldTime, Location newLoc,
                                 long newTime) {
                geoUpdateCoords(oldLoc, oldTime, newLoc, newTime);
            }

        };
        int iGeo = settings.getInt(Settings.PROP_APP_GEO, 0);
        if ((iGeo==2)||(iGeo==4)) this.loadMetroStations(coolReader, false);
        if ((iGeo==3)||(iGeo==4)) this.loadTransportStops(coolReader, false);
        if (iGeo>1) {
            this.gps.start(geoListener);
            this.netw.start(netwListener);
        }
    }

    public void gpsStart() {
        gps.start(geoListener);
    }

    public void gpsStop() {
        gps.stop();
    }

    public void netwStart() {
        netw.start(netwListener);
    }

    public void netwStop() {
        netw.stop();
    }

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 200002;

    public boolean checkLocationPermission(CoolReader cr) {
        if (ContextCompat.checkSelfPermission(cr,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(cr,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(cr)
                        .setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
                        .setPositiveButton(R.string.ok, (dialogInterface, i) -> ActivityCompat.requestPermissions(cr,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_CODE_LOCATION_PERMISSION)).create().show();
            } else {
                ActivityCompat.requestPermissions(cr,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_CODE_LOCATION_PERMISSION);
            }
            return false;
        } else {
            return true;
        }
    }

}
