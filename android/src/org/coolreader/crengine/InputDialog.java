package org.coolreader.crengine;

import org.coolreader.R;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class InputDialog extends BaseDialog {

	public interface InputHandler {
		boolean validate(String s) throws Exception;
		boolean validateNoCancel(String s) throws Exception;
		void onOk(String s) throws Exception;
		void onCancel();
	};

	private final InputHandler mInputHandler;
	private final EditText mEditText;
	private final int mMinValue;
	private final int mMaxValue;
	private SeekBar mSeekBar;

	public InputDialog( BaseActivity activity, final String title, final String prompt,
						String defVal, final InputHandler handler ) {
		this(activity, title, prompt, false, 1, 2, 3, handler );
		mEditText.setText(defVal);
	}

	public InputDialog(BaseActivity activity, final String title, final String prompt, boolean isNumberEdit, int minValue, int maxValue, int currentValue, final InputHandler inputHandler) {
		this(activity, title, true, prompt, isNumberEdit, minValue, maxValue, currentValue, inputHandler);
	}

	@SuppressLint("ClickableViewAccessibility")
	public InputDialog(BaseActivity activity, final String title, boolean showNegativeButton, final String prompt, boolean isNumberEdit, int minValue, int maxValue, int currentValue, final InputHandler inputHandler) {
		super("InputDialog", activity, title, showNegativeButton, false);
		this.mInputHandler = inputHandler;
		this.mMinValue = minValue;
		this.mMaxValue = maxValue;
        LayoutInflater mInflater = LayoutInflater.from(getContext());
        ViewGroup layout = (ViewGroup) mInflater.inflate(R.layout.line_edit_dlg, null);
		mEditText = layout.findViewById(R.id.input_field);
        TextView promptView = layout.findViewById(R.id.lbl_prompt);
        if (promptView != null) {
        	promptView.setText(prompt);
        }
        mSeekBar = layout.findViewById(R.id.goto_position_seek_bar);
        if (mSeekBar != null) {
        	if (!isNumberEdit) mSeekBar.setVisibility(View.INVISIBLE);
        	mSeekBar.setMax(maxValue - minValue);
        	mSeekBar.setProgress(currentValue - minValue);
        	mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}
				
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}
				
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					/*if (fromUser)*/
					{
						String value = String.valueOf(progress + InputDialog.this.mMinValue);
						try {
							if (inputHandler.validate(value))
								mEditText.setTextKeepState(value);
						} catch (Exception e) {
							// ignore
						}
					}
				}
			});
        }
        //input = new EditText(getContext());
		// Проверить потом как это работает, закомментарил две строчки
//        if ( isNumberEdit )
//        	mEditText.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
//	        input.getText().setFilters(new InputFilter[] {
//	        	new DigitsKeyListener()        
//	        });
		if (isNumberEdit) {
			mEditText.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
			mEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
			if (currentValue >= minValue)
				mEditText.setText(String.valueOf(currentValue));
//	        input.getText().setFilters(new InputFilter[] {
//	        	new DigitsKeyListener()
//	        });
			mEditText.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					try {
						int value = Integer.parseInt(s.toString());
						mSeekBar.setProgress(value - InputDialog.this.mMinValue);
					} catch (Exception ignored) {
					}
				}
			});
			ImageView decButton = layout.findViewById(R.id.btn_dec);
			decButton.setVisibility(View.VISIBLE);
			if (!isNumberEdit) decButton.setVisibility(View.INVISIBLE);
			decButton.setOnTouchListener(new RepeatOnTouchListener(500, 150,
					view -> mSeekBar.setProgress(mSeekBar.getProgress() - 1)));
			ImageView incButton = layout.findViewById(R.id.btn_inc);
			incButton.setVisibility(View.VISIBLE);
			if (!isNumberEdit) incButton.setVisibility(View.INVISIBLE);
			incButton.setOnTouchListener(new RepeatOnTouchListener(500, 150,
					view -> mSeekBar.setProgress(mSeekBar.getProgress() + 1)));
		}
        setView(layout);
	}
	@Override
	protected void onNegativeButtonClick() {
        cancel();
        mInputHandler.onCancel();
	}
	@Override
	protected void onPositiveButtonClick() {
        String value = mEditText.getText().toString().trim();
        try {
			if (mInputHandler.validateNoCancel(value))
				if (mInputHandler.validate(value))
					mInputHandler.onOk(value);
				else
					mInputHandler.onCancel();
			else
				return;
        } catch ( Exception e ) {
        	mInputHandler.onCancel();
        }
        cancel();
	}
}
