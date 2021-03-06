package ru.radiolight.radio;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.TextView;

public class RadioActivity extends Activity implements OnClickListener {

    private static final String TAG = "RL_RadioActivity";

    LinearLayout llLogo = null;

    // consts
    static final int SVET = 1;
    static final int SVOBODA = 2;
    static final int MIR = 3;
    static final String KEY = "stream";

    static boolean UPDATING = false;
    static int streamToPlay = -1;
    static String URL_PLAY = "";

    // toggle buttons
    boolean PLAY = false;
    // static boolean VOLUME_ON = true;
    static boolean RUS = true;

    // UI elements
    private Button buttonSvet, buttonSvoboda, buttonMir;
    private Button buttonStartPlay, buttonStopPlay, buttonSpeaker;
    private Button buttonHome, buttonLang, buttonFacebook, buttonTwitter, buttonVkontakte;
    private TextView mTvSong;

    private AudioManager mAudioManager;
    int maxVolume, mCurVolume, mBeforeMuteVolume = 0;
    private SeekBar mVolumeSeekBar;

    private Intent startServiceIntent = null;
    private Intent browserIntent = null;
    private RadioService mRadioService;
    boolean mBound = false;

    private ActivityHandler mActivityHandler = new ActivityHandler(this);
    private Messenger mActivityMessenger = new Messenger(mActivityHandler);


    private ServiceConnection sConn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            // TODO Auto-generated method stub
            Log.d(TAG, "onServiceConnected: mActivityMessenger = " + mActivityMessenger);

            RadioService.MyBinder serviceBinder = ((RadioService.MyBinder) binder);
            mRadioService = serviceBinder.getService();
            PLAY = mRadioService.isPlaying();
            Message msg = Message.obtain(null,Constants.MESSAGE_SERVICE_CONNECTION);
            msg.replyTo = mActivityMessenger;
            try {
                mRadioService.getMessenger().send(msg);
            } catch (RemoteException ex){
                Log.w(TAG, "no connection to service");
            }

