package org.coolreader.crengine;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.*;

import androidx.documentfile.provider.DocumentFile;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.CloudAction;
import org.coolreader.cloud.litres.LitresConfig;
import org.coolreader.cloud.litres.LitresMainDialog;
import org.coolreader.cloud.litres.LitresSearchParams;
import org.coolreader.crengine.OPDSUtil.DocInfo;
import org.coolreader.crengine.OPDSUtil.DownloadCallback;
import org.coolreader.crengine.OPDSUtil.EntryInfo;
import org.coolreader.readerview.ReaderView;
import org.coolreader.db.CRDBService;
import org.coolreader.db.MainDB;
import org.coolreader.options.ListOption;
import org.coolreader.plugins.*;
import org.coolreader.utils.FileUtils;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class FileBrowser extends LinearLayout implements FileInfoChangeListener {

	public static final Logger log = L.create("fb");

	private static int SEC_GROUP_AUTHOR = 0;
	private static int SEC_GROUP_SERIES = 1;
	private static int SEC_GROUP_GENRE = 2;
	private static int SEC_GROUP_BOOK_DATE = 3;
	private static int SEC_GROUP_DOC_DATE = 4;
	private static int SEC_GROUP_PUBL_YEAR = 5;
	private static int SEC_GROUP_FILE_CREATE_DATE = 6;
	private static int SEC_GROUP_RATING = 7;
	private static int SEC_GROUP_STATE = 8;
	private static int SEC_GROUP_TITLE = 9;
	private static int SEC_GROUP_TAG = 10;

	Engine mEngine;
	Scanner mScanner;
	Scanner.ScanControl mScanControl;
	CoolReader mActivity;
	LayoutInflater mInflater;
	History mHistory;
	ListView mListView;
	boolean mHideEmptyGenres;
	ArrayList<FileInfo> mFileSystemFolders;
	ArrayList<FileInfo> mfavFolders;
	ArrayList<String> mOnlyFSFolders;
	HashMap<Long, String> mFileSystemFoldersH;
	public static HashMap<String, Integer> mListPosCacheOld = new HashMap<String, Integer>();
	public static HashMap<String, Integer> mListPosCache = new HashMap<String, Integer>();
	String lastOPDScatalogURL = "";
	int newTextSize = 16;

	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;

	public static final int MAX_SUBDIR_LEN = 32;

	public void showItemPopupMenu() {
		mActivity.registerForContextMenu(mActivity.contentView);
		mActivity.contentView.setOnCreateContextMenuListener((menu, v, menuInfo) -> mListView.createContextMenu(menu));
		mActivity.contentView.showContextMenu();
	}

	private boolean contains(ArrayList<FileInfo> fiA, String s) {
		if (fiA == null) return false;
		for (FileInfo fi: fiA) {
			if (fi.pathname.contains(s)) return true;
		}
		return false;
	}

	private boolean containsEq(ArrayList<FileInfo> fiA, String s) {
		if (fiA == null) return false;
		for (FileInfo fi: fiA) {
			if (fi.pathname.equals(s)) return true;
		}
		return false;
	}

	private class FileBrowserListView extends BaseListView {

		public FileBrowserListView(Context context) {
			super(context, true);
	        setLongClickable(true);
	        //registerForContextMenu(this);
	        //final FileBrowser _this = this;
	        setFocusable(true);
	        setFocusableInTouchMode(true);
	        requestFocus();
	        
	        setOnItemLongClickListener((arg0, arg1, position, id) -> {
				log.d("onItemLongClick("+position+")");
				//return super.performItemClick(view, position, id);
				FileInfo item = (FileInfo) getAdapter().getItem(position);
				if (item == null)
					return false;
				// need to scan book properies if they're absent
				try {
					if (StrUtils.isEmptyStr(item.getAuthors()))
						item = FileUtils.getFileProps(item, new File(item.getBasePath()),
								new FileInfo(new File(item.getBasePath())), true);
					if (StrUtils.isEmptyStr(item.getAuthors()))
						Services.getEngine().scanBookProperties(item);
				} catch (Exception e) {
					// do nothing
				}
				//\
				if (currDirectory != null)
					if (!StrUtils.isEmptyStr(currDirectory.pathname)) {
						if (mActivity.mCurrentFrame == mActivity.mBrowserFrame) {
							mListPosCache.put(currDirectory.pathname, position);
							//log.i("scroll mListPosCache.put("+ currDirectory.pathname +", "+ position +");");
						}
					}
				selectedItem = item;

				int longAction = mActivity.settings().getInt(Settings.PROP_APP_FILE_BROWSER_LONGTAP_ACTION, 1);
				boolean bookInfoDialogEnabled = true; // TODO: it's for debug
				if (!item.isDirectory && !item.isOPDSBook() && bookInfoDialogEnabled && !item.isOnlineCatalogPluginDir()
						&& !item.isCloudBook() && !item.isLitresSpecialDir()) {
					if(longAction == 0) {
						FileInfo finalItem = item;
						Services.getHistory().getOrCreateBookInfo(mActivity.getDB(), item, bookInfo -> {
							BookInfo bi = new BookInfo(finalItem);
							doBookInfoClick(bi);
						});
						return true;
					}
					if (longAction == 1) {
						ignoreActionSetting = true;
						performItemClick(arg1, position, id);
						return true;
					}
					if (longAction == 2)  {
						mActivity.editBookInfo(currDirectory, item);
						return true;
					}
				}
				try {
					showContextMenu();
				} catch (Exception e) {
					mActivity.showToast("Cannot show menu...");
				}
				return true;
			});
	        setOnScrollListener(new OnScrollListener() {

				private int oldTop;
				private int oldFirstVisibleItem;

				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
				//	if (onScrollListener != null) {
				//		onScrollListener.onScrollStateChanged(view, scrollState);
				//	}
				}

				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				//	if (onScrollListener != null) {
				//		onScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
				//	}

				//	if (onDetectScrollListener != null) {
				//		onDetectedListScroll(view, firstVisibleItem);
				//	}
					if (currDirectory != null)
						if (!StrUtils.isEmptyStr(currDirectory.pathname)) {
							if (mActivity.mCurrentFrame == mActivity.mBrowserFrame) {
								//int i = firstVisibleItem + (visibleItemCount / 3);
								int i = firstVisibleItem;
								mListPosCache.put("##visibleItemCount##", visibleItemCount);
								mListPosCache.put(currDirectory.pathname, i);
								//log.i("scroll mListPosCache.put("+ currDirectory.pathname + ", "+ i +");");
							}
						}
				}

				private void onDetectedListScroll(AbsListView absListView, int firstVisibleItem) {
					View view = absListView.getChildAt(0);
					int top = (view == null) ? 0 : view.getTop();

				//	if (firstVisibleItem == oldFirstVisibleItem) {
				//		if (top > oldTop) {
				//			onDetectScrollListener.onUpScrolling();
				//		} else if (top < oldTop) {
				//			onDetectScrollListener.onDownScrolling();
				//		}
				//	} else {
				//		if (firstVisibleItem < oldFirstVisibleItem) {
				//			onDetectScrollListener.onUpScrolling();
				//		} else {
				//			onDetectScrollListener.onDownScrolling();
				//		}
				//	}

					oldTop = top;
					oldFirstVisibleItem = firstVisibleItem;
				}
			});
			setChoiceMode(CHOICE_MODE_SINGLE);
		}
		
		@Override
		public void createContextMenu(ContextMenu menu) {
			log.d("createContextMenu()");
			menu.clear();
		    MenuInflater inflater = mActivity.getMenuInflater();
		    if (isRecentDir()) {
		    	// highly experimental - for SergeyTSA
				//inflater.inflate(R.menu.cr3_file_browser_file_context_menu, menu);
				//menu.setHeaderTitle(mActivity.getString(R.string.context_menu_title_book));
			    inflater.inflate(R.menu.cr3_file_browser_recent_context_menu, menu);
			    menu.setHeaderTitle(mActivity.getString(R.string.context_menu_title_recent_book));
		    } else if (currDirectory.isOPDSRoot()) {
			    inflater.inflate(R.menu.cr3_file_browser_opds_context_menu, menu);
			    menu.setHeaderTitle(mActivity.getString(R.string.menu_title_catalog));
		    } else if (selectedItem!=null && selectedItem.isOPDSDir()) {
			    inflater.inflate(R.menu.cr3_file_browser_opds_dir_context_menu, menu);
			    menu.setHeaderTitle(mActivity.getString(R.string.menu_title_catalog));
		    } else if (selectedItem!=null && selectedItem.isOPDSBook()) {
			    inflater.inflate(R.menu.cr3_file_browser_opds_book_context_menu, menu);
			    menu.setHeaderTitle(mActivity.getString(R.string.menu_title_catalog));
			} else if ((selectedItem!=null) && (selectedItem.pathname.startsWith("@")) && (selectedItem.pathname.contains("Group:"))) {
				inflater.inflate(R.menu.cr3_file_browser_group_folder_context_menu, menu);
				menu.setHeaderTitle(mActivity.getString(R.string.context_menu_title_folder));
			} else if (selectedItem!=null && (selectedItem.isBooksByAuthorRoot() || selectedItem.isBooksByAuthorDir()
					|| selectedItem.isBooksByCalibreAuthorDir())) {
				inflater.inflate(R.menu.cr3_file_browser_author_folder_context_menu, menu);
				menu.setHeaderTitle(mActivity.getString(R.string.context_menu_author_folder));
			} else if (selectedItem!=null && (selectedItem.isBooksBySeriesRoot() || selectedItem.isBooksBySeriesDir())) {
				inflater.inflate(R.menu.cr3_file_browser_series_folder_context_menu, menu);
				menu.setHeaderTitle(mActivity.getString(R.string.context_menu_series_folder));
            } else if (selectedItem!=null && (selectedItem.isBooksByGenreRoot() || selectedItem.isBooksByGenreDir())) {
				inflater.inflate(R.menu.cr3_file_browser_categ_folder_context_menu, menu);
				menu.setHeaderTitle(mActivity.getString(R.string.context_menu_genres_folder));
			} else if (selectedItem!=null && (selectedItem.isBooksByTagRoot() || selectedItem.isBooksByTagDir())) {
				inflater.inflate(R.menu.cr3_file_browser_categ_folder_context_menu, menu);
				menu.setHeaderTitle(mActivity.getString(R.string.context_menu_tags_folder));
			} else if (selectedItem!=null && (selectedItem.isBooksByBookdateRoot() || selectedItem.isBooksByBookdateDir())) {
				inflater.inflate(R.menu.cr3_file_browser_categ_folder_context_menu, menu);
				menu.setHeaderTitle(mActivity.getString(R.string.context_menu_bookdate_folder));
			} else if (selectedItem!=null && (selectedItem.isBooksByDocdateRoot() || selectedItem.isBooksByDocdateDir())) {
				inflater.inflate(R.menu.cr3_file_browser_categ_folder_context_menu, menu);
				menu.setHeaderTitle(mActivity.getString(R.string.context_menu_docdate_folder));
			} else if (selectedItem!=null && (selectedItem.isBooksByPublyearRoot() || selectedItem.isBooksByPublyearDir())) {
				inflater.inflate(R.menu.cr3_file_browser_categ_folder_context_menu, menu);
				menu.setHeaderTitle(mActivity.getString(R.string.context_menu_publyear_folder));
			} else if (selectedItem!=null && (selectedItem.isBooksByFiledateRoot() || selectedItem.isBooksByFiledateDir())) {
				inflater.inflate(R.menu.cr3_file_browser_categ_folder_context_menu, menu);
				menu.setHeaderTitle(mActivity.getString(R.string.context_menu_filedate_folder));
			} else if (selectedItem!=null && (selectedItem.isBooksByRatingRoot())) {
				inflater.inflate(R.menu.cr3_file_browser_categ_folder_context_menu, menu);
				menu.setHeaderTitle(mActivity.getString(R.string.context_menu_rating_folder));
			} else if (selectedItem!=null && (selectedItem.isBooksByStateFinishedRoot() ||
					selectedItem.isBooksByStateReadingRoot() || selectedItem.isBooksByStateToReadRoot())) {
				inflater.inflate(R.menu.cr3_file_browser_categ_folder_context_menu, menu);
				menu.setHeaderTitle(mActivity.getString(R.string.context_menu_state_folder));
			} else if (selectedItem!=null && selectedItem.isDirectory && !selectedItem.isOPDSDir()) {
                inflater.inflate(R.menu.cr3_file_browser_file_folder_context_menu, menu);
                menu.setHeaderTitle(mActivity.getString(R.string.context_menu_title_folder));
		    } else if (selectedItem!=null && !selectedItem.isDirectory && !selectedItem.isOPDSDir() &&
					!selectedItem.isOPDSBook()) {
				inflater.inflate(R.menu.cr3_file_browser_file_context_menu, menu);
				menu.setHeaderTitle(mActivity.getString(R.string.context_menu_title_book));
			} else if (selectedItem!=null && selectedItem.isDirectory) {
			    inflater.inflate(R.menu.cr3_file_browser_folder_context_menu, menu);
			    menu.setHeaderTitle(mActivity.getString(R.string.context_menu_title_book));
		    } else {
				inflater.inflate(R.menu.cr3_file_browser_context_menu, menu);
				menu.setHeaderTitle(mActivity.getString(R.string.context_menu_title_book));
			}

			for ( int i=0; i<menu.size(); i++ ) {
				MenuItem menuItem = menu.getItem(i);
				if (selectedItem!=null && selectedItem.isDirectory && selectedItem.isSpecialDir()) {
					if (menuItem.getItemId() == R.id.folder_delete || menuItem.getItemId() == R.id.folder_to_favorites) {
						menuItem.setVisible(false);
					}
				}
				menu.getItem(i).setOnMenuItemClickListener(item -> {
					onContextItemSelected(item);
					return true;
				});
			}

		}

		boolean ignoreActionSetting = false;

		public void doBookInfoClick(BookInfo bi) {
			mActivity.showBookInfo(bi, BookInfoDialog.BOOK_INFO, currDirectoryFiltered, null);
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			log.d("performItemClick("+position+")");
			boolean ignoreActionSettingOnce = ignoreActionSetting;
			ignoreActionSetting = false;
			//return super.performItemClick(view, position, id);
			FileInfo item = (FileInfo) getAdapter().getItem(position);
			if (item == null)
				return false;
			if (currDirectory != null)
				if (!StrUtils.isEmptyStr(currDirectory.pathname)) {
					if (mActivity.mCurrentFrame == mActivity.mBrowserFrame) {
						mListPosCache.put(currDirectory.pathname, position);
						//log.i("scroll mListPosCache.put("+ currDirectory.pathname +", "+ position +");");
					}
				}
			if (item.isLitresSpecialDir()) {
				if (item.isLitresPagination())
					showDirectory(item.parent, null, "", item.lsp, -1);
				if (item.isBooksByLitresGenreGroupDir())
					showDirectory(item, null, "", null, -1);
				if (item.isBooksByLitresCollectionDir())
					showDirectory(item, null, "", null, -1);
				if (item.isBooksByLitresSequenceDir())
					showDirectory(item, null, "", null, -1);
				if (item.isBooksByLitresGenreDir()) {
					int genreId = Integer.parseInt(item.pathname.replace(FileInfo.LITRES_GENRE_PREFIX, ""));
					LitresSearchParams lsp = new LitresSearchParams(LitresSearchParams.SEARCH_TYPE_ARTS_BY_GENRE, 0,0, 20,
							"", genreId, 0);
					mActivity.showBrowser(FileInfo.LITRES_TAG, lsp);
				}
				if (item.isBooksByLitresCollectionDir()) {
					int collectionId = Integer.parseInt(item.pathname.replace(FileInfo.LITRES_COLLECTION_PREFIX, ""));
					LitresSearchParams lsp = new LitresSearchParams(LitresSearchParams.SEARCH_TYPE_ARTS_BY_COLLECTION, 0,0, 20,
							"", collectionId, 0);
					mActivity.showBrowser(FileInfo.LITRES_TAG, lsp);
				}
				if (item.isBooksByLitresSequenceDir()) {
					int sequenceId = Integer.parseInt(item.pathname.replace(FileInfo.LITRES_SEQUENCE_PREFIX, ""));
					LitresSearchParams lsp = new LitresSearchParams(LitresSearchParams.SEARCH_TYPE_ARTS_BY_SEQUENCE, 0,0, 20,
							"", sequenceId, 0);
					mActivity.showBrowser(FileInfo.LITRES_TAG, lsp);
				}
				if (item.isBooksByLitresPersonDir()) {
					int personId = Integer.parseInt(item.pathname.replace(FileInfo.LITRES_PERSONS_PREFIX, ""));
					LitresSearchParams lsp = new LitresSearchParams(LitresSearchParams.SEARCH_TYPE_ARTS_BY_PERSON, 0,0, 20,
							"", personId, 0);
					mActivity.showBrowser(FileInfo.LITRES_TAG, lsp);
				}
				return true;
			}
			if (item.isDirectory) {
				if (item.isOPDSSearchDir()) {
					showFindBookDialog(false, "", item);
				} else
					showDirectory(item, null, "", null, -1);
				return true;
			}
			final FileInfo finalItem = item;
			if (item.isOPDSDir() || item.isCloudBook()) {
				if (item.isCloudBook()) {
					boolean bOpenExisting = false;
					if (
							(
								(!StrUtils.isEmptyStr(item.pathnameR)) ||
								(!StrUtils.isEmptyStr(item.arcnameR))
							) && (
								(!StrUtils.isEmptyStr(item.opdsLinkR)) ||
								(!StrUtils.isEmptyStr(item.opdsLinkRfull))
							)
						)
					{
						String q = mActivity.getString(R.string.open_previously_downloaded);
						boolean fragm = ((StrUtils.getNonEmptyStr(item.pathnameR, true).contains("/fragments/")) ||
								(StrUtils.getNonEmptyStr(item.arcnameR, true).contains("/fragments/")));
						if (fragm) q = mActivity.getString(R.string.open_previously_downloaded_fragment);
						String itemlink = finalItem.opdsLinkR;
						if (!fragm)
							if (!StrUtils.isEmptyStr(finalItem.opdsLinkRfull))
								itemlink = finalItem.opdsLinkRfull;
						final String fitemLink = itemlink;
						mActivity.askConfirmation(q,
								() -> Services.getHistory().getFileInfoByOPDSLink(mActivity.getDB(), fitemLink, item.isLitresBook(),
										new History.FileInfo1LoadedCallback() {

											@Override
											public void onFileInfoLoadBegin() {

											}

											@Override
											public void onFileInfoLoaded(final FileInfo fileInfo) {
												if (fileInfo != null) {
													boolean isArc = !StrUtils.isEmptyStr(fileInfo.arcname);
													boolean fileExists = false;
													if (isArc)
														fileExists = new File(fileInfo.arcname).exists();
													else
														fileExists = new File(fileInfo.pathname).exists();
													if (fileExists)
														mActivity.showBookInfo(new BookInfo(fileInfo), BookInfoDialog.BOOK_INFO, currDirectory, null);
													else {
														mActivity.showToast(R.string.could_not_find_by_link);
														mActivity.showCloudItemInfo(finalItem, FileBrowser.this, currDirectory);
													}
												} else {
													mActivity.showToast(R.string.could_not_find_by_link);
													mActivity.showCloudItemInfo(finalItem, FileBrowser.this, currDirectory);
												}
											}
										}
								), () ->
										mActivity.showCloudItemInfo(finalItem, FileBrowser.this, currDirectory)
						);
					} else {
						mActivity.showCloudItemInfo(finalItem, FileBrowser.this, currDirectory);
					}
				} else
					if (item.isOPDSSearchDir()) {
						showFindBookDialog(false, "", item);
					} else showOPDSDir(item, null, "");
			}
			else if (item.isOnlineCatalogPluginBook())
				showOnlineCatalogBookDialog(item);
			else {
				boolean isODT = false;
				if (item.format != null) isODT = item.format.name().equals("ODT");
				if (
						//(item.format.name().equals("DOCX"))|| //we have native docx support now
						(isODT) &&
							(
								(StrUtils.getNonEmptyStr(item.getFilename(), true).toLowerCase().contains(".ods"))
								||
								(StrUtils.getNonEmptyStr(item.getFilename(), true).toLowerCase().contains(".odp"))
							)
				   ){
					DocConvertDialog dlgConv = new DocConvertDialog(mActivity, item.pathname);
					dlgConv.show();
				} else {
					if (ignoreActionSettingOnce) {
						mActivity.loadDocument(item, true);
					} else {
						int tapAction = mActivity.settings().getInt(Settings.PROP_APP_FILE_BROWSER_TAP_ACTION, 0);

						FileInfo itemF = item;
						try {
							if (StrUtils.isEmptyStr(item.getAuthors()))
								itemF = FileUtils.getFileProps(itemF, new File(item.getBasePath()),
										new FileInfo(new File(item.getBasePath()).getParent()), true);
							if (StrUtils.isEmptyStr(itemF.getAuthors()))
								Services.getEngine().scanBookProperties(itemF);
						} catch (Exception e) {
							//do nothing
						}

						if(tapAction == 0) {
							FileInfo finalItemF = itemF;
							Services.getHistory().getOrCreateBookInfo(mActivity.getDB(), itemF, bookInfo -> {
								BookInfo bi = new BookInfo(finalItemF);
								doBookInfoClick(bi);
							});
							return true;
						}
						if (tapAction == 1) {
							ignoreActionSetting = true;
							mActivity.loadDocument(itemF, true);
						}
						if (tapAction == 2)  {
							mActivity.editBookInfo(currDirectory, itemF);
							return true;
						}
						if (tapAction == 3)  {
							showContextMenu();
							return true;
						}
					}
				}
			}
			return true;
		}

		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
			//log.d("FileBrowser.ListView.onKeyDown(" + keyCode + ")");
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				if (isRootDir()) {
					mActivity.showRootWindow();
//					if (mActivity.isBookOpened()) {
//						mActivity.showReader();
//						return true;
//					} else
//						return super.onKeyDown(keyCode, event);
				}
				showParentDirectory();
				return true;
			}
			if (keyCode == KeyEvent.KEYCODE_SEARCH) {
				showFindBookDialog(false, "", null);
				return true;
			}
			return super.onKeyDown(keyCode, event);
		}

		@Override
		public void setSelection(int position) {
			super.setSelection(position);
		}
		
	}

	private void invalidateAdapter(final FileListAdapter adapter) {
		adapter.notifyInvalidated();
	}

	CoverpageManager.CoverpageReadyListener coverpageListener;

	public FileBrowser(CoolReader activity, Engine engine, Scanner scanner, History history, boolean hideEmptyGenres) {
		super(activity);
		this.mActivity = activity;
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors(mActivity, isEInk);
		this.mEngine = engine;
		this.mScanner = scanner;
		this.mScanControl = new Scanner.ScanControl();
		this.mInflater = LayoutInflater.from(activity);// activity.getLayoutInflater();
		this.mHistory = history;
		this.mCoverpageManager = Services.getCoverpageManager();
		this.mHideEmptyGenres = hideEmptyGenres;
		this.newTextSize = mActivity.settings().getInt(Settings.PROP_STATUS_FONT_SIZE, 16);
		Services.getFileSystemFolders().loadFavoriteFolders(mActivity.getDB());
		ArrayList<FileInfo> favFolders = new ArrayList<>();
		ArrayList<String> favFoldersS = new ArrayList<>();
		this.mfavFolders = Services.getFileSystemFolders().getFavoriteFolders();
		if (this.mfavFolders!=null)
			for (FileInfo fi: this.mfavFolders) {
				favFoldersS.add(fi.pathname);
			}
		this.mFileSystemFolders = Services.getFileSystemFolders().getFileSystemFolders();
		this.mFileSystemFoldersH = new HashMap<>();
		this.mOnlyFSFolders = new ArrayList<>();
		if (this.mFileSystemFolders!=null)
			for (FileInfo fi: this.mFileSystemFolders) {
				this.mFileSystemFoldersH.put(fi.id, fi.pathname);
				if (!favFoldersS.contains(fi.pathname)) this.mOnlyFSFolders.add(fi.pathname);
			}
		coverpageListener = files -> {
			if (currDirectory == null)
				return;
			boolean found = false;
			for (CoverpageManager.ImageItem file : files) {
				if (currDirectory.findItemByPathName(file.file.getPathName()) != null)
					found = true;
			}
			if (found) // && mListView.getS
				invalidateAdapter(currentListAdapter);
		};
		this.mCoverpageManager.addCoverpageReadyListener(coverpageListener);
		super.onAttachedToWindow();
		
		setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		createListView(true);
		history.addListener(this);
		scanner.addListener(this);
		showDirectory( null, null, "", null, -1);

	}

	public void setHideEmptyGenres(boolean value) {
		mHideEmptyGenres = value;
		if (null != currDirectory && (currDirectory.isBooksByGenreDir() || currDirectory.isBooksByGenreRoot())) {
			// update
			showDirectory(currDirectory, null, "", null, -1);
		}
	}
	
	public void onClose() {
		mScanControl.stop();
		this.mCoverpageManager.removeCoverpageReadyListener(coverpageListener);
		coverpageListener = null;
		super.onDetachedFromWindow();
	}

	public void stopCurrentScan() {
		mScanControl.stop();
	}

	public CoverpageManager getCoverpageManager() {
		return mCoverpageManager;
	}
	private CoverpageManager mCoverpageManager;
	
	private ProgressPopup progress;
	private void createListView(boolean recreateAdapter) {
		if (progress != null)
			progress.hide();
		mListView = new FileBrowserListView(mActivity);
		final GestureDetector detector = new GestureDetector(new MyGestureListener());
		mListView.setOnTouchListener((v, event) -> {
			try {
				return detector.onTouchEvent(event);
			} catch (Exception e) {
				L.e("Exception in onTouch", e);
				return false;
			}
		});
		if (currentListAdapter == null || recreateAdapter) {
			currentListAdapter = new FileListAdapter();
			mListView.setAdapter(currentListAdapter);
			currentListAdapter.notifyDataSetChanged();
		} else {
			currentListAdapter.notifyDataSetChanged();
		}
		mListView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		mListView.setCacheColorHint(0);
		//mListView.setBackgroundResource(R.drawable.background_tiled_light);
		removeAllViews();
		addView(mListView);
		mListView.setVisibility(VISIBLE);
		progress = new ProgressPopup(mActivity, mListView);
	}
	
	public void onThemeChanged() {
		createListView(true);
		currentListAdapter.notifyDataSetChanged();
	}
	
	FileInfo selectedItem = null;

	void saveBookInfo() {
		Services.getHistory().getOrCreateBookInfo(mActivity.getDB(), selectedItem, bookInfo -> {
			if (bookInfo == null)
				bookInfo = new BookInfo(selectedItem);
			bookInfo.getFileInfo().assign(selectedItem);
			mActivity.getDB().saveBookInfo(bookInfo);
			mActivity.getDB().flush();
			BookInfo bi2 = Services.getHistory().getBookInfo(selectedItem);
			if (bi2 != null)
				bi2.getFileInfo().setFileProperties(selectedItem);
			if ((selectedItem.parent) != null)
				mActivity.directoryUpdated(selectedItem.parent, selectedItem);
		});
	}

	public interface FileInfoCallbackExt {
		void onComplete(FileInfo fi);
		void onError(FileInfo fi, Exception e);
	}
	
	public boolean onContextItemSelected(MenuItem item) {
		
		if (selectedItem == null)
			return false;
			
		switch (item.getItemId()) {
			case R.id.book_open:
				log.d("book_open menu item selected");
				if (selectedItem.isOPDSDir())
					showOPDSDir(selectedItem, null, "");
				else
					mActivity.loadDocument(selectedItem, true);
				return true;
			case R.id.bookmarks:
				log.d("bookmarks menu item selected");
				Services.getHistory().getOrCreateBookInfo(mActivity.getDB(), selectedItem, bookInfo -> {
					BookmarksDlg dlg = new BookmarksDlg(mActivity, mActivity.getReaderView(),
							bookInfo, false, null);
					dlg.show();
				});
				return true;
			case R.id.book_sort_order:
				mActivity.showToast("Sorry, sort order selection is not yet implemented");
				return true;
			case R.id.book_recent_books:
				showRecentBooks();
				return true;
			case R.id.book_opds_root:
				showOPDSRootDirectory();
				return true;
			case R.id.mi_opds_fav:
				if (selectedItem.isOPDSDir()) {
					if (!selectedItem.isFav) {
						addToFavorites(selectedItem);
						selectedItem.isFav = true;
						boolean bExists = false;
						for (FileInfo fiF: mfavFolders)
							if (fiF.pathname.equals(selectedItem.pathname)) {
								bExists = true;
								break;
							}
						if (!bExists) mfavFolders.add(selectedItem);
					} else {
						FileInfo fi = new FileInfo(selectedItem);
						Services.getFileSystemFolders().removeFavoriteFolder(mActivity.getDB(), fi);
						selectedItem.isFav = false;
						for (FileInfo fi2 : mfavFolders) {
							if (fi2.pathname.equals(selectedItem.pathname)) {
								mfavFolders.remove(fi2);
								break;
							}
						}
					}
				}
				currentListAdapter = new FileListAdapter();
				mListView.setAdapter(currentListAdapter);
				currentListAdapter.notifyDataSetChanged();
				return true;
			case R.id.book_root:
				showRootDirectory();
				return true;
			case R.id.book_back_to_reading:
				if (mActivity.isBookOpened())
					mActivity.showReader();
				else
					mActivity.showToast("No book opened");
				return true;
			case R.id.book_delete:
				log.d("book_delete menu item selected");
				mActivity.askDeleteBook(selectedItem, new FileInfoCallbackExt() {
					@Override
					public void onComplete(FileInfo fi) {
						String ss1 = StrUtils.getNonEmptyStr(fi.arcname, true);
						String ss2 = StrUtils.getNonEmptyStr(fi.pathname, true);
						for (int i = 0; i < currDirectoryFiltered.fileCount(); i++) {
							String s1 = StrUtils.getNonEmptyStr(currDirectoryFiltered.getFile(i).arcname, true);
							String s2 = StrUtils.getNonEmptyStr(currDirectoryFiltered.getFile(i).pathname, true);
							if ((s1.equals(ss1)) && (s2.equals(ss2))) {
								currDirectoryFiltered.files.remove(i);
								break;
							}
						}
						currentListAdapter.notifyDataSetChanged();
					}

					@Override
					public void onError(FileInfo fi, Exception e) {

					}
				});
				return true;
			case R.id.book_recent_goto:
				log.d("book_recent_goto menu item selected");
				showDirectory(selectedItem, selectedItem, "", null, -1);
				return true;
			case R.id.book_recent_remove:
				log.d("book_recent_remove menu item selected");
				mActivity.askDeleteRecent(selectedItem);
				return true;
			case R.id.catalog_add:
				log.d("catalog_add menu item selected");
				mActivity.editOPDSCatalog(null);
				return true;
			case R.id.catalog_delete:
				log.d("catalog_delete menu item selected");
				mActivity.askDeleteCatalog(selectedItem);
				return true;
			case R.id.catalog_edit:
				log.d("catalog_edit menu item selected");
				mActivity.editOPDSCatalog(selectedItem);
				return true;
			case R.id.catalog_open:
				log.d("catalog_open menu item selected");
				showOPDSDir(selectedItem, null, "");
				return true;
			case R.id.folder_open:
				log.d("folder_open menu item selected");
				showDirectory(selectedItem, null, "", null, -1);
				return true;
			case R.id.folder_delete:
				log.d("folder_delete menu item selected");
				mActivity.askDeleteFolder(selectedItem);
				return true;
			case R.id.folder_to_favorites:
				log.d("folder_to_favorites menu item selected");
				if (selectedItem!=null) {
					File ff = new File(selectedItem.pathname);
					if (!ff.exists()) {
						mActivity.showToast(mActivity.getString(R.string.not_a_dir));
						return true;
					}
				}
				addToFavorites(selectedItem);
				return true;
			case R.id.book_info:
				log.d("book_info menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					Services.getHistory().getOrCreateBookInfo(mActivity.getDB(), selectedItem, bookInfo -> {
						BookInfo bi = new BookInfo(selectedItem);
						mActivity.showBookInfo(bi, BookInfoDialog.BOOK_INFO, null, null);
					});
				}
				return true;
			case R.id.book_edit:
				log.d("book_edit menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					mActivity.editBookInfo(currDirectory, selectedItem);
				}
				return true;
			case R.id.book_tags:
				log.d("book_tags menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					TagsEditDialog dlgTagsEditDialog = new TagsEditDialog(mActivity, selectedItem, true,
							new TagsEditDialog.TagsEditDialogCloseCallback() {
								@Override
								public void onOk() {
									mActivity.getDB().loadTags(selectedItem, tags -> {
										String stags = "";
										for (BookTag bookTag : tags)
											if (bookTag.isSelected)
												stags = stags + '|' + bookTag.name;
										if (!StrUtils.isEmptyStr(stags))
											selectedItem.setTags(stags.substring(1));
										else
											selectedItem.setTags("");
										mListView.invalidateViews();
									});
								}

								@Override
								public void onCancel() {

								}
							});
					dlgTagsEditDialog.show();
				}
				return true;
			case R.id.move_to_books:
				log.d("move_to_books menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					mActivity.askMoveBook(selectedItem, new FileInfoCallbackExt() {
						@Override
						public void onComplete(FileInfo fi) {
							String ss1 = StrUtils.getNonEmptyStr(fi.arcname, true);
							String ss2 = StrUtils.getNonEmptyStr(fi.pathname, true);
							for (int i = 0; i < currDirectoryFiltered.fileCount(); i++) {
								String s1 = StrUtils.getNonEmptyStr(currDirectoryFiltered.getFile(i).arcname, true);
								String s2 = StrUtils.getNonEmptyStr(currDirectoryFiltered.getFile(i).pathname, true);
								if ((s1.equals(ss1)) && (s2.equals(ss2))) {
									currDirectoryFiltered.files.remove(i);
									break;
								}
							}
							currentListAdapter.notifyDataSetChanged();
						}

						@Override
						public void onError(FileInfo fi, Exception e) {

						}
					});
				}
				return true;
			case R.id.book_set_custom_cover:
				log.d("book_set_custom_cover menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					if (mActivity.picReceived!=null) {
						if (mActivity.picReceived.bmpReceived!=null) {
							BookInfo bi = new BookInfo(selectedItem);
							PictureCameDialog dlg = new PictureCameDialog(mActivity,
									bi, "", "");
							dlg.show();
						}
					} else {
						mActivity.showToast(R.string.pic_no_pic);
					}
				}
				return true;
			case R.id.book_delete_custom_cover:
				log.d("book_delete_custom_cover menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
						BookInfo bi = new BookInfo(selectedItem);
						PictureCameDialog dlg = new PictureCameDialog(mActivity,
									bi, "", "");
						dlg.deleteBookPicture(selectedItem);
				}
				return true;
			case R.id.book_shortcut:
				log.d("book_shortcut menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					DisplayMetrics outMetrics = new DisplayMetrics();
					mActivity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
					int mWindowSize = outMetrics.widthPixels < outMetrics.heightPixels ? outMetrics.widthPixels : outMetrics.heightPixels;
					int w = mWindowSize * 4 / 10;
					int h = w * 4 / 3;
					Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
					Services.getCoverpageManager().drawCoverpageFor(mActivity.getDB(), selectedItem, bmp, false,
							(file, bitmap) -> mActivity.createBookShortcut(selectedItem,bitmap));
				}
				return true;
			case R.id.book_to_gd:
				log.d("book_to_cloud menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					CloudAction.yndOpenBookDialog(mActivity, selectedItem,true);
				}
				return true;
			case R.id.book_to_email:
				log.d("book_to_email menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					CloudAction.emailSendBook((CoolReader) mActivity, new BookInfo(selectedItem));
				}
				return true;
			case R.id.book_no_mark:
				log.d("book_no_mark menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					int state = selectedItem.getReadingState();
					int newState = FileInfo.STATE_NEW;
					boolean modified = state != newState;
					if (modified) mActivity.setBookState(selectedItem, FileInfo.STATE_NEW);
					selectedItem.setReadingState(FileInfo.STATE_NEW);
					mListView.invalidateViews();
				}
				return true;
			case R.id.book_to_read:
				log.d("book_to_read menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					int state = selectedItem.getReadingState();
					int newState = FileInfo.STATE_TO_READ;
					boolean modified = state != newState;
					if (modified) mActivity.setBookState(selectedItem, FileInfo.STATE_TO_READ);
					selectedItem.setReadingState(FileInfo.STATE_TO_READ);
					mListView.invalidateViews();
				}
				return true;
			case R.id.book_reading:
				log.d("book_reading menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					int state = selectedItem.getReadingState();
					int newState = FileInfo.STATE_READING;
					boolean modified = state != newState;
					if (modified) mActivity.setBookState(selectedItem, FileInfo.STATE_READING);
					selectedItem.setReadingState(FileInfo.STATE_READING);
					mListView.invalidateViews();
				}
				return true;
			case R.id.book_finished:
				log.d("book_finished menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					int state = selectedItem.getReadingState();
					int newState = FileInfo.STATE_FINISHED;
					boolean modified = state != newState;
					if (modified) mActivity.setBookState(selectedItem, FileInfo.STATE_FINISHED);
					selectedItem.setReadingState(FileInfo.STATE_FINISHED);
					mListView.invalidateViews();
				}
				return true;
			case R.id.book_rate_5:
				log.d("book_rate_5 menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					int rate = selectedItem.getRate();
					int newRate = 5;
					boolean modified = rate != newRate;
					if (modified) mActivity.setBookRate(selectedItem, newRate);
					selectedItem.setRate(newRate);
					mListView.invalidateViews();
				}
				return true;
			case R.id.book_rate_4:
				log.d("book_rate_4 menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					int rate = selectedItem.getRate();
					int newRate = 4;
					boolean modified = rate != newRate;
					if (modified) mActivity.setBookRate(selectedItem, newRate);
					selectedItem.setRate(newRate);
					mListView.invalidateViews();
				}
				return true;
			case R.id.book_rate_3:
				log.d("book_rate_3 menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					int rate = selectedItem.getRate();
					int newRate = 3;
					boolean modified = rate != newRate;
					if (modified) mActivity.setBookRate(selectedItem, newRate);
					selectedItem.setRate(newRate);
					mListView.invalidateViews();
				}
				return true;
			case R.id.book_rate_2:
				log.d("book_rate_2 menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					int rate = selectedItem.getRate();
					int newRate = 2;
					boolean modified = rate != newRate;
					if (modified) mActivity.setBookRate(selectedItem, newRate);
					selectedItem.setRate(newRate);
					mListView.invalidateViews();
				}
				return true;
			case R.id.book_rate_1:
				log.d("book_rate_1 menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					int rate = selectedItem.getRate();
					int newRate = 1;
					boolean modified = rate != newRate;
					if (modified) mActivity.setBookRate(selectedItem, newRate);
					selectedItem.setRate(newRate);
					mListView.invalidateViews();
				}
				return true;
			case R.id.book_rate_0:
				log.d("book_rate_0 menu item selected");
				if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					int rate = selectedItem.getRate();
					int newRate = 0;
					boolean modified = rate != newRate;
					if (modified) mActivity.setBookRate(selectedItem, newRate);
					selectedItem.setRate(newRate);
					mListView.invalidateViews();
				}
				return true;
			case R.id.folder_series_authors:
				log.d("folder_series_authors menu item selected");
				if (selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					String sItem = selectedItem.pathname.replace(FileInfo.SERIES_PREFIX, "");
					FileInfo dir = new FileInfo();
					dir.isDirectory = true;
					dir.pathname = FileInfo.AUTHORS_TAG;
					dir.setFilename(mActivity.getString(R.string.folder_name_books_by_author));
					dir.isListed = true;
					dir.isScanned = true;
					mActivity.showDirectory(dir, "seriesId:"+sItem);
				}
				return true;
			case R.id.folder_authors_series:
				log.d("folder_authors_series menu item selected");
				if (selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
					String sItem = selectedItem.pathname.replace(FileInfo.AUTHOR_PREFIX, "");
					FileInfo dir = new FileInfo();
					dir.isDirectory = true;
					dir.pathname = FileInfo.SERIES_TAG;
					dir.setFilename(mActivity.getString(R.string.folder_name_books_by_series));
					dir.isListed = true;
					dir.isScanned = true;
					mActivity.showDirectory(dir, "authorId:"+sItem);
				}
				return true;
			case R.id.show_group_books_by_author:
				log.d("show_group_books_by_author menu item selected");
				showDirectory(selectedItem, null, "", null, SEC_GROUP_AUTHOR);
				return true;
			case R.id.show_group_books_by_series:
				log.d("show_group_books_by_series menu item selected");
				showDirectory(selectedItem, null, "", null, SEC_GROUP_SERIES);
				return true;
			case R.id.show_group_books_by_genre:
				log.d("show_group_books_by_genre menu item selected");
				showDirectory(selectedItem, null, "", null, SEC_GROUP_GENRE);
				return true;
			case R.id.show_group_books_by_tag:
				log.d("show_group_books_by_tag menu item selected");
				showDirectory(selectedItem, null, "", null, SEC_GROUP_TAG);
				return true;
			case R.id.show_group_books_by_bookdate:
				log.d("show_group_books_by_bookdate menu item selected");
				showDirectory(selectedItem, null, "", null, SEC_GROUP_BOOK_DATE);
				return true;
			case R.id.show_group_books_by_docdate:
				log.d("show_group_books_by_docdate menu item selected");
				showDirectory(selectedItem, null, "", null, SEC_GROUP_DOC_DATE);
				return true;
			case R.id.show_group_books_by_publyear:
				log.d("show_group_books_by_publyear menu item selected");
				showDirectory(selectedItem, null, "", null, SEC_GROUP_PUBL_YEAR);
				return true;
			case R.id.show_group_books_by_filedate:
				log.d("show_group_books_by_filedate menu item selected");
				showDirectory(selectedItem, null, "", null, SEC_GROUP_FILE_CREATE_DATE);
				return true;
			case R.id.show_group_books_by_rating:
				log.d("show_group_books_by_rating menu item selected");
				showDirectory(selectedItem, null, "", null, SEC_GROUP_RATING);
				return true;
			case R.id.show_group_books_by_state:
				log.d("show_group_books_by_state menu item selected");
				showDirectory(selectedItem, null, "", null, SEC_GROUP_STATE);
				return true;
			case R.id.show_group_books_by_title:
				log.d("show_group_books_by_title menu item selected");
				showDirectory(selectedItem, null, "", null, SEC_GROUP_TITLE);
				return true;
		}
		return false;
	}

    private void addToFavorites(FileInfo folder) {
        Services.getFileSystemFolders().addFavoriteFolder(mActivity.getDB(), folder);
    }

    public void refreshOPDSRootDirectory(final boolean showInBrowser) {
		final FileInfo opdsRoot = mScanner.getOPDSRoot();
		if (opdsRoot != null) {
			mActivity.getDB().loadOPDSCatalogs(catalogs -> {
				opdsRoot.clear();
				for (FileInfo f : catalogs)
					opdsRoot.addDir(f);
				if (showInBrowser || (currDirectory!=null && currDirectory.isOPDSRoot()))
					showDirectory(opdsRoot, null, "", null, -1);
			});
		}
	}
	
	public void refreshDirectory(FileInfo dir, FileInfo selected) {
		if (dir.isSpecialDir()) {
			if (dir.isOPDSRoot())
				refreshOPDSRootDirectory(false);
		} else {
			if (dir.pathNameEquals(currDirectory))
				showDirectory(currDirectory, selected, "", null, -1);
		}
	}

	public void scrollToLastPos(boolean doStore) {
		// try to restore position
		if (currDirectory!=null) {
			if (currDirectory.isLitresPrefix()) return;
			if (doStore) {
				firstVisibleItem = mListPosCache.get(currDirectory.pathname);
				visibleItemCount = mListPosCache.get("##visibleItemCount##");
				log.i("mListPosCache.get store firstVisibleItem = " + firstVisibleItem);
				return;
			}
			log.i("mListPosCache.get set firstVisibleItem = " + firstVisibleItem);
			if (firstVisibleItem == null) firstVisibleItem = 0;
			if (visibleItemCount == null) visibleItemCount = 0;
			Integer firstVisibleItem2 = 0;
			if (mListPosCacheOld != null) {
				firstVisibleItem2 = mListPosCacheOld.get(currDirectory.pathname);
				if (firstVisibleItem2 == null) firstVisibleItem2 = 0;
				if ((firstVisibleItem2 > 0) && (firstVisibleItem == 0))
					firstVisibleItem = firstVisibleItem2;
				mListPosCacheOld = null;
			}
			if (firstVisibleItem > 0) {
				if (visibleItemCount == 0)
					mListView.setSelection(firstVisibleItem);
				else {
					int fiv = firstVisibleItem;
					//fiv = fiv - visibleItemCount / 4;
					//if (fiv < 0) fiv = 0;
					mListView.setSelection(fiv);
				}
			}
		}
	}

	public void showParentDirectory()
	{
		if (currDirectory == null || currDirectory.parent == null || currDirectory.parent.isRootDir()) {
			boolean showRoot = true;
			if (currDirectory != null)
				if (currDirectory.isLitresPrefix()) {
					showRoot = false;
					mActivity.showRootWindow();
					LitresMainDialog litresMainDialog = new LitresMainDialog(mActivity, FileBrowser.saveParams);
					litresMainDialog.show();
				}
			mScanControl.stop();
			if (showRoot) mActivity.showRootWindow();
		}
		else
			showDirectory(currDirectory.parent, currDirectory, "", null, -1);
	}
	
	boolean mInitStarted = false;
	public void init()
	{
		if (mInitStarted)
			return;
		log.e("FileBrowser.init() called");
		mInitStarted = true;
		
		//showDirectory( mScanner.mRoot, null );
		mListView.setSelection(0);
	}

	private FileInfo currDirectory;
	private FileInfo currDirectoryFiltered;
	Integer firstVisibleItem = 0;
	Integer visibleItemCount = 0;

	public void filterUpdated(String text) {
		if (currDirectory == null) return;
 		boolean bStateDir = mActivity.settings().getBool(Settings.PROP_APP_SHOW_FILES_DIR, true);
		boolean bStateNoMark = mActivity.settings().getBool(Settings.PROP_APP_SHOW_FILES_NO_MARK, true);
		boolean bStateToRead = mActivity.settings().getBool(Settings.PROP_APP_SHOW_FILES_TO_READ, true);
		boolean bStateReading = mActivity.settings().getBool(Settings.PROP_APP_SHOW_FILES_READING, true);
		boolean bStateFinished = mActivity.settings().getBool(Settings.PROP_APP_SHOW_FILES_FINISHED, true);
		if ((StrUtils.isEmptyStr(text))
				&& (bStateDir) && (bStateNoMark) && (bStateToRead) && (bStateReading) && (bStateFinished)) {
			currDirectoryFiltered = currDirectory;
		} else {
			ArrayList<FileInfo> filesN = new ArrayList<FileInfo>();// files
			ArrayList<FileInfo> dirsN = new ArrayList<FileInfo>(); // directories
			currDirectoryFiltered = new FileInfo(currDirectory);
			if (currDirectory.dirs != null)
				for (FileInfo dir: currDirectory.dirs) {
					boolean bOk = false;
					if (!StrUtils.isEmptyStr(text)) {
						if (dir.contains(text)) bOk = true;
					} else bOk = true;
					if (bOk)
						if (bStateDir)
							dirsN.add(dir);
				}
			if (currDirectory.files != null)
				for (FileInfo file: currDirectory.files) {
					boolean bOk = true;
					if (!StrUtils.isEmptyStr(text))
						if (!file.contains(text)) bOk = false;
					if (bOk) {
						bOk = false;
						int rs = file.getReadingState();
						if (
						    ((rs == 0) && (bStateNoMark))
							||
						    ((rs == FileInfo.STATE_TO_READ) && (bStateToRead))
						    ||
						    ((rs == FileInfo.STATE_READING) && (bStateReading))
						    ||
						    ((rs == FileInfo.STATE_FINISHED) && (bStateFinished))
						) bOk = true;
					}
					if (bOk) filesN.add(file);
				}
			log.i("finding: "+text);
			log.i("dir_cnt "+dirsN.size());
			log.i("files_cnt "+filesN.size());
			currDirectoryFiltered.dirs = dirsN;
			currDirectoryFiltered.files = filesN;
		}
		invalidateAdapter(currentListAdapter);
	}

	public boolean isRootDir()
	{
		return currDirectory != null && (currDirectory == mScanner.getRoot() || currDirectory.parent == mScanner.getRoot());
	}

	public boolean isRecentDir()
	{
		return currDirectory!=null && currDirectory.isRecentDir();
	}

	public void showRecentBooks()
	{
		showDirectory(mScanner.getRecentDir(), null, "", null, -1);
	}

	public boolean isBookShownInRecentList(FileInfo book) {
		if (currDirectory==null || !currDirectory.isRecentDir())
			return false;
		return currDirectory.findItemByPathName(book.getPathName())!=null;
	}
	
	public void showLastDirectory()
	{
		if (currDirectory == null || currDirectory == mScanner.getRoot())
			showRecentBooks();
		else
			showDirectory(currDirectory, null, "", null, -1);
	}

	MainDB.ItemGroupExtractor getExtractor(int etype) {
		if (etype == SEC_GROUP_AUTHOR) return new MainDB.ItemGroupAuthorExtractor();
		if (etype == SEC_GROUP_SERIES) return new MainDB.ItemGroupSeriesExtractor();
		if (etype == SEC_GROUP_GENRE) return new MainDB.ItemGroupGenresExtractor();
		if (etype == SEC_GROUP_BOOK_DATE) return new MainDB.ItemGroupBookDateNExtractor();
		if (etype == SEC_GROUP_DOC_DATE) return new MainDB.ItemGroupDocDateNExtractor();
		if (etype == SEC_GROUP_PUBL_YEAR) return new MainDB.ItemGroupPublYearNExtractor();
		if (etype == SEC_GROUP_FILE_CREATE_DATE) return new MainDB.ItemGroupFileCreateTimeExtractor();
		if (etype == SEC_GROUP_RATING) return new MainDB.ItemGroupRatingExtractor();
		if (etype == SEC_GROUP_STATE) return new MainDB.ItemGroupStateExtractor();
		if (etype == SEC_GROUP_TITLE) return new MainDB.ItemGroupTitleExtractor();
		if (etype == SEC_GROUP_TAG) return new MainDB.ItemGroupTagsExtractor();
		return new MainDB.ItemGroupAuthorExtractor();
	}

	MainDB.ItemGroupExtractor getExtractor2(int etype) {
		if (etype == SEC_GROUP_AUTHOR + 1) return new MainDB.ItemGroupAuthorExtractor();
		if (etype == SEC_GROUP_SERIES + 1) return new MainDB.ItemGroupSeriesExtractor();
		if (etype == SEC_GROUP_GENRE + 1) return new MainDB.ItemGroupGenresExtractor();
		if (etype == SEC_GROUP_BOOK_DATE + 1) return new MainDB.ItemGroupBookDateNExtractor();
		if (etype == SEC_GROUP_DOC_DATE + 1) return new MainDB.ItemGroupDocDateNExtractor();
		if (etype == SEC_GROUP_PUBL_YEAR + 1) return new MainDB.ItemGroupPublYearNExtractor();
		if (etype == SEC_GROUP_FILE_CREATE_DATE + 1) return new MainDB.ItemGroupFileCreateTimeExtractor();
		if (etype == SEC_GROUP_RATING + 1) return new MainDB.ItemGroupRatingExtractor();
		if (etype == SEC_GROUP_STATE + 1) return new MainDB.ItemGroupStateExtractor();
		if (etype == SEC_GROUP_TITLE + 1) return new MainDB.ItemGroupTitleExtractor();
		if (etype == SEC_GROUP_TAG + 1) return new MainDB.ItemGroupTagsExtractor();
		return new MainDB.ItemGroupAuthorExtractor();
	}

	MainDB.ItemGroupExtractor getExtractor3(int etype, String prefix) {
		if (etype == SEC_GROUP_AUTHOR + 1) return new MainDB.ItemGroupAuthorExtractor();
		if (etype == SEC_GROUP_SERIES + 1) return new MainDB.ItemGroupSeriesExtractor();
		if (etype == SEC_GROUP_GENRE + 1) return new MainDB.ItemGroupGenresExtractor();
		if (etype == 4) {
			if (prefix.equals(FileInfo.BOOK_DATE_GROUP_PREFIX)) return new MainDB.ItemGroupBookDateNExtractor();
			if (prefix.equals(FileInfo.DOC_DATE_GROUP_PREFIX)) return new MainDB.ItemGroupDocDateNExtractor();
			if (prefix.equals(FileInfo.PUBL_YEAR_GROUP_PREFIX)) return new MainDB.ItemGroupPublYearNExtractor();
			if (prefix.equals(FileInfo.FILE_DATE_GROUP_PREFIX)) return new MainDB.ItemGroupFileCreateTimeExtractor();
		};
		if (etype == 5) return new MainDB.ItemGroupRatingExtractor();
		if (etype == 6) return new MainDB.ItemGroupStateExtractor();
		if (etype == 7) return new MainDB.ItemGroupTitleExtractor();
		if (etype == 8) return new MainDB.ItemGroupTagsExtractor();
		return new MainDB.ItemGroupAuthorExtractor();
	}

	public void showSearchResult(FileInfo[] books) {
		FileInfo newGroup = MainDB.createItemGroup("", FileInfo.TITLE_GROUP_PREFIX);
		FileInfo dir = mScanner.setSearchResults(books);
		newGroup.setFilename(dir.getFilename());
		newGroup.title = dir.title;
		int sett0 = mActivity.settings().getInt(Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_COMMON, 0);
		int sett = mActivity.settings().getInt(Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_SEARCH, 0);
		MainDB.ItemGroupExtractor extractor = getExtractor(sett0);
		if (sett != 0) extractor = getExtractor2(sett);
		MainDB.addGroupedItems2(newGroup, "",
				dir.getElements(), FileInfo.TITLE_GROUP_PREFIX, extractor, 1);
		showDirectory(newGroup, null, "", null, -1);
	}
	
	public void showFindBookDialog(boolean isQuick, String sText, final FileInfo fi)
	{
		FileInfo fi2 = fi;
		if (currDirectory!=null) {
			if ((currDirectory.isOPDSDir()) && (fi == null)) {
				FileInfo par = currDirectory;
				while (par.parent != null) par=par.parent;
				for (int i=0; i<par.dirCount(); i++)
					if (par.getDir(i).pathname.startsWith(FileInfo.OPDS_DIR_PREFIX + "search:")) {
						fi2 = par.getDir(i);
						break;
					}
			}
		}
		if (fi2!=null) { // OPDS
			if (!FlavourConstants.PREMIUM_FEATURES) {
				mActivity.showToast(R.string.only_in_premium);
				return;
			}
			BookOPDSSearchDialog dlg = new BookOPDSSearchDialog(mActivity, sText, fi2, new BookSearchDialog.SearchCallback() {

				@Override
				public void start() {
					//showDirectoryLoadingStub();
				}

				@Override
				public void done(FileInfo[] results) {
					if (results != null) {
						if (results.length == 0)
							//showDirectoryNotFoundStub()
							;
						else {
							FileInfo fi3 = results[0];
							fi3.pathname = fi3.pathname.replace(FileInfo.OPDS_DIR_PREFIX + "search:", FileInfo.OPDS_DIR_PREFIX);
							showDirectory(fi3, null, "", null, -1);
						}
					} //else showDirectoryNotFoundStub();
				}
			});
			dlg.show();
		} else {
			BookSearchDialog dlg = new BookSearchDialog(mActivity, new BookSearchDialog.SearchCallback() {

				@Override
				public void start() {
					showDirectoryLoadingStub();
				}

				@Override
				public void done(FileInfo[] results) {
					if (results != null) {
						if (results.length == 0) {
							mActivity.showToast(R.string.dlg_book_search_not_found);
							showDirectoryNotFoundStub();
						} else {
							showSearchResult(results);
							//showDirectoryNotFoundStub();
						}
					} else {
						if (currDirectory == null || currDirectory.isRootDir())
							mActivity.showRootWindow();
						else
							showDirectoryNotFoundStub();
					}
				}
			});
			if (!isQuick)
				dlg.show();
			else
				dlg.qfind(dlg.callback, sText);
			;
		}
	}

	public void showRootDirectory()
	{
		log.v("showRootDirectory()");
		showDirectory(mScanner.getRoot(), null, "", null, -1);
	}

	private OnlineStoreWrapper getPlugin(FileInfo dir) {
		return OnlineStorePluginManager.getPlugin(mActivity, dir.getOnlineCatalogPluginPackage());
	}

	private void openPluginDirectory(OnlineStoreWrapper plugin, FileInfo dir) {
		progress.show();
		plugin.openDirectory(dir, new FileInfoCallback() {
			@Override
			public void onFileInfoReady(FileInfo fileInfo) {
				progress.hide();
				showDirectoryInternal(fileInfo, null);
			}
			@Override
			public void onError(int errorCode, String description) {
				progress.hide();
				mActivity.showToast("Cannot read from server");
			}
		});
	}
	
	private void openPluginDirectoryWithLoginDialog(final OnlineStoreWrapper plugin, final FileInfo dir) {
		OnlineStoreLoginDialog dlg = new OnlineStoreLoginDialog(mActivity, plugin, () -> openPluginDirectory(plugin, dir));
		dlg.show();
	}

	public void showOnlineStoreDirectory(final FileInfo dir)
	{
		log.v("showOnlineStoreDirectory(" + dir.pathname + ")");
		final OnlineStoreWrapper plugin = getPlugin(dir);
		if (plugin != null) {
			if (dir.fileCount() > 0 || dir.dirCount() > 0) {
				showDirectoryInternal(dir, null);
				return;
			}
			String path = dir.getOnlineCatalogPluginPath();
			String id = dir.getOnlineCatalogPluginId();
			if ("my".equals(path)) {
				String login = plugin.getLogin();
				String password = plugin.getPassword();
				if (login != null && password != null) {
					progress.show();
					plugin.authenticate(login, password, new AuthenticationCallback() {
						@Override
						public void onError(int errorCode, String errorMessage) {
							// ignore error 
							progress.hide();
							openPluginDirectoryWithLoginDialog(plugin, dir);
						}
						@Override
						public void onSuccess() {
							progress.hide();
							openPluginDirectory(plugin, dir);
						}
					});
					return;
				} else {
					openPluginDirectoryWithLoginDialog(plugin, dir);
				}
			}
			if ("genres".equals(path) || "popular".equals(path) || "new".equals(path) || (path.startsWith("genre=") && id != null) || (path.startsWith("authors=") && id != null) || (path.startsWith("author=") && id != null)) {
				openPluginDirectory(plugin, dir);
			}
		}
	}

	public void showOPDSRootDirectory()
	{
		log.v("showOPDSRootDirectory()");
		final FileInfo opdsRoot = mScanner.getOPDSRoot();
		if (opdsRoot != null) {
			mActivity.getDB().loadOPDSCatalogs(catalogs -> {
				opdsRoot.setItems(catalogs);
				showDirectoryInternal(opdsRoot, null);
			});
		}
	}

	public void showFavoriteFolders()
	{
		log.v("FavoriteFolders()");
		final FileInfo favRoot = mScanner.getOPDSRoot();
		Services.getFileSystemFolders().loadFavoriteFolders(mActivity.getDB());
//		if (opdsRoot != null) {
//			mActivity.getDB().loadOPDSCatalogs(catalogs -> {
//				opdsRoot.setItems(catalogs);
//				showDirectoryInternal(opdsRoot, null);
//			});
//		}
	}

	private FileInfo.SortOrder mSortOrder = FileInfo.DEF_SORT_ORDER; 
	public void setSortOrder(FileInfo.SortOrder order) {
		if (mSortOrder == order)
			return;
		mSortOrder = order!=null ? order : FileInfo.DEF_SORT_ORDER;
		if (currDirectory != null && currDirectory.allowSorting()) {
			currDirectory.sort(mSortOrder);
			showDirectory(currDirectory, selectedItem, "", null, -1);
			mActivity.setSetting(ReaderView.PROP_APP_BOOK_SORT_ORDER, mSortOrder.name(), false);
		}
	}
	public void setSortOrder(String orderName) {
		setSortOrder(FileInfo.SortOrder.fromName(orderName));
	}
	public void showSortOrderMenu() {
		final Properties properties = new Properties();
		properties.setProperty(ReaderView.PROP_APP_BOOK_SORT_ORDER, mActivity.settings().getProperty(ReaderView.PROP_APP_BOOK_SORT_ORDER));
		final String oldValue = properties.getProperty(ReaderView.PROP_APP_BOOK_SORT_ORDER);
		int[] optionLabels = {
			FileInfo.SortOrder.FILENAME.resourceId,	
			FileInfo.SortOrder.FILENAME_DESC.resourceId,	
			FileInfo.SortOrder.AUTHOR_TITLE.resourceId,	
			FileInfo.SortOrder.AUTHOR_TITLE_DESC.resourceId,
			FileInfo.SortOrder.TITLE_AUTHOR.resourceId,
			FileInfo.SortOrder.TITLE_AUTHOR_DESC.resourceId,
			FileInfo.SortOrder.SERIES_AUTHOR.resourceId,
			FileInfo.SortOrder.SERIES_AUTHOR_DESC.resourceId,
			FileInfo.SortOrder.TIMESTAMP.resourceId,
			FileInfo.SortOrder.TIMESTAMP_DESC.resourceId,	
		};
		String[] optionValues = {
			FileInfo.SortOrder.FILENAME.name(),	
			FileInfo.SortOrder.FILENAME_DESC.name(),	
			FileInfo.SortOrder.AUTHOR_TITLE.name(),	
			FileInfo.SortOrder.AUTHOR_TITLE_DESC.name(),
			FileInfo.SortOrder.TITLE_AUTHOR.name(),	
			FileInfo.SortOrder.TITLE_AUTHOR_DESC.name(),
			FileInfo.SortOrder.SERIES_AUTHOR.name(),
			FileInfo.SortOrder.SERIES_AUTHOR_DESC.name(),
			FileInfo.SortOrder.TIMESTAMP.name(),	
			FileInfo.SortOrder.TIMESTAMP_DESC.name(),	
		};
		int[] optionAddInfos = {
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
		};
		ListOption dlg = new ListOption(
			new OptionOwner() {
				public BaseActivity getActivity() { return mActivity; }
				public Properties getProperties() { return properties; }
				public LayoutInflater getInflater() { return mInflater; }
			}, 
			mActivity.getString(R.string.mi_book_sort_order), 
			ReaderView.PROP_APP_BOOK_SORT_ORDER,
				mActivity.getString(R.string.option_add_info_empty_text), "").add(optionValues, optionLabels, optionAddInfos);
		dlg.setOnChangeHandler(() -> {
			final String newValue = properties.getProperty(ReaderView.PROP_APP_BOOK_SORT_ORDER);
			if (newValue != null && oldValue != null && !newValue.equals(oldValue)) {
				log.d("New sort order: " + newValue);
				setSortOrder(newValue);
			}
		});
		dlg.onSelect();
	}

	public void showOPDSDir(final FileInfo fileOrDir, final FileInfo itemToSelect, final String annot) {
		
		if (fileOrDir.fileCount() > 0 || fileOrDir.dirCount() > 0) {
			// already downloaded
			BackgroundThread.instance().executeGUI(() -> showDirectoryInternal(fileOrDir, itemToSelect));
			return;
		}

		for (FileInfo fi: CRRootView.lastCatalogs)
			if (fileOrDir.getOPDSUrl().equals(fi.getOPDSUrl())) lastOPDScatalogURL = fileOrDir.getOPDSUrl();

		if (currDirectory != null && !currDirectory.isOPDSRoot() && !currDirectory.isOPDSBook() && !currDirectory.isOPDSDir()) {
			// show empty directory before trying to download catalog
			showDirectoryInternal(fileOrDir, itemToSelect);
			// update last usage
			mActivity.getDB().updateOPDSCatalog(fileOrDir.getOPDSUrl(), "last_usage", "max");
			mActivity.refreshOPDSRootDirectory(false);
		}
		
		String url = fileOrDir.getOPDSUrl();
		final FileInfo myCurrDirectory = currDirectory;
		if (url != null) {
			try {
				final URL uri = new URL(url);
				DownloadCallback callback = new DownloadCallback() {

					private boolean processNewEntries(DocInfo doc,
							Collection<EntryInfo> entries, boolean notifyNoEntries) {
						log.d("OPDS: processNewEntries: " + entries.size() + (notifyNoEntries ? " - done":" - to continue"));
						if (myCurrDirectory != currDirectory && currDirectory != fileOrDir) {
							log.w("current directory has been changed: ignore downloaded items");
							return false;
						}
						ArrayList<FileInfo> items = new ArrayList<>();
						if ((doc.searchLink != null) && (fileOrDir.parent == null)) {
							FileInfo file = new FileInfo();
							file.isDirectory = true;
							file.pathname = FileInfo.OPDS_DIR_PREFIX + "search:"+ doc.searchLink.href;
							file.title = doc.searchLink.href;
							file.setFilename(mActivity.getString(R.string.opds_search));
							file.isListed = true;
							file.isScanned = true;
							file.tag = doc.searchLink;
							file.parent = fileOrDir;
							items.add(file);
						}
						ArrayList<FileInfo> itemsF = new ArrayList<>();
						for (FileInfo fi: mfavFolders) {
							if ((fi.isOPDSDir())&&(fileOrDir.parent == null)) {
								FileInfo fi2 = new FileInfo(fi);
								fi2.parent = fileOrDir;
								fi2.isFav = true;
								if (fi2.pathname.startsWith(fileOrDir.pathname)) itemsF.add(fi2);
							}
						}
						ArrayList<FileInfo> itemsE = new ArrayList<>();
						for (EntryInfo entry : entries) {
							OPDSUtil.LinkInfo acquisition = entry.getBestAcquisitionLink();
							if (acquisition != null) {
								final FileInfo file = new FileInfo();
								file.isDirectory = false;
								file.pathname = FileInfo.OPDS_DIR_PREFIX + acquisition.href;
								file.annotation = entry.summary;
								file.setFilename(Utils.cleanupHtmlTags(entry.content));
								file.name_crc32 = FileInfo.getFNameCRC(file.getFilename());
								file.title = entry.title;
								file.format = DocumentFormat.byMimeType(acquisition.type);
								file.setAuthors(entry.getAuthors());
								file.setAuthorsLFM(entry.getAuthors());
								file.isListed = true;
								file.isScanned = true;
								file.parent = fileOrDir;
								file.tag = entry;
								file.links = entry.links;
								itemsE.add(file);
								final String sLink = acquisition.href;
								Services.getHistory().getFileInfoByOPDSLink(mActivity.getDB(), acquisition.href, false,
										new History.FileInfo1LoadedCallback() {

											@Override
											public void onFileInfoLoadBegin() {

											}

											@Override
											public void onFileInfoLoaded(final FileInfo fileInfo) {
												if (fileInfo!=null) {
													if (fileInfo.exists()) {
														file.pathnameR = fileInfo.pathname;
														file.arcnameR = fileInfo.arcname;
														file.pathR = fileInfo.path;
														file.opdsLinkR = sLink;
													}
												}
											}
										}
								);
							} else if (entry.link.type!=null && entry.link.type.startsWith("application/atom+xml")) {
								FileInfo file = new FileInfo();
								file.isDirectory = true;
								file.pathname = FileInfo.OPDS_DIR_PREFIX + entry.link.href;
								file.title = Utils.cleanupHtmlTags(entry.content);
								file.setFilename(entry.title);
								file.isListed = true;
								file.isScanned = true;
								file.tag = entry;
								file.links = entry.links;
								file.parent = fileOrDir;
								itemsE.add(file);
							}
						}
						for (FileInfo fiF: itemsF) {
							boolean bExists = false;
							for (FileInfo fiE: itemsE) {
								if (fiE.pathname.equals(fiF.pathname)) {
									bExists = true;
									break;
								}
							}
							for (FileInfo fi: items) {
								if (fi.pathname.equals(fiF.pathname)) {
									bExists = true;
									break;
								}
							}
							if (!bExists) items.add(fiF);
						}
						for (FileInfo fiE: itemsE) {
							boolean bExists = false;
							for (FileInfo fi: items) {
								if (fi.pathname.equals(fiE.pathname)) {
									bExists = true;
									break;
								}
							}
							if (!bExists) items.add(fiE);
						}
						if (items.size() > 0) {
							fileOrDir.replaceItems(items);
							if (currDirectory == fileOrDir)
								currentListAdapter.notifyDataSetChanged();
							else
								showDirectoryInternal(fileOrDir, null);
						} else {
							if (notifyNoEntries)
								mActivity.showToast(R.string.no_entries_found);
						}
						return true;
					}

					@Override
					public boolean onEntries(DocInfo doc,
							Collection<EntryInfo> entries) {
						return processNewEntries(doc, entries, false);
					}

					@Override
					public boolean onFinish(DocInfo doc,
							Collection<EntryInfo> entries) {
						return processNewEntries(doc, entries, true);
					}

					@Override
					public void onError(String message) {
						mEngine.hideProgress();
						mActivity.showToast(message);
					}

					FileInfo downloadDir;
					@Override
					public File onDownloadStart(String type, String initial_url, String url) {
						//mEngine.showProgress(0, "Downloading " + url);
						//mActivity.showToast("Starting download of " + type + " from " + url);
						log.d("onDownloadStart: called for " + type + " " + url );
						downloadDir = Services.getScanner().getDownloadDirectory();
						log.d("onDownloadStart: after getDownloadDirectory()" );
						String subdir = null;
						//asdf - transcribe
						int naming = mActivity.settings().getInt(Settings.PROP_APP_CLOUD_SAVE_FOLDER_NAMING, 0);
						if (!StrUtils.isEmptyStr(fileOrDir.getAuthors())) {
							subdir = Utils.transcribeFileName(fileOrDir.getAuthors(), naming);
							if (subdir.length() > MAX_SUBDIR_LEN)
								subdir = subdir.substring(0, MAX_SUBDIR_LEN);
						} else {
							subdir = "NoAuthor";
						}
						if (downloadDir == null)
							return null;
						File result = new File(downloadDir.getPathName());
						result = new File(result, subdir);
						result.mkdirs();
						downloadDir.findItemByPathName(result.getAbsolutePath());
						log.d("onDownloadStart: returning " + result.getAbsolutePath());
						return result;
					}

					@Override
					public void onDownloadEnd(String type, String initial_url, String url, File file) {
						FileUtils.fileDownloadEndThenOpen(
								type, initial_url, url, file,
								mActivity, mEngine, lastOPDScatalogURL, mScanner,
								downloadDir, fileOrDir, currDirectory,
								annot
						);
					}

					@Override
					public void onDownloadProgress(String type, String url,
							int percent) {
						mEngine.showProgress(percent * 100, "Downloading", "");
					}

				};
				String fileMimeType = fileOrDir.format!=null ? fileOrDir.format.getMimeFormat() : null;
				String defFileName = Utils.transcribeFileName( fileOrDir.title!=null ? fileOrDir.title : fileOrDir.getFilename());
				if (fileOrDir.format != null)
					defFileName = defFileName + fileOrDir.format.getExtensions()[0];
				final OPDSUtil.DownloadTask downloadTask = OPDSUtil.create(lastOPDScatalogURL,
						mActivity, uri, defFileName, fileOrDir.isDirectory?"application/atom+xml":fileMimeType,
						myCurrDirectory.getOPDSUrl(), callback, fileOrDir.username, fileOrDir.password,
						fileOrDir.proxy_addr, fileOrDir.proxy_port,
						fileOrDir.proxy_uname, fileOrDir.proxy_passw,
						fileOrDir.onion_def_proxy);
				downloadTask.run();
			} catch (MalformedURLException e) {
				log.e("MalformedURLException: " + url);
				mActivity.showToast("Wrong URI: " + url);
			}
		}
	}

	public void showOTGDir(final FileInfo fileOrDir, final FileInfo itemToSelect, final String annot) {
		Uri uri =  mActivity.usbDevices.get(fileOrDir.pathname);
		if (uri == null) uri = fileOrDir.documentFile.getUri();
		if (uri != null) {
			DocumentFile rootUri = DocumentFile.fromTreeUri(getContext(), uri);
			ArrayList<FileInfo> items = new ArrayList<>();
			for (DocumentFile docFile: rootUri.listFiles()) {
				if (docFile.exists()) {
					final FileInfo file = new FileInfo();
					file.isDirectory = docFile.isDirectory();
					if (fileOrDir.documentFile == null)
						file.pathname = FileInfo.OTG_DIR_PREFIX + "/";
					else
						file.pathname = fileOrDir.pathname + "/" + fileOrDir.title;
					file.documentFile = docFile;
					file.setFilename(docFile.getName());
					file.name_crc32 = FileInfo.getFNameCRC(docFile.getName());
					file.title = docFile.getName();
					file.format = DocumentFormat.NONE;
					file.isListed = true;
					file.isScanned = true;
					file.parent = fileOrDir;
					items.add(file);
				}
			}
			if (items.size() > 0) {
				fileOrDir.replaceItems(items);
				if (currDirectory == fileOrDir)
					currentListAdapter.notifyDataSetChanged();
				else
					showDirectoryInternal(fileOrDir, null);
			} else {
				mActivity.showToast(R.string.no_entries_found);
			}
		}
	}

	private class ItemGroupsLoadingCallback implements CRDBService.ItemGroupsLoadingCallback {
		private final FileInfo baseDir;
		private final FileInfo itemToSelect;
		public ItemGroupsLoadingCallback(FileInfo baseDir, FileInfo itemToSelect) {
			this.baseDir = baseDir;
			this.itemToSelect = itemToSelect;
		}
		@Override
		public void onItemGroupsLoaded(FileInfo parent) {
			baseDir.setItems(parent);
			//plotn - very experimental
			baseDir.sort(mSortOrder);
			showDirectoryInternal(baseDir, itemToSelect);
		}
	}
	
	private class FileInfoLoadingCallback implements CRDBService.FileInfoLoadingCallback {
		private final FileInfo baseDir;
		private final FileInfo itemToSelect;
		private final int secGroup;
		public FileInfoLoadingCallback(FileInfo baseDir, FileInfo itemToSelect, int secGroup) {
			//plotn - very experimental
			baseDir.sort(mSortOrder);
			this.secGroup = secGroup;
			this.baseDir = baseDir;
			this.itemToSelect = itemToSelect;
		}

		@Override
		public void onFileInfoListLoadBegin(String prefix) {
			showDirectoryLoadingStub();
		}

		@Override
		public void onFileInfoListLoaded(ArrayList<FileInfo> list, String prefix) {
			//baseDir.setItems(list);
			baseDir.clear();
			MainDB.ItemGroupExtractor extractor;
			if (secGroup >= 0) {
				extractor = getExtractor(secGroup);
			} else {
				int sett0 = mActivity.settings().getInt(Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_COMMON, 0);
				extractor = getExtractor(sett0);
				if (StrUtils.getNonEmptyStr(prefix, true).equals(FileInfo.AUTHOR_GROUP_PREFIX)) {
					int sett = mActivity.settings().getInt(Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_AUTHOR, 0);
					if (sett != 0) extractor = getExtractor2(sett);
				} else if (StrUtils.getNonEmptyStr(prefix, true).equals(FileInfo.SERIES_GROUP_PREFIX)) {
					int sett = mActivity.settings().getInt(Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_SERIES, 0);
					if (sett != 0) extractor = getExtractor2(sett);
				} else if (
						(StrUtils.getNonEmptyStr(prefix, true).equals(FileInfo.GENRE_GROUP_PREFIX)) ||
								(StrUtils.getNonEmptyStr(prefix, true).equals(FileInfo.LITRES_GENRE_GROUP_PREFIX))
				) {
					int sett = mActivity.settings().getInt(Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_GENRES, 0);
					if (sett != 0) extractor = getExtractor2(sett);
				}  else if (StrUtils.getNonEmptyStr(prefix, true).equals(FileInfo.TAG_GROUP_PREFIX)
				) {
					int sett = mActivity.settings().getInt(Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_TAGS, 0);
					if (sett != 0) extractor = getExtractor2(sett);
				} else if (StrUtils.getNonEmptyStr(prefix, true).equals(FileInfo.RATING_TAG)) {
					int sett = mActivity.settings().getInt(Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_RATING, 0);
					if (sett != 0) extractor = getExtractor2(sett);
				} else if (StrUtils.getNonEmptyStr(prefix, true).equals(FileInfo.STATE_TAG)) {
					int sett = mActivity.settings().getInt(Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_STATE, 0);
					if (sett != 0) extractor = getExtractor2(sett);
				} else if (
						(StrUtils.getNonEmptyStr(prefix, true).equals(FileInfo.BOOK_DATE_GROUP_PREFIX)) ||
								(StrUtils.getNonEmptyStr(prefix, true).equals(FileInfo.DOC_DATE_GROUP_PREFIX)) ||
								(StrUtils.getNonEmptyStr(prefix, true).equals(FileInfo.PUBL_YEAR_GROUP_PREFIX)) ||
								(StrUtils.getNonEmptyStr(prefix, true).equals(FileInfo.FILE_DATE_GROUP_PREFIX))
				) {
					int sett = mActivity.settings().getInt(Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_DATES, 0);
					if (sett != 0)
						extractor = getExtractor3(sett, StrUtils.getNonEmptyStr(prefix, true));
				}
			}
			if (extractor == null) extractor = new MainDB.ItemGroupAuthorExtractor();
			MainDB.addGroupedItems2(baseDir, "", list, prefix, extractor, 1);
			//plotn - very experimental
			baseDir.sort(mSortOrder);
			showDirectoryInternal(baseDir, itemToSelect);
			mActivity.setBrowserBottomBar(false);
			if (baseDir.isLitresDir()) {
				if (saveParams != null) {
					mActivity.setBrowserTitle("LitRes: " + saveParams.searchString + " [" +
							(saveParams.beginIndex + 1) + ", " + (saveParams.beginIndex + saveParams.count) + "]", baseDir);
					if (saveParams.allowNewOrPop())
						mActivity.setBrowserBottomBar(true);
				}
			}
		}
	};
	
	@Override
	public void onChange(FileInfo object, boolean filePropsOnly) {
		if (currDirectory == null)
			return;
		if (!currDirectory.pathNameEquals(object) && !currDirectory.hasItem(object))
			return;
		if (currDirectory != object) {
			if (currDirectory.pathNameEquals(object)) {
				currDirectory.setItems(object);
			} else if (currDirectory.hasItem(object)) {
				currDirectory.updateItem(object);
			}
		}
		// refresh
		if (filePropsOnly)
			currentListAdapter.notifyInvalidated();
		else
			showDirectoryInternal(currDirectory, null);
	}

	public static LitresSearchParams saveParams;

	public void showDirectory(FileInfo fileOrDir, FileInfo itemToSelect, String addFilter, Object params, int secGroup)
	{
		BackgroundThread.ensureGUI();
		if (fileOrDir != null) {
			if (fileOrDir.isRootDir()) {
				mActivity.showRootWindow();
				return;
			}
			if (fileOrDir.isLoadingStub()) {
				showDirectoryLoadingStub();
				return;
			}
			if (fileOrDir.isNotFoundStub()) {
				showDirectoryNotFoundStub();
				return;
			}
			if (fileOrDir.isOnlineCatalogPluginDir()) {
				if (fileOrDir.getOnlineCatalogPluginPath() == null) {
					// root
					showDirectoryLoadingStub();
					OnlineStoreWrapper plugin = OnlineStorePluginManager.getPlugin(mActivity, fileOrDir.getOnlineCatalogPluginPackage());
					if (plugin != null) {
						String login = plugin.getLogin();
						String password = plugin.getPassword();
						if (login != null && password != null) {
							final FileInfo dir = fileOrDir;
							// just do authentication in background
							plugin.authenticate(login, password, new AuthenticationCallback() {
								@Override
								public void onError(int errorCode, String errorMessage) {
									// ignore error 
								}
								@Override
								public void onSuccess() {
									// ignore result
								}
							});
							showOnlineStoreDirectory(dir);
							return;
						}
					}
				}
				showDirectoryLoadingStub();
				showOnlineStoreDirectory(fileOrDir);
				return;
			}
			if (fileOrDir.isOPDSRoot()) {
				showDirectoryLoadingStub();
				showOPDSRootDirectory();
				return;
			}
			if (fileOrDir.isOTGDir()) {
				showDirectoryLoadingStub();
				showOTGDir(fileOrDir, itemToSelect, "");
				return;
			}
			if (fileOrDir.isOPDSDir()) {
				showDirectoryLoadingStub();
				showOPDSDir(fileOrDir, itemToSelect, "");
				return;
			}
			if (fileOrDir.isSearchShortcut()) {
				showFindBookDialog(false, "", null);
				return;
			}
//			CR genres implementation - commenting
//			if (fileOrDir.isBooksByGenreRoot()) {
//				// Display genres list
//				log.d("Show genres list");
//				mActivity.getDB().loadGenresList(fileOrDir, !mHideEmptyGenres, new ItemGroupsLoadingCallback(fileOrDir, itemToSelect));
//				return;
//			}
			if (fileOrDir.isQSearchShortcut()) {
				currDirectory = null;
				currDirectoryFiltered = null;
				showFindBookDialog(true, fileOrDir.getFilename(), null);
				return;
			}
			if (fileOrDir.isBooksByAuthorRoot()) {
				showDirectoryLoadingStub();
				// refresh authors list
				log.d("Updating authors list");
				boolean withAliases = mActivity.settings().getBool(Settings.PROP_APP_FILE_BROWSER_AUTHOR_ALIASES_ENABLED, false);
				mActivity.getDB().loadAuthorsList(fileOrDir, addFilter, withAliases, new ItemGroupsLoadingCallback(fileOrDir, itemToSelect));
				return;
			}
			if (fileOrDir.isBooksBySeriesRoot()) {
				showDirectoryLoadingStub();
				// refresh authors list
				log.d("Updating series list");
				mActivity.getDB().loadSeriesList(fileOrDir, addFilter, new ItemGroupsLoadingCallback(fileOrDir, itemToSelect));
				return;
			}
            if (fileOrDir.isBooksByBookdateRoot()) {
				showDirectoryLoadingStub();
				// refresh authors list
                log.d("Updating bookdate list");
                mActivity.getDB().loadByDateList(fileOrDir, "book_date_n", new ItemGroupsLoadingCallback(fileOrDir, itemToSelect));
                return;
            }
			if (fileOrDir.isBooksByDocdateRoot()) {
				showDirectoryLoadingStub();
				// refresh authors list
				log.d("Updating docdate list");
				mActivity.getDB().loadByDateList(fileOrDir, "doc_date_n", new ItemGroupsLoadingCallback(fileOrDir, itemToSelect));
				return;
			}
			if (fileOrDir.isBooksByPublyearRoot()) {
				showDirectoryLoadingStub();
				// refresh authors list
				log.d("Updating publyear list");
				mActivity.getDB().loadByDateList(fileOrDir, "publ_year_n", new ItemGroupsLoadingCallback(fileOrDir, itemToSelect));
				return;
			}
			if (fileOrDir.isBooksByFiledateRoot()) {
				showDirectoryLoadingStub();
				// refresh authors list
				log.d("Updating filedate list");
				mActivity.getDB().loadByDateList(fileOrDir, "file_create_time", new ItemGroupsLoadingCallback(fileOrDir, itemToSelect));
				return;
			}
			if (fileOrDir.isBooksByRatingRoot()) {
				showDirectoryLoadingStub();
				log.d("Updating rated books list");
				mActivity.getDB().loadBooksByRating(1, 10, new FileInfoLoadingCallback(fileOrDir, itemToSelect, secGroup));
				return;
			}
			if (fileOrDir.isBooksByStateFinishedRoot()) {
				showDirectoryLoadingStub();
				log.d("Updating books by state=finished");
				mActivity.getDB().loadBooksByState(FileInfo.STATE_FINISHED, new FileInfoLoadingCallback(fileOrDir, itemToSelect, secGroup));
				return;
			}
			if (fileOrDir.isBooksByStateReadingRoot()) {
				showDirectoryLoadingStub();
				log.d("Updating books by state=reading");
				mActivity.getDB().loadBooksByState(FileInfo.STATE_READING, new FileInfoLoadingCallback(fileOrDir, itemToSelect, secGroup));
				return;
			}
			if (fileOrDir.isBooksByStateToReadRoot()) {
				showDirectoryLoadingStub();
				log.d("Updating books by state=toRead");
				mActivity.getDB().loadBooksByState(FileInfo.STATE_TO_READ, new FileInfoLoadingCallback(fileOrDir, itemToSelect, secGroup));
				return;
			}
			if (fileOrDir.isBooksByTitleRoot()) {
				showDirectoryLoadingStub();
				// refresh authors list
				log.d("Updating title list");
				mActivity.getDB().loadTitleList(fileOrDir, new ItemGroupsLoadingCallback(fileOrDir, itemToSelect));
				return;
			}
			if (fileOrDir.isBooksByTitleLevel()) {
				showDirectoryLoadingStub();
				// refresh authors list
				log.d("Updating title list");
				mActivity.getDB().loadTitleList(fileOrDir, new ItemGroupsLoadingCallback(fileOrDir, itemToSelect));
				return;
			}
//			CR genres implementation - commenting
//			if (fileOrDir.isBooksByGenreDir()) {
//				log.d("Updating genres book list");
//				mActivity.getDB().loadGenresBooks(fileOrDir.getGenreCode(), !mHideEmptyGenres, new FileInfoLoadingCallback(fileOrDir, itemToSelect));
//				return;
//			}
			if (fileOrDir.isBooksByAuthorDir()) {
				showDirectoryLoadingStub();
				log.d("Updating author book list");
				mActivity.getDB().loadAuthorBooks(fileOrDir.getAuthorId(), addFilter, new FileInfoLoadingCallback(fileOrDir, itemToSelect, secGroup));
				return;
			}
			if (fileOrDir.isBooksByCalibreAuthorDir()) {
				showDirectoryLoadingStub();
				log.d("Updating calibre author book list");
				mActivity.getDB().loadCalibreAuthorBooks(fileOrDir.getAuthorId(), addFilter,
						new FileInfoLoadingCallback(fileOrDir, itemToSelect, secGroup));
				return;
			}
			if (fileOrDir.isBooksBySeriesDir()) {
				showDirectoryLoadingStub();
				log.d("Updating series book list");
				mActivity.getDB().loadSeriesBooks(fileOrDir.getSeriesId(), addFilter, new FileInfoLoadingCallback(fileOrDir, itemToSelect, secGroup));
				return;
			}
			if (fileOrDir.isBooksByBookdateDir()) {
				showDirectoryLoadingStub();
				log.d("Updating bookdate book list");
				mActivity.getDB().loadByDateBooks(fileOrDir.getBookdateId(), "book_date_n", new FileInfoLoadingCallback(fileOrDir, itemToSelect, secGroup));
				return;
			}
			if (fileOrDir.isBooksByDocdateDir()) {
				showDirectoryLoadingStub();
				log.d("Updating docdate book list");
				mActivity.getDB().loadByDateBooks(fileOrDir.getDocdateId(), "doc_date_n", new FileInfoLoadingCallback(fileOrDir, itemToSelect, secGroup));
				return;
			}
			if (fileOrDir.isBooksByPublyearDir()) {
				showDirectoryLoadingStub();
				log.d("Updating publyear book list");
				mActivity.getDB().loadByDateBooks(fileOrDir.getPublyearId(), "publ_year_n", new FileInfoLoadingCallback(fileOrDir, itemToSelect, secGroup));
				return;
			}
			if (fileOrDir.isBooksByFiledateDir()) {
				showDirectoryLoadingStub();
				log.d("Updating filedate book list");
				mActivity.getDB().loadByDateBooks(fileOrDir.getFiledateId(), "file_create_time",  new FileInfoLoadingCallback(fileOrDir, itemToSelect, secGroup));
				return;
			}
			if (fileOrDir.isBooksByGenreRoot()) {
				showDirectoryLoadingStub();
				// refresh authors list
				log.d("Updating genre list");
				mActivity.getDB().loadGenresList(fileOrDir, new ItemGroupsLoadingCallback(fileOrDir, itemToSelect));
				return;
			}
			if (fileOrDir.isBooksByGenreDir()) {
				showDirectoryLoadingStub();
				log.d("Updating genre book list");
				mActivity.getDB().loadGenreBooks(fileOrDir.getGenreId(), new FileInfoLoadingCallback(fileOrDir, itemToSelect, secGroup));
				return;
			}
			if (fileOrDir.isBooksByTagRoot()) {
				showDirectoryLoadingStub();
				// refresh authors list
				log.d("Updating tag list");
				mActivity.getDB().loadTagsList(fileOrDir, new ItemGroupsLoadingCallback(fileOrDir, itemToSelect));
				return;
			}
			if (fileOrDir.isBooksByTagDir()) {
				showDirectoryLoadingStub();
				log.d("Updating tag book list");
				mActivity.getDB().loadTagBooks(fileOrDir.getTagId(), new FileInfoLoadingCallback(fileOrDir, itemToSelect, secGroup));
				return;
			}
			if (fileOrDir.isLitresDir()) {
				LitresSearchParams lsp = (LitresSearchParams) params;
				if ((lsp != null) && (saveParams != null)) lsp.newOrPop = saveParams.newOrPop;
				if (lsp == null) lsp = saveParams;
				else saveParams = lsp;
				CloudAction.litresSearchBooks(mActivity, lsp, new FileInfoLoadingCallback(fileOrDir, itemToSelect, secGroup));
			}
			if (fileOrDir.isBooksByLitresGenreRoot()) {
				LitresSearchParams lsp = (LitresSearchParams) params;
				if (lsp == null) lsp = saveParams;
				else saveParams = lsp;
				CloudAction.litresGetGenreList(mActivity, lsp, new FileInfoLoadingCallback(fileOrDir, itemToSelect, secGroup));
			}
			if (fileOrDir.isBooksByLitresCollectionRoot()) {
				LitresSearchParams lsp = (LitresSearchParams) params;
				if (lsp == null) lsp = saveParams;
				else saveParams = lsp;
				CloudAction.litresGetCollectionList(mActivity, lsp, new FileInfoLoadingCallback(fileOrDir, itemToSelect, secGroup));
			}
			if (fileOrDir.isBooksByLitresSequenceRoot()) {
				LitresSearchParams lsp = (LitresSearchParams) params;
				if (lsp == null) lsp = saveParams;
				else saveParams = lsp;
				CloudAction.litresGetSequenceList(mActivity, lsp, new FileInfoLoadingCallback(fileOrDir, itemToSelect, secGroup));
			}
			if (fileOrDir.isBooksByLitresPersonRoot()) {
				LitresSearchParams lsp = (LitresSearchParams) params;
				if (lsp == null) lsp = saveParams;
				else saveParams = lsp;
				CloudAction.litresSearchPersonsList(mActivity, lsp, new FileInfoLoadingCallback(fileOrDir, itemToSelect, secGroup));
			}
			if (fileOrDir.isCalibreRoot()) {
				if (fileOrDir.isCalibreByAuthors())
					//mActivity.showToast("calibre "+fileOrDir.pathname+" "+fileOrDir.getFilename());
					showDirectoryLoadingStub();
					// refresh calibre authors list
					log.d("Updating calibre authors list");
					boolean withAliases = mActivity.settings().getBool(Settings.PROP_APP_FILE_BROWSER_AUTHOR_ALIASES_ENABLED, false);
					mActivity.getDB().loadCalibreAuthorsList(fileOrDir, addFilter, withAliases,
							new ItemGroupsLoadingCallback(fileOrDir, itemToSelect));
					return;
			}
		} else {
			if (currDirectory != null)
				return; // just show current directory
			if (mScanner.getRoot() != null && mScanner.getRoot().dirCount() > 0) {
				if (mScanner.getRoot().getDir(0).fileCount() > 0) {
					fileOrDir = mScanner.getRoot().getDir(0);
					itemToSelect = mScanner.getRoot().getDir(0).getFile(0);
				} else {
					fileOrDir = mScanner.getRoot();
					itemToSelect = mScanner.getRoot().dirCount() > 1 ? mScanner.getRoot().getDir(1) : null;
				}
			}
		}
		final FileInfo file = fileOrDir == null || fileOrDir.isDirectory ? itemToSelect : fileOrDir;
		final FileInfo dir = fileOrDir!=null && !fileOrDir.isDirectory ? mScanner.findParent(file, mScanner.getRoot()) : fileOrDir;
		if (dir != null) {
			if (dir.isSpecialDir()) {
				if (dir.allowSorting())
					dir.sort(mSortOrder);
				showDirectoryInternal(dir, file);
			} else {
				showDirectoryLoadingStub();
				// if previous scan is in progress, interrupt it
				if (!mScanControl.isStopped())
					mScanControl.stop();
				mScanControl = new Scanner.ScanControl();
				mScanner.scanDirectory(mActivity.getDB(), dir, () -> {
					if (dir.allowSorting())
						dir.sort(mSortOrder);
					showDirectoryInternal(dir, file);
					scrollToLastPos(true);
					mActivity.setBrowserProgressStatus(true);
				}, (scanControl) -> {
					if (!scanControl.isStopped()) {
						if (dir.allowSorting())
							dir.sort(mSortOrder);
						showDirectoryInternal(dir, file);
					}
					mActivity.setBrowserProgressStatus(false);
					// try to restore position
					scrollToLastPos(false);
				}, false, mScanControl);
			}
		} else
			showDirectoryInternal(null, file);
	}
	
	public void scanCurrentDirectoryRecursive() {
		if (currDirectory == null || currDirectory.isSpecialDir())
			return;
		if (currDirectory.isOPDSDir()) {
			currDirectory.dirs = null;
			currDirectory.files = null;
			showOPDSDir(currDirectory, null, "");
		} else {
			log.i("scanCurrentDirectoryRecursive started");
			if (!mScanControl.isStopped())
				mScanControl.stop();
			mScanControl = new Scanner.ScanControl();
			final ProgressDialog dlg = ProgressDialog.show(mActivity,
					mActivity.getString(R.string.dlg_scan_title),
					mActivity.getString(R.string.dlg_scan_message),
					true, true, dialog -> {
						log.i("scanCurrentDirectoryRecursive : stop handler");
						mScanControl.stop();
					});
			mScanner.scanDirectory(mActivity.getDB(), currDirectory, () -> {
				showDirectoryInternal(currDirectory, null);
			}, (scanControl) -> {
				log.i("scanCurrentDirectoryRecursive : finish handler");
				if (!scanControl.isStopped()) {
					if (currDirectory.allowSorting())
						currDirectory.sort(mSortOrder);
					showDirectoryInternal(currDirectory, null);
				}
				if (dlg.isShowing())
					dlg.dismiss();
			}, true, mScanControl);
		}
	}


	public int isFileBrowserModeSelected() {
		return fileBrowserModeSelected;
	}

	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		ImageView bmImage;

		public DownloadImageTask(ImageView bmImage) {
			this.bmImage = bmImage;
		}

		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon11 = null;
			try {
				InputStream in = new java.net.URL(urldisplay).openStream();
				mIcon11 = BitmapFactory.decodeStream(in);
			} catch (Exception e) {
				Log.e("Error", e.getMessage());
				e.printStackTrace();
			}
			return mIcon11;
		}

		protected void onPostExecute(Bitmap result) {
			bmImage.setImageBitmap(result);
		}
	}

	public void setFileBrowserModeSelected(int modeSelected) {
		if (fileBrowserModeSelected != modeSelected) {
			fileBrowserModeSelected = modeSelected;
			if (modeSelected == 4) {
				mSortOrder = FileInfo.SortOrder.FILENAME;
				mActivity.saveSetting(ReaderView.PROP_APP_BOOK_SORT_ORDER, mSortOrder.name());
			}
			if (isShown() && currDirectory != null) {
				showDirectory(currDirectory, null, "", null, -1);
			}
		}
	}

	private int fileBrowserModeSelected = 0;

	private FileListAdapter currentListAdapter;
	
	private class FileListAdapter extends BaseListAdapter {

		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			if (currDirectoryFiltered == null)
				return 0;
			return currDirectoryFiltered.fileCount() + currDirectoryFiltered.dirCount();
		}

		public Object getItem(int position) {
			if (currDirectoryFiltered == null)
				return null;
			if (position < 0)
				return null;
			return currDirectoryFiltered.getItem(position);
		}

		public long getItemId(int position) {
			if (currDirectoryFiltered == null)
				return 0;
			return position;
		}

		public final int VIEW_TYPE_LEVEL_UP = 0;
		public final int VIEW_TYPE_DIRECTORY = 1;
		public final int VIEW_TYPE_FILE = 2;
		public final int VIEW_TYPE_FILE_SIMPLE = 3;
		public final int VIEW_TYPE_OPDS_BOOK = 4;
		public final int VIEW_TYPE_DIRECTORY_LITRES = 5;
		public final int VIEW_TYPE_DIRECTORY_LITRES_1BTN = 6;
		public final int VIEW_TYPE_DIRECTORY_LITRES_2BTN = 7;
		public final int VIEW_TYPE_COUNT = 8;
		public final int VIEW_TYPE_STUB = 9;

		public int getItemViewType(int position) {
			Object itm = getItem(position);
			if (currDirectoryFiltered == null)
				return 0;
			if (position < 0)
				return Adapter.IGNORE_ITEM_VIEW_TYPE;
			if (itm instanceof FileInfo) {
				FileInfo fi = (FileInfo) itm;
				if (fi.isLoadingStub() || (fi.isNotFoundStub())) return VIEW_TYPE_DIRECTORY;
			}
			if (position < currDirectoryFiltered.dirCount()) {
				if (itm instanceof FileInfo) {
					FileInfo fi = (FileInfo)itm;
					if ((fi.isLitresSpecialDir()) &&
						(!fi.isLitresPagination())
						)
					{
						if (fi.isLitresPerson()) {
							return VIEW_TYPE_DIRECTORY_LITRES_2BTN;
						}
						if (fi.isBooksByLitresGenreGroupDir()) {
							return VIEW_TYPE_DIRECTORY_LITRES_1BTN;
						}
						if (fi.isBooksByLitresGenreDir())
							if ((StrUtils.isEmptyStr(fi.top_genres)) && (StrUtils.isEmptyStr(fi.top_arts)))
								return VIEW_TYPE_DIRECTORY;
						return VIEW_TYPE_DIRECTORY_LITRES;
					}
				}
				return VIEW_TYPE_DIRECTORY;
			}
			int position2 = position - currDirectoryFiltered.dirCount();
			if (position2 < currDirectoryFiltered.fileCount()) {
				if (itm instanceof FileInfo) {
					FileInfo fi = (FileInfo)itm;
					if (fi.isOPDSBook())
						return VIEW_TYPE_OPDS_BOOK;
					if (fi.isLitresPrefix())
						return VIEW_TYPE_FILE;
				}
				return (fileBrowserModeSelected == 4) ? VIEW_TYPE_FILE_SIMPLE : VIEW_TYPE_FILE;
			}
			return Adapter.IGNORE_ITEM_VIEW_TYPE;
		}

		class ViewHolder {
			int viewType;
			ImageView image;
			TextView name;
			TextView author;
			TextView series;
			TextView filename;
			LinearLayout linearLayoutF1and2;
			TextView field1;
			TextView field2;
			View fbody;
			TextView linkToFile;
			TextView fieldState;
			ImageView imageAddInfo;
			LinearLayout browserItemBtnsBody;
			ImageView imageAddMenu;
			ImageView imageFavFolder;
			Button btn1;
			Button btn2;
			LinearLayout llTags;
			//TextView field3;
			//ImageView infoButton; //CR - commenting

			void setText(TextView view, String text, int color)
			{
				setText(view, text, color, null);
			}

			void setText(TextView view, String text, int color, int count)
			{
				if (count>0)
					setText(view, text, color, null);
				else
					setText(view, "", color, null);
			}

			void setText(TextView view, String text, int color, String pathname)
			{
				if (pathname != null) {
					String s = pathname;
					if (s.startsWith("@author:"))
						s = s.replace("@author:", "").trim();
					if (s.startsWith("@authorId:"))
						s = s.replace("@authorId:", "").trim();
					long l = 0L;
					try {
						l = Long.valueOf(s);
					} catch (Exception e) {

					}
					view.setTypeface(null, Typeface.NORMAL);
					if (l > 10000000L) view.setTypeface(null, Typeface.ITALIC);
				}

				if (view == null)
					return;
				if (text != null && text.length() > 0) {
					view.setText(text);
					if (color != 0) view.setTextColor(color);
					view.setVisibility(ViewGroup.VISIBLE);
				} else {
					view.setText(null);
					view.setVisibility(ViewGroup.INVISIBLE);
				}
			}

			@SuppressLint("ResourceType")
			void setItem(final FileInfo item, FileInfo parentItem)
			{
				int colorIcon;
				int colorGrayC;
				TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
						{R.attr.colorIcon, R.attr.colorThemeGray2Contrast});
				colorIcon = a.getColor(0, Color.GRAY);
				colorGrayC = a.getColor(1, Color.GRAY);
				int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
				int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
				if (item == null) {
					image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_back_drawable, R.drawable.cr3_browser_back));
                    mActivity.tintViewIcons(image,true);
					String thisDir = "";
					if (parentItem != null) {
						if (parentItem.pathname.startsWith("@"))
							thisDir = "/" + parentItem.getFilename();
//						else if ( parentItem.isArchive )
//							thisDir = parentItem.arcname;
						else
							thisDir = parentItem.pathname;
						//parentDir = parentItem.path;
					}
					name.setText(thisDir);
					return;
				}
				if (imageAddInfo!=null) {
					mActivity.tintViewIcons(imageAddInfo,true);
					imageAddInfo.setOnClickListener(v -> Services.getHistory().getOrCreateBookInfo(mActivity.getDB(), item, bookInfo -> {
						FileInfo itemF = item;
						try {
							if (StrUtils.isEmptyStr(item.getAuthors()))
								itemF = FileUtils.getFileProps(itemF, new File(item.getBasePath()),
										new FileInfo(new File(item.getBasePath()).getParent()), true);
							if (StrUtils.isEmptyStr(itemF.getAuthors()))
								Services.getEngine().scanBookProperties(itemF);
						} catch (Exception e) {
							//do nothing
						}
						doBookAddInfoClick(itemF);
					}));
				}

				if (imageAddMenu!=null) {
					mActivity.tintViewIcons(imageAddMenu,true);
					imageAddMenu.setOnClickListener(v -> {
						if (item != null) {
							FileInfo itemF = item;
							try {
								if (StrUtils.isEmptyStr(item.getAuthors()))
									itemF = FileUtils.getFileProps(itemF, new File(item.getBasePath()),
											new FileInfo(new File(item.getBasePath()).getParent()), true);
								if (StrUtils.isEmptyStr(itemF.getAuthors()))
									Services.getEngine().scanBookProperties(itemF);
							} catch (Exception e) {

							}
							selectedItem = itemF;
							showItemPopupMenu();
						}
					});
				}

				if (browserItemBtnsBody != null) {
					browserItemBtnsBody.setOnClickListener(v -> {
						boolean b = false;
						if (imageFavFolder != null)
							if (imageFavFolder.getVisibility() == INVISIBLE) b = true;
						if (b && (item != null)) {
							FileInfo itemF = item;
							try {
								if (StrUtils.isEmptyStr(item.getAuthors()))
									itemF = FileUtils.getFileProps(itemF, new File(item.getBasePath()),
											new FileInfo(new File(item.getBasePath()).getParent()), true);
								if (StrUtils.isEmptyStr(itemF.getAuthors()))
									Services.getEngine().scanBookProperties(itemF);
							} catch (Exception e) {

							}
							selectedItem = itemF;
							showItemPopupMenu();
						}
					});
				}

				if (imageFavFolder!=null) {
                    imageFavFolder.setVisibility(VISIBLE);
					boolean isFav = false;
					if
					(
						(mOnlyFSFolders.contains(item.pathname))
						||
						(containsEq(mfavFolders, item.pathname))
					)
					{
						imageFavFolder.setImageResource(
								Utils.resolveResourceIdByAttr(mActivity,
										R.attr.attr_icons8_fav_star_filled, R.drawable.icons8_fav_star_filled));
                        mActivity.tintViewIcons(imageFavFolder,true);
					} else {
						imageFavFolder.setImageResource(
								Utils.resolveResourceIdByAttr(mActivity,
										R.attr.attr_icons8_fav_star, R.drawable.icons8_fav_star));
                        mActivity.tintViewIcons(imageFavFolder,true);
					}
					if (item.pathname.startsWith(FileInfo.OPDS_DIR_PREFIX+"search:")) imageFavFolder.setVisibility(INVISIBLE);
					if (!item.pathname.startsWith(FileInfo.OPDS_DIR_PREFIX)) {
						File f = new File(item.pathname);
						if (!f.exists()) imageFavFolder.setVisibility(INVISIBLE);
					}
					if (!item.isOPDSDir()) item.isFav = false;
					Long id = -1L;
					if (!item.isOPDSDir())
						if (mFileSystemFoldersH!=null)
							for (Map.Entry<Long, String> entry : mFileSystemFoldersH.entrySet()) {
								if (entry.getValue().equals(item.pathname)) {
									id = entry.getKey();
									item.isFav = true;
									imageFavFolder.setImageResource(
											Utils.resolveResourceIdByAttr(mActivity,
													R.attr.attr_icons8_fav_star_filled, R.drawable.icons8_fav_star_filled));
									mActivity.tintViewIcons(imageFavFolder,true);
								}
							}
					final Long id2 = id;
					imageFavFolder.setOnClickListener(v -> {
						if (item != null) {
							selectedItem = item;
							boolean bCont = containsEq(mfavFolders, item.pathname);
							bCont = bCont && item.isOPDSDir();
							if (!mOnlyFSFolders.contains(item.pathname)) {
								if (!(item.isFav||bCont)) {
									addToFavorites(selectedItem);
									item.isFav = true;
									if (!item.isOPDSDir()) mFileSystemFoldersH.put(item.id, item.pathname);
									if (item.isOPDSDir()) {
										boolean bExists = false;
										for (FileInfo fiF: mfavFolders)
											if (fiF.pathname.equals(item.pathname)) {
												bExists = true;
												break;
											}
										if (!bExists) mfavFolders.add(item);
									}
								} else {
									FileInfo fi = new FileInfo(item);
									if (!item.isOPDSDir()) fi.id = id2;
									Services.getFileSystemFolders().removeFavoriteFolder(mActivity.getDB(), fi);
									item.isFav = false;
									if (!item.isOPDSDir()) mFileSystemFoldersH.remove(id2);
									else {
										for (FileInfo fi2: mfavFolders) {
											if (fi2.pathname.equals(item.pathname)) {
												mfavFolders.remove(fi2);
												break;
											}
										}
									}
								}
								currentListAdapter = new FileListAdapter();
								mListView.setAdapter(currentListAdapter);
								currentListAdapter.notifyDataSetChanged();
							}
						}
					});
				}
				boolean doTint = true;
				if (item.isDirectory) {
					// CR implementation of genres - commenting
					//if (item.isBooksByGenreRoot() || item.isBooksByGenreDir())
					//	image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_folder_authors_drawable, R.drawable.cr3_browser_folder_authors));
					if (item.isLitresPaginationNextPage()) {
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_button_next_drawable, R.drawable.icons8_forward));
					} else if (item.isLitresPaginationPrevPage())
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_button_prev_drawable, R.drawable.icons8_back));
					else if (item.isBooksByAuthorDir() || item.isBooksByAuthorRoot() || item.isBooksByLitresPersonDir()
							|| item.isBooksByLitresPersonRoot() || item.isBooksByCalibreAuthorDir())
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.attr_icons8_folder_author, R.drawable.icons8_folder_author));
					else if (item.isBooksBySeriesRoot() || item.isBooksByLitresSequenceDir() || item.isBooksByLitresSequenceRoot() || item.isLitresSequence())
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.attr_icons8_folder_hash, R.drawable.icons8_folder_hash));
                    else if (item.isBooksByBookdateRoot()||item.isBooksByDocdateRoot()||item.isBooksByPublyearRoot()||item.isBooksByFiledateRoot())
                        image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.attr_icons8_folder_year, R.drawable.icons8_folder_year));
                    else if (item.isBooksByTitleRoot())
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_folder_authors_drawable, R.drawable.cr3_browser_folder_authors));
					else if (item.isBooksByRatingRoot() )
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.attr_icons8_folder_stars, R.drawable.icons8_folder_stars));
					else if (item.isBooksByStateReadingRoot() || item.isBooksByStateToReadRoot() || item.isBooksByStateFinishedRoot())
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_folder_authors_drawable, R.drawable.cr3_browser_folder_authors));
					else if (item.isOPDSRoot() || item.isOPDSDir())
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_folder_opds_drawable, R.drawable.cr3_browser_folder_opds));
					else if (item.isBooksByLitresGenreRoot() || item.isBooksByLitresGenreDir() || item.isBooksByLitresGenreGroupDir()
						|| item.isBooksByGenreDir() || item.isBooksByGenreRoot())
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.attr_icons8_theatre_mask, R.drawable.icons8_theatre_mask));
					else if (item.isBooksByTagRoot() || item.isBooksByTagDir())
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.attr_icons8_tag, R.drawable.icons8_tag));
					else if (item.isOnlineCatalogPluginDir()) {
						image.setImageResource(R.drawable.litres);
						doTint = false;
					}
					else if (item.isSearchShortcut())
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_find_drawable, R.drawable.cr3_browser_find));
					else if (item.isRecentDir())
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_folder_recent_drawable, R.drawable.cr3_browser_folder_recent));
					else if (item.isArchive)
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_folder_zip_drawable, R.drawable.cr3_browser_folder_zip));
					else
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_folder_drawable, R.drawable.cr3_browser_folder));
                    if (doTint) mActivity.tintViewIcons(image,true);
					String title = item.getFilename();
					
					if (item.isOnlineCatalogPluginDir())
						title = translateOnlineStorePluginItem(item);
					
					setText(name, title, 0, item.pathname);

					if (item.isLitresPrefix()) {
						setText(series, "", colorIcon);
						setText(author, "", colorIcon);
					}

