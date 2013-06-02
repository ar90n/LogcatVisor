package net.ar90n.logcatvisor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class LoggerService extends Service {
	
		private static final String TAG = LoggerService.class.getSimpleName();
		private static final int MAX_OPPACITY = 100;
		private static final float DEFAULT_FONT_SIZE = 8.0F;
		private static final int MAX_LOGLINE_SIZE = 1024;
		private static final int DEFAULT_FONT_COLOR = Color.BLACK;
		
        private WindowManager mWindowManager;

        private Thread mLogcatThread = null;
        private LinkedList< String[] > mLogMessageQueueParts = null;
        private Handler mHandler = null;
        
        private View mViews = null;
        private ListView mListView = null;
        private LinearLayout.LayoutParams mListViewLayoutParams = null;
        private LinearLayout.LayoutParams mListViewWrapperLayoutParams = null;
        private LinearLayout.LayoutParams mVerticalTopPlaceHolderLayoutParams = null;
        private LinearLayout.LayoutParams mVerticalBottomPlaceHolderLayoutParams = null;
        private LinearLayout.LayoutParams mHorizontalLeftPlaceHolderLayoutParams = null;
        private LinearLayout.LayoutParams mHorizontalRightPlaceHolderLayoutParams = null;
        private int mListViewWeight = 0;
        private int mListViewWrapperWeight = 0;
        private int mHorizontalLeftPlaceHolderWeight = 0;
        private int mVerticalTopPlaceHolderWeight = 0;
        private int mSelectedContents = 0;
        private float mFontSize = DEFAULT_FONT_SIZE;
        private int mFontColor = DEFAULT_FONT_COLOR;
        private Pattern mGrepPattern = Pattern.compile( "" );
        private boolean mGrepEnable = false;
        private LayoutInflater mLayoutInflater = null;
        
        private LoggerServiceBinder mLoggerServiceBinder = new LoggerServiceBinder();
        Object mLoggerThreadLock = new Object();
        boolean mLoggerThreadShouldRun = true;

        private Runnable logcatRunnable = new Runnable() {
			String logcatCommand = "logcat -v threadtime *:V\n";
        	
			@Override
			public void run() {				
			    Process proc = null;
				BufferedReader bf = null;
				BufferedWriter bw = null;
				
				try {
					proc = Runtime.getRuntime().exec("su"); 
					bw = new BufferedWriter( new OutputStreamWriter( proc.getOutputStream()), 4096);
					bf = new BufferedReader(new InputStreamReader(proc.getInputStream()), 4096);

					bw.write( logcatCommand );
					bw.flush();
					
					while ( true ) {
						synchronized ( mLoggerThreadLock ) {
							if( mLoggerThreadShouldRun == false ) {
								break;
							}
						}
						
						String logcatLine = bf.readLine();
						if( logcatLine.length() == 0 ) {
							try {
								Thread.sleep(200);
							} catch (InterruptedException e) {
							}
							continue;
						}
						updateLogList( logcatLine );
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if( bf != null ) {
						try {
							bf.close();
						} catch ( IOException e) {
							e.printStackTrace();
						}
					}
					
					if( bw != null ) {
						try {
							bw.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
					if( proc != null ) {
						proc.destroy();
					}
				}
			}
		};
		private ArrayAdapter<String[]> mAdapter;

        @Override
        public void onCreate() {
                super.onCreate();

                mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                mLayoutInflater = LayoutInflater.from(this);
                mViews = mLayoutInflater.inflate(R.layout.service_main, null);

                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                                WindowManager.LayoutParams.MATCH_PARENT,
                                WindowManager.LayoutParams.MATCH_PARENT,
                                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                                PixelFormat.TRANSLUCENT);
                mWindowManager.addView(mViews, params);

                mHandler = new Handler();
                mLogMessageQueueParts = new LinkedList<String[]>();
                
                FrameLayout topVerticalPlaceHolder = (FrameLayout) mViews.findViewById( R.id.vertical_top_place_holder );
                mVerticalTopPlaceHolderLayoutParams =(android.widget.LinearLayout.LayoutParams) topVerticalPlaceHolder.getLayoutParams();
                
                FrameLayout bottomVerticalPlaceHolder = ( FrameLayout ) mViews.findViewById( R.id.vertical_bottom_place_holder );
                mVerticalBottomPlaceHolderLayoutParams =(android.widget.LinearLayout.LayoutParams) bottomVerticalPlaceHolder.getLayoutParams();
                
                FrameLayout leftHorizontalPlaceHolder = ( FrameLayout ) mViews.findViewById( R.id.horizontal_left_palce_holder );
                mHorizontalLeftPlaceHolderLayoutParams = (android.widget.LinearLayout.LayoutParams) leftHorizontalPlaceHolder.getLayoutParams();
                
                FrameLayout rightHorizontalPlaceHolder = ( FrameLayout ) mViews.findViewById( R.id.horizontal_right_palce_holder );
                mHorizontalRightPlaceHolderLayoutParams = (android.widget.LinearLayout.LayoutParams) rightHorizontalPlaceHolder.getLayoutParams();
                
                LinearLayout linearLayout = (LinearLayout)mViews.findViewById(R.id.log_view_wrapper);
                mListViewWrapperLayoutParams = (android.widget.LinearLayout.LayoutParams) linearLayout.getLayoutParams();
                
                mListView = (ListView)mViews.findViewById( R.id.log_view );
                mAdapter = new ArrayAdapter< String[] >( this, R.layout.list_row, mLogMessageQueueParts ) {
                	@Override
                	public View getView( int position, View convertView, ViewGroup parent ) {
                		TextView view = ( TextView )convertView;
                		
                		if( view == null )
                		{
                			view = ( TextView )mLayoutInflater.inflate( R.layout.list_row , null );
                		}

                		String displayLogcatLine = generateDisplayString( mLogMessageQueueParts.get( position ) );
                		view.setText( displayLogcatLine );
                		
                		synchronized (mLoggerThreadLock) {
                			view.setTextSize( mFontSize );
                			view.setTextColor( mFontColor );
						}
                		
                		return view;
                	}
                };
                mListView.setAdapter( mAdapter );
                mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                mListViewLayoutParams = (android.widget.LinearLayout.LayoutParams) mListView.getLayoutParams();
                
        }
        
        private String generateDisplayString( String[] logcatLineParts ) {

        	String result = "";
        	
        	for( int i = 0; i < logcatLineParts.length; i++ )
        	{
        		if( ( mSelectedContents & ( 1 << i ) ) != 0 )
        		{
        			result += " " + logcatLineParts[ i ];
        		}
        	}
        	
			return result;
		}

		@Override
        public void onStart(Intent intent, int startId) {
        		super.onStart(intent, startId);
        }

        @Override
        public void onDestroy() {
        	stopLoggerThread();

        	mWindowManager.removeView(mViews);

        	super.onDestroy();
        }

        private class LoggerServiceBinder extends ILoggerService.Stub {

			@Override
			public void startLogger() throws RemoteException {
				startLoggerThread();
			}

			@Override
			public void stopLogger() throws RemoteException {
				stopLoggerThread();
			}

			@Override
			public void setOppacity(int oppacity) throws RemoteException {
				final float alpha = Math.min( MAX_OPPACITY, oppacity ) / (float)MAX_OPPACITY;
				mListView.setAlpha( alpha );
			}
			
			@Override
			public void setHorizontalLogLinesWeight( int weight ) throws RemoteException {
				synchronized ( mLoggerThreadLock ) {
					mListViewWeight = weight;
					mListViewLayoutParams.weight = mListViewWeight;
					
					updateHorizontalOffset();
				}
			}

			@Override
			public void setVerticalLogLinesWeight(int weight) throws RemoteException {
				synchronized (mLoggerThreadLock) {
					mListViewWrapperWeight = weight;
					mListViewWrapperLayoutParams.weight = mListViewWrapperWeight;
					
					updateVerticalOffset();
				}
			}
			
			@Override
			public void setHorizontalLogLinesOffset(int weight) throws RemoteException {
				synchronized ( mLoggerThreadLock ) {
					mHorizontalLeftPlaceHolderWeight = weight;
					
					updateHorizontalOffset();
				}
			}

			@Override
			public void setVerticalLogLinesOffset(int weight) throws RemoteException {
				synchronized ( mLoggerThreadLock ) {
					mVerticalTopPlaceHolderWeight = weight;
					
					updateVerticalOffset();
				}
			}

			private void updateVerticalOffset() {
				int marginWeight = 100 - mListViewWrapperWeight;
				int topPlaceHolderWeight = ( marginWeight * mVerticalTopPlaceHolderWeight + 50 ) / 100;
				mVerticalTopPlaceHolderLayoutParams.weight = topPlaceHolderWeight;
				mVerticalBottomPlaceHolderLayoutParams.weight = marginWeight - topPlaceHolderWeight;
				mAdapter.notifyDataSetChanged();
			}
			
			private void updateHorizontalOffset() {
				int marginWeight = 100 - mListViewWeight;
				int leftPlaceHolderWeight = ( marginWeight * mHorizontalLeftPlaceHolderWeight + 50 ) / 100;
				mHorizontalLeftPlaceHolderLayoutParams.weight = ( marginWeight * mHorizontalLeftPlaceHolderWeight + 50 ) / 100;
				mHorizontalRightPlaceHolderLayoutParams.weight = marginWeight - leftPlaceHolderWeight;
				mAdapter.notifyDataSetChanged();
			}

			@Override
			public void setFontsize(int size) throws RemoteException {
				synchronized ( mLoggerThreadLock ) {
					mFontSize = size;
	                mListView.setAdapter( mAdapter );
				}
			}

			@Override
			public void setFontColor(int color) throws RemoteException {
				synchronized ( mLoggerThreadLock ) {
					mFontColor = color;
					mListView.setAdapter( mAdapter );
				}
			}

			@Override
			public void setSelectedContents(int contents) throws RemoteException {
				synchronized ( mLoggerThreadLock ) {
					mSelectedContents = contents;
					mAdapter.notifyDataSetChanged();
				}
			}

			@Override
			public void setGrepText(String text) throws RemoteException {
				synchronized ( mLoggerThreadLock ) {
					mGrepPattern = Pattern.compile( text );
				}
			}

			@Override
			public void enableGrep(boolean flag) throws RemoteException {
				synchronized ( mLoggerThreadLock ) {
					mGrepEnable = flag;
				}
			}
        }

		private void startLoggerThread() {
			synchronized ( mLoggerThreadLock ) {
				mLoggerThreadShouldRun = true;
			}
			
			if( mLogcatThread == null ) {
	        	mLogcatThread = new Thread( logcatRunnable );
	        	mLogcatThread.start();
			}
		}
		
		private void stopLoggerThread() {
			synchronized ( mLoggerThreadLock ) {
				mLoggerThreadShouldRun = false;
			}
			
			mLogcatThread = null;
		}      
        
        @Override
        public IBinder onBind(Intent arg0) {
                // TODO Auto-generated method stub
                return mLoggerServiceBinder;
        }

        public static boolean isRunning( Context context ) {
            boolean result = false;
            
            ActivityManager am = (ActivityManager)context.getSystemService( Context.ACTIVITY_SERVICE );
            List<RunningServiceInfo> runningService = am.getRunningServices( 1024 );
            if( runningService != null )
            {
                for( RunningServiceInfo servInfo : runningService )
                {
                    if( servInfo.service.getClassName().equals( LoggerService.class.getName() ) )
                    {
                        result = true;
                        break;
                    }
                }
            }

            return result;
        }
        
        private void updateLogList( final String logcatLine ) {
        	mHandler.post( new Runnable() {
				
				@Override
				public void run() {
					if( mGrepEnable && !mGrepPattern.matcher( logcatLine ).find() )
					{
						return;
					}
						
					mLogMessageQueueParts.add( logcatLine.split( ":? +" ) );
					mAdapter.notifyDataSetChanged();
					
					while( MAX_LOGLINE_SIZE < mLogMessageQueueParts.size() )
					{
						mLogMessageQueueParts.remove();
					}
				}
			});
        }

}
