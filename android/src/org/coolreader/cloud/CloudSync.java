package org.coolreader.cloud;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.ReaderView;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.StrUtils;
import org.coolreader.crengine.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.CRC32;

public class CloudSync {

    public static int CLOUD_SAVE_READING_POS = 1;
    public static int CLOUD_SAVE_BOOKMARKS = 2;
	public static int CLOUD_SAVE_SETTINGS = 3;
	public static int MAX_FILES_FROM_ONE_DEVICE = 10;
    public static long MIN_DELETE_FILES_DELAY = 300000; // 5 mins
    public static long DELETE_FILES_DELAY = 180000; // 3 mins
    public static long lastDeleteTime = System.currentTimeMillis();

    private static final String TAG = "CloudSyncFolder";

    public static ArrayList<DeviceKnown> devicesKnown = new ArrayList<DeviceKnown>();

    public CloudSync(CoolReader cr) {
        readKnownDevices(cr);
    }

    public static void readKnownDevices(CoolReader cr)
    {
        Log.i("CLOUD","Reading known_devices.json");
        String rh = Utils.readFileToString(cr.getSettingsFile(0).getParent() + "/known_devices.json");
        try {
            devicesKnown = new ArrayList<DeviceKnown>(StrUtils.stringToArray(rh, DeviceKnown[].class));
        } catch (Exception e) {
        }
    }

    public static void saveKnownDevices(CoolReader cr)
    {
        Log.i("CLOUD","Starting save known_devices.json");
        try {
            final Gson gson = new GsonBuilder().setPrettyPrinting().create();
            final String prettyJson = gson.toJson(devicesKnown);
            Utils.saveStringToFileSafe(prettyJson,cr.getSettingsFile(0).getParent() + "/known_devices.json");
        } catch (Exception e) {
        }
    }
    // filemark, device, booksrc, filepaths
    public static HashMap<String, HashMap<String, HashMap<String, ArrayList<CloudFileInfo>>>> hashFiles = null;

    public static void checkFileForDeleteInit() {
        hashFiles = new HashMap<String, HashMap<String, HashMap<String, ArrayList<CloudFileInfo>>>>();
    }

