package net.ar90n.logcatvisor;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceFragment;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();
	private static final String PREFERENCE_OPPACITY_KEY = "oppacity";
	private static final String PREFERENCE_VERTICAL_WEIGHT_KEY = "vertical_weight";
	private static final String PREFERENCE_VERTICAL_OFFSET_KEY = "vertical_offset";
	private static final String PREFERENCE_HORIZONTAL_WEIGHT_KEY = "horizontal_weight";
	private static final String PREFERENCE_HOZITONTAL_OFFSET_KEY = "horizontal_offset";
	private static final String PREFERENCE_SELECTED_FONT_SIZE_POSITION_KEY = "selected_font_size_position";
	private static final String PREFERENCE_SELECTED_FONT_COLOR_POSITION_KEY = "selected_font_color_position";
	private static final String PREFERENCE_DISPLAY_CONTENTS_CHECKBOX_STATE_KEY = "display_contents_checkbox_state";
	private static final String PREFERENCE_GREP_ENABLE_KEY = "grep_enable";
	private static final String PREFERENCE_GREP_TEXT_KEY = "grep_text";
	
	private ILoggerService mLoggerService = null;
	private int mOppacity;
	private int mVerticalLogLinesWeight;
	private int mHorizontalLogLinesWeight;
	private int mHorizontalLogLinesOffset;
	private int mVerticalLogLinesOffset;
	private int mFontSize;
	private int mSelectedFontSizePosition;
	private int mFontColor;
	private int mSelectedFontColorPosition;
	private int mDisplayContents;
	private int mDisplayContentsCheckBoxState;
	private boolean mGrepEnable = false;
	private String mGrepText = "";
	private Object mLockObect;
	private SharedPreferences mPreference;
	
	private static final int[] LOGCAT_COLUMN_POS = {
													0, // DATE
													1, // TIme
													2, // PID
													4, // Priority
													5, // Tag
													6, // Log
												   };
	
	private ServiceConnection mLoggerServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			try {
				mLoggerService.stopLogger();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mLoggerService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mLoggerService = ILoggerService.Stub.asInterface( service );
			try {
				mLoggerService.setOppacity( mOppacity);
				mLoggerService.setVerticalLogLinesWeight( mVerticalLogLinesWeight );
				mLoggerService.setVerticalLogLinesOffset( mVerticalLogLinesOffset );
				mLoggerService.setHorizontalLogLinesWeight( mHorizontalLogLinesWeight );
				mLoggerService.setHorizontalLogLinesOffset( mHorizontalLogLinesOffset );
				mLoggerService.setFontsize( mFontSize );
				mLoggerService.setFontColor( mFontColor );
				mLoggerService.enableGrep( mGrepEnable );
				mLoggerService.setGrepText( mGrepText );
				mLoggerService.setSelectedContents( mDisplayContents );
				
				mLoggerService.startLogger();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};
	
    private CompoundButton.OnCheckedChangeListener mEnableButtonListener = new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    	if( isChecked ) 
                    	{
                            startLogger();
                    	}
                    	else
                    	{
                    		stopLogger();
                    	}
            }
    };

	private void stopLogger() {
		if( mLoggerService != null ) {
		    try {
				mLoggerService.stopLogger();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			mLoggerServiceConnection.onServiceDisconnected(null);
			unbindService(mLoggerServiceConnection);
		}
	}

	private void startLogger() {
		if( mLoggerService == null ) {
			bindService( new Intent( getApplicationContext(), LoggerService.class), mLoggerServiceConnection, Context.BIND_AUTO_CREATE);
		}
		
		if( mLoggerService != null ) {
		    try {
				mLoggerService.setOppacity( mOppacity);
				mLoggerService.setVerticalLogLinesWeight( mVerticalLogLinesWeight );
				mLoggerService.setVerticalLogLinesOffset( mVerticalLogLinesOffset );
				mLoggerService.setHorizontalLogLinesWeight( mHorizontalLogLinesWeight );
				mLoggerService.setHorizontalLogLinesOffset( mHorizontalLogLinesOffset );
				
				mLoggerService.startLogger();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}    
    
	private SeekBar.OnSeekBarChangeListener mVerticalLogLinesWeightSliderListener = new SeekBar.OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			try {
				mVerticalLogLinesWeight = progress;
				
				if( mLoggerService != null )
				{
					mLoggerService.setVerticalLogLinesWeight( mVerticalLogLinesWeight );
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
				
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {}
	};
	
	private SeekBar.OnSeekBarChangeListener mVerticalLogLinesOffsetSliderListener = new SeekBar.OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			try {
				mVerticalLogLinesOffset = progress;
				
				if( mLoggerService != null )
				{
					mLoggerService.setVerticalLogLinesOffset( mVerticalLogLinesOffset );
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {}
	};
	
	private SeekBar.OnSeekBarChangeListener mHorizontalLogLinesWeightSliderListener = new SeekBar.OnSeekBarChangeListener() {
			
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			try {
				mHorizontalLogLinesWeight = progress;
				
				if( mLoggerService != null )
				{
					mLoggerService.setHorizontalLogLinesWeight( mHorizontalLogLinesWeight );
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {}
	};
	
	private SeekBar.OnSeekBarChangeListener mHorizontalLogLinesOffsetSliderListener = new SeekBar.OnSeekBarChangeListener() {
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			try {
				mHorizontalLogLinesOffset = progress;
				
				if( mLoggerService != null )
				{
					mLoggerService.setHorizontalLogLinesOffset( mHorizontalLogLinesOffset );
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
			
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {}
	};
	
    private SeekBar.OnSeekBarChangeListener mOpacitySliderListener = new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            	try {
            		mOppacity = progress;
            		
            		if( mLoggerService != null )
            		{
            			mLoggerService.setOppacity( mOppacity );
            		}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
    };
    
    private OnItemSelectedListener mFontsizeSpinnerListener = new  OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			Spinner spinenr = ( Spinner ) arg0;
			String fontsizeStr = (String)spinenr.getSelectedItem();
			mSelectedFontSizePosition = arg2;
			
			try {
				mFontSize = Integer.valueOf( fontsizeStr );
				
				if( mLoggerService != null )
				{
					mLoggerService.setFontsize( mFontSize );
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) { }
	};
	
	private OnItemSelectedListener mFontColorSpinnerListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			Spinner spinner = ( Spinner ) arg0;
			mSelectedFontColorPosition = spinner.getSelectedItemPosition();
			
			try {
				TypedArray ta = getResources().obtainTypedArray( R.array.fontcolor_value );
				mFontColor = ta.getColor( mSelectedFontColorPosition, 0);
				
				if( mLoggerService != null )
				{
					mLoggerService.setFontColor( mFontColor );
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {}
	};
	
	private CompoundButton.OnCheckedChangeListener mContentsCheckBoxListener = new CompoundButton.OnCheckedChangeListener() {
		
		@Override
		public void onCheckedChanged( CompoundButton checBox, boolean isChecked ) {
			int checkboxIndex = (Integer)checBox.getTag();
			int logcatPartMaskOffset = LOGCAT_COLUMN_POS[ checkboxIndex ];
			int logcatPartMask = 1 << logcatPartMaskOffset;
			int checkboxStateMask = 1 << checkboxIndex;

			if( isChecked )
			{
				mDisplayContents |= logcatPartMask;
				mDisplayContentsCheckBoxState |= checkboxStateMask;
			}
			else
			{
				mDisplayContents &= ~logcatPartMask;
				mDisplayContentsCheckBoxState &= checkboxStateMask;
			}

			try {
				if( mLoggerService != null )
				{
					mLoggerService.setSelectedContents( mDisplayContents );
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};
	
	private CompoundButton.OnCheckedChangeListener mGrepEnableButtonListener = new CompoundButton.OnCheckedChangeListener() {
		
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			synchronized (mLockObect) {
				mGrepEnable = isChecked;
				
				try {
					if( mLoggerService != null )
					{
						mLoggerService.enableGrep( mGrepEnable );
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	private TextWatcher mGrepTextWatcher = new TextWatcher() {
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		
		@Override
		public void afterTextChanged(Editable s)
		{
			synchronized ( mLockObect ) {
				mGrepText = s.toString();
				
				try {
					if( mLoggerService != null )
					{
						mLoggerService.setGrepText( mGrepText );
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	};

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.activity_main ); 
        
        mLockObect = new Object();
        
        mPreference = getPreferences( MODE_PRIVATE );
        mOppacity = mPreference.getInt( PREFERENCE_OPPACITY_KEY, 0 );
        mVerticalLogLinesWeight = mPreference.getInt( PREFERENCE_VERTICAL_WEIGHT_KEY, 0);
        mVerticalLogLinesOffset = mPreference.getInt( PREFERENCE_VERTICAL_OFFSET_KEY, 0);
        mHorizontalLogLinesWeight = mPreference.getInt( PREFERENCE_HORIZONTAL_WEIGHT_KEY, 0);
        mHorizontalLogLinesOffset = mPreference.getInt( PREFERENCE_HOZITONTAL_OFFSET_KEY, 0);
        mSelectedFontSizePosition = mPreference.getInt( PREFERENCE_SELECTED_FONT_SIZE_POSITION_KEY, 0);
        mSelectedFontColorPosition = mPreference.getInt( PREFERENCE_SELECTED_FONT_COLOR_POSITION_KEY, 0);
        mDisplayContentsCheckBoxState = mPreference.getInt( PREFERENCE_DISPLAY_CONTENTS_CHECKBOX_STATE_KEY, 0);
        mGrepEnable = mPreference.getBoolean( PREFERENCE_GREP_ENABLE_KEY, false);
        mGrepText = mPreference.getString( PREFERENCE_GREP_TEXT_KEY, "");
        
        ToggleButton tb = (ToggleButton) findViewById(R.id.enableButton);
        tb.setChecked( LoggerService.isRunning( this ) );
        tb.setOnCheckedChangeListener( mEnableButtonListener );
        
        Spinner fontSizeSpinner = ( Spinner )findViewById( R.id.fontSizeSpinner );
        fontSizeSpinner.setSelection( mSelectedFontSizePosition );
        fontSizeSpinner.setOnItemSelectedListener( mFontsizeSpinnerListener );
        mFontSize = getResources().obtainTypedArray( R.array.fontsize ).getInt( mSelectedFontSizePosition, 8);
        
        Spinner fontColorSpinner = ( Spinner )findViewById( R.id.fontColorSpinner );
        fontColorSpinner.setOnItemSelectedListener( mFontColorSpinnerListener );
        fontColorSpinner.setSelection( mSelectedFontColorPosition );
        mFontColor = getResources().obtainTypedArray( R.array.fontcolor_value).getIndex( mSelectedFontColorPosition );
        
        TableLayout tblLayout = ( TableLayout )findViewById( R.id.logContentCheckBox );
        int checkBoxCount = 0;
        for( int i = 0; i < tblLayout.getChildCount(); i++ )
        {
        	TableRow row = (TableRow) tblLayout.getChildAt(i);
        	final int rowChildCound = row.getChildCount();
        	for( int j = 0; j < rowChildCound; j++ )
        	{
        		CheckBox checkBox = (CheckBox) row.getChildAt( j );
        		checkBox.setOnCheckedChangeListener( mContentsCheckBoxListener );
        		checkBox.setTag( checkBoxCount );
        		if( ( mDisplayContentsCheckBoxState & ( 1 << checkBoxCount ) ) != 0 )
        		{
        			mDisplayContents |= ( 1 << LOGCAT_COLUMN_POS[ checkBoxCount ]);
        			checkBox.setChecked( true );
        		}
        		checkBoxCount++;
        	}
        }
        
        ToggleButton grepEnableButton = ( ToggleButton ) findViewById( R.id.grepEnableButton);
        grepEnableButton.setOnCheckedChangeListener( mGrepEnableButtonListener );
        grepEnableButton.setChecked( mGrepEnable );
        
        EditText grepEditText = ( EditText ) findViewById( R.id.grepEditText );
        grepEditText.addTextChangedListener( mGrepTextWatcher );
        grepEditText.setText( mGrepText );
        
        SeekBar horizontalLogLinesWeightSB = (SeekBar) findViewById( R.id.horizontalSizeSlider );
        horizontalLogLinesWeightSB.setOnSeekBarChangeListener( mHorizontalLogLinesWeightSliderListener );
        horizontalLogLinesWeightSB.setProgress( mHorizontalLogLinesWeight );        

        SeekBar verticalLogLinesWeightSB = (SeekBar) findViewById( R.id.logLinesSlider);
        verticalLogLinesWeightSB.setOnSeekBarChangeListener( mVerticalLogLinesWeightSliderListener );
        verticalLogLinesWeightSB.setProgress( mVerticalLogLinesWeight );        
        
        SeekBar horizontalLogLinesOffsetSB = ( SeekBar ) findViewById( R.id.horizontalOffsetSlider );
        horizontalLogLinesOffsetSB.setOnSeekBarChangeListener( mHorizontalLogLinesOffsetSliderListener );
        horizontalLogLinesOffsetSB.setProgress( mHorizontalLogLinesOffset );
        
        SeekBar verticalLogLinesOffsetSB = ( SeekBar ) findViewById( R.id.verticalOffsetSlider );
        verticalLogLinesOffsetSB.setOnSeekBarChangeListener( mVerticalLogLinesOffsetSliderListener );
        verticalLogLinesOffsetSB.setProgress( mVerticalLogLinesOffset );
        
        SeekBar sb = (SeekBar) findViewById(R.id.opacitySlider);
        sb.setOnSeekBarChangeListener( mOpacitySliderListener );
        sb.setProgress( mOppacity );
    }
    
    @Override
    public void onStart(){
        super.onStart();
    }
 
    @Override
    public void onResume(){
        super.onResume();
    }    
   
    @Override
    public void onDestroy() {
    	stopLogger();
    	
    	Editor e = mPreference.edit();
    	e.putInt( PREFERENCE_OPPACITY_KEY,  mOppacity );
    	e.putInt( PREFERENCE_VERTICAL_WEIGHT_KEY, mVerticalLogLinesWeight );
    	e.putInt( PREFERENCE_VERTICAL_OFFSET_KEY, mVerticalLogLinesOffset );
    	e.putInt( PREFERENCE_HORIZONTAL_WEIGHT_KEY, mHorizontalLogLinesWeight );
    	e.putInt( PREFERENCE_HOZITONTAL_OFFSET_KEY, mHorizontalLogLinesOffset );
    	e.putInt( PREFERENCE_SELECTED_FONT_SIZE_POSITION_KEY, mSelectedFontSizePosition );
    	e.putInt( PREFERENCE_SELECTED_FONT_COLOR_POSITION_KEY, mSelectedFontColorPosition );
    	e.putInt( PREFERENCE_DISPLAY_CONTENTS_CHECKBOX_STATE_KEY, mDisplayContentsCheckBoxState );
    	e.putBoolean( PREFERENCE_GREP_ENABLE_KEY, mGrepEnable );
    	e.putString( PREFERENCE_GREP_TEXT_KEY, mGrepText );
    	e.commit();
    	
    	super.onDestroy();
    }
    
    public static class LogcatVisorPreference extends PreferenceFragment {
        @Override
        public void onCreate( Bundle savedInstanceState ) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref);
        }
    }
}
