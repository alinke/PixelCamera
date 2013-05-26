package ioio.examples.camera2matrix;

//import ioio.lib.api.RgbLedMatrix;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import alt.android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuInflater;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

public class CameraToMatrixActivity extends IOIOActivity {
	//private static ioio.lib.api.RgbLedMatrix matrix_;
	//private static ioio.lib.api.RgbLedMatrix.Matrix KIND;  //have to do it this way because there is a matrix library conflict
	private static final String TAG = "CameraToMatrixActivity";
	private Camera camera_;
	private int height_;
	private int width_;

	//private short[] frame_ = new short[512];
	private short[] rgb_;
	
	//private static GifView gifView;
	private static ioio.lib.api.RgbLedMatrix matrix_;
	private static ioio.lib.api.RgbLedMatrix.Matrix KIND;  //have to do it this way because there is a matrix library conflict
	private static android.graphics.Matrix matrix2;
    //private static final String TAG = "PixelPileDriver";	  	
  	//private static short[] frame_ = new short[512];
	private static short[] frame_;
  	public static final Bitmap.Config FAST_BITMAP_CONFIG = Bitmap.Config.RGB_565;
  	private static byte[] BitmapBytes;
  	private static InputStream BitmapInputStream;
  	private static Bitmap canvasBitmap;
  	private static Bitmap IOIOBitmap;
  	private static Bitmap originalImage;
  	private static int width_original;
  	private static int height_original; 	  
  	private static float scaleWidth; 
  	private static float scaleHeight; 	  	
  	private static Bitmap resizedBitmap;  	
  	private static int deviceFound = 0;
  	
  	private SharedPreferences prefs;
	private String OKText;
	private Resources resources;
	private String app_ver;	
	private int matrix_model;
	private final String tag = "";	
	private final String LOG_TAG = "PixelPileDriver";
	private String imagePath;
	private static int resizedFlag = 0;
	
	private ConnectTimer connectTimer; 	
   // private static DecodedTimer decodedtimer; 
	private Canvas canvas;
	private static Canvas canvasIOIO;
	
	private String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
    private String basepath = extStorageDirectory;
    private static String decodedDirPath =  Environment.getExternalStorageDirectory() + "/pixel/pixelpiledriver/decoded"; 
    private String artpath = "/media";
    private static Context context;
    private Context frameContext;
    private GridView sdcardImages;
	
	///********** Timers
  //  private MediaScanTimer mediascanTimer; 	
	private boolean noSleep = false;	
	private int countdownCounter;
	private static final int countdownDuration = 30;
	private Display display;
//	private ImageAdapter imageAdapter;
	private Cursor cursor;
	private int size;  //the number of pictures
	private ProgressDialog pDialog = null;
	private int columnIndex; 
	private TextView firstTimeSetup1_;
	private TextView firstTimeSetup2_;
	private TextView firstTimeInstructions_;
	private TextView firstTimeSetupCounter_;
	private boolean debug_;
	private static int appAlreadyStarted = 0;
	private int FPSOverride_ = 0;
	private static int fps = 0;
	private static int x = 0;
	private static int u = 0;
	private static String selectedFileName;
	private static int selectedFileTotalFrames;
	private static int selectedFileDelay;
	private static int Playing = 0;
	private static int selectedFileResolution;
	private static int currentResolution;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        try
        {
            app_ver = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        }
       catch (NameNotFoundException e)
        {
            Log.v(tag, e.getMessage());
        }
        
        //******** preferences code
        resources = this.getResources();
        setPreferences();
        //***************************
        
        if (noSleep == true) {        	      	
        	this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //disables sleep mode
        }	
        
        connectTimer = new ConnectTimer(30000,5000); //pop up a message if it's not connected by this timer
 		connectTimer.start(); //this timer will pop up a message box if the device is not found
 		
