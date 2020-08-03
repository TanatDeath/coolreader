package org.coolreader.crengine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.coolreader.CoolReader;
import org.coolreader.CoolReader.DonationListener;
import org.coolreader.R;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TableLayout;
import android.widget.TextView;

public class AboutDialog extends BaseDialog implements TabContentFactory {
	final CoolReader mCoolReader;
	
	private View mAppTab;
	private View mDirsTab;
	private View mLicenseTab;
	private View mDonationTab;

	private boolean isPackageInstalled( String packageName ) {
		try {
			mCoolReader.getPackageManager().getApplicationInfo(packageName, 0);
			return true;
		} catch ( Exception e ) {
			return false;
		}
	}

	private void installPackage( String packageName ) {
		Log.i("cr3", "installPackageL " + packageName);
		try {
			mCoolReader.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
		} catch ( ActivityNotFoundException e ) {
			mCoolReader.showToast("Cannot run Android Market application");
		}
	}
	
	private void setupDonationButton( final Button btn, final String packageName ) {
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		int colorGray = a.getColor(0, Color.GRAY);
		int colorGrayC = a.getColor(1, Color.GRAY);
		int colorIcon = a.getColor(2, Color.GRAY);
		a.recycle();
		if ( isPackageInstalled(packageName)) {
			btn.setEnabled(false);
			btn.setText(R.string.dlg_about_donation_installed);
			btn.setBackgroundColor(colorGrayC);
			btn.setTextColor(colorIcon);
		} else {
			btn.setBackgroundColor(colorGrayC);
			btn.setTextColor(colorIcon);
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					installPackage(packageName);
				}
			});
		}
	}
	
	private void setupInAppDonationButton( final Button btn, final double amount ) {
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		int colorGray = a.getColor(0, Color.GRAY);
		int colorGrayC = a.getColor(1, Color.GRAY);
		int colorIcon = a.getColor(2, Color.GRAY);
		a.recycle();
		btn.setText("$" + amount);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mCoolReader.makeDonation(amount);
			}
		});
		btn.setBackgroundColor(colorGrayC);
		btn.setTextColor(colorIcon);
	}
	
	private void updateTotalDonations() {
		double amount = mCoolReader.getTotalDonations();
		if (isPackageInstalled("org.coolreader.donation.gold"))
			amount += 10.0;
		if (isPackageInstalled("org.coolreader.donation.silver"))
			amount += 3.0;
		if (isPackageInstalled("org.coolreader.donation.bronze"))
			amount += 1.0;
		TextView text = ((TextView)mDonationTab.findViewById(R.id.btn_about_donation_total));
		if (text != null)
			text.setText(mCoolReader.getString(R.string.dlg_about_donation_total) + " $" + amount);
	}

	public AboutDialog( CoolReader activity)
	{
		super("AboutDialog", activity);
		mCoolReader = activity;
		//boolean isFork = !mCoolReader.getPackageName().equals(CoolReader.class.getPackage().getName());
		boolean isFork = true;
		setTitle(R.string.dlg_about);
		LayoutInflater inflater = LayoutInflater.from(getContext());
		final TabHost tabs = (TabHost)inflater.inflate(R.layout.about_dialog, null);

		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		final int colorGray = a.getColor(0, Color.GRAY);
		final int colorGrayC = a.getColor(1, Color.GRAY);
		final int colorIcon = a.getColor(2, Color.GRAY);

		tabs.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId) {
				for (int i = 0; i < tabs.getTabWidget().getChildCount(); i++) {
					tabs.getTabWidget().getChildAt(i)
							.setBackgroundColor(colorGrayC); // unselected
				}

				tabs.getTabWidget().getChildAt(tabs.getCurrentTab())
						.setBackgroundColor(colorGray); // selected

			}
		});

		mAppTab = (View)inflater.inflate(R.layout.about_dialog_app, null);
		if (isFork) {
			((TextView) mAppTab.findViewById(R.id.version)).setText("KnownReader " + mCoolReader.getVersion());
			((TextView) mAppTab.findViewById(R.id.www1)).setText("KnownReader.com");
		} else {
			((TextView) mAppTab.findViewById(R.id.version)).setText("KnownReader " + mCoolReader.getVersion());
		}
		TextView tv_icons8 = (TextView)mAppTab.findViewById(R.id.www_icons8);
		TextView tv_email = (TextView)mAppTab.findViewById(R.id.email);
		TextView tv_www1 = (TextView)mAppTab.findViewById(R.id.www1);
		if (tv_icons8 != null) tv_icons8.setLinkTextColor(colorIcon);
		if (tv_email != null) tv_email.setLinkTextColor(colorIcon);
		if (tv_www1 != null) tv_www1.setLinkTextColor(colorIcon);

		mDirsTab = (View)inflater.inflate(R.layout.about_dialog_dirs, null);
		TextView fonts_dir = (TextView)mDirsTab.findViewById(R.id.fonts_dirs);

		ArrayList<String> fontsDirs = Engine.getFontsDirs();
		StringBuilder sbuf = new StringBuilder();
		Iterator<String> it = fontsDirs.iterator();
		while (it.hasNext()) {
			String s = it.next();
			sbuf.append(s);
			if (it.hasNext()) {
				sbuf.append("\n");
			}
		}
		fonts_dir.setText(sbuf.toString());

		ArrayList<String> texturesDirs = Engine.getDataDirs(Engine.DataDirType.TexturesDirs);
		sbuf = new StringBuilder();
		it = texturesDirs.iterator();
		while (it.hasNext()) {
			String s = it.next();
			sbuf.append(s);
			if (it.hasNext()) {
				sbuf.append("\n");
			}
		}
		TextView textures_dir = (TextView)mDirsTab.findViewById(R.id.textures_dirs);
		textures_dir.setText(sbuf.toString());

		ArrayList<String> backgroundsDirs = Engine.getDataDirs(Engine.DataDirType.BackgroundsDirs);
		sbuf = new StringBuilder();
		it = backgroundsDirs.iterator();
		while (it.hasNext()) {
			String s = it.next();
			sbuf.append(s);
			if (it.hasNext()) {
				sbuf.append("\n");
			}
		}
		TextView backgrounds_dir = (TextView)mDirsTab.findViewById(R.id.backgrounds_dirs);
		backgrounds_dir.setText(sbuf.toString());

		ArrayList<String> hyphDirs = Engine.getDataDirs(Engine.DataDirType.HyphsDirs);
		sbuf = new StringBuilder();
		it = hyphDirs.iterator();
		while (it.hasNext()) {
			String s = it.next();
			sbuf.append(s);
			if (it.hasNext()) {
				sbuf.append("\n");
			}
		}
		TextView hyph_dir = (TextView)mDirsTab.findViewById(R.id.hyph_dirs);
		hyph_dir.setText(sbuf.toString());

		mLicenseTab = (View)inflater.inflate(R.layout.about_dialog_license, null);
		String license = Engine.getInstance(mCoolReader).loadResourceUtf8(R.raw.license);
		((TextView)mLicenseTab.findViewById(R.id.license)).setText(license);
        boolean billingSupported = mCoolReader.isDonationSupported() && !isFork;
		mDonationTab = (View)inflater.inflate(billingSupported ? R.layout.about_dialog_donation2 : R.layout.about_dialog_donation, null);

		if (billingSupported) {
			setupInAppDonationButton( (Button)mDonationTab.findViewById(R.id.btn_about_donation_install_vip), 100);
			setupInAppDonationButton( (Button)mDonationTab.findViewById(R.id.btn_about_donation_install_platinum), 30);
			setupInAppDonationButton( (Button)mDonationTab.findViewById(R.id.btn_about_donation_install_gold), 10);
			setupInAppDonationButton( (Button)mDonationTab.findViewById(R.id.btn_about_donation_install_silver), 3);
			setupInAppDonationButton( (Button)mDonationTab.findViewById(R.id.btn_about_donation_install_bronze), 1);
			setupInAppDonationButton( (Button)mDonationTab.findViewById(R.id.btn_about_donation_install_iron), 0.3);
			updateTotalDonations();
			mCoolReader.setDonationListener(new DonationListener() {
				@Override
		    	public void onDonationTotalChanged(double total) {
		    		updateTotalDonations();
		    	}
		    });
			setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					mCoolReader.setDonationListener(null);
				}
			});
		} else {
			setupDonationButton( (Button)mDonationTab.findViewById(R.id.btn_about_donation_install_gold), "org.coolreader.donation.gold");
			setupDonationButton( (Button)mDonationTab.findViewById(R.id.btn_about_donation_install_silver), "org.coolreader.donation.silver");
			setupDonationButton( (Button)mDonationTab.findViewById(R.id.btn_about_donation_install_bronze), "org.coolreader.donation.bronze");
		}

		if (isFork) {
			LinearLayout ll_donate1 = (LinearLayout)mDonationTab.findViewById(R.id.ll_donate1);
			LinearLayout ll_donate2 = (LinearLayout)mDonationTab.findViewById(R.id.ll_donate2);
			LinearLayout ll_donate3 = (LinearLayout)mDonationTab.findViewById(R.id.ll_donate3);
			if (ll_donate1 != null) ((ViewGroup) ll_donate1.getParent()).removeView(ll_donate1);
			if (ll_donate2 != null) ((ViewGroup) ll_donate2.getParent()).removeView(ll_donate2);
			if (ll_donate3 != null) ((ViewGroup) ll_donate3.getParent()).removeView(ll_donate3);
			TableLayout tl_donate1 = (TableLayout)mDonationTab.findViewById(R.id.tl_donate1);
			if (tl_donate1 != null) ((ViewGroup) tl_donate1.getParent()).removeView(ll_donate3);
			TextView btn_about_donation_total = (TextView)mDonationTab.findViewById(R.id.btn_about_donation_total);
			if (btn_about_donation_total != null) ((ViewGroup) btn_about_donation_total.getParent()).removeView(ll_donate3);
			TextView tv_donate_message = (TextView)mDonationTab.findViewById(R.id.tv_donate_message);
			TextView tv_donate_message2 = (TextView)mDonationTab.findViewById(R.id.tv_donate_message2);
			TextView tv_donate_message3 = (TextView)mDonationTab.findViewById(R.id.tv_donate_message3);
			TextView tv_donate_message4 = (TextView)mDonationTab.findViewById(R.id.tv_donate_message4);
			TextView tv_donate_message5 = (TextView)mDonationTab.findViewById(R.id.tv_donate_message5);
			if (tv_donate_message != null) tv_donate_message.setText(R.string.knownreader_donate_line1);
			if (tv_donate_message2 != null) tv_donate_message2.setText(R.string.knownreader_donate_line2);
			if (tv_donate_message3 != null) {
				tv_donate_message3.setText(R.string.knownreader_donate_line3);
				tv_donate_message3.setLinkTextColor(colorIcon);
			}
			if (tv_donate_message4 != null) tv_donate_message4.setText(R.string.knownreader_donate_line4);
			if (tv_donate_message5 != null) {
				tv_donate_message5.setText(R.string.knownreader_donate_line5);
				tv_donate_message5.setLinkTextColor(colorIcon);
			}
		}
		
		tabs.setup();
		TabHost.TabSpec tsApp = tabs.newTabSpec("App");
		Drawable d = getContext().getResources().getDrawable(Utils.resolveResourceIdByAttr(activity, R.attr.attr_icons8_link, R.drawable.icons8_link));
		mCoolReader.tintViewIcons(d,true);
		tsApp.setIndicator("", d);
				//getContext().getResources().getDrawable(R.drawable.icons8_link));
		tsApp.setContent(this);
		tabs.addTab(tsApp);

		TabHost.TabSpec tsDirectories = tabs.newTabSpec("Directories");

		Drawable d2 = getContext().getResources().getDrawable(Utils.resolveResourceIdByAttr(activity, R.attr.attr_icons8_folder_2, R.drawable.icons8_folder_2));
		mCoolReader.tintViewIcons(d2,true);

		tsDirectories.setIndicator("", d2);
				//getContext().getResources().getDrawable(R.drawable.icons8_folder_2));
		tsDirectories.setContent(this);
		tabs.addTab(tsDirectories);

		TabHost.TabSpec tsLicense = tabs.newTabSpec("License");
		Drawable d3 = getContext().getResources().getDrawable(Utils.resolveResourceIdByAttr(activity, R.attr.attr_icons8_star, R.drawable.icons8_star));
		mCoolReader.tintViewIcons(d3,true);
		tsLicense.setIndicator("", d3);
				//getContext().getResources().getDrawable(R.drawable.icons8_star));
		tsLicense.setContent(this);
		tabs.addTab(tsLicense);
		
		TabHost.TabSpec tsDonation = tabs.newTabSpec("Donation");
		Drawable d4 = getContext().getResources().getDrawable(Utils.resolveResourceIdByAttr(activity, R.attr.attr_icons8_happy, R.drawable.icons8_happy));
		mCoolReader.tintViewIcons(d4,true);
		tsDonation.setIndicator("", d4);
				//getContext().getResources().getDrawable(R.drawable.icons8_happy));
		tsDonation.setContent(this);
		tabs.addTab(tsDonation);
		
		setView( tabs );
		tabs.setCurrentTab(1);
		tabs.setCurrentTab(0);
		// 25% chance to show Donations tab
		rnd = new Random(android.os.SystemClock.uptimeMillis());
		if ((rnd.nextInt() & 3) == 3)
			tabs.setCurrentTab(3);
		
	}
	private static Random rnd = new Random(android.os.SystemClock.uptimeMillis()); 

	
	@Override
	public View createTabContent(String tag) {

		if ( "App".equals(tag) )
			return mAppTab;
		else if ( "Directories".equals(tag) )
			return mDirsTab;
		else if ( "License".equals(tag) )
			return mLicenseTab;
		else if ( "Donation".equals(tag) )
			return mDonationTab;
		return null;
	}
	
}