    public static void checkFileForDelete(CloudFileInfo cfile) {
        String[] arrS = cfile.name.split("_"); // 2020-03-30_222303_rpos_635942216_36b928e773055c4a_0.16.json
        String fileMark = "";
        if (cfile.name.contains("_rpos_")) fileMark = "_rpos_";
        if (cfile.name.contains("_bmk_")) fileMark = "_bmk_";
        if ((arrS.length>=5)&&(!StrUtils.isEmptyStr(fileMark))) {
            String sBookCRC = arrS[3];
            String sDevId = arrS[4].replace(".json","");
            if ((!StrUtils.isEmptyStr(sBookCRC))&&(!StrUtils.isEmptyStr(sDevId))&&(!StrUtils.isEmptyStr(fileMark))) {
                HashMap<String, HashMap<String, ArrayList<CloudFileInfo>>> markHash = hashFiles.get(fileMark);
                if (markHash == null) markHash = new HashMap<String, HashMap<String, ArrayList<CloudFileInfo>>>();
                HashMap<String, ArrayList<CloudFileInfo>> devHash = markHash.get(sDevId);
                if (devHash == null) devHash = new HashMap<String, ArrayList<CloudFileInfo>>();
                ArrayList<CloudFileInfo> bookFiles = devHash.get(sBookCRC);
                if (bookFiles == null) bookFiles = new ArrayList<CloudFileInfo>();
                boolean found = false;
                for (CloudFileInfo cf: bookFiles) {
                    if (cf.path.equals(cfile.path)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    bookFiles.add(cfile);
                    devHash.put(sBookCRC,bookFiles);
                    markHash.put(sDevId,devHash);
                    hashFiles.put(fileMark, markHash);
                }
            }
        }
    }

    public static void checkFileForDeleteFinish(CoolReader cr) {
        Date referenceDate = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(referenceDate);
        c.add(Calendar.MONTH, -3);
        referenceDate = c.getTime();
        ArrayList<CloudFileInfo> filesToDel = new ArrayList<CloudFileInfo>();
        String[] fileMarks = {"_rpos_", "_bmk_"};
        for (String fileMark: fileMarks) {
            HashMap<String, HashMap<String, ArrayList<CloudFileInfo>>> markHash = hashFiles.get(fileMark);
            if (markHash != null) {
                for (Map.Entry<String, HashMap<String, ArrayList<CloudFileInfo>>> entryMark : markHash.entrySet()) {
                    String devId = entryMark.getKey();
                    HashMap<String, ArrayList<CloudFileInfo>> devHash = entryMark.getValue();
                    for (Map.Entry<String, ArrayList<CloudFileInfo>> entryDev : devHash.entrySet()) {
                        String bookId = entryDev.getKey();
                        ArrayList<CloudFileInfo> bookList = entryDev.getValue();
                        Comparator<CloudFileInfo> compareByDate = new Comparator<CloudFileInfo>() {
                            @Override
                            public int compare(CloudFileInfo o1, CloudFileInfo o2) {
                                return -(o1.created.compareTo(o2.created));
                            }
                        };
                        Collections.sort(bookList, compareByDate);
                        for (int i = 0; i < bookList.size(); i++) {
                            if ((i>MAX_FILES_FROM_ONE_DEVICE-1)||(bookList.get(i).created.before(referenceDate))) filesToDel.add(bookList.get(i));
                        }
                    }
                }
            }
        }
        if (System.currentTimeMillis()-lastDeleteTime > MIN_DELETE_FILES_DELAY) {
            lastDeleteTime = System.currentTimeMillis();
            BackgroundThread.instance().postGUI(new Runnable() {
                @Override
                public void run() {
                    CloudAction.yndDeleteOldCloudFiles(cr, filesToDel, true);
                }
            }, DELETE_FILES_DELAY);
        }
    }

    public static void checkKnownDevice(CoolReader cr, String devId, String devTitle)
    {
        boolean found = false;
        for (DeviceKnown dev: devicesKnown) {
            if (devId.equals(devId)) found = true;
        }
        if (!found) {
            DeviceKnown d = new DeviceKnown(devId, devTitle);
            devicesKnown.add(d);
            Log.i("CLOUD", "Starting save known_devices.json");
            try {
                final Gson gson = new GsonBuilder().setPrettyPrinting().create();
                final String prettyJson = gson.toJson(devicesKnown);
                Utils.saveStringToFileSafe(prettyJson, cr.getSettingsFile(0).getParent() + "/known_devices.json");
            } catch (Exception e) {
            }
        }
    }

    public static void saveSettingsToFilesOrCloud(CoolReader cr, boolean bQuiet, boolean toFile) {
        if (toFile) saveSettingsFiles(cr, bQuiet);
            else saveSettingsFilesToCloud(cr, bQuiet);
    }

    public static void saveSettingsFiles(CoolReader cr, boolean bQuiet) {
        int iSettClount = 1;
        ArrayList<File> arrSett = new ArrayList<File>();
        File fSett = cr.getSettingsFile(0);
        while (fSett.exists()) {
            arrSett.add(fSett);
            fSett = cr.getSettingsFile(iSettClount);
            iSettClount++;
        }
        ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.CloudSyncDirs, true);
        String sDir = "";
        if (tDirs.size() > 0) sDir = tDirs.get(0);
        if (!StrUtils.isEmptyStr(sDir))
            if ((!sDir.endsWith("/")) && (!sDir.endsWith("\\"))) sDir = sDir + "/";
        boolean bWasErr = false;
        if (!StrUtils.isEmptyStr(sDir)) {
            Log.d(TAG, "Starting save cr3.ini files to drive...");
            final android.text.format.DateFormat dfmt = new android.text.format.DateFormat();
            final CharSequence sFName0 = dfmt.format("yyyy-MM-dd_kkmmss", new java.util.Date());
            for (File fS : arrSett) {
                final String sFName = sFName0.toString() + "_" + fS.getName() + "_" + cr.getAndroid_id();
                try {
                    FileInputStream fin = new FileInputStream(fS);
                    byte[] buffer = new byte[fin.available()];
                    fin.read(buffer, 0, buffer.length);
                    File f = new File(sDir + sFName);
                    if (f.exists()) f.delete();
                    File file = new File(sDir + sFName);
                    OutputStream fOut = new FileOutputStream(file);
                    fOut.write(buffer, 0, buffer.length);
                    fOut.flush();
                    fOut.close();
                } catch (Exception e) {
                    bWasErr = true;
                    if (!bQuiet)
                        cr.showCloudToast(cr.getString(R.string.cloud_error) + ": Error saving file (" + e.getClass().getSimpleName() + ")",true);
                }
            } //for (File fS : arrSett) {
            try {
                final String sFName = sFName0.toString() + "_cr3_ini_" + cr.getAndroid_id();
                File f2 = new File(sDir + sFName + ".info");
                if (f2.exists()) f2.delete();
                File file2 = new File(sDir + sFName + ".info");
                OutputStream fOut2 = new FileOutputStream(file2);
                String descr = String.valueOf(arrSett.size()) + " profiles ~from " + cr.getModel();
                fOut2.write(descr.getBytes(), 0, descr.getBytes().length);
                fOut2.flush();
                fOut2.close();
            } catch (Exception e) {
                bWasErr = true;
                if (!bQuiet)
                    cr.showCloudToast(cr.getString(R.string.cloud_error) + ": Error saving file (" + e.getClass().getSimpleName() + ")",true);
            }
        }
        if (!bWasErr) {
            if (!bQuiet)
                cr.showCloudToast(cr.getString(R.string.cloud_ok) + ": "+cr.getString(R.string.settings_files_were_saved),true);

        }
    }

