package org.coolreader.crengine;

import org.coolreader.CoolReader;
import org.coolreader.R;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

public class OPDSCatalogEditDialog extends BaseDialog {

	private final CoolReader mActivity;
	private final LayoutInflater mInflater;
	private final FileInfo mItem;
	private final EditText nameEdit;
	private final EditText urlEdit;
	private final EditText usernameEdit;
	private final EditText passwordEdit;
	private final EditText proxyaddrEdit;
	private final EditText proxyportEdit;
	private final EditText proxyunameEdit;
	private final EditText proxypasswEdit;
	private final CheckBox onionDefProxyChb;
	private final Runnable mOnUpdate;

	public OPDSCatalogEditDialog(CoolReader activity, FileInfo item, Runnable onUpdate) {
		super("OPDSCatalogEditDialog", activity, activity.getString((item.id == null) ? R.string.dlg_catalog_add_title
				: R.string.dlg_catalog_edit_title), true,
				false);
		mActivity = activity;
		mItem = item;
		mOnUpdate = onUpdate;
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.catalog_edit_dialog, null);
		nameEdit = (EditText) view.findViewById(R.id.catalog_name);
		urlEdit = (EditText) view.findViewById(R.id.catalog_url);
		usernameEdit = (EditText) view.findViewById(R.id.catalog_username);
		passwordEdit = (EditText) view.findViewById(R.id.catalog_password);
		proxyaddrEdit = (EditText) view.findViewById(R.id.edt_proxy_addr);
		proxyportEdit = (EditText) view.findViewById(R.id.edt_proxy_port);
		proxyunameEdit = (EditText) view.findViewById(R.id.edt_proxy_uname);
		proxypasswEdit = (EditText) view.findViewById(R.id.edt_proxy_passw);
		onionDefProxyChb = (CheckBox) view.findViewById(R.id.chb_onion_def_proxy);
		nameEdit.setText(mItem.filename);
		urlEdit.setText(mItem.getOPDSUrl());
		usernameEdit.setText(mItem.username);
		passwordEdit.setText(mItem.password);
		proxyaddrEdit.setText(mItem.proxy_addr);
		proxyportEdit.setText(mItem.proxy_port);
		proxyunameEdit.setText(mItem.proxy_uname);
		proxypasswEdit.setText(mItem.proxy_passw);
		onionDefProxyChb.setChecked(mItem.onion_def_proxy==1);
		setThirdButtonImage(
				Utils.resolveResourceIdByAttr(activity, R.attr.attr_icons8_minus, R.drawable.icons8_minus),
				//R.drawable.icons8_minus,
				R.string.mi_catalog_delete);
		setView(view);
	}

	@Override
	protected void onPositiveButtonClick() {
		String url = urlEdit.getText().toString();
		boolean blacklist = checkBlackList(url);
		if (OPDSConst.BLACK_LIST_MODE == OPDSConst.BLACK_LIST_MODE_FORCE) {
			mActivity.showToast(R.string.black_list_enforced);
		} else if (OPDSConst.BLACK_LIST_MODE == OPDSConst.BLACK_LIST_MODE_WARN) {
			mActivity.askConfirmation(R.string.black_list_warning, new Runnable() {
				@Override
				public void run() {
					save();
					OPDSCatalogEditDialog.super.onPositiveButtonClick();
				}
				
			}, new Runnable() {
				@Override
				public void run() {
					onNegativeButtonClick();
				}
			});
		} else {
			save();
			super.onPositiveButtonClick();
		}
	}
	
	private boolean checkBlackList(String url) {
		for (String s : OPDSConst.BLACK_LIST) {
			if (s.equals(url))
				return true;
		}
		return false;
	}
	
	private void save() {
		activity.getDB().saveOPDSCatalog(mItem.id,
				urlEdit.getText().toString(), nameEdit.getText().toString(), 
				usernameEdit.getText().toString(), passwordEdit.getText().toString(),
				proxyaddrEdit.getText().toString(), proxyportEdit.getText().toString(),
				proxyunameEdit.getText().toString(), proxypasswEdit.getText().toString(),
				onionDefProxyChb.isChecked()?1:0);
		mOnUpdate.run();
		super.onPositiveButtonClick();
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
	}

	@Override
	protected void onThirdButtonClick() {
		mActivity.askDeleteCatalog(mItem);
		super.onThirdButtonClick();
	}

	
}
