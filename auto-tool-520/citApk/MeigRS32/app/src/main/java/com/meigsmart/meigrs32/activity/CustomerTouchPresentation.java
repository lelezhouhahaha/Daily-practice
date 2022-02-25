package com.meigsmart.meigrs32.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Presentation;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.log.LogUtil;


public class CustomerTouchPresentation extends Presentation implements OnTouchListener{
	private int OFFSET = 0;
	private int mRectWidth;
	private int mRectHeight;
	private int mCount = 1;
	private boolean mIsHorizontal = false;
	private boolean mIsDrawrectEnd = false;
	private boolean mIsSlash_1 = false;
	private boolean mIsSlash_2 = false;
	private List<PointEntity> mDrawPointList = new ArrayList<PointEntity>();
	private List<RectEntity> mDrawRectList = new ArrayList<RectEntity>();
	private Paint mPaint = null;
	private CustomerView mCustomerView;
	private Path path_1;
	private Path path_2;
	private Context mContext;
	private boolean mIsSuccess;
	//modify by gongming for BUG 14668;
	private int distance_Middle = 54;

	public CustomerTouchPresentation(Context outerContext, Display display) {
		super(outerContext, display);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean onTouch(View view, MotionEvent e) {
		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			initPoint((int) e.getX(), (int) e.getY());
			break;
		case MotionEvent.ACTION_MOVE:
			initPoint((int) e.getX(), (int) e.getY());
			for (int j = 0; j < mDrawPointList.size(); j++) {
				PointEntity pe = mDrawPointList.get(j);
				if (!mIsSlash_1 && !mIsSlash_2) {
					if (IsCollision(pe.px, pe.py)) {
						mDrawPointList.clear();
					}
				}
			}
			isSuccess(mCount - 1);
			if (mIsSlash_1 || mIsSlash_2) {
				isSlashSuccess();
			}
			break;
		case MotionEvent.ACTION_UP:
			if (mIsDrawrectEnd) {
				mDrawRectList.clear();
				mIsDrawrectEnd = false;
				initSlash();
				mIsSlash_1 = true;
			}
			mDrawPointList.clear();
			break;
		}
		mCustomerView.invalidate();
		return true;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        mCustomerView = new CustomerView(getContext());
        mCustomerView.setOnTouchListener(this);
        setContentView(R.layout.presentation);
		LinearLayout layout = (LinearLayout) findViewById(R.id.root_view);
        mRectWidth = dm.widthPixels;
        mRectHeight = dm.heightPixels;
        
        mCustomerView.setMinimumHeight(mRectHeight);  
        mCustomerView.setMinimumWidth(mRectWidth); 
        mCustomerView.invalidate();  
        layout.addView(mCustomerView);
        //modify by gongming for BUG 14595 and 14596;
        OFFSET = 80;
        RectEntity rect = new RectEntity(0, 0, OFFSET, mRectHeight);
		mDrawRectList.add(rect);
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setARGB(255, 255, 0, 0);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeWidth(4);
	}
	
	public void initPoint(int px, int py) {
		PointEntity pe = new PointEntity(px, py);
		mDrawPointList.add(pe);
	}
	
	public void initSlash() {
		path_1 = new Path();
		path_1.moveTo(OFFSET, 0);
		path_1.lineTo(mRectWidth, mRectHeight - OFFSET);
		path_1.moveTo(mRectWidth - OFFSET, mRectHeight);
		path_1.lineTo(0, OFFSET);

		path_2 = new Path();
		path_2.moveTo(mRectWidth - OFFSET, 0);
		path_2.lineTo(0, mRectHeight - OFFSET);
		path_2.moveTo(OFFSET, mRectHeight);
		path_2.lineTo(mRectWidth, OFFSET);
	}
	