 		context = getApplicationContext();
		
		
		
	}

	@Override
	protected void onStart() {
		// Start the camera preview
		camera_ = getCameraInstance();
		Parameters params = camera_.getParameters();
		params.setPreviewFormat(ImageFormat.NV21);
		getSmallestPreviewSize(params);
		params.setPreviewSize(width_, height_);
		rgb_ = new short[width_ * height_];
		// params.setFlashMode(Parameters.FLASH_MODE_TORCH);
		params.setWhiteBalance(Parameters.WHITE_BALANCE_AUTO);
		camera_.setParameters(params);

		camera_.setPreviewCallback(new PreviewCallback() {
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				toRGB565(data, width_, height_, rgb_);
				synchronized (frame_) {
					for (int i = 0; i < 16; ++i) {
						System.arraycopy(rgb_, i * width_, frame_, i * 32, 32);
					}
					frame_.notify();
					//Toast.makeText(getBaseContext(), "Frame: " + frame_, Toast.LENGTH_LONG).show();
				}
			}
		});
		camera_.startPreview();
		super.onStart();
	}

	/** Chooses the smallest supported preview size. */
	private void getSmallestPreviewSize(Parameters params) {
		List<Size> supportedPreviewSizes = params.getSupportedPreviewSizes();
		Size minSize = null;
		for (Size s : supportedPreviewSizes) {
			if (minSize == null || s.width < minSize.width) {
				minSize = s;
			}
		}
		height_ = minSize.height;
		width_ = minSize.width;
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Stop the camera preview.
		camera_.stopPreview();
		camera_.setPreviewCallback(null);
		camera_.release();
	}
	
	 @Override
	    public boolean onCreateOptionsMenu(Menu menu) 
	    {
	       MenuInflater inflater = getMenuInflater();
	       inflater.inflate(R.menu.mainmenu, menu);
	       return true;
	    }

	    @Override
	    public boolean onOptionsItemSelected (MenuItem item)
	    {
	       
			
	      if (item.getItemId() == R.id.menu_instructions) {
	 	    	AlertDialog.Builder alert=new AlertDialog.Builder(this);
	 	      	alert.setTitle(getResources().getString(R.string.setupInstructionsStringTitle)).setIcon(R.drawable.icon).setMessage(getResources().getString(R.string.setupInstructionsString)).setNeutralButton(getResources().getString(R.string.OKText), null).show();
	 	   }
	    	
		  if (item.getItemId() == R.id.menu_about) {
			  
			    AlertDialog.Builder alert=new AlertDialog.Builder(this);
		      	alert.setTitle(getString(R.string.menu_about_title)).setIcon(R.drawable.icon).setMessage(getString(R.string.menu_about_summary) + "\n\n" + getString(R.string.versionString) + " " + app_ver).setNeutralButton(getResources().getString(R.string.OKText), null).show();	
		   }
	    	
	    	if (item.getItemId() == R.id.menu_prefs)
	       {
	    		
	    		appAlreadyStarted = 0;    		
	    		Intent intent = new Intent()
	       				.setClass(this,
	       						preferences.class);   
	    				//this.startActivityForResult(intent, 0);
	    				this.startActivity(intent);
	       }
	    	
	     	
	    	
	       return true;
	    }
	    
	    


	@Override
	    public void onActivityResult(int reqCode, int resCode, Intent data) //we'll go into a reset after this
	    {
	    	super.onActivityResult(reqCode, resCode, data);    	
	    	setPreferences(); //very important to have this here, after the menu comes back this is called, we'll want to apply the new prefs without having to re-start the app
	    	
	    	//if (reqCode == 0 || reqCode == 1) //then we came back from the preferences menu so re-load all images from the sd card, 1 is a re-scan
	    	//if (reqCode == 1)
	    //	{
	    		//imagedisplaydurationTimer.cancel(); //we may have been running a slideshow so kill it
	    	    //pausebetweenimagesdurationTimer.cancel();
	    	//	setupViews();
	    	 //   setProgressBarIndeterminateVisibility(true); 
	    	  //  loadImages();      
	       // }
	    } 
	    
	    private void setPreferences() //here is where we read the shared preferences into variables
	    {
	     SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);     
	    
	     //scanAllPics = prefs.getBoolean("pref_scanAll", false);
	     //slideShowMode = prefs.getBoolean("pref_slideshowMode", false);
	     noSleep = prefs.getBoolean("pref_noSleep", false);
	     debug_ = prefs.getBoolean("pref_debugMode", false);
	
	     
	     matrix_model = Integer.valueOf(prefs.getString(   //the selected RGB LED Matrix Type
	    	        resources.getString(R.string.selected_matrix),
	    	        resources.getString(R.string.matrix_default_value))); 
	     
	     if (matrix_model == 0 || matrix_model == 1) {
	    	 currentResolution = 16;
	     }
	     else
	     {
	    	 currentResolution = 32;
	     }
	     
	  
	     
	     switch (matrix_model) {  //get this from the preferences
	     case 0:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x16;
	    	 BitmapInputStream = getResources().openRawResource(R.raw.selectimage16);
	    	 break;
	     case 1:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.ADAFRUIT_32x16;
	    	 BitmapInputStream = getResources().openRawResource(R.raw.selectimage16);
	    	 break;
	     case 2:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x32_NEW; //v1
	    	 BitmapInputStream = getResources().openRawResource(R.raw.selectimage32);
	    	 break;
	     case 3:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x32; //v2
	    	 BitmapInputStream = getResources().openRawResource(R.raw.selectimage32);
	    	 break;
	     default:	    		 
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x32; //v2 as the default
	    	 BitmapInputStream = getResources().openRawResource(R.raw.selectimage32);
	     }
	         
	     frame_ = new short [KIND.width * KIND.height];
		 BitmapBytes = new byte[KIND.width * KIND.height *2]; //512 * 2 = 1024 or 1024 * 2 = 2048
		 
		 loadRGB565(); //this function loads a raw RGB565 image to the matrix
	 }
	    
		private static void loadRGB565() {
		 	   
			try {
	   			int n = BitmapInputStream.read(BitmapBytes, 0, BitmapBytes.length); // reads
	   																				// the
	   																				// input
	   																				// stream
	   																				// into
	   																				// a
	   																				// byte
	   																				// array
	   			Arrays.fill(BitmapBytes, n, BitmapBytes.length, (byte) 0);
	   		} catch (IOException e) {
	   			e.printStackTrace();
	   		}

	   		int y = 0;
	   		for (int i = 0; i < frame_.length; i++) {
	   			frame_[i] = (short) (((short) BitmapBytes[y] & 0xFF) | (((short) BitmapBytes[y + 1] & 0xFF) << 8));
	   			y = y + 2;
	   		}
	   }
	
	
	 class IOIOThread extends BaseIOIOLooper {
	  		//private ioio.lib.api.RgbLedMatrix matrix_;
	    	//public AnalogInput prox_;  //just for testing , REMOVE later

	  		@Override
	  		protected void setup() throws ConnectionLostException {
	  			matrix_ = ioio_.openRgbLedMatrix(KIND);
	  			deviceFound = 1; //if we went here, then we are connected over bluetooth or USB
	  			connectTimer.cancel(); //we can stop this since it was found
	  		  			
	  			if (debug_ == true) {  			
		  			showToast("Bluetooth Connected");
	  			}
	  			
	  			//if (fps != 0) {  //then we're doing the FPS override which the user selected from settings
	  			//	matrixdrawtimer.start(); 
	  			//}
	  			//else {
	  			matrix_.frame(frame_); //write select pic to the frame since we didn't start the timer
	  			//}
	  			
	  			appAlreadyStarted = 1;
	  			
	  			
	  			
	  		}

	  

			@Override
			public void loop() throws ConnectionLostException {
				synchronized (frame_) {
					try {
						frame_.wait();
						matrix_.frame(frame_);
						
					} catch (InterruptedException e) {
					}
				}
			}

	  		
	  		@Override
			public void disconnected() {   			
	  			Log.i(LOG_TAG, "IOIO disconnected");
				if (debug_ == true) {  			
		  			showToast("Bluetooth Disconnected");
	  			}			
			}

			@Override
			public void incompatible() {  //if the wrong firmware is there
				//AlertDialog.Builder alert=new AlertDialog.Builder(context); //causing a crash
				//alert.setTitle(getResources().getString(R.string.notFoundString)).setIcon(R.drawable.icon).setMessage(getResources().getString(R.string.bluetoothPairingString)).setNeutralButton(getResources().getString(R.string.OKText), null).show();	
				showToast("Incompatbile firmware!");
				showToast("This app won't work until you flash PIXEL with the correct firmware!");
				showToast("You can use the IOIO Manager Android app to flash the correct firmware");
				Log.e(LOG_TAG, "Incompatbile firmware!");
			}
	  		
	  		}

	  	@Override
	  	protected IOIOLooper createIOIOLooper() {
	  		return new IOIOThread();
	  	}
	  	
	  	 private  void showToast(final String msg) {
	 		runOnUiThread(new Runnable() {
	 			@Override
	 			public void run() {
	 				Toast toast = Toast.makeText(CameraToMatrixActivity.this, msg, Toast.LENGTH_LONG);
	                 toast.show();
	 			}
	 		});
	 	}  
	     
	     private void showToastShort(final String msg) {
	 		runOnUiThread(new Runnable() {
	 			@Override
	 			public void run() {
	 				Toast toast = Toast.makeText(CameraToMatrixActivity.this, msg, Toast.LENGTH_SHORT);
	                 toast.show();
	 			}
	 		});
	 	} 
	     
	     public class ConnectTimer extends CountDownTimer
	    	{

	    		public ConnectTimer(long startTime, long interval)
	    			{
	    				super(startTime, interval);
	    			}

	    		@Override
	    		public void onFinish()
	    			{
	    				if (deviceFound == 0) {
	    					showNotFound(); 					
	    				}
	    				
	    			}

	    		@Override
	    		public void onTick(long millisUntilFinished)				{
	    			//not used
	    		}
	    	}
	     
	     private void showNotFound() {	
	 		AlertDialog.Builder alert=new AlertDialog.Builder(this);
	 		alert.setTitle(getResources().getString(R.string.notFoundString)).setIcon(R.drawable.icon).setMessage(getResources().getString(R.string.bluetoothPairingString)).setNeutralButton(getResources().getString(R.string.OKText), null).show();	
	     }

	   
	

	//class IOIOThread extends BaseIOIOLooper {
		//private RgbLedMatrix matrix_;

		//@Override
		//protected void setup() throws ConnectionLostException {
		//	matrix_ = ioio_.openRgbLedMatrix();
	//	}

	//	@Override
	//	public void loop() throws ConnectionLostException {
		//	synchronized (frame_) {
			//	try {
				//	frame_.wait();
				//	matrix_.frame(frame_);
					
			//	} catch (InterruptedException e) {
			//	}
		//	}
	//	}
