package fr.ravenfeld.example.livewallpaper;

import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;

public class AnimationThread extends Thread {

    private static final String TAG = "AnimationThread";

    private Object mPauseLock = new Object();

    private boolean mRunning = true;
    private boolean mPaused = true;

    private int fps = 30;
    private int mTimeFrame = 1000 / fps; // drawing time frame in miliseconds 1000 ms / fps

    private SurfaceHolder mSurfaceHolder;
    private Scene mScene;

    AnimationThread(SurfaceHolder surfaceHolder, Scene scene) {
        this.mSurfaceHolder = surfaceHolder;
        this.mScene = scene;

    }

    @Override
    public void run() {

        while (mRunning) {

            waitOnPause();

            if (!mRunning) {
                return;
            }

            long beforeDrawTime = System.currentTimeMillis();

            Canvas canvas = null;
            try {

                canvas = mSurfaceHolder.lockCanvas();

                /** Workaround for: SurfaceTextureClient: dequeueBuffer failed (No such device) */
                if (canvas == null) {
                    continue;
                }

                mScene.update();
                mScene.draw(canvas);

            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Error during surfaceHolder.lockCanvas()", e);
                stopThread();
            } finally {
                if (canvas != null) {
                    try {
                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Error during unlockCanvasAndPost()", e);
                        stopThread();
                    }
                }
            }

            long afterDrawTime = System.currentTimeMillis() - beforeDrawTime;
            try {
                if (mTimeFrame > afterDrawTime) {
                    Thread.sleep(mTimeFrame - afterDrawTime);
                }
            } catch (InterruptedException ex) {
                Log.e(TAG, "Exception during Thread.sleep().", ex);
            }

        }

    }

    public void stopThread() {
        synchronized (mPauseLock) {
            mPaused = false;
            mRunning = false;
            mPauseLock.notifyAll();
        }
    }

    public void pauseThread() {
        synchronized (mPauseLock) {
            mPaused = true;
        }
    }

    public void resumeThread() {
        synchronized (mPauseLock) {
            mPaused = false;
            mPauseLock.notifyAll();
        }
    }

    private void waitOnPause() {
        synchronized (mPauseLock) {
            while (mPaused) {
                try {
                    mPauseLock.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

}
