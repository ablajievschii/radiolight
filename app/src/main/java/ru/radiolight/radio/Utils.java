package ru.radiolight.radio;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

public class Utils {

    final static String TAG = "RL_Utils";
    // ////////////////Checking internet connection
    static boolean checkInternetConnection(Context appContext) {
        //Log.i(TAG, "checkInternetConnection()");
        final ConnectivityManager connMgr = (ConnectivityManager) appContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        final NetworkInfo wifi = connMgr
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final NetworkInfo mobile = connMgr
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifi.isAvailable() && wifi.isConnected()) {
            return true;
        } else if (mobile != null) {
            if (mobile.isAvailable() && mobile.isConnected()) {
                return true;
            } else {

                Toast.makeText(appContext, Constants.CHECK_CONNECTION,Toast.LENGTH_SHORT).show();
                return false;
            }

        } else {
            Log.i(TAG, "no internet connection!!!");
            Toast.makeText(appContext, Constants.CHECK_CONNECTION,Toast.LENGTH_SHORT).show();
            return false;
        }
		// }

		// if (wifi.isAvailable() && wifi.isConnected()) {
		// // Toast.makeText(this, "Wifi" , Toast.LENGTH_LONG).show();
		// return true;
		// } else if (mobile.isAvailable() && mobile.isConnected()) {
		// // Toast.makeText(this, "Mobile 3G " ,
		// // Toast.LENGTH_LONG).show();
		// return true;
		// } else {
		// Toast.makeText(appContext, "Check network connection",
		// Toast.LENGTH_SHORT).show();
		// return false;
		// }

	}
}
