package org.coolreader.crengine;

import android.app.Activity;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import org.coolreader.CoolReader;

import java.util.ArrayList;

public abstract class SearchableBaseAdapter extends BaseAdapter implements Filterable {

    private ArrayList<OptionsDialog.Three> itemList;
    private Activity context;
    private LayoutInflater inflater;
    private ValueFilter valueFilter;
    protected ArrayList<OptionsDialog.Three> itemListFiltered;

    public SearchableBaseAdapter(Activity context, ArrayList<OptionsDialog.Three> itemList, LayoutInflater inflater) {
        super();
        this.context = context;
        this.itemList = itemList;
        this.itemListFiltered =  itemList;
        this.inflater = inflater;
        getFilter();
    }

    @Override
    public int getCount() {
        return itemList.size();
    }

    @Override
    public Object getItem(int position) {
        return itemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Filter getFilter() {
        if(valueFilter==null) {

            valueFilter=new ValueFilter();
        }

        return valueFilter;
    }

    private class ValueFilter extends Filter {

        //Invoked in a worker thread to filter the data according to the constraint.
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ((CoolReader)context).showToast(constraint.toString());
            FilterResults results=new FilterResults();
            if(constraint!=null && constraint.length()>0){
                ArrayList<OptionsDialog.Three> filterList=new ArrayList<OptionsDialog.Three>();
                for(int i=0;i<itemListFiltered.size();i++){
                    if (
                            ((itemListFiltered.get(i).label.toLowerCase())
                                .contains(constraint.toString().toLowerCase()))||
                            ((itemListFiltered.get(i).value.toLowerCase())
                                .contains(constraint.toString().toLowerCase()))||
                            ((itemListFiltered.get(i).addInfo.toLowerCase())
                                .contains(constraint.toString().toLowerCase()))
                       ) {
                        OptionsDialog.Three item = new OptionsDialog.Three(
                                itemListFiltered.get(i).label, itemListFiltered.get(i).value,
                                itemListFiltered.get(i).addInfo);
                        filterList.add(item);
                    }
                }
                results.count=filterList.size();
                results.values=filterList;
            }else{
                results.count=itemListFiltered.size();
                results.values=itemListFiltered;
            }
            return results;
        }


        //Invoked in the UI thread to publish the filtering results in the user interface.
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            itemList=(ArrayList<OptionsDialog.Three>) results.values;
            ((CoolReader)context).showToast("filtered: "+constraint+" "+itemList.size());
            notifyDataSetChanged();
        }
    }
}