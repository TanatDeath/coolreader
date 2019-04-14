package org.coolreader.cloud;

import android.util.Log;

import com.dropbox.core.v2.users.FullAccount;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.dropbox.DBXConfig;
import org.coolreader.cloud.dropbox.DBXPerformAction;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.StrUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CloudAction {
    public static final int NO_ACTION = 0;
    public static final int DBX_GET_CURRENT_ACCOUNT = 20000;
    public static final int DBX_LIST_FOLDER = 20001;
    public static final int DBX_LIST_FOLDER_THEN_OPEN_DLG = 20002;
    public static final int DBX_LIST_FOLDER_IN_DLG = 20003;
    public static final int DBX_DOWNLOAD_FILE = 20004;

    public int action; // action, that will be performed
    public String param; // param(s) passed to an action...
    public String param2; // param(s) passed to an action...
    public com.dropbox.core.v2.files.Metadata mDbxMd; // metadata for dropbox
    public String mFile;

    public CoolReader mActivity;

    public CloudAction(CoolReader cr, int a) {
        action = a;
        param = "";
        mActivity = cr;
    }

    public CloudAction(CoolReader cr, int a, String p) {
        action = a;
        param = p;
        mActivity = cr;
    }

    public CloudAction(CoolReader cr, int a, String p, String p2) {
        action = a;
        param = p;
        mActivity = cr;
        param2 = p2;
    }

    public CloudAction(CoolReader cr, int a, String p, com.dropbox.core.v2.files.Metadata md, String sFile) {
        action = a;
        param = p;
        mDbxMd = md;
        mActivity = cr;
        mFile = sFile;
    }

    public static void onDBXComplete(CoolReader cr, DBXPerformAction a, String res, Object o, Object dlg) {
        System.out.println("DBX:" + a.mCurAction.action);
        System.out.println("DBX:" + a.mCurAction.param);
        if ("FullAccount".equals(res)) {
            FullAccount fa = (FullAccount) o;
        }
        if (("ListFolderResult".equals(res))&&(a.mCurAction.action == CloudAction.DBX_LIST_FOLDER_THEN_OPEN_DLG)) {
            List<com.dropbox.core.v2.files.Metadata> l = (List<com.dropbox.core.v2.files.Metadata>) o;
            onDBXListFolderResultThenOpenDlg(cr, l);
        }
        if (("ListFolderResult".equals(res))&&(a.mCurAction.action == CloudAction.DBX_LIST_FOLDER_IN_DLG)) {
            List<com.dropbox.core.v2.files.Metadata> l = (List<com.dropbox.core.v2.files.Metadata>) o;
            onDBXListFolderResultInDlg(cr, l, (OpenBookFromCloudDlg) dlg);
        }
        a.DoNextAction();
    }

    public static void onDBXError(CoolReader cr, DBXPerformAction a, String res, Exception e) {
        Log.e(e.getClass().getName(), "Cloud operation error: "+res, e);
        if (cr!=null) cr.showToast("Cloud operation error: "+res);
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

    public static void dbxOpenBookDialog(final CoolReader cr) {
        try {
            cr.showToast(R.string.cloud_begin);
            if (!DBXConfig.init(cr)) return;
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca1 = new CloudAction(cr, CloudAction.DBX_GET_CURRENT_ACCOUNT);
            al.add(ca1);
            CloudAction ca2 = new CloudAction(cr, CloudAction.DBX_LIST_FOLDER_THEN_OPEN_DLG);
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
            cr.showToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage());
            System.out.println("DBBOX err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    public static void dbxLoadFolderContents(final CoolReader cr, final OpenBookFromCloudDlg dlg,
                                             final String sFolder, final String sFindStr) {
        try {
            cr.showToast(R.string.cloud_begin);
            if (StrUtils.isEmptyStr(sFolder))
                if (!DBXConfig.init(cr)) return;
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca2 = new CloudAction(cr, CloudAction.DBX_LIST_FOLDER_IN_DLG, sFolder, sFindStr);
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
            cr.showToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage());
            System.out.println("DBBOX err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    public static void dbxDownloadFile(final CoolReader cr, final OpenBookFromCloudDlg dlg,
            final String sFolder, com.dropbox.core.v2.files.Metadata md) {
        try {
            final FileInfo downloadDir = Services.getScanner().getDownloadDirectory();
            final String fName = downloadDir.pathname+"/Dropbox/"+md.getName();
            final File fPath = new File(downloadDir.pathname+"/Dropbox/");
            if (!fPath.exists()) fPath.mkdir();
            final File fBook = new File(fName);
            cr.showToast(R.string.cloud_begin);
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca2 = new CloudAction(cr, CloudAction.DBX_DOWNLOAD_FILE,
                    (sFolder+"/"+md.getName()).replace("//","/"), md, fName);
            al.add(ca2);
            final DBXPerformAction a = new DBXPerformAction(cr, al, new DBXPerformAction.Callback() {
                @Override
                public void onComplete(DBXPerformAction a, String res, Object o) {
                    CloudAction.onDBXComplete(cr, a, res, o, dlg);
                    cr.showToast(cr.getString(R.string.cloud_ok) + ": successfully saved to "+fName);
                    FileInfo fi = new FileInfo(fBook);
                    FileInfo dir = Services.getScanner().findParent(fi, downloadDir);
                    if ( dir==null )
                        dir = downloadDir;
                    Services.getScanner().listDirectory(dir);
                    FileInfo item = dir.findItemByPathName(fBook.getAbsolutePath());
                    if ( item!=null )
                        cr.loadDocument(item);
                    else
                        cr.loadDocument(fi);
                }

                @Override
                public void onError(DBXPerformAction a, String res, Exception e) {
                    CloudAction.onDBXError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage());
            System.out.println("DBBOX err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }
}
