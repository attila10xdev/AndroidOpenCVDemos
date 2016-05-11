#include <jni.h>
#include <string.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>

#include <cmath>
#include <queue>
#include <vector>
#include <math.h>

using namespace std;
using namespace cv;

Mat result;

extern "C" {
JNIEXPORT jstring JNICALL Java_hu_uszeged_ipcg_opencvlivenativetest_MainActivity_OcvProcess( JNIEnv* env,
                                                  jobject thiz, jlong addrIn, jlong addrOut, jint thval )
{
	/*
    Mat& mGr  = *(Mat*)addrIn;
    Mat& mRgb = *(Mat*)addrOut;
	cvtColor( mGr, mRgb, CV_GRAY2BGRA );
    vector<KeyPoint> v;

    FastFeatureDetector detector(50);
    detector.detect(mGr, v);
    for( unsigned int i = 0; i < v.size(); i++ )
    {
        const KeyPoint& kp = v[i];
        circle(mRgb, Point(kp.pt.x, kp.pt.y), 10, Scalar(255,0,0,255));
    }
	*/

	Mat &mIn = *(Mat *)addrIn; 
	Mat& mOut = *(Mat*)addrOut;
	threshold( mIn, mOut, thval, 255, THRESH_BINARY );

	char buffer[128];
	//sprintf( buffer, "Hello from JNI!" );
	sprintf( buffer, "Hello from JNI! Width = %d", mIn.cols );
	return env->NewStringUTF( buffer );	
}
}
