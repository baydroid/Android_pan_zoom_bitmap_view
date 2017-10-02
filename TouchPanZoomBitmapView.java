package baydroid.android_pan_zoom_bitmap_view.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;



public class TouchPanZoomBitmapView extends PanZoomBitmapView implements Runnable
    {
    private static final int   FRAME_INTERVAL_MILLIS = 30;
    private static final float MOMENTUM_SCALE        = 0.03f;
    private static final float MOMENTUM_DELTA        = 10.0f;

    private ScaleGestureDetector sgd            = null;
    private GestureDetector      gd             = null;
    private float                momentumX      = 0.0f;
    private float                momentumY      = 0.0f;
    private float                momentumDeltaX = 0.0f;
    private float                momentumDeltaY = 0.0f;

    public TouchPanZoomBitmapView(Context ctx)
        {
        super(ctx);
        init(ctx);
        }

    public TouchPanZoomBitmapView(Context ctx, AttributeSet attrs)
        {
        super(ctx, attrs);
        init(ctx);
        }

    public TouchPanZoomBitmapView(Context ctx, AttributeSet attrs, int defStyleAttr)
        {
        super(ctx, attrs, defStyleAttr);
        init(ctx);
        }

    private void init(Context ctx)
        {
        sgd = new ScaleGestureDetector
            (
            ctx,
            new ScaleGestureDetector.SimpleOnScaleGestureListener()
                {

                public boolean onScale(ScaleGestureDetector detector)
                    {
                    momentumX = momentumY = momentumDeltaX = momentumDeltaY = 0.0f;
                    zoom(detector.getScaleFactor(), (int)detector.getFocusX(), (int)detector.getFocusY());
                    return true;
                    }

                }
            );
        gd = new GestureDetector
            (
            ctx,
            new GestureDetector.SimpleOnGestureListener()
                {

                public boolean onDoubleTap(MotionEvent e1)
                    {
                    momentumX = momentumY = momentumDeltaX = momentumDeltaY = 0.0f;
                    if (isZoomedToFit())
                        zoomToPixel1To1((int)e1.getX(), (int)e1.getY());
                    else
                        zoomToFit();
                    return true;
                    }

                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
                    {
                    momentumX = momentumY = momentumDeltaX = momentumDeltaY = 0.0f;
                    pan((int)distanceX, (int)distanceY);
                    return true;
                    }

                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
                    {
                    momentumX = -MOMENTUM_SCALE*velocityX;
                    if ((int)momentumX == 0) momentumX = 0.0f;
                    momentumY = -MOMENTUM_SCALE*velocityY;
                    if ((int)momentumY == 0) momentumY = 0.0f;
                    if (momentumX != 0.0f || momentumY != 0.0f)
                        {
                        float h = (float)Math.sqrt(velocityX*velocityX + velocityY*velocityY);
                        momentumDeltaX = MOMENTUM_DELTA*velocityX/h;
                        momentumDeltaY = MOMENTUM_DELTA*velocityY/h;
                        getHandler().postDelayed(TouchPanZoomBitmapView.this, FRAME_INTERVAL_MILLIS);
                        }
                    return true;
                    }

                public boolean onDown(MotionEvent e)
                    {
                    momentumX = momentumY = momentumDeltaX = momentumDeltaY = 0.0f;
                    return super.onDown(e);
                    }

                }
            );
        }

    public void run()
        {
        if ((momentumX != 0.0f || momentumY != 0.0f) && pan((int)momentumX, (int)momentumY))
            {
            momentumX = reduceMomentum(momentumX, momentumDeltaX);
            momentumY = reduceMomentum(momentumY, momentumDeltaY);
            if (momentumX != 0.0f || momentumY != 0.0f)
                {
                getHandler().postDelayed(this, FRAME_INTERVAL_MILLIS);
                return;
                }
            }
        momentumX = momentumY = momentumDeltaX = momentumDeltaY = 0.0f;
        }

    private float reduceMomentum(float m, float deltaM)
        {
        if (deltaM != 0.0f && m != 0.0f)
            {
            m += deltaM;
            if ((int)m == 0)
                m = 0.0f;
            else
                {
                if (deltaM > 0.0f)
                    {
                    if (m > 0.0f) m = 0.0f;
                    }
                else
                    {
                    if (m < 0.0f) m = 0.0f;
                    }
                }
            }
        return m;
        }

    public boolean onTouchEvent(MotionEvent ev)
        {
        boolean processed = sgd.onTouchEvent(ev);
        processed = gd.onTouchEvent(ev) || processed;
        return processed || super.onTouchEvent(ev);
        }
    }