//					CR implementation of genres - commenting
//					if ( item.isBooksByGenreDir() ) {
//						if (item.tag instanceof Integer) {
//							int code = (Integer) item.tag;
//							setText(field1, "books: " + (code & FileInfo.GENRE_DATA_BOOKCOUNT_MASK));
//							if ((code & FileInfo.GENRE_DATA_INCCHILD_MASK) == FileInfo.GENRE_DATA_INCCHILD_MASK) {
//								setText(field2, mActivity.getString(R.string.including_subgenres));
//							} else {
//								setText(field2, "");
//							}
//						} else {
//							setText(field1, "");
//							setText(field2, "");
//						}
//					}

					if (item.isBooksBySeriesDir() || item.isBooksByBookdateDir() || item.isBooksByDocdateDir()
							|| item.isBooksByPublyearDir() || item.isBooksByFiledateDir() ||
							item.isBooksByAuthorDir() || item.isBooksByCalibreAuthorDir() ||
							item.isBooksByGenreDir() || item.isBooksByTitleLevel()
							|| item.isBooksByTagDir()) {
						int bookCount = 0;
						if (item.fileCount() > 0)
							bookCount = item.fileCountTotal();
						else if (item.tag != null && item.tag instanceof Integer)
							bookCount = (Integer)item.tag;
						if (!item.isLitresPrefix())
							setText(field1, mActivity.getString(R.string.books_l) + " " + bookCount, colorIcon, bookCount);
						else
							setText(field1, "", colorIcon);
						if (item.isLoadingStub())
							setText(field1, mActivity.getString(R.string.action_in_progress), colorIcon);
						if  (item.isNotFoundStub())
							setText(field1, mActivity.getString(R.string.action_finished), colorIcon);
						//setText(field2, "folders: 0", colorIcon);
						setText(field2, "", colorIcon);
						setText(fieldState, "", colorIcon);
						if (item.isBooksByTagDir()) {
							if (fbody != null)
								fbody.setBackgroundColor(CoverpageManager.randomColor((item.getFilename()).hashCode()));
						}
					}  else  if (item.isOPDSDir()) {
						setText(field1, item.title, colorIcon);
						setText(field2, "", colorIcon);
						setText(fieldState, "", colorIcon);
					} else  if (!item.isOPDSDir() && !item.isSearchShortcut() && ((!item.isOPDSRoot()
                            && !item.isBooksByAuthorRoot() && !item.isBooksBySeriesRoot() && !item.isBooksByBookdateRoot()
							&& !item.isBooksByDocdateRoot() && !item.isBooksByPublyearRoot() && !item.isBooksByFiledateRoot()
                            && !item.isBooksByTitleRoot()) || item.dirCount()>0) && !item.isOnlineCatalogPluginDir()
							&& !item.isLitresPrefix()) {
						int fc = item.fileCountTotal();
						setText(field1,mActivity.getString(R.string.books_l) + " " + fc, colorIcon, fc);
						if (item.dirCount()>0)
							setText(field2, mActivity.getString(R.string.folders_l) + " " + item.dirCount(), colorIcon, item.dirCount());
						else
							setText(field2, "", colorIcon);
						setText(fieldState, "", colorIcon);
					} else {
						setText(field1, "", colorIcon);
						setText(field2, "", colorIcon);
						setText(fieldState, "", colorIcon);
					}
					if (item.isLoadingStub())
						setText(field1, mActivity.getString(R.string.action_in_progress), colorIcon);
					if  (item.isNotFoundStub())
						setText(field1, mActivity.getString(R.string.action_finished), colorIcon);
					if (item.isLitresCollection() || item.isLitresSequence() || item.isBooksByLitresGenreGroupDir() ||
							item.isBooksByLitresGenreDir() || item.isLitresPerson()) {
						setText(series, StrUtils.getNonEmptyStr(item.top_arts, true).replace("|", "; "), colorIcon);
					}
					if (item.isLitresSequence() || item.isLitresPerson()) {
						setText(author, StrUtils.getNonEmptyStr(item.top_genres, true).replace("|", "; "), colorIcon);
					}
					if (item.isLitresPerson()) {
						btn1.setBackgroundColor(colorGrayC);
						btn1.setText(R.string.info);
						btn1.setOnClickListener(v -> {
							mActivity.showCloudItemInfo(item, FileBrowser.this, currDirectory);
						});
						btn2.setBackgroundColor(colorGrayC);
						btn2.setOnClickListener(v -> {
							mActivity.showCloudItemInfo(item, FileBrowser.this, currDirectory);
						});
						btn2.setText(R.string.books);
						btn2.setOnClickListener(v -> {
							int personId = Integer.parseInt(item.pathname.replace(FileInfo.LITRES_PERSONS_PREFIX, ""));
							LitresSearchParams lsp = new LitresSearchParams(LitresSearchParams.SEARCH_TYPE_ARTS_BY_PERSON, 0,0, 20,
									"", personId, 0);
							mActivity.showBrowser(FileInfo.LITRES_TAG, lsp);
						});
					}
					if (item.isBooksByLitresGenreGroupDir()) {
						btn1.setBackgroundColor(colorGrayC);
						btn1.setText(R.string.books);
						btn1.setOnClickListener(v -> {
							int genreId = Integer.parseInt(item.pathname.replace(FileInfo.LITRES_GENRE_GROUP_PREFIX, ""));
							LitresSearchParams lsp = new LitresSearchParams(LitresSearchParams.SEARCH_TYPE_ARTS_BY_GENRE, 0,0, 20,
									"", genreId, 0);
							mActivity.showBrowser(FileInfo.LITRES_TAG, lsp);
						});
					}
				} else {
					boolean isSimple = (viewType == VIEW_TYPE_FILE_SIMPLE);
					if (image != null) {
						if (isSimple) {
							image.setImageResource(
									//item.format.getIconResourceId()
									item.format.getIconResourceIdThemed(mActivity)
							);
                            mActivity.tintViewIcons(image,true);
						} else {
							if (coverPagesEnabled) {
								image.setImageDrawable(mCoverpageManager.getCoverpageDrawableFor(mActivity.getDB(), item));
								if (item.isOPDSBook()) {
									final FileInfo finalItem = item;
									if	(
											(
												(!StrUtils.isEmptyStr(item.pathnameR))||
												(!StrUtils.isEmptyStr(item.arcnameR))
											) && (!StrUtils.isEmptyStr(item.opdsLinkR))
									) {
											Services.getHistory().getFileInfoByOPDSLink(mActivity.getDB(), finalItem.opdsLinkR, finalItem.isLitresBook(),
													new History.FileInfo1LoadedCallback() {

														@Override
														public void onFileInfoLoadBegin() {

														}

														@Override
														public void onFileInfoLoaded(final FileInfo fileInfo) {
															if (fileInfo!=null) {
																image.setImageDrawable(mCoverpageManager.getCoverpageDrawableFor(mActivity.getDB(), fileInfo));
															} else {
																image.setImageDrawable(mCoverpageManager.getCoverpageDrawableFor(mActivity.getDB(), item));
															}
														}
													}
											);
										}
								}
//								still doesnt work
//								if (item!=null)
//									if (item.isOPDSBook() || item.isOPDSRoot() || item.isOPDSDir()) {
//										for (OPDSUtil.LinkInfo link: item.links) {
//											if (link.type.contains("image")) {
//												new DownloadImageTask(image).execute(link.href);
//												break;
//											}
//										}
//									}
								image.setMinimumHeight(coverPageHeight);
								image.setMinimumWidth(coverPageWidth);
								image.setMaxHeight(coverPageHeight);
								image.setMaxWidth(coverPageWidth);
								image.setTag(item);
							} else {
								image.setImageDrawable(null);
								image.setMinimumHeight(0);
								image.setMinimumWidth(0);
								image.setMaxHeight(0);
								image.setMaxWidth(0);
							}
                            //mActivity.tintViewIcons(image,true);
						}
					}
					if (isSimple) {
						String fn = item.getFileNameToDisplay2();
						setText(filename, fn, 0);
					} else {
						setText(author, Utils.formatAuthors(item.getAuthors()), colorIcon);
                        //setText( author, item.authors );
                        String seriesName = Utils.formatSeries(item.series, item.seriesNumber);
						String genres = Utils.getGenreText(mActivity, item, false);
						if (!StrUtils.isEmptyStr(genres)) {
							if (!StrUtils.isEmptyStr(seriesName))
								seriesName = seriesName + ";\n" + genres + "\n";
							else
								seriesName = genres + "\n";
						}
						String title = StrUtils.getNonEmptyStr(item.title, true);
						String filename1 = StrUtils.getNonEmptyStr(item.getFilename(),true);
						String filename2 = StrUtils.getNonEmptyStr(item.isArchive && item.arcname != null /*&& !item.isDirectory */
								? new File(item.arcname).getName() : null, true);
						if (!StrUtils.isEmptyStr(filename1.trim())) {
							if (fileBrowserModeSelected == 2)
								filename2 = StrUtils.getNonEmptyStr(filename2, true).replace(filename1.trim(), "*");
							else
								filename2 = StrUtils.getNonEmptyStr(filename2, true);
						}
						if (!StrUtils.isEmptyStr(title)) {
							filename1 = StrUtils.getNonEmptyStr(filename1, true).replace(title, "*");
						}
						String onlineBookInfo = "";
						if (item.getOnlineStoreBookInfo() != null) {
							OnlineStoreBook book = item.getOnlineStoreBookInfo();
							onlineBookInfo = "";
							if (book.rating > 0)
								onlineBookInfo = onlineBookInfo + "rating:" + book.rating + "  ";
							if (book.price > 0)
								onlineBookInfo = onlineBookInfo + "price: " + book.price + "  ";
							
						}
						if (item.isLitresBook()) {
							if (item.free == 1) {
								onlineBookInfo = onlineBookInfo + mActivity.getString(R.string.online_store_status_free)+" ";
							} else
								if (item.finalPrice > 0)
									onlineBookInfo = onlineBookInfo + mActivity.getString(R.string.online_store_book_price) +
											": " + String.format("%.2f", item.finalPrice) + " " + LitresConfig.currency + " ";
							if (item.lvl > 0)
								onlineBookInfo = onlineBookInfo + mActivity.getString(R.string.online_store_book_rating) +
										": " + item.lvl + " ";
						}
						if (item.isLitresPerson()) {
							if (item.lvl > 0)
								onlineBookInfo = onlineBookInfo + mActivity.getString(R.string.online_store_book_rating) +
										": " + item.lvl;
							if (!StrUtils.isEmptyStr(item.top_genres))
								//onlineBookInfo = onlineBookInfo + "\n" + item.top_genres.replace("|", "; ");
								seriesName = "; " + item.top_genres.replace("|", "; ");
							if (!StrUtils.isEmptyStr(item.top_arts))
								onlineBookInfo = onlineBookInfo + "; " + item.top_arts.replace("|", "; ");
							if (onlineBookInfo.startsWith("; ")) onlineBookInfo = onlineBookInfo.substring(1);
						}
						if (!item.isCloudBook()) {
							boolean wasFN1 = false;
							boolean wasFN2 = false;
							if (title == null || title.length() == 0) {
								title = filename1;
								wasFN1 = true;
								if (seriesName == null) {
									seriesName = "(" + filename2 + ")";
									wasFN2 = true;
								}
							} else if (seriesName == null) {
								seriesName = "(" + filename1 + ")";
								wasFN1 = true;
							}
							if ((fileBrowserModeSelected == 1) || (fileBrowserModeSelected == 2))
							  if ((!wasFN1) && (!StrUtils.isEmptyStr(filename1)))
							  	seriesName = (StrUtils.getNonEmptyStr(seriesName, true) + "; (" + filename1 + ")").trim();
							if ((fileBrowserModeSelected == 2) || (fileBrowserModeSelected == 3))
							  if ((!wasFN2) && (!StrUtils.isEmptyStr(filename2)))
								seriesName = (StrUtils.getNonEmptyStr(seriesName, true) + " [" + filename2 + "]").trim();
							if (seriesName.startsWith(";"))
								seriesName = seriesName.substring(1).trim();
						}
						String sLangFrom = item.lang_from;
						String sLangTo = item.lang_to;
						String sLang = "";
						if ((!StrUtils.isEmptyStr(sLangFrom))||(!StrUtils.isEmptyStr(sLangTo))) {
							if (StrUtils.isEmptyStr(sLangFrom)) sLangFrom = "any";
							if (StrUtils.isEmptyStr(sLangTo)) sLangTo = "any";
							sLang = "[" +sLangFrom+" > "+sLangTo + "] ";
						}
						if (sLang.equals(""))
							setText(name, title, 0);
						else setText(name, title + "; "+sLang, 0);
						setText(series, seriesName, colorIcon);

//						field1.setVisibility(VISIBLE);
//						field2.setVisibility(VISIBLE);
//						field3.setVisibility(VISIBLE);
						String state = Utils.formatReadingState(mActivity, item);
						String pos = " " + Utils.formatLastPositionShort(mActivity, mHistory.getLastPos(item));
						if (field1 != null) {
							if (fieldState == null)	{
								field1.setText(onlineBookInfo + "  " + state + pos + " " + Utils.formatFileInfo(mActivity, item));
								field1.setTextColor(mActivity.getTextColor(colorIcon));
							} else {
								field1.setText(onlineBookInfo  + pos + " " + Utils.formatFileInfo(mActivity, item));
								field1.setTextColor(mActivity.getTextColor(colorIcon));
							}
						}
						if (fieldState != null) {
							if (field1 == null) {
								fieldState.setText(state + pos);
							} else {
								fieldState.setText(state);
							}
							int colorBlue = themeColors.get(R.attr.colorThemeBlue);
							int colorGreen = themeColors.get(R.attr.colorThemeGreen);
							int colorGray = themeColors.get(R.attr.colorThemeGray);
							fieldState.setTag("notint");
							if (state.contains(mActivity.getString(R.string.book_state_reading))) {
								fieldState.setTextColor(colorGreen);
							}
							if (state.contains(mActivity.getString(R.string.book_state_toread))) {
								fieldState.setTextColor(colorBlue);
							}
							if (state.contains(mActivity.getString(R.string.book_state_finished))) {
								fieldState.setTextColor(colorGray);
							}
						}
						if ((field2 != null) && (field1 == null) && (fieldState == null)) {
							field2.setText(Utils.formatLastPosition(mActivity, mHistory.getLastPos(item)));
							field2.setTextColor(mActivity.getTextColor(colorIcon));
							field2.setBackgroundColor(colorGrayCT);
						}
						//field2.setText(formatDate(pos!=null ? pos.getTimeStamp() : item.createTime));
						TextView fld = linkToFile;
						if (fld == null) fld = field2;
						if (fld != null) fld.setText("");
						if (fld != null) {
							if ((!StrUtils.isEmptyStr(item.pathnameR)) || (!StrUtils.isEmptyStr(item.arcnameR)))
							{
								String s = "";
								if (!StrUtils.isEmptyStr(item.arcnameR)) {
									if ((item.arcnameR.contains(item.pathnameR)) ||
											(item.arcnameR.contains(StrUtils.stripExtension(item.pathnameR))))
										s = item.arcnameR;
									else
										s = item.arcnameR + "@" + item.pathnameR;
								} else s = item.pathnameR;
								s = s.replace("/storage/emulated/0", "/s/e/0");
								if (s.contains("/fragments/"))
									s = mActivity.getString(R.string.fragment) + ": " + s;
								final String fS = s;
								fld.setText(fS);
								//log.i("Setting linked file for: " + item.pathname + " to " +
								//		item.pathnameR);
								fld.setTextColor(mActivity.getTextColor(colorIcon));
								fld.setBackgroundColor(colorGrayCT2);
							}
						}
						if (llTags != null) {
							llTags.removeAllViews();
							if (!StrUtils.isEmptyStr(item.tag_list)) {
								for (String s: StrUtils.getNonEmptyStr(item.tag_list, true).split("\\|")) {
									TextView tagButton = new TextView(mActivity);
									tagButton.setText(s);
									tagButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
									tagButton.setTextColor(mActivity.getTextColor(colorIcon));
									tagButton.setBackgroundColor(CoverpageManager.randomColor(s.hashCode()));
									tagButton.setPadding(5, 5, 5, 5);
									LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
											ViewGroup.LayoutParams.WRAP_CONTENT,
											ViewGroup.LayoutParams.WRAP_CONTENT);
									llp.setMargins(4, 2, 8, 4);
									tagButton.setLayoutParams(llp);
									tagButton.setMaxLines(1);
									tagButton.setEllipsize(TextUtils.TruncateAt.END);
									tagButton.setClickable(false);
									tagButton.setFocusable(false);
									tagButton.setFocusableInTouchMode(false);
									llTags.addView(tagButton);
								}
							}
						}
//						CR infoButton - we dont use it, KR have its own UI here
//						if (infoButton != null)
//							infoButton.setOnClickListener(v -> mActivity.editBookInfo(Services.getScanner().createRecentRoot(), item));
 					}
					
				}
				// This is not a decision - view always reused and removing view affects future rendering