    public static void saveSettingsFilesToCloud(CoolReader cr, boolean bQuiet) {
        int iSettClount = 1;
        ArrayList<File> arrSett = new ArrayList<File>();
        File fSett = cr.getSettingsFile(0);
        while (fSett.exists()) {
            arrSett.add(fSett);
            fSett = cr.getSettingsFile(iSettClount);
            iSettClount++;
        }
        boolean bWasErr = false;
        Log.d(TAG, "Starting save cr3.ini files to drive...");
        final android.text.format.DateFormat dfmt = new android.text.format.DateFormat();
        final CharSequence sFName0 = dfmt.format("yyyy-MM-dd_kkmmss", new java.util.Date());
        ArrayList<String> arrSFull = new ArrayList<String>();
        if (arrSett.size()>0) {
            String sFName = sFName0.toString() + "_settings_" + cr.getAndroid_id()+'_'+iSettClount;
            for (File fS : arrSett) {
                ArrayList<String> arrS = Utils.readFileToArrayList(fS.getPath());
                arrSFull.add("~~~ settings: " + fS.getName() + "|" + cr.getAndroid_id() + "|" + cr.getModel());
                for (String s: arrS) arrSFull.add(s);
            }
            sFName = sFName + ".txt";
            String sText = "";
            for (String s: arrSFull) sText = sText + s + "\n";
            CloudAction.yndSaveJsonFile(cr, sFName, sText, bQuiet);
        }
    }

