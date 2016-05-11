#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>

#include <cmath>
#include <string>
#include <queue>
#include <vector>
#include <math.h>

using namespace std;
using namespace cv;

Mat result;
int drawBlobContours = 1;
int drawBestBlobContour = 1;
int drawNumberPlateBox = 1;
int compensateRotation = 1;
double im_size_percentage_x1, im_size_percentage_x2, im_size_percentage_y1, im_size_percentage_y2;

int niblack( cv::Mat &src, cv::Mat &res, int wing_min = 5, double koeff = 0.2 )
{
  res = src.clone();
  //int wing_min = 5;
  int wing_max = wing_min *32;

  Mat sum, sumsq;
  cv::integral( src, sum, sumsq );

  double avesigma = 0;
  int avesigma_cnt = 0;

  for (int y=0; y<src.rows; y++)
    for (int x=0; x<src.cols; x++)
    {
      for (int wing = wing_min; wing <= wing_max; wing*=2 )
      {
				int sizex = min(wing, x) + min(wing, src.cols - x - 1) + 1;
				int sizey = min(wing, y) + min(wing, src.rows - y - 1) + 1;
				double size = sizex * sizey;

        Point from = Point(max(0, x - wing), max(0, y - wing));
				Point to = Point(min(src.cols, x + wing + 1), min(src.rows, y + wing + 1));
				int s = sum.at<int>(to.y, to.x) - sum.at<int>(from.y, to.x) - sum.at<int>(to.y, from.x) + sum.at<int>(from.y, from.x);
				double sq = sumsq.at<double>(to.y, to.x) - sumsq.at<double>(from.y, to.x) - sumsq.at<double>(to.y, from.x) + sumsq.at<double>(from.y, from.x);
				
				double E = double(s) / size;
				double sigma = sqrt( (double(sq) / size) - E*E );

        avesigma += sigma;
        avesigma_cnt ++;

        //double koeff = 0.2;

        if ( koeff * sigma < 4 && (wing/wing_min) >= 4 )
        {
          res.at<uchar>(y,x) = E > 128 ? 255 : 64; /////(128+64) : 64;
          break;
        }

        int T1 = int( E - koeff * sigma  +0.5 );
        int T2 = int( E + koeff * sigma  +0.5 );


        int srcxy = src.at<uchar>(y,x);


        if ( srcxy < T1 )
        {
          res.at<uchar>(y,x) = 0;
          break;
        }
        else if ( srcxy > T2 )
        {
          res.at<uchar>(y,x) = 255;
          break;
        }
        else
        {
          res.at<uchar>(y,x) = 128;
        }
      }

				//cerr << E[0] << " " << E[1] << " " << E[2] << endl;
	
				//ave(y, x) = E;
				//avesq_minus_sqave(y, x) = D;


      ////////
      ////////int wing = wing_min;
      ////////double Esq = 
      ////////while ( sigma( x, y, sum, sumsq, wing ) < min_sigma && wing <= wing_max )
      ////////  wing *=2;

    }
    avesigma /= avesigma_cnt;
    //printf( "avesigma = %f", avesigma );
  return 0;
}

cv::Mat rotate( cv::Mat src, double angle )
{
    cv::Mat dst;
    cv::Point2f pt(src.cols/2., src.rows/2.);   
    cv::Mat r = getRotationMatrix2D(pt, angle, 1.0);
	warpAffine(src, dst, r, cv::Size(src.cols, src.rows), cv::INTER_AREA);

	return dst;
}

//
// Median
// http://stackoverflow.com/questions/2114797/compute-median-of-values-stored-in-vector-c
//

int CalcMHWScore(std::vector<int> scores)
{
  int median;
  size_t size = scores.size();

  std::sort(scores.begin(), scores.end());
  median = scores[size / 2];

  /*
  if (size  % 2 == 0)
  {
      median = (scores[size / 2 - 1] + scores[size / 2]) / 2;
  }
  else 
  {
      median = scores[size / 2];
  }
  */

  return median;
}

//

