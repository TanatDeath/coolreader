package org.coolreader.crengine;

import org.coolreader.R;

import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class InputDialog extends BaseDialog {

	public interface InputHandler {
		boolean validate( String s ) throws Exception;
		boolean validateNoCancel( String s ) throws Exception;
		void onOk( String s ) throws Exception;
		void onCancel();
	};
	
	private InputHandler handler;
	private EditText input;
	int minValue;
	int maxValue;

	public InputDialog( BaseActivity activity, final String title, final String prompt,
						String defVal, final InputHandler handler ) {
		this(activity, title, prompt, false, 1, 2, 3, handler );
		input.setText(defVal);
	}


	public InputDialog( BaseActivity activity, final String title, final String prompt, boolean isNumberEdit, int minValue, int maxValue, int currentValue, final InputHandler handler )
	{
		super("InputDialog", activity, title, true, false);
		this.handler = handler;
		this.minValue = minValue;
		this.maxValue = maxValue;
        LayoutInflater mInflater = LayoutInflater.from(getContext());
        ViewGroup layout = (ViewGroup)mInflater.inflate(R.layout.line_edit_dlg, null);
        input = (EditText)layout.findViewById(R.id.input_field);
        TextView promptView = (TextView)layout.findViewById(R.id.lbl_prompt);
        if (promptView != null) {
        	promptView.setText(prompt);
        }
        SeekBar seekBar = layout.findViewById(R.id.goto_position_seek_bar);
        if (seekBar != null) {
        	if (!isNumberEdit) seekBar.setVisibility(View.INVISIBLE);
        	seekBar.setMax(maxValue - minValue);
        	seekBar.setProgress(currentValue);
        	seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}
				
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}
				
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					if (fromUser) {
						String value = String.valueOf(progress + InputDialog.this.minValue);
						try {
							if (handler.validate(value))
								input.setText(value);
						} catch (Exception e) {
							// ignore
						}
					}
				}
			});
        }
        //input = new EditText(getContext());
        if ( isNumberEdit )
        	input.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
//	        input.getText().setFilters(new InputFilter[] {
//	        	new DigitsKeyListener()        
//	        });
        setView(layout);
	}
	@Override
	protected void onNegativeButtonClick() {
        cancel();
        handler.onCancel();
	}
	@Override
	protected void onPositiveButtonClick() {
        String value = input.getText().toString().trim();
        try {
			if ( handler.validateNoCancel(value) )
				if ( handler.validate(value) )
					handler.onOk(value);
				else
					handler.onCancel();
			else
				return;
        } catch ( Exception e ) {
        	handler.onCancel();
        }
        cancel();
	}
}
