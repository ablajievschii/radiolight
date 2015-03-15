package ru.radiolight.radio;

import java.io.IOException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class RadioService extends Service implements OnInfoListener,
        OnAudioFocusChangeListener, OnErrorListener {

    final static String TAG = "RL_RadioService";
    private MediaPlayer player;
//	 "http://94.25.53.133/nashe-128.mp3" 

    protected static final int TIMER_LIMIT = 12;

    final static String UPDATE = "updating";
    static boolean IS_PLAYING = false;
    String URL_PLAYING = "";
    String mArtistTitle = "-";

    static int stream = -1;
    private int timer = 0;
    private TelephonyManager tm;
    private Context mContext;

    MyBinder mBinder = new MyBinder();

    static final int NOTIFICATION_ID = 1;

// ////////////////////
    @Override
    public IBinder onBind(Intent intent) {
        // int flag = arg0.getFlags();
        // Toast.makeText(this, "flags = " + flag, Toast.LENGTH_SHORT).show();
        mUpdateMeta = true;
        return mBinder;
    }

    class MyBinder extends Binder {

        RadioService getService() {
            return RadioService.this;
        }
    }

    // //////////////////

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);

        mServiceMessenger = new Messenger(mServiceHandler);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
    // Notification starts
        initService(intent);
        // the START_STICKY return value will ensure
        // that the service stays running until you call
        // stopService() from our activity.
        return START_NOT_STICKY;
    }

    void initService(Intent intent){
        initNotification();

//        Messenger messenger = intent.getParcelableExtra(Constants.ACTIVITY_MESSENGER);
//        if (messenger != null) {
//            mActivityMessenger = messenger;
//        }

        stream = intent.getFlags();
        Log.d(TAG, "onStart , stream = " + stream);
        if ((stream != 0)) {
            initializeMediaPlayer();;
            if (Utils.checkInternetConnection(getApplicationContext())){
                startPlaying();
                getMetaSendMsg();
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        stopPlaying();
        cancelNotification();

        mServiceHandler.removeCallbacks(updateMetaRunnable);

        IS_PLAYING = false;
        if (mPhoneListener != null){
            tm.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    // ///////////////////////////////////////////////////
    @SuppressWarnings("deprecation")
    private void initNotification() {
//        NotificationManager  notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        CharSequence contentTitle = "RadioLight";
        CharSequence contentText = "Playing...";

        int icon = R.drawable.logo_16x16;
        CharSequence tickerText = "";
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, tickerText, when);
        notification.flags = Notification.FLAG_ONGOING_EVENT;
//      notification.setContentTitle(contentTitle).setContentText(contentText);

        Intent notificationIntent = new Intent(this, RadioActivity.class);

//      notification.setWhen(System.currentTimeMillis());
//      notification.setContentIntent(PendingIntent.getService(context
//          , 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT));
//
//
        PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
        notification.setLatestEventInfo(mContext, contentTitle, contentText,
            contentIntent);

        startForeground(NOTIFICATION_ID, notification);
//mNotificationManager.notify(NOTIFICATION_ID, notification.build());
    }

    private void updateNotification(){
        Log.d(TAG, "updateNotification");
    }

    private void cancelNotification() {
        stopForeground(true);
    }

    // ///////////////////////////////////////////////////////////////////////////////////////////////
    private void initializeMediaPlayer() {
        Log.d(TAG, "creating player, stream = " + stream);
        if (player != null) {
            player = null;
        }
        player = new MediaPlayer();
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        // ////////////////////////////

        try {
            switch (stream) {
            case RadioActivity.SVET:
                Log.d(TAG, "Started Svet : ");
                // The url to the shoutcast stream
                player.setDataSource(Constants.URL_SVET);
                URL_PLAYING = Constants.URL_SVET;
                break;
            case RadioActivity.SVOBODA:
                Log.d(TAG, "Started Svoboda : ");
                // The url to the shoutcast stream
                player.setDataSource(Constants.URL_SVOBODA);
                URL_PLAYING = Constants.URL_SVOBODA;
                break;
            case RadioActivity.MIR:
                Log.d(TAG, "Started Mir : ");
                // The url to the shoutcast stream
                player.setDataSource(Constants.URL_MIR);
                URL_PLAYING = Constants.URL_MIR;
                break;
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, e.getMessage());
        } catch (IllegalStateException e) {
            Log.w(TAG, e.getMessage());
        } catch (IOException e) {
            Log.w(TAG, e.getMessage());
        }

        player.setOnInfoListener(this);
        player.setOnErrorListener(this);

        player.setOnPreparedListener(new OnPreparedListener() {

            public void onPrepared(MediaPlayer mediaPlayer) {
                if (mediaPlayer != null){
                    mediaPlayer.start();
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                        mServiceHandler.postDelayed(updateMetaRunnable, 1000);
                    }
                } else {
                    Log.w(TAG, "Error: mediaPlayer is null");
                }
            }
        });

        player.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {

            public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent) {
                Log.d(TAG, "buffering: " + percent);
                timer++;
                if (timer == TIMER_LIMIT) {
                    getMetaSendMsg();
                    updateNotification();
                    timer = 0;
                }

                if (!Utils.checkInternetConnection(getApplicationContext()))
                    stopPlaying();
            }
        });
    }

    //final boolean updating = false;

    Runnable updateMetaRunnable = new Runnable() {
        @Override
        public void run() {
            getMetaSendMsg();
            mServiceHandler.sendEmptyMessage(Constants.UPDATE_META);
        }
    };

    void getMetaSendMsg(){
        Log.d(TAG, "getMetaSendMsg");
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mArtistTitle = GetMetaData.getMeta(URL_PLAYING);
                    Message msg = Message.obtain(null, Constants.UPDATE_TITLE);
                    msg.obj = mArtistTitle;
                    if (null != mActivityMessenger && mUpdateMeta) {
                        Log.d(TAG, "sending message to activity: " + mActivityMessenger);
                        mActivityMessenger.send(msg);
                    }
                    //Log.d(TAG, "CURENT RESOURCE: " + mArtistTitle);
                } catch (IOException e) {
                    Log.w(TAG,"getMetaSendMsg() Error: " + e.getMessage());
                }
                catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    Log.w(TAG, "Error sending message to update title: " + e.getMessage());
                }
            }
        });
        t.start();
    }

    void startPlaying() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int audioFocus = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (audioFocus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            try {
                IS_PLAYING = true;
                player.prepareAsync();
                //player.start();

            } catch (IllegalStateException e) {
                Log.w(TAG, "Could not start player");
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
    }

    void stopPlaying() {
        try {
            if (!player.isPlaying()) {
                Log.d(TAG, "player stoped, was not playing " + stream);
                IS_PLAYING = false;
            } else {
                player.stop();
                Log.d(TAG, "player stoped, was playing " + stream);
                // initializeMediaPlayer();
                IS_PLAYING = false;
            }
            resetPlayer();
        } catch (IllegalStateException e) {
            Log.w(TAG, e.getMessage());
        }
    }

    private void resetPlayer(){
        player.reset();
        player.release();
        player = null;
    }

    boolean isPlaying() {
        return IS_PLAYING;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        // if (focusChange > 0)
        Log.d(TAG, "focusChanged " + focusChange);
        switch (focusChange) {
        case AudioManager.AUDIOFOCUS_GAIN:
            // Toast.makeText(this, "Focus gained" , Toast.LENGTH_LONG).show();
            // resume playback
            Log.d(TAG, "AUDIOFOCUS_GAIN");
            if (player == null)
                initializeMediaPlayer();
            else if (!player.isPlaying())
                player.start();
            player.setVolume(1.0f, 1.0f);
            break;

        case AudioManager.AUDIOFOCUS_LOSS:
            // Toast.makeText(this, "Focus lost" , Toast.LENGTH_LONG).show();
            // Lost focus for an unbounded amount of time: stop playback and
            // release media player
            Log.d(TAG, "AUDIOFOCUS_LOSS");
            break;

        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            // Lost focus for a short time, but we have to stop
            // playback. We don't release the media player because playback
            // is likely to resume
            Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
            break;

        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
            // Lost focus for a short time, but it's ok to keep playing
            // at an attenuated level
            Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
            if (player.isPlaying())
                player.setVolume(0.1f, 0.1f);
            break;
        }

    }

    // ������� ������
    private PhoneStateListener mPhoneListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
            try {
                switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.d(TAG, "RINGING");
                    if (player.isPlaying())
                        player.pause();
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.d(TAG, "OFFHOOK");
                    if (player.isPlaying())
                        player.pause();
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    Log.d(TAG, "IDLE");
                    if (player == null)
                        initializeMediaPlayer();
                    else if (!player.isPlaying())
                        player.start();
                    player.setVolume(1.0f, 1.0f);
                    break;
                default:
                    Log.d(RadioService.TAG, "Unknown phone state = "
                            + state);
                }
            } catch (Exception e) {
                Log.w("Exception", "PhoneStateListener() e = " + e.getMessage());
            }
        }
    };

