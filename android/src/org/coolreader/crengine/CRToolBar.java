package org.coolreader.crengine;

import java.util.ArrayList;
import java.util.HashMap;

import org.coolreader.CoolReader;
import org.coolreader.R;

import android.graphics.Bitmap;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

public class CRToolBar extends ViewGroup {

	private static final Logger log = L.create("tb");
	
	final private BaseActivity activity;
	private ArrayList<ReaderAction> actions = new ArrayList<>();
	private ArrayList<ReaderAction> actionsToolbar = new ArrayList<>();
	private ArrayList<ReaderAction> actionsMore = new ArrayList<>();
	private ArrayList<ReaderAction> iconActions = new ArrayList<>();
	private boolean showLabels;
	private boolean ignoreInv;
	private int buttonHeight;
	private int buttonWidth;
	private int itemHeight; // multiline mode, line height 
	private int visibleButtonCount;
	private int visibleNonButtonCount;
	private boolean isVertical;
	private boolean isMultiline;
	final private int preferredItemHeight;
	private int BUTTON_SPACING = 4;
	private int BAR_SPACING = 4;
	private int buttonAlpha = 0xFF;
	private int textColor = 0x000000;
	private int windowDividerHeight = 0; // for popup window, height of divider below buttons
	private ImageButton overflowButton;
	private LayoutInflater inflater;
	private PopupWindow popup;
	private int popupLocation = Settings.VIEWER_TOOLBAR_BOTTOM;
	private int maxMultilineLines = 3;
	private int optionAppearance = 0;
	private float toolbarScale = 1.0f;
	private boolean grayIcons = false;
	private boolean invIcons = false;
	public boolean useBackgrColor = false;
	boolean isEInk;
	HashMap<Integer, Integer> themeColors;

	public boolean isColorDark(int color){
		double darkness = 1-(0.299*Color.red(color) + 0.587*Color.green(color) + 0.114*Color.blue(color))/255;
		if(darkness<0.5){
			return false; // It's a light color
		}else{
			return true; // It's a dark color
		}
	}

