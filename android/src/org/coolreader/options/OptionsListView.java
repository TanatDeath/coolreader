package org.coolreader.options;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

import org.coolreader.R;
import org.coolreader.crengine.BaseListView;
import org.coolreader.crengine.StrUtils;

import java.util.ArrayList;

public class OptionsListView extends BaseListView {
    public ArrayList<OptionBase> mOptions = new ArrayList<>();
    public ArrayList<OptionBase> mOptionsFiltered = new ArrayList<>();
    public ArrayList<OptionBase> mOptionsThis;
    private ListAdapter mAdapter;
    private OptionBase root;
    public void refresh()
    {
        //setAdapter(mAdapter);
        for (OptionBase item : mOptions) {
            item.refreshItem();
        }
        invalidate();
    }
    public OptionsListView add(OptionBase option) {
        if ((option.lastFiltered)||(root != null)) {
            mOptions.add(option);
            option.optionsListView = this;
            if (root != null) root.updateFilteredMark(option.lastFiltered);
        }
        return this;
    }

    public boolean remove(int index) {
        try {
            mOptions.remove(index);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }
    public boolean remove( OptionBase option ) {
        return mOptions.remove(option);
    }
    public void clear() {
        mOptions.clear();
    }

    public OptionsListView addExt(OptionBase option, String addWords ) {
        if (!addWords.equals("")) {
            for (String s: addWords.split("\\,")) option.updateFilteredMark(s);
        }
        option.updateFilteredMark(addWords);
        if ((option.lastFiltered)||(root != null)) {
            mOptions.add(option);
            option.optionsListView = this;
            if (root != null) root.updateFilteredMark(option.lastFiltered);
        }
        return this;
    }

    public void listUpdated(String sText) {
        mOptionsFiltered.clear();
        for(int i=0;i<mOptions.size();i++){
            if (
                    (StrUtils.getNonEmptyStr(mOptions.get(i).getValueLabel().toLowerCase(), true).contains(sText.toLowerCase()))||
                            ((mOptions.get(i).label.toLowerCase()).contains(sText.toLowerCase()))||
                            (
                                    ((mOptions.get(i).property.toLowerCase()).contains(sText.toLowerCase())) &&
                                            (!mOptions.get(i).property.startsWith("viewer.toolbar.buttons"))
                            ) ||
                            ((mOptions.get(i).addInfo.toLowerCase()).contains(sText.toLowerCase()))
            ) {
                mOptionsFiltered.add(mOptions.get(i));
            }
        }
        mOptionsThis = mOptionsFiltered;
        if ((sText.equals(""))&&(mOptions.size()==0)) mOptionsThis = mOptions;
        mAdapter = new BaseAdapter() {
            public boolean areAllItemsEnabled() {
                return true;
            }

            public boolean isEnabled(int position) {
                return true;
            }

            public int getCount() {
                return mOptionsThis.size();
            }

            public Object getItem(int position) {
                return mOptionsThis.get(position);
            }

            public long getItemId(int position) {
                return position;
            }

            public int getItemViewType(int position) {
//					OptionBase item = mOptions.get(position);
//					return item.getItemViewType();
                return position;
            }


            public View getView(int position, View convertView, ViewGroup parent) {
                OptionBase item = mOptionsThis.get(position);
                return item.getView(convertView, parent);
            }

            public int getViewTypeCount() {
                //return OPTION_VIEW_TYPE_COUNT;
                return mOptionsThis.size() > 0 ? mOptionsThis.size() : 1;
            }

            public boolean hasStableIds() {
                return true;
            }

            public boolean isEmpty() {
                return mOptionsThis.size()==0;
            }

            private ArrayList<DataSetObserver> observers = new ArrayList<>();

            public void registerDataSetObserver(DataSetObserver observer) {
                observers.add(observer);
            }

            public void unregisterDataSetObserver(DataSetObserver observer) {
                observers.remove(observer);
            }
        };
        setAdapter(mAdapter);
    }

    public OptionsListView(Context context, OptionBase root)
    {
        super(context, false);
        setFocusable(true);
        setFocusableInTouchMode(true);
        this.root = root;
        listUpdated("");
        this.setOnItemLongClickListener((arg0, arg1, pos, id) -> {
            if (mOptionsThis!=null) {
                return mOptionsThis.get(pos).onLongClick(arg0);
            }
            else {
                return mOptions.get(pos).onLongClick(arg0);
            }
        });
    }

    @Override
    public boolean performItemClick(View view, int position, long id) {
        try {
            if (mOptionsThis != null) {
                OptionBase option = mOptionsThis.get(position);
                if (option.enabled) {
                    option.onSelect();
                    return true;
                }
            }
            else {
                OptionBase option = mOptions.get(position);
                if (option.enabled) {
                    option.onSelect();
                    return true;
                }
            }
        } catch (Exception ignored) {
            Log.e("OPT", "performItemClick" + ignored.getMessage());
            if (root != null)
                root.mActivity.showToast(root.mActivity.getString(R.string.unhandled_error)+": "+ignored.getMessage());
        }
        return false;
    }

    public int size() {
        if (mOptionsThis!=null) {
            return mOptionsThis.size();
        }
        return mOptions.size();
    }

}