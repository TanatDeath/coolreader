package org.coolreader.crengine;

import org.coolreader.CoolReader;
import org.coolreader.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;

	public class SwitchProfileDialog extends BaseDialog implements Settings {
	CoolReader mCoolReader;
	ReaderView mReaderView;
	ListView mListView;
	int currentProfile;
	OptionsDialog optionsDialog = null;

	private String[] profileNames = {
			"Profile 1",
			"Profile 2",
			"Profile 3",
			"Profile 4",
			"Profile 5",
			"Profile 6",
			"Profile 7",
	};

	public SwitchProfileDialog(CoolReader coolReader, ReaderView readerView, OptionsDialog od)
	{
		super("SwitchProfileDialog", coolReader, coolReader.getResources().getString(R.string.action_switch_settings_profile), false, false);
        setCancelable(true);
		this.mCoolReader = coolReader;
		this.mReaderView = readerView;
		this.optionsDialog = od;
		Properties props = new Properties(mCoolReader.settings());
		for (int i=0; i<7; i++) {
			String pname = props.getProperty(Settings.PROP_PROFILE_NAME + "." + (i+1), "");
			if (!StrUtils.isEmptyStr(pname)) profileNames[i] = pname;
		}
		this.mListView = new BaseListView(getContext(), false);
		currentProfile = this.mCoolReader.getCurrentProfile();
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> listview, View view,
					int position, long id) {
				if (optionsDialog != null) {
					BackgroundThread.instance().executeGUI(new Runnable() {
						public void run() {
							optionsDialog.apply();
							optionsDialog.dismiss();
							mReaderView.setCurrentProfile(position + 1);
							mCoolReader.showOptionsDialog(OptionsDialog.Mode.READER);
						}
					});
				} else mReaderView.setCurrentProfile(position + 1);
				SwitchProfileDialog.this.dismiss();
			}
		});

		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> listview, View view,
					int position, long id) {
				ArrayList<String[]> vl = new ArrayList<String[]>();
				String[] arrS1 = {mCoolReader.getString(R.string.new_profile), mCoolReader.getString(R.string.new_profile),
						profileNames[position]};
				vl.add(arrS1);
				AskSomeValuesDialog dlgA = new AskSomeValuesDialog(
						mCoolReader,
						mCoolReader.getString(R.string.rename_profile),
						mCoolReader.getString(R.string.rename_profile) + ": " +
						 profileNames[position],
						vl, new AskSomeValuesDialog.ValuesEnteredCallback() {
					@Override
					public void done(ArrayList<String> results) {
						if (results != null) {
							if (results.size() >= 1)
								if (!StrUtils.isEmptyStr(results.get(0))) {
									profileNames[position] = results.get(0).trim();
									Properties props = new Properties(mCoolReader.settings());
									props.setProperty(Settings.PROP_PROFILE_NAME + "." + (position+1), results.get(0).trim());
									if (optionsDialog != null)
										optionsDialog.mProperties.setProperty(Settings.PROP_PROFILE_NAME + "." + (position+1), results.get(0).trim());
									mCoolReader.setSettings(props, -1, true);
									profileNames[position] = results.get(0).trim();
									mListView.invalidateViews();
								}
						}
					}
				});
				dlgA.show();
				return true;
			}
		});
		mListView.setLongClickable(true);
		mListView.setClickable(true);
		mListView.setFocusable(true);
		mListView.setFocusableInTouchMode(true);
		mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		setView(mListView);
		setFlingHandlers(mListView, new Runnable() {
			@Override
			public void run() {
				// cancel
				SwitchProfileDialog.this.dismiss();
			}
		}, new Runnable() {
			@Override
			public void run() {
				// 
				SwitchProfileDialog.this.dismiss();
			}
		});
		ProfileListAdapter pla = new ProfileListAdapter();
		mListView.setAdapter(pla);
		pla.notifyDataSetChanged();
	}

	class ProfileListAdapter extends BaseListAdapter {
		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			return Settings.MAX_PROFILES;
		}

		public Object getItem(int position) {
			return profileNames[position];
		}

		public long getItemId(int position) {
			return position;
		}

		public int getItemViewType(int position) {
			return 0;
		}

		
		public View getView(final int position, View convertView, ViewGroup parent) {
			View view;
			boolean isCurrentItem = position == currentProfile - 1;
			if ( convertView==null ) {
				//view = new TextView(getContext());
				LayoutInflater inflater = LayoutInflater.from(getContext());
				view = inflater.inflate(R.layout.profile_item, null);
			} else {
				view = (View)convertView;
			}
			RadioButton cb = (RadioButton)view.findViewById(R.id.option_value_check);
			TextView title = (TextView)view.findViewById(R.id.option_value_text);
			ImageView iv = (ImageView)view.findViewById(R.id.btn_option_add_info);
			iv.setVisibility(view.INVISIBLE);
			cb.setChecked(isCurrentItem);
			cb.setFocusable(false);
			cb.setFocusableInTouchMode(false);
			title.setText(profileNames[position]);
			cb.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (optionsDialog != null) {
						BackgroundThread.instance().executeGUI(new Runnable() {
							public void run() {
								optionsDialog.apply();
								optionsDialog.dismiss();
								mReaderView.setCurrentProfile(position + 1);
								mCoolReader.showOptionsDialog(OptionsDialog.Mode.READER);
							}
						});
					} else mReaderView.setCurrentProfile(position + 1);
					SwitchProfileDialog.this.dismiss();
				}
			});
			return view;
		}

		public int getViewTypeCount() {
			return 1;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			return false;
		}
	}

}