	@ColorInt
	static int darkenColor(@ColorInt int color) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		hsv[2] *= 0.7f;
		return Color.HSVToColor(hsv);
	}

	@ColorInt
	static int lightenColor(@ColorInt int color) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		hsv[2] *= 1.3f;
		return Color.HSVToColor(hsv);
	}

	public void tintViewIconsColor(View v) {
		if (!useBackgrColor) activity.tintViewIcons(v, true);
		else {
			Boolean custIcons = activity.settings().getBool(BaseActivity.PROP_APP_ICONS_IS_CUSTOM_COLOR, false);
			if (DeviceInfo.isForceHCTheme(BaseActivity.getScreenForceEink())) custIcons = false;
			int custColor = activity.settings().getColor(BaseActivity.PROP_APP_ICONS_CUSTOM_COLOR, 0x000000);
			if (custIcons) {
				int colorCur = custColor;
				if ((isColorDark(ReaderView.backgrNormalizedColor)) && (isColorDark(colorCur))) {
					while (isColorDark(colorCur)) colorCur = lightenColor(colorCur);
					colorCur = lightenColor(colorCur);
					colorCur = lightenColor(colorCur);
				} else
					if ((!isColorDark(ReaderView.backgrNormalizedColor)) && (!isColorDark(colorCur))) {
						while (!isColorDark(colorCur)) colorCur = darkenColor(colorCur);
						colorCur = darkenColor(colorCur);
						colorCur = darkenColor(colorCur);
					}
				activity.tintViewIconsC(v, true, colorCur);
			} else {
				TypedArray a = ((CoolReader) activity).getTheme().obtainStyledAttributes(new int[]
						{R.attr.colorIcon, R.attr.colorIconL});
				int colorIcon = a.getColor(0, Color.GRAY);
				int colorIconL = a.getColor(1, Color.GRAY);
				a.recycle();
				if (isColorDark(ReaderView.backgrNormalizedColor)) {
					if (isColorDark(colorIcon)) activity.tintViewIconsC(v, true, colorIconL);
					else activity.tintViewIconsC(v, true, colorIcon);
				} else {
					if (isColorDark(colorIcon)) activity.tintViewIconsC(v, true, colorIcon);
					else activity.tintViewIconsC(v, true, colorIconL);
				}
			}
		}
	}

	private void setPopup(PopupWindow popup, int popupLocation) {
		this.popup = popup;
		this.popupLocation = popupLocation;
	}

	private ArrayList<ReaderAction> itemsOverflow = new ArrayList<>();
	
	public void setButtonAlpha(int alpha) {
		this.buttonAlpha = alpha;
		if (isShown()) {
			requestLayout();
			invalidate();
		}
	}
	
	public void setVertical(boolean vertical) {
		this.isVertical = vertical;
		if (isVertical) {
			//setPadding(BUTTON_SPACING, BUTTON_SPACING, BAR_SPACING, BUTTON_SPACING);
			setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
		} else {
			//setPadding(BUTTON_SPACING, BAR_SPACING, BUTTON_SPACING, BUTTON_SPACING);
			setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		}
	}
	public boolean isVertical() {
		return this.isVertical;
	}

	public CRToolBar(BaseActivity context) {
		super(context);
		this.activity = context;
		this.preferredItemHeight = context.getPreferredItemHeight();
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors((CoolReader) context, isEInk);
	}

	private LinearLayout inflateItem(ReaderAction action) {
		final LinearLayout view = (LinearLayout)inflater.inflate(R.layout.popup_toolbar_item, null);
		ImageView icon = view.findViewById(R.id.action_icon);
		TextView label = view.findViewById(R.id.action_label);
		int colorIcon;
		if (activity instanceof CoolReader) {
			TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
					{R.attr.colorIcon});
			colorIcon = a.getColor(0, Color.GRAY);
			a.recycle();
			label.setTextColor(colorIcon);
		}
		icon.setImageResource(action != null ? action.getIconIdWithDef(activity) : Utils.resolveResourceIdByAttr(activity, R.attr.cr3_button_more_drawable, R.drawable.cr3_button_more));
		//icon.setMinimumHeight(buttonHeight);
		icon.setMinimumWidth(buttonWidth);
		Utils.setContentDescription(icon, activity.getString(action != null ? action.nameId : R.string.btn_toolbar_more));
		label.setText(action != null ? action.nameId : R.string.btn_toolbar_more);
		view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		return view;
	}

	public void createActionsLists(ArrayList<ReaderAction> actions, boolean ignoreSett) {
		if (ignoreSett) {
			this.actions = actions;
			this.actionsToolbar = actions;
			this.actionsMore = actions;
		} else {
			this.actions = new ArrayList<ReaderAction>();
			this.actionsToolbar = new ArrayList<ReaderAction>();
			this.actionsMore = new ArrayList<ReaderAction>();
			ReaderAction[] actions_all = ReaderAction.AVAILABLE_ACTIONS;

			//first priority
			for (ReaderAction a : actions_all)
				if ((a != ReaderAction.NONE) && (a != ReaderAction.EXIT) && (a != ReaderAction.ABOUT)) {
					int aVis = 0;
					try {
						aVis = a.getIsVisibleOnToolbar(((CoolReader) activity).getReaderView());
					} catch (Exception e) {
					}
					if ((aVis == 4) || (aVis == 5) || (aVis == 6)) this.actions.add(a);
					if ((aVis == 4) || (aVis == 6)) this.actionsToolbar.add(a);
					if ((aVis == 5) || (aVis == 6)) this.actionsMore.add(a);
				}
			//second priority
			for (ReaderAction a : actions_all)
				if ((a != ReaderAction.NONE) && (a != ReaderAction.EXIT) && (a != ReaderAction.ABOUT)) {
					int aVis = 0;
					try {
						aVis = a.getIsVisibleOnToolbar(((CoolReader) activity).getReaderView());
					} catch (Exception e) {
					}
					if (!((aVis == 4) || (aVis == 5) || (aVis == 6))) this.actions.add(a);
					if ((aVis == 1) || (aVis == 3)) this.actionsToolbar.add(a);
					if ((aVis == 2) || (aVis == 3)) this.actionsMore.add(a);
				}
			if ((this.actionsToolbar.size() == 0) && (this.actionsMore.size() == 0)) {
				ReaderAction[] ReaderActionDef =
						new ReaderAction[]{
								ReaderAction.GO_BACK,
								ReaderAction.TOC,
								ReaderAction.BOOK_INFO,
								ReaderAction.FONTS_MENU,
								ReaderAction.SEARCH,
								ReaderAction.OPTIONS,
								ReaderAction.BOOKMARKS,
								ReaderAction.FILE_BROWSER_ROOT,
								ReaderAction.TOGGLE_DAY_NIGHT,
								ReaderAction.TOGGLE_SELECTION_MODE,
								ReaderAction.GO_PAGE,
								ReaderAction.GO_PERCENT,
								ReaderAction.FILE_BROWSER,
								ReaderAction.TTS_PLAY,
								ReaderAction.GO_FORWARD,
								ReaderAction.RECENT_BOOKS,
								ReaderAction.OPEN_PREVIOUS_BOOK,
								ReaderAction.TOGGLE_AUTOSCROLL,
								ReaderAction.ABOUT,
								ReaderAction.HIDE
						};

				for (ReaderAction act : ReaderActionDef) {
					this.actionsToolbar.add(act);
					this.actionsMore.add(act);
				}
			}
			;
			if (!this.actions.contains(ReaderAction.ABOUT)) this.actions.add(ReaderAction.ABOUT);
			if (!this.actions.contains(ReaderAction.EXIT)) this.actions.add(ReaderAction.EXIT);
			if (!this.actionsMore.contains(ReaderAction.ABOUT))
				this.actionsMore.add(ReaderAction.ABOUT);
			if (!this.actionsMore.contains(ReaderAction.EXIT))
				this.actionsMore.add(ReaderAction.EXIT);
		}
	}
	
	public CRToolBar(BaseActivity context, ArrayList<ReaderAction> actions, boolean multiline, boolean useActionsMore,
					 boolean ignoreSett, boolean ignoreInv) {
		super(context);
		this.activity = context;
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors((CoolReader) context, isEInk);
		//this.actions = actions;
		createActionsLists(actions, ignoreSett);
		if (useActionsMore) this.actionsToolbar = this.actionsMore;
		this.isMultiline = multiline;
		this.preferredItemHeight = activity.getPreferredItemHeight(); //context.getPreferredItemHeight();
		this.inflater = LayoutInflater.from(activity);
		this.windowDividerHeight = multiline ? 8 : 0;
		this.ignoreInv = ignoreInv;
		context.getWindow().getAttributes();
		if (context.isSmartphone()) {
			BUTTON_SPACING = 3;
			BAR_SPACING = 0; //3;
		} else {
			BUTTON_SPACING = preferredItemHeight / 20;
			BAR_SPACING = 0; //preferredItemHeight / 20;
		}
		calcLayout();
		L.d("CRToolBar preferredItemHeight=" + preferredItemHeight + " buttonWidth=" + buttonWidth + " buttonHeight=" + buttonHeight + " buttonSpacing=" + BUTTON_SPACING);
	}
	
	public void calcLayout() {
		if (activity instanceof CoolReader) {
			//Properties settings = ((CoolReader)activity).getReaderView().getSettings();
			//this.optionAppearance = settings.getInt(ReaderView.PROP_TOOLBAR_APPEARANCE, 0);
			optionAppearance = Integer.parseInt(((CoolReader)activity).getToolbarAppearance());
			toolbarScale = 1.0f;
			grayIcons = false;
			invIcons = false;
			switch (optionAppearance) {
				case Settings.VIEWER_TOOLBAR_100:           // 0
				case Settings.VIEWER_TOOLBAR_100_gray:      // 1
					grayIcons = true;
					break;
				case Settings.VIEWER_TOOLBAR_100_inv:      // 2
					invIcons = true;
					break;
				case Settings.VIEWER_TOOLBAR_75:            // 3
					toolbarScale = 0.75f;
					break;
				case Settings.VIEWER_TOOLBAR_75_gray:       // 4
					toolbarScale = 0.75f;
					grayIcons = true;
					break;
				case Settings.VIEWER_TOOLBAR_75_inv:       // 5
					toolbarScale = 0.75f;
					invIcons = true;
					break;
				case Settings.VIEWER_TOOLBAR_50:            // 6
					toolbarScale = 0.5f;
					break;
				case Settings.VIEWER_TOOLBAR_50_gray:       // 7
					toolbarScale = 0.5f;
					grayIcons = true;
					break;
				case Settings.VIEWER_TOOLBAR_50_inv:       // 8
					toolbarScale = 0.5f;
					invIcons = true;
					break;
			}
			grayIcons = false; // there is no sense in grayIcons since new icons
			if (this.ignoreInv) invIcons = false;
		}
		int sz = (int)((float)preferredItemHeight * toolbarScale); //(activity.isSmartphone() ? preferredItemHeight * 6 / 10 - BUTTON_SPACING : preferredItemHeight);
		buttonWidth = buttonHeight = sz - BUTTON_SPACING;
		if (isMultiline)
			buttonHeight = sz / 2;
		for (int i=0; i<actions.size(); i++) {
			ReaderAction item = actions.get(i);
			int iconId = item.iconId;
			if (iconId == 0) {
				iconId = Utils.resolveResourceIdByAttr(activity, R.attr.cr3_option_other_drawable, R.drawable.cr3_option_other);
			}
			// more
			if (actionsMore.contains(item)) {
				if (!actionsToolbar.contains(item)) {
					visibleNonButtonCount++;
					itemsOverflow.add(item);
					continue;
				}
			}
			if (!actionsToolbar.contains(item))
				continue;
			//if (iconId == 0) {
			//if (0 == 0) {
			//		itemsOverflow.add(item);
			//	visibleNonButtonCount++;
			//	continue;
			//}
			iconActions.add(item);
			Drawable d = activity.getResources().getDrawable(item.getIconIdWithDef(activity));
			visibleButtonCount++;
			int w = d.getIntrinsicWidth();// * dpi / 160;
			int h = d.getIntrinsicHeight();// * dpi / 160;
			if (buttonWidth < w) {
				buttonWidth = w;
			}
			if (buttonHeight < h) {
				buttonHeight = h;
			}
		}
		if (isMultiline) {
			LinearLayout item = inflateItem(iconActions.get(0));
			itemHeight = item.getMeasuredHeight() + BUTTON_SPACING;
		}
	}

	private OnActionHandler onActionHandler;
	
	public void setOnActionHandler(OnActionHandler handler) {
		this.onActionHandler = handler;
	}
	
	private OnOverflowHandler onOverflowHandler;
	
	public void setOnOverflowHandler(OnOverflowHandler handler) {
		this.onOverflowHandler = handler;
	}
	
	public interface OnActionHandler {
		boolean onActionSelected(ReaderAction item);
	}
	public interface OnOverflowHandler {
		boolean onOverflowActions(ArrayList<ReaderAction> actions);
	}
	
	@Override
	protected void onCreateContextMenu(ContextMenu menu) {
		int order = 0;
		for (ReaderAction action : itemsOverflow) {
			menu.add(0, action.menuItemId, order++, action.nameId);
		}
	}
	
	private static boolean allActionsHaveIcon(ArrayList<ReaderAction> list) {
		//for (ReaderAction item : list) {
		//	if (item.iconId == 0)
		//		return false;
		//}
		return true;
	}
	
	public void showOverflowMenu() {
		if (itemsOverflow.size() > 0) {
			if (onOverflowHandler != null)
				onOverflowHandler.onOverflowActions(itemsOverflow);
			else {
				if (!isMultiline && visibleNonButtonCount == 0) {
					showPopup(activity, activity.getContentView(), actionsMore, onActionHandler, onOverflowHandler, actionsMore.size(), Settings.VIEWER_TOOLBAR_TOP);
				} else {
					if (allActionsHaveIcon(itemsOverflow)) {
						if (popup != null)
							popup.dismiss();
						showPopup(activity, activity.getContentView(), itemsOverflow, onActionHandler, onOverflowHandler, actions.size(), isMultiline ? popupLocation : Settings.VIEWER_TOOLBAR_BOTTOM);
					} else
						activity.showActionsPopupMenu(itemsOverflow, onActionHandler);
				}
			}
//			PopupMenu menu = new PopupMenu(activity, this);
//			int order = 0;
//			for (ReaderAction action : itemsOverflow) {
//				menu.getMenu().add(0, action.menuItemId, order++, action.nameId);
//			}
//			menu.show();
//			showContextMenuForChild(overflowButton);
		}
	}

	public void showPopupMenu(final ReaderAction[] actions, final OnActionHandler onActionHandler) {
		if (popup != null)
			popup.dismiss();
		ArrayList<ReaderAction> act = new ArrayList<ReaderAction>();
		for (ReaderAction ra: actions) act.add(ra);
		showPopup(activity, activity.getContentView(), act,
				onActionHandler, onOverflowHandler, act.size(), isMultiline ? popupLocation : Settings.VIEWER_TOOLBAR_BOTTOM);
	}

	public void showPopupMenu(final ReaderAction[] actions, View anchor, View parentAnchor, final OnActionHandler onActionHandler) {
		if (popup != null)
			popup.dismiss();
		ArrayList<ReaderAction> act = new ArrayList<ReaderAction>();
		for (ReaderAction ra: actions) act.add(ra);
		showPopup(activity, anchor == null? activity.getContentView(): anchor,
				parentAnchor == null? activity.getContentView(): parentAnchor, (anchor == null), act,
				onActionHandler, onOverflowHandler, act.size(), isMultiline ? popupLocation : Settings.VIEWER_TOOLBAR_BOTTOM);
	}
