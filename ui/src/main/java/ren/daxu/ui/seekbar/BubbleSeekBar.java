package ren.daxu.ui.seekbar;

import android.animation.Animator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import ren.daxu.ui.R;

/**
 * 自定义seek bar
 */
public class BubbleSeekBar extends View {

    private OnProgressChangedListener mOnProgressChangedListener;

    private float mMin;//最小
    private float mMax;//最大
    private float mProgress;//当前量

    private Drawable mThumb;//轨道
    private float mThumbHeight;//拇指高度
    private float mThumbWidth;//拇指宽度
    private int mThumbTextColor;//拇指文字颜色
    private int mThumbTextSize;//拇指文字大小
    private String mThumbText;//拇指上的文字

    private Drawable mTrack;//轨道
    private Drawable mSecondTrack;//二层轨道
    private float mTrackHeight;//轨道高度
    private float mTrackMarginLeft;//轨道左边空余
    private float mTrackMarginRight;//轨道右边空余

    private float mThumbOffset;//拇指偏移量
    private float mTrackCenterY;//轨道中心Y坐标
    private boolean isThumbOnDragging = false;

    private int mBubbleOffset;

    private float mTrackLength;
    private Paint mPaint;
    private WindowManager mWindowManager;
    private FrameLayout mBubbleFL;
    private WindowManager.LayoutParams mLayoutParams;

    public BubbleSeekBar(Context context) {
        this(context, null);
    }