void RemoveSmallComponents( cv::Mat &in, cv::Mat &out, int min_size, int max_size )
{
	std::queue<cv::Point> cq;
	std::vector<cv::Point> vec_remove;
	int x, y, i;
	cv::Mat temp = cv::Mat::zeros( in.size().height, in.size().width, CV_8UC1 );
	out = in.clone();
	int w = in.size().width;
	int h = in.size().height;

	//
	// Make sure that the border contains no object points
	//
	for( y = 0; y < h; y++ )
	{
		out.at<uchar>( y, 0 ) = 0;
		out.at<uchar>( y, w - 1 ) = 0;
	}

	for( x = 0; x < w; x++ )
	{
		out.at<uchar>( 0, x ) = 0;
		out.at<uchar>( h - 1, x ) = 0;
	}

	//
	// Start
	//

	cv::Point p, p1;
	int counter;
	//for( y = h * 0.2; y < h * 0.8; y++ )
	//	for( x = w * 0.1; x < w * 0.7; x++ )
	for( y = 0; y < h - 1; y++ )
		for( x = 0; x < w - 1; x++ )
	//x = 276;
	//y = 284;
		{
			p1 = cv::Point( x, y );
			if( ( temp.at<uchar>( p1 ) == 0 ) && ( out.at<uchar>( p1 ) > 0 ) )
			{
				//std::cout << p1 << std::endl;
				cq.push( p1 );
				counter = 0;
				while( !cq.empty() )
				{
					p = cq.front();
					//std::cout << p << std::endl;
					cq.pop();
					if( ( ( temp.at<uchar>( p ) == 0 ) ) && ( out.at<uchar>( p ) > 0 ) )
					{
						temp.at<uchar>( p ) = 255;
						counter ++;
						cq.push( cv::Point( p.x - 1, p.y ) );
						cq.push( cv::Point( p.x + 1, p.y ) );
						cq.push( cv::Point( p.x, p.y - 1 ) );
						cq.push( cv::Point( p.x, p.y + 1 ) );
					}
				}
				//std::cout << counter << " ";
				if( ( counter < min_size ) || ( counter > max_size ) )
				{
					vec_remove.push_back( p1 );
				}
			}
		}

		//std::cout << vec_remove.size() << std::endl;
	for( i = 0; i < vec_remove.size(); i++ )
	{
		p1 = vec_remove[i];
		cq.push( p1 );
		while( !cq.empty() )
		{
			p = cq.front();
			cq.pop();
			if( ( temp.at<uchar>( p ) > 0 ) && ( out.at<uchar>( p ) > 0 ) )
			{
				temp.at<uchar>( p ) = 0;
				cq.push( cv::Point( p.x - 1, p.y ) );
				cq.push( cv::Point( p.x + 1, p.y ) );
				cq.push( cv::Point( p.x, p.y - 1 ) );
				cq.push( cv::Point( p.x, p.y + 1 ) );
			}
		}
	}

	out = temp.clone();
}

cv::Mat equalizeIntensity( const cv::Mat &inputImage ) {
	cv::Mat result;

	//std::cout << "equalizeIntensity started..." << std::endl;

	if( inputImage.channels() >= 3 ) {
		cv::Mat ycrcb;
		cv::cvtColor( inputImage, ycrcb, CV_BGR2YCrCb );
		std::vector<cv::Mat> channels;
		cv::split( ycrcb, channels );
		cv::equalizeHist( channels[0], channels[0] );

		cv::merge( channels, ycrcb );
		cv::cvtColor( ycrcb, result, CV_YCrCb2BGR );
	} else {
		cv::equalizeHist( inputImage, result );
	}

	//std::cout << "equalizeIntensity finished..." << std::endl;

	return result;
}

void filterContours( std::vector< std::vector<cv::Point> > &contours, int cmin, int cmax ) {
	double area;
	std::vector< std::vector< cv::Point > >::iterator itc;
	itc = contours.begin();
	while( itc != contours.end() ) {
		area = contourArea( *itc );
		//if( itc->size() < cmin || itc->size() > cmax )
		if( area < cmin || area > cmax )
			itc = contours.erase( itc );
		else {
			++itc;
		}
	}
}

