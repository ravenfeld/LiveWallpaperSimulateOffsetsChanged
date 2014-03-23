package fr.ravenfeld.example.livewallpaper;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;

public class Scene {

    private Paint mBackgroundPaint;
    private Paint mOuterCirclePaint;
    private Paint mCirclePaint;

    private int mWidth;
    // animation specific variables
    private float mOuterCircleRadius;
    private float mCircleRadius;

    private int mCenterX;
    private int mCenterY;

    private int mCircleX;
    private int mCircleY;

    private float mAngle;

    public Scene() {

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(0xff8aa8a0);

        mOuterCirclePaint = new Paint();
        mOuterCirclePaint.setAntiAlias(true);
        mOuterCirclePaint.setColor(0xff5e736d);
        mOuterCirclePaint.setStyle(Style.STROKE);
        mOuterCirclePaint.setStrokeWidth(3.0f);

        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setColor(0xffa2bd3a);
        mCirclePaint.setStyle(Style.FILL);

    }

    public synchronized void updateSize(int width, int height) {
        mWidth =width;
        mCenterX = width / 2;
        mCenterY = height / 2;

        int size = (width < height) ? width : height;

        mOuterCircleRadius = size / 3;
        mCircleRadius = mOuterCircleRadius * 0.2f;

        update();

    }

    public synchronized void update() {

        mAngle += 1.0f;
        if (mAngle > 360f) {
            mAngle -= 360f;
        }

        mCircleX = (int) (mCenterX - mOuterCircleRadius * Math.cos(Math.toRadians(mAngle)));
        mCircleY = (int) (mCenterY - mOuterCircleRadius * Math.sin(Math.toRadians(mAngle)));

    }

    public synchronized void draw(Canvas canvas) {

        // clear the background
        canvas.drawPaint(mBackgroundPaint);

        // draw objects
        canvas.drawCircle(mCenterX, mCenterY, mOuterCircleRadius, mOuterCirclePaint);
        canvas.drawCircle(mCenterX, mCenterY, 5f, mCirclePaint);
        canvas.drawCircle(mCircleX, mCircleY, mCircleRadius, mCirclePaint);

    }

    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep,
                                 int xPixelOffset, int yPixelOffset) {
        mCenterX = (int) ((mWidth) * (xOffset));
    }
}
