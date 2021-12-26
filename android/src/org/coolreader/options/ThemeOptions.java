package org.coolreader.options;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.InterfaceTheme;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

public class ThemeOptions extends ListOption
{
	public ThemeOptions(BaseActivity activity, OptionOwner owner, String label, String addInfo, String filter )
	{
		super( owner, label, Settings.PROP_APP_THEME, addInfo, filter );
		setDefaultValue(DeviceInfo.isForceHCTheme(BaseActivity.getScreenForceEink()) ? "WHITE" : "GRAY1");
		for (InterfaceTheme theme : InterfaceTheme.allThemes)
			add(theme.getCode(), activity.getString(theme.getDisplayNameResourceId()), activity.getString(R.string.option_add_info_empty_text));
	}
}

