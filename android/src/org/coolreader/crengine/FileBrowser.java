package org.coolreader.crengine;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.*;

import com.google.android.gms.common.util.IOUtils;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.CoverpageManager.CoverpageReadyListener;
import org.coolreader.crengine.OPDSUtil.DocInfo;
import org.coolreader.crengine.OPDSUtil.DownloadCallback;
import org.coolreader.crengine.OPDSUtil.EntryInfo;
import org.coolreader.db.CRDBService;
import org.coolreader.plugins.*;
import org.koekak.android.ebookdownloader.SonyBookSelector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FileBrowser extends LinearLayout implements FileInfoChangeListener {

	public static final Logger log = L.create("fb");
	
	Engine mEngine;
	Scanner mScanner;
	CoolReader mActivity;
	LayoutInflater mInflater;
	History mHistory;
	ListView mListView;
	ArrayList<FileInfo> mFileSystemFolders;
	ArrayList<FileInfo> mfavFolders;
	ArrayList<String> mOnlyFSFolders;
	HashMap<Long, String> mFileSystemFoldersH;

	public static final int MAX_SUBDIR_LEN = 32;

	public void showItemPopupMenu() {
		mActivity.registerForContextMenu(mActivity.contentView);
		mActivity.contentView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v,
											ContextMenu.ContextMenuInfo menuInfo) {
				mListView.createContextMenu(menu);
			}
		});
		mActivity.contentView.showContextMenu();
	}

	private boolean contains(ArrayList<FileInfo> fiA, String s) {
		for (FileInfo fi: fiA) {
			if (fi.pathname.contains(s)) return true;
		}
		return false;
	}

	private boolean containsEq(ArrayList<FileInfo> fiA, String s) {
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
	        
	        setOnItemLongClickListener(new OnItemLongClickListener() {

				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
						int position, long id) {
					log.d("onItemLongClick("+position+")");
					//return super.performItemClick(view, position, id);
					FileInfo item = (FileInfo) getAdapter().getItem(position);
					//mActivity.showToast(item.filename);
					if ( item==null )
						return false;
					//openContextMenu(_this);
					//mActivity.loadDocument(item);
					selectedItem = item;
					
					boolean bookInfoDialogEnabled = true; // TODO: it's for debug
					if (!item.isDirectory && !item.isOPDSBook() && bookInfoDialogEnabled && !item.isOnlineCatalogPluginDir()) {
						mActivity.editBookInfo(currDirectory, item);
						return true;
					}
					
					showContextMenu();
					return true;
				}
			});
			setChoiceMode(CHOICE_MODE_SINGLE);
		}
		
		@Override
		public void createContextMenu(ContextMenu menu) {
			log.d("createContextMenu()");
			menu.clear();
		    MenuInflater inflater = mActivity.getMenuInflater();
		    if ( isRecentDir() ) {
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
		    	menu.getItem(i).setOnMenuItemClickListener(new OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem item) {
						onContextItemSelected(item);
						return true;
					}
				});
		    }
		}


		@Override
		public boolean performItemClick(View view, int position, long id) {
			log.d("performItemClick("+position+")");
			//return super.performItemClick(view, position, id);
			FileInfo item = (FileInfo) getAdapter().getItem(position);
			if ( item==null )
				return false;
			if ( item.isDirectory ) {
				if (item.isOPDSSearchDir()) {
					showFindBookDialog(false, "", item);
				} else showDirectory(item, null);
				return true;
			}
			final FileInfo finalItem = item;
			if (item.isOPDSDir() || item.isOPDSBook()) {
				if (item.isOPDSBook()) {
					boolean bOpenExisting = false;
					if	(
							(
								(!StrUtils.isEmptyStr(item.pathnameR))||
								(!StrUtils.isEmptyStr(item.arcnameR))
							) && (!StrUtils.isEmptyStr(item.opdsLinkR))
						)
						mActivity.askConfirmation(R.string.open_previously_downloaded, new Runnable() {
							@Override
							public void run() {
								Services.getHistory().getFileInfoByOPDSLink(mActivity.getDB(), finalItem.opdsLinkR,
										new History.FileInfo1LoadedCallack() {
											@Override
											public void onFileInfoLoaded(final FileInfo fileInfo) {
												if (fileInfo!=null) {
													mActivity.showBookInfo(new BookInfo(fileInfo),BookInfoDialog.BOOK_INFO, currDirectory);
												} else {
													mActivity.showToast(R.string.could_not_find_by_link);
													mActivity.showOPDSBookInfo(finalItem, FileBrowser.this, currDirectory);
												}
											}
										}
								);
							}
						}, new Runnable() {
							@Override
							public void run() {
								mActivity.showOPDSBookInfo(finalItem, FileBrowser.this, currDirectory);
							}
						}
					); else {
						mActivity.showOPDSBookInfo(finalItem, FileBrowser.this, currDirectory);
					}
				} else
					if (item.isOPDSSearchDir()) {
						showFindBookDialog(false, "", item);
					} else showOPDSDir(item, null, "");
			}
			else if (item.isOnlineCatalogPluginBook())
				showOnlineCatalogBookDialog(item);
			else {
				if (
						(item.format.name().equals("DOCX"))||
						(item.format.name().equals("ODT"))
				   ){
					final FileInfo downloadDir = Services.getScanner().getDownloadDirectory();
					File f = new File(downloadDir.pathname+"/converted/"+item.filename+".html");
					if (f.exists()) {
						FileInfo fi = new FileInfo(f);
						mActivity.showToast(mActivity.getString(R.string.docx_open_converted));
						mActivity.loadDocument(fi);
					} else {
						PackageManager pm = mActivity.getPackageManager();
						boolean binst = false;
						try {
							pm.getPackageInfo("org.coolreader.docx", 0); //PackageManager.GET_ACTIVITIES);
							binst = true;
						} catch (PackageManager.NameNotFoundException e) {
							binst = false;
						}
						if (!binst) {
							mActivity.showToast(mActivity.getString(R.string.docx_is_not_installed));
						} else {
							final FileInfo item1 = item;
							mActivity.askConfirmation(R.string.docx_convert, new Runnable() {
								@Override
								public void run() {
								Intent intent = new Intent(Intent.ACTION_SEND);
								intent.setPackage("org.coolreader.docx");
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								intent.setType("text/plain");
								final String fName = downloadDir.pathname + "/converted/";
								intent.putExtra(android.content.Intent.EXTRA_SUBJECT, fName);
								intent.putExtra(android.content.Intent.EXTRA_TEXT, item1.pathname);
								try {
									log.i("start docx intent");
									mActivity.startActivity(intent);
								} catch (ActivityNotFoundException e) {
									mActivity.showToast(mActivity.getString(R.string.docx_is_not_installed));
								} catch (Exception e) {
									mActivity.showToast("exception while working with org.coolreader.docx");
								}
								}
							});
						}
					}
				} else mActivity.loadDocument(item);
			}
			return true;
		}

		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
			//log.d("FileBrowser.ListView.onKeyDown(" + keyCode + ")");
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				if ( isRootDir() ) {
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
	public FileBrowser(CoolReader activity, Engine engine, Scanner scanner, History history) {
		super(activity);
		this.mActivity = activity;
		this.mEngine = engine;
		this.mScanner = scanner;
		this.mInflater = LayoutInflater.from(activity);// activity.getLayoutInflater();
		this.mHistory = history;
		this.mCoverpageManager = Services.getCoverpageManager();
		Services.getFileSystemFolders().loadFavoriteFolders(mActivity.getDB());
		ArrayList<FileInfo> favFolders = new ArrayList<FileInfo>();
		ArrayList<String> favFoldersS = new ArrayList<String>();
		this.mfavFolders = Services.getFileSystemFolders().getFavoriteFolders();
		if (this.mfavFolders!=null)
			for (FileInfo fi: this.mfavFolders) {
				favFoldersS.add(fi.pathname);
			}
		this.mFileSystemFolders = Services.getFileSystemFolders().getFileSystemFolders();
		this.mFileSystemFoldersH = new HashMap<Long, String>();
		this.mOnlyFSFolders = new ArrayList<String>();
		if (this.mFileSystemFolders!=null)
			for (FileInfo fi: this.mFileSystemFolders) {
				this.mFileSystemFoldersH.put(fi.id, fi.pathname);
				if (!favFoldersS.contains(fi.pathname)) this.mOnlyFSFolders.add(fi.pathname);
			}
		coverpageListener =	new CoverpageReadyListener() {
			@Override
			public void onCoverpagesReady(ArrayList<CoverpageManager.ImageItem> files) {
				if (currDirectory == null)
					return;
				boolean found = false;
				for (CoverpageManager.ImageItem file : files) {
					if (currDirectory.findItemByPathName(file.file.getPathName()) != null)
						found = true;
				}
				if (found) // && mListView.getS
					invalidateAdapter(currentListAdapter);
			}
		};
		this.mCoverpageManager.addCoverpageReadyListener(coverpageListener);
		super.onAttachedToWindow();
		
		setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		createListView(true);
		history.addListener(this);
		scanner.addListener(this);
		showDirectory( null, null );

	}
	
	public void onClose() {
		this.mCoverpageManager.removeCoverpageReadyListener(coverpageListener);
		coverpageListener = null;
		super.onDetachedFromWindow();
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
		mListView.setOnTouchListener(new ListView.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				try {
					return detector.onTouchEvent(event);
				} catch (Exception e) {
					L.e("Exception in onTouch", e);
					return false;
				}
			}
		});
		if (currentListAdapter == null || recreateAdapter) {
			currentListAdapter = new FileListAdapter();
			mListView.setAdapter(currentListAdapter);
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
		Services.getHistory().getOrCreateBookInfo(mActivity.getDB(), selectedItem, new History.BookInfoLoadedCallack() {
			@Override
			public void onBookInfoLoaded(BookInfo bookInfo) {
				BookInfo bi = new BookInfo(selectedItem);
				mActivity.getDB().saveBookInfo(bi);
				mActivity.getDB().flush();
				BookInfo bi2 = Services.getHistory().getBookInfo(selectedItem);
				if (bi2 != null)
					bi2.getFileInfo().setFileProperties(selectedItem);
				if ((selectedItem.parent) != null)
					mActivity.directoryUpdated(selectedItem.parent, selectedItem);
			}
		});
	}
	
	public boolean onContextItemSelected(MenuItem item) {
		
		if ( selectedItem==null )
			return false;
			
		switch (item.getItemId()) {
		case R.id.book_open:
			log.d("book_open menu item selected");
			if ( selectedItem.isOPDSDir() )
				showOPDSDir(selectedItem, null, "");
			else
				mActivity.loadDocument(selectedItem);
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
			mActivity.askDeleteBook(selectedItem);
			return true;
		case R.id.book_recent_goto:
			log.d("book_recent_goto menu item selected");
			showDirectory(selectedItem, selectedItem);
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
            showDirectory(selectedItem, null);
            return true;
		case R.id.folder_delete:
			log.d("folder_delete menu item selected");
			//mActivity.showToast(selectedItem.pathname);
			final File f = new File(selectedItem.pathname);
			String[] children = f.list();
			if (children.length>0)
				mActivity.askConfirmation( mActivity.getString(R.string.delete_dir_confirm)+" "+children.length, new Runnable() {
					@Override
					public void run() {
						mActivity.DeleteRecursive(f,selectedItem);
					}
				}); else mActivity.DeleteRecursive(f,selectedItem);
			return true;
		case R.id.folder_to_favorites:
            log.d("folder_to_favorites menu item selected");
            addToFavorites(selectedItem);
            return true;
		case R.id.book_info:
			log.d("book_info menu item selected");
			if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
				Services.getHistory().getOrCreateBookInfo(mActivity.getDB(), selectedItem, new History.BookInfoLoadedCallack() {
					@Override
					public void onBookInfoLoaded(BookInfo bookInfo) {
						BookInfo bi = new BookInfo(selectedItem);
						mActivity.showBookInfo(bi, BookInfoDialog.BOOK_INFO, null);
					}
				});
			}
			return true;
		case R.id.book_edit:
			log.d("book_edit menu item selected");
			if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
				mActivity.editBookInfo(currDirectory, selectedItem);
			}
			return true;
		case R.id.book_set_custom_cover:
			log.d("book_set_custom_cover menu item selected");
			if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
				if (mActivity.picReceived!=null) {
					if (mActivity.picReceived.bmpReceived!=null) {
						BookInfo bi = new BookInfo(selectedItem);
						PictureCameDialog dlg = new PictureCameDialog(mActivity,
								bi, "");
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
								bi, "");
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
				Services.getCoverpageManager().drawCoverpageFor(mActivity.getDB(), selectedItem, bmp, new CoverpageManager.CoverpageBitmapReadyListener() {
					@Override
					public void onCoverpageReady(CoverpageManager.ImageItem file, Bitmap bitmap) {
						mActivity.createBookShortcut(selectedItem,bitmap);
					}
				});
			}
			return true;
		case R.id.book_to_gd:
			log.d("book_to_gd menu item selected");
			if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
				mActivity.mGoogleDriveTools.signInAndDoAnAction(
						mActivity.mGoogleDriveTools.REQUEST_CODE_SAVE_CURRENT_BOOK_TO_GD, FileBrowser.this);
			}
			return true;
		case R.id.book_no_mark:
			log.d("book_no_mark menu item selected");
			if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
				int state = selectedItem.getReadingState();
				int newState = FileInfo.STATE_NEW;
				boolean modified = state != newState;
				selectedItem.setReadingState(FileInfo.STATE_NEW);
				if (modified) saveBookInfo();
			}
			return true;
		case R.id.book_to_read:
			log.d("book_to_read menu item selected");
			if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
				int state = selectedItem.getReadingState();
				int newState = FileInfo.STATE_TO_READ;
				boolean modified = state != newState;
				selectedItem.setReadingState(FileInfo.STATE_TO_READ);
				if (modified) saveBookInfo();
			}
			return true;
		case R.id.book_reading:
			log.d("book_reading menu item selected");
			if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
				int state = selectedItem.getReadingState();
				int newState = FileInfo.STATE_READING;
				boolean modified = state != newState;
				selectedItem.setReadingState(FileInfo.STATE_READING);
				if (modified) saveBookInfo();
			}
			return true;
		case R.id.book_finished:
			log.d("book_finished menu item selected");
			if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
				int state = selectedItem.getReadingState();
				int newState = FileInfo.STATE_FINISHED;
				boolean modified = state != newState;
				selectedItem.setReadingState(FileInfo.STATE_FINISHED);
				if (modified) saveBookInfo();
			}
			return true;
		case R.id.book_rate_5:
			log.d("book_rate_5 menu item selected");
			if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
				int rate = selectedItem.getRate();
				int newRate = 5;
				boolean modified = rate != newRate;
				selectedItem.setRate(newRate);
				if (modified) saveBookInfo();
			}
			return true;
		case R.id.book_rate_4:
			log.d("book_rate_4 menu item selected");
			if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
				int rate = selectedItem.getRate();
				int newRate = 4;
				boolean modified = rate != newRate;
				selectedItem.setRate(newRate);
				if (modified) saveBookInfo();
			}
			return true;
		case R.id.book_rate_3:
			log.d("book_rate_3 menu item selected");
			if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
				int rate = selectedItem.getRate();
				int newRate = 3;
				boolean modified = rate != newRate;
				selectedItem.setRate(newRate);
				if (modified) saveBookInfo();
			}
			return true;
		case R.id.book_rate_2:
			log.d("book_rate_2 menu item selected");
			if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
				int rate = selectedItem.getRate();
				int newRate = 2;
				boolean modified = rate != newRate;
				selectedItem.setRate(newRate);
				if (modified) saveBookInfo();
			}
			return true;
		case R.id.book_rate_1:
			log.d("book_rate_1 menu item selected");
			if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
				int rate = selectedItem.getRate();
				int newRate = 1;
				boolean modified = rate != newRate;
				selectedItem.setRate(newRate);
				if (modified) saveBookInfo();
			}
			return true;
		case R.id.book_rate_0:
			log.d("book_rate_0 menu item selected");
			if (!selectedItem.isDirectory && !selectedItem.isOPDSBook() && !selectedItem.isOnlineCatalogPluginDir()) {
				int rate = selectedItem.getRate();
				int newRate = 0;
				boolean modified = rate != newRate;
				selectedItem.setRate(newRate);
				if (modified) saveBookInfo();
			}
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
			mActivity.getDB().loadOPDSCatalogs(new CRDBService.OPDSCatalogsLoadingCallback() {
				@Override
				public void onOPDSCatalogsLoaded(ArrayList<FileInfo> catalogs) {
					opdsRoot.clear();
					for (FileInfo f : catalogs)
						opdsRoot.addDir(f);
					if (showInBrowser || (currDirectory!=null && currDirectory.isOPDSRoot()))
						showDirectory(opdsRoot, null);
				}
			});
		}
	}
	
	public void refreshDirectory(FileInfo dir, FileInfo selected) {
		if (dir.isSpecialDir()) {
			if (dir.isOPDSRoot())
				refreshOPDSRootDirectory(false);
		} else {
			if (dir.pathNameEquals(currDirectory))
				showDirectory(currDirectory, selected);
		}
	}

	public void showParentDirectory()
	{
		if (currDirectory == null || currDirectory.parent == null || currDirectory.parent.isRootDir())
			mActivity.showRootWindow();
		else
			showDirectory(currDirectory.parent, currDirectory);
	}
	
	boolean mInitStarted = false;
	public void init()
	{
		if ( mInitStarted )
			return;
		log.e("FileBrowser.init() called");
		mInitStarted = true;
		
		//showDirectory( mScanner.mRoot, null );
		mListView.setSelection(0);
	}
	
	private FileInfo currDirectory;

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
		showDirectory(mScanner.getRecentDir(), null);
	}

	public boolean isBookShownInRecentList(FileInfo book) {
		if (currDirectory==null || !currDirectory.isRecentDir())
			return false;
		return currDirectory.findItemByPathName(book.getPathName())!=null;
	}
	
	public void showLastDirectory()
	{
		if ( currDirectory==null || currDirectory==mScanner.getRoot() )
			showRecentBooks();
		else
			showDirectory(currDirectory, null);
	}

	public void showSearchResult( FileInfo[] books ) {
		FileInfo dir = mScanner.setSearchResults( books );
		showDirectory(dir, null);
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
			BookOPDSSearchDialog dlg = new BookOPDSSearchDialog(mActivity, sText, fi2, new BookSearchDialog.SearchCallback() {
				@Override
				public void done(FileInfo[] results) {
					if (results != null) {
						FileInfo fi3 = results[0];
						fi3.pathname = fi3.pathname.replace(FileInfo.OPDS_DIR_PREFIX+"search:",FileInfo.OPDS_DIR_PREFIX);
						showDirectory(fi3,null);
					}
				}
			});
			dlg.show();
		} else {
			BookSearchDialog dlg = new BookSearchDialog(mActivity, new BookSearchDialog.SearchCallback() {
				@Override
				public void done(FileInfo[] results) {
					if (results != null) {
						if (results.length == 0) {
							mActivity.showToast(R.string.dlg_book_search_not_found);
						} else {
							showSearchResult(results);
						}
					} else {
						if (currDirectory == null || currDirectory.isRootDir())
							mActivity.showRootWindow();
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
		showDirectory(mScanner.getRoot(), null);
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
		OnlineStoreLoginDialog dlg = new OnlineStoreLoginDialog(mActivity, plugin, new Runnable() {
			@Override
			public void run() {
				openPluginDirectory(plugin, dir);
			}
		});
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
			mActivity.getDB().loadOPDSCatalogs(new CRDBService.OPDSCatalogsLoadingCallback() {
				@Override
				public void onOPDSCatalogsLoaded(ArrayList<FileInfo> catalogs) {
					opdsRoot.setItems(catalogs);
					showDirectoryInternal(opdsRoot, null);
				}
			});
		}
	}

	private FileInfo.SortOrder mSortOrder = FileInfo.DEF_SORT_ORDER; 
	public void setSortOrder(FileInfo.SortOrder order) {
		if ( mSortOrder == order )
			return;
		mSortOrder = order!=null ? order : FileInfo.DEF_SORT_ORDER;
		if (currDirectory != null && currDirectory.allowSorting()) {
			currDirectory.sort(mSortOrder);
			showDirectory(currDirectory, selectedItem);
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
		};
		OptionsDialog.ListOption dlg = new OptionsDialog.ListOption(
			new OptionOwner() {
				public BaseActivity getActivity() { return mActivity; }
				public Properties getProperties() { return properties; }
				public LayoutInflater getInflater() { return mInflater; }
			}, 
			mActivity.getString(R.string.mi_book_sort_order), 
			ReaderView.PROP_APP_BOOK_SORT_ORDER,
				mActivity.getString(R.string.option_add_info_empty_text), "").add(optionValues, optionLabels, optionAddInfos);
		dlg.setOnChangeHandler(new Runnable() {
			public void run() {
				final String newValue = properties.getProperty(ReaderView.PROP_APP_BOOK_SORT_ORDER);
				if ( newValue!=null && oldValue!=null && !newValue.equals(oldValue) ) {
					log.d("New sort order: " + newValue);
					setSortOrder(newValue);
				}
			}
		});
		dlg.onSelect();
	}

	private static boolean isArchive(File f) {
		int fileSignature = 0;
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(f, "r");
			fileSignature = raf.readInt();
		} catch (IOException e) {
			// handle if you like
		} finally {
			IOUtils.closeQuietly(raf);
		}
		return fileSignature == 0x504B0304 || fileSignature == 0x504B0506 || fileSignature == 0x504B0708;
	}
	
	public void showOPDSDir( final FileInfo fileOrDir, final FileInfo itemToSelect, final String annot ) {
		
		if ( fileOrDir.fileCount()>0 || fileOrDir.dirCount()>0 ) {
			// already downloaded
			BackgroundThread.instance().executeGUI(new Runnable() {
				@Override
				public void run() {
					showDirectoryInternal(fileOrDir, itemToSelect);					
				}
			});
			return;
		}
		
		if (currDirectory != null && !currDirectory.isOPDSRoot() && !currDirectory.isOPDSBook() && !currDirectory.isOPDSDir()) {
			// show empty directory before trying to download catalog
			showDirectoryInternal(fileOrDir, itemToSelect);
			// update last usage
			mActivity.getDB().updateOPDSCatalogLastUsage(fileOrDir.getOPDSUrl());
			mActivity.refreshOPDSRootDirectory(false);
		}
		
		String url = fileOrDir.getOPDSUrl();
		final FileInfo myCurrDirectory = currDirectory;
		if ( url!=null ) {
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
						ArrayList<FileInfo> items = new ArrayList<FileInfo>();
						if ((doc.searchLink != null) && (fileOrDir.parent == null)) {
							FileInfo file = new FileInfo();
							file.isDirectory = true;
							file.pathname = FileInfo.OPDS_DIR_PREFIX + "search:"+ doc.searchLink.href;
							file.title = doc.searchLink.href;
							file.filename =  mActivity.getString(R.string.opds_search);
							file.isListed = true;
							file.isScanned = true;
							file.tag = doc.searchLink;
							file.parent = fileOrDir;
							items.add(file);
						}
						ArrayList<FileInfo> itemsF = new ArrayList<FileInfo>();
						for (FileInfo fi: mfavFolders) {
							if ((fi.isOPDSDir())&&(fileOrDir.parent == null)) {
								FileInfo fi2 = new FileInfo(fi);
								fi2.parent = fileOrDir;
								fi2.isFav = true;
								if (fi2.pathname.startsWith(fileOrDir.pathname)) itemsF.add(fi2);
							}
						}
						ArrayList<FileInfo> itemsE = new ArrayList<FileInfo>();
						for ( EntryInfo entry : entries ) {
							OPDSUtil.LinkInfo acquisition = entry.getBestAcquisitionLink();
							if ( acquisition!=null ) {
								final FileInfo file = new FileInfo();
								file.isDirectory = false;
								file.pathname = FileInfo.OPDS_DIR_PREFIX + acquisition.href;
								file.filename = Utils.cleanupHtmlTags(entry.content);
								file.title = entry.title;
								file.format = DocumentFormat.byMimeType(acquisition.type);
								file.setAuthors(entry.getAuthors());
								file.isListed = true;
								file.isScanned = true;
								file.parent = fileOrDir;
								file.tag = entry;
								file.links = entry.links;
								itemsE.add(file);
								final String sLink = acquisition.href;
								Services.getHistory().getFileInfoByOPDSLink(mActivity.getDB(), acquisition.href,
										new History.FileInfo1LoadedCallack() {
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
							} else if ( entry.link.type!=null && entry.link.type.startsWith("application/atom+xml") ) {
								FileInfo file = new FileInfo();
								file.isDirectory = true;
								file.pathname = FileInfo.OPDS_DIR_PREFIX + entry.link.href;
								file.title = Utils.cleanupHtmlTags(entry.content);
								file.filename = entry.title;
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
						if ( items.size()>0 ) {
							fileOrDir.replaceItems(items);
							if (currDirectory == fileOrDir)
								currentListAdapter.notifyDataSetChanged();
							else
								showDirectoryInternal(fileOrDir, null);
						} else {
							if (notifyNoEntries)
								mActivity.showToast("No OPDS entries found");
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
					public File onDownloadStart(String type, String url) {
						//mEngine.showProgress(0, "Downloading " + url);
						//mActivity.showToast("Starting download of " + type + " from " + url);
						log.d("onDownloadStart: called for " + type + " " + url );
						downloadDir = Services.getScanner().getDownloadDirectory();
						log.d("onDownloadStart: after getDownloadDirectory()" );
						String subdir = null;
						if ( fileOrDir.getAuthors()!=null ) {
							subdir = Utils.transcribeFileName(fileOrDir.getAuthors());
							if ( subdir.length()>MAX_SUBDIR_LEN )
								subdir = subdir.substring(0, MAX_SUBDIR_LEN);
						} else {
							subdir = "NoAuthor";
						}
						if ( downloadDir==null )
							return null;
						File result = new File(downloadDir.getPathName());
						result = new File(result, subdir);
						result.mkdirs();
						downloadDir.findItemByPathName(result.getAbsolutePath());
						log.d("onDownloadStart: returning " + result.getAbsolutePath() );
						return result;
					}

					@Override
					public void onDownloadEnd(String type, String url, File file) {
                        if (DeviceInfo.EINK_SONY) {
                            SonyBookSelector selector = new SonyBookSelector(mActivity);
                            selector.notifyScanner(file.getAbsolutePath());
                        }
						mEngine.hideProgress();
						//mActivity.showToast("Download is finished");
						//fileOrDir.parent.pathname // @opds:http://89.179.127.112/opds/search/books/u/0/
						FileInfo fi = new FileInfo(file);
						boolean isArch = isArchive(file);
						FileInfo dir = mScanner.findParent(fi, downloadDir);
						if ( dir==null )
							dir = downloadDir;
						String sPath = file.getAbsolutePath();
						String sPathZ = sPath;
						File fileZ = new File(sPath);
						if (isArch) {
							int i = 0;
							sPathZ = sPath + ".zip";
							fileZ = new File(sPathZ);
							boolean bExists = fileZ.exists();
							while (bExists) {
								i++;
								sPathZ = sPath + " ("+i+").zip";
								fileZ = new File(sPathZ);
								bExists = fileZ.exists();
							}
							file.renameTo(fileZ);
						}
						mScanner.listDirectory(dir);
						final FileInfo item = dir.findItemByPathName(sPathZ);
						BackgroundThread.ensureGUI();
						item.opdsLink = url;
						fileOrDir.pathnameR = item.pathname;
						fileOrDir.arcnameR = item.arcname;
						fileOrDir.pathR = item.path;
						mActivity.getDB().saveBookInfo(new BookInfo(fileOrDir));
						mActivity.getDB().flush();
						if (item.getTitle() == null) {
							if (!mEngine.scanBookProperties(item))
								mActivity.showToast("Could not read file properties, possibly format is unsupported");
							if (item.getTitle() != null) {
								mActivity.getDB().saveBookInfo(new BookInfo(item));
								mActivity.getDB().flush();
							}
						}
						if ( item!=null ) {
							Services.getHistory().getOrCreateBookInfo(mActivity.getDB(), item,
								new History.BookInfoLoadedCallack() {
									@Override
									public void onBookInfoLoaded(final BookInfo bookInfo) {
										BookInfo bif2 = bookInfo;

										if (bif2 == null) bif2 = new BookInfo(item);
										final BookInfo bif = bif2;
										if (StrUtils.isEmptyStr(bif.getFileInfo().annotation))
											bif.getFileInfo().setAnnotation(annot);
										if (bif.getFileInfo().getTitle() != null) {
											if (mActivity.settings().getBool(Settings.PROP_APP_MARK_DOWNLOADED_TO_READ, false)) {
												bif.getFileInfo().setReadingState(FileInfo.STATE_TO_READ);
												mActivity.showToast(item.pathname+" "+
														mActivity.getString(R.string.mi_opds_mark_as_toread));
											}
											mActivity.getDB().saveBookInfo(bif);
											mActivity.getDB().flush();
										}
										FileInfo dir1 =bif.getFileInfo().parent;
										if (dir1 == null) {
											dir1 = mScanner.findParent(bif.getFileInfo(), downloadDir);
											bif.getFileInfo().parent = dir1;
										}
										if (dir1!=null) {
											final FileInfo dir2 = dir1;
											bif.getFileInfo().setFileProperties(bif.getFileInfo());
											dir2.setFile(bif.getFileInfo());
											mActivity.directoryUpdated(dir2, bif.getFileInfo());
										}
										mActivity.showBookInfo(bif, BookInfoDialog.OPDS_FINAL_INFO, currDirectory);
									}
								});
						}
						else
							mActivity.loadDocument(fi);
					}

					@Override
					public void onDownloadProgress(String type, String url,
							int percent) {
						mEngine.showProgress(percent * 100, "Downloading");
					}
					
				};
				String fileMimeType = fileOrDir.format!=null ? fileOrDir.format.getMimeFormat() : null;
				String defFileName = Utils.transcribeFileName( fileOrDir.title!=null ? fileOrDir.title : fileOrDir.filename );
				if ( fileOrDir.format!=null )
					defFileName = defFileName + fileOrDir.format.getExtensions()[0];
				final OPDSUtil.DownloadTask downloadTask = OPDSUtil.create(mActivity, uri, defFileName, fileOrDir.isDirectory?"application/atom+xml":fileMimeType, 
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
	
	private class ItemGroupsLoadingCallback implements CRDBService.ItemGroupsLoadingCallback {
		private final FileInfo baseDir;
		public ItemGroupsLoadingCallback(FileInfo baseDir) {
			this.baseDir = baseDir;
		}
		@Override
		public void onItemGroupsLoaded(FileInfo parent) {
			baseDir.setItems(parent);
			showDirectoryInternal(baseDir, null);
		}
	};
	
	private class FileInfoLoadingCallback implements CRDBService.FileInfoLoadingCallback {
		private final FileInfo baseDir;
		public FileInfoLoadingCallback(FileInfo baseDir) {
			this.baseDir = baseDir;
		}
		@Override
		public void onFileInfoListLoaded(ArrayList<FileInfo> list) {
			baseDir.setItems(list);
			showDirectoryInternal(baseDir, null);
		}
	};
	
	@Override
	public void onChange(FileInfo object, boolean filePropsOnly) {
		if (currDirectory == null)
			return;
		if (!currDirectory.pathNameEquals(object) && !currDirectory.hasItem(object))
			return;
		// refresh
		if (filePropsOnly)
			currentListAdapter.notifyInvalidated();
		else
			showDirectoryInternal(currDirectory, null);
	}

	public void showDirectory(FileInfo fileOrDir, FileInfo itemToSelect)
	{
		BackgroundThread.ensureGUI();
		if (fileOrDir != null) {
			if (fileOrDir.isRootDir()) {
				mActivity.showRootWindow();
				return;
			}
			if (fileOrDir.isOnlineCatalogPluginDir()) {
				if (fileOrDir.getOnlineCatalogPluginPath() == null) {
					// root
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
				showOnlineStoreDirectory(fileOrDir);
				return;
			}
			if (fileOrDir.isOPDSRoot()) {
				showOPDSRootDirectory();
				return;
			}
			if (fileOrDir.isOPDSDir()) {
				showOPDSDir(fileOrDir, itemToSelect, "");
				return;
			}
			if (fileOrDir.isSearchShortcut()) {
				showFindBookDialog(false, "", null);
				return;
			}
			if (fileOrDir.isQSearchShortcut()) {
				showFindBookDialog(true, fileOrDir.filename, null);
				return;
			}
			if (fileOrDir.isBooksByAuthorRoot()) {
				// refresh authors list
				log.d("Updating authors list");
				mActivity.getDB().loadAuthorsList(fileOrDir, new ItemGroupsLoadingCallback(fileOrDir));
				return;
			}
			if (fileOrDir.isBooksBySeriesRoot()) {
				// refresh authors list
				log.d("Updating series list");
				mActivity.getDB().loadSeriesList(fileOrDir, new ItemGroupsLoadingCallback(fileOrDir));
				return;
			}
            if (fileOrDir.isBooksByBookdateRoot()) {
                // refresh authors list
                log.d("Updating bookdate list");
                mActivity.getDB().loadByDateList(fileOrDir, "book_date_n", new ItemGroupsLoadingCallback(fileOrDir));
                return;
            }
			if (fileOrDir.isBooksByDocdateRoot()) {
				// refresh authors list
				log.d("Updating docdate list");
				mActivity.getDB().loadByDateList(fileOrDir, "doc_date_n", new ItemGroupsLoadingCallback(fileOrDir));
				return;
			}
			if (fileOrDir.isBooksByPublyearRoot()) {
				// refresh authors list
				log.d("Updating publyear list");
				mActivity.getDB().loadByDateList(fileOrDir, "publ_year_n", new ItemGroupsLoadingCallback(fileOrDir));
				return;
			}
			if (fileOrDir.isBooksByFiledateRoot()) {
				// refresh authors list
				log.d("Updating filedate list");
				mActivity.getDB().loadByDateList(fileOrDir, "file_create_time", new ItemGroupsLoadingCallback(fileOrDir));
				return;
			}
			if (fileOrDir.isBooksByRatingRoot()) {
				log.d("Updating rated books list");
				mActivity.getDB().loadBooksByRating(1, 10, new FileInfoLoadingCallback(fileOrDir));
				return;
			}
			if (fileOrDir.isBooksByStateFinishedRoot()) {
				log.d("Updating books by state=finished");
				mActivity.getDB().loadBooksByState(FileInfo.STATE_FINISHED, new FileInfoLoadingCallback(fileOrDir));
				return;
			}
			if (fileOrDir.isBooksByStateReadingRoot()) {
				log.d("Updating books by state=reading");
				mActivity.getDB().loadBooksByState(FileInfo.STATE_READING, new FileInfoLoadingCallback(fileOrDir));
				return;
			}
			if (fileOrDir.isBooksByStateToReadRoot()) {
				log.d("Updating books by state=toRead");
				mActivity.getDB().loadBooksByState(FileInfo.STATE_TO_READ, new FileInfoLoadingCallback(fileOrDir));
				return;
			}
			if (fileOrDir.isBooksByTitleRoot()) {
				// refresh authors list
				log.d("Updating title list");
				mActivity.getDB().loadTitleList(fileOrDir, new ItemGroupsLoadingCallback(fileOrDir));
				return;
			}
			if (fileOrDir.isBooksByAuthorDir()) {
				log.d("Updating author book list");
				mActivity.getDB().loadAuthorBooks(fileOrDir.getAuthorId(), new FileInfoLoadingCallback(fileOrDir));
				return;
			}
			if (fileOrDir.isBooksBySeriesDir()) {
				log.d("Updating series book list");
				mActivity.getDB().loadSeriesBooks(fileOrDir.getSeriesId(), new FileInfoLoadingCallback(fileOrDir));
				return;
			}
			if (fileOrDir.isBooksByBookdateDir()) {
				log.d("Updating bookdate book list");
				mActivity.getDB().loadByDateBooks(fileOrDir.getBookdateId(), "book_date_n", new FileInfoLoadingCallback(fileOrDir));
				return;
			}
			if (fileOrDir.isBooksByDocdateDir()) {
				log.d("Updating docdate book list");
				mActivity.getDB().loadByDateBooks(fileOrDir.getDocdateId(), "doc_date_n", new FileInfoLoadingCallback(fileOrDir));
				return;
			}
			if (fileOrDir.isBooksByPublyearDir()) {
				log.d("Updating publyear book list");
				mActivity.getDB().loadByDateBooks(fileOrDir.getPublyearId(), "publ_year_n", new FileInfoLoadingCallback(fileOrDir));
				return;
			}
			if (fileOrDir.isBooksByFiledateDir()) {
				log.d("Updating filedate book list");
				mActivity.getDB().loadByDateBooks(fileOrDir.getFiledateId(), "file_create_time",  new FileInfoLoadingCallback(fileOrDir));
				return;
			}
		} else {
			// fileOrDir == null
			if (currDirectory != null)
				return; // just show current directory
			if (mScanner.getRoot() != null && mScanner.getRoot().dirCount() > 0) {
				if ( mScanner.getRoot().getDir(0).fileCount()>0 ) {
					fileOrDir = mScanner.getRoot().getDir(0);
					itemToSelect = mScanner.getRoot().getDir(0).getFile(0);
				} else {
					fileOrDir = mScanner.getRoot();
					itemToSelect = mScanner.getRoot().dirCount()>1 ? mScanner.getRoot().getDir(1) : null;
				}
			}
		}
		final FileInfo file = fileOrDir==null || fileOrDir.isDirectory ? itemToSelect : fileOrDir;
		final FileInfo dir = fileOrDir!=null && !fileOrDir.isDirectory ? mScanner.findParent(file, mScanner.getRoot()) : fileOrDir;
		if ( dir!=null ) {
			mScanner.scanDirectory(mActivity.getDB(), dir, new Runnable() {
				public void run() {
					if (dir.allowSorting())
						dir.sort(mSortOrder);
					showDirectoryInternal(dir, file);
				}
			}, false, new Scanner.ScanControl() );
		} else
			showDirectoryInternal(null, file);
	}
	
	public void scanCurrentDirectoryRecursive() {
		if (currDirectory == null)
			return;
		if (currDirectory.isOPDSDir()) {
			currDirectory.dirs = null;
			currDirectory.files = null;
			showOPDSDir(currDirectory, null, "");
		} else {
			log.i("scanCurrentDirectoryRecursive started");
			final Scanner.ScanControl control = new Scanner.ScanControl();
			final ProgressDialog dlg = ProgressDialog.show(mActivity,
					mActivity.getString(R.string.dlg_scan_title),
					mActivity.getString(R.string.dlg_scan_message),
					true, true, new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							log.i("scanCurrentDirectoryRecursive : stop handler");
							control.stop();
						}
					});
			mScanner.scanDirectory(mActivity.getDB(), currDirectory, new Runnable() {
				@Override
				public void run() {
					log.i("scanCurrentDirectoryRecursive : finish handler");
					if (dlg.isShowing())
						dlg.dismiss();
				}
			}, true, control);
		}
	}


	public boolean isSimpleViewMode() {
		return isSimpleViewMode;
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

	public void setSimpleViewMode( boolean isSimple ) {
		if ( isSimpleViewMode!=isSimple ) {
			isSimpleViewMode = isSimple;
			if (isSimple) {
				mSortOrder = FileInfo.SortOrder.FILENAME;
				mActivity.saveSetting(ReaderView.PROP_APP_BOOK_SORT_ORDER, mSortOrder.name());
			}
			if ( isShown() && currDirectory!=null ) {
				showDirectory(currDirectory, null);
			}
		}
	}
	private boolean isSimpleViewMode = true;

	private FileListAdapter currentListAdapter;
	
	private class FileListAdapter extends BaseListAdapter {

		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			if (currDirectory == null)
				return 0;
			return currDirectory.fileCount() + currDirectory.dirCount();
		}

		public Object getItem(int position) {
			if (currDirectory == null)
				return null;
			if ( position<0 )
				return null;
			return currDirectory.getItem(position);
		}

		public long getItemId(int position) {
			if (currDirectory == null)
				return 0;
			return position;
		}

		public final int VIEW_TYPE_LEVEL_UP = 0;
		public final int VIEW_TYPE_DIRECTORY = 1;
		public final int VIEW_TYPE_FILE = 2;
		public final int VIEW_TYPE_FILE_SIMPLE = 3;
		public final int VIEW_TYPE_OPDS_BOOK = 4;
		public final int VIEW_TYPE_COUNT = 5;
		public int getItemViewType(int position) {
			if (currDirectory == null)
				return 0;
			if (position < 0)
				return Adapter.IGNORE_ITEM_VIEW_TYPE;
			if (position < currDirectory.dirCount())
				return VIEW_TYPE_DIRECTORY;
			position -= currDirectory.dirCount();
			if (position < currDirectory.fileCount()) {
				Object itm = getItem(position);
				if (itm instanceof FileInfo) {
					FileInfo fi = (FileInfo)itm;
					if (fi.isOPDSBook())
						return VIEW_TYPE_OPDS_BOOK;
				}
				return isSimpleViewMode ? VIEW_TYPE_FILE_SIMPLE : VIEW_TYPE_FILE;
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
			TextView linkToFile;
			TextView fieldState;
			ImageView imageAddInfo;
			ImageView imageAddMenu;
			ImageView imageFavFolder;
			//TextView field3;
			void setText( TextView view, String text, int color )
			{
				if ( view==null )
					return;
				if ( text!=null && text.length()>0 ) {
					view.setText(text);
					if (color != 0) view.setTextColor(color);
					view.setVisibility(ViewGroup.VISIBLE);
				} else {
					view.setText(null);
					view.setVisibility(ViewGroup.INVISIBLE);
				}
			}
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
				if ( item==null ) {
					image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_back_drawable, R.drawable.cr3_browser_back));
                    mActivity.tintViewIcons(image,true);
					String thisDir = "";
					if ( parentItem!=null ) {
						if ( parentItem.pathname.startsWith("@") )
							thisDir = "/" + parentItem.filename;
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
					imageAddInfo.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							Services.getHistory().getOrCreateBookInfo(mActivity.getDB(), item, new History.BookInfoLoadedCallack() {
								@Override
								public void onBookInfoLoaded(BookInfo bookInfo) {
									//mActivity.showToast(item.annotation);
									//log.v("BOOKNF annotation:"+item.annotation);
                                    BookInfo bi = new BookInfo(item);
									mActivity.showBookInfo(bi, BookInfoDialog.BOOK_INFO, currDirectory);
								}
							});
						}
					});
				}

				if (imageAddMenu!=null) {
					mActivity.tintViewIcons(imageAddMenu,true);
					imageAddMenu.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							if ( item!=null ) {
								selectedItem = item;
								showItemPopupMenu();
							}
//							Services.getHistory().getOrCreateBookInfo(mActivity.getDB(), item, new History.BookInfoLoadedCallack() {
//								@Override
//								public void onBookInfoLoaded(BookInfo bookInfo) {
//									mActivity.showToast("Did not implemented yet...");
//									showContextMenu();
//								}
//							});
						}
					});
				}

				if (imageFavFolder!=null) {
                    imageFavFolder.setVisibility(VISIBLE);
                    //ArrayList<FileInfo> ff = Services.getFileSystemFolders().getFavoriteFolders();
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
					imageFavFolder.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							if ( item!=null ) {
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
								}
							}
						}
					});
				}
				boolean doTint = true;
				if ( item.isDirectory ) {
					if (item.isBooksByAuthorRoot())
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.attr_icons8_folder_author, R.drawable.icons8_folder_author));
					else if (item.isBooksBySeriesRoot())
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
					else if (item.isOnlineCatalogPluginDir()) {
						image.setImageResource(R.drawable.litres);
						doTint = false;
					}
					else if (item.isSearchShortcut())
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_find_drawable, R.drawable.cr3_browser_find));
					else if ( item.isRecentDir() )
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_folder_recent_drawable, R.drawable.cr3_browser_folder_recent));
					else if ( item.isArchive )
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_folder_zip_drawable, R.drawable.cr3_browser_folder_zip));
					else
						image.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_folder_drawable, R.drawable.cr3_browser_folder));
                    if (doTint) mActivity.tintViewIcons(image,true);
					String title = item.filename;
					
					if (item.isOnlineCatalogPluginDir())
						title = translateOnlineStorePluginItem(item);
					
					setText(name, title, 0);

					if ( item.isBooksByAuthorDir() ) {
						int bookCount = 0;
						if (item.fileCount() > 0)
							bookCount = item.fileCount();
						else if (item.tag != null && item.tag instanceof Integer)
							bookCount = (Integer)item.tag;
						setText(field1, "books: " + String.valueOf(bookCount), colorIcon);
						setText(field2, "folders: 0", colorIcon);
						setText(fieldState, "", colorIcon);
					} else if ( item.isBooksBySeriesDir() || item.isBooksByBookdateDir() || item.isBooksByDocdateDir()
							|| item.isBooksByPublyearDir() || item.isBooksByFiledateDir()) {
						int bookCount = 0;
						if (item.fileCount() > 0)
							bookCount = item.fileCount();
						else if (item.tag != null && item.tag instanceof Integer)
							bookCount = (Integer)item.tag;
						setText(field1, "books: " + String.valueOf(bookCount), colorIcon);
						setText(field2, "folders: 0", colorIcon);
						setText(fieldState, "", colorIcon);
					}  else  if (item.isOPDSDir()) {
						setText(field1, item.title, colorIcon);
						setText(field2, "", colorIcon);
						setText(fieldState, "", colorIcon);
					} else  if ( !item.isOPDSDir() && !item.isSearchShortcut() && ((!item.isOPDSRoot()
                            && !item.isBooksByAuthorRoot() && !item.isBooksBySeriesRoot() && !item.isBooksByBookdateRoot()
							&& !item.isBooksByDocdateRoot() && !item.isBooksByPublyearRoot() && !item.isBooksByFiledateRoot()
                            && !item.isBooksByTitleRoot()) || item.dirCount()>0) && !item.isOnlineCatalogPluginDir()) {
						setText(field1, "books: " + String.valueOf(item.fileCount()), colorIcon);
						setText(field2, "folders: " + String.valueOf(item.dirCount()), colorIcon);
						setText(fieldState, "", colorIcon);
					} else {
						setText(field1, "", colorIcon);
						setText(field2, "", colorIcon);
						setText(fieldState, "", colorIcon);
					}
				} else {
					boolean isSimple = (viewType == VIEW_TYPE_FILE_SIMPLE);
					if ( image!=null ) {
						if ( isSimple ) {
							image.setImageResource(
									//item.format.getIconResourceId()
									item.format.getIconResourceIdThemed(mActivity)
							);
                            mActivity.tintViewIcons(image,true);
						} else {
							if (coverPagesEnabled) {
								image.setImageDrawable(mCoverpageManager.getCoverpageDrawableFor(mActivity.getDB(), item));
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
					if ( isSimple ) {
						String fn = item.getFileNameToDisplay();
						setText( filename, fn , 0);
					} else {
						setText( author, Utils.formatAuthors(item.getAuthors()), colorIcon );
                        //setText( author, item.authors );
                        String seriesName = Utils.formatSeries(item.series, item.seriesNumber);
						String title = item.title;
						String filename1 = item.filename;
						String filename2 = item.isArchive && item.arcname != null /*&& !item.isDirectory */
								? new File(item.arcname).getName() : null;
								
						String onlineBookInfo = "";
						if (item.getOnlineStoreBookInfo() != null) {
							OnlineStoreBook book = item.getOnlineStoreBookInfo();
							onlineBookInfo = "";
							if (book.rating > 0)
								onlineBookInfo = onlineBookInfo + "rating:" + book.rating + "  ";
							if (book.price > 0)
								onlineBookInfo = onlineBookInfo + "price:" + book.price + "  ";
							
						}
						if ( title==null || title.length()==0 ) {
							title = filename1;
							if (seriesName==null)
								seriesName = filename2;
						} else if (seriesName==null) 
							seriesName = filename1;

						String sLangFrom = item.lang_from;
						String sLangTo = item.lang_to;
						String sLang = "";
						if ((!StrUtils.isEmptyStr(sLangFrom))||(!StrUtils.isEmptyStr(sLangTo))) {
							if (StrUtils.isEmptyStr(sLangFrom)) sLangFrom = "any";
							if (StrUtils.isEmptyStr(sLangTo)) sLangTo = "any";
							sLang = "[" +sLangFrom+" > "+sLangTo + "] ";
						}
						if (sLang.equals(""))
							setText( name, title, 0 );
						else setText( name, title + "; "+sLang, 0);
						setText( series, seriesName , colorIcon);

//						field1.setVisibility(VISIBLE);
//						field2.setVisibility(VISIBLE);
//						field3.setVisibility(VISIBLE);
						String state = Utils.formatReadingState(mActivity, item);
						if (field1 != null) {
							if (fieldState == null)	{
								field1.setText(onlineBookInfo + "  " + state + " " + Utils.formatFileInfo(mActivity, item));
								field1.setTextColor(colorIcon);
							} else {
								field1.setText(onlineBookInfo + " " + Utils.formatFileInfo(mActivity, item));
								field1.setTextColor(colorIcon);
							}
						}
						if (fieldState != null) {
							fieldState.setText(state);
//							int colorBlue = Utils.resolveResourceIdByAttr(mActivity, R.attr.colorThemeBlue, Color.BLUE);
//							int colorGreen = Utils.resolveResourceIdByAttr(mActivity, R.attr.colorThemeGreen, Color.GREEN);
//							int colorGray = Utils.resolveResourceIdByAttr(mActivity, R.attr.colorThemeGray, Color.GRAY);
							int colorBlue;
							int colorGreen;
							int colorGray;
							a = mActivity.getTheme().obtainStyledAttributes(new int[]
									{R.attr.colorThemeBlue,
									 R.attr.colorThemeGreen,
									 R.attr.colorThemeGray});
							colorBlue = a.getColor(0, Color.BLUE);
							colorGreen = a.getColor(1, Color.GREEN);
							colorGray = a.getColor(2, Color.GRAY);
                            a.recycle();
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
						//field2.setText(formatDate(pos!=null ? pos.getTimeStamp() : item.createTime));
						if (field2 != null) {
							field2.setText(Utils.formatLastPosition(mActivity, mHistory.getLastPos(item)));
							field2.setTextColor(colorIcon);
							field2.setBackgroundColor(colorGrayCT);
						}
						TextView fld = linkToFile;
						if (fld == null) fld = field2;
						if (
							 (fld != null)
						  	  && ((!StrUtils.isEmptyStr(item.pathnameR))||(!StrUtils.isEmptyStr(item.arcnameR)))
						) {
							String s="";
							if (!StrUtils.isEmptyStr(item.arcnameR)) {
								if ((item.arcnameR.contains(item.pathnameR)) ||
								  (item.arcnameR.contains(StrUtils.stripExtension(item.pathnameR))))
									s = item.arcnameR; else
									s = item.arcnameR + "@"+item.pathnameR;
							}
								else s = item.pathnameR;
							s = s.replace("/storage/emulated/0","/s/e/0");
							fld.setText(s);
							fld.setTextColor(colorIcon);
							fld.setBackgroundColor(colorGrayCT2);
						}
 					}
					
				}
				if ((linearLayoutF1and2!=null)&&(field2!=null)) {
					if (StrUtils.isEmptyStr(field2.getText().toString())) {
						linearLayoutF1and2.removeView(field2);
					}
				}
			}
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			if (currDirectory == null)
				return null;
			View view;
			ViewHolder holder;
			int vt = getItemViewType(position);
			if (convertView == null) {
				if ( vt==VIEW_TYPE_LEVEL_UP )
					view = mInflater.inflate(R.layout.browser_item_parent_dir, null);
				else if ( vt==VIEW_TYPE_DIRECTORY )
					view = mInflater.inflate(R.layout.browser_item_folder, null);
				else if ( vt==VIEW_TYPE_FILE_SIMPLE )
					view = mInflater.inflate(R.layout.browser_item_book_simple, null);
				else if (vt == VIEW_TYPE_OPDS_BOOK)
					view = mInflater.inflate(R.layout.browser_item_opds_book, null);
				else
					view = mInflater.inflate(R.layout.browser_item_book, null);
				holder = new ViewHolder();
				holder.image = (ImageView)view.findViewById(R.id.book_icon);
				holder.name = (TextView)view.findViewById(R.id.book_name);
				holder.author = (TextView)view.findViewById(R.id.book_author);
				holder.series = (TextView)view.findViewById(R.id.book_series);
				holder.filename = (TextView)view.findViewById(R.id.book_filename);
				holder.field1 = (TextView)view.findViewById(R.id.browser_item_field1);
				holder.field2 = (TextView)view.findViewById(R.id.browser_item_field2);
				holder.linearLayoutF1and2 = (LinearLayout)view.findViewById(R.id.browser_item_fields1_and_2);
				holder.linkToFile = (TextView)view.findViewById(R.id.linkToFile);
				holder.fieldState = (TextView)view.findViewById(R.id.browser_item_field_state);
				holder.imageAddInfo = (ImageView)view.findViewById(R.id.btn_option_add_info);
				holder.imageAddMenu = (ImageView)view.findViewById(R.id.btn_add_menu);
				holder.imageFavFolder = (ImageView)view.findViewById(R.id.btn_fav_folder);

				//holder.field3 = (TextView)view.findViewById(R.id.browser_item_field3);
				view.setTag(holder);
			} else {
				view = convertView;
				holder = (ViewHolder)view.getTag();
			}
			holder.viewType = vt;
			FileInfo item = (FileInfo)getItem(position);
			FileInfo parentItem = null;//item!=null ? item.parent : null;
			if ( vt == VIEW_TYPE_LEVEL_UP ) {
				item = null;
				parentItem = currDirectory;
			}
			holder.setItem(item, parentItem);
//			if ( DeviceInfo.FORCE_HC_THEME ) {
//				view.setBackgroundColor(Color.WHITE);
//			}
			return view;
		}

		public int getViewTypeCount() {
			if (currDirectory == null)
				return 1;
			return VIEW_TYPE_COUNT;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			if (currDirectory == null)
				return true;
			return mScanner.mFileList.size()==0;
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
		return item.filename;
	}
	
	private void setCurrDirectory(FileInfo newCurrDirectory) {
		if (currDirectory != null && currDirectory != newCurrDirectory) {
			ArrayList<CoverpageManager.ImageItem> filesToUqueue = new ArrayList<CoverpageManager.ImageItem>();
			for (int i=0; i<currDirectory.fileCount(); i++)
				filesToUqueue.add(new CoverpageManager.ImageItem(currDirectory.getFile(i), -1, -1));
			mCoverpageManager.unqueue(filesToUqueue);
		}
		currDirectory = newCurrDirectory;
	}
	
	private void showDirectoryInternal( final FileInfo dir, final FileInfo file )
	{
		BackgroundThread.ensureGUI();
		setCurrDirectory(dir);
		
		if (dir!=null && dir != currDirectory) {
			log.i("Showing directory " + dir + " " + Thread.currentThread().getName());
			if (dir.isRecentDir())
				mActivity.setLastLocation(dir.getPathName());
			else if (!dir.isSpecialDir())
				mActivity.setLastDirectory(dir.getPathName());
		}
		if ( !BackgroundThread.isGUIThread() )
			throw new IllegalStateException("showDirectoryInternal should be called from GUI thread!");
		int index = dir!=null ? dir.getItemIndex(file) : -1;
		if ( dir!=null && !dir.isRootDir() )
			index++;
		
		String title = "";
		if (dir != null) {
			title = dir.filename;
			if (!dir.isSpecialDir())
				title = dir.getPathName();
			if (dir.isOnlineCatalogPluginDir())
				title = translateOnlineStorePluginItem(dir);
		}
		
		mActivity.setBrowserTitle(title, dir);
		mListView.setAdapter(currentListAdapter);
		currentListAdapter.notifyDataSetChanged();
		mListView.setSelection(index);
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
    	int minScreenSize = screenWidth < screenHeight ? screenWidth : screenHeight;
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
		if ( !coverPagesEnabled ) {
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

