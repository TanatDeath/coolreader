package org.coolreader.crengine;

import java.util.ArrayList;

import org.coolreader.CoolReader;
import org.coolreader.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class BookmarksDlg  extends BaseDialog {
	CoolReader mCoolReader;
	ReaderView mReaderView;
	private LayoutInflater mInflater;
	BookInfo mBookInfo;
	BookmarkList mList;
	BookmarksDlg mThis;
	boolean mOnlyChoose;
	Object mObj;
	public Bookmark chosenBmk = null;

	public final static int ITEM_POSITION=0;
	public final static int ITEM_COMMENT=1;
	public final static int ITEM_CORRECTION=2;
	public final static int ITEM_SHORTCUT=3;
	
	class BookmarkListAdapter extends BaseAdapter {
		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			return mBookInfo.getBookmarkCount();
		}

		public Object getItem(int position) {
			if ( position<0 || position>=mBookInfo.getBookmarkCount() )
				return null;
			return mBookInfo.getBookmark(position);
		}

		public long getItemId(int position) {
			return position;
		}

		
		
		public int getItemViewType(int position) {
			Bookmark bm = (Bookmark)getItem(position);
			if ( bm==null )
				return ITEM_POSITION;
			switch ( bm.getType() ) {
			case Bookmark.TYPE_COMMENT:
				return ITEM_COMMENT;
			case Bookmark.TYPE_CORRECTION:
				return ITEM_CORRECTION;
			default:
				if ( bm.getShortcut()>0 )
					return ITEM_SHORTCUT;
				return ITEM_POSITION;
			}
		}

		public int getViewTypeCount() {
			return 4;
		}

		
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			int type = getItemViewType(position);
			if ( convertView==null ) {
				//view = new TextView(getContext());
				int res = R.layout.bookmark_position_item;
				switch ( type ) {
				case ITEM_COMMENT:
					res = R.layout.bookmark_comment_item;
					break;
				case ITEM_CORRECTION:
					res = R.layout.bookmark_correction_item;
					break;
				case ITEM_SHORTCUT:
					res = R.layout.bookmark_shortcut_item;
					break;
				}
				view = mInflater.inflate(res, null);
			} else {
				view = (View)convertView;
			}
			int colorIcon;
			TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
					{R.attr.colorIcon});
			colorIcon = a.getColor(0, Color.GRAY);
			a.recycle();

			TextView labelView = view.findViewById(R.id.bookmark_item_shortcut);
			if (labelView!=null) labelView.setTextColor(colorIcon);
			TextView posTextView = view.findViewById(R.id.bookmark_item_pos_text);
			if (posTextView!=null) posTextView.setTextColor(colorIcon);
			TextView titleTextView = view.findViewById(R.id.bookmark_item_title);
			if (titleTextView!=null) titleTextView.setTextColor(colorIcon);
			TextView commentTextView = view.findViewById(R.id.bookmark_item_comment_text);
			if (commentTextView!=null) commentTextView.setTextColor(colorIcon);
			if ( type==ITEM_CORRECTION && posTextView!=null )
				posTextView.setPaintFlags(posTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG );
				
			Bookmark b = (Bookmark)getItem(position);
			if ( labelView!=null ) {
				if ( b!=null && b.getShortcut()>0 )
					labelView.setText(String.valueOf(b.getShortcut()));
				else
					labelView.setText(String.valueOf(position+1));
			}
			if ( b!=null ) {
				String percentString = Utils.formatPercent(b.getPercent());
				String s1 = b.getTitleText();
				String s2 = b.getPosText();
				String s3 = b.getCommentText();
				String s4 = Utils.formatDateFixed(b.getTimeStamp()) + " " + Utils.formatTime(activity, b.getTimeStamp());
				if ( s1!=null && s2!=null ) {
					s1 = percentString + "   " + s1 + " (" + s4 + ")";
				} else if ( s1!=null ) {
					s2 = s1;
					s1 = percentString + " (" + s4 + ")";
				} else if ( s2!=null ) {
					s1 = percentString + " (" + s4 + ")";
				} else {
					s1 = s2 = "";
				}
				if ( titleTextView!=null )
					titleTextView.setText(s1);
				if ( posTextView!=null )
					posTextView.setText(s2);
				if ( commentTextView!=null )
					commentTextView.setText(s3);
			} else {
				if ( commentTextView!=null )
					commentTextView.setText("");
				if ( titleTextView!=null )
					titleTextView.setText("");
				if ( posTextView!=null )
					posTextView.setText("");
			}
			activity.tintViewIcons(view);
			return view;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			return mBookInfo.getBookmarkCount()==0;
		}

		private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();
		
		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}
	}
	
	class ShortcutBookmarkListAdapter extends BookmarkListAdapter {
		public int getCount() {
			return SHORTCUT_COUNT;
		}

		public Object getItem(int position) {
			return mBookInfo.findShortcutBookmark(position+1);
		}

		public int getItemViewType(int position) {
			return ITEM_SHORTCUT;
		}
	}
	
	class BookmarkList extends BaseListView {
		private ListAdapter mAdapter;
		private boolean mShortcutMode = false;
		
		public boolean isShortcutMode() {
			return mShortcutMode;
		}
		public void setShortcutMode( boolean shortcutMode ) {
			if (mBookInfo == null) {
				L.e("BookmarkList - mBookInfo is null");
				return;
			}
			if ( !shortcutMode )
				mBookInfo.sortBookmarks();
			updateAdapter( shortcutMode ? new ShortcutBookmarkListAdapter() : new BookmarkListAdapter() );
		}
		public void updateAdapter( BookmarkListAdapter adapter ) {
			mAdapter = adapter;
			setAdapter(mAdapter);
		}
		public BookmarkList( Context context, boolean shortcutMode ) {
			super(context, true);
			setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setShortcutMode(shortcutMode);
			setLongClickable(!mOnlyChoose);
			setOnItemLongClickListener((arg0, arg1, position, arg3) -> {
				selectedItem = position;
				selectedItemLong = position;
				openContextMenu(BookmarkList.this);
				return true;
			});
		}

		public Bookmark getSelectedBookmark() {
			return (Bookmark)mAdapter.getItem(selectedItem);
		}
		
		@Override
		public boolean performItemClick(View view, int position, long id) {
			if ( mShortcutMode ) {
				Bookmark b = mBookInfo.findShortcutBookmark(position+1);
				if ( b==null ) {
					mReaderView.addBookmark(position+1);
					mThis.dismiss();
					return true;
				}
				selectedItem = position;
				openContextMenu(this);
			} else {
				Bookmark bm = (Bookmark)mAdapter.getItem(position);
				if ( bm!=null ) {
					chosenBmk = bm;
					if (!mOnlyChoose) mReaderView.goToBookmark(bm);
					if (mObj != null) {
						if (mObj instanceof BookmarkEditDialog) {
						    BookmarkEditDialog bed = ((BookmarkEditDialog) mObj);
                            if (bed.isShowing()) {
						        bed.BookmarkChooseCallback(bm);
                            }
                        }
                    }
					dismiss();
				}
			}
			return true;
		}
		
		
	}
	
	final static int SHORTCUT_COUNT = 10;
	
	public BookmarksDlg( CoolReader activity, ReaderView readerView, final boolean bOnlyChoose, final Object obj )
	{
		super("BookmarksDlg", activity, activity.getResources().getString(R.string.win_title_bookmarks), true, false);
		mThis = this; // for inner classes
        mInflater = LayoutInflater.from(getContext());
		mCoolReader = activity;
		mReaderView = readerView;
		mBookInfo = mReaderView.getBookInfo();
		mOnlyChoose = bOnlyChoose;
		mObj = obj;
		setPositiveButtonImage(Utils.resolveResourceIdByAttr(activity, R.attr.cr3_button_add_drawable, R.drawable.cr3_button_add), R.string.mi_bookmark_add);
		View frame = mInflater.inflate(R.layout.bookmark_list_dialog, null);
		ViewGroup body = frame.findViewById(R.id.bookmark_list);
		frame.findViewById(R.id.btn_user_dic).setOnClickListener(v -> {
			cancel();
			UserDicDlg dlg = new UserDicDlg(activity,0);
			dlg.show();
		});
		int colorGrayC;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast});
		colorGrayC = a.getColor(0, Color.GRAY);
		a.recycle();
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));

		frame.findViewById(R.id.btn_user_dic).setBackgroundColor(colorGrayCT2);

		mList = new BookmarkList(activity, false);
		body.addView(mList);
		setView(frame);
		setFlingHandlers(mList, null, null);
	}

	@Override
	protected void onPositiveButtonClick() {
		// add bookmark
        if (!mOnlyChoose) {
            mReaderView.addBookmark(0);
            BookmarksDlg.this.dismiss();
        } else {
            activity.showToast("Not in selection mode");
        }
	}

	@Override
	protected void onNegativeButtonClick() {
		chosenBmk = null;
		if (mObj != null) {
			if (mObj instanceof BookmarkEditDialog) {
				BookmarkEditDialog bed = ((BookmarkEditDialog) mObj);
				if (bed.isShowing()) {
					bed.BookmarkChooseCallback(null);
				}
			}
		}
		BookmarksDlg.this.dismiss();
	}



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v("cr3", "creating BookmarksDlg");
		//setTitle(mCoolReader.getResources().getString(R.string.win_title_bookmarks));
        setCancelable(true);
		super.onCreate(savedInstanceState);
		if (!mOnlyChoose)
			registerForContextMenu(mList);
	}
	
	private void listUpdated() {
		mList.setShortcutMode(mList.isShortcutMode());
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		selectedItem = selectedItemLong;
		int shortcut = selectedItem; //mList.getSelectedItemPosition();
		Bookmark bm = mList.getSelectedBookmark();
		if ( mList.isShortcutMode() ) {
			if ( shortcut>=0 && shortcut<SHORTCUT_COUNT ) {
				switch (item.getItemId()) {
				case R.id.bookmark_shortcut_add:
					mReaderView.addBookmark(shortcut+1);
					listUpdated();
					dismiss();
					return true;
				case R.id.bookmark_delete:
					if (mReaderView.removeBookmark(bm) != null)
						listUpdated();
					return true;
				case R.id.bookmark_shortcut_goto:
					mReaderView.goToBookmark(shortcut+1);
					dismiss();
					return true;
				}
			}
			return super.onContextItemSelected(item);
		}
		switch (item.getItemId()) {
		case R.id.bookmark_add:
			mReaderView.addBookmark(0);
			listUpdated();
			dismiss();
			return true;
		case R.id.bookmark_delete:
			if (mReaderView.removeBookmark(bm) != null)
				listUpdated();
			return true;
		case R.id.bookmark_goto:
			if ( bm!=null )
				mReaderView.goToBookmark(bm);
			dismiss();
			return true;
		case R.id.bookmark_edit:
			if ( bm!=null && (bm.getType()==Bookmark.TYPE_COMMENT || bm.getType()==Bookmark.TYPE_CORRECTION)) {
				BookmarkEditDialog dlg = new BookmarkEditDialog(mCoolReader, mReaderView, bm, false, Bookmark.TYPE_COMMENT, "");
				dlg.show();
			}
			dismiss();
			return true;
		case R.id.bookmark_export:
			if ( mBookInfo.getBookmarkCount()>0 ) {
				FileInfo fi = mBookInfo.getFileInfo();
				String s = fi.getPathName();
				s = s.replace(FileInfo.ARC_SEPARATOR, "_");
				s = s + ".bmk.txt";
				if ( mBookInfo.exportBookmarks(s) )
					mCoolReader.showToast( getContext().getString(R.string.toast_bookmark_export_ok) + " " + s);
				else
					mCoolReader.showToast(getContext().getString(R.string.toast_bookmark_export_failed) + " " + s);
			}
			dismiss();
			return true;
		case R.id.bookmark_send:
			if ( mBookInfo.getBookmarkCount()>0 ) {
				String s = mBookInfo.getBookmarksExportText();
				mCoolReader.sendBookFragment(mBookInfo, s);
			}
			dismiss();
			return true;
		}
		return super.onContextItemSelected(item);
	}
	
	private int selectedItem;
	private int selectedItemLong;
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
	    MenuInflater inflater = mCoolReader.getMenuInflater();
	    menu.clear();
	    inflater.inflate(mList.isShortcutMode() ? R.menu.cr3_bookmark_shortcut_context_menu : R.menu.cr3_bookmark_context_menu, menu);
	    //AdapterContextMenuInfo mi = (AdapterContextMenuInfo)menuInfo;
	    //if ( mi!=null )
	    //	selectedItem = mi.position;
        // in this function menuInfo is always first item,
        // but selectedItem is already set in BookmarkList's OnItemLongClickListener
		Bookmark bm = mList.getSelectedBookmark();
		String bookmarkText = bm.getPosText();
		if (bookmarkText == null || bookmarkText.length() == 0)
			bookmarkText = bm.getTitleText();
		if (bookmarkText != null && bookmarkText.length() > 0)
			menu.setHeaderTitle(getContext().getString(R.string.context_menu_title_bookmark) + ": " + bookmarkText);
		else
			menu.setHeaderTitle(getContext().getString(R.string.context_menu_title_bookmark));
	    for ( int i=0; i<menu.size(); i++ ) {
	    	MenuItem menuItem = menu.getItem(i);
	    	if ( menuItem.getItemId()==R.id.bookmark_shortcut_goto || menuItem.getItemId()==R.id.bookmark_edit ||
	    			menuItem.getItemId()==R.id.bookmark_delete )
	    		menuItem.setEnabled(bm!=null);
	    	if ( menuItem.getItemId()==R.id.bookmark_edit )
	    		menuItem.setEnabled(bm!=null && (bm.getType()==Bookmark.TYPE_COMMENT || bm.getType()==Bookmark.TYPE_CORRECTION));
	    	menuItem.setOnMenuItemClickListener(item -> {
				onContextItemSelected(item);
				return true;
			});
	    }
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ( keyCode==KeyEvent.KEYCODE_MENU ) {
			openContextMenu(mList);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	

}
