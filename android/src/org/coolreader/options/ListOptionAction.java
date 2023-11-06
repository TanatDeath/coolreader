package org.coolreader.options;

import org.coolreader.crengine.OptionOwner;

public class ListOptionAction extends ListOption {
	public ListOptionAction(OptionOwner owner, String label, String property, String addInfo, String filter) {
		super(owner, label, property, addInfo, filter);
	}

	@Override
	protected String updateValue(String value) {
		return OptionsDialog.updDicValue(value, mProperties, mActivity, false);
	}
}

