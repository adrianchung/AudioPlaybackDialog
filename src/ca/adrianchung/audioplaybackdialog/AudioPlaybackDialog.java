package ca.adrianchung.audioplaybackdialog;
/*
 * The application needs to have the permission to write to external storage
 * if the output file is written to the external storage, and also the
 * permission to record audio. These permissions must be set in the
 * application's AndroidManifest.xml file, with something like:
 *
 * <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
 *
 */


import java.io.IOException;

import android.app.Dialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A simple dialog window for playing back audio media. 
 * 
 * @author adrian.chung
 */
public class AudioPlaybackDialog extends Dialog implements View.OnClickListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener
{
    private static final String TAG = "AudioPlaybackDialog";
    private static String mFileName = null;

    private Button mPlayButton = null;
    private Button mCancelButton = null;
    private Button mStopButton = null;

    private ProgressBar mProgressBar = null;
    
    private TextView mTimerStartView = null;
    private TextView mTimerEndView = null;
    private String mTimerFormat = null;
    
    private MediaPlayer mPlayer = null;
    private int mDurationSeconds = 0;
    private int mProgress = 0;
    
    private enum State {
    	IDLE,
    	PLAYING
    }
    
    /**
     * Start in idle state, waiting to play audio
     */
    private State mState = State.IDLE;
    
    /**
     * This handler is used when we are playing the audio file. It will run periodically to
     * update the current duration so that we can see how far into the audio file we are.
     */
    final Handler mHandler = new Handler();
	Runnable mUpdateTimer = new Runnable() {
		@Override
		public void run() {
			updateTimerView();
		}
	};

    private void preparePlaying() {
    	mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.setOnErrorListener(this);
            mPlayer.setOnCompletionListener(this);
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed", e);
            stopPlaying();
            Toast.makeText(getContext(), R.string.err_playing, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startPlaying() {
    	if (mPlayer == null) {
    		preparePlaying();
    	}
    	
    	if (mPlayer != null) {
    		mPlayer.start();
    	}
    }

    private void stopPlaying() {
    	if (mPlayer != null) {
    		mPlayer.stop();
    		mPlayer.release();
    		mPlayer.release();
            mPlayer = null;
    	}
    }

    /**
     * Constructor. We could add a constructor to take in a file here if we'd like
     * @param context
     */
    public AudioPlaybackDialog(Context context) {
    	super(context);
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "my_audio_file.wav";
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        View view = getLayoutInflater().inflate(R.layout.audio_playback_dialog, null);
        setContentView(view);
        setTitle("Play");
        
        mPlayButton = (Button)findViewById(R.id.playButton);
        mPlayButton.setOnClickListener(this);
        
        mCancelButton = (Button)findViewById(R.id.cancelButton);
        mCancelButton.setOnClickListener(this);
        
        mStopButton = (Button)findViewById(R.id.stopButton);
        mStopButton.setOnClickListener(this);
        
        mProgressBar = (ProgressBar)findViewById(R.id.stateProgressBar);
        mProgressBar.setProgress(0);
        
        mTimerFormat = getContext().getResources().getString(R.string.timer_format);
        mTimerStartView = (TextView)findViewById(R.id.timerStartView);
        mTimerEndView = (TextView)findViewById(R.id.timerEndView);
        
        // Set the max for the progress bar
        preparePlaying();
        int durationMs = mPlayer.getDuration();
        mDurationSeconds = durationMs / 1000;
        Log.i(TAG, "Audio file duration: " + mDurationSeconds);
        
        updateUi();
        updateTimerView();
    }
    
    @Override
    public void onStop() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        
        super.onStop();
    }
    
    private void updateUi() {
    	switch(mState) {
    	case IDLE:
    		mPlayButton.setVisibility(View.VISIBLE);
    		mStopButton.setVisibility(View.GONE);
    		break;
    	case PLAYING:
    		mPlayButton.setVisibility(View.GONE);
    		mStopButton.setVisibility(View.VISIBLE);
    		break;
    	}
    	
    	mCancelButton.setVisibility(View.VISIBLE);
    }

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.playButton:
			Log.i(TAG, "playButton pressed");
			mState = State.PLAYING;
			startPlaying();
			mHandler.post(mUpdateTimer);
			break;
		case R.id.stopButton:
			Log.i(TAG, "stopButton pressed");
			mState = State.IDLE;
			stopPlaying();
			
			mProgressBar.setProgress(0);
			mTimerStartView.setText(String.format(mTimerFormat, 0, 0));
			
			break;
		case R.id.cancelButton:
			Log.i(TAG, "cancelButton pressed");
			mState = State.IDLE;
			this.dismiss();
			break;
		default:
			throw new RuntimeException("Did not understand view: " + v.getId());
		}
		
		updateUi();
	}
	
	/**
	 * Update the time and progress bar to reflect where we are in the audio file. It
	 * will queue itself to update the timer every second until the audio file is done
	 * playing, or if we stop the playback.
	 */
	private void updateTimerView() {
		Log.i(TAG, "updating timer view");
		String timeStr = null;
		
		switch(mState) {
		case IDLE:
			timeStr = String.format(mTimerFormat, mDurationSeconds / 60, mDurationSeconds % 60);
			mTimerEndView.setText(timeStr);
			mProgressBar.setMax(mDurationSeconds);
			mProgressBar.setProgress(0);
			mProgress = 0;
			break;
		case PLAYING:
			++mProgress;
			timeStr = String.format(mTimerFormat, mProgress / 60, mProgress % 60);
			mTimerStartView.setText(timeStr);
			mProgressBar.setProgress(mProgress);
			break;
		}
		
		if (mProgress != mDurationSeconds && mState == State.PLAYING) {
			mHandler.postDelayed(mUpdateTimer, 1000);
		}
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		stopPlaying();
		mState = State.IDLE;
		updateUi();
		updateTimerView();
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Toast.makeText(getContext(), R.string.err_playing, Toast.LENGTH_SHORT).show();
		stopPlaying();
		dismiss();
		return false;
	}
}