void filterContoursGray( cv::Mat &in_gray, std::vector<std::vector< cv::Point> > &contours, int cmin, int cmax, int th ) {
	int x, y;
	double area;
	std::vector< std::vector< cv::Point > >::iterator itc;
	itc = contours.begin();
	while( itc != contours.end() ) {
		x = itc->data()[0].x;
		y = itc->data()[0].y;
		area = contourArea( *itc );
		if( ( x < 0 ) || ( y < 0 ) || ( y >= in_gray.size().height ) || ( x >= in_gray.size().width ) ) {
			itc = contours.erase( itc );
			continue;
		}
		if( ( area < cmin ) || ( area > cmax ) ||  ( in_gray.at<uchar>( y, x ) > th ) )
			itc = contours.erase( itc );
		else {
			//std::cout << "x = " << x << "; y = " << y << "; length = " << itc->size() << "; area = " << area << std::endl;
			++itc;
		}
	}
}

void drawCircleContourCentroid( cv::Mat &im_rgb, std::vector< std::vector< cv::Point > > &contours, cv::Scalar color ) {
	std::vector< std::vector< cv::Point > >::iterator itc;
	itc = contours.begin();
	while( itc != contours.end() ) {
		cv::Moments mom = cv::moments(cv::Mat(*itc));
		//cv::circle( im_rgb, cv::Point( mom.m10 / mom.m00, mom.m01 / mom.m00 ), 2, cv::Scalar(255, 0, 0), 2 );
		cv::circle( im_rgb, cv::Point( mom.m10 / mom.m00, mom.m01 / mom.m00 ), 2, color, 2 );
		++itc;
	}
}

std::vector<cv::Point> createContoursCentroidList( std::vector< std::vector< cv::Point > > &contours ) {
	std::vector<cv::Point> cl;

	std::vector< std::vector< cv::Point > >::const_iterator itc;
	itc = contours.begin();
	while( itc != contours.end() ) {
		cv::Moments mom = cv::moments(cv::Mat(*itc));
		cl.push_back( cv::Point( mom.m10 / mom.m00, mom.m01 / mom.m00 ) );
		++itc;
	}

	return cl;
}