//	}

	//@Override
//	protected IOIOLooper createIOIOLooper() {
		//return new IOIOThread();
	//}

	private static Camera getCameraInstance() {
		try {
			return Camera.open();
		} catch (Exception e) {
			Log.e(TAG, "Failed to open camera.", e);
		}
		return null;
	}

	// From: http://lanedetectionandroid.googlecode.com/svn-history/r8/trunk/tests/TestJniCall/src/org/siprop/opencv/Preview.java
	static public void toRGB565(byte[] yuvs, int width, int height, short[] rgbs) {
	    //the end of the luminance data
	    final int lumEnd = width * height;
	    //points to the next luminance value pair
	    int lumPtr = 0;
	    //points to the next chromiance value pair
	    int chrPtr = lumEnd;
	    //points to the next byte output pair of RGB565 value
	    int outPtr = 0;
	    //the end of the current luminance scanline
	    int lineEnd = width;

	    while (true) {

	        //skip back to the start of the chromiance values when necessary
	        if (lumPtr == lineEnd) {
	            if (lumPtr == lumEnd) break; //we've reached the end
	            //division here is a bit expensive, but's only done once per scanline
	            chrPtr = lumEnd + ((lumPtr  >> 1) / width) * width;
	            lineEnd += width;
	        }

	        //read the luminance and chromiance values
	        final int Y1 = yuvs[lumPtr++] & 0xff; 
	        final int Y2 = yuvs[lumPtr++] & 0xff; 
	        final int Cr = (yuvs[chrPtr++] & 0xff) - 128; 
	        final int Cb = (yuvs[chrPtr++] & 0xff) - 128;
	        int R, G, B;

	        //generate first RGB components
	        B = Y1 + ((454 * Cb) >> 8);
	        if(B < 0) B = 0; else if(B > 255) B = 255; 
	        G = Y1 - ((88 * Cb + 183 * Cr) >> 8); 
	        if(G < 0) G = 0; else if(G > 255) G = 255; 
	        R = Y1 + ((359 * Cr) >> 8); 
	        if(R < 0) R = 0; else if(R > 255) R = 255; 
	        rgbs[outPtr++]  = (short) ((R >> 3) << 11 | (G >> 2) << 5 | (B >> 3));

	        //generate second RGB components
	        B = Y2 + ((454 * Cb) >> 8);
	        if(B < 0) B = 0; else if(B > 255) B = 255; 
	        G = Y2 - ((88 * Cb + 183 * Cr) >> 8); 
	        if(G < 0) G = 0; else if(G > 255) G = 255; 
	        R = Y2 + ((359 * Cr) >> 8); 
	        if(R < 0) R = 0; else if(R > 255) R = 255; 
	        rgbs[outPtr++]  = (short) ((R >> 3) << 11 | (G >> 2) << 5 | (B >> 3));
	    }
	}
}