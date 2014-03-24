package fr.ravenfeld.example.livewallpaper.gesture;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

public class OffsetsDetector {
    private static final String TAG = "OffsetsDetector";
    private final OnOffsetsListener mListener;
    Animate mSwipeAnimation = null;
    private GestureThread mGestureThread;
    private int mMaximumFlingVelocity;
    private VelocityTracker mVelocityTracker;
    private MotionEvent mCurrentDownEvent;
    private MotionEvent mCurrentDeltaEvent;
    private float mTouchOffsetX = -1.0F;
    private float mXOffsetDefault = 0.5f;
    private float mYOffsetDefault = 0.5f;
    private float mYOffsetStepDefault = 1f;
    private int mNbScreens = 4; //0 1 2 3 4 also 5 screen actif
    private float mXOffsetStepDefault = 1f / mNbScreens;
    private int mScreenWidth;
    private boolean mManualThread;

    public OffsetsDetector(Context context, OnOffsetsListener listener) {
        this(context, listener, false);
    }

    public OffsetsDetector(Context context, OnOffsetsListener listener, boolean manualThread) {
        mListener = listener;
        mManualThread = manualThread;
        init(context);
    }

    private void init(Context context) {
        if (mListener == null) {
            throw new NullPointerException("OnGestureListener must not be null");
        }
        if (context == null) {
            mMaximumFlingVelocity = ViewConfiguration.getMaximumFlingVelocity();
        } else {
            final ViewConfiguration configuration = ViewConfiguration.get(context);
            mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        }
        if (!mManualThread) {
            mGestureThread = new GestureThread();
            mGestureThread.start();
        }
    }

    public Animate getSwipeAnimation() {
        return mSwipeAnimation;
    }

    public void swipeAnimationUpdate() {
        if (mSwipeAnimation != null)
            mSwipeAnimation.update();
    }

    public float getXOffsetDefault() {
        return mXOffsetDefault;
    }

    public void setXOffsetDefault(float xOffsetDefault) {
        this.mXOffsetDefault = xOffsetDefault;
    }

    public float getYOffsetDefault() {
        return mYOffsetDefault;
    }

    public void setYOffsetDefault(float yOffsetDefault) {
        this.mYOffsetDefault = yOffsetDefault;
    }

    public float getYOffsetStepDefault() {
        return mYOffsetStepDefault;
    }

    public void setYOffsetStepDefault(float yOffsetStepDefault) {
        this.mYOffsetStepDefault = yOffsetStepDefault;
    }

    public float getXOffsetStepDefault() {
        return mXOffsetStepDefault;
    }

    public void setXOffsetStepDefault(float xOffsetStepDefault) {
        this.mXOffsetStepDefault = xOffsetStepDefault;
    }

    public int getScreenWidth() {
        return mScreenWidth;
    }

    public void setScreenWidth(int width) {
        this.mScreenWidth = width;
        if (mTouchOffsetX == -1)
            mTouchOffsetX = width * mNbScreens / 2f;
        mListener.onOffsetsChanged(mXOffsetDefault, mYOffsetDefault, mXOffsetStepDefault, mYOffsetStepDefault);

    }

    public int getScreens() {
        return mNbScreens;
    }

    public void setScreens(int nbScreen) {
        this.mNbScreens = nbScreen;
        mXOffsetStepDefault = 1f / this.mNbScreens;
    }

    public void onVisibilityChanged(boolean visible) {
        if (!mManualThread) {
            if (visible) {
                mGestureThread.resumeThread();
            } else {
                mGestureThread.pauseThread();
            }
        }
    }

    public void onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        final int count = event.getPointerCount();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_UP:

                mVelocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                final int upIndex = event.getActionIndex();
                final int id1 = event.getPointerId(upIndex);
                final float x1 = mVelocityTracker.getXVelocity(id1);
                final float y1 = mVelocityTracker.getYVelocity(id1);
                for (int i = 0; i < count; i++) {
                    if (i == upIndex) continue;

                    final int id2 = event.getPointerId(i);
                    final float x = x1 * mVelocityTracker.getXVelocity(id2);
                    final float y = y1 * mVelocityTracker.getYVelocity(id2);

                    final float dot = x + y;
                    if (dot < 0) {
                        mVelocityTracker.clear();
                        break;
                    }
                }
                break;
            case MotionEvent.ACTION_DOWN:
                if (mCurrentDownEvent != null) {
                    mCurrentDownEvent.recycle();
                }
                if (mCurrentDeltaEvent != null) {
                    mCurrentDeltaEvent.recycle();
                }
                mCurrentDownEvent = MotionEvent.obtain(event);
                mCurrentDeltaEvent = MotionEvent.obtain(event);
                break;
            case MotionEvent.ACTION_MOVE:
                mTouchOffsetX += mCurrentDeltaEvent.getX() - event.getX();
                if (mCurrentDeltaEvent != null) {
                    mCurrentDeltaEvent.recycle();
                }
                mCurrentDeltaEvent = MotionEvent.obtain(event);

                mListener.onOffsetsChanged(getViewOffset(), mYOffsetDefault, mXOffsetStepDefault, mYOffsetStepDefault);
                break;

            case (MotionEvent.ACTION_UP):
                final VelocityTracker velocityTracker = mVelocityTracker;
                final int pointerId = event.getPointerId(0);
                velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                final float velocityY = velocityTracker.getYVelocity(pointerId);
                final float velocityX = velocityTracker.getXVelocity(pointerId);