// /////////////////////////////////////////////////////
    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
     // Toast.makeText(getApplicationContext(), "onInfo - " + what
     // ,Toast.LENGTH_SHORT).show();
//		if (what == 703) {
//			stopPlaying();
//			if (checkInternetConnection(getApplicationContext())) {
//				initializeMediaPlayer();
//				startPlaying();
//			}
//		}
        return false;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (what != -38) {
            Log.d(TAG, "onError(): " + what);
            stopPlaying();
            if (Utils.checkInternetConnection(getApplicationContext())) {
                initializeMediaPlayer();
                if (what != 1){
                    startPlaying();
                } else {
                    Message msg = Message.obtain(null, Constants.SERVICE_STOPED);
                    /*
                    try {
                        Log.d(TAG, "Sending message STOP to activity");
                        //mActivityMessenger.send(msg);
                    } catch (RemoteException e) {
                        Log.w(TAG, "Error sending message to activity: " + e.getMessage());
                    }
                    */
//                    sendMsgToActivity(SERVICE_STOPED);
                    stopSelf();
                }
            }
        }
        return false;
    }
    // /////////////////////////////////////////////////


    private Messenger mServiceMessenger;
    private Messenger mActivityMessenger ;
    private ServiceHandler mServiceHandler = new ServiceHandler();
    private boolean mUpdateMeta = false;

    Messenger getMessenger() {return mServiceMessenger; }

    private class ServiceHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case Constants.UPDATE_META:
                    if (isPlaying() && mUpdateMeta){
                        removeCallbacks(updateMetaRunnable);
                        postDelayed(updateMetaRunnable, Constants.UPDATE_META_DELAY);
                    }
                    break;
                case Constants.MESSAGE_SERVICE_CONNECTION:
                    Log.d(TAG, "ServiceHandler: handleMessage() target = " + msg.replyTo);
                    mActivityMessenger = null;
                    mActivityMessenger = msg.replyTo;
                    Message m = Message.obtain(null, Constants.UPDATE_TITLE);
                    msg.obj = mArtistTitle;
                    Log.d(TAG, "trying to send message to activity: " + mActivityMessenger);
                    try {
                        mActivityMessenger.send(m);
                    } catch (Exception e) {
                    }
                    break;
                case Constants.ACTIVITY_STOPED:
                    mUpdateMeta = false;
                    break;
            }
        }
    }
}