    public static void loadSettingsFiles(CoolReader cr, boolean bQuiet) {
        final File fSett = cr.getSettingsFile(0);
        Log.d(TAG, "Starting load settings files from drive...");
        ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.CloudSyncDirs, true);
        String sDir = "";
        if (tDirs.size() > 0) sDir = tDirs.get(0);
        if (!StrUtils.isEmptyStr(sDir))
            if ((!sDir.endsWith("/")) && (!sDir.endsWith("\\"))) sDir = sDir + "/";
        if (!StrUtils.isEmptyStr(sDir)) {
            File fDir = new File(sDir);
            File[] matchingFilesInfo = fDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.contains("_cr3_ini_") && (name.endsWith(".info"));
                }
            });
            File[] matchingFiles = fDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.contains("_cr3.ini");
                }
            });
            if (matchingFiles.length == 0) {
                cr.showToast(cr.getString(R.string.no_cloud_files));
            } else {
                ChooseConfFileDlg dlg = new ChooseConfFileDlg(cr, matchingFilesInfo, matchingFiles);
                dlg.show();
            }
        }
    }

    public static void restoreSettingsFiles(CoolReader cr, CloudFileInfo fi, ArrayList<CloudFileInfo> afi, boolean bQuiet) {
        Log.d(TAG, "Starting load settings file from drive...");
        boolean bWasErr = false;
        for (CloudFileInfo fiSett: afi) {
            int profileNum = 0;
            try {
                if (fiSett.name.contains("profile")) {
                    for (String s : fiSett.name.split("_")) {
                        if (s.contains("profile"))
                            profileNum = Integer.valueOf(s.replace("cr3.ini.profile", ""));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            File fSett = cr.getSettingsFile(profileNum);
            String sFName = fSett.getPath();
            File file = new File(sFName + ".bk");
            if (file.exists()){
                if (!file.delete()) {
                    if (!bQuiet) cr.showCloudToast(cr.getString(R.string.cloud_error) + ": "+cr.getString(R.string.delete_file)+" (.bk)",true);
                    bWasErr = true;
                };
            }
            if (!bWasErr) {
                try {
                    Utils.copyFile(sFName, sFName + ".bk");
                } catch (Exception e) {
                    bWasErr = true;
                    if (!bQuiet) cr.showCloudToast(cr.getString(R.string.cloud_error) + ": " + cr.getString(R.string.copy_current_file_to)+" .bk", true);
                }
            }
            if (!bWasErr) {
                if (fSett.exists()) {
                    if (!fSett.delete()) {
                        if (!bQuiet) cr.showCloudToast(cr.getString(R.string.cloud_error) + ": "+cr.getString(R.string.delete_file)+" - "+fSett.getName(), true);
                        bWasErr = true;
                    };
                }
            }
            if (!bWasErr) {
                try {
                    Utils.copyFile(fiSett.path, sFName);
                } catch (Exception e) {
                    bWasErr = true;
                    if (!bQuiet) cr.showCloudToast(cr.getString(R.string.cloud_error) + ": "+ cr.getString(R.string.copy_new_file_to_current), true);
                }
            }
        }
        if (!bWasErr) {
            if (!bQuiet) cr.showToast(cr.getString(R.string.cloud_ok) + ": " + cr.getString(R.string. settings_were_restored));
            cr.finish();
        } else {
            if (!bQuiet) cr.showCloudToast(cr.getString(R.string.cloud_error) + ": "+cr.getString(R.string.settings_restore_error), true);
            cr.finish();
        }
    }

    public static boolean restoreSettingsFile(CoolReader cr, String fileName, ArrayList<String> content, boolean bQuiet) {
        Log.d(TAG, "Starting load settings file from drive...");
        File fDir = cr.getSettingsFile(0).getParentFile();
        File file = new File(fDir.getAbsolutePath() + "/" + fileName + ".bk");
        boolean bWasErr = false;
        if (file.exists()){
            if (!file.delete()) {
                if (!bQuiet) cr.showCloudToast(cr.getString(R.string.cloud_error) + ": "+cr.getString(R.string.delete_file)+" .bk",true);
                bWasErr = true;
            };
        }
        file = new File(fDir.getAbsolutePath() + "/" + fileName);
        if (file.exists()) {
            if (!bWasErr) {
                try {
                    Utils.copyFile(fDir.getAbsolutePath() + "/" + fileName, fDir.getAbsolutePath() + "/" + fileName + ".bk");
                } catch (Exception e) {
                    bWasErr = true;
                    if (!bQuiet)
                        cr.showCloudToast(cr.getString(R.string.cloud_error) + ": "+cr.getString(R.string.copy_current_file_to)+" .bk", true);
                }
            }
            if (!bWasErr) {
                if (!file.delete()) {
                    if (!bQuiet)
                        cr.showCloudToast(cr.getString(R.string.cloud_error) + ": delete " + file.getName() + " file", true);
                    bWasErr = true;
                }
            }
        }
        try {
            FileWriter writer = new FileWriter(fDir.getAbsolutePath() + "/" + fileName);
            for (String str : content) {
                writer.write(str + "\n");
            }
            writer.close();
        } catch (Exception e) {
            if (!bQuiet)
                cr.showCloudToast(cr.getString(R.string.cloud_error) + ": create " + fDir.getAbsolutePath() + "/" + fileName + " file", true);
            bWasErr = true;
        }
        if (bWasErr) {
            if (!bQuiet) cr.showCloudToast(cr.getString(R.string.cloud_error) + ": "+cr.getString(R.string.settings_restore_error), true);
            return false;
        }
        return true;
    }

    public static void checkOldFiles(ArrayList<String> tDirs, String sCRC, int iSaveType) {
        String sDir = "";
        if (tDirs.size() > 0) sDir = tDirs.get(0);
        if (!StrUtils.isEmptyStr(sDir))
            if ((!sDir.endsWith("/")) && (!sDir.endsWith("\\"))) sDir = sDir + "/";
        if (!StrUtils.isEmptyStr(sDir)) {
            if (iSaveType == CLOUD_SAVE_READING_POS) {
                File fDir = new File(sDir);
                File[] matchingFiles = fDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.contains("_rpos_") && (name.contains(sCRC));
                    }
                });
                ArrayList<File> mReadingPosList = new ArrayList<File>();
                for (File f : matchingFiles) mReadingPosList.add(f);

                Comparator<File> compareByDate = new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        return -(o1.getName().compareTo(o2.getName()));
                    }
                };
                Collections.sort(mReadingPosList, compareByDate);
                for (int i = 0; i < 40; i++) {
                    if (mReadingPosList.size() > 0) mReadingPosList.remove(0);
                }
                for (File f : mReadingPosList) f.delete();
            }
        }
    }

    public static void saveJsonToFile(CoolReader cr, ArrayList<String> tDirs,
            String sCRC, int iSaveType, String fileMark, String prettyJson,
                                      String descr, boolean bQuiet) {
        final android.text.format.DateFormat dfmt = new android.text.format.DateFormat();
        final CharSequence sFName0 = dfmt.format("yyyy-MM-dd_kkmmss", new java.util.Date());
        final String sFName = sFName0.toString() + fileMark + String.valueOf(sCRC) + "_" + cr.getAndroid_id();
        String sDir = "";
        if (tDirs.size() > 0) sDir = tDirs.get(0);
        if (!StrUtils.isEmptyStr(sDir))
            if ((!sDir.endsWith("/")) && (!sDir.endsWith("\\"))) sDir = sDir + "/";
        if (!StrUtils.isEmptyStr(sDir)) {
            boolean bWasErr = false;
            try {
                File f = new File(sDir + sFName + ".json");
                if (f.exists()) f.delete();
                File file = new File(sDir + sFName + ".json");
                OutputStream fOut = new FileOutputStream(file);
                fOut.write(prettyJson.getBytes(), 0, prettyJson.getBytes().length);
                fOut.flush();
                fOut.close();
                File f2 = new File(sDir + sFName + ".info");
                if (f2.exists()) f2.delete();
                File file2 = new File(sDir + sFName + ".info");
                OutputStream fOut2 = new FileOutputStream(file2);
                fOut2.write(descr.getBytes(), 0, descr.getBytes().length);
                fOut2.flush();
                fOut2.close();
            } catch (Exception e) {
                bWasErr = true;
                if (!bQuiet)
                    cr.showCloudToast(cr.getString(R.string.cloud_error) + ": "+cr.getString(R.string.error_saving_file)+" (" + e.getClass().getSimpleName() + ")", true);
            }
            if (!bWasErr) {
                if (!bQuiet)
                    cr.showCloudToast(cr.getString(R.string.cloud_ok) + ": " + cr.getString(R.string.successfully_saved), !bQuiet);

            }
        }
    }

    public static void saveJsonToCloud(CoolReader cr,
                                      String sCRC, int iSaveType, String fileMark, String prettyJson,
                                      String descr, boolean bQuiet, String addName) {
        final android.text.format.DateFormat dfmt = new android.text.format.DateFormat();
        final CharSequence sFName0 = dfmt.format("yyyy-MM-dd_kkmmss", new java.util.Date());
        String sFName = sFName0.toString() + fileMark + String.valueOf(sCRC) + "_" + cr.getAndroid_id();
        sFName = sFName + addName + ".json";
        CloudAction.yndSaveJsonFile(cr, sFName, prettyJson, bQuiet);
    }

    public static void saveJsonInfoFileOrCloud(CoolReader cr, int iSaveType, boolean bQuiet, boolean toFile) {
        Log.d(TAG, "Starting save json to drive...");

        final ReaderView rv = cr.getReaderView();
        if (rv == null) {
            if (!bQuiet) cr.showCloudToast(cr.getString(R.string.cloud_error) + ": "+cr.getString(R.string.book_was_not_found),true);
            return;
        }
        if (rv.getBookInfo() == null) {
            if (!bQuiet) cr.showCloudToast(cr.getString(R.string.cloud_error) + ": "+cr.getString(R.string.book_was_not_found),true);
            return;
        }
        if (rv.getBookInfo().getFileInfo() == null) {
            if (!bQuiet) cr.showCloudToast(cr.getString(R.string.cloud_error) + ": "+cr.getString(R.string.book_was_not_found),true);
            return;
        }
        String prettyJson = "";
        Bookmark bmk = null;
        ArrayList<Bookmark> abmk = null;
        String fileMark = "";
        String descr = "";
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final String sBookFName = rv.getBookInfo().getFileInfo().getFilename();
        CRC32 crc = new CRC32();
        crc.update(sBookFName.getBytes());
        final String sCRC = String.valueOf(crc.getValue());
        String addName = "";
        if (iSaveType == CLOUD_SAVE_READING_POS) {
            fileMark = "_rpos_";
            bmk = rv.getCurrentPositionBookmark();
            if (bmk == null) {
                if (!bQuiet)
                    cr.showCloudToast(cr.getString(R.string.cloud_error) + ": "+cr.getString(R.string.pos_was_not_got),true);
                return;
            }
            double dPerc = bmk.getPercent();
            dPerc = dPerc / 100;
            Locale l =new Locale("en", "US");
            String sPerc1 = String.format(l,"%5.2f", dPerc) + "% of ";
            final String sPerc = sPerc1.replace(",", ".");
            addName = "_" + String.format(l,"%5.2f", dPerc).trim();
            addName = addName.replace(",", ".");
            descr = sPerc + sBookFName + " ~from " + cr.getModel();
            bmk.setAddCommentText(descr);
            prettyJson = gson.toJson(bmk);
        }
        if (iSaveType == CLOUD_SAVE_BOOKMARKS) {
            fileMark = "_bmk_";
            abmk = rv.getBookInfo().getAllBookmarksWOPos();
            if (abmk == null) {
                if (!bQuiet)
                    cr.showCloudToast(cr.getString(R.string.cloud_error) + ": "+cr.getString(R.string.bookmarks_was_not_got),true);
                return;
            }
            if (abmk.isEmpty()) {
                cr.showToast(cr.getString(R.string.no_bookmarks));
                return;
            }
            descr = String.valueOf(abmk.size()) + " bookmark(s) of " + sBookFName + " ~from " + cr.getModel();
            addName = "_" + String.valueOf(abmk.size()).trim();
            for (Bookmark bmk2: abmk) bmk2.setAddCommentText(descr);
            prettyJson = gson.toJson(abmk);
        }
        if (toFile) {
            ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.CloudSyncDirs, true);
            checkOldFiles(tDirs, sCRC, iSaveType);
            saveJsonToFile(cr, tDirs, sCRC, iSaveType, fileMark, prettyJson, descr, bQuiet);
        } else {
            saveJsonToCloud(cr, sCRC, iSaveType, fileMark, prettyJson, descr, bQuiet, addName);
        }

    }

    public static void loadFromJsonInfoFileListFS(CoolReader cr, int iSaveType, boolean bQuiet, String sCRC) {
        ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.CloudSyncDirs, true);
        String sDir = "";
        if (tDirs.size() > 0) sDir = tDirs.get(0);
        if (!StrUtils.isEmptyStr(sDir))
            if ((!sDir.endsWith("/")) && (!sDir.endsWith("\\"))) sDir = sDir + "/";
        if (!StrUtils.isEmptyStr(sDir)) {
            File fDir = new File(sDir);
            if (iSaveType == CLOUD_SAVE_READING_POS) {
                File[] matchingFiles = fDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.contains("_rpos_") && (name.endsWith(".json")) && (name.contains(sCRC));
                    }
                });
                if (matchingFiles.length == 0) {
                    cr.showToast(cr.getString(R.string.no_cloud_files));
                } else {
                    ChooseReadingPosDlg dlg = new ChooseReadingPosDlg(cr, matchingFiles);
                    dlg.show();
                }
            }
            if (iSaveType == CLOUD_SAVE_BOOKMARKS) {
                File[] matchingFiles = fDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.contains("_bmk_") && (name.endsWith(".json")) && (name.contains(sCRC));
                    }
                });
                if (matchingFiles.length == 0) {
                    cr.showToast(cr.getString(R.string.no_cloud_files));
                } else {
                    ChooseBookmarksDlg dlg = new ChooseBookmarksDlg(cr, matchingFiles);
                    dlg.show();
                }
            }
        }
    }

    public static void loadFromJsonInfoFileList(CoolReader cr, int iSaveType, boolean bQuiet,
                                                boolean fromFile, boolean findingLastPos) {
        Log.d(TAG, "Starting load json file list from drive...");
        String sCRC = "";
        if (iSaveType != CLOUD_SAVE_SETTINGS) {
            final ReaderView rv = cr.getReaderView();
            if (rv == null) {
                if (!bQuiet)
                    cr.showCloudToast(cr.getString(R.string.cloud_error) + ": "+cr.getString(R.string.book_was_not_found), true);
                return;
            }
            final String sBookFName = rv.getBookInfo().getFileInfo().getFilename();
            CRC32 crc = new CRC32();
            crc.update(sBookFName.getBytes());
            sCRC = String.valueOf(crc.getValue());
        }
        if (fromFile) {
            if (!findingLastPos)
                loadFromJsonInfoFileListFS(cr, iSaveType, bQuiet, sCRC);
        } else {
            String fileMark = "_rpos_";
            if (iSaveType == CLOUD_SAVE_BOOKMARKS) fileMark = "_bmk_";
			if (iSaveType == CLOUD_SAVE_SETTINGS) fileMark = "_settings_";
            CloudAction.yndLoadJsonFileList(cr, fileMark, sCRC, findingLastPos);
        }
    }

    public static void checkKnownDevices(CoolReader cr, String comment, String name) {
        String sDev = "";
        if (comment.contains("~from")) {
            int ipos = comment.indexOf("~from");
            sDev = comment.substring(ipos+6).trim();
        }
        String[] arrS = name.split("_"); // 2020-03-30_222303_rpos_635942216_36b928e773055c4a_0.16.json
        String sDevId ="";
        if (arrS.length>=5) {
            sDevId = arrS[4];
        }
        if ((!StrUtils.isEmptyStr(sDev))&&(!StrUtils.isEmptyStr(sDevId))) {
            if (devicesKnown == null) readKnownDevices(cr);
                else if (devicesKnown.isEmpty()) readKnownDevices(cr);
            boolean found = false;
            for (DeviceKnown dev: devicesKnown) {
                if (dev.deviceId.equals(sDevId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                DeviceKnown dev = new DeviceKnown(sDevId, sDev);
                devicesKnown.add(dev);
                saveKnownDevices(cr);
            }
        }
    }

    public static void applyRPosOrBookmarks(CoolReader cr, int iSaveType, String sContent,
                                            String name, boolean bQuiet) {
        final ReaderView rv = cr.getReaderView();
        if (rv == null) {
            if (!bQuiet) cr.showCloudToast(cr.getString(R.string.cloud_error) + ": "+cr.getString(R.string.book_was_not_found),true);
            return;
        }

        if (StrUtils.isEmptyStr(sContent)) {
            if (!bQuiet)
                cr.showCloudToast(cr.getString(R.string.cloud_error) + ": json "+cr.getString(R.string.file_was_not_found_or_empty),true);
            return;
        }

        if (iSaveType == CLOUD_SAVE_READING_POS) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Bookmark bmk = gson.fromJson(sContent, Bookmark.class);
            if (bmk != null) {
                checkKnownDevices(cr, StrUtils.getNonEmptyStr(bmk.getAddCommentText(),true), name);
                bmk.setTimeStamp(System.currentTimeMillis());
                rv.savePositionBookmark(bmk);
                if ( rv.getBookInfo()!=null )
                    rv.getBookInfo().setLastPosition(bmk);
                rv.goToBookmark(bmk);
                if (!bQuiet) cr.showToast(cr.getString(R.string.cloud_ok) + ": "+cr.getString(R.string.reading_pos_updated));
            }
        }

        if (iSaveType == CLOUD_SAVE_BOOKMARKS) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            ArrayList<Bookmark> abmkThis = rv.getBookInfo().getAllBookmarks();
            ArrayList<Bookmark> abmk = new ArrayList<Bookmark>(StrUtils.stringToArray(sContent, Bookmark[].class));
            int iCreated = 0;
            boolean devChecked = false;
            for (Bookmark bmk: abmk) {
                if (!devChecked)
                    checkKnownDevices(cr, StrUtils.getNonEmptyStr(bmk.getAddCommentText(),true), name);
                devChecked = true;
                boolean bFound = false;
                for (Bookmark bm: abmkThis) {
                    if (bm.getStartPos().equals(bmk.getStartPos())) bFound = true;
                }
                if ((!bFound)&(bmk.getType()!=Bookmark.TYPE_LAST_POSITION)) {
                    rv.getBookInfo().addBookmark(bmk);
                    iCreated++;
                }
            }
            rv.highlightBookmarks();
            if (!bQuiet) cr.showToast(cr.getString(R.string.cloud_ok) + ": "+cr.getString(R.string.bookmarks_created)+" - " + iCreated);
        }
    }

    public static void loadFromJsonInfoFile(CoolReader cr, int iSaveType, String filePath, boolean bQuiet,
                                            String name, int cloudMode) {
        final File fSett = cr.getSettingsFile(0);
        Log.d(TAG, "Starting load json file from drive...");

        final ReaderView rv = cr.getReaderView();
        if (rv == null) {
            if (!bQuiet) cr.showCloudToast(cr.getString(R.string.cloud_error) + ": "+cr.getString(R.string.book_was_not_found),true);
            return;
        }

        if (cloudMode == Settings.CLOUD_SYNC_VARIANT_YANDEX) {
            CloudAction.yndLoadJsonFile(cr, iSaveType, filePath, bQuiet, name, cloudMode);
        } else {
            String sFile = Utils.readFileToString(filePath);
            applyRPosOrBookmarks(cr, iSaveType, sFile, name, bQuiet);
        }
    }
}