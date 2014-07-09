package com.rob.thebutton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;

public class startingPoint extends Activity {

	private static ArrayList<PInfo> mApplications;
	private final BroadcastReceiver mApplicationsReceiver = new ApplicationsIntentReceiver();
	private GridView mGrid;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		setContentView(R.layout.main);

		registerIntentReceivers();

		loadApplications(true);

		setupGallery();

		

	}
	
	public void setupGallery(){
        mGrid = (GridView) findViewById(R.id.gridView1);
		mGrid.setAdapter(new GridAdapter(this, mApplications));
		mGrid.setSelection(0);
		mGrid.setOnItemClickListener(new ApplicationLauncher());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		// Close the menu
		if (Intent.ACTION_MAIN.equals(intent.getAction())) {
			getWindow().closeAllPanels();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// Remove the callback for the cached drawables or we leak
		// the previous Home screen on orientation change
		final int count = mApplications.size();
		for (int i = 0; i < count; i++) {
			mApplications.get(i).icon.setCallback(null);
		}

		
		unregisterReceiver(mApplicationsReceiver);
	}

	private void registerIntentReceivers() {
		IntentFilter filter = new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED);
		filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
		filter.addDataScheme("package");
		registerReceiver(mApplicationsReceiver, filter);
	}

	
	
	/**
	 * Loads the list of installed applications in mApplications.
	 */
	private void loadApplications(boolean isLaunching) {

		if (isLaunching && mApplications != null) {
			return;
		}

		PackageManager manager = getPackageManager();
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		intentAddon myIntent = new intentAddon();

		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

	//	mainIntent.addCategory(myIntent.CATEGORY_GAME);

		// mainIntent.addCategory(addSomething.CATEGORY_APP);

		final List<ResolveInfo> apps = manager.queryIntentActivities(
				mainIntent, 0);
		Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

		if (apps != null) {
			final int count = apps.size();

			if (mApplications == null) {
				mApplications = new ArrayList<PInfo>(count);
			}
			mApplications.clear();

			for (int i = 0; i < count; i++) {
				PInfo application = new PInfo();
				ResolveInfo info = apps.get(i);
				application.title = info.loadLabel(manager);
				application.setActivity(new ComponentName(
						info.activityInfo.applicationInfo.packageName,
						info.activityInfo.name), Intent.FLAG_ACTIVITY_NEW_TASK
						| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
				application.icon = info.activityInfo.loadIcon(manager);

				mApplications.add(application);
			}
		}
	}

	private class ApplicationsIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			loadApplications(false);
		

		}
	}

	/**
	 * ListView adapter to show the list of all installed applications.
	 */
	private class GridAdapter extends ArrayAdapter<PInfo> {
		private Rect mOldBounds = new Rect();

		public GridAdapter(Context context, ArrayList<PInfo> apps) {
			super(context, 0, apps);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final PInfo info = mApplications.get(position);

			if (convertView == null) {
				final LayoutInflater inflater = getLayoutInflater();
				convertView = inflater.inflate(R.layout.application, parent,
						false);
			}

			Drawable icon = info.icon;

			if (!info.filtered) {
				// The drawer icons size
				// final Resources resources = getContext().getResources();
				int width = 624;// (int)524
								// resources.getDimension(android.R.dimen.app_icon_size);
				int height = width;// (int)
									
				
				final Bitmap.Config c = icon.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
						: Bitmap.Config.RGB_565;
				final Bitmap thumb = Bitmap.createBitmap(width, height, c);
				final Canvas canvas = new Canvas(thumb);
				canvas.setDrawFilter(new PaintFlagsDrawFilter(
						Paint.DITHER_FLAG, 0));
				// Copy the old bounds to restore them later
				// If we were to do oldBounds = icon.getBounds(),
				// the call to setBounds() that follows would
				// change the same instance and we would lose the
				// old bounds
				mOldBounds.set(icon.getBounds());
				icon.setBounds(0, 0, width, height);
				icon.draw(canvas);
				icon.setBounds(mOldBounds);
				icon = info.icon = new BitmapDrawable(thumb);
				info.filtered = true;
			}

			final TextView textView = (TextView) convertView
					.findViewById(R.id.label);
			textView.setCompoundDrawablesWithIntrinsicBounds(null, icon, null,
					null);
			textView.setText(info.title);
			return convertView;
		}
	}

	
	private class ApplicationLauncher implements
			AdapterView.OnItemClickListener {
		public void onItemClick(AdapterView parent, View v, int position,
				long id) {
			PInfo app = (PInfo) parent.getItemAtPosition(position);

			startActivity(app.intent);

		}

	}

	public class intentAddon extends Intent {
		public static final String CATEGORY_GAME = "tv.ouya.intent.category.GAME";
		public static final String CATEGORY_APP = "tv.ouya.intent.category.APP";

	}

}
