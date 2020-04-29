package com.vikas.nfc.utils;

import android.content.Context;


public class CoffeeMachinePreferences {

    private final static String PREFS_NAME = "CoffeeMachine";
    private final static String WATER_LEVEL = "WaterLevel";
    private final static String COFFEE_LEVEL = "CoffeeLevel";
    private final static String GLASS_POSITION = "GlassPosition";
    private final static String TURN_ON_OFF = "TurnOnOff";

    /**
     * @param context
     * @param name    to be set as username.
     */
    public static void setWaterLevel(final Context context,
                                     final String name) {
        final android.content.SharedPreferences settings = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);
        final android.content.SharedPreferences.Editor editor = settings.edit();

        editor.putString(WATER_LEVEL, name);
        editor.commit();
    }

    /**
     * @param context
     * @return user name
     */
    public static String getWaterLevel(Context context) {
        final android.content.SharedPreferences sharedPref = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);

        return sharedPref.getString(WATER_LEVEL, "none");
    }

    /**
     * @param context
     * @param name    to be set as username.
     */
    public static void setCoffeeLevel(final Context context,
                                      final String name) {
        final android.content.SharedPreferences settings = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);
        final android.content.SharedPreferences.Editor editor = settings.edit();

        editor.putString(COFFEE_LEVEL, name);
        editor.commit();
    }

    /**
     * @param context
     * @return user name
     */
    public static String getCoffeeLevel(Context context) {
        final android.content.SharedPreferences sharedPref = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);

        return sharedPref.getString(COFFEE_LEVEL, "none");
    }

    /**
     * @param context
     * @param name    to be set as username.
     */
    public static void setGlassPosition(final Context context,
                                      final String name) {
        final android.content.SharedPreferences settings = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);
        final android.content.SharedPreferences.Editor editor = settings.edit();

        editor.putString(GLASS_POSITION, name);
        editor.commit();
    }

    /**
     * @param context
     * @return user name
     */
    public static String getGlassPosiion(Context context) {
        final android.content.SharedPreferences sharedPref = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);

        return sharedPref.getString(GLASS_POSITION, "none");
    }

    /**
     * @param context
     * @param name    to be set as username.
     */
    public static void setTurnOnOff(final Context context,
                                        final String name) {
        final android.content.SharedPreferences settings = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);
        final android.content.SharedPreferences.Editor editor = settings.edit();

        editor.putString(TURN_ON_OFF, name);
        editor.commit();
    }

    /**
     * @param context
     * @return user name
     */
    public static String getTurnOnOff(Context context) {
        final android.content.SharedPreferences sharedPref = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);

        return sharedPref.getString(TURN_ON_OFF, "0");
    }
}
