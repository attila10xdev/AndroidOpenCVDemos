package hu.uszeged.ipcg.opencvtest;

import android.os.Bundle;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class OpenCVTestMainActivity extends AppCompatActivity {
    private static final String TAG = OpenCVTestMainActivity.class.getSimpleName();
    ImageView iv;
    TextView tv;
    Bitmap bm;
    Button btn, btn2, btn3;

    // The OpenCV loader callback.
    private BaseLoaderCallback mLoaderCallback =
            new BaseLoaderCallback(this) {
                @Override
                public void onManagerConnected(final int status) {
                    switch (status) {
                        case LoaderCallbackInterface.SUCCESS:
                            Log.d(TAG, "OpenCV loaded successfully");
                            tv.setText( "OpenCV loaded successfully" );
                            break;
                        default:
                            super.onManagerConnected(status);
                            break;
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opencv_main);
        bm = BitmapFactory.decodeResource(getResources(), R.drawable.t68ikamera);
        iv = (ImageView)findViewById(R.id.imageView1);
        iv.setImageBitmap( bm );
        tv = (TextView)findViewById(R.id.textView1);
        btn = (Button)findViewById(R.id.button1);
        btn.setOnClickListener(mBlurFlipBtnListener);
        btn2 = (Button)findViewById(R.id.button2);
        btn2.setOnClickListener(mEdgeBtnListener);
        btn2 = (Button)findViewById(R.id.button3);
        btn2.setOnClickListener(mReloadBtnListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);
    }

    private OnClickListener mBlurFlipBtnListener = new OnClickListener() {
        public void onClick(View v) {
            tv.setText("BlurFlip button is pressed...");
            Mat im = new Mat();
            Utils.bitmapToMat( bm, im );
            Imgproc.cvtColor( im, im, Imgproc.COLOR_RGB2GRAY );
            Imgproc.blur( im, im, new Size(15,15) );
            Core.flip( im, im, 1 );
            Utils.matToBitmap( im, bm );
            iv.setImageBitmap( bm );
        }
    };

    private OnClickListener mEdgeBtnListener = new OnClickListener() {
        public void onClick(View v) {
            tv.setText("Edge button is pressed...");
            Mat im = new Mat();
            Utils.bitmapToMat(bm, im);
            Mat edges = new Mat();
            //Imgproc.Canny(im, edges, 125, 350);
            Imgproc.Canny( im, edges, 125, 350, 3, false );
            Utils.matToBitmap( edges, bm );
            iv.setImageBitmap( bm );
        }
    };

    private OnClickListener mReloadBtnListener = new OnClickListener() {
        public void onClick(View v) {
            tv.setText("Reload button is pressed...");
            bm = BitmapFactory.decodeResource(getResources(), R.drawable.t68ikamera);
            iv.setImageBitmap( bm );
        }
    };
}
