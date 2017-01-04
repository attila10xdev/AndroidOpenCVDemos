package hu.uszeged.ipcg.opencvlivehistogramdemo;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OpenCVLiveHistogramDemoMainActivity
        extends AppCompatActivity
        implements CvCameraViewListener2, OnItemSelectedListener {
    /*
    TODO:
    - Változtatható élőkép méret
    - Hisztogramrajzolás memóriahasználatát átnézni
    - Fotókészítés (szürke/színes + aktuális trafó)
    - Üzenet kilépés előtt, ha nincs kamera engedély

    Link:
    - http://stackoverflow.com/questions/2661536/how-to-programmatically-take-a-screenshot-in-android
    - http://stackoverflow.com/questions/30196965/how-to-take-a-screenshot-of-current-activity-and-then-share-it
     */
    private static final String TAG = OpenCVLiveHistogramDemoMainActivity.class.getSimpleName();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private AppCompatActivity mActivity = null;

    private CameraBridgeViewBase   mOpenCvCameraView;
    private int th1Value = 128;
    private int th2Value = 64;
    private SeekBar seekBar, seekBar2;
    private TextView tv1;
    private Bitmap bmHistogram;
    private ImageView ivHistogram;
    private enum operationTypes { ORIGINAL, HISTEQ, CONTRASTSTRETCH }
    private operationTypes mOpType;
    private boolean mProcessing;
    private Mat retMat, retMatRgb, ycrcb, yChannel;
    private boolean mDrawHistogram;
    private boolean mIsColorProcessing;
    private Spinner sp_color, sp_method;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            Log.d("status code", Integer.toString(status));

            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                    Log.d(TAG, "OpenCV loaded successfully");
                    bmHistogram = Bitmap.createBitmap( 514, 200, Bitmap.Config.ARGB_8888 );
                    updateInformationText();
                    mProcessing = false;
                    mDrawHistogram = true;
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
        setContentView(R.layout.activity_opencv_live_histogram_demo_main);
        mActivity = this;

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById( R.id.cameraView );
        mOpenCvCameraView.setCvCameraViewListener(this);
        //mOpenCvCameraView.setMaxFrameSize( 640, 480 );
        //mOpenCvCameraView.enableFpsMeter();

        mOpType = operationTypes.ORIGINAL;
        mIsColorProcessing = true;

        // https://www.tutorialspoint.com/android/android_spinner_control.htm
        sp_color = (Spinner) findViewById( R.id.sp_color );
        List<String> categories_color = Arrays.asList( getResources().getStringArray( R.array.sp_color_elements ) );
        ArrayAdapter<String> dataAdapterColor = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories_color);
        dataAdapterColor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_color.setAdapter(dataAdapterColor);
        sp_color.setOnItemSelectedListener(this);

        sp_method = (Spinner) findViewById( R.id.sp_method );
        List<String> categories = Arrays.asList( getResources().getStringArray( R.array.sp_method_elements ) );
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_method.setAdapter(dataAdapter);
        sp_method.setOnItemSelectedListener(this);

        tv1 = (TextView) findViewById( R.id.tv1 );
        tv1.setText(R.string.tv_caption_color);
        ivHistogram = (ImageView) findViewById( R.id.iv_histogram );

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
                th1Value = progress;
                updateInformationText();
            }
        });

        seekBar2 = (SeekBar) findViewById( R.id.slider2 );
        seekBar2.setMax( 255 );
        seekBar2.setProgress( 64 );
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
                th2Value = progress;
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

    private void updateInformationText() {
        switch( mOpType ) {
            case ORIGINAL:
                if( mIsColorProcessing ) {
                    tv1.setText( R.string.tv_caption_color );
                } else {
                    tv1.setText( R.string.tv_caption_gray );
                }
                seekBar.setVisibility( View.GONE );
                seekBar2.setVisibility( View.GONE );
                break;
            case HISTEQ:
                if( mIsColorProcessing ) {
                    tv1.setText( R.string.tv_caption_coloreq );
                } else {
                    tv1.setText( R.string.tv_caption_grayeq );
                }
                seekBar.setVisibility( View.GONE );
                seekBar2.setVisibility( View.GONE );
                break;
            case CONTRASTSTRETCH:
                int th_lower = th1Value < th2Value ? th1Value : th2Value;
                int th_upper = th1Value > th2Value ? th1Value : th2Value;
                if( mIsColorProcessing ) {
                    tv1.setText( String.format( getString( R.string.tv_caption_colorstretch ), th_lower, th_upper ));
                } else {
                    tv1.setText( String.format( getString( R.string.tv_caption_graystretch ), th_lower, th_upper ));
                }
                seekBar.setVisibility( View.VISIBLE );
                seekBar2.setVisibility( View.VISIBLE );
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        //String item = parent.getItemAtPosition(position).toString();
        //Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_LONG).show();

        if( parent == sp_color ) {
            switch( position ) {
                case 0:
                    mIsColorProcessing = true;
                    break;
                case 1:
                    mIsColorProcessing = false;
                    break;
            }
        }

        if( parent == sp_method ) {
            switch( position ) {
                case 0:
                    mOpType = operationTypes.ORIGINAL;
                    break;
                case 1:
                    mOpType = operationTypes.HISTEQ;
                    break;
                case 2:
                    mOpType = operationTypes.CONTRASTSTRETCH;
            }
        }

        updateInformationText();
    }
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        retMat = new Mat( height, width, CvType.CV_8UC1 );
        retMatRgb = new Mat( height, width, CvType.CV_8UC4 );
        ycrcb = new Mat( height, width, CvType.CV_8UC3 );
        yChannel = new Mat( height, width, CvType.CV_8UC1 );
        Toast.makeText(this, R.string.warning_experimental, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat iframe;
        if( mIsColorProcessing ) {
            iframe = inputFrame.rgba();
        } else {
            iframe = inputFrame.gray();
        }
        //Log.d("LiveHistogram", "Mat size: " + iframe.size() );

        if( !mProcessing ) {
            mProcessing = true;
            switch ( mOpType ) {
                case ORIGINAL:
                    if( mIsColorProcessing ) {
                        if( iframe.channels() == 4 ) {
                            iframe.copyTo( retMatRgb );
                        }
                    } else {
                        if( iframe.channels() == 1 ) {
                            iframe.copyTo( retMat );
                        }
                    }
                    break;
                case HISTEQ:
                    if( mIsColorProcessing ) {
                        Imgproc.cvtColor(iframe, ycrcb, Imgproc.COLOR_RGB2YCrCb);
                        // Core.split() has a severe memory leak problem!
                        // Must use extractChannel() and insertChannel()!
                        Core.extractChannel( ycrcb, yChannel, 0 );
                        Imgproc.equalizeHist( yChannel, yChannel );
                        Core.insertChannel( yChannel, ycrcb, 0);
                        Imgproc.cvtColor(ycrcb, retMatRgb, Imgproc.COLOR_YCrCb2RGB);
                    } else {
                        if( iframe.channels() == 1 ) {
                            Imgproc.equalizeHist( iframe, iframe );
                            iframe.copyTo( retMat );
                        }
                    }
                    break;
                case CONTRASTSTRETCH:
                    if( mIsColorProcessing ) {
                        int th_lower = th1Value < th2Value ? th1Value : th2Value;
                        int th_upper = th1Value > th2Value ? th1Value : th2Value;
                        Imgproc.cvtColor(iframe, ycrcb, Imgproc.COLOR_RGB2YCrCb);
                        // Core.split() has a severe memory leak problem!
                        // Must use extractChannel() and insertChannel()!
                        Core.extractChannel(ycrcb, yChannel, 0 );
                        Imgproc.threshold( yChannel, yChannel, th_upper, 0, Imgproc.THRESH_TRUNC );
                        // Bit strange, but found no straight solution for clipping
                        Core.bitwise_not(yChannel, yChannel); // Grayscale inverse
                        Imgproc.threshold(yChannel, yChannel, 255 - th_lower, 0, Imgproc.THRESH_TRUNC);
                        Core.bitwise_not(yChannel, yChannel);
                        Core.normalize(yChannel, yChannel, 0, 255, Core.NORM_MINMAX);
                        Core.insertChannel( yChannel, ycrcb, 0);
                        Imgproc.cvtColor(ycrcb, retMatRgb, Imgproc.COLOR_YCrCb2RGB);
                    } else {
                        if( iframe.channels() == 1 ) {
                            // Preserve iframe for histogram plot
                            iframe.copyTo( yChannel );
                            int th_lower = th1Value < th2Value ? th1Value : th2Value;
                            int th_upper = th1Value > th2Value ? th1Value : th2Value;
                            Imgproc.threshold( yChannel, yChannel, th_upper, 0, Imgproc.THRESH_TRUNC );
                            // Bit strange, but found no straight solution for clipping
                            Core.bitwise_not(yChannel, yChannel); // Grayscale inverse
                            Imgproc.threshold(yChannel, yChannel, 255 - th_lower, 0, Imgproc.THRESH_TRUNC);
                            Core.bitwise_not(yChannel, yChannel);
                            Core.normalize(yChannel, yChannel, 0, 255, Core.NORM_MINMAX);
                            yChannel.copyTo( retMat );
                        }
                    }
                    break;
            }

            if( !mIsColorProcessing ) {
                Mat histImage = new Mat();
                if( mDrawHistogram ) {
                    List<Mat> channel = new ArrayList<>();
                    channel.add( iframe );
                    Mat histogram = new Mat();
                    MatOfFloat ranges = new MatOfFloat( 0, 256 );
                    MatOfInt histSize = new MatOfInt( 256 );
                    Imgproc.calcHist(channel, new MatOfInt(0), new Mat(), histogram, histSize, ranges);
                    //histImage = new Mat( 200, 2 * (int)histSize.get( 0, 0 )[ 0 ] + 2, CvType.CV_8UC3 );
                    histImage = new Mat( 200, 2 * 256 + 2, CvType.CV_8UC3 );
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

                    //if( mOpType != operationTypes.GRAY && mOpType != operationTypes.COLOR_HISTEQ ) {
                    if( mOpType == operationTypes.CONTRASTSTRETCH ) {
                        if( mIsColorProcessing ) {
                            // TODO: Implement color histogram drawing!
                        } else {
                            int th_lower = th1Value < th2Value ? th1Value : th2Value;
                            int th_upper = th1Value > th2Value ? th1Value : th2Value;
                            Imgproc.line(histImage,
                                    new Point(2 * th_lower + 1, histImage.rows()),
                                    new Point(2 * th_lower + 1, histImage.rows() - 200),
                                    new Scalar( 255, 255, 255 ),
                                    3, 8, 0 );
                            Imgproc.line(histImage,
                                    new Point(2 * th_lower + 1, histImage.rows()),
                                    new Point(0, histImage.rows() - 100),
                                    new Scalar( 255, 255, 255 ),
                                    3, 8, 0 );
                            Imgproc.line(histImage,
                                    new Point(2 * th_lower + 1, histImage.rows() - 200),
                                    new Point(0, histImage.rows() - 100),
                                    new Scalar( 255, 255, 255 ),
                                    3, 8, 0 );

                            Imgproc.line(histImage,
                                    new Point(2 * th_upper + 1, histImage.rows()),
                                    new Point(2 * th_upper + 1, histImage.rows() - 200),
                                    new Scalar( 255, 255, 255 ),
                                    3, 8, 0 );
                            Imgproc.line(histImage,
                                    new Point(2 * th_upper + 1, histImage.rows()),
                                    new Point(515, histImage.rows() - 100),
                                    new Scalar( 255, 255, 255 ),
                                    3, 8, 0 );
                            Imgproc.line(histImage,
                                    new Point(2 * th_upper + 1, histImage.rows() - 200),
                                    new Point(515, histImage.rows() - 100),
                                    new Scalar( 255, 255, 255 ),
                                    3, 8, 0 );
                        }
                    }
                }

                List<Mat> result = new ArrayList<>(3);
                result.add( retMat );
                result.add(retMat);
                result.add(retMat);
                Core.merge(result, retMatRgb);

                if( mDrawHistogram ) {
                    //Mat submat = retMatRgb.submat( 100, 100 + histImage.height(), 0, histImage.width() );
                    //histImage.copyTo( submat );
                    Utils.matToBitmap(histImage, bmHistogram);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            ivHistogram.setVisibility( View.VISIBLE );
                            ivHistogram.setImageBitmap(bmHistogram);
                        }
                    });
                }
            } else { // Color histogram is not yet implemented
                runOnUiThread(new Runnable() {
                    public void run() {
                        ivHistogram.setVisibility( View.GONE );
                    }
                });
            }

            mProcessing = false;
        }

        System.gc();
        return retMatRgb;
    }
}