//				if ((linearLayoutF1and2!=null)&&(field2!=null)) {
//					if (StrUtils.isEmptyStr(field2.getText().toString())) {
//						//linearLayoutF1and2.removeView(field2);
//						field2.setTextSize(1);
//					} else field2.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
//				}
			}
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			if (currDirectoryFiltered == null)
				return null;
			View view;
			ViewHolder holder;
			int vt = getItemViewType(position);
			if (convertView == null) {
				if (vt == VIEW_TYPE_LEVEL_UP)
					view = mInflater.inflate(R.layout.browser_item_parent_dir, null);
				else if (vt == VIEW_TYPE_DIRECTORY)
					view = mInflater.inflate(R.layout.browser_item_folder, null);
				else if (vt == VIEW_TYPE_DIRECTORY_LITRES)
					view = mInflater.inflate(R.layout.browser_item_folder_litres, null);
				else if (vt == VIEW_TYPE_DIRECTORY_LITRES_1BTN)
					view = mInflater.inflate(R.layout.browser_item_folder_litres_1btn, null);
				else if (vt == VIEW_TYPE_DIRECTORY_LITRES_2BTN)
					view = mInflater.inflate(R.layout.browser_item_folder_litres_2btn, null);
				else if (vt == VIEW_TYPE_FILE_SIMPLE)
					view = mInflater.inflate(R.layout.browser_item_book_simple, null);
				else if (vt == VIEW_TYPE_OPDS_BOOK)
					view = mInflater.inflate(R.layout.browser_item_opds_book, null);
				else if (vt == VIEW_TYPE_STUB)
					view = mInflater.inflate(R.layout.browser_item_stub, null);
				else
					view = mInflater.inflate(R.layout.browser_item_book, null);
				holder = new ViewHolder();
				holder.image = view.findViewById(R.id.book_icon);
				holder.name = view.findViewById(R.id.book_name);
				holder.author = view.findViewById(R.id.book_author);
				holder.series = view.findViewById(R.id.book_series);
				holder.filename = view.findViewById(R.id.book_filename);
				holder.field1 = view.findViewById(R.id.browser_item_field1);
				holder.field2 = view.findViewById(R.id.browser_item_field2);
				holder.fbody = view.findViewById(R.id.browser_item_body);
				//holder.field2.setTextSize(2);
				holder.linearLayoutF1and2 = view.findViewById(R.id.browser_item_fields1_and_2);
				holder.linkToFile = view.findViewById(R.id.linkToFile);
				holder.fieldState = view.findViewById(R.id.browser_item_field_state);
				holder.llTags = view.findViewById(R.id.ll_tags);
				holder.imageAddInfo = view.findViewById(R.id.btn_option_add_info);
				holder.browserItemBtnsBody = view.findViewById(R.id.browser_item_btns_body);
				holder.imageAddMenu = view.findViewById(R.id.btn_add_menu);
				holder.imageFavFolder = view.findViewById(R.id.btn_fav_folder);
				holder.btn1 = view.findViewById(R.id.btn_1);
				holder.btn2 = view.findViewById(R.id.btn_2);
				//holder.field3 = (TextView)view.findViewById(R.id.browser_item_field3);
				//CR infoButton - we dont use it, KR have its own UI here
				//holder.infoButton = view.findViewById(R.id.browser_item_button_info);
				view.setTag(holder);
			} else {
				view = convertView;
				holder = (ViewHolder)view.getTag();
			}
			holder.viewType = vt;
			FileInfo item = (FileInfo)getItem(position);
			FileInfo parentItem = null;//item!=null ? item.parent : null;
			if (vt == VIEW_TYPE_LEVEL_UP) {
				item = null;
				parentItem = currDirectoryFiltered;
			}
			if (vt == VIEW_TYPE_STUB) {
				item = null;
				parentItem = currDirectoryFiltered;
			}
			holder.setItem(item, parentItem);