void ProcessImage( Mat im_read )
{
	std::string filename;
	//cv::Mat im_read;
	cv::Mat im_rgb;
	cv::Mat im_rgb_orig;
	cv::Mat im_gray;
	cv::Mat im_bg_rgb;
	cv::Mat temp;
	cv::Mat cropped;
	cv::Mat nibl, threshold;
	int top_y, bottom_y;
	std::vector<cv::Point> bestNPContourRotated, leftcntrot, rightcntrot;
	int cropOk = 0;

	top_y = bottom_y = -1;

	//im_read = cv::imread( fname );
	cv::Rect im_roi;
	im_roi.x = im_read.size().width * im_size_percentage_x1;
	im_roi.y = im_read.size().height * im_size_percentage_y1;
	im_roi.width = im_read.size().width * im_size_percentage_x2 - im_roi.x;
	im_roi.height = im_read.size().height * im_size_percentage_y2 - im_roi.y;
	im_rgb = im_read( im_roi );
	im_rgb_orig = im_rgb.clone();

	cv::cvtColor( im_rgb, im_gray, CV_BGRA2GRAY );
	niblack( im_gray, nibl, 5 );
	//cv::imwrite( "threshold.png", threshold );
	//std::cout << "RemoveSmallComponents starts..." << std::endl;
	RemoveSmallComponents( nibl, threshold, 500, 10000 );
	//std::cout << "RemoveSmallComponents ends..." << std::endl;
	//cv::imwrite( "cfiltered.png", threshold );
	result = threshold;

	cv::Mat detectedNumbers, detectedNumberPlate, detectedBluePart;
	if( im_rgb.channels() >= 3 )
	{
		//
		// Find numberplate
		//
		cv::Mat labels = cv::Mat::zeros( im_gray.size(), CV_8UC1 );
		std::vector< std::vector< cv::Point > > contoursNP;
		cv::findContours( threshold, contoursNP, CV_RETR_LIST, CV_CHAIN_APPROX_NONE );
		filterContours( contoursNP, 1000, 9000 );
		if( drawBlobContours )
			cv::drawContours( im_rgb, contoursNP, -1, cv::Scalar(255), 2 );

		cv::Rect r0;
		double rectangularity, bestrect;
		int bestrectidx;
		bestrectidx = -1;
		bestrect = 0.0; // for rectangularity
		double rectratio, bestratio;
		bestratio = 100000.0;
		std::vector<cv::Point> bestNPContour;
		for( size_t i = 0; i < contoursNP.size(); i++ )
		{
		    // Labels starts at 1 because 0 means no contour
		    cv::drawContours(labels, contoursNP, i, cv::Scalar(255), CV_FILLED);
			r0 = cv::boundingRect( cv::Mat( contoursNP[i] ) );
			cv::rectangle( labels, r0, cv::Scalar( 128 ), 2 );
			rectangularity = cv::contourArea( contoursNP[i] ) / r0.area();
			if( bestrect < rectangularity )
			{
				bestrect = rectangularity;
				//bestrectidx = i;
			}
			//std::cout << "Contour label drawn... Rectangularity = " << rectangularity << " w,h: " << r0.width << "," << r0.height << std::endl;

			rectratio = fabs( ( 520.0 / 110.0 ) - (double)r0.width / (double)r0.height );
			if( ( r0.width >= 80 ) && ( r0.width <= 200 ) && ( r0.height >= 15 ) && ( r0.height <= 60 ) && ( rectangularity >= 0.70 ) && ( bestratio > rectratio ) )
			{
				bestratio = rectratio;
				bestrectidx = i;
			}
			//std::cout << "Rectangle ratio = " << rectratio << std::endl;
		}
		//std::cout << "Selected idx: " << bestrectidx << std::endl;

		if( bestrectidx != -1 )
		{
			bestNPContour = contoursNP[ bestrectidx ];
			r0 = cv::boundingRect( cv::Mat( bestNPContour ) );
			//std::cout << r0 << std::endl;

			cv::Mat NPcut, NPcutorig;
			NPcutorig = im_rgb( r0 );
			NPcut = nibl( r0 );
			NPcut = 255 - NPcut;
			RemoveSmallComponents( NPcut, NPcut, 40, 1000 );
			cv::Mat NPcutbgr;
			NPcutbgr = im_rgb( r0 ).clone();
			cv::cvtColor( NPcut, NPcutbgr, CV_GRAY2BGR );
			//im_rgb( r0 ) = NPcutbgr;

			NPcut = 255 - NPcut;
			std::vector< std::vector< cv::Point > > contoursSymbols;
			cv::findContours( NPcut, contoursSymbols, CV_RETR_CCOMP, CV_CHAIN_APPROX_NONE );
			filterContours( contoursSymbols, 20, 1000 );
			//drawCircleContourCentroid( NPcutbgr, contoursNumbers, cv::Scalar( 0, 255, 255 ) );
			//cv::drawContours( NPcutbgr, contoursNumbers, -1, cv::Scalar( 0, 255, 255 ), 2 );
			cv::Rect rnumber;
			//std::cout << "Number box sizes:";
			std::vector<int> vecBoxSizes;
			std::vector<int> vecBoxX;
			for( size_t i = 0; i < contoursSymbols.size(); i++ )
			{
			    // Labels starts at 1 because 0 means no contour
				cv::drawContours(labels, contoursSymbols, i, cv::Scalar(255), CV_FILLED);
				rnumber = cv::boundingRect( cv::Mat( contoursSymbols[i] ) );
				if( rnumber.height > rnumber.width )
					vecBoxSizes.push_back( rnumber.height );
				//std::cout << " " << rnumber.width << "x" << rnumber.height;
				//cv::rectangle( NPcutbgr, rnumber, cv::Scalar( 0, 255, 255 ), 2 );
			}
			//std::cout << std::endl;
			//
			if( vecBoxSizes.size() > 0 )
			{
				int medianSize = CalcMHWScore( vecBoxSizes );
				//std::cout << "Median height: " << medianSize << std::endl;
				//
				int numh_th1 = (int)( medianSize * 0.9 );
				int numh_th2 = 1 + (int)( medianSize * 1.1 );
				for( size_t i = 0; i < contoursSymbols.size(); i++ )
				{
					// Remove those symbols that are not close enough to the median height
					cv::drawContours(labels, contoursSymbols, i, cv::Scalar(255), CV_FILLED);
					rnumber = cv::boundingRect( cv::Mat( contoursSymbols[i] ) );
					//std::cout << " " << rnumber.width << "x" << rnumber.height;
					if( ( rnumber.height >= numh_th1 ) && ( rnumber.height <= numh_th2 ) )
					{
						cv::rectangle( NPcutbgr, rnumber, cv::Scalar( 0, 255, 255 ), 2 );
						vecBoxX.push_back( rnumber.x );
					}
				}
				if( vecBoxX.size() >= 4 )
				{
					// If at least 4 symbols were found, find the leftmost and rightmost ones
					std::sort(vecBoxX.begin(), vecBoxX.end());
					int left_y, right_y, left_x, right_x;
					std::vector<cv::Point> symbolContourLeft;
					std::vector<cv::Point> symbolContourRight;
					for( size_t i = 0; i < contoursSymbols.size(); i++ )
					{
						cv::drawContours(labels, contoursSymbols, i, cv::Scalar(255), CV_FILLED);
						rnumber = cv::boundingRect( cv::Mat( contoursSymbols[ i ] ) );
						if( rnumber.x == vecBoxX[0] )
						{
							left_y = rnumber.y;
							left_x = rnumber.x;
							symbolContourLeft = contoursSymbols[ i ];
						}
						if( rnumber.x == vecBoxX[ vecBoxX.size() - 1 ] )
						{
							right_y = rnumber.y;
							right_x = rnumber.x;
							symbolContourRight = contoursSymbols[ i ];
						}
					}
					float angle = 180.0 * atan2( (float) right_y - left_y , (float) right_x - left_x ) / 3.14159265;
					//std::cout << right_y << " - " << left_y << " = " << right_y - left_y << std::endl;
					//std::cout << "angle: " << angle << std::endl;

					// From ROI to image coordinates
					for( int i = 0; i < symbolContourRight.size(); i++ )
					{
						symbolContourRight[ i ].x += r0.x + im_roi.x;
						symbolContourRight[ i ].y += r0.y + im_roi.y;
					}
					for( int i = 0; i < symbolContourLeft.size(); i++ )
					{
						symbolContourLeft[ i ].x += r0.x + im_roi.x;
						symbolContourLeft[ i ].y += r0.y + im_roi.y;
					}
					for( int i = 0; i < bestNPContour.size(); i++ )
					{
						bestNPContour[ i ].x += im_roi.x;
						bestNPContour[ i ].y += im_roi.y;
					}

					if( compensateRotation )
					{
						//cv::Mat rotated = rotate( im_rgb, angle );
						cv::Mat rotated;
						cv::Mat M = cv::getRotationMatrix2D(cv::Point2f( im_read.size().width / 2.0, im_read.size().height / 2.0 ), angle, 1.0);
						warpAffine( im_read, rotated, M, im_read.size(), cv::INTER_AREA );
						//warpAffine( im_read, rotated, M, im_read.size(), cv::INTER_LANCZOS4 );
						//warpAffine( im_read, rotated, M, im_read.size(), cv::INTER_NEAREST );
						//std::cout << "warpAffine done..." << std::endl;

						cv::transform( bestNPContour, bestNPContourRotated, M );
						cv::transform( symbolContourLeft, leftcntrot, M );
						cv::transform( symbolContourRight, rightcntrot, M );

						// New ROI
						r0 = cv::boundingRect( cv::Mat( bestNPContourRotated ) );
						if( r0.x < 0)
							r0.x = 0;
						if( r0.y < 0)
							r0.y = 0;
						/*
						if( r0.x + r0.width > im_rgb.size().width )
							r0.width = im_rgb.size().width - r0.x - 1;
						if( r0.y + r0.height > im_rgb.size().height )
							r0.height = im_rgb.size().height - r0.y - 1;
						*/
						//std::cout << r0 << std::endl;

						rnumber = cv::boundingRect( cv::Mat( leftcntrot ) );
						left_x = rnumber.x;
						left_y = rnumber.y;
						medianSize = rnumber.height;
						rnumber = cv::boundingRect( cv::Mat( rightcntrot ) );
						right_x = rnumber.x;
						right_y = rnumber.y;
						medianSize += rnumber.height;
						medianSize /= 2;

						std::vector< std::vector< cv::Point > > cnts;
						cnts.push_back( leftcntrot );
						cnts.push_back( rightcntrot );
						cv::drawContours( rotated, cnts, 0, cv::Scalar( 0, 255, 255 ) );
						cv::drawContours( rotated, cnts, 1, cv::Scalar( 0, 255, 255 ) );

						//cv::imwrite( "rotated.png", rotated );
						im_read = rotated;
					}
					else // No rotation compensation
					{
						left_x += r0.x + im_roi.x;
						left_y += r0.y + im_roi.y;
						right_x += r0.x + im_roi.x;
						right_y += r0.y + im_roi.y;
						r0.x += im_roi.x;
						r0.y += im_roi.y;
					}

					//std::cout << r0 << std::endl;
					top_y = ( ( left_y + right_y ) / 2 ) + 1 - medianSize * 0.1;
					bottom_y = ( ( left_y + right_y ) / 2 ) + medianSize * 1.1;
					cv::Rect NPBox;
					NPBox.y = top_y;
					NPBox.height = bottom_y - top_y;
					NPBox.width = NPBox.height * 580.0 / 110.0;
					NPBox.x = r0.x + r0.width - NPBox.width;
					////cv::line( NPcutorig, cv::Point( 0, top_y ), cv::Point( NPcutbgr.size().width - 1, top_y ), cv::Scalar( 0, 255, 0 ) );
					////cv::line( NPcutorig, cv::Point( 0, bottom_y ), cv::Point( NPcutbgr.size().width - 1, bottom_y ), cv::Scalar( 0, 255, 0 ) );
					//cv::line( im_rgb, cv::Point( r0.x + 0, r0.y + top_y ), cv::Point( r0.x + NPcutbgr.size().width - 1, r0.y + top_y ), cv::Scalar( 0, 255, 0 ) );
					//cv::line( im_rgb, cv::Point( r0.x + 0, r0.y + bottom_y ), cv::Point( r0.x + NPcutbgr.size().width - 1, r0.y + bottom_y ), cv::Scalar( 0, 255, 0 ) );
					//cv::line( im_read, cv::Point( r0.x, top_y ), cv::Point( r0.x + r0.width - 1, top_y ), cv::Scalar( 0, 255, 0 ) );
					//cv::line( im_read, cv::Point( r0.x, bottom_y ), cv::Point( r0.x + r0.width - 1, bottom_y ), cv::Scalar( 0, 255, 0 ) );
					cv::rectangle( im_read, NPBox, cv::Scalar( 0, 255, 0 ), 1 );
					r0 = NPBox;
					cropOk = 1;
				}
			}
			//
			//std::cout << "r0: " << r0 << std::endl;
			//im_rgb( r0 ) = NPcutbgr;
			//std::cout << "im_rgb( r0 ) done." << std::endl;

			if( drawBestBlobContour )
			{
				std::vector< std::vector< cv::Point > > cnts;
				cnts.push_back( bestNPContourRotated );
				cv::drawContours( im_read, cnts, 0, cv::Scalar( 255, 0, 0 ), 2 );
			}

			//if( drawBestBlobContour && !compensateRotation )
				//cv::drawContours( im_rgb, contoursNP, bestrectidx, cv::Scalar( 255, 0, 0 ), 2 );
			if( drawNumberPlateBox )
				cv::rectangle( im_read, r0, cv::Scalar( 0, 0, 255 ), 2 );

			//r0.x += im_roi.x;
			//r0.y += im_roi.y;
		cv::Rect r1;

		//r1.x = r0.x >= r0.width / 2 ? r0.x - r0.width / 2 : 0;
		//r1.y = r0.y >= 3 * r0.height ? r0.y - 3 * r0.height : 0;
		//r1.width = 2 * r0.width;
		//r1.height = 5 * r0.height;
		r1.x = r0.x;
		r1.width = 1 * r0.width;
		r1.height = (int)( 4.0 * r1.width * 110.0 / 580.0 );
		r1.y = r0.y >= r1.height ? r0.y - r1.height : 0;

		/*
		//std::cout << "r1: " << r1 << std::endl;
		if( r1.x + r1.width  > im_read.size().width )
			r1.width = im_read.size().width - r1.x - 1;
		if( r1.y + r1.height  > im_read.size().height )
			r1.height = im_read.size().height - r1.y - 1;
		//std::cout << "r1: " << r1 << std::endl;
		*/

		//temp = im_rgb_orig( r1 );
		//cv::resize( temp, cropped, cv::Size( 218, 120 ) );

		//cv::rectangle( im_read, r0, cv::Scalar( 255, 255, 255 ), -1 );
		temp = im_read( r1 );
		//cv::resize( temp, cropped, cv::Size( 158, 120 ) );
		cv::resize( temp, cropped, cv::Size( 316, 240 ) );
		//cropped = processCropped( temp );
		//temp = cropped.clone();
		//cv::resize( temp, cropped, cv::Size( 218, 120 ) );
		//cv::resize( temp, cropped, cv::Size( 240, 204 ) );
		//cv::resize( temp, cropped, cv::Size( 80, 68 ) );
		//cv::resize( temp, cropped, cv::Size( 80, 34 ) );
		if( ( top_y != -1 ) && ( bottom_y != -1 ) )
			cv::rectangle( im_read, r1, cv::Scalar( 0, 255, 0 ), 2 );
		}

		//cv::namedWindow ("Example1", CV_WINDOW_AUTOSIZE);
		//cv::imshow("Example1", labels );
		//cv::waitKey(0);

		//std::cout << "Saving results..." << std::endl;
		//CreateOutputFileNameExt( fname, filename, "_out.png" );
		//std::cout << "filename: " << filename << std::endl;
		//cv::imwrite( filename, im_read );
		//cv::imwrite( filename, threshold );
		//cv::imwrite( filename, detectedNumberPlate * 255 );
		//CreateOutputFileNameExt( fname, filename, "_crop.png" );
		//if( cropped.size().width > 0 )
		if( cropOk )
		{
			//cv::imwrite( filename, cropped );
			result = cropped;
		}
		//cv::imwrite( filename, labels );
	} else {
		//std::cout << "Image with three channels is needed!" << std::endl;
	}
}

