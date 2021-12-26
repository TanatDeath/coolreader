package org.coolreader.options;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

public class HyphenationOptions extends ListOption
{
	final BaseActivity mActivity;

	public HyphenationOptions(BaseActivity activity, OptionOwner owner, String label, String addInfo, String filter )
	{
		super(owner, label, Settings.PROP_HYPHENATION_DICT, addInfo, filter);
		mActivity = activity;
		setDefaultValue(Engine.HyphDict.RUSSIAN.code);
		Engine.HyphDict[] dicts = Engine.HyphDict.values();
		for ( Engine.HyphDict dict : dicts ) {
			String ainfo = mActivity.getString(R.string.option_add_info_empty_text);
			if (dict.file!=null) ainfo = "Language: " + dict.language + "; file: " +
					dict.file;
			if (!dict.hide)
				add(dict.toString(), dict.getName(), ainfo);
		};
	}
}