	private boolean IsCollision(int dx, int dy) {
		try {
			if (mCount > 4) {
				return true;
			}
			RectEntity rect = mDrawRectList.get(mCount - 1);
			if (rect.rx < dx && rect.rWidth > dx && rect.ry < dy
					&& rect.rHeight > dy) {
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return true;
	}
	//modify by gongming for BUG 14668;
	private boolean mIsSlashCollision(int rx,int ry){
		try {
			if (mCount < 4) {
				return true;
			}
			if (mIsSlash_1) {
				if(Math.abs((-mRectHeight*rx+mRectWidth*ry)/Math.sqrt(mRectHeight*mRectHeight+mRectWidth*mRectWidth))>distance_Middle){
					return true;
				}
				else {
					return false;
				}
			}
			else if (mIsSlash_2) {

				if (Math.abs((-mRectHeight * rx - mRectWidth * ry + mRectWidth * mRectHeight) / Math.sqrt(mRectHeight * mRectHeight + mRectWidth * mRectWidth)) > distance_Middle) {
					return true;
				} else {
					return false;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return true;
	}
	private boolean isSlashSuccess() {
		PointEntity p0 = mDrawPointList.get(0);
		PointEntity pN = mDrawPointList.get(mDrawPointList.size() - 1);
		//modify by gongming for BUG 146668;
		if(mIsSlash_1 || mIsSlash_2 ){
			if(mIsSlashCollision(pN.px, pN.py)){
				LogUtil.d("mIsSlashCollision" );
				mDrawPointList.clear();
			}}
		if (mIsSlash_1 && Math.abs(pN.px - p0.px) >= mRectWidth * 0.93) {
			mIsSlash_1 = false;
			mIsSlash_2 = true;
			mDrawPointList.clear();
		} else if (mIsSlash_2 && Math.abs(pN.px - p0.px) >= mRectWidth * 0.93) {
			mIsSlash_2 = false;
			dismiss();
			setmIsSuccess(true);
			return true;
		}
		return false;
	}
	
	private boolean isSuccess(int count) {
		if (mCount == 1 || mCount == 3) {
			mIsHorizontal = false;
		} else if (mCount == 2 || mCount == 4) {
			mIsHorizontal = true;
		} else if (mCount > 4) {
			return true;
		}
		if (mDrawPointList.size() > 0) {
			PointEntity p0 = mDrawPointList.get(0);
			PointEntity pN = mDrawPointList.get(mDrawPointList.size() - 1);
			RectEntity re = mDrawRectList.get(count);
			if (mIsHorizontal) {
				if (Math.abs(pN.px - p0.px) >= mRectWidth * 0.96) {
					re.rPaint.setARGB(255, 0, 255, 0);
					mCount++;
					if (mCount == 3) {
						mDrawRectList.add(new RectEntity((mRectWidth - OFFSET),
								0, mRectWidth, mRectHeight));
						mDrawPointList.clear();
					}
				} else {
					re.rPaint.setARGB(255, 255, 0, 0);
				}
			} else {
				if (Math.abs(pN.py - p0.py) >= mRectHeight * 0.96) {
					re.rPaint.setARGB(255, 0, 255, 0);
					mCount++;
					if (mCount == 2) {
						mDrawRectList
								.add(new RectEntity(0, (mRectHeight - OFFSET),
										mRectWidth, mRectHeight));
						mDrawPointList.clear();
					} else if (mCount == 4) {
						mDrawRectList.add(new RectEntity(0, 0, mRectWidth,
								OFFSET));
						mDrawPointList.clear();
					}
				} else {
					re.rPaint.setARGB(255, 255, 0, 0);
				}
			}
		}
		return false;
	}
	
	public boolean ismIsSuccess() {
		return mIsSuccess;
	}

	public void setmIsSuccess(boolean mIsSuccess) {
		this.mIsSuccess = mIsSuccess;
	}

	public class CustomerView extends View {

		public CustomerView(Context context) {
			super(context);
		}

		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}

		protected void onDraw(Canvas canvas) {
			canvas.drawColor(0xFF0000);
			for (int i = 0; i < mDrawRectList.size(); i++) {
				RectEntity re = mDrawRectList.get(i);
				canvas.drawRect(re.rx, re.ry, re.rWidth, re.rHeight, re.rPaint);
			}

			if (mIsSlash_1) {
				canvas.drawPath(path_1, mPaint);
			}
			if (mIsSlash_2) {
				canvas.drawPath(path_2, mPaint);
			}

			Point lastPoint = new Point(0, 0);
			for (int j = 0; j < mDrawPointList.size(); j++) {
				PointEntity pe = mDrawPointList.get(j);
				if (j == 0) {
					canvas.drawPoint((int) pe.px, (int) pe.py, pe.pPaint);
				} else {
					canvas.drawLine(lastPoint.x, lastPoint.y, pe.px, pe.py,
							pe.pPaint/* mPaint */);
				}
				lastPoint.x = pe.px;
				lastPoint.y = pe.py;
			}
			if (mCount == 5) {
				mIsDrawrectEnd = true;
				mCount++;
			}
		}
	}

	public class PointEntity {
		private Paint pPaint = null;
		int px, py = 0;

		PointEntity(int x, int y) {
			px = x;
			py = y;
			pPaint = new Paint();
			pPaint.setAntiAlias(true);
			pPaint.setARGB(255, 255, 0, 0);
			pPaint.setStyle(Paint.Style.STROKE);
			pPaint.setStrokeWidth(4);
		}
	}

	public class RectEntity {
		private Paint rPaint = null;
		int rx, ry, rWidth, rHeight = 0;

		RectEntity(int _rx, int _ry, int _rWidth, int _rHeight) {
			rx = _rx;
			ry = _ry;
			rWidth = _rWidth;
			rHeight = _rHeight;
			rPaint = new Paint();
			rPaint.setAntiAlias(true);
			rPaint.setARGB(255, 255, 0, 0);
			rPaint.setStyle(Paint.Style.STROKE);
			rPaint.setStrokeWidth(3);
		}
	}

}
