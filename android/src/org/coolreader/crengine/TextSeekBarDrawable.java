package org.coolreader.crengine;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.StateSet;
import android.view.animation.AnimationUtils;

import org.coolreader.R;

public class TextSeekBarDrawable extends Drawable implements Runnable {

    static final int[] STATE_FOCUSED = {android.R.attr.state_focused};
    static final int[] STATE_PRESSED = {android.R.attr.state_pressed};

    private static final String TAG = "TextSeekBarDrawable";
    private static final long DELAY = 50;
    private String mText;
    private Drawable mProgress;
    private Paint mPaint;
    private Paint mOutlinePaint;
    private float mTextWidth;
    private boolean mActive;
    private float mTextXScale;
    private int mDelta;
    private ColorPickerDialog.ScrollAnimation mAnimation;
    private GetTextListener mGetTextListener;

    public TextSeekBarDrawable(Resources res, int id, boolean labelOnRight,
                               BaseActivity activity, boolean darkenColor,
                               GetTextListener getTextListener) {
        int colorGray;
        int colorGrayC;
        int colorIcon;
        TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
                {R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
        colorGray = a.getColor(0, Color.GRAY);
        colorGrayC = a.getColor(1, Color.GRAY);
        colorIcon = a.getColor(2, Color.GRAY);
        a.recycle();

        mGetTextListener = null;
        if (getTextListener != null) {
            mText = getTextListener.onGetText();
            mGetTextListener = getTextListener;
        } else
            mText = res.getString(id);
        //mProgress = res.getDrawable(R.drawable.seekbar_colorbar);
        int colorGrayCT= Color.argb(128,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
        mProgress = new ColorDrawable(colorGrayCT);
        //mProgress = res.getDrawable(R.drawable.seekbar_progressbar);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //mPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mPaint.setTextSize(42);
        mPaint.setColor(colorIcon);
        mOutlinePaint = new Paint(mPaint);
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setStrokeWidth(3);
        if (darkenColor) mOutlinePaint.setColor(colorGrayC);
        else mOutlinePaint.setColor(colorIcon);
        if (darkenColor) mOutlinePaint.setMaskFilter(new BlurMaskFilter(1, BlurMaskFilter.Blur.NORMAL));
        mTextWidth = mOutlinePaint.measureText(mText);
        mTextXScale = labelOnRight? 1 : 0;
        mAnimation = new ColorPickerDialog.ScrollAnimation();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        mProgress.setBounds(bounds);
    }

    @Override
    protected boolean onStateChange(int[] state) {
        mActive = StateSet.stateSetMatches(STATE_FOCUSED, state) | StateSet.stateSetMatches(STATE_PRESSED, state);
        invalidateSelf();
        return false;
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    protected boolean onLevelChange(int level) {
        if (mGetTextListener != null) mText = mGetTextListener.onGetText();
        if (level < 4000 && mDelta <= 0) {
//				Log.d(TAG, "onLevelChange scheduleSelf ++");
            mDelta = 1;
            mAnimation.startScrolling(mTextXScale, 1);
            scheduleSelf(this, SystemClock.uptimeMillis() + DELAY);
        } else
        if (level > 6000 && mDelta >= 0) {
            mDelta = -1;
            mAnimation.startScrolling(mTextXScale, 0);
            scheduleSelf(this, SystemClock.uptimeMillis() + DELAY);
        }
        return mProgress.setLevel(level);
    }

    @Override
    public void draw(Canvas canvas) {
        mProgress.draw(canvas);

        if (mAnimation.hasStarted() && !mAnimation.hasEnded()) {
            // pending animation
            mAnimation.getTransformation(AnimationUtils.currentAnimationTimeMillis(), null);
            mTextXScale = mAnimation.getCurrent();
        }

        Rect bounds = getBounds();
        if (mGetTextListener != null) {
            mText = mGetTextListener.onGetText();
            mTextWidth = mOutlinePaint.measureText(mText);
        }
        float x = 6 + mTextXScale * (bounds.width() - mTextWidth - 6 - 6);
        float y = (bounds.height() + mPaint.getTextSize()) / 2;
        //mOutlinePaint.setAlpha(mActive? 255 : 180);
        mPaint.setAlpha(mActive? 255 : 180);
        canvas.drawText(mText, x, y, mOutlinePaint);
        canvas.drawText(mText, x, y, mPaint);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    public void run() {
        mAnimation.getTransformation(AnimationUtils.currentAnimationTimeMillis(), null);
        // close interpolation of mTextX
        mTextXScale = mAnimation.getCurrent();
        if (!mAnimation.hasEnded()) {
            scheduleSelf(this, SystemClock.uptimeMillis() + DELAY);
        }
        invalidateSelf();
    }
}