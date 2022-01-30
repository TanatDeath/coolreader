package org.coolreader.options;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.utils.StrUtils;
import org.coolreader.dic.Dictionaries;

import java.util.List;

public class DicListOption extends ListOption
{
	public DicListOption(OptionOwner owner, String label, String prop, String addInfo, String filter )
	{
		super(owner, label, prop, addInfo, filter);
		List<Dictionaries.DictInfo> dicts = Dictionaries.getDictList(mActivity);
		setDefaultValue(dicts.get(0).id);
		for (Dictionaries.DictInfo dict : dicts) {
			boolean installed = mActivity.isPackageInstalled(dict.packageName) || StrUtils.isEmptyStr(dict.packageName);
			String sAdd = mActivity.getString(R.string.options_app_dictionary_not_installed);
			String sAdd2 = dict.getAddText((CoolReader) mActivity);
			if (!StrUtils.isEmptyStr(sAdd2)) sAdd2 = ": " + sAdd2;
			if (StrUtils.isEmptyStr(dict.packageName)) sAdd = "";
			if (((dict.internal==1)||(dict.internal==6)) &&
					(dict.packageName.equals("com.socialnmobile.colordict")) && (!installed)) {
				installed = mActivity.isPackageInstalled("mobi.goldendict.android")||
						mActivity.isPackageInstalled("mobi.goldendict.androie")
						|| StrUtils.isEmptyStr(dict.packageName); // changed package name - 4pda version 2.0.1b7
				String sMinicard="";
				if (dict.internal==6) sMinicard=" (minicard)";
				String sInfo = "";
				if (!((StrUtils.isEmptyStr(dict.packageName))&&(StrUtils.isEmptyStr(dict.className))))
					sInfo = "Package: " + dict.packageName + "; \nclass: " + dict.className;
				else sInfo = "Link: " + dict.httpLink;
				add(dict.id, (installed ? "GoldenDict" + sMinicard: dict.name + sAdd2 + " " + sAdd),
						sInfo);
			} else {
				String sInfo = "";
				if (!((StrUtils.isEmptyStr(dict.packageName))&&(StrUtils.isEmptyStr(dict.className))))
					sInfo = "Package: " + dict.packageName + "; \nclass: " + dict.className;
				else sInfo = "Link: " + dict.httpLink;
				add(dict.id, dict.name + sAdd2 + (installed ? "" : " " + sAdd),
						sInfo);
			}
		}
	}

	protected int getItemLayoutId() {
		return R.layout.option_value;
	}

	protected void updateItemContents(final View layout, final OptionsDialog.Three item, final ListView listView, final int position ) {
		super.updateItemContents(layout, item, listView, position);
		ImageView img = (ImageView) layout.findViewById(R.id.option_value_icon);
		TextView tv = (TextView) layout.findViewById(R.id.option_value_text);
		List<Dictionaries.DictInfo> dicts = Dictionaries.getDictList(mActivity);
		for (Dictionaries.DictInfo dict : dicts) {
			if (item.value.equals(dict.id)) {
				if (tv != null) {
					if (dict.isOnline) tv.setTextColor(themeColors.get(R.attr.colorThemeGreen));
					if (dict.internal == 4) tv.setTextColor(themeColors.get(R.attr.colorThemeBlue));
					else if ((!dict.isOnline) && (dict.internal != 4)) tv.setTextColor(themeColors.get(R.attr.colorIcon));
				}
				if (dict.dicIcon !=0)
					img.setImageDrawable(mActivity.getResources().getDrawable(dict.dicIcon));
				else
					img.setImageDrawable(null);
				if (dict.icon != null) {
					Drawable fakeIcon = mActivity.getResources().getDrawable(R.drawable.lingvo);
					final Bitmap bmp = Bitmap.createBitmap(dict.icon.getIntrinsicWidth(), dict.icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
					final Canvas canvas = new Canvas(bmp);
					dict.icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
					dict.icon.draw(canvas);
					Bitmap resizedBitmap = Bitmap.createScaledBitmap(
							bmp, fakeIcon.getIntrinsicWidth(), fakeIcon.getIntrinsicHeight(), false);
					img.setImageBitmap(resizedBitmap);
				}
			}
		}
	}
}