    public BubbleSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DaXuBubbleSeekBar, defStyleAttr, 0);
        mMin = a.getFloat(R.styleable.DaXuBubbleSeekBar_min, 0.0f);
        mMax = a.getFloat(R.styleable.DaXuBubbleSeekBar_max, 100.0f);
        mThumbHeight = a.getDimensionPixelSize(R.styleable.DaXuBubbleSeekBar_indicatorHeight, 60);
        mThumbWidth = a.getDimensionPixelSize(R.styleable.DaXuBubbleSeekBar_indicatorWidth, 100);
        mTrackMarginLeft = a.getDimensionPixelSize(R.styleable.DaXuBubbleSeekBar_trackMarginLeft, 0);
        mTrackMarginRight = a.getDimensionPixelSize(R.styleable.DaXuBubbleSeekBar_trackMarginRight, 0);
        mThumbTextColor = a.getColor(R.styleable.DaXuBubbleSeekBar_indicatorTextColor, Color.BLACK);
        mThumbTextSize = a.getDimensionPixelSize(R.styleable.DaXuBubbleSeekBar_indicatorTextSize, 14);
        mTrackHeight = a.getDimension(R.styleable.DaXuBubbleSeekBar_barHeight, 10);
        mTrack = a.getDrawable(R.styleable.DaXuBubbleSeekBar_track);
        mSecondTrack = a.getDrawable(R.styleable.DaXuBubbleSeekBar_secondTrack);
        mBubbleOffset = a.getDimensionPixelOffset(R.styleable.DaXuBubbleSeekBar_bubbleOffset, 10);
        mThumb = a.getDrawable(R.styleable.DaXuBubbleSeekBar_indicator);
        mBubbleFL = new FrameLayout(getContext());
        mBubbleFL.setVisibility(GONE);
        mBubbleFL.setBackgroundDrawable(a.getDrawable(R.styleable.DaXuBubbleSeekBar_bubbleBackgroud));
        mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        mLayoutParams = new WindowManager.LayoutParams();
        mLayoutParams.x = 0;
        mLayoutParams.y = 0;
        mLayoutParams.gravity = Gravity.START | Gravity.TOP;
        mLayoutParams.width = a.getDimensionPixelSize(R.styleable.DaXuBubbleSeekBar_bubbleWidth, 150);
        mLayoutParams.height = a.getDimensionPixelSize(R.styleable.DaXuBubbleSeekBar_bubbleHeight, 10);
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setColor(mThumbTextColor);
        mPaint.setTextSize(mThumbTextSize);

        mLayoutParams.format = PixelFormat.TRANSLUCENT;
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;

        if (BubbleSeekBarUtils.isMIUI() || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION;
        } else {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        }

        mWindowManager.addView(mBubbleFL, mLayoutParams);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mTrackCenterY = getMeasuredHeight() >> 1;
        mTrackLength = getMeasuredWidth() - mThumbWidth;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                performClick();
                isThumbOnDragging = true;
                if (mOnProgressChangedListener != null) {
                    mOnProgressChangedListener.onStartTrackingTouch(this);
                }
                showBubble();
            }
            break;
            case MotionEvent.ACTION_MOVE: {
                mThumbOffset = event.getX() - mThumbWidth / 2;
                if (mThumbOffset < 0)
                    mThumbOffset = 0;
                else if (mThumbOffset > mTrackLength)
                    mThumbOffset = mTrackLength;
                mProgress = mMin + (mMax - mMin) * (mThumbOffset) / mTrackLength;
                calculateBubble();
                postInvalidate();
                if (mOnProgressChangedListener != null) {
                    mOnProgressChangedListener.onProgressChanged(this, getProgress(), true);
                }
            }
            break;
            case MotionEvent.ACTION_UP: {
                if (mOnProgressChangedListener != null) {
                    mOnProgressChangedListener.onStopTrackingTouch(this);
                }
            }
            case MotionEvent.ACTION_CANCEL: {
                isThumbOnDragging = false;
                hideBubble();
            }
            break;
        }
        return isThumbOnDragging | super.onTouchEvent(event);
    }

    /**
     * 计算Bubble
     */
    private void calculateBubble() {
        mLayoutParams.x = mPoint[0] + Math.round(mThumbOffset) - Math.round((mBubbleFL.getWidth() - mThumbWidth) / 2);
        mLayoutParams.y = mPoint[1] - mBubbleFL.getHeight() - Math.round(mThumbHeight / 2.0f) - mBubbleOffset;
        mWindowManager.updateViewLayout(mBubbleFL, mLayoutParams);
    }

    /**
     * 显示弹出框
     */
    private void showBubble() {
        calculateBubble();
        if (mBubbleFL == null)
            return;
        mBubbleFL.setAlpha(0);
        mBubbleFL.animate().alpha(1f).setDuration(200).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mBubbleFL.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

        }).start();
    }

    /**
     * 隐藏弹出框
     */
    private void hideBubble() {
        if (mBubbleFL == null)
            return;
        mBubbleFL.setAlpha(1);
        mBubbleFL.animate().alpha(0f).setDuration(200).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mBubbleFL.setVisibility(GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        }).start();
    }

    /**
     * 设置最大值
     *
     * @param max
     */
    public void setMax(float max) {
        mMax = max;
    }

    /**
     * 获取最大值
     *
     * @return
     */
    public float getMax() {
        return mMax;
    }

    /**
     * 设置最小值
     *
     * @param min
     */
    public void setMin(float min) {
        mMin = min;
    }

    /**
     * 获取最小值
     *
     * @return
     */
    public float getMin() {
        return mMin;
    }

    /**
     * 获取当前位置
     *
     * @return
     */
    private float getProgress() {
        return mProgress;
    }

    /**
     *
     * @param thumbText
     */
    public void updateThumbText(String thumbText) {
        mThumbText = thumbText;
        postInvalidate();
    }

    /**
     * 设置当前位置
     *
     * @param progress
     */
    public void setProgress(float progress) {
        mProgress = progress;
        if (mOnProgressChangedListener != null) {
            mOnProgressChangedListener.onProgressChanged(this, mProgress, false);
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mThumbOffset = mTrackLength * (mProgress - mMin) / (mMax - mMin);
        drawTrack(canvas);
        drawThumb(canvas);
    }

    /**
     * 绘制拇指
     *
     * @param canvas
     */
    private void drawThumb(Canvas canvas) {
        int save = canvas.save();
        if (mThumb != null) {
            canvas.translate(mThumbOffset, mTrackCenterY - mThumbHeight / 2.0f);
            mThumb.setBounds(0, 0, Math.round(mThumbWidth), Math.round(mThumbHeight));
            mThumb.draw(canvas);

            if (mThumbText != null) {
                /**
                 * 绘制文字
                 */
                Rect rect = new Rect();
                mPaint.getTextBounds(mThumbText, 0, mThumbText.length(), rect);
                float x = (mThumbWidth - rect.width()) / 2.0f + rect.width() / 2.0f;
                float y = (mThumbHeight - rect.height()) / 2.0f + rect.height();
                canvas.drawText(mThumbText, x, y, mPaint);
            }
        }
        canvas.restoreToCount(save);
    }

    /**
     * 绘制轨道
     *
     * @param canvas
     */
    private void drawTrack(Canvas canvas) {
        int save = canvas.save();
        canvas.translate(0, mTrackCenterY - mTrackHeight / 2.0f);
        if (mTrack != null) {
            mTrack.setBounds(Math.round(mTrackMarginLeft), 0, getMeasuredWidth() - Math.round(mTrackMarginLeft) - Math.round(mTrackMarginRight), Math.round(mTrackHeight));
            mTrack.draw(canvas);
        }
        if (mSecondTrack != null) {
            mSecondTrack.setBounds(Math.round(mTrackMarginLeft), 0, Math.round(mThumbOffset + mThumbWidth / 2.0f) - Math.round(mTrackMarginLeft) - Math.round(mTrackMarginRight), Math.round(mTrackHeight));
            mSecondTrack.draw(canvas);
        }
        canvas.restoreToCount(save);
    }


    private int[] mPoint = new int[2];

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        getLocationOnScreen(mPoint);
    }

    /**
     * 设置监听
     *
     * @param progressChangedListener
     */
    public void setOnProgressChangedListener(OnProgressChangedListener progressChangedListener) {
        mOnProgressChangedListener = progressChangedListener;
    }

    /**
     * 监听进度
     */
    public interface OnProgressChangedListener {

        void onProgressChanged(BubbleSeekBar bubbleSeekBar, float progress, boolean fromUser);

        void onStartTrackingTouch(BubbleSeekBar bubbleSeekBar);

        void onStopTrackingTouch(BubbleSeekBar bubbleSeekBar);
    }


    /**
     *
     *
     */
    public static class BubbleSeekBarUtils {
        private static final File BUILD_PROP_FILE = new File(Environment.getRootDirectory(), "build.prop");
        private static Properties sBuildProperties;
        private static final Object sBuildPropertiesLock = new Object();

        private static Properties getBuildProperties() {
            synchronized (sBuildPropertiesLock) {
                if (sBuildProperties == null) {
                    sBuildProperties = new Properties();
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(BUILD_PROP_FILE);
                        sBuildProperties.load(fis);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            return sBuildProperties;
        }

        /**
         * 判断是否是MIUI
         *
         * @return
         */
        static boolean isMIUI() {
            return getBuildProperties().containsKey("ro.miui.ui.version.name");
        }

    }


}