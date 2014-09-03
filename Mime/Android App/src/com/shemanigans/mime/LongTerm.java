package com.shemanigans.mime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class LongTerm extends Activity
implements NavigationDrawerFragment.NavigationDrawerCallbacks, 
NameTextFileFragment.NameTextFileListener,
BioimpFragment.OnButtonClickedListener {

	/**
	 * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;
	private NameTextFileFragment dialog;
	/**
	 * Used to store the last screen title. For use in {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;
	private final static String TAG = LongTerm.class.getSimpleName();
	private BluetoothLeService mBluetoothLeService;
	ServiceBinder mServiceBinder;
	private static final String LIVE_DATA_TAG = "LIVE_DATA_TAG";
	private static final String PAST_HOUR_TAG = "PAST_HOUR_TAG";
	private static final String PAST_DAY_TAG = "PAST_DAY_TAG";
	private static final String CUSTOM_RANGE_TAG = "CUSTOM_RANGE_TAG";
	private static final String SCAN_TAG = "SCAN_TAG";


	public double[] values = {1, 2, 3, 4};
	private boolean checkNamedTextFile = false;
	public ArrayList<String> textFile = new ArrayList<String>();
	private String textFileName = "AccelData";
	private Calendar c = Calendar.getInstance();
	private String mDeviceName;
	private String mDeviceAddress;
	private boolean mConnected = true;


	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnectionBLE = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			Log.i(TAG, "Bound to BLE Service.");
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService.clientDisconnected();
		}
	};

	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnectionBioImp = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mServiceBinder = ((ServiceBinder.LocalBinder) service).getService();
			Log.i(TAG, "Bound to ServiceBinder");
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
		}
	};

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				mConnected = false;
				mTitle = getString(R.string.disconnected);
				invalidateOptionsMenu();
			} 
			else if (BluetoothLeService.ACTION_DATA_AVAILABLE_BIOIMPEDANCE.equals(action)) {
				//Log.i(TAG, intent.getStringExtra(BluetoothLeService.EXTRA_DATA_BIOIMPEDANCE_STRING));
				values = intent.getDoubleArrayExtra(BluetoothLeService.EXTRA_DATA_BIOIMPEDANCE_DOUBLE);
				textFile.add(intent.getStringExtra(BluetoothLeService.EXTRA_DATA_BIOIMPEDANCE_STRING));

				// Find fragment hosted in the activity with the specified tag and update the plot
				BioimpFragment bioimpFragment = (BioimpFragment)
						getFragmentManager().findFragmentByTag(LIVE_DATA_TAG);
				if (bioimpFragment != null) {
					bioimpFragment.updatePlot(values);
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_long_term);
		Intent intent = getIntent();
		mDeviceAddress = intent.getStringExtra(DeviceControlActivity.EXTRA_DEVICE_ADDRESS_BINDER);
		mDeviceName = intent.getStringExtra(DeviceControlActivity.EXTRA_DEVICE_NAME_BINDER);

		Intent BLE_Intent = new Intent(this, BluetoothLeService.class);
		Intent ServiceBinderIntent = new Intent(this, ServiceBinder.class);
		bindService(BLE_Intent, mServiceConnectionBLE, BIND_AUTO_CREATE);
		bindService(ServiceBinderIntent, mServiceConnectionBioImp, BIND_AUTO_CREATE);
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

		mNavigationDrawerFragment = (NavigationDrawerFragment)
				getFragmentManager().findFragmentById(R.id.navigation_drawer);
		mTitle = getTitle();

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(
				R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));
	}

	@Override
	protected void onStop() {
		super.onStop();
		finish();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Toast.makeText(this, "Long term sampling off", Toast.LENGTH_SHORT).show();
		unregisterReceiver(mGattUpdateReceiver);
		unbindService(mServiceConnectionBLE);
		unbindService(mServiceConnectionBioImp);
	}

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		Log.i(TAG, String.valueOf(position));
		// update the main content by replacing fragments
		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		switch (position) {
		// Depending on the item selected in the list, add unique tags / identifiers.
		case 0:
			fragmentTransaction
			.replace(R.id.container, BioimpFragment.newInstance(position + 1), LIVE_DATA_TAG);			
			break;
		case 1:
			fragmentTransaction
			.replace(R.id.container, BioimpFragment.newInstance(position + 1), PAST_HOUR_TAG);			
			break;
		case 2:
			fragmentTransaction
			.replace(R.id.container, BioimpFragment.newInstance(position + 1), PAST_DAY_TAG);			
			break;
		case 3:
			fragmentTransaction
			.replace(R.id.container, BioimpFragment.newInstance(position + 1), CUSTOM_RANGE_TAG);			
			break;
		case 4:
			fragmentTransaction
			.replace(R.id.container, BioimpFragment.newInstance(position + 1), SCAN_TAG);			
			break;
		}
		fragmentTransaction.commit();
	}

	public void onSectionAttached(int number) {
		switch (number) {
		// When a fragment is attached to the hosting activity, pass the connected device's name.
		case 1:
			BioimpFragment bioimpFrag = (BioimpFragment)
			getFragmentManager().findFragmentByTag(LIVE_DATA_TAG);
			Log.i(TAG, mDeviceName + ".");
			bioimpFrag.setDeviceName(mDeviceName);
			bioimpFrag.setDeviceAddress(mDeviceAddress);

			if(mConnected == true) {
				mTitle = getString(R.string.live_data);
			}
			else {
				mTitle = getString(R.string.disconnected);
			}
			break;
		case 2:
			if(mConnected == true) {
				mTitle = getString(R.string.past_hour);
			}
			else {
				mTitle = getString(R.string.disconnected);
			}
			break;
		case 3:
			if(mConnected == true) {
				mTitle = getString(R.string.past_day);
			}
			else {
				mTitle = getString(R.string.disconnected);
			}
			break;
		case 4:
			if(mConnected == true) {
				mTitle = getString(R.string.set_time_range);
			}
			else {
				mTitle = getString(R.string.disconnected);
			}
			break;
		case 5:
			if(mConnected == true) {
				mTitle = getString(R.string.back_to_scan);
			}
			else {
				mTitle = getString(R.string.disconnected);
			}
			mBluetoothLeService.stopForeground(true);
			mBluetoothLeService.disconnect();
			Intent intent = new Intent(this, ServiceBinder.class);
			stopService(intent);
			Intent scan = new Intent(this, Scan.class);
			startActivity(scan);
			finish();
			break;
		}
	}

	public void restoreActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mNavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			getMenuInflater().inflate(R.menu.long_term, menu);
			restoreActionBar();
			if (mConnected) {    	
				menu.findItem(R.id.menu_disconnect).setVisible(true);
			} else {
				menu.findItem(R.id.menu_disconnect).setVisible(false);
			}
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {

		case R.id.menu_disconnect:
			mBluetoothLeService.stopForeground(true);
			mBluetoothLeService.disconnect();
			Intent intent = new Intent(this, ServiceBinder.class);
			stopService(intent);
			Log.i(TAG, "Attempted disconnect.");
			//mBluetoothLeService.close();
			return true;
		case android.R.id.home:
			onBackPressed();
			return true;
		case R.id.name_text_file:
			setTextFileName();
			Log.i(TAG, "Attempted text file naming.");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void clearTextFile() {		
		textFile = new ArrayList<String>();
		Toast.makeText(getBaseContext(),
				"Database sucessfully purged",
				Toast.LENGTH_SHORT).show();
	}

	public void exportToText() {
		// write on SD card file data in the text box
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
			String strDate = sdf.format(c.getTime());

			// check if User has changed filename
			if (checkNamedTextFile == false) {
				textFileName = "AccelData" + strDate;
			}
			else {
				textFileName = dialog.getName();
			}
			// set checkNamedTextFile back to false to revert back to default naming scheme.
			checkNamedTextFile = false;

			//			File accelData = new File(Environment.getExternalStorageDirectory() 
			//					+ textFileName + ".txt");

			File accelDataDir = new File(Environment.getExternalStorageDirectory() + "/Mime/");	

			accelDataDir.mkdirs();			

			File accelData = new File(accelDataDir, textFileName + ".txt");			

			//			File accelData = new File("/sdcard/" 
			//					+ textFileName + ".txt");

			accelData.createNewFile();
			FileOutputStream fOut = new FileOutputStream(accelData);
			OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);

			myOutWriter.append(
					fixedLengthString("X", 6) 
					+ fixedLengthString("Y", 6)
					+ fixedLengthString("Z", 6)
					+ fixedLengthString("I", 7)
					+ "\n");

			for (int i = 0; i < textFile.size(); i++) {
				myOutWriter.append(textFile.get(i));
				myOutWriter.append("\n");
			}

			myOutWriter.close();
			fOut.close();
			Toast.makeText(getBaseContext(),
					"Done writing to SD Card " + textFileName + ".txt",
					Toast.LENGTH_SHORT).show();

			textFile = new ArrayList<String>();
		} 
		catch (Exception e) {
			Toast.makeText(getBaseContext(), e.getMessage(),
					Toast.LENGTH_SHORT).show();
		}
	}

	public void setTextFileName() {
		showTextFileDialog();
	}

	public static String fixedLengthString(String string, int length) {
		return String.format("%-"+length+ "s", string);
	}

	public void showTextFileDialog() {
		// Create an instance of the dialog fragment and show it
		dialog = new NameTextFileFragment();
		dialog.show(getFragmentManager(), "NameTextFileFragment");
	}

	// The dialog fragment receives a reference to this Activity through the
	// Fragment.onAttach() callback, which it uses to call the following methods
	// defined by the NoticeDialogFragment.NoticeDialogListener interface
	@Override
	public void onDialogPositiveClick(DialogFragment dialog) {
		// User touched the dialog's positive button
		//checkNamedTextFile
		checkNamedTextFile = true;
	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
		// User touched the dialog's negative button
		checkNamedTextFile = false;
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE_BIOIMPEDANCE);
		return intentFilter;
	}


}
