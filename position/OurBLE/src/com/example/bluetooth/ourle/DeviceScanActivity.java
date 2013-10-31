/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluetooth.ourle;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    HashMap<String, ArrayList<Integer>> mRssiDbMap;
    
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 5 seconds.
    private static final long SCAN_PERIOD = 5000;
    
    // TODO: initial values
    private static final String ADDRESS_0 = new String("C7:B9:5F:FC:29:82");
    private static final String ADDRESS_1 = new String("FA:30:A2:4F:E6:48");
    private static final double N_0 = 0.830482;
    private static final double N_1 = 0.498289;
    private static final int A_0 = 85;
    private static final int A_1 = 88;
    private static final PointF POS_0 = new PointF(0, 0);
    private static final PointF POS_1 = new PointF(20, 0);
    private static final RectF TARGET = new RectF(8, -5, 15, 5);

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        mRssiDbMap = new HashMap<String, ArrayList<Integer>>();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_report).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_report).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            case R.id.menu_report:
            	reportRSSI();
            	break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        return;
    }

    private class reportRSSITask extends AsyncTask<String,Void,Void> {
    	protected Void doInBackground(String...args) {
    		System.out.println("reportRSSItask:background:"+args.length);
    		
			try {
			  URL url = new URL(args[0]);
			  System.out.println("reportRSSItask:url:" + args[0]);
			  HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
			  InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			  if (in.available() != 0) {
			    byte[] buf = new byte[256];
			    in.read(buf);
			    System.out.println("Response: "+buf);
			  }
			  urlConnection.disconnect();
			} catch (MalformedURLException ex) {
				System.out.println("MalformedURLEx:"+ex.getMessage());
			} catch (IOException ex) {
				System.out.println("IOEx:"+ex.getMessage());
			}
    		return null;
    	} // doInBackground
    	
    	protected void onPostExecute(Void voids) {
    		System.out.println("reportRSSItask:post");
    	}
    };
    
	public static double getDistRSSI(double n, int a, int rssi) {
		return Math.pow(10.0, -(a+rssi)/(10*n));
	}
	
	public static double getDistRSSI(double n, int a, ArrayList<Integer> rssiDb) {
		// TODO: magic
		// NOTE: these coefficients must sum to 1
		double coef[] = { 0.96, 0.02, 0.01, 0.005, 0.003, 0.001, 0.001, 0, 0, 0 };
		while (rssiDb.size() > 10) {
			rssiDb.remove(0);
		}
		double eRSSI = 0;
		for (int i = 0;i < rssiDb.size();i ++) {
			eRSSI += rssiDb.get(rssiDb.size() - i - 1) * coef[i];
		}
		return getDistRSSI(n, a, (int)eRSSI);
	}
	
	public static int findPosition(PointF c1, double r1, PointF c2, double r2, PointF p1, PointF p2)
	{
		// http://blog.csharphelper.com/2010/03/29/determine-where-two-circles-intersect-in-c.aspx
		double dx = c1.x - c2.x;
		double dy = c1.y - c2.y;
		double dist = Math.sqrt(dx*dx + dy*dy);
		
		if (dist > r1 + r2) {
			// the formed circles are too far apart to intersect
			return 0;
		}
		if (dist < Math.abs(r1 - r2)) {
			// one circle is within the other
			return 0;
		}
		if (dist == 0 && r1 == r2) {
			// circles coincide
			return 0;
		}
		
		double a = (r1*r1 - r2*r2 + dist*dist) / (2*dist);
		double h = Math.sqrt(r1*r1 - a*a);
		
		double cx2 = c1.x + a*(c2.x - c1.x)/dist;
		double cy2 = c1.y + a*(c2.y - c1.y)/dist;
		
		p1.set((float)(cx2 + h*(c2.y-c1.y)/dist),
				                  (float)(cy2 - h*(c2.x-c1.x)/dist));
		p2.set((float)(cx2 - h*(c2.y-c1.y)/dist),
                                  (float)(cy2 + h*(c2.x-c1.x)/dist));

		if (dist == r1 + r2) return 1;
		return 2;
	}
	
    private void reportRSSI() {
    	System.out.println("reportRSSI()");
    	
    	String toast = new String("");
    	if (mRssiDbMap.get(ADDRESS_0) == null || mRssiDbMap.get(ADDRESS_1) == null) {
    		toast = "NOT ENOUGH DEVICES";
    		Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
    		return;
    	}
    	double dist_0 = getDistRSSI(N_0, A_0, mRssiDbMap.get(ADDRESS_0));
    	double dist_1 = getDistRSSI(N_1, A_1, mRssiDbMap.get(ADDRESS_1));
    	PointF p1 = new PointF();
    	PointF p2 = new PointF();
    	int np = findPosition(POS_0, dist_0, POS_1, dist_1, p1, p2);
    	toast += "np:"+np;
    	boolean inTarget = false;
    	if (np > 0) {
    		toast += " p1:("+p1.x+","+p1.y+") ";
    		if (TARGET.contains(p1.x, p1.y)){
    			inTarget = true;
    		}
    		if (np == 2) {
    			toast += " p2:("+p2.x+","+p2.y+") ";
    			if (TARGET.contains(p2.x, p2.y)) {
    				inTarget = true;
    			}
    		}
    	}
    	toast += " d0:"+dist_0+" d1:"+dist_1;
    	String query_string = new String("");
    	if (inTarget) {
    		toast += " IN";
    		query_string = "Z";
    	} else {
    		query_string = "X";
    	}
    	Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
    	
		// TODO: Hard coded IPs/URLs
    	new reportRSSITask().execute("http://192.168.1.201/"+query_string);
    	new reportRSSITask().execute("http://192.168.1.202/"+query_string);
    }
    
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
    	private class BLEDevWithRSSI {
    		public BluetoothDevice dev;
    		public int rssi;
    		BLEDevWithRSSI(BluetoothDevice i_dev, int i_rssi) {
    			dev = i_dev;
    			rssi = i_rssi;
    		}
    	};
    	
        private ArrayList<BLEDevWithRSSI> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BLEDevWithRSSI>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device, int rssi) {
        	BLEDevWithRSSI rDev = new BLEDevWithRSSI(device, rssi);
            if(!mLeDevices.contains(rDev)) {
                mLeDevices.add(rDev);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position).dev;
        }
        
        public int getRSSI(int position) {
        	return mLeDevices.get(position).rssi;
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i).dev;
            int d_rssi = mLeDevices.get(i).rssi;
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress() + " RSSI:" + d_rssi);

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device, rssi);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    ArrayList<Integer> rssiDb = mRssiDbMap.get(device.getAddress());
                    if ( rssiDb == null ) {
                    	rssiDb = new ArrayList<Integer>();
                    	mRssiDbMap.put(device.getAddress(), rssiDb);
                    }
                    while (rssiDb.size() > 10) {
                    	rssiDb.remove(0);
                    }
                    rssiDb.add(rssi);
                    double sum = 0;
                    for (int i = 0;i < rssiDb.size();i ++) {
                    	sum += rssiDb.get(i);
                    }
                    System.out.println("Device - "+device.getAddress()+" avgRSSI: "+ (int)(sum/rssiDb.size()) );
                }
            });
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}