            Log.d(TAG, "isPlaying = " + PLAY);
            if (PLAY)
                buttonStartPlay
                        .setBackgroundResource(R.drawable.play_off_32x32);
            streamToPlay = RadioService.stream;
            changeColor(streamToPlay);

            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            Log.d(TAG, "onServiceDisconnected ");
            Log.d(TAG, "isPlaying = " + PLAY);
            mBound = false;
        }

    };

    private final static int FONT_SIZE_SMALL = 24;

    private final static String APPLICATION_DATA = "applicationData";
    private SharedPreferences applicationData;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");
        // Log.i("MAXMEMORY","max memory = "+Runtime.getRuntime().maxMemory());

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);

        initializeUIElements();

        initializeVolumeControls();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater blowUp = getMenuInflater();
        blowUp.inflate(R.menu.my_menu, menu);
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.exit) {
            stopPlay();
            streamToPlay = -1;
            moveTaskToBack(true);
            System.runFinalizersOnExit(true);
            System.exit(0);
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_BACK:
                Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startMain);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                int volumeDown = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                volumeDown --;
                setSeekBarVolume(volumeDown);
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                int volumeUp = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                volumeUp ++;
                setSeekBarVolume(volumeUp);
                break;
        }
        return false;
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        if (!PLAY) {
            finish();
        }
        savePrivateData();
        super.onPause();
    }

    private void savePrivateData() {
        Editor editor = applicationData.edit();
        editor.putBoolean("languageData", RUS);
        editor.apply();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        Message msg = Message.obtain(null,Constants.ACTIVITY_STOPED);
        try {
            mRadioService.getMessenger().send(msg);
        } catch (RemoteException ex){
            Log.w(TAG, "no connection to service");
        }
        unbind();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        int volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        setSeekBarVolume(volume);
        if (volume == 0) {
            buttonSpeaker.setBackgroundResource(R.drawable.speaker_off_32x32);
        }
        if (streamToPlay != -1)
            changeColor(streamToPlay);

        boolean serviceRunning = Utils.isServiceRunning(RadioService.class, this);
        PLAY = serviceRunning;
        Log.d(TAG, "ServiceRunning: " + serviceRunning);
        if (!serviceRunning){
            buttonStartPlay.setBackgroundResource(R.drawable.play_on_32x32);
            buttonStopPlay.setBackgroundResource(R.drawable.stop_off);
        } else {
            Intent bindToServiceIntent = new Intent(this, RadioService.class);
            //bindToServiceIntent.putExtra(Constants.ACTIVITY_MESSENGER, mActivityMessenger);
            bindService(bindToServiceIntent, sConn, 0);

            buttonStartPlay.setBackgroundResource(R.drawable.play_off_32x32);
            buttonStopPlay.setBackgroundResource(R.drawable.stop_on);
        }
        super.onResume();
    }

    private void initializeVolumeControls() {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (null == mAudioManager) mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mCurVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mBeforeMuteVolume = mCurVolume;

        mVolumeSeekBar.setMax(maxVolume);
        setSeekBarVolume(mCurVolume);
        mVolumeSeekBar
            .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar arg0) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar arg0) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser) {
                    Log.d(TAG, "onProgressChanged(): volume changed = " + progress + " from user: " + fromUser);
                    mCurVolume = progress;
                    if (fromUser) {
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                                mCurVolume, 0);
                    }

                    if (mCurVolume > 0) {
                        buttonSpeaker
                                .setBackgroundResource(R.drawable.speaker_on_32x32);

                    } else if (mCurVolume == 0) {
                        buttonSpeaker
                                .setBackgroundResource(R.drawable.speaker_off_32x32);
                    } else {
                        Log.w(TAG, "Wrong volume value: " + mCurVolume);
                    }
                }
            });
    }

    private void setSeekBarVolume(int volume){
        Log.d(TAG, "setSeekBarVolume " + volume);

        mVolumeSeekBar.setProgress(volume);
    }

	private void initializeUIElements() {

		// llMain = (LinearLayout) findViewById(R.id.llMain);
		// llMedia = (LinearLayout) findViewById(R.id.llMedia);
		// llSocials = (LinearLayout) findViewById(R.id.llSocials);
		// llStreams = (LinearLayout) findViewById(R.id.llStreams);
		llLogo = (LinearLayout) findViewById(R.id.llLogo);

		//
		// ivBackgroundLogo = (ImageView) findViewById(R.id.ivBackgroundLogo);
		setBackground();

		mTvSong = (TextView) findViewById(R.id.tvSong);
		mTvSong.setFocusable(true);

		buttonSvet = (Button) findViewById(R.id.buttonSvet);
		buttonSvoboda = (Button) findViewById(R.id.buttonSvoboda);
		buttonMir = (Button) findViewById(R.id.buttonMir);
		buttonSvet.setOnClickListener(this);
		buttonSvoboda.setOnClickListener(this);
		buttonMir.setOnClickListener(this);

		// links to social
		buttonHome = (Button) findViewById(R.id.buttonHome);
		buttonLang = (Button) findViewById(R.id.buttonLang);
		buttonFacebook = (Button) findViewById(R.id.buttonFacebook);
		buttonTwitter = (Button) findViewById(R.id.buttonTwitter);
		buttonVkontakte = (Button) findViewById(R.id.buttonVkontakte);
		buttonHome.setOnClickListener(this);
		buttonLang.setOnClickListener(this);
		buttonFacebook.setOnClickListener(this);
		buttonTwitter.setOnClickListener(this);
		buttonVkontakte.setOnClickListener(this);

		// media buttons
		buttonStartPlay = (Button) findViewById(R.id.buttonStartPlay);
		buttonStopPlay = (Button) findViewById(R.id.buttonStopPlay);
		buttonSpeaker = (Button) findViewById(R.id.buttonSpeaker);
		buttonStartPlay.setOnClickListener(this);
		buttonStopPlay.setOnClickListener(this);
		buttonSpeaker.setOnClickListener(this);

        mVolumeSeekBar = (SeekBar) findViewById(R.id.volumeControl);

		// set backgrounds

		buttonStartPlay.setBackgroundResource(R.drawable.play_on_32x32);
		buttonStopPlay.setBackgroundResource(R.drawable.stop_on);
		buttonSpeaker.setBackgroundResource(R.drawable.speaker_on_32x32);
		// social
		buttonHome.setBackgroundResource(R.drawable.home_button_32x32);
		buttonLang.setBackgroundResource(R.drawable.lang_back_32x32);
		buttonLang.setText("en");

		Typeface tf = Typeface.createFromAsset(getAssets(), "iskoola_pota.otf");
		mTvSong.setTypeface(tf);
		buttonSvet.setTypeface(tf);
		buttonSvoboda.setTypeface(tf);
		buttonMir.setTypeface(tf);

		setFontSize(FONT_SIZE_SMALL);

		applicationData = getSharedPreferences(APPLICATION_DATA, 0);
		RUS = applicationData.getBoolean("languageData", true);
		setLanguage();
	}

    @SuppressWarnings("deprecation")
    private void setBackground() {
        Display disp = getWindowManager().getDefaultDisplay();
        Log.d("Resolution",
                "width = " + disp.getWidth() + "; height = " + disp.getHeight());
        llLogo.setLayoutParams(new LayoutParams(disp.getWidth() - 20, disp
                .getWidth() - 20));

    }

    private void setFontSize(int fontSize) {
        buttonSvet.setTextSize(fontSize);
        buttonSvoboda.setTextSize(fontSize);
        buttonMir.setTextSize(fontSize);
    }

    public void onClick(View v) {
        if (v.getId() != R.id.buttonHome && v.getId() != R.id.buttonSpeaker
                && v.getId() != R.id.buttonFacebook
                && v.getId() != R.id.buttonTwitter
                && v.getId() != R.id.buttonVkontakte
                && v.getId() != R.id.buttonLang)
            // bindService(startServiseIntent, null, 0);
        {
            if ((PLAY && v.getId() != R.id.buttonStartPlay)) {
                buttonStopPlay.setBackgroundResource(R.drawable.stop_off);
                startServiceIntent = new Intent(RadioActivity.this, RadioService.class);
                startServiceIntent.setFlags(-1);
                stopService(startServiceIntent);
                // Toast.makeText(this, "stoped", Toast.LENGTH_SHORT).show();
            }
        }

        switch (v.getId()) {
        case R.id.buttonSvet:
            Log.d(TAG, "service Svet chosen");
            streamToPlay = SVET;
            URL_PLAY = Constants.URL_SVET;
            PLAY = false;
            startPlay();
            // ////////////////////////
            changeColor(streamToPlay);
            //
            break;
        case R.id.buttonSvoboda:
            Log.d(TAG, "service Svoboda chosen");
            streamToPlay = SVOBODA;
            URL_PLAY = Constants.URL_SVOBODA;
            PLAY = false;
            startPlay();
            // ////////////////////////
            changeColor(streamToPlay);
            //
            break;
        case R.id.buttonMir:
            Log.d(TAG, "service Mir chosen");
            streamToPlay = MIR;
            URL_PLAY = Constants.URL_MIR;
            PLAY = false;
            startPlay();
            // ////////////////////////
            changeColor(streamToPlay);

            //
            break;
        case R.id.buttonStartPlay:
            startPlay();
            break;
        case R.id.buttonStopPlay:
            // myService.stopPlaying();
            stopPlay();
            break;
        case R.id.buttonSpeaker:
            if (mCurVolume == 0) {
                if (mBeforeMuteVolume > 0) {
                    buttonSpeaker.setBackgroundResource(R.drawable.speaker_on_32x32);
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mBeforeMuteVolume, 0);
                    setSeekBarVolume(mBeforeMuteVolume);
                }
            } else {
                mBeforeMuteVolume = mCurVolume;
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                setSeekBarVolume(0);
                buttonSpeaker
                     .setBackgroundResource(R.drawable.speaker_off_32x32);
            }
            break;
        case R.id.buttonHome:
            browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://www.radiolight.ru/"));
            startActivity(browserIntent);
            break;
        case R.id.buttonFacebook:
            Intent browserIntent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.facebook.com/radioteam.radiolight?fref=ts"));
            startActivity(browserIntent);
            break;
        case R.id.buttonTwitter:
            browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://www.radiolight.ru/"));
            startActivity(browserIntent);
            break;
        case R.id.buttonVkontakte:
            browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://vk.com/id136108401"));
            startActivity(browserIntent);
            break;
        case R.id.buttonLang:
            changeLanguage();
            break;
        }
    }

    private void changeColor(int str) {
        switch (str) {
        case SVET:
            buttonSvet.setTextColor(Color.RED);
            buttonSvoboda.setTextColor(Color.BLACK);
            buttonMir.setTextColor(Color.BLACK);
            break;
        case SVOBODA:
            buttonSvet.setTextColor(Color.BLACK);
            buttonSvoboda.setTextColor(Color.RED);
            buttonMir.setTextColor(Color.BLACK);
            break;
        case MIR:
            buttonSvet.setTextColor(Color.BLACK);
            buttonSvoboda.setTextColor(Color.BLACK);
            buttonMir.setTextColor(Color.RED);
            break;
        }
    }

    private void changeLanguage() {
        if (buttonLang.getText().toString().equals("ru")) {
            RUS = true;
            setLanguage();
        } else {
            RUS = false;
            setLanguage();
        }
    }

    private void setLanguage() {
        if (RUS) {
            buttonLang.setText("en");
            buttonSvet.setText(R.string.svet);
            buttonSvoboda.setText(R.string.svoboda);
            buttonMir.setText(R.string.mir);
            changeColor(streamToPlay);
        } else {
            buttonLang.setText("ru");
            buttonSvet.setText(R.string.light);
            buttonSvoboda.setText(R.string.freedom);
            buttonMir.setText(R.string.world);
            changeColor(streamToPlay);
        }
    }

    // ////////////////////////////////////////////////////////

    // ////////////////////////////////////////////////////////
    private void startPlay() {
        Log.d(TAG, "startPlay");
        if (!PLAY
            && Utils.checkInternetConnection(getApplicationContext())) {
            Log.d(TAG, "was not playing");
            if (streamToPlay != -1) {
                mTvSong.setText("-");
                startServiceIntent = new Intent(RadioActivity.this,
                        RadioService.class);
                startServiceIntent.setFlags(streamToPlay);
                //startServiseIntent.putExtra(Constants.ACTIVITY_MESSENGER, mActivityMessenger);
                startService(startServiceIntent);
                bindService(startServiceIntent, sConn, 0);
                buttonStartPlay
                        .setBackgroundResource(R.drawable.play_off_32x32);
                buttonStopPlay.setBackgroundResource(R.drawable.stop_on);
                PLAY = true;
            } else {
                Log.w(TAG, "no stream selected to play");
            }
        }
    }

    protected void stopPlay() {
        if (PLAY) {
            Log.d(TAG, "stopPlay()");
            if (startServiceIntent == null) {
                startServiceIntent = new Intent(RadioActivity.this,
                     RadioService.class);
            }
            stopService(startServiceIntent);
            buttonStopPlay.setBackgroundResource(R.drawable.stop_off);
            PLAY = false;
            buttonStartPlay.setBackgroundResource(R.drawable.play_on_32x32);
            if (streamToPlay == -1)
                startServiceIntent = null;
        }
    }

    protected void updateTitle(String title){
        Log.d(TAG, "updateTitle(): " + title);
        if (mRadioService != null && !mTvSong.getText().equals(title)){
            mTvSong.setText(title);
        }
    }

    private void unbind() {
        Log.d(TAG, "unbind: " + mBound);
        if (mBound) {
            unbindService(sConn);
            mBound = false;
        }
    }

}
