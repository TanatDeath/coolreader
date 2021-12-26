package org.coolreader.options;

import android.view.LayoutInflater;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Properties;
import org.coolreader.crengine.Settings;

public class FallbackFontsOptions extends SubmenuOption {

	OptionsListView mListView;
	private final Properties mFallbackProps = new Properties();

	final BaseActivity mActivity;
	final OptionsDialog mOptionsDialog;

	private final OptionOwner mFallbackOptionItemOwner = new OptionOwner() {
		@Override
		public BaseActivity getActivity() {
			return  mOwner.getActivity();
		}
		@Override
		public Properties getProperties() {
			return mFallbackProps;
		}
		@Override
		public LayoutInflater getInflater() {
			return mOwner.getInflater();
		}
	};

	public FallbackFontsOptions(BaseActivity activity, OptionsDialog od, OptionOwner owner, String label, String addInfo, String filter) {
		super( owner, label, Settings.PROP_FALLBACK_FONT_FACES, addInfo, filter );
		mListView = new OptionsListView(od.getContext(), this);
		mActivity = activity;
		mOptionsDialog = od;
	}

	protected void addFallbackFontItem(int pos, String fontFace) {
		String propName = Integer.toString(pos);
		String label = mOptionsDialog.getString(R.string.options_font_fallback_face_num, pos + 1);
		mFallbackProps.setProperty(propName, fontFace);
		FallbackFontItemOptions option = new FallbackFontItemOptions(mFallbackOptionItemOwner, label, propName, this, pos,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue);
		option.add("", "(empty)", "");
		option.add(FontSelectOption.mFontFaces);
		option.setDefaultValue("");
		option.noIcon();
		mListView.add(option);
	}

	protected boolean removeFallbackFontItem(int pos) {
		boolean res = mListView.remove(pos);
		if (res)
			mListView.refresh();
		return res;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("dlgFallbackFontsOptions", mActivity, label, false, false) {
			protected void onClose()
			{
				updateMainPropertyValue();
				this.setView(null);
				super.onClose();
			}
		};
		mListView.clear();
		String fallbackFaces = mOptionsDialog.getProperties().getProperty(property);
		if (null == fallbackFaces)
			fallbackFaces = "";
		String[] list = fallbackFaces.split(";");
		int pos = 0;
		for (String face : list) {
			face = face.trim();
			if (face.length() > 0) {
				addFallbackFontItem(pos, face);
				pos++;
			}
		}
		addFallbackFontItem(pos, "");
		dlg.setView(mListView);
		dlg.show();
	}

	public int size() {
		return mListView.size();
	}

	public String getValueLabel() { return ">"; }

	private void updateMainPropertyValue() {
		StringBuilder fallbackFacesBuilder = new StringBuilder();
		for (int i = 0; i < OptionsDialog.MAX_FALLBACK_FONTS_COUNT; i++) {
			String propName = Integer.toString(i);
			String fallback = mFallbackProps.getProperty(propName);
			if (null != fallback && fallback.length() > 0) {
				fallbackFacesBuilder.append(fallback);
				fallbackFacesBuilder.append("; ");
			}
		}
		String fallbackFaces = fallbackFacesBuilder.toString();
		// Remove trailing "; " part
		if (fallbackFaces.length() >= 2)
			fallbackFaces = fallbackFaces.substring(0, fallbackFaces.length() - 2);
		mOptionsDialog.getProperties().setProperty(property, fallbackFaces);
	}

	public boolean updateFilterEnd() {
		for (String s: FontSelectOption.mFontFaces) this.updateFilteredMark(s);
		String fallbackFaces = mOptionsDialog.getProperties().getProperty(property);
		if (null == fallbackFaces)
			fallbackFaces = "";
		String[] list = fallbackFaces.split(";");
		int pos = 0;
		for (String face : list) {
			face = face.trim();
			if (face.length() > 0) {
				this.updateFilteredMark(face);
				pos++;
			}
		}
		return this.lastFiltered;
	}
}
