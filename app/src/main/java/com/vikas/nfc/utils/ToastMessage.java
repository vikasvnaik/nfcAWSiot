package com.vikas.nfc.utils;

import android.content.Context;
import android.widget.Toast;

public abstract class ToastMessage {

    /**
     * Instance of Toast class of android.
     */
    private static Toast sToastMessage;

    /**
     * Static toast message method to avoid instantiate more than one time.
     *
     * @param context  Context of application.
     * @param msg      String with message to show.
     * @param duration Duration of toast message.
     */
    public static void setToastMessage(final Context context,
                                       final String msg,
                                       final int duration) {
        //create toast message if it was not created previously
        if (sToastMessage == null) {
            sToastMessage = Toast.makeText(context, msg, duration);
        } else {
            sToastMessage.setText(msg);
            sToastMessage.setDuration(duration);
        }
        //show the toast message
        sToastMessage.show();
    }

    /**
     * Method to cancel a toast message.
     */
    public static void cancelToastMessage() {
        sToastMessage.cancel();
    }
}