extern "C" {
JNIEXPORT void JNICALL Java_hu_forditbt_mrcdrecognizer_MainActivity_NPDetect( JNIEnv*, jobject, jlong addrRgba, jlong addrResult );

JNIEXPORT void JNICALL Java_hu_forditbt_mrcdrecognizer_MainActivity_NPDetect( JNIEnv*, jobject, jlong addrRgba, jlong addrResult )
{
	/*
    Mat& mGr  = *(Mat*)addrGray;
    Mat& mRgb = *(Mat*)addrRgba;
    vector<KeyPoint> v;

    FastFeatureDetector detector(50);
    detector.detect(mGr, v);
    for( unsigned int i = 0; i < v.size(); i++ )
    {
        const KeyPoint& kp = v[i];
        circle(mRgb, Point(kp.pt.x, kp.pt.y), 10, Scalar(255,0,0,255));
    }
	*/
	
	im_size_percentage_x1 = 0.1;
	im_size_percentage_x2 = 0.7;
	im_size_percentage_y1 = 0.2;
	im_size_percentage_y2 = 0.8;

	//im_size_percentage_x1 = 0.0;
	//im_size_percentage_x2 = 1.0;
	//im_size_percentage_y1 = 0.0;
	//im_size_percentage_y2 = 1.0;

	Mat &mRgb = *(Mat *)addrRgba;
	Mat &mRetMat = *(Mat *)addrResult;
	//Mat mGray;
	//cvtColor( mRgb, mGray, CV_BGR2GRAY );
	//niblack( mGray, mGray, 5 );
	//cvtColor( mGray, mRgb, CV_GRAY2BGR );
	ProcessImage( mRgb );
	if( result.type() == CV_8UC1 )
		cvtColor( result, mRetMat, CV_GRAY2BGRA );
	if( result.type() == CV_8UC3 )
		cvtColor( result, mRetMat, CV_BGR2BGRA );
	if( result.type() == CV_8UC4 )
		mRetMat = result;
	//cvtColor( result, mRetMat, CV_BGR2BGRA );
	//mRetMat = result;
}
}
