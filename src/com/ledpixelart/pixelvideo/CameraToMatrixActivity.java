package com.ledpixelart.pixelvideo;

//import ioio.lib.api.RgbLedMatrix;
import ioio.lib.api.IOIO.VersionType;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
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
import android.content.pm.PackageManager;
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
import android.os.SystemClock;
import alt.android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuInflater;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class CameraToMatrixActivity extends IOIOActivity implements SurfaceHolder.Callback {
	//private static ioio.lib.api.RgbLedMatrix matrix_;
	//private static ioio.lib.api.RgbLedMatrix.Matrix KIND;  //have to do it this way because there is a matrix library conflict
	private static final String TAG = "PixelVideo";
	private Camera camera_;
	private int height_;
	private int width_;
	private short[] rgb_;

	//private static GifView gifView;
	private static ioio.lib.api.RgbLedMatrix matrix_;
	private static ioio.lib.api.RgbLedMatrix.Matrix KIND;  //have to do it this way because there is a matrix library conflict
	private static android.graphics.Matrix matrix2;
	private static short[] frame_;
  	public static final Bitmap.Config FAST_BITMAP_CONFIG = Bitmap.Config.RGB_565;
  	private static byte[] BitmapBytes;
  	private static InputStream BitmapInputStream;
  	private static Bitmap canvasBitmap;
  	private static Bitmap originalImage;
  	private static int width_original;
  	private static int height_original; 	  
  	private static float scaleWidth; 
  	private static float scaleHeight; 	  	
  	private static Bitmap resizedBitmap;  	
  	private static int deviceFound = 0;
  	
  	private SharedPreferences prefs;
	private Resources resources;
	private String app_ver;	
	private int matrix_model;
	private final String tag = "";	
	private final String LOG_TAG = "PixelVideo";
	private static int resizedFlag = 0;

	private ConnectTimer connectTimer; 	
    private static Context context;
    private Context frameContext;

	///********** Timers 	
	private boolean noSleep = false;	
	private int countdownCounter;
	private static final int countdownDuration = 30;
	private Display display;
	private boolean debug_;
	private static int appAlreadyStarted = 0;
	private int FPSOverride_ = 0;
	private static int fps = 0;
	private static int x = 0;
	private static int u = 0;
	private static int Playing = 0;
	private static int selectedFileResolution;
	private static int currentResolution;
	private int cameraId;
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	//private Camera mCamera;
	private boolean previewing;
	private boolean writing;
	private boolean AndroidSDCardWriting = false;
	private static String decodedDirPath =  Environment.getExternalStorageDirectory() + "/pixel/video"; 
 	//private static int selectedFileTotalFrames;
	private static int selectedFileDelay;
 	private  ProgressDialog progress;
 	private int size = 0;
 	public long frame_length;
	private static String pixelHardwareID = "Not Connected";
	private static final int REQUEST_PAIR_DEVICE = 10;
	private static VersionType v;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //force only portrait mode
		
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
 		
 		Button buttonStartCameraPreview = (Button)findViewById(R.id.startcamerapreview);
        Button buttonStopCameraPreview = (Button)findViewById(R.id.stopcamerapreview);
        Button buttonStartCameraRecord = (Button)findViewById(R.id.startcamerarecord);
        Button buttonStopCameraRecord = (Button)findViewById(R.id.stopcamerarecord);
 		
        context = getApplicationContext();
 		getWindow().setFormat(PixelFormat.UNKNOWN);
 		mSurfaceView = (SurfaceView)findViewById(R.id.surface_camera);
 		mSurfaceHolder = mSurfaceView.getHolder();
 		mSurfaceHolder.addCallback(this);
 		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
 		
 		mSurfaceView.setKeepScreenOn(false);
 		
 		 buttonStartCameraPreview.setOnClickListener(new Button.OnClickListener(){

 			@Override
 			public void onClick(View v) {
 				
 				if (deviceFound == 1) {   
 					writing = false;
		 				try {
							matrix_.interactive();
						} catch (ConnectionLostException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} //put back into interactive mode
		 				
		 				try {
							
		 					startCam();  //note preview = true is called in startCam
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		 			}
 				  else {
 	    	    	   showToast("PIXEL was not found, did you Bluetooth pair to PIXEL? Use Code 0000 for PIXEL V2 and 4545 for PIXEL V1.");
 	    		  }
			}});
 		 		
         
         buttonStopCameraPreview.setOnClickListener(new Button.OnClickListener(){

 			@Override
 			public void onClick(View v) {
 				if (deviceFound == 1) {   
		 				writing = false;
		 				// TODO Auto-generated method stub
		 				if(camera_ != null && previewing){
		 					camera_.stopPreview();
		 					camera_.release();
		 					camera_ = null;
		 					
		 					previewing = false;
		 					
		 				}
		 			}
			  else {
   	    	   showToast("PIXEL was not found, did you Bluetooth pair to PIXEL? Use Code 0000 for PIXEL V2 and 4545 for PIXEL V1.");
			  }
 			}});
         
    	 buttonStartCameraRecord.setOnClickListener(new Button.OnClickListener(){

 			@Override
 			public void onClick(View v) {
 				if (deviceFound == 1) {   
	 				AndroidSDCardWriting = true; //tells the writeimagetomatrix routine to write the file in append mode
	 				mSurfaceView.setKeepScreenOn(true); //keep the screen on while writing
	 				writing = true;   //tells the loop not to write in frame_
	 				try {
						matrix_.interactive();
						matrix_.writeFile(24); //24 fps video, can make this a setting later
					} catch (ConnectionLostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} //put into write mode
					
					//new recordVideoAsync().execute();
					size = 0; //the frame counter, we'll reset it here
					
					File decodedFileDir = new File(decodedDirPath); //pixel/video  ,create it if not there
					if(decodedFileDir.exists() == false) {
						decodedFileDir.mkdirs();
					}
			        	
		    		File decodedFile = new File(decodedDirPath + "/video.rgb565"); //decoded/video.rgb565 , if it's already there, then we need to delete
		    		if(decodedFile.exists()) {
		    			decodedFile.delete(); //delete the file as we're going to re-create it
		    		}
					
	 				try {
						startCam();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}  //note preview = true is called in startCam
 				}
 				  else {
 	   	    	   showToast("PIXEL was not found, did you Bluetooth pair to PIXEL? Use Code 0000 for PIXEL V2 and 4545 for PIXEL V1.");
 				  }
 			}});
       
       buttonStopCameraRecord.setOnClickListener(new Button.OnClickListener(){

 			@Override
 			public void onClick(View v) {
 				if (deviceFound == 1) {   
	 				//only after writing is done should we put writing back to false
	 				AndroidSDCardWriting = false;
	 				//we need to stop writing the file to the Android sd card here
	 				new writePixelAsync().execute(); //here we will write the file FROM the Android sd card .rgb565 to PIXEL
	 				
	 			/*	// TODO Auto-generated method stub
	 				if(camera_ != null && previewing){
	 					camera_.stopPreview();
	 					camera_.release();
	 					camera_ = null;
	 					
	 					previewing = false;
	 				}*/
 				}
				  else {
	   	    	   showToast("PIXEL was not found, did you Bluetooth pair to PIXEL? Use Code 0000 for PIXEL V2 and 4545 for PIXEL V1.");
				  }
 			}});
 	
	}
	
 private class recordVideoAsync extends AsyncTask<Void, Integer, Void>{
	   
	      
	     @Override
	  protected void onPreExecute() {
		   // update the UI immediately after the task is executed
		   super.onPreExecute();
	    
	  }
	      
	  @Override
	  protected Void doInBackground(Void... params) {
			
			
		  new recordVideoAsync().execute();
		 //  startCam();  //note preview = true is called in startCam
		    
	   return null;
	  }
	  
	  @Override
	  protected void onProgressUpdate(Integer... values) {
	   super.onProgressUpdate(values);
	  }
	   
	  @Override
	  protected void onPostExecute(Void result) {
	   super.onPostExecute(result);
	  
	 //  continueOnCreate();
	  }
	
 }
	
	public void surfaceCreated(SurfaceHolder holder) {

		
	}

		
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

	
	}
		
	public void surfaceDestroyed(SurfaceHolder holder) {
		
		if(camera_ != null && previewing == true){
				camera_.stopPreview();
				camera_.release();
				camera_ = null;
				
				previewing = false;
		}
		
		
	}
	
	private void startCam() throws IOException {
		if(!previewing){
				//camera_ = Camera.open();
				camera_ = getCameraInstance();
				if (camera_ != null){
					
					Parameters params = camera_.getParameters();
					params.setPreviewFormat(ImageFormat.NV21);
					getSmallestPreviewSize(params);
					params.setPreviewSize(width_, height_);
					rgb_ = new short[width_ * height_];
					params.setWhiteBalance(Parameters.WHITE_BALANCE_AUTO);
					camera_.setParameters(params);
					setDisplayOrientation(camera_, 90);
					
					try {
						camera_.setPreviewDisplay(mSurfaceHolder);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				camera_.startPreview();
				previewing = true;
				streamVideo();
				}
		}
	}
	
	protected void setDisplayOrientation(Camera camera, int angle){
        Method downPolymorphic;
        try
        {
            downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[] { int.class });
            if (downPolymorphic != null)
                downPolymorphic.invoke(camera, new Object[] { angle });
        }
        catch (Exception e1)
        {
        }
}
	
	//@Override
	//protected void onStart() {
    private void streamVideo() {
		
		camera_.setPreviewCallback(new PreviewCallback() {  //this is called on each camera playback
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {  //data is in yuv format
				//showToast("went here");
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				YuvImage yuv = new YuvImage(data, ImageFormat.NV21, width_, height_, null);
				yuv.compressToJpeg(new Rect(0, 0, width_, height_), 50, out);
				byte[] outputStream = out.toByteArray(); //we need to get the yuv format and convert to bmp
				originalImage = BitmapFactory.decodeByteArray(outputStream,0,out.size()); //now we have bitmap format which we can actually use
				WriteImagetoMatrix(originalImage);
								
				synchronized (frame_) {
					
					if (AndroidSDCardWriting == true) {
						 try {
							 size++; // let's track how many frames	
							 appendWrite(BitmapBytes, decodedDirPath + "/" + "video" + ".rgb565");
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
					}
					
					
					loadImage();
					frame_.notify();
				}
				
			}
		});
	//	camera_.startPreview();
		//super.onStart();
	}
	
	private Camera openFrontFacingCameraGingerbread() {  //not used
	    int cameraCount = 0;
	    Camera cam = null;
	    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
	    cameraCount = Camera.getNumberOfCameras();
	    for (int camIdx = 0; camIdx<cameraCount; camIdx++) {
	        Camera.getCameraInfo(camIdx, cameraInfo);
	        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
	            try {
	                cam = Camera.open(camIdx);
	            } catch (RuntimeException e) {
	                Log.e(LOG_TAG, "Camera failed to open: " + e.getLocalizedMessage());
	            }
	        }
	    }
	    return cam;
	}

 
  private int findFrontFacingCamera() {
    int cameraId = -1;
    // Search for the front facing camera
   // int numberOfCameras = Camera.getNumberOfCameras();
    int numberOfCameras = Camera.getNumberOfCameras();
    for (int i = 0; i < numberOfCameras; i++) {
      CameraInfo info = new CameraInfo();
      Camera.getCameraInfo(i, info);
      if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
        Log.d(LOG_TAG, "Camera found");
        cameraId = i;
        break;
      }
    }
    return cameraId;
  }

	
	  private void WriteImagetoMatrix(Bitmap img) {  //here we'll take a PNG, BMP, or whatever and convert it to RGB565 via a canvas, also we'll re-size the image if necessary
	    	
	 		 //let's test if the image is 32x32 resolution
			 width_original = img.getWidth();
			 height_original = img.getHeight();
			 
			// Bitmap img = RotateBitmap(img,90); //rotate the image 90
			 
			 //if not, no problem, we will re-size it on the fly here		 
			// if (width_original != KIND.width || height_original != KIND.height) {
				 resizedFlag = 1;
				 scaleWidth = ((float) KIND.width) / width_original;
	   		 	 scaleHeight = ((float) KIND.height) / height_original;
		   		 // create matrix for the manipulation
		   		 matrix2 = new Matrix();
		   		 // resize the bit map
		   		 matrix2.postScale(scaleWidth, scaleHeight);
		   	     matrix2.postRotate(90); //had to add this because the rotation was off
		   		 resizedBitmap = Bitmap.createBitmap(img, 0, 0, width_original, height_original, matrix2, true);
		   		 canvasBitmap = Bitmap.createBitmap(KIND.width, KIND.height, Config.RGB_565); 
		   		 Canvas canvas = new Canvas(canvasBitmap);
		   		 canvas.drawRGB(0,0,0); //a black background
		   	   	 canvas.drawBitmap(resizedBitmap, 0, 0, null);
		   		 ByteBuffer buffer = ByteBuffer.allocate(KIND.width * KIND.height *2); //Create a new buffer
		   		 canvasBitmap.copyPixelsToBuffer(buffer); //copy the bitmap 565 to the buffer		
		   		 BitmapBytes = buffer.array(); //copy the buffer into the type array
			// }
			/* else {  //if we went here, then the image was already the correct dimension so no need to re-size
				 resizedFlag = 0;
				 canvasBitmap = Bitmap.createBitmap(KIND.width, KIND.height, Config.RGB_565); 
		   		 Canvas canvas = new Canvas(canvasBitmap);
		   	   	 canvas.drawBitmap(img, 0, 0, null);
		   		 ByteBuffer buffer = ByteBuffer.allocate(KIND.width * KIND.height *2); //Create a new buffer
		   		 canvasBitmap.copyPixelsToBuffer(buffer); //copy the bitmap 565 to the buffer		
		   		 BitmapBytes = buffer.array(); //copy the buffer into the type array
			 }	*/
	}
	 
	  
	  public void appendWrite(byte[] data, String filename) throws IOException {
			 FileOutputStream fos = new FileOutputStream(filename, true);  //true means append, false is over-write
		     fos.write(data);
		     fos.close();
		}

	public void loadImage() {
	 

	 		int y = 0;
	 		for (int i = 0; i < frame_.length; i++) {
	 			frame_[i] = (short) (((short) BitmapBytes[y] & 0xFF) | (((short) BitmapBytes[y + 1] & 0xFF) << 8));
	 			y = y + 2;
	 		}
	 		
	 		//we're done with the images so let's recycle them to save memory
		    canvasBitmap.recycle();
		    originalImage.recycle(); 
		    
		    if ( resizedFlag == 1) {
		    	resizedBitmap.recycle(); //only there if we had to resize an image
		    }
	 	}
	
	public class writePixelAsync extends AsyncTask<Void, Integer, Void>{  //writing to PIXEL
		
		 int progress_status;
	      
		  @Override
		  protected void onPreExecute() {
	      super.onPreExecute();
	    
	     progress = new ProgressDialog(CameraToMatrixActivity.this);
		        progress.setMax(size);
		        progress.setTitle("Writing Video to PIXEL");
		        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		        progress.setCancelable(false); //must have this as we don't want users cancel while it's writing
		        progress.show();
		  }
	      
	  @Override
	  protected Void doInBackground(Void... params) {
			
	  int count;
	  for (count=0;count<size-1;count++) {  //size is the number of video frames
				 
				
				  File file = new File(decodedDirPath + "/" + "video" + ".rgb565"); //this is one big file now, no longer separate files
				  
					RandomAccessFile raf = null;
				  
					//let's setup the seeker object
					try {
						raf = new RandomAccessFile(file, "r");
						
					} catch (FileNotFoundException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}  // "r" means open the file for reading
					
					if (x == size) { // Manju - Reached End of the file.
		   				x = 0;
		   				try {
							raf.seek(0); //move pointer back to the beginning of the file
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
		   			}
					
					 switch (selectedFileResolution) { //16x32 matrix = 1024k frame size, 32x32 matrix = 2048k frame size
			            case 16: frame_length = 1024;
			                     break;
			            case 32: frame_length = 2048;
			                     break;
			            case 64: frame_length = 4096;
			                     break;
			            case 128: frame_length = 8192;
	                     		 break;         
			            default: frame_length = 2048;
			                     break;
			          }
					 
					//now let's see forward to a part of the file
						try {
							raf.seek(x*frame_length);
						} catch (IOException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						} 
						//Log.d("PixelAnimations","from sd card write, x is: " + x);
						x++;
						
						if (frame_length > Integer.MAX_VALUE) {
			   			    try {
								throw new IOException("The file is too big");
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
			   			}
						
						// Create the byte array to hold the data
			   			//byte[] bytes = new byte[(int)length];
			   			BitmapBytes = new byte[(int)frame_length];
			   			 
			   			// Read in the bytes
			   			int offset = 0;
			   			int numRead = 0;
			   			try {
							while (offset < BitmapBytes.length && (numRead=raf.read(BitmapBytes, offset, BitmapBytes.length-offset)) >= 0) {
							    offset += numRead;
							}
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
			   			 
			   			// Ensure all the bytes have been read in
			   			if (offset < BitmapBytes.length) {
			   			    try {
								throw new IOException("The file was not completely read: "+file.getName());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
			   			}
			   			 
			   			// Close the input stream, all file contents are in the bytes variable
			   			try {
			   				raf.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}	
							
							//now that we have the byte array loaded, load it into the frame short array
							
						int y = 0;
						for (int i = 0; i < frame_.length; i++) {
							frame_[i] = (short) (((short) BitmapBytes[y] & 0xFF) | (((short) BitmapBytes[y + 1] & 0xFF) << 8));
							y = y + 2;
						}
					
						//need to add something in here if the transfer got interruped, then go back to interactive mode and start over
						//downloadCounter++;
					   	try {
					   	 Log.i("PixelAnimations ","Starting-->"+ count + " " + String.valueOf(size-1));
					   		matrix_.frame(frame_);
					   		progress_status++;
						    publishProgress(progress_status);
					   	
						} catch (ConnectionLostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
			  }  // end for
		  
			//progress.incrementProgressBy(1);
			//copyDecodedThread("0rain");
			
		    
	   return null;
	  }
	  
	  @Override
	  protected void onProgressUpdate(Integer... values) {
	   super.onProgressUpdate(values);
	   progress.incrementProgressBy(1);
	  }
	   
	  @Override
	  protected void onPostExecute(Void result) {
		  
		  progress.dismiss();
		  //we can put writing back to false here
	 
	   try {
		matrix_.playFile(); //put PIXEL into local playback mode, if pixel in local playback mode,
	} catch (ConnectionLostException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	   writing = false; //we've finished writing to PIXEL so we can set writing to false
	   mSurfaceView.setKeepScreenOn(false); //keep the screen on while writing
	  super.onPostExecute(result);
}
	
}
	
	
	private byte[] resizeImage(byte[] input) { //not used
	    Bitmap original = BitmapFactory.decodeByteArray(input , 0, input.length); //let's decode this byte stream
	    Bitmap resized = Bitmap.createScaledBitmap(original, 32, 32, true);
	    ByteArrayOutputStream blob = new ByteArrayOutputStream();
	    resized.compress(Bitmap.CompressFormat.PNG, 0, blob);
	    return blob.toByteArray();
	}


	/** Chooses the smallest supported preview size. */
	private void getSmallestPreviewSize(Parameters params) {
		List<Size> supportedPreviewSizes = params.getSupportedPreviewSizes();
		Size minSize = null;
		for (Size s : supportedPreviewSizes) {
			//Log.e("al",String.valueOf(supportedPreviewSizes));
			//Log.i(LOG_TAG, "number supported sizes: " + String.valueOf(supportedPreviewSizes));
			Log.i(LOG_TAG, "supported width: " + String.valueOf(s.width));
			Log.i(LOG_TAG, "supported height: " + String.valueOf(s.height));
			if (minSize == null || s.width < minSize.width) {
				minSize = s;
			}
		}
		height_ = minSize.height;
		width_ = minSize.width;
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
	 	      	alert.setTitle(getResources().getString(R.string.setupInstructionsStringTitle)).setIcon(R.drawable.ic_launcher).setMessage(getResources().getString(R.string.setupInstructionsString)).setNeutralButton(getResources().getString(R.string.OKText), null).show();
	 	   }
	    	
	     /* if (item.getItemId() == R.id.menu_btPair) //app crashes when going to this menu, to do fix this later
	       {
   			
	   		if (pixelHardwareID.substring(0,4).equals("MINT")) { //then it's a PIXEL V1 unit
	   			showToast("Bluetooth Pair to PIXEL using code: 4545");
	   		}
	   		else { //we have a PIXEL V2 unit
	   			showToast("Bluetooth Pair to PIXEL using code: 0000");
	   		}
	   		
	   		Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
		        startActivityForResult(intent, REQUEST_PAIR_DEVICE);
		    }*/
	      
	      if (item.getItemId() == R.id.menu_about) {
			  
			    AlertDialog.Builder alert=new AlertDialog.Builder(this);
		      	alert.setTitle(getString(R.string.menu_about_title)).setIcon(R.drawable.ic_launcher).setMessage(getString(R.string.menu_about_summary) + "\n\n" + getString(R.string.versionString) + " " + app_ver).setNeutralButton(getResources().getString(R.string.OKText), null).show();	
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
	     
	   /*  if (matrix_model == 0 || matrix_model == 1) {
	    	 currentResolution = 16;
	     }
	     else
	     {
	    	 currentResolution = 32;
	     }*/
	  
	     switch (matrix_model) {  //get this from the preferences
	     case 0:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x16;
	    	 BitmapInputStream = getResources().openRawResource(R.raw.selectimage16);
	    	 currentResolution = 16;
	    	 break;
	     case 1:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.ADAFRUIT_32x16;
	    	 BitmapInputStream = getResources().openRawResource(R.raw.selectimage16);
	    	 currentResolution = 16;
	    	 break;
	     case 2:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x32_NEW; //v1
	    	 BitmapInputStream = getResources().openRawResource(R.raw.selectimage32);
	    	 currentResolution = 32;
	    	 break;
	     case 3:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x32; //v2
	    	 BitmapInputStream = getResources().openRawResource(R.raw.selectimage32);
	    	 currentResolution = 32;
	    	 break;
	     case 4:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_64x32; 
	    	 BitmapInputStream = getResources().openRawResource(R.raw.selectimage64);
	    	 currentResolution = 64; //?
	    	 break;
	     case 5:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_64x64;
	    	 BitmapInputStream = getResources().openRawResource(R.raw.selectimage128);
	    	 currentResolution = 128; //?
	    	 break;	 	 
	     default:	    		 
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x32; //v2 as the default
	    	 BitmapInputStream = getResources().openRawResource(R.raw.selectimage32);
	    	 currentResolution = 32;
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
	  			
	  			pixelHardwareID = ioio_.getImplVersion(v.HARDWARE_VER); 
	  		  			
	  			if (debug_ == true) {  			
		  			showToast("Bluetooth Connected");
	  			}
	  			
	  			matrix_.frame(frame_); 
	  			appAlreadyStarted = 1;
	  		}

	  

			@Override
			public void loop() throws ConnectionLostException {
				
				if (writing == false) {
					synchronized (frame_) {
						try {
							frame_.wait();
							matrix_.frame(frame_);
							
						} catch (InterruptedException e) {
						}
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
	 		alert.setTitle(getResources().getString(R.string.notFoundString)).setIcon(R.drawable.ic_launcher).setMessage(getResources().getString(R.string.bluetoothPairingString)).setNeutralButton(getResources().getString(R.string.OKText), null).show();	
	     }

	private static Camera getCameraInstance() {
		try {
			Log.i(TAG, "Found camera");
			return Camera.open();
		//	showToast("got camera");
		} catch (Exception e) {
			Log.e(TAG, "Failed to open camera.", e);
			//showToast("no camera found");
		}
		return null;
	}
	
	/** A safe way to get an instance of the Camera object. *//*
	public static Camera getCameraInstance(){
	    Camera c = null;
	    try {
	        c = Camera.open(); // attempt to get a Camera instance
	    }
	    catch (Exception e){
	    	Log.e(TAG, "Failed to open camera.", e);
	    }
	    return c; // returns null if camera is unavailable
	}*/

	// From: http://lanedetectionandroid.googlecode.com/svn-history/r8/trunk/tests/TestJniCall/src/org/siprop/opencv/Preview.java
	static public void YuvtoRGB565(byte[] yuvs, int width, int height, short[] rgbs) {
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