//	private void onMoreButtonClick() {
//		showOverflowMenu();
//	}
	
	private void onButtonClick(ReaderAction item) {
		if (item!=null)
			if (onActionHandler != null)
				onActionHandler.onActionSelected(item);
	}

	private Bitmap InverseBitmap(Bitmap src){
		Bitmap dest = Bitmap.createBitmap(
				src.getWidth(), src.getHeight(), src.getConfig());

//		for(int i = 0; i < src.getWidth(); i++){
//			for(int j = 0; j < src.getHeight(); j++){
//
//				dest.setPixel(i, j, Color.argb(
//						Color.alpha(src.getPixel(i, j)),
//						255 - Color.red(src.getPixel(i, j)),
//						255 - Color.green(src.getPixel(i, j)),
//						255 - Color.blue(src.getPixel(i, j))));
//			}
//		}
		int colorIcon;
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorIcon});
		colorIcon = a.getColor(0, Color.GRAY);
		a.recycle();
		boolean isDark = (Color.red(colorIcon)+Color.green(colorIcon)+Color.blue(colorIcon))<(128*3);

		for(int i = 0; i < src.getWidth(); i++){
			for(int j = 0; j < src.getHeight(); j++){
				if (!isDark) {
					dest.setPixel(i, j, Color.argb(
							Color.alpha(src.getPixel(i, j)),
							(Color.red(src.getPixel(i, j)) > 160) ? Color.red(src.getPixel(i, j)) - 160 : 0,
							(Color.green(src.getPixel(i, j)) > 160) ? Color.green(src.getPixel(i, j)) - 160 : 0,
							(Color.blue(src.getPixel(i, j)) > 160) ? Color.blue(src.getPixel(i, j)) - 160 : 0
					));
				} else {
					dest.setPixel(i, j, Color.argb(
							Color.alpha(src.getPixel(i, j)),
							(Color.red(src.getPixel(i, j))+160 < 255) ? Color.red(src.getPixel(i, j)) + 160 : 255,
							(Color.green(src.getPixel(i, j))+160 < 255) ? Color.green(src.getPixel(i, j)) + 160 : 255,
							(Color.blue(src.getPixel(i, j))+160 < 255) ? Color.blue(src.getPixel(i, j)) + 160 : 255
					));
				}
			}
		}

		return dest;
	}

	private void setButtonImageResource(final ReaderAction item, ImageButton ib, int resId) {
		//if (optionAppearance == Settings.VIEWER_TOOLBAR_100) {
		//	ib.setImageResource(resId);
		//	return;
		//}
		Drawable dr = getResources().getDrawable(resId);
		int iWidth = dr.getIntrinsicWidth();
		iWidth = (int) ((float) iWidth * this.toolbarScale);
		int iHeight = dr.getIntrinsicHeight();
		iHeight = (int) ((float) iHeight * this.toolbarScale);
		Bitmap bitmap = Bitmap.createBitmap(iWidth, iHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		dr.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		dr.draw(canvas);
		int icId = 0;
		if (item != null) {
			icId = item.iconId;
			if (item.iconId == 0) {
				Bitmap bmpWithText = Bitmap.createBitmap(iWidth, iHeight, Bitmap.Config.ARGB_8888);
				Canvas c = new Canvas(bmpWithText);
				Paint paint = Utils.createSolidPaint(0xFF000000 | textColor);
				//paint.setColor(0xFF000000);
				paint.setTextAlign(Paint.Align.LEFT);
				int newTextSize = 10;
				float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
						newTextSize, getResources().getDisplayMetrics());
				paint.setTextSize(textSize);
				//paint.setTextSize(25f);
				paint.setSubpixelText(true);
				paint.setAntiAlias(true);
				paint.setFakeBoldText(true);
				paint.setShadowLayer(6f, 0, 0, Color.WHITE);
				paint.setStyle(Paint.Style.FILL);
				paint.setTextAlign(Paint.Align.LEFT);
				Paint paintG = paint;
				if (this.grayIcons) {
					paintG = new Paint();
					ColorMatrix cm = new ColorMatrix();
					cm.setSaturation(0);
					ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
					paintG.setColorFilter(f);
				}
				String sText = activity.getString(item.nameId);
				String sTextR = "";
				if(sText != null){
					for(int i = 0; i < sText.length(); i++){
						if(Character.isWhitespace(sText.charAt(i))){
							sTextR = sTextR + " ";
						} else sTextR = sTextR + sText.charAt(i);
					}
				}
				String res = "";
				int i=0;
				for (String word: sTextR.split(" ")) {
					if (word.length()>0)
						if (!(word.substring(0,1).equals("(")||word.substring(0,1).equals(")"))) {
							res = res + word.substring(0, 1);
							i++;
						}
						//if (i==2) break;
				}
				res = res.toUpperCase();
				//c.drawBitmap(bitmap, 0, 0, paintG);
				c.drawText(res, 0, textSize + ((iHeight-textSize)/2), paint);
				ib.setImageBitmap(bmpWithText);
			}
		}
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.isTintedIcons, R.attr.colorIcon});
		int isTintedIcons = a.getInt(0, 0);
		boolean nghtMode = nightMode && (isTintedIcons == 1);
		a.recycle();
		if ((icId!=0)||(item == null)) {
			if (this.grayIcons) {
				Bitmap bmpGrayscale = Bitmap.createBitmap(iWidth, iHeight, Bitmap.Config.ARGB_8888);
				Canvas c = new Canvas(bmpGrayscale);
				Paint paint = new Paint();
				ColorMatrix cm = new ColorMatrix();
				cm.setSaturation(0);
				ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
				paint.setColorFilter(f);
				c.drawBitmap(bitmap, 0, 0, paint);
				if (nghtMode) ib.setImageBitmap(InverseBitmap(bmpGrayscale));
				else ib.setImageBitmap(bmpGrayscale);
			} else if (this.invIcons) {
				if (nghtMode) ib.setImageBitmap(bitmap);
				else ib.setImageBitmap(InverseBitmap(bitmap));
			} else
				{
					if (nghtMode) ib.setImageBitmap(InverseBitmap(bitmap));
					else ib.setImageBitmap(bitmap);
					tintViewIconsColor(ib);
				}
		}
	}

	public boolean onContextItemSelected(MenuItem item) {

		CoolReader cr = null;
		if (activity!=null)
			if (activity instanceof CoolReader)
				cr = ((CoolReader) activity);

		switch (item.getItemId()) {
			case R.id.options_view_toolbar_position:
				if (cr!=null) {
					log.d("options_view_toolbar_position menu item selected");
					if (activity instanceof CoolReader)
						((CoolReader)activity).optionsFilter = "";
					cr.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_TOOLBAR_TITLE);
				};
				break;
			case R.id.view_toolbar_position_change:
				if (cr!=null) {
					log.d("view_toolbar_position_change menu item selected");
					int toolbarLocation =
							(cr.settings()).getInt(Settings.PROP_TOOLBAR_LOCATION, Settings.VIEWER_TOOLBAR_SHORT_SIDE);
					if (toolbarLocation == Settings.VIEWER_TOOLBAR_LONG_SIDE)
						toolbarLocation = Settings.VIEWER_TOOLBAR_TOP;
					else toolbarLocation++;
					(cr.settings()).setInt(Settings.PROP_TOOLBAR_LOCATION, toolbarLocation);
					cr.setSettings(cr.settings(), 0, true);
					calcLayout();
				};
				break;
			case R.id.options_view_toolbar_hide_in_fullscreen:
				if (cr!=null) {
					log.d("options_view_toolbar_hide_in_fullscreen menu item selected");
					boolean hideToolb =
							(cr.settings()).getBool(Settings.PROP_TOOLBAR_HIDE_IN_FULLSCREEN, false);
					hideToolb = !hideToolb;
					(cr.settings()).setBool(Settings.PROP_TOOLBAR_HIDE_IN_FULLSCREEN, hideToolb);
					cr.setSettings(cr.settings(), 0, true);
					calcLayout();
				};
				break;
			case R.id.options_app_tapzones_normal:
				if (cr!=null) {
					log.d("options_app_tapzones_normal menu item selected");
					if (activity instanceof CoolReader)
						((CoolReader)activity).optionsFilter = "";
					cr.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_APP_TAP_ZONE_ACTIONS_TAP);
				};
				break;
			case R.id.options_app_key_actions:
				if (cr!=null) {
					log.d("options_app_key_actions menu item selected");
					if (activity instanceof CoolReader)
						((CoolReader)activity).optionsFilter = "";
					cr.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_APP_KEY_ACTIONS_PRESS);
				};
				break;
		}
		return true;
	}


	private ImageButton addButton(Rect rect, final ReaderAction item, boolean left) {
		Rect rc = new Rect(rect);
		if (isVertical) {
			if (left) {
				rc.bottom = rc.top + buttonHeight;
				rect.top += buttonHeight + BUTTON_SPACING;
			} else {
				rc.top = rc.bottom - buttonHeight;
				rect.bottom -= buttonHeight + BUTTON_SPACING;
			}
		} else {
			if (left) {
				rc.right = rc.left + buttonWidth;
				rect.left += buttonWidth + BUTTON_SPACING;
			} else {
				rc.left = rc.right - buttonWidth;
				rect.right -= buttonWidth + BUTTON_SPACING;
			}
		}
		if (rc.isEmpty())
			return null;
		ImageButton ib = new ImageButton(getContext());
		ib.setOnLongClickListener(v -> {
			ImageButton ib1 =(ImageButton) v;
			ReaderAction ra = null;
			ReaderAction ram = null;
			if (ib1.getTag()!= null) {
				if (ib1.getTag() instanceof ReaderAction) {
					ra = (ReaderAction) ib1.getTag();
					ram = ra.getMirrorAction();
					if (ram != null) onButtonClick(ram);
				}
			} else {
				if (activity!=null)
					if (activity instanceof CoolReader) {
						final CoolReader cr = ((CoolReader) activity);
						cr.registerForContextMenu(cr.contentView);
						cr.contentView.setOnCreateContextMenuListener((menu, v1, menuInfo) -> {
							menu.clear();
							MenuInflater inflater = cr.getMenuInflater();
							inflater.inflate(R.menu.cr3_reader_toolbar_context_menu, menu);
							menu.setHeaderTitle(cr.getString(R.string.options_view_toolbar_settings));
							for ( int i=0; i<menu.size(); i++ ) {
								if (menu.getItem(i).getItemId()==R.id.options_view_toolbar_hide_in_fullscreen) {
									boolean hideToolb =
											(cr.settings()).getBool(Settings.PROP_TOOLBAR_HIDE_IN_FULLSCREEN, false);
									menu.getItem(i).setCheckable(true);
									menu.getItem(i).setChecked(hideToolb);
								}
								menu.getItem(i).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
									public boolean onMenuItemClick(MenuItem item1) {
										onContextItemSelected(item1);
										return true;
									}
								});
							}

						});
						cr.contentView.showContextMenu();
					}
			}
			return true;
		});

		if (item != null) {
			if (item.getIconIdWithDef(activity)!=0)	setButtonImageResource(item, ib,item.getIconIdWithDef(activity));
			Utils.setContentDescription(ib, getContext().getString(item.nameId));
			ib.setTag(item);
		} else {
			setButtonImageResource(item, ib,Utils.resolveResourceIdByAttr(activity, R.attr.cr3_button_more_drawable, R.drawable.cr3_button_more));
			Utils.setContentDescription(ib, getContext().getString(R.string.btn_toolbar_more));
		}
		TypedArray a = activity.getTheme().obtainStyledAttributes( new int[] { R.attr.cr3_toolbar_button_background_drawable } );
		int cr3_toolbar_button_background = a.getResourceId(0, 0);
		a.recycle();
		if (0 == cr3_toolbar_button_background)
			cr3_toolbar_button_background = R.drawable.cr3_toolbar_button_background;
		ib.setBackgroundResource(cr3_toolbar_button_background);
		ib.layout(rc.left, rc.top, rc.right, rc.bottom);
		if (item == null)
			overflowButton = ib;
		ib.setOnClickListener(v -> {
			if (item != null)
				onButtonClick(item);
			else
				showOverflowMenu();
		});
		ib.setAlpha(nightMode ? 0x60 : buttonAlpha);
		addView(ib);
		return ib;
	}
	
	private Rect layoutRect = new Rect();
	private Rect layoutLineRect = new Rect();
	private Rect layoutItemRect = new Rect();
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		right -= left;
		bottom -= top;
		left = top = 0;
		//calcLayout();
		removeAllViews();
		overflowButton = null;
		
		if (isMultiline) {
			itemsOverflow.clear();
			int lastButtonIndex = -1;
			
        	int lineCount = calcLineCount(right);
        	int btnCount = iconActions.size() + (visibleNonButtonCount > 0 ? 1 : 0);
        	int buttonsPerLine = (btnCount + lineCount - 1) / lineCount;

        	int y0 = 0;
        	//plotn - let it be always
        	//if (popupLocation == Settings.VIEWER_TOOLBAR_BOTTOM) {
			View separator = new View(activity);
			//separator.setBackgroundResource(activity.getCurrentTheme().getBrowserStatusBackground());
			separator.setBackgroundColor(themeColors.get(R.attr.colorIcon));
			addView(separator);
			separator.layout(left, top, right, top + windowDividerHeight);
			View separator2 = new View(activity);
			separator2.setBackgroundResource(activity.getCurrentTheme().getBrowserStatusBackground());
			addView(separator2);
			separator2.layout(left, top + windowDividerHeight, right, top + (2 * windowDividerHeight));
			y0 = (2 *windowDividerHeight) + 4;
			//}
        	
        	
//        	ScrollView scroll = new ScrollView(activity);
//        	scroll.setLayoutParams(new LayoutParams(right, bottom));
//        	AbsoluteLayout content = new AbsoluteLayout(activity);
        	
        	layoutRect.set(left + getPaddingLeft() + BUTTON_SPACING, top + getPaddingTop() + BUTTON_SPACING, right - getPaddingRight() - BUTTON_SPACING, bottom - getPaddingBottom() - BUTTON_SPACING - y0);
    		int lineH = itemHeight; //rect.height() / lineCount;
    		int spacing = 0;
    		int maxLines = bottom / lineH;
    		if (maxLines > maxMultilineLines)
    			maxLines = maxMultilineLines;
        	for (int currentLine = 0; currentLine < lineCount && currentLine < maxLines; currentLine++) {
        		int startBtn = currentLine * buttonsPerLine;
        		int endBtn = (currentLine + 1) * buttonsPerLine;
        		if (endBtn > btnCount)
        			endBtn = btnCount;
        		int currentLineButtons = endBtn - startBtn;
        		layoutLineRect.set(layoutRect);
        		layoutLineRect.top += currentLine * lineH + spacing + y0;
        		layoutLineRect.bottom = layoutLineRect.top + lineH - spacing;
        		int itemWidth = layoutLineRect.width() / currentLineButtons;
        		for (int i = 0; i < currentLineButtons; i++) {
        			layoutItemRect.set(layoutLineRect);
        			layoutItemRect.left += i * itemWidth + spacing;
        			layoutItemRect.right = layoutItemRect.left + itemWidth - spacing;
        			final ReaderAction action = (visibleNonButtonCount > 0 && i + startBtn == iconActions.size()) ||
							(lineCount > maxLines && currentLine == maxLines - 1 && i == currentLineButtons - 1) ? null :
							iconActions.get(startBtn + i);
					//log.v("action = "+action);
					if (action != null)
        				lastButtonIndex = startBtn + i;
        			//log.v("item=" + layoutItemRect);
        			final LinearLayout item = inflateItem(action);
        			//item.setLayoutParams(new LinearLayout.LayoutParams(itemRect.width(), itemRect.height()));
        			item.measure(MeasureSpec.makeMeasureSpec(layoutItemRect.width(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(layoutItemRect.height(), MeasureSpec.EXACTLY));
        			item.layout(layoutItemRect.left, layoutItemRect.top, layoutItemRect.right, layoutItemRect.bottom);
        			//item.forceLayout();
        			addView(item);
        			// this is overflow panel
        			item.setOnClickListener(v -> {
					if (action != null)
						onButtonClick(action);
					else
						showOverflowMenu();
					});
					activity.tintViewIcons(item);
					item.setOnLongClickListener(v -> {
						ReaderAction ram = action.getMirrorAction();
						if (ram != null) onButtonClick(ram);
						return true;
					});
        		}
//        		addView(scroll);
        	}
        	//if (popupLocation != Settings.VIEWER_TOOLBAR_BOTTOM) {
			View separator3 = new View(activity);
			separator3.setBackgroundResource(activity.getCurrentTheme().getBrowserStatusBackground());
			addView(separator3);
			separator3.layout(left, bottom - (2 * windowDividerHeight), right, bottom);
			View separator4 = new View(activity);
			separator4.setBackgroundColor(themeColors.get(R.attr.colorIcon));
			addView(separator4);
			separator4.layout(left, bottom - windowDividerHeight, right, bottom);
        	//}
    		if (lastButtonIndex > 0)
    			for (int i=lastButtonIndex + 1; i < actions.size(); i++)
    				if (actionsMore.contains(actions.get(i)))
    					itemsOverflow.add(actions.get(i));
        	return;
		}

//		View divider = new View(getContext());
//		addView(divider);
//		if (isVertical()) {
//			divider.setBackgroundResource(R.drawable.divider_light_vertical_tiled);
//			divider.layout(right - 8, top, right, bottom);
//		} else {
//			divider.setBackgroundResource(R.drawable.divider_light_tiled);
//			divider.layout(left, bottom - 8, right, bottom);
//		}

		visibleButtonCount = 0;
		for (int i=0; i<actions.size(); i++) {
			//if (actions.get(i).iconId != 0)
			if (actionsToolbar.contains(actions.get(i)))
				visibleButtonCount++;
		}
		
		
		Rect rect = new Rect(left + getPaddingLeft() + BUTTON_SPACING, top + getPaddingTop() + BUTTON_SPACING, right - getPaddingRight() - BUTTON_SPACING, bottom - getPaddingBottom() - BUTTON_SPACING);
		if (rect.isEmpty())
			return;
		ArrayList<ReaderAction> itemsToShow = new ArrayList<ReaderAction>();
		itemsOverflow.clear();
		int maxButtonCount = 1;
		if (isVertical) {
			rect.right -= BAR_SPACING;
			int maxHeight = bottom - top - getPaddingTop() - getPaddingBottom() + BUTTON_SPACING;
			maxButtonCount = maxHeight / (buttonHeight + BUTTON_SPACING);
		} else {
			rect.bottom -= BAR_SPACING;
			int maxWidth = right - left - getPaddingLeft() - getPaddingRight() + BUTTON_SPACING;
			maxButtonCount = maxWidth / (buttonWidth + BUTTON_SPACING);
		}
		int count = 0;
		boolean addEllipsis = visibleButtonCount > maxButtonCount || visibleNonButtonCount > 0;
		if (addEllipsis) {
			addButton(rect, null, false);
			maxButtonCount--;
		}
		for (int i = 0; i < actions.size(); i++) {
			ReaderAction item = actions.get(i);
			if (count >= maxButtonCount) {
				if (actionsMore.contains(item))
					itemsOverflow.add(item);
				continue;
			}
			if (actionsMore.contains(item)) {
				itemsOverflow.add(item);
			}
				//if (item.iconId == 0) {
			//	itemsOverflow.add(item);
			//	continue;
			//}
			if (actionsToolbar.contains(item)) {
				itemsToShow.add(item);
				count++;
				addButton(rect, item, true);
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
//        if (widthMode != MeasureSpec.EXACTLY) {
//            throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
//                    "with android:layout_width=\"match_parent\" (or fill_parent)");
//        }
        
//        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
//        if (heightMode != MeasureSpec.AT_MOST) {
//            throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
//                    "with android:layout_height=\"wrap_content\"");
//        }

        if (isVertical) {
	        int contentHeight = MeasureSpec.getSize(heightMeasureSpec);
	        int maxWidth = buttonWidth + BUTTON_SPACING + BUTTON_SPACING + BAR_SPACING + getPaddingLeft() + getPaddingRight();
	        setMeasuredDimension(maxWidth, contentHeight + (windowDividerHeight * 4));
        } else {
	        int contentWidth = MeasureSpec.getSize(widthMeasureSpec);
	        if (isMultiline) {
		        int contentHeight = MeasureSpec.getSize(heightMeasureSpec);
	        	int lineCount = calcLineCount(contentWidth);
	        	if (lineCount > maxMultilineLines)
	        		lineCount = maxMultilineLines;
	        	int h = lineCount * itemHeight + BAR_SPACING + BAR_SPACING + (windowDividerHeight *4) + 4;
//	        	if (h > contentHeight - itemHeight)
//	        		h = contentHeight - itemHeight;
	        	setMeasuredDimension(contentWidth, h);
	        } else {
	        	setMeasuredDimension(contentWidth, buttonHeight + BUTTON_SPACING * 2 + BAR_SPACING + (windowDividerHeight *4));
	        }
        }
	}
	
	protected int calcLineCount(int contentWidth) {
		if (!isMultiline)
			return 1;
    	int lineCount = 1;
    	int btnCount = iconActions.size() + (visibleNonButtonCount > 0 ? 1 : 0);
    	int minLineItemCount = 3;
    	int maxLineItemCount = contentWidth / (preferredItemHeight * 3 / 2);
    	if (maxLineItemCount < minLineItemCount)
    		maxLineItemCount = minLineItemCount;

    	for (;;) {
	    	if (btnCount <= maxLineItemCount * lineCount)
	    		return lineCount;
	    	lineCount++;
    	}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		log.v("CRToolBar.onSizeChanged(" + w + ", " + h + ")");
	}
	
	

	@Override
	protected void onDraw(Canvas canvas) {
		log.v("CRToolBar.onDraw(" + getWidth() + ", " + getHeight() + ")");
		super.onDraw(canvas);
	}
	public PopupWindow showAsPopup(View anchor, OnActionHandler onActionHandler, OnOverflowHandler onOverflowHandler) {
		return showPopup(activity, anchor, actionsMore, onActionHandler, onOverflowHandler, 3, Settings.VIEWER_TOOLBAR_BOTTOM);
	}
	
	private void setMaxLines(int maxLines) {
		this.maxMultilineLines = maxLines;
	}

	public static PopupWindow showPopup(BaseActivity context, View anchor, ArrayList<ReaderAction> actions, final OnActionHandler onActionHandler, final OnOverflowHandler onOverflowHandler, int maxLines, int popupLocationDummy) {
		return showPopup(context, anchor, anchor, true, actions, onActionHandler, onOverflowHandler, maxLines, popupLocationDummy);
	}

	public static PopupWindow showPopup(BaseActivity context, View anchor, View parentAnchor, boolean isRootAnchor,
										ArrayList<ReaderAction> actions, final OnActionHandler onActionHandler,
										final OnOverflowHandler onOverflowHandler, int maxLines, int popupLocationDummy) {
	    int popupLocation = Settings.VIEWER_TOOLBAR_BOTTOM; // plotn - костыль, а то когда панель сверху, то половина ее закрашивается светлее
		final ScrollView scroll = new ScrollView(context);
		final CRToolBar tb = new CRToolBar(context, actions, true, true, true, false);
		tb.setMaxLines(maxLines);
		tb.setOnActionHandler(onActionHandler);
		tb.setVertical(false);
		tb.measure(MeasureSpec.makeMeasureSpec(parentAnchor.getWidth(), MeasureSpec.EXACTLY), ViewGroup.LayoutParams.WRAP_CONTENT);
		int w = tb.getMeasuredWidth();
		int h = tb.getMeasuredHeight();
		scroll.addView(tb);
		scroll.setLayoutParams(new LayoutParams(w, h/2));
		scroll.setVerticalFadingEdgeEnabled(true);
		scroll.setFadingEdgeLength(h / 10);
		final PopupWindow popup = new PopupWindow(context);
		tb.setPopup(popup, popupLocation);
		ReaderAction longMenuAction = null;
		for (ReaderAction action : actions) {
			if (action.activateWithLongMenuKey())
				longMenuAction = action;
		}
		final ReaderAction foundLongMenuAction = longMenuAction;
		
		popup.setTouchInterceptor((v, event) -> {
			if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
				popup.dismiss();
				return true;
			}
			return false;
		});
		tb.setOnActionHandler(item -> {
			popup.dismiss();
			if (onActionHandler==null) log.v("EMPTY!!!");
			else
				return onActionHandler.onActionSelected(item);
			return false;
		});
		if (onOverflowHandler != null)
			tb.setOnOverflowHandler(actions1 -> {
				popup.dismiss();
				return onOverflowHandler.onOverflowActions(actions1);
			});
		// close on menu or back keys
		tb.setFocusable(true);
		tb.setFocusableInTouchMode(true);
		tb.setOnKeyListener((view, keyCode, event) -> {
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_BACK) {
					//popup.dismiss();
					return true;
				}
			} else if (event.getAction() == KeyEvent.ACTION_UP) {
				if (keyCode == KeyEvent.KEYCODE_MENU && foundLongMenuAction != null && event.getDownTime() >= 500) {
					popup.dismiss();
					return onActionHandler.onActionSelected(foundLongMenuAction);
				}
				if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_BACK) {
					popup.dismiss();
					return true;
				}
			}
			return false;
		});
		//popup.setBackgroundDrawable(new BitmapDrawable());
		popup.setWidth(WindowManager.LayoutParams.FILL_PARENT);
		int hh = h;
		if (isRootAnchor) {
			int maxh = parentAnchor.getHeight();
			if (hh > maxh - context.getPreferredItemHeight())
				hh = maxh - context.getPreferredItemHeight() * 3 / 2;
		}
		popup.setHeight(hh);
		popup.setFocusable(true);
		popup.setFocusable(true);
		popup.setTouchable(true);
		popup.setOutsideTouchable(true);
		popup.setContentView(scroll);
		InterfaceTheme theme = context.getCurrentTheme();
		Drawable bg;
		if (theme.getPopupToolbarBackground() != 0)
			bg = context.getResources().getDrawable(theme.getPopupToolbarBackground());
		else
			bg = Utils.solidColorDrawable(theme.getPopupToolbarBackgroundColor());
		popup.setBackgroundDrawable(bg);
		int [] location = new int[2];
		anchor.getLocationOnScreen(location);
		if (!isRootAnchor) {
			int popupY = location[1] + anchor.getHeight();
			popup.showAtLocation(anchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, popupY);
		} else {
			if (popupLocation == Settings.VIEWER_TOOLBAR_BOTTOM)
				popup.showAtLocation(anchor, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL, 0, 0); //location[0], popupY - anchor.getHeight());
			else
				popup.showAtLocation(anchor, Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0); //, location[0], popupY);
		}
		tb.tintViewIconsColor(tb);
		//Utils.drawFrame2(popup, new Rect(0,0,popup.getWidth(), popup.getHeight()), Utils.createSolidPaint(0xC0000000), 4);
		return popup;
	}
	
	private boolean nightMode;
	public void updateNightMode(boolean nightMode) {
		if (this.nightMode != nightMode) {
			this.nightMode = nightMode;
			if (isShown()) {
				requestLayout();
				invalidate();
			}
		}
	}
	
	public void onThemeChanged(InterfaceTheme theme) {
		//buttonAlpha = theme.getToolbarButtonAlpha();
		//textColor = theme.getStatusTextColor();
		if (isShown()) {
			requestLayout();
			invalidate();
		}
	}
}
