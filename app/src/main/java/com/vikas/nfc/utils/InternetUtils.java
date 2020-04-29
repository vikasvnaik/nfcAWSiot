package com.vikas.nfc.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.vikas.nfc.Controller.AWSConnection;
import com.vikas.nfc.R;

public class InternetUtils {

    public static final String LOG_TAG = InternetUtils.class.getCanonicalName();

    //Boolean to check if the app was already connected before losing connection
    private static boolean mLastStateConnected = true;
    /**
     * Method to check if there is Internet connection. If there is not, the app disconnects from
     * AWS. If then the phone reconnects to a valid network, is reconnects to AWS.
     *
     * @return if the device is connected to a valid network.
     */
    public static boolean hasInternetConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.
                        getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            ToastMessage.setToastMessage(context,
                    context.getString(R.string.no_internet) + " " + context.getString(R.string.please_connect),
                    Toast.LENGTH_LONG);
            Log.d(LOG_TAG, "Internet desconectada.");
            mLastStateConnected = false;
            AWSConnection.getInstance(context).disconnectAWS();
        } else {
            if (!mLastStateConnected) {
                AWSConnection.getInstance(context).getConnection();
                ToastMessage.setToastMessage(
                        context,
                        context.getString(R.string.internet_connected),
                        Toast.LENGTH_SHORT);
            }
            Log.d(LOG_TAG, "Internet OK");
            mLastStateConnected = true;
        }

        return isConnected;
    }
}
