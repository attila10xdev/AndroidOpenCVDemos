package hu.uszeged.ipcg.opencvlivenativetest;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 {
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private AppCompatActivity mActivity = null;

    private CameraBridgeViewBase   mOpenCvCameraView;
    private TextView tv1;
    private Button btn1;
    private SeekBar seekBar;
    private boolean mProcessing;
    private enum operationTypes { OPENCV_THRESH, JAVA_THRESH, JAVA_THRESH_32, OPENCV_NATIVE }
    operationTypes mOpType;
    private int paramValue;
    private Mat workMat, retMat, retMatRgb;
    private byte[] frameData;
    private int[] frameData32;
    private String nativeResult;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            Log.d("status code", Integer.toString(status));

            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    System.loadLibrary("ocv_jni");
                    mOpenCvCameraView.enableView();
                    Log.d("enabled view", "enabled");
                    paramValue = 128;
                    mOpType = operationTypes.OPENCV_THRESH;
                    mProcessing = false;
                    updateInformationText();
                    nativeResult = "No result.";
                } break;
                default: {

                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById( R.id.cameraView );
        mOpenCvCameraView.setCvCameraViewListener(this);
        //mOpenCvCameraView.setMaxFrameSize( 640, 480 );
        //mOpenCvCameraView.enableFpsMeter();

        tv1 = (TextView) findViewById( R.id.tv1 );

        btn1 = (Button) findViewById( R.id.btn1 );
        btn1.setOnClickListener(mBtn1Listener);

        seekBar = (SeekBar) findViewById( R.id.slider );
        seekBar.setMax( 255 );
        seekBar.incrementProgressBy( 1 );
        seekBar.setProgress( 128 );
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                paramValue = progress;
                updateInformationText();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();
        //if (id == R.id.action_settings)
        //{
        //    return true;
        //}
        //return super.onOptionsItemSelected(item);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if( Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 ) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission();
                //return;
            } else {
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
            }
        } else {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        }
    }

    private void requestCameraPermission() {
        if( ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            // Explanation must be shown because the user revoked previously granted camera permission!
            new AlertDialog.Builder(mActivity)
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(mActivity,
                                    new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mActivity.finish();
                                }
                            })
                    .show();
        } else {
            // No explanation needed, just ask for permission...
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);
            }else {
                // User gave no camera permission...
                mActivity.finish();
            }
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        frameData = new byte [ width * height ];
        frameData32 = new int [ width * height ];
        retMat = new Mat( height, width, CvType.CV_8UC1 );
        retMatRgb = new Mat( height, width, CvType.CV_8UC4);
        workMat = new Mat( height, width, CvType.CV_32SC1 );
    }

    @Override
    public void onCameraViewStopped() {
    }

    private void updateInformationText() {
        switch( mOpType ) {
            case OPENCV_THRESH:
                tv1.setText(String.format(getString(R.string.tv_caption_op1), paramValue));
                break;
            case JAVA_THRESH:
            case JAVA_THRESH_32:
                tv1.setText(String.format(getString(R.string.tv_caption_op2), paramValue));
                break;
            case OPENCV_NATIVE:
                tv1.setText( nativeResult );
                break;
        }
    }

    private View.OnClickListener mBtn1Listener = new View.OnClickListener() {
        public void onClick(View v) {
            if( mOpType == operationTypes.OPENCV_THRESH ) {
                mOpType = operationTypes.OPENCV_NATIVE;
            } else {
                mOpType = operationTypes.OPENCV_THRESH;
            }

            updateInformationText();
        }
    };

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat gray = inputFrame.gray();

        if( !mProcessing ) {
            mProcessing = true;
            //Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);
            switch( mOpType ) {
                case OPENCV_THRESH:
                    Imgproc.threshold(gray, retMat, paramValue, 255, Imgproc.THRESH_BINARY);
                    //Core.bitwise_not( gray, retMat );
                    break;
                case JAVA_THRESH:
                    gray.get( 0, 0, frameData);
                    for( int pos = 0, y = 0; y < gray.rows(); y++) {
                        for( int x = 0; x < gray.cols(); x++, pos++ ) {
                            if( (frameData[ pos ] & 255) > paramValue ) {
                                frameData[ pos ] = (byte)255;
                            } else {
                                frameData[ pos ] = (byte)0;
                            }
                        }
                    }
                    retMat.put( 0, 0, frameData );
                    break;
                case JAVA_THRESH_32:
                    gray.convertTo( workMat, CvType.CV_32SC1 );
                    workMat.get( 0, 0, frameData32 );
                    for( int pos = 0, y = 0; y < gray.rows(); y++ ) {
                        for( int x = 0; x < gray.cols(); x++, pos++ ) {
                            if( frameData32[ pos ]  > paramValue ) {
                                frameData32[ pos ] = 255;
                            } else {
                                frameData32[ pos ] = 0;
                            }
                        }
                    }
                    workMat.put( 0, 0, frameData32 );
                    workMat.convertTo( retMat, CvType.CV_8UC1 );
                    break;
                case OPENCV_NATIVE:
                    nativeResult = OcvProcess( gray.getNativeObjAddr(), retMat.getNativeObjAddr(), paramValue );
                    runOnUiThread(new Runnable() {
                        public void run() {
                            updateInformationText();
                        }
                    });
                    break;
            }
            mProcessing = false;
        }

        return retMat;
    }

    public native String OcvProcess( long inGray, long outGray, int paramValue );

    /*
    // When using OpenCV Manager, native library can be loaded when the initialization successful.
    // Uncomment this part when linking OpenCV statically.
    static {
        System.loadLibrary("ocv_jni");
    }*/
}
