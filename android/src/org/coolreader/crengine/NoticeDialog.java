package org.coolreader.crengine;

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

public class NoticeDialog extends Dialog {

	public NoticeDialog(BaseActivity activity, String sText, final Runnable onOkButton, final Runnable onCancelButton) {
		super(activity, activity.getCurrentTheme().getThemeId());
		setOwnerActivity(activity);
        LayoutInflater mInflater = LayoutInflater.from(getContext());
        ViewGroup layout = (ViewGroup)mInflater.inflate(R.layout.notice_dialog, null);
        setTitle(R.string.app_name);
        Button button1 = layout.findViewById(R.id.base_dlg_btn_positive);
        button1.setOnClickListener(v -> {
			if (onOkButton != null)
				onOkButton.run();
			dismiss();
		});
		Button button2 = layout.findViewById(R.id.base_dlg_btn_negative);
        if (onCancelButton != null)
	        button2.setOnClickListener(v -> {
				onCancelButton.run();
				dismiss();
			});
        else
        	button2.setVisibility(View.GONE);
		TextView noticeText = layout.findViewById(R.id.notice_text_view);
		if (!StrUtils.isEmptyStr(sText)) noticeText.setText(sText);

		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		int colorGray = a.getColor(0, Color.GRAY);
		int colorGrayC = a.getColor(1, Color.GRAY);
		int colorIcon = a.getColor(2, Color.GRAY);
		a.recycle();
		if (button1!=null) button1.setBackgroundColor(colorGrayC);
		if (button2!=null) button2.setBackgroundColor(colorGrayC);

		setContentView(layout);
	}

	public void setMessage(int resourceId) {
		TextView textView = findViewById(R.id.notice_text_view);
		textView.setText(resourceId);
	}

}
