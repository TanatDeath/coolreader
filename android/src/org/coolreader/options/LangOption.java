package org.coolreader.options;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

import java.util.Locale;

public class LangOption extends ListOption {

	final BaseActivity mActivity;

	public LangOption(BaseActivity activity, OptionOwner owner, String filter) {
		super(owner, activity.getString(R.string.options_app_locale), Settings.PROP_APP_LOCALE, activity.getString(R.string.options_app_locale_add_info), filter);
		mActivity = activity;
		for (Settings.Lang lang : Settings.Lang.values()) {
			Locale l =  lang.getLocale();
			String s = "";
			if (l!=null) s = lang.getLocale().getDisplayName();
			add(lang.code, mActivity.getString(lang.nameId), s);
		}
		if (mProperties.getProperty(property) == null)
			mProperties.setProperty(property, Settings.Lang.DEFAULT.code);
		this.updateFilteredMark(Settings.Lang.DEFAULT.code);
	}
}
