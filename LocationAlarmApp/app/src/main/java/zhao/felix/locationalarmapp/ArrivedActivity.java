package zhao.felix.locationalarmapp;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ArrivedActivity extends Activity {
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;

    MediaPlayer mp = null;

    public static final String EXTRA_MESSAGE = "sound_status";

    public void cancelAlarm(View view)
    {
        Intent intent = new Intent(this, AlarmReceiver.class);

        PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        alarmManager.cancel(sender);

        Toast.makeText(this, "Cancelled alarm", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.arrived);

        Context context = getApplicationContext();

        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(EXTRA_MESSAGE, true);
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(), alarmIntent);

        Log.i("Arrived.", "Alarm Start! ");

        Toast.makeText(context
                , "Alarm Start! "
                , Toast.LENGTH_LONG).show();

        // play sound
        mp = MediaPlayer.create(context, Settings.System.DEFAULT_RINGTONE_URI);//Onreceive gives you context
        mp.start();// and this to play it

        TextView mMsg = (TextView) findViewById(R.id.ArrivedMsg);

        /// return main page after user click anywhere
        mMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelAlarm(view);

                // stop sound
                mp.stop();

                Context context = getApplicationContext();
                Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
                startActivity(intent);
            }
        });
    }
}
