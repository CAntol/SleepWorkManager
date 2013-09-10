package com.antol.batterymanager;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;

/**
 * Created by CSAntol on 9/7/13.
 */
public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = AlarmReceiver.class.getSimpleName();
    protected static enum Alteration { ENABLE_NETWORK, DISABLE_NETWORK, ENABLE_RINGER, DISABLE_RINGER }
    protected static final String ALTER = "alter";

    @Override
    public void onReceive(Context context, Intent intent) {
        final SharedPreferences sp = context.getSharedPreferences(TAG, Activity.MODE_PRIVATE);
        if (sp.getBoolean(MainActivity.DISABLE_WEEKENDS, true)) {
            Calendar calendar = Calendar.getInstance();
            int day = calendar.get(Calendar.DAY_OF_WEEK);
            if (day == 1 || day == 7) {
                return;
            }
        }
        /*PowerManager powermanager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powermanager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();*/
        try {
            Bundle bundle = intent.getExtras();
            assert bundle != null;
            Alteration alteration = Alteration.valueOf(bundle.getString(ALTER));

            switch(alteration) {
                case ENABLE_NETWORK: enableNetwork(context, true);
                    break;
                case DISABLE_NETWORK: enableNetwork(context, false);
                    break;
                case ENABLE_RINGER: enableRinger(context, true);
                    break;
                case DISABLE_RINGER: enableRinger(context, false);
                    break;
            }

        } catch (Exception e) {
            Toast.makeText(context, context.getString(R.string.alarm_error), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        //wakeLock.release();
    }

    protected void cancelSleepAlarm(Context context, AlarmManager alarmManager) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, MainActivity.REQUEST_CODE_ENABLE_NETWORK, intent, 0);
        alarmManager.cancel(sender);
        sender = PendingIntent.getBroadcast(context, MainActivity.REQUEST_CODE_DISABLE_NETWORK, intent, 0);
        alarmManager.cancel(sender);
    }

    protected void cancelWorkAlarm(Context context, AlarmManager alarmManager) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, MainActivity.REQUEST_CODE_ENABLE_RINGER, intent, 0);
        alarmManager.cancel(sender);
        sender = PendingIntent.getBroadcast(context, MainActivity.REQUEST_CODE_DISABLE_RINGER, intent, 0);
        alarmManager.cancel(sender);
    }

    protected void cancelAlarm(Context context, AlarmManager alarmManager) {
        cancelSleepAlarm(context, alarmManager);
        cancelWorkAlarm(context, alarmManager);
    }

    private void enableNetwork(Context context, boolean enabled) {
        // http://developer.android.com/reference/android/net/wifi/WifiManager.html
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (wifiManager.isWifiEnabled() != enabled) {
            wifiManager.setWifiEnabled(enabled);
        }

        try {
            // http://stackoverflow.com/questions/3644144/how-to-disable-mobile-data-on-android
            final ConnectivityManager conManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final Class conManagerClass = Class.forName(conManager.getClass().getName());
            final Field iConnectivityManagerField = conManagerClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(conManager);
            final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);

            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);

            if (enabled) {
                Toast.makeText(context, context.getString(R.string.network_enabled), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, context.getString(R.string.network_disabled), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, context.getString(R.string.mobile_data_error), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }

    private void enableRinger(Context context, boolean enabled) {
        // http://developer.android.com/reference/android/media/AudioManager.html
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (enabled) {
            manager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        } else {
            manager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        }
    }
}
