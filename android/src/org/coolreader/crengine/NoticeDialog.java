package org.coolreader.crengine;

import org.coolreader.CoolReader;
import org.coolreader.R;

import android.app.Dialog;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.HashMap;

public class NoticeDialog extends Dialog {

	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;

	public NoticeDialog(BaseActivity activity, String sText, final Runnable onOkButton) {
		super(activity, activity.getCurrentTheme().getThemeId());
		init(activity, sText, R.string.dlg_button_ok, onOkButton, R.string.dlg_button_cancel, null);
	}

	public NoticeDialog(BaseActivity activity, String sText, final Runnable onOkButton, final Runnable onCancelButton) {
		super(activity, activity.getCurrentTheme().getThemeId());
		init(activity, sText, R.string.dlg_button_ok, onOkButton, R.string.dlg_button_cancel, onCancelButton);
	}

	public NoticeDialog(BaseActivity activity, String sText, int button1TextRes, final Runnable button1Runnable, int button2TextRes, final Runnable button2Runnable,
			final Runnable dismissRunnable) {
		super(activity, activity.getCurrentTheme().getThemeId());
		init(activity, sText, button1TextRes, button1Runnable, button2TextRes, button2Runnable);
	}

	private void init(BaseActivity activity, String sText, int button1TextRes, final Runnable button1Runnable, int button2TextRes, final Runnable button2Runnable) {
		setOwnerActivity(activity);
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors((CoolReader) activity, isEInk);
		LayoutInflater mInflater = LayoutInflater.from(getContext());
		ViewGroup layout = (ViewGroup) mInflater.inflate(R.layout.notice_dialog, null);
		setTitle(R.string.app_name);
		Button button1 = layout.findViewById(R.id.notice_dlg_btn_positive);
		button1.setText(button1TextRes);
		button1.setOnClickListener(v -> {
			if (button1Runnable != null)
				button1Runnable.run();
			dismiss();
		});
		Button button2 = layout.findViewById(R.id.notice_dlg_btn_negative);
		button2.setText(button2TextRes);
		if (button2Runnable != null)
			button2.setOnClickListener(v -> {
				button2Runnable.run();
				dismiss();
			});
		else
			button2.setVisibility(View.GONE);
		TextView noticeText = layout.findViewById(R.id.notice_text_view);
		if (!StrUtils.isEmptyStr(sText)) noticeText.setText(sText);
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		if (button1!=null) button1.setBackgroundColor(colorGrayC);
		if (button2!=null) button2.setBackgroundColor(colorGrayC);
		setContentView(layout);
	}

	public void setMessage(int resourceId) {
		TextView textView = findViewById(R.id.notice_text_view);
		textView.setText(resourceId);
	}

}