//			if ( DeviceInfo.FORCE_HC_THEME ) {
//				view.setBackgroundColor(Color.WHITE);
//			}
			return view;
		}

		public int getViewTypeCount() {
			if (currDirectoryFiltered == null)
				return 1;
			return VIEW_TYPE_COUNT;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			if (currDirectoryFiltered == null)
				return true;
			return mScanner.mFileList.size()==0;
		}

	}

	public void doBookAddInfoClick(FileInfo item) {
		if (item.isCloudBook() || item.isLitresSpecialDir()) {
			mActivity.showCloudItemInfo(item, FileBrowser.this, currDirectory);
		} else {
			BookInfo bi = new BookInfo(item);
			mActivity.showBookInfo(bi, BookInfoDialog.BOOK_INFO, currDirectoryFiltered, null);
		}
	}

	private String translateOnlineStorePluginItem(FileInfo item) {
		String path = item.getOnlineCatalogPluginPath();
		int resourceId = 0;
		if ("genres".equals(path))
			resourceId = R.string.online_store_genres;
		else if ("authors".equals(path))
			resourceId = R.string.online_store_authors;
		else if ("my".equals(path))
			resourceId = R.string.online_store_my;
		else if ("popular".equals(path))
			resourceId = R.string.online_store_popular;
		else if ("new".equals(path))
			resourceId = R.string.online_store_new;
		if (resourceId != 0)
			return mActivity.getString(resourceId);
		return item.getFilename();
	}
	
	private void setCurrDirectory(FileInfo newCurrDirectory) {
		if (currDirectory != null && currDirectory != newCurrDirectory) {
			ArrayList<CoverpageManager.ImageItem> filesToUqueue = new ArrayList<CoverpageManager.ImageItem>();
			for (int i=0; i<currDirectory.fileCount(); i++)
				filesToUqueue.add(new CoverpageManager.ImageItem(currDirectory.getFile(i), -1, -1));
			mCoverpageManager.unqueue(filesToUqueue);
		}
		currDirectory = newCurrDirectory;
		currDirectoryFiltered = newCurrDirectory;
	}

	private void showDirectoryLoadingStub() {
		BackgroundThread.ensureGUI();
		BackgroundThread.instance().executeGUI(() -> {
			FileInfo dir = new FileInfo();
			dir.id = 0L;
			dir.isDirectory = true;
			dir.isListed = true;
			dir.isScanned = true;
			dir.pathname = FileInfo.LOADING_STUB_PREFIX;
			dir.setFilename(mActivity.getString(R.string.loading_stub));
			dir.title = mActivity.getString(R.string.loading_stub);
			FileInfo file = new FileInfo();
			file.isDirectory = true;
			file.isListed = true;
			file.isScanned = true;
			file.id = 1L;
			file.setAuthors("");
			file.setSeriesName("");
			file.pathname = FileInfo.LOADING_STUB_PREFIX;
			file.setFilename(mActivity.getString(R.string.loading_stub));
			file.title = mActivity.getString(R.string.loading_stub);
			file.parent = dir;
			file.format = DocumentFormat.NONE;
			dir.addFile(file);
			showDirectoryInternal(dir, null);
		});
	}

	private void showDirectoryNotFoundStub() {
		BackgroundThread.ensureGUI();
		BackgroundThread.instance().executeGUI(() -> {
			FileInfo dir = new FileInfo();
			dir.id = 0L;
			dir.isDirectory = true;
			dir.isListed = true;
			dir.isScanned = true;
			dir.pathname = FileInfo.NOT_FOUND_STUB_PREFIX;
			dir.setFilename(mActivity.getString(R.string.search_not_found));
			dir.title = mActivity.getString(R.string.search_not_found);
			FileInfo file = new FileInfo();
			file.isDirectory = true;
			file.isListed = true;
			file.isScanned = true;
			file.setAuthors("");
			file.setSeriesName("");
			file.id = 1L;
			file.pathname = FileInfo.NOT_FOUND_STUB_PREFIX;
			file.setFilename(mActivity.getString(R.string.search_not_found));
			file.title = mActivity.getString(R.string.search_not_found);
			file.parent = dir;
			file.format = DocumentFormat.NONE;
			dir.addFile(file);
			showDirectoryInternal(dir, null);
		});
	}

	private void showDirectoryInternal(final FileInfo dir, final FileInfo file)
	{
		BackgroundThread.ensureGUI();

		if (dir!=null && dir.isOPDSRoot()) {
			boolean bOnlineCatalogsSortAZ = mActivity.settings().getBool(Settings.PROP_APP_ROOT_VIEW_OPDS_SECTION_SORT_AZ, false);
			if (bOnlineCatalogsSortAZ) {
				Comparator<FileInfo> compareByName = (o1, o2) -> o1.getFilename().compareToIgnoreCase(o2.getFilename());
				Collections.sort(dir.dirs, compareByName);
			}
		}

		if (dir!=null && dir != currDirectory) {
			log.i("Showing directory " + dir + " " + Thread.currentThread().getName());
			if (dir.isRecentDir())
				mActivity.setLastLocation(dir.getPathName());
			else if (!dir.isSpecialDir())
				mActivity.setLastDirectory(dir.getPathName());
		}

		setCurrDirectory(dir);

		if (!BackgroundThread.isGUIThread())
			throw new IllegalStateException("showDirectoryInternal should be called from GUI thread!");
		int index = dir!=null ? dir.getItemIndex(file) : -1;

		String title = "";
		if (dir != null) {
			title = dir.getFilename();
			if (!dir.isSpecialDir())
				title = dir.getPathName();
			if (dir.isOnlineCatalogPluginDir())
				title = translateOnlineStorePluginItem(dir);
		}
		if (dir != null) {
			if (dir.isLitresPrefix()) {
				if ((title.startsWith("@")) && (dir.isBooksByLitresGenreRoot()))
					title = mActivity.getString(R.string.search_genres);
				if ((title.startsWith("@")) && (dir.isBooksByLitresBooksRoot()))
					title = mActivity.getString(R.string.search_books);
				if ((title.startsWith("@")) && (dir.isLitresDir()))
					title = mActivity.getString(R.string.litres_main);
				if ((title.startsWith("@")) && (dir.isBooksByLitresSequenceRoot()))
					title = mActivity.getString(R.string.search_sequences);
				if ((title.startsWith("@")) && (dir.isBooksByLitresCollectionRoot()))
					title = mActivity.getString(R.string.search_collections);
				if ((title.startsWith("@")) && (dir.isBooksByLitresPersonRoot()))
					title = mActivity.getString(R.string.search_persons);
			}

			if (dir.isCalibrePrefix()) {
				title = FileInfo.getDisplayName(mActivity, title);
			}
		}
		filterUpdated("");
		mActivity.setBrowserTitle(title, dir);
		mListView.setAdapter(currentListAdapter);
		currentListAdapter.notifyDataSetChanged();
		if (index >= 0)
			mListView.setSelection(index);
		else
			mListView.setSelection(0);
		mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		mListView.invalidate();
	}

	private class MyGestureListener extends SimpleOnGestureListener {

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			if (e1 == null || e2 == null)
				return false;
			int thresholdDistance = mActivity.getPalmTipPixels() * 2;
			int thresholdVelocity = mActivity.getPalmTipPixels();
			int x1 = (int)e1.getX();
			int x2 = (int)e2.getX();
			int y1 = (int)e1.getY();
			int y2 = (int)e2.getY();
			int dist = x2 - x1;
			int adist = dist > 0 ? dist : -dist;
			int ydist = y2 - y1;
			int aydist = ydist > 0 ? ydist : -ydist;
			int vel = (int)velocityX;
			if (vel<0)
				vel = -vel;
			if (vel > thresholdVelocity && adist > thresholdDistance && adist > aydist * 2) {
				if (dist > 0) {
					log.d("LTR fling detected: moving to parent");
					showParentDirectory();
					return true;
				} else {
					log.d("RTL fling detected: show menu");
					mActivity.openOptionsMenu();
					return true;
				}
			}
			return false;
		}
		
	}
	
    private abstract class Task implements Engine.EngineTask {
    	
		public void done() {
			// override to do something useful
		}

		public void fail(Exception e) {
			// do nothing, just log exception
			// override to do custom action
			log.e("Task " + this.getClass().getSimpleName() + " is failed with exception " + e.getMessage(), e);
		}
    }

    public FileInfo getCurrentDir() {
    	return currDirectory;
    }

    private boolean coverPagesEnabled = true;
    private int coverPageHeight = 120;
    private int coverPageWidth = 90;
    private int coverPageSizeOption = 1; // 0==small, 2==BIG
    private int screenWidth = 480;
    private int screenHeight = 320;
    private void setCoverSizes(int screenWidth, int screenHeight) {
    	this.screenWidth = screenWidth;
    	this.screenHeight = screenHeight;
    	int minScreenSize = Math.min(screenWidth, screenHeight);
		int minh = 80;
    	int maxh = minScreenSize / 3;
    	int avgh = (minh + maxh) / 2;  
    	int h = avgh; // medium
    	if (coverPageSizeOption == 2)
    		h = maxh; // big
    	else if (coverPageSizeOption == 0)
    		h = minh; // small
    	int w = h * 3 / 4;
    	if (coverPageHeight != h) {
	    	coverPageHeight = h;
	    	coverPageWidth = w;
	    	if (mCoverpageManager.setCoverpageSize(coverPageWidth, coverPageHeight))
	    		currentListAdapter.notifyDataSetChanged();
    	}
    }

    @Override
	protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
    	setCoverSizes(w, h);
	}
    
    public void setCoverPageSizeOption(int coverPageSizeOption) {
    	if (this.coverPageSizeOption == coverPageSizeOption)
    		return;
    	this.coverPageSizeOption = coverPageSizeOption;
    	setCoverSizes(screenWidth, screenHeight);
    }

    public void setCoverPageFontFace(String face) {
    	if (mCoverpageManager.setFontFace(face))
    		currentListAdapter.notifyDataSetChanged();
    }

	public void setCoverPagesEnabled(boolean coverPagesEnabled)
	{
		this.coverPagesEnabled = coverPagesEnabled;
		if (!coverPagesEnabled) {
			//mCoverpageManager.clear();
		}
		currentListAdapter.notifyDataSetChanged();
	}

	public void setCoverpageData(FileInfo fileInfo, byte[] data) {
		mCoverpageManager.setCoverpageData(mActivity.getDB(), fileInfo, data);
		currentListAdapter.notifyInvalidated();
	}
	
	protected void showOnlineCatalogBookDialog(final FileInfo book) {
		OnlineStoreWrapper plugin = getPlugin(book);
		if (plugin == null) {
			mActivity.showToast("cannot find plugin");
			return;
		}
		String bookId = book.getOnlineCatalogPluginId();
		progress.show();
		plugin.loadBookInfo(bookId, new BookInfoCallback() {
			@Override
			public void onError(int errorCode, String errorMessage) {
				progress.hide();
				mActivity.showToast("Error while loading book info");
			}
			
			@Override
			public void onBookInfoReady(OnlineStoreBookInfo bookInfo) {
				progress.hide();
				OnlineStoreBookInfoDialog dlg = new OnlineStoreBookInfoDialog(mActivity, bookInfo, book);
				dlg.show();
			}
		});
	}

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
		mActivity.onUserActivity();
       if( this.mListView != null ) {
            if (this.mListView.onKeyDown(keyCode, event))
            	return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}

