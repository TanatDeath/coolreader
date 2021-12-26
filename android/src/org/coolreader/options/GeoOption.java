package org.coolreader.options;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;
import org.coolreader.geo.MetroLocation;
import org.coolreader.geo.TransportStop;

import java.util.ArrayList;

public class GeoOption extends ListOption {

	final BaseActivity mActivity;

	public GeoOption(BaseActivity activity, OptionOwner owner, String filter) {
		super(owner, activity.getString(R.string.options_app_geo), Settings.PROP_APP_GEO,
				activity.getString(R.string.options_app_geo_add_info), filter);
		mActivity = activity;
		add("1", mActivity.getString(R.string.options_app_geo_1),"");
		add("2", mActivity.getString(R.string.options_app_geo_2),mActivity.getString(R.string.options_app_geo_2_add_info));
		add("3", mActivity.getString(R.string.options_app_geo_3),mActivity.getString(R.string.options_app_geo_3_add_info));
		add("4", mActivity.getString(R.string.options_app_geo_4),"");
		if (mProperties.getProperty(property) == null)
			mProperties.setProperty(property, "1");
	}

	@Override
	public void onClick( OptionsDialog.Three item ) {
		super.onClick(item);
		CoolReader cr = (CoolReader)mActivity;
		if ((item.value.equals("2"))||(item.value.equals("3"))||(item.value.equals("4"))) {
			if (cr.geoLastData != null) {
				cr.geoLastData.gpsStop();
				cr.geoLastData.netwStop();
				if ((item.value.equals("2")) || (item.value.equals("4")))
					cr.geoLastData.loadMetroStations(cr, true);
				if ((item.value.equals("3")) || (item.value.equals("4")))
					cr.geoLastData.loadTransportStops(cr, true);
				cr.geoLastData.gpsStart();
				cr.geoLastData.netwStart();
			}
			((CoolReader) mActivity).checkLocationPermission();
		} else {
			if (cr.geoLastData != null) {
				cr.geoLastData.gpsStop();
				cr.geoLastData.netwStop();
				if (cr.geoLastData.metroLocations == null)
					cr.geoLastData.metroLocations = new ArrayList<>();
				cr.geoLastData.metroLocations.clear();
				if (cr.geoLastData.transportStops == null)
					cr.geoLastData.transportStops = new ArrayList<>();
				cr.geoLastData.transportStops.clear();
			}
		}
	}
}
