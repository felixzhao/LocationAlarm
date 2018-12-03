package zhao.felix.locationalarmapp;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.provider.Settings;
import android.provider.SyncStateContract;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public final class AlarmReceiver extends BroadcastReceiver {
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        Toast.makeText(context, "Alarm worked.", Toast.LENGTH_LONG).show();
//    }


    MediaPlayer mp = null;// Here
    private static final String TAG = "VPET";

//    private boolean _soundStatus = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Loop running");

        boolean soundStatus = intent.getBooleanExtra(ArrivedActivity.EXTRA_MESSAGE, true);
        if(soundStatus) {
//            mp = MediaPlayer.create(context, Settings.System.DEFAULT_RINGTONE_URI);//Onreceive gives you context
//            mp.start();// and this to play it
        }else{
            if( mp != null && mp.isPlaying() == true){
                mp.stop();
            }
        }
    }
}