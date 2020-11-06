package org.coolreader.cloud.litres;

import org.coolreader.CoolReader;

public class LitresConfig {
    public static boolean didLogin = false;
    public static boolean needReAuth = false;
    public static String country = "";
    public static String currency = "";
    public static String region = "";
    public static String city = "";
    public static long whenLogin;

    public static boolean init(CoolReader cr) {
        cr.readLitresCloudSettings();
        return true;
    }

}
