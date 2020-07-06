package org.coolreader.cloud;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.dropbox.core.v2.users.FullAccount;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.dropbox.DBXConfig;
import org.coolreader.cloud.dropbox.DBXPerformAction;
import org.coolreader.cloud.yandex.YNDConfig;
import org.coolreader.cloud.yandex.YNDListFiles;
import org.coolreader.cloud.yandex.YNDPerformAction;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.PictureCameDialog;
import org.coolreader.crengine.ReaderView;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.SomeButtonsToolbarDlg;
import org.coolreader.crengine.StrUtils;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CloudAction {

    private static final String TAG = "CloudAction";

    public static final int NO_ACTION = 0;
    public static final int DBX_GET_CURRENT_ACCOUNT = 20000;
    public static final int DBX_LIST_FOLDER = 20001;
    public static final int DBX_LIST_FOLDER_THEN_OPEN_DLG = 20002;
    public static final int DBX_LIST_FOLDER_IN_DLG = 20003;
    public static final int DBX_DOWNLOAD_FILE = 20004;

    public static final int YND_LIST_FOLDER = 20101;
    public static final int YND_LIST_FOLDER_THEN_OPEN_DLG = 20102;
    public static final int YND_LIST_FOLDER_IN_DLG = 20103;
    public static final int YND_GET_DOWNLOAD_LINK = 20104;
    public static final int YND_DOWNLOAD_FILE = 20105;
    public static final int YND_CHECK_CR_FOLDER = 20106;
    public static final int YND_CREATE_CR_FOLDER = 20107;
    public static final int YND_SAVE_TO_FILE_GET_LINK = 20108;
    public static final int YND_SAVE_STRING_TO_FILE = 20109;
    public static final int YND_LIST_JSON_FILES = 20110;
    public static final int YND_LIST_JSON_FILES_LASTPOS = 20111;
    public static final int YND_DOWNLOAD_FILE_TO_STRING = 20112;
    public static final int YND_DELETE_FILE_ASYNC = 20113;
    public static final int YND_SAVE_CUR_BOOK = 20114;
    public static final int YND_SAVE_TO_FILE_GET_LINK_W_DIR = 20115;
    public static final String CLOUD_COMPLETE_LIST_FOLDER_RESULT = "ListFolderResult";
    public static final String CLOUD_COMPLETE_FULL_ACCOUNT = "FullAccount";
    public static final String CLOUD_COMPLETE_GET_DOWNLOAD_LINK = "GetDownloadLink";
    public static final String CLOUD_COMPLETE_CHECK_CR_FOLDER = "CheckCrFolder";
    public static final String CLOUD_COMPLETE_CREATE_CR_FOLDER = "CreateCrFolder";
    public static final String CLOUD_COMPLETE_DOWNLOAD_FILE = "DownloadFile";
    public static final String CLOUD_COMPLETE_SAVE_TO_FILE_GET_LINK = "SaveToFileGetLink";
    public static final String CLOUD_COMPLETE_SAVE_STRING_TO_FILE = "SaveStringToFile";
    public static final String CLOUD_COMPLETE_LIST_JSON_FILES = "ListJsonFiles";
    public static final String CLOUD_COMPLETE_LIST_JSON_FILES_LASTPOS = "ListJsonFilesLastpos";
    public static final String CLOUD_COMPLETE_DOWNLOAD_FILE_TO_STRING = "DownloadFileToString";
    public static final String CLOUD_COMPLETE_DELETE_FILE_ASYNC = "DeleteFileAsync";
    public static final String CLOUD_COMPLETE_YND_SAVE_CUR_BOOK = "SaveCurBook";

    public int action; // action, that will be performed
    public String param; // param(s) passed to an action...
    public String param2; // param(s) passed to an action...
    public String param3; // param(s) passed to an action...
    public String bookCRC; // Book's CRC
    public boolean mQuiet;
    public com.dropbox.core.v2.files.Metadata mDbxMd; // metadata for dropbox
    public CloudFileInfo mYndMd; // metadata for yandex
    public String mFile;

    public CoolReader mActivity;

    public CloudAction(CoolReader cr, int a, boolean bQuiet) {
        action = a;
        param = "";
        mActivity = cr;
        mQuiet = bQuiet;
    }

    public CloudAction(CoolReader cr, int a, String p, boolean bQuiet) {
        action = a;
        param = p;
        mActivity = cr;
        mQuiet = bQuiet;
    }

    public CloudAction(CoolReader cr, int a, String p, String p2, boolean bQuiet) {
        action = a;
        param = p;
        mActivity = cr;
        param2 = p2;
        mQuiet = bQuiet;
    }

    public CloudAction(CoolReader cr, int a, String p, com.dropbox.core.v2.files.Metadata md, String sFile, boolean bQuiet) {
        action = a;
        param = p;
        mDbxMd = md;
        mActivity = cr;
        mFile = sFile;
        mQuiet = bQuiet;
    }

    public CloudAction(CoolReader cr, int a, String p, CloudFileInfo md, String sFile, boolean bQuiet) {
        action = a;
        param = p;
        mYndMd = md;
        mActivity = cr;
        mFile = sFile;
        mQuiet = bQuiet;
    }

    @SuppressWarnings("unchecked")
    public static void onDBXComplete(CoolReader cr, DBXPerformAction a, String res, Object o, Object dlg) {
        System.out.println("DBX:" + a.mCurAction.action);
        System.out.println("DBX:" + a.mCurAction.param);
        if (CLOUD_COMPLETE_FULL_ACCOUNT.equals(res)) {
            FullAccount fa = (FullAccount) o;
        }
        if ((CLOUD_COMPLETE_LIST_FOLDER_RESULT.equals(res))&&(a.mCurAction.action == CloudAction.DBX_LIST_FOLDER_THEN_OPEN_DLG)) {
            List<com.dropbox.core.v2.files.Metadata> l = (List<com.dropbox.core.v2.files.Metadata>) o;
            onDBXListFolderResultThenOpenDlg(cr, l);
        }
        if ((CLOUD_COMPLETE_LIST_FOLDER_RESULT.equals(res))&&(a.mCurAction.action == CloudAction.DBX_LIST_FOLDER_IN_DLG)) {
            List<com.dropbox.core.v2.files.Metadata> l = (List<com.dropbox.core.v2.files.Metadata>) o;
            onDBXListFolderResultInDlg(cr, l, (OpenBookFromCloudDlg) dlg);
        }
        a.DoNextAction();
    }

    public static void onDBXError(CoolReader cr, DBXPerformAction a, String res, Exception e) {
        Log.e(e.getClass().getName(), "Cloud operation error: "+res, e);
        final CoolReader crf = cr;
        final String resf = res;
        BackgroundThread.instance().postGUI(new Runnable() {
            @Override
            public void run() {
                if (crf!=null) crf.showCloudToast(cr.getString(R.string.cloud_error)+": "+resf,true);
            }}, 200);
    }

    public static void onDBXListFolderResultThenOpenDlg(CoolReader cr,
                                                        List<com.dropbox.core.v2.files.Metadata> lfr) {
        OpenBookFromCloudDlg dlg = new OpenBookFromCloudDlg(cr, lfr);
        dlg.show();
    }

    public static void onDBXListFolderResultInDlg(CoolReader cr, List<com.dropbox.core.v2.files.Metadata> lfr,
                                                  OpenBookFromCloudDlg dlg) {
        if (dlg.isShowing()) {
            dlg.setDBXLfrList(lfr);
            dlg.listUpdated();
        }
    }

    public static void onYNDListFolderResultThenOpenDlg(CoolReader cr, YNDListFiles lfr, String withSaveOption) {
        OpenBookFromCloudDlg dlg = new OpenBookFromCloudDlg(cr, lfr, withSaveOption == "true");
        dlg.show();
    }

    public static void onYNDListFolderResultInDlg(CoolReader cr, YNDListFiles lfr, OpenBookFromCloudDlg dlg) {
        if (dlg.isShowing()) {
            dlg.setYNDLfrList(lfr);
            dlg.listUpdated();
        }
    }

    public static void dbxOpenBookDialog(final CoolReader cr) {
        try {
            cr.showCloudToast(R.string.cloud_begin,false);
            if (!DBXConfig.init(cr)) return;
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca1 = new CloudAction(cr, CloudAction.DBX_GET_CURRENT_ACCOUNT, true);
            al.add(ca1);
            CloudAction ca2 = new CloudAction(cr, CloudAction.DBX_LIST_FOLDER_THEN_OPEN_DLG, true);
            al.add(ca2);
            final DBXPerformAction a = new DBXPerformAction(cr, al, new DBXPerformAction.Callback() {
                @Override
                public void onComplete(DBXPerformAction a, String res, Object o) {
                    CloudAction.onDBXComplete(cr, a, res, o, null);
                }

                @Override
                public void onError(DBXPerformAction a, String res, Exception e) {
                    CloudAction.onDBXError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showCloudToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage(),true);
            System.out.println("DBBOX err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static void onYNDComplete(CoolReader cr, YNDPerformAction a, String res, Object o, Object dlg) {
        System.out.println("YND:" + a.mCurAction.action);
        System.out.println("YND:" + a.mCurAction.param);
        if ((CLOUD_COMPLETE_LIST_FOLDER_RESULT.equals(res))&&(a.mCurAction.action == CloudAction.YND_LIST_FOLDER_THEN_OPEN_DLG)) {
            final YNDListFiles l = (YNDListFiles) o;
            Log.i("YND","ynd files = "+ l.fileList.size());
            final CoolReader crf = cr;
            BackgroundThread.instance().postGUI(new Runnable() {
                @Override
                public void run() {
                    onYNDListFolderResultThenOpenDlg(crf, l, a.mCurAction.param3);
                }}, 200);
        }
        if ((CLOUD_COMPLETE_LIST_FOLDER_RESULT.equals(res))&&(a.mCurAction.action == CloudAction.YND_LIST_FOLDER_IN_DLG)) {
            final YNDListFiles l = (YNDListFiles) o;
            final CoolReader crf = cr;
            final OpenBookFromCloudDlg dlgf = (OpenBookFromCloudDlg) dlg;
            BackgroundThread.instance().postGUI(new Runnable() {
                @Override
                public void run() {
                    onYNDListFolderResultInDlg(crf, l, dlgf);
                }}, 200);
        }
        if ((CLOUD_COMPLETE_GET_DOWNLOAD_LINK.equals(res))&&(a.mCurAction.action == CloudAction.YND_GET_DOWNLOAD_LINK)) {
            if (a.mActionList.size()>0) {
                // setting download link param
                a.mActionList.get(0).param2 = a.mCurAction.param2;
            }
        }
        if ((CLOUD_COMPLETE_CHECK_CR_FOLDER.equals(res))&&(a.mCurAction.action == CloudAction.YND_CHECK_CR_FOLDER)) {
            if (a.mActionList.size()>0) {
                // setting download link param
                a.mActionList.get(0).param2 = "skip";
                if (o instanceof YNDListFiles) {
                    YNDListFiles lf = (YNDListFiles) o;
                    boolean needCreate = (lf.fileList.size()==0);
                    if (lf.fileList.size()==1)
                        if (!StrUtils.getNonEmptyStr(lf.fileList.get(0).type,true).equals("dir")) needCreate = true;
                    if (needCreate) a.mActionList.get(0).param2 = "create";
                }
            }
        }
        if ((CLOUD_COMPLETE_SAVE_TO_FILE_GET_LINK.equals(res))&&
            (
                (a.mCurAction.action == CloudAction.YND_SAVE_TO_FILE_GET_LINK)
                ||
                (a.mCurAction.action == CloudAction.YND_SAVE_TO_FILE_GET_LINK_W_DIR)
            )
        ) {
            if (a.mActionList.size()>0) {
                // setting download link param
                if (o instanceof String)
                    a.mActionList.get(0).param2 = (String) o;
            }
        }
        if ((CLOUD_COMPLETE_SAVE_STRING_TO_FILE.equals(res))&&(!a.mCurAction.mQuiet)) {
            final CoolReader crf = cr;
            BackgroundThread.instance().postGUI(new Runnable() {
                 @Override
                 public void run() {
                     if (crf!=null) crf.showCloudToast(R.string.cloud_ok, true);
                 }}, 200);
        }
        a.DoNextAction();
    }

    public static void onYNDError(CoolReader cr, YNDPerformAction a, String res, Exception e) {
        if (e == null) Log.e("", "Cloud operation error: "+res);
            else Log.e(e.getClass().getName(), "Cloud operation error: "+res, e);
        final CoolReader crf = cr;
        final String resf = res;
        BackgroundThread.instance().postGUI(new Runnable() {
            @Override
            public void run() {
                if (crf!=null) crf.showCloudToast(cr.getString(R.string.cloud_error)+": "+resf, true);
            }}, 200);
    }

    public static void yndOpenBookDialog(final CoolReader cr, boolean withSaveOption) {
        try {
            cr.showCloudToast(R.string.cloud_begin,false);
            if (!YNDConfig.init(cr)) return;
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca1 = new CloudAction(cr, CloudAction.YND_LIST_FOLDER_THEN_OPEN_DLG, true);
            ca1.param3 = Boolean.toString(withSaveOption);
            al.add(ca1);
            final YNDPerformAction a = new YNDPerformAction(cr, al, new YNDPerformAction.Callback() {
                @Override
                public void onComplete(YNDPerformAction a, String res, Object o) {
                    CloudAction.onYNDComplete(cr, a, res, o, null);
                }

                @Override
                public void onError(YNDPerformAction a, String res, Exception e) {
                    CloudAction.onYNDError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showCloudToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage(),true);
            System.out.println("YND err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    public static void yndSaveJsonFile(final CoolReader cr, String sFName, String prettyJson, boolean bQuiet) {
        try {
            cr.showCloudToast(R.string.cloud_begin,false);
            if (!YNDConfig.init(cr)) return;
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca1 = new CloudAction(cr, CloudAction.YND_CHECK_CR_FOLDER, true);
            al.add(ca1);
            CloudAction ca2 = new CloudAction(cr, CloudAction.YND_CREATE_CR_FOLDER, true);
            al.add(ca2);
            CloudAction ca3 = new CloudAction(cr, CloudAction.YND_SAVE_TO_FILE_GET_LINK, true);
            ca3.param = sFName;
            al.add(ca3);
            CloudAction ca4 = new CloudAction(cr, CloudAction.YND_SAVE_STRING_TO_FILE, bQuiet);
            ca4.param = prettyJson;
            al.add(ca4);
            final YNDPerformAction a = new YNDPerformAction(cr, al, new YNDPerformAction.Callback() {
                @Override
                public void onComplete(YNDPerformAction a, String res, Object o) {
                    CloudAction.onYNDComplete(cr, a, res, o, null);
                }

                @Override
                public void onError(YNDPerformAction a, String res, Exception e) {
                    CloudAction.onYNDError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showCloudToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage(),true);
            System.out.println("YND err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    public static void yndLoadJsonFileList(final CoolReader cr, String fileMark, String sCRC,
                                           boolean findingLastPos) {
        try {
            cr.showCloudToast(R.string.cloud_begin,false);
            if (!YNDConfig.init(cr)) return;
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca1 = new CloudAction(cr, CloudAction.YND_CHECK_CR_FOLDER, true);
            al.add(ca1);
            CloudAction ca2;
            if (findingLastPos)
                ca2 = new CloudAction(cr, CloudAction.YND_LIST_JSON_FILES_LASTPOS, true);
            else
                ca2 = new CloudAction(cr, CloudAction.YND_LIST_JSON_FILES, true);
            ca2.param = fileMark;
            // ca2.param2 - will contain skip if folder exists. Means "skip create, because exists"
            ca2.bookCRC = sCRC;
            al.add(ca2);
            final YNDPerformAction a = new YNDPerformAction(cr, al, new YNDPerformAction.Callback() {
                @Override
                public void onComplete(YNDPerformAction a, String res, Object o) {
					if (o instanceof YNDListFiles) {
						YNDListFiles o1 = (YNDListFiles) o;
						if (o1.fileList.size() == 0) {
                            BackgroundThread.instance().postGUI(new Runnable() {
                                @Override
                                public void run() {
                                    if (!findingLastPos) cr.showToast(cr.getString(R.string.no_cloud_files));
                                }
                            },200);
						    return;
                        }
						Comparator<CloudFileInfo> compareByDate = new Comparator<CloudFileInfo>() {
							@Override
							public int compare(CloudFileInfo o1, CloudFileInfo o2) {
								return -(o1.created.compareTo(o2.created));
							}
						};
						Collections.sort(o1.fileList, compareByDate);
						int iPos = -1;
						for (int i = 0; i < o1.fileList.size(); i++) {
                            if (!o1.fileList.get(i).name.contains(cr.getAndroid_id())) {
                                iPos = i;
                                break;
                            }
                        }
						if (StrUtils.getNonEmptyStr(a.mCurAction.param,true).equals("_rpos_")) {
							if (a.mCurAction.action == YND_LIST_JSON_FILES_LASTPOS) {
                                Bookmark bmk = null;
							    if (cr.getReaderView()!=null) bmk = cr.getReaderView().getCurrentPositionBookmark();
                                if ((bmk != null) && (iPos >= 0)) {
                                    CloudFileInfo cfi = o1.fileList.get(iPos);
                                    String sTitle0 = StrUtils.getNonEmptyStr(cfi.name,true);
                                    String[] arrS = sTitle0.split("_"); // 2020-03-30_222303_rpos_635942216_36b928e773055c4a_0.16.json
                                    String sAdd = "";
                                    String sPerc = "";
                                    double curPos = 0.0;
                                    if (arrS.length>=6) {
                                        sPerc = arrS[5].replace(".json","");
                                        try {
                                            //curPos = Double.valueOf(sPerc) * 100;
                                            NumberFormat format = NumberFormat.getInstance(Locale.US);
                                            Number number = format.parse(sPerc);
                                            curPos = number.doubleValue() * 100;
                                        } catch (Exception e){
                                            curPos = 0.0;
                                        }
                                        sAdd = arrS[4];
                                        if (sAdd.contains(cr.getAndroid_id())) {
                                            sAdd = sAdd.replace(cr.getAndroid_id(), cr.getString(R.string.rpos_current_device));
                                        } else {
                                            if (CloudSync.devicesKnown != null)
                                                for (DeviceKnown dev: CloudSync.devicesKnown) {
                                                    if (sAdd.contains(dev.deviceId))
                                                        sAdd = sAdd.replace(dev.deviceId, dev.deviceName);
                                                }
                                        }
                                    }
                                    int iCurPos = (int) curPos;
                                    final String sAddF = sAdd;
                                    final String sPercF = sPerc;
                                    double dPerc = bmk.getPercent();
                                    dPerc = dPerc / 100;
                                    Locale l =new Locale("en", "US");
                                    final String sPercCur1 = String.format(l, "%5.2f", dPerc) + "%";
                                    final String sPercCur = sPercCur1.replace(",", ".");
                                    if (iCurPos > bmk.getPercent())
                                        BackgroundThread.instance().postGUI(new Runnable() {
                                        @Override
                                        public void run() {
                                            ArrayList<String> sButtons = new ArrayList<String>();
                                            sButtons.add("*"+sPercF+"% "+cr.getString(R.string.rpos_from)+" "+
                                                    sAddF+" ("+cr.getString(R.string.rpos_now_at)+" "+sPercCur+")");
                                            sButtons.add(cr.getString(R.string.rpos_restore));
                                            sButtons.add(cr.getString(R.string.rpos_list));
                                            sButtons.add(cr.getString(R.string.rpos_cancel));
                                            SomeButtonsToolbarDlg.showDialog(cr, cr.getReaderView(), 0, true,
                                                    cr.getString(R.string.rpos_found),
                                                    sButtons, o1, new SomeButtonsToolbarDlg.ButtonPressedCallback() {
                                                        @Override
                                                        public void done(Object o, String btnPressed) {
                                                            YNDListFiles o1 = (YNDListFiles) o;
                                                            if (o1.fileList.size()>0) {
                                                                CloudFileInfo cfi = o1.fileList.get(0);
                                                                if (btnPressed.equals(cr.getString(R.string.rpos_restore))) {
                                                                    CloudSync.loadFromJsonInfoFile(cr, CloudSync.CLOUD_SAVE_READING_POS, cfi.path, false,
                                                                            cfi.name, Settings.CLOUD_SYNC_VARIANT_YANDEX);
                                                                }
                                                                if (btnPressed.equals(cr.getString(R.string.rpos_list))) {
                                                                    BackgroundThread.instance().postGUI(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            ChooseReadingPosDlg dlg2 = new ChooseReadingPosDlg(cr, o1);
                                                                            dlg2.show();
                                                                        }
                                                                    }, 200);
                                                                }
                                                            }
                                                        }
                                                    });
                                        }
                                    }, 200);
                                }
                            }
							else {
                                BackgroundThread.instance().postGUI(new Runnable() {
                                    @Override
                                    public void run() {
                                        ChooseReadingPosDlg dlg2 = new ChooseReadingPosDlg(cr, o1);
                                        dlg2.show();
                                    }
                                }, 200);
                            }
						}
						if (StrUtils.getNonEmptyStr(a.mCurAction.param,true).equals("_bmk_")) {
							BackgroundThread.instance().postGUI(new Runnable() {
								@Override
								public void run() {
									ChooseBookmarksDlg dlg2 = new ChooseBookmarksDlg(cr, o1);
									dlg2.show();
								}}, 200);
						}
                        if (StrUtils.getNonEmptyStr(a.mCurAction.param,true).equals("_settings_")) {
                            BackgroundThread.instance().postGUI(new Runnable() {
                                @Override
                                public void run() {
                                    ChooseConfFileDlg dlg = new ChooseConfFileDlg(cr, o1);
                                    dlg.show();
                                }}, 200);
                        }
					}
                    CloudAction.onYNDComplete(cr, a, res, o, null);
                }

                @Override
                public void onError(YNDPerformAction a, String res, Exception e) {
                    CloudAction.onYNDError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showCloudToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage(),true);
            System.out.println("YND err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    public static void dbxLoadFolderContents(final CoolReader cr, final OpenBookFromCloudDlg dlg,
                                             final String sFolder, final String sFindStr) {
        try {
            cr.showCloudToast(R.string.cloud_begin,false);
            if (StrUtils.isEmptyStr(sFolder))
                if (!DBXConfig.init(cr)) return;
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca2 = new CloudAction(cr, CloudAction.DBX_LIST_FOLDER_IN_DLG, sFolder, sFindStr, true);
            al.add(ca2);
            final DBXPerformAction a = new DBXPerformAction(cr, al, new DBXPerformAction.Callback() {
                @Override
                public void onComplete(DBXPerformAction a, String res, Object o) {
                    CloudAction.onDBXComplete(cr, a, res, o, dlg);
                }

                @Override
                public void onError(DBXPerformAction a, String res, Exception e) {
                    CloudAction.onDBXError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showCloudToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage(),true);
            System.out.println("DBBOX err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    public static void dbxDownloadFile(final CoolReader cr, final OpenBookFromCloudDlg dlg,
            final String sFolder, com.dropbox.core.v2.files.Metadata md, boolean bQuiet) {
        try {
            final FileInfo downloadDir = Services.getScanner().getDownloadDirectory();
            final String fName = downloadDir.pathname+"/Dropbox/"+md.getName();
            final File fPath = new File(downloadDir.pathname+"/Dropbox/");
            if (!fPath.exists()) fPath.mkdir();
            final File fBook = new File(fName);
            cr.showCloudToast(R.string.cloud_begin,false);
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca2 = new CloudAction(cr, CloudAction.DBX_DOWNLOAD_FILE,
                    (sFolder+"/"+md.getName()).replace("//","/"), md, fName, bQuiet);
            al.add(ca2);
            final DBXPerformAction a = new DBXPerformAction(cr, al, new DBXPerformAction.Callback() {
                @Override
                public void onComplete(DBXPerformAction a, String res, Object o) {
                    CloudAction.onDBXComplete(cr, a, res, o, dlg);
                    if (!bQuiet)
                        cr.showToast(cr.getString(R.string.cloud_ok) + ": "+cr.getString(R.string.successfully_saved_to)+" " + fName);
                    if (PictureCameDialog.isFileIsPicture(fName)) {
                        cr.pictureCame(fName);
                    } else {
                        FileInfo fi = new FileInfo(fBook);
                        FileInfo dir = Services.getScanner().findParent(fi, downloadDir);
                        if (dir == null)
                            dir = downloadDir;
                        Services.getScanner().listDirectory(dir);
                        FileInfo item = dir.findItemByPathName(fBook.getAbsolutePath());
                        if (item != null)
                            cr.loadDocument(item);
                        else
                            cr.loadDocument(fi);
                    }
                }

                @Override
                public void onError(DBXPerformAction a, String res, Exception e) {
                    CloudAction.onDBXError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showCloudToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage(),true);
            //System.out.println("DBBOX err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    public static void yndLoadFolderContents(final CoolReader cr, final OpenBookFromCloudDlg dlg,
                                             final String sFolder, final String sFindStr) {
        try {
            cr.showCloudToast(R.string.cloud_begin,false);
            if (StrUtils.isEmptyStr(sFolder))
                if (!YNDConfig.init(cr)) return;
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca2 = new CloudAction(cr, CloudAction.YND_LIST_FOLDER_IN_DLG, sFolder, sFindStr, true);
            al.add(ca2);
            final YNDPerformAction a = new YNDPerformAction(cr, al, new YNDPerformAction.Callback() {
                @Override
                public void onComplete(YNDPerformAction a, String res, Object o) {
                    CloudAction.onYNDComplete(cr, a, res, o, dlg);
                }

                @Override
                public void onError(YNDPerformAction a, String res, Exception e) {
                    CloudAction.onYNDError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showCloudToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage(),true);
            System.out.println("YND err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    public static void yndSaveCurBookThenLoadFolderContents(final CoolReader cr, final OpenBookFromCloudDlg dlg,
                                                            final String sFolder, final String sFindStr, boolean bQuiet) {
        try {
            cr.showCloudToast(R.string.cloud_begin,false);
            if (StrUtils.isEmptyStr(sFolder))
                if (!YNDConfig.init(cr)) return;
            if (cr.getReaderView()==null) return;
            if (cr.getReaderView().getBookInfo()==null) return;
            if (cr.getReaderView().getBookInfo().getFileInfo()==null) return;
            FileInfo fi = cr.getReaderView().getBookInfo().getFileInfo();
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            String sFName = "";
            if (!StrUtils.isEmptyStr(fi.arcname)) sFName = fi.arcname;
                else sFName = fi.pathname;
            if (StrUtils.isEmptyStr(sFName)) return;
            CloudAction ca = new CloudAction(cr, CloudAction.YND_SAVE_TO_FILE_GET_LINK_W_DIR, true);
            ca.param = sFName;
            ca.param2 = sFolder;
            al.add(ca);
            CloudAction ca2 = new CloudAction(cr, CloudAction.YND_SAVE_CUR_BOOK, sFolder, sFindStr, bQuiet);
            al.add(ca2);
            CloudAction ca3 = new CloudAction(cr, CloudAction.YND_LIST_FOLDER_IN_DLG, sFolder, sFindStr, true);
            al.add(ca3);
            final YNDPerformAction a = new YNDPerformAction(cr, al, new YNDPerformAction.Callback() {
                @Override
                public void onComplete(YNDPerformAction a, String res, Object o) {
                    CloudAction.onYNDComplete(cr, a, res, o, dlg);
                }

                @Override
                public void onError(YNDPerformAction a, String res, Exception e) {
                    CloudAction.onYNDError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showCloudToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage(),true);
            System.out.println("YND err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    public static void yndDownloadFile(final CoolReader cr, final OpenBookFromCloudDlg dlg,
                                       final String sFolder, CloudFileInfo md, boolean bQuiet) {
        try {
            final FileInfo downloadDir = Services.getScanner().getDownloadDirectory();
            final String fName = downloadDir.pathname+"/YandexDisc/"+md.name;
            final File fPath = new File(downloadDir.pathname+"/YandexDisc");
            if (!fPath.exists()) fPath.mkdir();
            final File fBook = new File(fName);
            cr.showCloudToast(R.string.cloud_begin,false);
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca2 = new CloudAction(cr, CloudAction.YND_GET_DOWNLOAD_LINK,
                    (sFolder+"/"+md.name).replace("//","/"), md, fName, true);
            al.add(ca2);
            CloudAction ca3 = new CloudAction(cr, CloudAction.YND_DOWNLOAD_FILE,
                    (sFolder+"/"+md.name).replace("//","/"), md, fName, bQuiet);
            al.add(ca3);
            final YNDPerformAction a = new YNDPerformAction(cr, al, new YNDPerformAction.Callback() {
                @Override
                public void onComplete(YNDPerformAction a, String res, Object o) {
                    if (a.mCurAction.action == YND_DOWNLOAD_FILE) {
                        BackgroundThread.instance().postGUI(new Runnable() {
                            @Override
                            public void run() {
                                cr.showToast(cr.getString(R.string.cloud_ok) + ": " +cr.getString(R.string.successfully_saved_to)+" " + fName);
                                if (PictureCameDialog.isFileIsPicture(fName)) {
                                    cr.pictureCame(fName);
                                } else {
                                    FileInfo fi = new FileInfo(fBook);
                                    FileInfo dir = Services.getScanner().findParent(fi, downloadDir);
                                    if (dir == null)
                                        dir = downloadDir;
                                    Services.getScanner().listDirectory(dir);
                                    FileInfo item = dir.findItemByPathName(fBook.getAbsolutePath());
                                    if (item != null)
                                        cr.loadDocument(item);
                                    else
                                        cr.loadDocument(fi);
                                }
                            }
                        }, 200);
                    }
                    CloudAction.onYNDComplete(cr, a, res, o, dlg);
                }

                @Override
                public void onError(YNDPerformAction a, String res, Exception e) {
                    CloudAction.onYNDError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showCloudToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage(),true);
            System.out.println("YND err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    public static void emailSendBook(CoolReader cr, BookInfo bi) {
        Log.d(TAG, "Starting emailSendBook...");

        final ReaderView rv = cr.getReaderView();
        if ((rv == null)&&(bi == null)) {
            cr.showCloudToast(cr.getString(R.string.cloud_error) + ": book was not found",true);
            return;
        }
        try {
            BookInfo bookinfo = bi;
            if (bi == null) bookinfo = rv.getBookInfo();
            String sBookFN = bookinfo.getFileInfo().pathname;
            if (!StrUtils.isEmptyStr(bookinfo.getFileInfo().arcname))
                sBookFN = bookinfo.getFileInfo().arcname;
            final String sBookFName = sBookFN;
            File bookFile = new File(sBookFName);
            //Uri path = Uri.fromFile(bookFile);
            String sPkg = cr.getApplicationContext().getPackageName();
            Uri path = FileProvider.getUriForFile(cr, "org.knownreader.fileprovider", bookFile);
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            // set the type to 'email'
            emailIntent.setType("vnd.android.cursor.dir/email");
            // the attachment
            emailIntent.putExtra(Intent.EXTRA_STREAM, path);
            // the mail subject
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, bookinfo.getFileInfo().title + " " +
                bookinfo.getFileInfo().getAuthors());
            cr.startActivity(Intent.createChooser(emailIntent, cr.getString(R.string.send_book_by_email)+ "..."));
        } catch (Exception e) {
            cr.showCloudToast(cr.getString(R.string.could_not_email_book)+": "+e.getMessage(),true);
            e.printStackTrace();
        }
    }

    public static void yndLoadJsonFile(CoolReader cr, int iSaveType, String filePath, boolean bQuiet,
                                String name, int cloudMode) {
        try {
            cr.showCloudToast(R.string.cloud_begin,false);
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca2 = new CloudAction(cr, CloudAction.YND_GET_DOWNLOAD_LINK, filePath, true);
            al.add(ca2);
            CloudAction ca3 = new CloudAction(cr, CloudAction.YND_DOWNLOAD_FILE_TO_STRING, filePath,false);
            al.add(ca3);
            final YNDPerformAction a = new YNDPerformAction(cr, al, new YNDPerformAction.Callback() {
                @Override
                public void onComplete(YNDPerformAction a, String res, Object o) {
					if (o instanceof String) {
						final String sContent = (String) o;
						Log.i("CLOUD", "Complete action: "+ a.mCurAction.action);
						if (a.mCurAction.action == YND_DOWNLOAD_FILE_TO_STRING) {
							BackgroundThread.instance().postGUI(new Runnable() {
								@Override
								public void run() {
									//cr.showToast(cr.getString(R.string.cloud_ok) + ": "+cr.getString(R.string.successfully_loaded)); // not needed
									int iSaveType = CloudSync.CLOUD_SAVE_READING_POS;
									if (a.mCurAction.param.contains("_bmk_")) iSaveType = CloudSync.CLOUD_SAVE_BOOKMARKS;
                                    if (a.mCurAction.param.contains("_settings_")) iSaveType = CloudSync.CLOUD_SAVE_SETTINGS;
                                    if (iSaveType != CloudSync.CLOUD_SAVE_SETTINGS) {
                                        CloudSync.applyRPosOrBookmarks(cr, iSaveType, ((String) o), a.mCurAction.param, false);
                                    } else {
                                        String[] lines = ((String) o).split("\\r?\\n");
                                        boolean fileBegun = false;
                                        ArrayList<String> arrCurFile = new ArrayList<String>();
                                        String sFName = "";
                                        boolean bWasErr = false;
                                        for (String line : lines) {
                                            if (line.startsWith("~~~ settings: ")) {
                                                if (fileBegun) {
                                                    if ((arrCurFile.size()>0) && (!StrUtils.isEmptyStr(sFName)))
                                                        if (!CloudSync.restoreSettingsFile(cr, sFName, arrCurFile, bQuiet)) bWasErr = true;
                                                    arrCurFile.clear();
                                                }
                                                fileBegun = true;
                                                String s = line.replace("~~~ settings: ","");
                                                if (s.split("\\|").length>2) {
                                                    String[] arrS = s.split("\\|");
                                                    String sKnownId = arrS[1];
                                                    String sKnownDesc = arrS[2];
                                                    s = arrS[0];
                                                    if (CloudSync.devicesKnown != null) {
                                                        CloudSync.checkKnownDevice(cr, sKnownId, sKnownDesc);
                                                    }
                                                }
                                                sFName = s;
                                            } else arrCurFile.add(line);
                                        }
                                        if (fileBegun) {
                                            if ((arrCurFile.size()>0) && (!StrUtils.isEmptyStr(sFName)))
                                                if (!CloudSync.restoreSettingsFile(cr, sFName, arrCurFile, bQuiet)) bWasErr = true;
                                        }
                                        if (!bWasErr) {
                                            if (!bQuiet) cr.showToast(cr.getString(R.string.cloud_ok) + ": "+cr.getString(R.string.settings_were_restored));
                                            cr.finish();
                                        } else {
                                            if (!bQuiet) cr.showCloudToast(cr.getString(R.string.cloud_error) + ": "+ cr.getString(R.string.settings_restore_error), true);
                                            cr.finish();
                                        }
                                    }
								}}, 200);
						}

					}
                    CloudAction.onYNDComplete(cr, a, res, o, null);
                }

                @Override
                public void onError(YNDPerformAction a, String res, Exception e) {
                    CloudAction.onYNDError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showCloudToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage(),true);
            System.out.println("YND err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    public static void yndDeleteOldCloudFiles(final CoolReader cr, ArrayList<CloudFileInfo> arrCf,
            boolean bQuiet) {
        try
        {
            cr.showCloudToast(R.string.cloud_begin,false);
            if (!YNDConfig.init(cr)) return;
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            for (CloudFileInfo cf: arrCf) {
                CloudAction ca = new CloudAction(cr, CloudAction.YND_DELETE_FILE_ASYNC, bQuiet);
                ca.param = cf.path;
                al.add(ca);
            }
            if (al.size()>0) {
                final YNDPerformAction a = new YNDPerformAction(cr, al, new YNDPerformAction.Callback() {
                    @Override
                    public void onComplete(YNDPerformAction a, String res, Object o) {
                        CloudAction.onYNDComplete(cr, a, res, o, null);
                    }

                    @Override
                    public void onError(YNDPerformAction a, String res, Exception e) {
                        CloudAction.onYNDError(cr, a, res, e);
                    }
                });
                a.DoNextAction();
            }
        } catch (Exception e) {
            cr.showCloudToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage(),true);
            System.out.println("YND err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }
}
