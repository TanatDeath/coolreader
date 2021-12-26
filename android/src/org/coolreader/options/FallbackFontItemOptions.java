package org.coolreader.options;

import org.coolreader.crengine.OptionOwner;

public class FallbackFontItemOptions extends ListOption {

	private final FallbackFontsOptions mParentOptions;	// parent option item
	private final int mPosition;						// position in parent list

	public FallbackFontItemOptions(OptionOwner owner, String label, String property, FallbackFontsOptions parentOptions, int pos,
								   String addInfo, String filter) {
		super(owner, label, property, addInfo, filter);
		mParentOptions = parentOptions;
		mPosition = pos;
	}

	public void onClick( OptionsDialog.Three item ) {
		super.onClick(item);
		if (item.value.length() > 0) {
			if (mPosition == mParentOptions.size() - 1) {
				if (mPosition < OptionsDialog.MAX_FALLBACK_FONTS_COUNT - 1) {
					// last item in parent list not empty => add new empty item in parent list
					mParentOptions.addFallbackFontItem(mParentOptions.size(), "");
				}
			}
		} else {
			if (mPosition == mParentOptions.size() - 2) {
				// penultimate item in parent list set to empty => remove last empty item
				mParentOptions.removeFallbackFontItem(mParentOptions.size() - 1);
			}
		}
	}
}