                onFling(mCurrentDownEvent, event, velocityX, velocityY);

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }

                break;
        }
    }

    private float getViewOffset() {
        if (mTouchOffsetX < 0.0F)
            mTouchOffsetX = 0.0F;
        if (mTouchOffsetX > mScreenWidth * mNbScreens)
            mTouchOffsetX = mScreenWidth * mNbScreens;
        return mTouchOffsetX / (float) (mScreenWidth * mNbScreens);
    }

    public void onFling(MotionEvent paramMotionEvent1, MotionEvent paramMotionEvent2, float paramFloat1, float paramFloat2) {
        if (Math.abs(paramMotionEvent1.getY() - paramMotionEvent2.getY()) > 250.0F)
            return;
        if ((paramMotionEvent1.getX() - paramMotionEvent2.getX() > 25.0F) && (Math.abs(paramFloat1) > 500.0F)) {

            animationRightScreen();

        } else if ((paramMotionEvent1.getX() - paramMotionEvent2.getX() > mScreenWidth * 0.4f)) {
            animationRightScreen();

        } else if ((paramMotionEvent1.getX() - paramMotionEvent2.getX() > 0.0F)) {
            animationLeftScreen();
        } else if ((paramMotionEvent2.getX() - paramMotionEvent1.getX() > 25.0F) && (Math.abs(paramFloat1) > 500.0F) && (mTouchOffsetX > 0.0F)) {

            animationLeftScreen();
        } else if ((paramMotionEvent2.getX() - paramMotionEvent1.getX() > mScreenWidth * 0.4f)) {
            animationLeftScreen();
        } else if ((paramMotionEvent2.getX() - paramMotionEvent1.getX() > 0.0F)) {
            animationRightScreen();

        }

    }

    private float nextScreen(float mTotalTouchOffsetX) {
        int nb = mNbScreens;
        while (nb >= 0) {
            if (mTotalTouchOffsetX <= mScreenWidth * nb && mTotalTouchOffsetX > mScreenWidth * (nb - 1)) {
                break;
            }
            nb--;
        }
        return mScreenWidth * (nb);
    }

    private float beforeScreen(float mTotalTouchOffsetX) {
        int nb = mNbScreens;
        while (nb >= 0) {
            if (mTotalTouchOffsetX <= mScreenWidth * nb && mTotalTouchOffsetX > mScreenWidth * (nb - 1)) {
                break;
            }
            nb--;
        }
        return mScreenWidth * (nb - 1);
    }

    private void animationRightScreen() {
        if (mTouchOffsetX < mScreenWidth * mNbScreens) {

            if (mSwipeAnimation != null) {
                mSwipeAnimation.destroyAnimation();
            }
            mSwipeAnimation = new Animate(mTouchOffsetX, nextScreen(mTouchOffsetX));
            mSwipeAnimation.setAnimationListener(new AnimateListener() {
                public void AnimationEnded(Animate paramAnonymousAnimate) {

                }

                public void AnimationStarted(Animate paramAnonymousAnimate) {
                }

                public void AnimationUpdated(Animate paramAnonymousAnimate) {
                    mTouchOffsetX = paramAnonymousAnimate.getCurrentValue();
                    mListener.onOffsetsChanged(getViewOffset(), mYOffsetDefault, mXOffsetStepDefault, mYOffsetStepDefault);
                }
            });
            mSwipeAnimation.startAnimation();
        }
    }

    private void animationLeftScreen() {
        if (mTouchOffsetX > 0) {
            if (mSwipeAnimation != null) {
                mSwipeAnimation.destroyAnimation();
            }
            mSwipeAnimation = new Animate(mTouchOffsetX, beforeScreen(mTouchOffsetX));
            mSwipeAnimation.setAnimationListener(new AnimateListener() {
                public void AnimationEnded(Animate paramAnonymousAnimate) {
                }

                public void AnimationStarted(Animate paramAnonymousAnimate) {
                }

                public void AnimationUpdated(Animate paramAnonymousAnimate) {
                    mTouchOffsetX = paramAnonymousAnimate.getCurrentValue();
                    mListener.onOffsetsChanged(getViewOffset(), mYOffsetDefault, mXOffsetStepDefault, mYOffsetStepDefault);
                }
            });
            mSwipeAnimation.startAnimation();
        }
    }

    public void onDestroy() {
        if (!mManualThread) {
            mGestureThread.stopThread();
            joinThread(mGestureThread);
            mGestureThread = null;
        }
    }

    private void joinThread(Thread thread) {
        boolean retry = true;
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }

    }

    public interface OnOffsetsListener {
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep);
    }

    class GestureThread extends Thread {
        private Object pauseLock = new Object();

        private boolean running = true;
        private boolean paused = true;

        private int fps = 30;
        private int timeFrame = 1000 / fps; // drawing time frame in miliseconds 1000 ms / fps

        @Override
        public void run() {

            while (running) {

                waitOnPause();

                if (!running) {
                    return;
                }
                long beforeDrawTime = System.currentTimeMillis();
                swipeAnimationUpdate();
                long afterDrawTime = System.currentTimeMillis() - beforeDrawTime;
                try {
                    if (timeFrame > afterDrawTime) {
                        Thread.sleep(timeFrame - afterDrawTime);
                    }
                } catch (InterruptedException ex) {
                    Log.e(TAG, "Exception during Thread.sleep().", ex);
                }
            }
        }

        public void stopThread() {
            synchronized (pauseLock) {
                paused = false;
                running = false;
                pauseLock.notifyAll();
            }
        }

        public void pauseThread() {
            synchronized (pauseLock) {
                paused = true;
            }
        }

        public void resumeThread() {
            synchronized (pauseLock) {
                paused = false;
                pauseLock.notifyAll();
            }
        }

        private void waitOnPause() {
            synchronized (pauseLock) {
                while (paused) {
                    try {
                        pauseLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }
}
