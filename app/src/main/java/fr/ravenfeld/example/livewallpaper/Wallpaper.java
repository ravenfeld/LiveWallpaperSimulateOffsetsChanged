package fr.ravenfeld.example.livewallpaper;

import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import fr.ravenfeld.example.livewallpaper.gesture.OffsetsDetector;

public class Wallpaper extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new WallpaperEngine();
    }

    class WallpaperEngine extends Engine {

        private static final String TAG = "WallpaperEngine";

        private AnimationThread mAnimationThread;
        private Scene mScene;
        private OffsetsDetector mOffsetsDetector;
        private boolean mScrollingWorking = false;



        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            // create the scene
            mScene = new Scene();
            mOffsetsDetector = new OffsetsDetector(getBaseContext(), new
                    OffsetsDetector.OnOffsetsListener() {
                        @Override
                        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep) {
                            if (isPreview())
                                xOffset = 0.5f;
                            mScene.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, 0, 0);
                        }
                    }, false);
            // start animation thread; thread starts paused
            // will run onVisibilityChanged
            mAnimationThread = new AnimationThread(surfaceHolder, mScene);
            mAnimationThread.start();

        }

        @Override
        public void onDestroy() {
            mOffsetsDetector.onDestroy();
            mAnimationThread.stopThread();
            joinThread(mAnimationThread);
            mAnimationThread = null;

            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            mOffsetsDetector.onVisibilityChanged(visible);
            if (visible) {
                mAnimationThread.resumeThread();
            } else {
                mAnimationThread.pauseThread();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mOffsetsDetector.setScreenWidth(width);
            mScene.updateSize(width, height);

        }

        public void onTouchEvent(MotionEvent paramMotionEvent) {
            super.onTouchEvent(paramMotionEvent);
            if (!mScrollingWorking) {
                mOffsetsDetector.onTouchEvent(paramMotionEvent);
            }        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep,
                                     int xPixelOffset, int yPixelOffset) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset);
            if (isPreview()) {
                xOffset = 0.5f;
            }
            if(xOffsetStep>0.0f){
                mOffsetsDetector.setScreens((int)(1f/xOffsetStep));
            }
            if(xOffset!=0.5f) {
                mScrollingWorking = true;
            }
            if(mOffsetsDetector.getOffsetXCurrent()!=0.5f && xOffset!=mOffsetsDetector.getOffsetXCurrent()){
                xOffset=mOffsetsDetector.getOffsetXCurrent();
            }
            mScene.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset);
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

    }

}
