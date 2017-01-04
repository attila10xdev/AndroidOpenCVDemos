package hu.uszeged.ipcg.opencvlivethresholddemo;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.*;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class OpenCVLiveThresholdDemoMainActivity
        extends AppCompatActivity
        implements CvCameraViewListener2 {
    /*
    TODO:
    - Változtatható élőkép méret
    - Képernyőmérettől függő hisztogramkép méret
    - Hisztogram rajzolás kiszervezése külön View osztályba
    - Fotókészítés (szürke + aktuális trafó)
        - http://stackoverflow.com/questions/2661536/how-to-programmatically-take-a-screenshot-in-android
        - http://stackoverflow.com/questions/30196965/how-to-take-a-screenshot-of-current-activity-and-then-share-it
    - Rádiógombok a funkciókhoz
    - Módszerenként különböző intervallumú csúszka
    - Niblack
    - Üzenet kilépés előtt, ha nincs kamera engedély
    - FPS számolás
    Kész, tesztelni:
    - Csúszkák távolsága a felületen
    - Képmérettől függő kezdeti ablakméret paraméter az adaptív küszöböléshez
     */
    private static final String TAG = OpenCVLiveThresholdDemoMainActivity.class.getSimpleName();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private AppCompatActivity mActivity = null;

    private CameraBridgeViewBase   mOpenCvCameraView;
    private int thresValue = 128;
    private int sizeValue = 25;
    private SeekBar seekBar, seekBar2;
    private TextView tv1;
    private Button btn3, btn6;
    private Bitmap bmHistogram;
    private ImageView ivHistogram;
    private enum operationTypes { GRAY, GLOBAL_THRESH, OTSU, ADAPTIVE_THRESH_1, ADAPTIVE_THRESH_2 }
    private operationTypes mOpType;
    private int mThreshType;
    private boolean mProcessing;
    private Mat retMat, retMatRgb;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            Log.d("status code", Integer.toString(status));

            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                    Log.d(TAG, "OpenCV loaded successfully");
                    mProcessing = false;
                    mOpType = operationTypes.GLOBAL_THRESH;
                    mThreshType = Imgproc.THRESH_BINARY;
                    bmHistogram = Bitmap.createBitmap( 514, 200, Bitmap.Config.ARGB_8888 );
                    updateInformationText();
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
        setContentView(R.layout.activity_opencv_live_threshold_main);
        mActivity = this;

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById( R.id.cameraView );
        mOpenCvCameraView.setCvCameraViewListener(this);
        //mOpenCvCameraView.setMaxFrameSize( 320, 240 );
        //mOpenCvCameraView.enableFpsMeter();

        tv1 = (TextView) findViewById( R.id.tv1 );
        ivHistogram = (ImageView) findViewById( R.id.iv_histogram );

        Button btn1 = (Button) findViewById( R.id.btn1 );
        btn1.setOnClickListener(mBtn1Listener);

        Button btn2 = (Button) findViewById( R.id.btn2 );
        btn2.setOnClickListener(mBtn2Listener);

        btn3 = (Button) findViewById( R.id.btn3 );
        btn3.setOnClickListener( mBtn3Listener );

        Button btn4 = (Button) findViewById( R.id.btn4 );
        btn4.setOnClickListener(mBtn4Listener);

        Button btn5 = (Button) findViewById( R.id.btn5 );
        btn5.setOnClickListener( mBtn5Listener );

        btn6 = (Button) findViewById( R.id.btn6 );
        btn6.setOnClickListener( mBtn6Listener );

        seekBar = (SeekBar) findViewById( R.id.slider );
        seekBar.setMax( 255 );
        seekBar.setProgress( 128 );
        seekBar.incrementProgressBy( 1 );
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                thresValue = progress;
                updateInformationText();
            }
        });

        seekBar2 = (SeekBar) findViewById( R.id.slider2 );
        seekBar2.setVisibility( View.GONE );
        seekBar2.setMax( 49 );
        seekBar2.setProgress( 11 ); // 11 * 2 + 3 = 25
        seekBar2.incrementProgressBy( 1 );
        seekBar2.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sizeValue = progress * 2 + 3;
                updateInformationText();
            }
        });
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
                OpenCVLoader.initAsync( OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback );
            }
        } else {
            OpenCVLoader.initAsync( OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback );
        }
    }

    private void requestCameraPermission() {
        if( ActivityCompat.shouldShowRequestPermissionRationale( this, Manifest.permission.CAMERA )) {
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
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
            }else {
                // User gave no camera permission...
                mActivity.finish();
            }
        }
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

    private void computeInitialAdaptiveWindowSize( int width ) {
        sizeValue = ( width / 80 );
        if( sizeValue % 2 == 0 ) {
            sizeValue++;
        }
        //seekBar2.setProgress( ( sizeValue - 3 ) / 2  ); // 11 * 2 + 3 = 25
        //Log.d( "LiveThresholdDemo", "Computed sizeValue: " + sizeValue );
    }

    private void updateInformationText() {
        switch(mOpType) {
            case GRAY:
                tv1.setText( R.string.tv_caption_gray );
                break;
            case GLOBAL_THRESH:
                tv1.setText(String.format(getString(R.string.tv_caption_globalth), thresValue));
                break;
            case OTSU:
                tv1.setText(String.format(getString(R.string.tv_caption_otsu), thresValue));
                break;
            case ADAPTIVE_THRESH_1:
                if( thresValue >= 128) {
                    tv1.setText(String.format(getString(R.string.tv_caption_adaptive1plus), sizeValue, sizeValue, thresValue - 128));
                } else {
                    tv1.setText(String.format(getString(R.string.tv_caption_adaptive1minus), sizeValue, sizeValue, Math.abs(thresValue - 128)));
                }
                break;
            case ADAPTIVE_THRESH_2:
                if( thresValue >= 128 ) {
                    tv1.setText(String.format(getString(R.string.tv_caption_adaptive2plus), sizeValue, sizeValue, thresValue - 128));
                } else {
                    tv1.setText(String.format(getString(R.string.tv_caption_adaptive2minus), sizeValue, sizeValue, Math.abs(thresValue - 128)));
                }
                break;
        }

    }

    private View.OnClickListener mBtn1Listener = new View.OnClickListener() {
        public void onClick(View v) {
            mOpType = operationTypes.GRAY;
            seekBar.setVisibility( View.GONE );
            seekBar2.setVisibility( View.GONE );
            updateInformationText();
        }
    };

    private View.OnClickListener mBtn2Listener = new View.OnClickListener() {
        public void onClick(View v) {
            mOpType = operationTypes.GLOBAL_THRESH;
            seekBar.setVisibility( View.VISIBLE );
            seekBar2.setVisibility( View.GONE );
            updateInformationText();
        }
    };

    private View.OnClickListener mBtn3Listener = new View.OnClickListener() {
        public void onClick(View v) {
            mOpType = operationTypes.OTSU;
            seekBar.setVisibility( View.VISIBLE );
            seekBar2.setVisibility( View.GONE );
            btn3.setSelected(true);
        }
    };

    private View.OnClickListener mBtn4Listener = new View.OnClickListener() {
        public void onClick(View v) {
            mOpType = operationTypes.ADAPTIVE_THRESH_1;
            seekBar.setVisibility( View.VISIBLE );
            seekBar2.setVisibility( View.VISIBLE );
            updateInformationText();
        }
    };

    private View.OnClickListener mBtn5Listener = new View.OnClickListener() {
        public void onClick(View v) {
            mOpType = operationTypes.ADAPTIVE_THRESH_2;
            seekBar.setVisibility( View.VISIBLE );
            seekBar2.setVisibility( View.VISIBLE );
            updateInformationText();
        }
    };

    private View.OnClickListener mBtn6Listener = new View.OnClickListener() {
        public void onClick(View v) {
            if( mThreshType == Imgproc.THRESH_BINARY ) {
                mThreshType = Imgproc.THRESH_BINARY_INV;
                btn6.setText( R.string.btn_caption_THRESH_BINARY_INV );
            } else {
                mThreshType = Imgproc.THRESH_BINARY;
                btn6.setText( R.string.btn_caption_THRESH_BINARY );
            }
            updateInformationText();
        }
    };

    @Override
    public void onCameraViewStarted(int width, int height) {
        retMat = new Mat( height, width, CvType.CV_8UC1 );
        retMatRgb = new Mat( height, width, CvType.CV_8UC4 );
        computeInitialAdaptiveWindowSize( width );
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat gray = inputFrame.gray();

        if( !mProcessing ) {
            mProcessing = true;
            //Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);
            //Imgproc.equalizeHist( gray, gray );
            switch (mOpType) {
                case GRAY:
                    gray.copyTo( retMat );
                    break;
                case GLOBAL_THRESH:
                    Imgproc.threshold( gray, retMat, thresValue, 255, mThreshType );
                    break;
                case OTSU:
                    double th = Imgproc.threshold( gray, retMat, 0, 255, mThreshType + Imgproc.THRESH_OTSU );
                    thresValue = (int) th;
                    final int ith = (int) th;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            seekBar.setProgress(ith);
                            updateInformationText();
                        }
                    });
                    break;
                case ADAPTIVE_THRESH_1:
                    /*
                        Setting adaptive threshold parameter C makes more sense this way.
                        Global threshold and adaptive ones can share the same slider.
                        Higher slider value represent higher threshold.
                    */
                    Imgproc.adaptiveThreshold( gray, retMat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, mThreshType, sizeValue, -1 * (thresValue - 128 ));
                    break;
                case ADAPTIVE_THRESH_2:
                    /*
                        Setting adaptive threshold parameter C makes more sense this way.
                        Global threshold and adaptive ones can share the same slider.
                        Higher slider value represent higher threshold.
                    */
                    Imgproc.adaptiveThreshold( gray, retMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, mThreshType, sizeValue, -1 * (thresValue - 128) );
                    break;
            }

            List<Mat> channel = new ArrayList<>();
            channel.add( gray );
            Mat histogram = new Mat();
            MatOfFloat ranges = new MatOfFloat( 0, 256 );
            MatOfInt histSize = new MatOfInt( 256 );
            Imgproc.calcHist(channel, new MatOfInt(0), new Mat(), histogram, histSize, ranges);
            Mat histImage = new Mat( 200, 2 * (int)histSize.get( 0, 0 )[ 0 ] + 2, CvType.CV_8UC3 );
            //histImage.setTo(new Scalar(192, 192, 192));
            histImage.setTo( new Scalar( 0x5a, 0x59, 0x5b ) );
            Core.normalize( histogram, histogram, 1, histImage.rows() , Core.NORM_MINMAX, -1, new Mat() );
            for( int i = 0; i < (int)histSize.get(0, 0)[0]; i++ ) {
                Imgproc.line( histImage,
                        new Point( 2 * i + 1, histImage.rows() ),
                        new Point( 2 * i + 1, histImage.rows() - Math.round( histogram.get(i, 0)[0]) ),
                        new Scalar( 255, 64, 129 ),
                        3, 8, 0 );
            }
            if( mOpType != operationTypes.GRAY && mOpType != operationTypes.ADAPTIVE_THRESH_1 && mOpType != operationTypes.ADAPTIVE_THRESH_2 ) {
                Imgproc.line( histImage,
                        new Point( 2 * thresValue + 1, histImage.rows() ),
                        new Point( 2 * thresValue + 1, histImage.rows() - 200 ),
                        new Scalar( 255, 255, 255 ),
                        3, 8, 0 );
            }

            List<Mat> result = new ArrayList<>();
            result.add( retMat );
            result.add( retMat );
            result.add( retMat );
            Core.merge(result, retMatRgb);

            //Mat submat = retMatRgb.submat( 100, 100 + histImage.height(), 0, histImage.width() );
            //histImage.copyTo(submat);
            Utils.matToBitmap( histImage, bmHistogram );
            runOnUiThread(new Runnable() {
                public void run() {
                    ivHistogram.setImageBitmap( bmHistogram );
                }
            });

            mProcessing = false;
        }

        return retMatRgb;
    }
}
