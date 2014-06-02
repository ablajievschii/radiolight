package ru.radiolight.radio;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
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

	LinearLayout llLogo = null;// , llMedia, llMain,llSocials, llStreams;
	// ImageView ivBackgroundLogo;

	// consts
	static final int SVET = 1;
	static final int SVOBODA = 2;
	static final int MIR = 3;
	static final String KEY = "stream";

	static boolean UPDATING = false;
	static int streamToPlay = -1;
	static String URL_PLAY = "";

	// toggle buttons
	static boolean PLAY = false;
	// static boolean VOLUME_ON = true;
	static boolean RUS = true;

	// UI elements
	private Button buttonSvet, buttonSvoboda, buttonMir;
	private Button buttonStartPlay, buttonStopPlay, buttonSpeaker;
	private Button buttonHome, buttonLang, buttonFacebook, buttonTwitter,
			buttonVkontakte;
	private TextView tvSong;

	AudioManager audioManager;
	int maxVolume, curVolume;
	SeekBar volControl;

	Intent startServiseIntent = null;
	Intent browserIntent = null;
	ServiceConnection sConn;
	RadioService radioService;
	boolean bound = false;

	// message from service
	private BroadcastReceiver mReceiver;

	private final static int FONT_SIZE_SMALL = 24;

	private final static String APPLICATION_DATA = "applicationData";
	private SharedPreferences applicationData;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.i(TAG, "onCreate Activity");
		// Log.i("MAXMEMORY","max memory = "+Runtime.getRuntime().maxMemory());

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.main);

		// ///////
		initializeUIElements();

		initializeVolumeControls();
		
		

		// StrictMode.ThreadPolicy policy = new
		// StrictMode.ThreadPolicy.Builder()
		// .permitNetwork().build();
		// StrictMode.setThreadPolicy(policy);
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
			//
			// Intent startMain = new Intent(Intent.ACTION_MAIN);
			// startMain.addCategory(Intent.CATEGORY_HOME);
			// startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			// startActivity(startMain);
		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent startMain = new Intent(Intent.ACTION_MAIN);
			startMain.addCategory(Intent.CATEGORY_HOME);
			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(startMain);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause Activity");
		this.unregisterReceiver(this.mReceiver);
		this.mReceiver = null;
		unbind();
		if (!PLAY) {
			finish();
		}
		savePrivateData();
		super.onPause();
	}

	private void savePrivateData() {
		Editor editor = applicationData.edit();
		editor.putBoolean("languageData", RUS);
		editor.commit();
	}

	@Override
	protected void onStop() {
		Log.i(TAG, "onStop Activity");
		super.onStop();
	}

	@Override
	protected void onStart() {
		Log.i(TAG, "onStart Activity");
		super.onStart();
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume Activity");
		curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		volControl.setProgress(curVolume);
		if (curVolume == 0) {
			buttonSpeaker.setBackgroundResource(R.drawable.speaker_off_32x32);
		}
		if (streamToPlay != -1)
			changeColor(streamToPlay);

		sConn = new ServiceConnection() {

			@Override
			public void onServiceConnected(ComponentName name, IBinder binder) {
				// TODO Auto-generated method stub
				Log.i(TAG, "onServiceConnected");
				radioService = ((RadioService.MyBinder) binder).getService();
				PLAY = radioService.isPlaying();
				Log.i(TAG, "isPlaying = " + PLAY);
				if (PLAY)
					buttonStartPlay
							.setBackgroundResource(R.drawable.play_off_32x32);
				streamToPlay = RadioService.stream;
				changeColor(streamToPlay);

				bound = true;

			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				// TODO Auto-generated method stub
				Log.i(TAG, "onServiceDisconnected ");
				Log.i(TAG, "isPlaying = " + PLAY);
				unbind();
			}

		};
		// Toast.makeText(RadioAvtivity.this, "PLAY = " + PLAY,
		// Toast.LENGTH_SHORT).show();
		if (PLAY) {
			bindService(new Intent(RadioActivity.this, RadioService.class),
					sConn, 0);
		} else {
			buttonStartPlay.setBackgroundResource(R.drawable.play_on_32x32);
		}

		// ////////////
		IntentFilter intentFilter = new IntentFilter(
				"android.intent.action.MAIN");
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				// extract our message from intent
				String msg_service = intent.getStringExtra("msg");
				// log our message value
				Log.i(TAG, "Activity - > onReceve : " + msg_service);
				if (msg_service != null)
					if (msg_service.equals(RadioService.SERVICE_STOPED)) {
						stopPlay();
					} else {
						tvSong.setText(msg_service);
					}

			}
		};
		// registering our receiver
		this.registerReceiver(mReceiver, intentFilter);
		// ////////////
		super.onResume();
	}

	private void initializeVolumeControls() {
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

		volControl.setMax(maxVolume);
		volControl.setProgress(curVolume);
		volControl
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

					@Override
					public void onStopTrackingTouch(SeekBar arg0) {
					}

					@Override
					public void onStartTrackingTouch(SeekBar arg0) {
					}

					@Override
					public void onProgressChanged(SeekBar arg0, int arg1,
							boolean arg2) {
						audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
								arg1, 0);

						// Toast.makeText(RadioAvtivity.this, "curVolume = " +
						// arg1, Toast.LENGTH_SHORT).show();
						if (arg1 > 0) {
							curVolume = arg1;
							buttonSpeaker
									.setBackgroundResource(R.drawable.speaker_on_32x32);

						} else if (arg1 == 0) {
							buttonSpeaker
									.setBackgroundResource(R.drawable.speaker_off_32x32);

						}
					}
				});
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

		tvSong = (TextView) findViewById(R.id.tvSong);
		tvSong.setFocusable(true);

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

		volControl = (SeekBar) findViewById(R.id.volumeControl);

		// set backgrounds

		buttonStartPlay.setBackgroundResource(R.drawable.play_on_32x32);
		buttonStopPlay.setBackgroundResource(R.drawable.stop_on);
		buttonSpeaker.setBackgroundResource(R.drawable.speaker_on_32x32);
		// social
		buttonHome.setBackgroundResource(R.drawable.home_button_32x32);
		buttonLang.setBackgroundResource(R.drawable.lang_back_32x32);
		buttonLang.setText("en");

		Typeface tf = Typeface.createFromAsset(getAssets(), "iskoola_pota.otf");
		tvSong.setTypeface(tf);
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
        Log.i("Resolution",
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
			if ((PLAY && v.getId() != R.id.buttonStartPlay)) {
				buttonStopPlay.setBackgroundResource(R.drawable.stop_off);
				startServiseIntent = new Intent(RadioActivity.this,
						RadioService.class);
				startServiseIntent.setFlags(-1);
				stopService(startServiseIntent);
				// Toast.makeText(this, "stoped", Toast.LENGTH_SHORT).show();
			}

		switch (v.getId()) {
		case R.id.buttonSvet:
			Log.i(TAG, "service ���� choosed");
			streamToPlay = SVET;
			URL_PLAY = Constants.URL_SVET;
			PLAY = false;
			startPlay();
			// ////////////////////////
			buttonSvet.setTextColor(Color.RED);
			buttonSvoboda.setTextColor(Color.BLACK);
			buttonMir.setTextColor(Color.BLACK);

			//
			break;
		case R.id.buttonSvoboda:
			Log.i(TAG, "service ������� choosed");
			streamToPlay = SVOBODA;
			URL_PLAY = Constants.URL_SVOBODA;
			PLAY = false;
			startPlay();
			// ////////////////////////
			buttonSvet.setTextColor(Color.BLACK);
			buttonSvoboda.setTextColor(Color.RED);
			buttonMir.setTextColor(Color.BLACK);

			//
			break;
		case R.id.buttonMir:
			Log.i(TAG, "service ��� choosed");
			streamToPlay = MIR;
			URL_PLAY = Constants.URL_MIR;
			PLAY = false;
			startPlay();
			// ////////////////////////
			buttonSvet.setTextColor(Color.BLACK);
			buttonSvoboda.setTextColor(Color.BLACK);
			buttonMir.setTextColor(Color.RED);

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
			int volume = audioManager
					.getStreamVolume(AudioManager.STREAM_MUSIC);
			if (volume == 0) {
				buttonSpeaker
						.setBackgroundResource(R.drawable.speaker_on_32x32);
				// audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
				audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
						curVolume, 0);
				volControl.setProgress(curVolume);
			} else {
				curVolume = audioManager
						.getStreamVolume(AudioManager.STREAM_MUSIC);
				audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
				volControl.setProgress(0);
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
			break;
		case SVOBODA:
			buttonSvoboda.setTextColor(Color.RED);
			break;
		case MIR:
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
		Log.i(TAG, "startPlay()");
		if (!PLAY
				&& Utils
						.checkInternetConnection(getApplicationContext())) {
			Log.i(TAG, "was not playing");
			if (streamToPlay != -1) {
				tvSong.setText("-");
				startServiseIntent = new Intent(RadioActivity.this,
						RadioService.class);
				startServiseIntent.setFlags(streamToPlay);
				startService(startServiseIntent);
				buttonStartPlay
						.setBackgroundResource(R.drawable.play_off_32x32);
				buttonStopPlay.setBackgroundResource(R.drawable.stop_on);
				PLAY = true;
			}
		}
	}

	private void stopPlay() {
		if (PLAY) {
			if (startServiseIntent == null) {
				startServiseIntent = new Intent(RadioActivity.this,
						RadioService.class);
			}
			stopService(startServiseIntent);
			buttonStopPlay.setBackgroundResource(R.drawable.stop_off);
			PLAY = false;
			buttonStartPlay.setBackgroundResource(R.drawable.play_on_32x32);
			if (streamToPlay == -1)
				startServiseIntent = null;
		}
	}

	private void unbind() {
		if (bound) {
			unbindService(sConn);
			bound = false;
		}
	}

}
