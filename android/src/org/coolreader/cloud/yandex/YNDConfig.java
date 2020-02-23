package org.coolreader.cloud.yandex;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.StrUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import okhttp3.OkHttpClient;

public class YNDConfig {
    public static final String YND_ID = "put_yours";
    public static final String YND_PASSW = "put_yours";
    public static final String YND_CALLBACK_URL = "https://yxda6aa587a2924b49b072cd5729fd03e2.oauth.yandex.ru/auth/finish?platform=android";
    public static final String YND_REDIRECT_URL = "https://github.com/plotn/coolreader/ynd";
    public static final String YND_AUTH_URL = "https://oauth.yandex.ru/authorize";
    public static final String YND_DISK_URL = "https://cloud-api.yandex.net:443/v1/disk/resources";
    public static final String YND_DISK_URL_LAST_UPL = "https://cloud-api.yandex.net:443/v1/disk/resources/last-uploaded";
    public static final String YND_DOWNLOAD_URL = "https://cloud-api.yandex.net:443/v1/disk/resources/download";
    public static final Integer YND_ITEMS_LIMIT = 20000;
    public static boolean didLogin = false;
    public static String yndToken;
    public static OkHttpClient client;

    public static boolean init(CoolReader cr) throws IOException {
        // Create ynd client
        final File fYND = new File(cr.getSettingsFile(0).getParent() + "/ynd.token");
        if (!fYND.exists()) {
            cr.showToast(R.string.cloud_need_authorization);
            cr.yndInputTokenDialog = new YNDInputTokenDialog(cr);
            cr.yndInputTokenDialog.show();
            return false;
        } else {
            if ((!didLogin)||(StrUtils.isEmptyStr(yndToken))) {
                BufferedReader reader = new BufferedReader(
                        new FileReader(fYND));
                StringBuilder stringBuilder = new StringBuilder();
                String line = null;
                String ls = System.getProperty("line.separator");
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                    stringBuilder.append(ls);
                }
                reader.close();
                yndToken = stringBuilder.toString().trim();
                client = new OkHttpClient();
                didLogin = true;
                cr.showToast("Using Yandex token: [" + yndToken + "]");
            }
            return true;
        }
    }
}
