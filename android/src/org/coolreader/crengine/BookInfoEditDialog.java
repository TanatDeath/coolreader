package org.coolreader.crengine;

import java.util.ArrayList;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.CoverpageManager.CoverpageBitmapReadyListener;

import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class BookInfoEditDialog extends BaseDialog {

	private int mChosenState = 0;
	private int mChosenRate = 0;

	private CoolReader mActivity;

	public BookInfo getmBookInfo() {
		return mBookInfo;
	}

	private BookInfo mBookInfo;
	private FileInfo mParentDir;
	private Bitmap mBookCover;
	private LayoutInflater mInflater;
	private int mWindowSize;
	private boolean mIsRecentBooksItem;
	private Button btnStateNone;
	private Button btnStateToRead;
	private Button btnStateReading;
	private Button btnStateFinished;
	private ImageButton btnStar1;
	private ImageButton btnStar2;
	private ImageButton btnStar3;
	private ImageButton btnStar4;
	private ImageButton btnStar5;
	private int attrStar;
	private int attrStarFilled;

	public BookInfoEditDialog(CoolReader activity, FileInfo baseDir, BookInfo book, boolean isRecentBooksItem)
	{
		super("BookInfoEditDialog", activity, null, false, false);
		this.mParentDir = baseDir;
		DisplayMetrics outMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
		this.mWindowSize = outMetrics.widthPixels < outMetrics.heightPixels ? outMetrics.widthPixels : outMetrics.heightPixels;
		this.mActivity = activity;
		this.mBookInfo = book;
		this.mIsRecentBooksItem = isRecentBooksItem;
		if(getWindow().getAttributes().softInputMode==WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED) {
		    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		}
	}

	@Override
	protected void onCreate() {
        setCancelable(true);
        setCanceledOnTouchOutside(true);

        super.onCreate();
		L.v("OptionsDialog is created");
	}
	
	class AuthorItem {
		EditText editor;
		String value;
	}
	class AuthorList {
		String oldValue;
		ArrayList<AuthorItem> authorItems = new ArrayList<AuthorItem>();
		ViewGroup parent;
		void adjustEditors(AuthorItem item, boolean empty) {
			int index = authorItems.indexOf(item);
			if (empty) {
				// remove extra empty lines
				for (int i=authorItems.size() - 1; i >= 0; i--) {
					if (index == i)
						continue;
					AuthorItem v = authorItems.get(i); 
					if (v.value.length() == 0) {
						parent.removeView(v.editor);
						authorItems.remove(i);
					}
				}
			} else {
				// add one empty line, if none
				boolean found = false;
				for (int i=authorItems.size() - 1; i >= 0; i--) {
					if (index == i)
						continue;
					AuthorItem v = authorItems.get(i); 
					if (v.value.length() == 0) {
						found = true;
					}
				}
				if (!found) {
					add("");
				}
			}
		}
		void add(String value) {
			final AuthorItem item = new AuthorItem();
			item.editor = new EditText(getContext());
			item.value = value != null ? value : "";
			item.editor.setText(value != null ? value : "");
			//item.editor.setFocusableInTouchMode(false);
			authorItems.add(item);
			parent.addView(item.editor);
			item.editor.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					boolean oldValueEmpty = item.value == null || item.value.trim().length() == 0;
					item.value = String.valueOf(s).trim();
					boolean newValueEmpty = item.value == null || item.value.trim().length() == 0;
					if (oldValueEmpty != newValueEmpty) {
						adjustEditors(item, newValueEmpty);
					}
				}
				
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count,
						int after) {
				}
				
				@Override
				public void afterTextChanged(Editable s) {
				}
			});
		}
		public AuthorList(ViewGroup parent, String authors) {
			this.oldValue = authors;
			this.parent = parent;
	        parent.removeAllViews();
	        if (authors != null) {
		        String[] list = authors.split("\\|");
		        for (String author : list)
		        	add(author);
	        }
			add("");
		}
		public String getAuthorsList() {
			StringBuilder buf = new StringBuilder();
			for (AuthorItem item : authorItems) {
				String author = item.value != null ? item.value.trim() : "";
				if (author.length() > 0) {
					if (buf.length() > 0)
						buf.append("|");
					buf.append(author);
				}
			}
			return buf.toString();
		}
	}

	static class ProgressDrawable extends Drawable {

		int progress;
		int dx;
		int dy;
		public ProgressDrawable(int dx, int dy, int progress) {
			this.dx = dx;
			this.dy = dy;
			this.progress = progress;
		}

		@Override
		public int getIntrinsicHeight() {
			return dy;
		}

		@Override
		public int getIntrinsicWidth() {
			return dx;
		}

		private void drawRect(Canvas canvas, Rect rc, Paint paint) {
			canvas.drawRect(rc.left, rc.top, rc.right, rc.top + 1, paint);
			canvas.drawRect(rc.left, rc.bottom - 1, rc.right, rc.bottom, paint);
			canvas.drawRect(rc.left, rc.top + 1, rc.left + 1, rc.bottom - 1, paint);
			canvas.drawRect(rc.right - 1, rc.top + 1, rc.right, rc.bottom - 1, paint);
		}

		private void shrink(Rect rc, int by) {
			rc.top += by;
			rc.bottom -= by;
			rc.left += by;
			rc.right -= by;
		}
		
		@Override
		public void draw(Canvas canvas) {
			Rect bounds = getBounds();
			if (progress >= 0 && progress <= 10000) {
				Rect rc = new Rect(bounds);
				Paint light = new Paint();
				light.setColor(0xC0FFFFFF);
				Paint dark = new Paint();
				dark.setColor(0xC0404040);
				drawRect(canvas, rc, light);
				shrink(rc, 1);
				drawRect(canvas, rc, dark);
				shrink(rc, 2);
				int x = rc.width() * progress / 10000 + rc.left;
				Rect rc1 = new Rect(rc);
				rc1.right = x;
				canvas.drawRect(rc1, light);
				Rect rc2 = new Rect(rc);
				rc2.left = x;
				canvas.drawRect(rc2, dark);
			}
		}

		@Override
		public int getOpacity() {
			return PixelFormat.TRANSPARENT;
		}

		@Override
		public void setAlpha(int alpha) {
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
		}
	}
	
    EditText edTitle;
    EditText edSeriesName;
    EditText edSeriesNumber;
	AuthorList authors;
	EditText edLangFrom;
	EditText edLangTo;
    EditText edGenre;
    EditText edAnnotation;
    EditText edSrclang;
    EditText edBookdate;
    EditText edTranslator;
    EditText edDocauthor;
    EditText edDocprogram;
    EditText edDocdate;
    EditText edDocsrcurl;
    EditText edDocsrcocr;
    EditText edDocversion;
    EditText edPublname;
    EditText edPublisher;
    EditText edPublcity;
    EditText edPublyear;
    EditText edPublisbn;
    EditText edPublseriesName;
    EditText edPublseriesNumber;
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        mInflater = LayoutInflater.from(getContext());
        FileInfo file = mBookInfo.getFileInfo();
        ViewGroup view = (ViewGroup)mInflater.inflate(R.layout.book_info_edit_dialog, null);
        
        ImageButton btnBack = (ImageButton)view.findViewById(R.id.base_dlg_btn_back);
		btnBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onNegativeButtonClick();
			}
		});
		ImageButton btnBack1 = (ImageButton)view.findViewById(R.id.base_dlg_btn_back1);
		btnBack1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onNegativeButtonClick();
			}
		});
		ImageButton btnOk = (ImageButton)view.findViewById(R.id.base_dlg_btn_positive);
		btnOk.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onOkButtonClick();
			}
		});
		ImageButton btnOk1 = (ImageButton)view.findViewById(R.id.base_dlg_btn_positive1);
		btnOk1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onOkButtonClick();
			}
		});
		ImageButton btnOpenBook = (ImageButton)view.findViewById(R.id.btn_open_book);
        btnOpenBook.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onPositiveButtonClick();
			}
		});
        ImageButton btnDeleteBook = (ImageButton)view.findViewById(R.id.book_delete);
        btnDeleteBook.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			mActivity.askDeleteBook(mBookInfo.getFileInfo());
			dismiss();
			}
		});
        ImageButton btnCustomCover = (ImageButton)view.findViewById(R.id.book_custom_cover);
		btnCustomCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
		        if (mActivity.picReceived!=null) {
					if (mActivity.picReceived.bmpReceived!=null) {
						PictureCameDialog dlg = new PictureCameDialog(((CoolReader) activity),
								mBookInfo, "");
						dlg.show();
					}
                } else {
                    ((CoolReader)activity).showToast(R.string.pic_no_pic);
                }
            }
        });

		ImageButton btnShortcutBook = (ImageButton)view.findViewById(R.id.book_shortcut);
		btnShortcutBook.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			mActivity.createBookShortcut(mBookInfo.getFileInfo(),mBookCover);
			dismiss();
			}
		});

		ImageButton btnSaveToGD = (ImageButton)view.findViewById(R.id.save_to_gd);

		btnSaveToGD.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((CoolReader)mActivity).mGoogleDriveTools.signInAndDoAnAction(
						((CoolReader)mActivity).mGoogleDriveTools.REQUEST_CODE_SAVE_CURRENT_BOOK_TO_GD, BookInfoEditDialog.this);
			}
		});

		btnStateNone = (Button)view.findViewById(R.id.book_state_new);
		btnStateToRead  = (Button)view.findViewById(R.id.book_state_toread);
		btnStateReading = (Button)view.findViewById(R.id.book_state_reading);
		btnStateFinished = (Button)view.findViewById(R.id.book_state_finished);
		btnStar1 = (ImageButton)view.findViewById(R.id.book_star1);
		btnStar2 = (ImageButton)view.findViewById(R.id.book_star2);
		btnStar3 = (ImageButton)view.findViewById(R.id.book_star3);
		btnStar4 = (ImageButton)view.findViewById(R.id.book_star4);
		btnStar5 = (ImageButton)view.findViewById(R.id.book_star5);
		int colorBlue;
		int colorGreen;
		int colorGray;
		int colorIcon;
		TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeBlue,
				 R.attr.colorThemeGreen,
				 R.attr.colorThemeGray,
				 R.attr.colorIcon,
				 R.attr.attr_icons8_rate_star,
				 R.attr.attr_icons8_rate_star_filled
				});
		colorBlue = a.getColor(0, Color.BLUE);
		colorGreen = a.getColor(1, Color.GREEN);
		colorGray = a.getColor(2, Color.GRAY);
		colorIcon = a.getColor(3, Color.GRAY);
		attrStar = a.getResourceId(4, 0);
		attrStarFilled = a.getResourceId(5, 0);

		btnStateNone.setTextColor(colorIcon);
		btnStateReading.setTextColor(colorGreen);
		btnStateToRead.setTextColor(colorBlue);
		btnStateFinished.setTextColor(colorGray);
		a.recycle();
		edTitle = (EditText)view.findViewById(R.id.book_title);
        edSeriesName = (EditText)view.findViewById(R.id.book_series_name);
        edSeriesNumber = (EditText)view.findViewById(R.id.book_series_number);
   		edLangFrom = (EditText)view.findViewById(R.id.book_lang_from);
		edLangTo = (EditText)view.findViewById(R.id.book_lang_to);
        edGenre = (EditText)view.findViewById(R.id.genre);
        edAnnotation = (EditText)view.findViewById(R.id.annotation);
        edSrclang = (EditText)view.findViewById(R.id.srclang);
        edBookdate = (EditText)view.findViewById(R.id.bookdate);
        edTranslator = (EditText)view.findViewById(R.id.translator);
        edDocauthor = (EditText)view.findViewById(R.id.docauthor);
        edDocprogram = (EditText)view.findViewById(R.id.docprogram);
        edDocdate = (EditText)view.findViewById(R.id.docdate);
        edDocsrcurl = (EditText)view.findViewById(R.id.docsrcurl);
        edDocsrcocr = (EditText)view.findViewById(R.id.docsrcocr);
        edDocversion = (EditText)view.findViewById(R.id.docversion);
        edPublname = (EditText)view.findViewById(R.id.publname);
        edPublisher = (EditText)view.findViewById(R.id.publisher);
        edPublcity = (EditText)view.findViewById(R.id.publcity);
        edPublyear = (EditText)view.findViewById(R.id.publyear);
        edPublisbn = (EditText)view.findViewById(R.id.publisbn);
        edPublseriesName = (EditText)view.findViewById(R.id.book_publseries_name);
        edPublseriesNumber = (EditText)view.findViewById(R.id.book_publseries_number);
		int state = file.getReadingState();
 		setChecked(btnStateNone);
		if (state == FileInfo.STATE_TO_READ) setChecked(btnStateToRead);
		if (state == FileInfo.STATE_READING) setChecked(btnStateReading);
		if (state == FileInfo.STATE_FINISHED) setChecked(btnStateFinished);
		mChosenState = state;
		btnStateNone.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setChecked(btnStateNone);
			}
		});
		btnStateToRead.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setChecked(btnStateToRead);
			}
		});
		btnStateReading.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setChecked(btnStateReading);
			}
		});
		btnStateFinished.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setChecked(btnStateFinished);
			}
		});
		btnStar1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setRate(btnStar1);
			}
		});
		btnStar2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setRate(btnStar2);
			}
		});
		btnStar3.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setRate(btnStar3);
			}
		});
		btnStar4.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setRate(btnStar4);
			}
		});
		btnStar5.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setRate(btnStar5);
			}
		});
		final ImageView image = (ImageView)view.findViewById(R.id.book_cover);
        image.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// open book
				onPositiveButtonClick();
			}
		});
        int w = mWindowSize * 4 / 10;
        int h = w * 4 / 3;
        image.setMinimumHeight(h);
        image.setMaxHeight(h);
        image.setMinimumWidth(w);
        image.setMaxWidth(w);
        Bitmap bmp = Bitmap.createBitmap(w, h, Config.RGB_565);
        Services.getCoverpageManager().drawCoverpageFor(mActivity.getDB(), file, bmp, new CoverpageBitmapReadyListener() {
			@Override
			public void onCoverpageReady(CoverpageManager.ImageItem file, Bitmap bitmap) {
		        BitmapDrawable drawable = new BitmapDrawable(bitmap);
				mBookCover = bitmap;
				image.setImageDrawable(drawable);
			}
		}); 

        final ImageView progress = (ImageView)view.findViewById(R.id.book_progress);
        int percent = -1;
        Bookmark bmk = mBookInfo.getLastPosition();
        if (bmk != null)
        	percent = bmk.getPercent();
        if (percent >= 0 && percent <= 10000) {
        	progress.setMinimumWidth(w);
        	progress.setMaxWidth(w);
        	progress.setMinimumHeight(8);
        	progress.setMaxHeight(8);
        } else {
        	progress.setMinimumWidth(w);
        	progress.setMaxWidth(w);
        	progress.setMinimumHeight(0);
        	progress.setMaxHeight(0);
        }
        progress.setImageDrawable(new ProgressDrawable(w, 8, percent));
        
        edTitle.setText(file.title);
        //edAuthor.setText(file.authors);
        edSeriesName.setText(file.series);
        if (file.series != null && file.series.trim().length() > 0 && file.seriesNumber > 0)
        	edSeriesNumber.setText(String.valueOf(file.seriesNumber));
        edLangFrom.setText(file.lang_from);
		edLangTo.setText(file.lang_to);
        edGenre.setText(file.genre);
        edAnnotation.setText(file.annotation);
        edSrclang.setText(file.srclang);
        edBookdate.setText(file.getBookdate());
        edTranslator.setText(file.translator);
        edDocauthor.setText(file.docauthor);
        edDocprogram.setText(file.docprogram);
        edDocdate.setText(file.getDocdate());
        edDocsrcurl.setText(file.docsrcurl);
        edDocsrcocr.setText(file.docsrcocr);
        edDocversion.setText(file.docversion);
        edPublname.setText(file.publname);
        edPublisher.setText(file.publisher);
        edPublcity.setText(file.publcity);
        edPublyear.setText(file.getPublyear());
        edPublisbn.setText(file.publisbn);
        edPublseriesName.setText(file.publseries);
		if (file.publseries != null && file.publseries.trim().length() > 0 && file.publseriesNumber > 0)
			edPublseriesNumber.setText(String.valueOf(file.publseriesNumber));
		LinearLayout llBookAuthorsList = (LinearLayout)view.findViewById(R.id.book_authors_list);
        authors = new AuthorList(llBookAuthorsList, file.getAuthors());
        mChosenRate = 1;
        setRate(btnStar1);
        int mChosenRate1 = file.getRate();
        if (mChosenRate1 == 1) setRate(btnStar1);
		if (mChosenRate1 == 2) setRate(btnStar2);
		if (mChosenRate1 == 3) setRate(btnStar3);
		if (mChosenRate1 == 4) setRate(btnStar4);
		if (mChosenRate1 == 5) setRate(btnStar5);
        
    	ImageButton btnRemoveRecent = ((ImageButton)view.findViewById(R.id.book_recent_delete));
    	ImageButton btnOpenFolder = ((ImageButton)view.findViewById(R.id.book_folder_open));
        if (mIsRecentBooksItem) {
        	btnRemoveRecent.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mActivity.askDeleteRecent(mBookInfo.getFileInfo());
					dismiss();
				}
			});
        	btnOpenFolder.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mActivity.showDirectory(mBookInfo.getFileInfo());
					dismiss();
				}
			});
        } else {
        	ViewGroup parent = ((ViewGroup)btnRemoveRecent.getParent());
        	parent.removeView(btnRemoveRecent);
        	parent.removeView(btnOpenFolder);
        }
	   setView(view);
	}
	
	private void save() {
		L.d("BookInfoEditDialog.save()");
		
        FileInfo file = mBookInfo.getFileInfo();
        boolean modified = false;
        modified = file.setTitle(edTitle.getText().toString().trim()) || modified;
        modified = file.setAuthors(authors.getAuthorsList()) || modified;
        modified = file.setSeriesName(edSeriesName.getText().toString().trim()) || modified;
        modified = file.setLangFrom(edLangFrom.getText().toString().trim()) || modified;
        modified = file.setLangTo(edLangTo.getText().toString().trim()) || modified;
        modified = file.setGenre(edGenre.getText().toString().trim()) || modified;
        modified = file.setAnnotation(edAnnotation.getText().toString().trim()) || modified;
        modified = file.setSrclang(edSrclang.getText().toString().trim()) || modified;
        modified = file.setBookdate(edBookdate.getText().toString().trim()) || modified;
        modified = file.setTranslator(edTranslator.getText().toString().trim()) || modified;
        modified = file.setDocauthor(edDocauthor.getText().toString().trim()) || modified;
        modified = file.setDocprogram(edDocprogram.getText().toString().trim()) || modified;
        modified = file.setDocdate(edDocdate.getText().toString().trim()) || modified;
        modified = file.setDocsrcurl(edDocsrcurl.getText().toString().trim()) || modified;
        modified = file.setDocsrcocr(edDocsrcocr.getText().toString().trim()) || modified;
        modified = file.setDocversion(edDocversion.getText().toString().trim()) || modified;
        modified = file.setPublname(edPublname.getText().toString().trim()) || modified;
        modified = file.setPublisher(edPublisher.getText().toString().trim()) || modified;
        modified = file.setPublcity(edPublcity.getText().toString().trim()) || modified;
        modified = file.setPublyear(edPublyear.getText().toString().trim()) || modified;
        modified = file.setPublisbn(edPublisbn.getText().toString().trim()) || modified;
        modified = file.setPublseries(edPublseriesName.getText().toString().trim()) || modified;
        if (mActivity.getReaderView()!=null) {
			if (mActivity.getReaderView().getBookInfo()!=null) {
				BookInfo book = mActivity.getReaderView().getBookInfo();
				book.getFileInfo().lang_from = edLangFrom.getText().toString().trim();
				book.getFileInfo().lang_to = edLangTo.getText().toString().trim();
			}
		}
		int number = 0;
        if (file.series != null && file.series.length() > 0) {
    	    String numberString = edSeriesNumber.getText().toString().trim();
    	    try {
    	    	number = Integer.valueOf(numberString);
    	    } catch (NumberFormatException e) {
    	    	// ignore
    	    }
        }
        modified = file.setSeriesNumber(number) || modified;
        number = 0;
        if (file.publseries != null && file.publseries.length() > 0) {
            String numberString = edPublseriesNumber.getText().toString().trim();
            try {
                number = Integer.valueOf(numberString);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        modified = file.setPublseriesNumber(number) || modified;
        int rate = mChosenRate;
        if (rate >=0 && rate <= 5)
            modified = file.setRate(rate) || modified;
		int state = mChosenState;
		modified = file.setReadingState(state) || modified;
        if (modified) {
			mActivity.getDB().saveBookInfo(mBookInfo);
        	mActivity.getDB().flush();
        	BookInfo bi = Services.getHistory().getBookInfo(file);
        	if (bi != null)
        		bi.getFileInfo().setFileProperties(file);
        	mParentDir.setFile(file);
        	mActivity.directoryUpdated(mParentDir, file);
        }
	}

	@Override
	protected void onPositiveButtonClick() {
		save();
		mActivity.loadDocument(mBookInfo.getFileInfo(), new Runnable() {
			@Override
			public void run() {
				// error occured
				// ignoring
			}
		});
		super.onPositiveButtonClick();
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
	}

	protected void onOkButtonClick() {
		save();
		super.onPositiveButtonClick();
	}

	private void setChecked(Button btn) {
		if (btn == btnStateNone) {
			mChosenState = FileInfo.STATE_NEW;
		}
		if (btn == btnStateToRead) {
			mChosenState = FileInfo.STATE_TO_READ;

		}
		if (btn == btnStateReading) {
			mChosenState = FileInfo.STATE_READING;
		}
		if (btn == btnStateFinished) {
			mChosenState = FileInfo.STATE_FINISHED;
		}
		int colorGray;
		int colorGrayC;
		int colorIcon;
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		colorGray = a.getColor(0, Color.GRAY);
		colorGrayC = a.getColor(1, Color.GRAY);
		colorIcon = a.getColor(2, Color.GRAY);
		a.recycle();
		int colorGrayCT=Color.argb(0,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		btnStateNone.setBackgroundColor(colorGrayCT);
		btnStateToRead.setBackgroundColor(colorGrayCT);
		btnStateReading.setBackgroundColor(colorGrayCT);
		btnStateFinished.setBackgroundColor(colorGrayCT);
		btn.setBackgroundColor(colorGray);
	}

	private void setRate(ImageButton btn) {
		int mChosenRate1 = 0;
		if (btn == btnStar1) {
			mChosenRate1 = 1;
		}
		if (btn == btnStar2) {
			mChosenRate1 = 2;
		}
		if (btn == btnStar3) {
			mChosenRate1 = 3;
		}
		if (btn == btnStar4) {
			mChosenRate1 = 4;
		}
		if (btn == btnStar5) {
			mChosenRate1 = 5;
		}
		if (mChosenRate1==mChosenRate) mChosenRate=0; else mChosenRate=mChosenRate1;
		btnStar1.setImageResource(mChosenRate>=1?attrStarFilled:attrStar);
		btnStar2.setImageResource(mChosenRate>=2?attrStarFilled:attrStar);
		btnStar3.setImageResource(mChosenRate>=3?attrStarFilled:attrStar);
		btnStar4.setImageResource(mChosenRate>=4?attrStarFilled:attrStar);
		btnStar5.setImageResource(mChosenRate>=5?attrStarFilled:attrStar);
	}
	
}

