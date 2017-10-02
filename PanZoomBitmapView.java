package baydroid.android_pan_zoom_bitmap_view.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Rect;



public class PanZoomBitmapView extends View
    {
    /**
     * Options for setting the maximum extent to which the view can zoom out.
     */
    public enum MaxZoomOut
        {
        /**
         * Set the maximum zoom out to the point at which the image occupies approximately 1/8th of the view linearly.
         */
        BY8,
        /**
         * Set the maximum zoom out to the point at which the entire image just fits in the view
         */
        FITS,
        /**
         * Set the maximum zoom out to whichever of MaxZoomOut.FITS or pixel 1-to-1 makes the image appear smallest in te view.
         */
        FITS_OR_1TO1
        }

    private static final int MIN_BM_RECT_DIMENSION = 32;

    private Bitmap bm = null;                // The bitmap.
    private int bmW = -1;                    // Bitmap width in pixels.
    private int bmH = -1;                    // Bitmap height in pixels.
    private MaxZoomOut mzo = MaxZoomOut.BY8; // Maximum allowed zoom out.
    private int minViewInBmRDimension = -1;  // Minimum height or width of the view mapped into the bitmap's co-ordinate space (in bitmap pixels).
    private int layoutViewW = 0;             // The view width (in view pixels) at the time the layout (I.E. viewInBmR) was computed.
    private int layoutViewH = 0;             // The view height (in view pixels) at the time the layout (I.E. viewInBmR) was computed.
    private Rect viewInBmR = null;           // The frame of the view mapped into the bitmap's co-ordinate space (in bitmap pixel co-ordinates).
    private Rect prevViewInBmR = new Rect(); // viewInBmR as before a call to zoom() or pan() etc. Used for checking to see if anything changed during the call.
    private Rect bmShowingR = new Rect();    // The part of the bitmap showing (in bitmap pixel co-ordinates).
    private Rect viewShowingR = new Rect();  // The part of the view showing the bitmap (in view pixel co-ordinates).

    public PanZoomBitmapView(Context ctx)
        {
        super(ctx);
        }

    public PanZoomBitmapView(Context ctx, AttributeSet attrs)
        {
        super(ctx, attrs);
        }

    public PanZoomBitmapView(Context ctx, AttributeSet attrs, int defStyleAttr)
        {
        super(ctx, attrs, defStyleAttr);
        }

    /**
     * Load a new bitmap.
     * @param bm The bitmap to load, or null to show no bitmap.
     * @return The PanZoomBitmapView object on which this method is being called.
     */
    public PanZoomBitmapView setImageBitmap(Bitmap bm)
        {
        viewInBmR = null;
        this.bm = bm;
        if (bm != null)
            {
            bmW = bm.getWidth();
            bmH = bm.getHeight();
            minViewInBmRDimension = MIN_BM_RECT_DIMENSION > bmW ? bmW : MIN_BM_RECT_DIMENSION;
            if (minViewInBmRDimension > bmH) minViewInBmRDimension = bmH;
            }
        else
            {
            bmW = -1;
            bmH = -1;
            }
        invalidate();
        return this;
        }

    /**
     * Clear any loaded bitmap.
     * @return The PanZoomBitmapView object on which this method is being called.
     */
    public PanZoomBitmapView clearImageBitmap()
        {
        return setImageBitmap(null);
        }

    /**
     * Is there a loaded bitmap?
     * @return true if there's a bitmap already loaded and false if there's no bitmap loaded.
     */
    public boolean hasLoadedBitmap()
        {
        return bm != null;
        }

    /**
     * Get the currently loaded bitmap.
     * @return The currently loaded bitmap.
     */
    public Bitmap getBitmap()
        {
        return bm;
        }

    /**
     * Get the width in pixels of the currently loaded bitmap.
     * @return The width in pixels of the currently loaded bitmap.
     */
    public int getBitmapWidth()
        {
        return bmW;
        }

    /**
     * Get the height in pixels of the currently loaded bitmap.
     * @return The height in pixels of the currently loaded bitmap.
     */
    public int getBitmapHeight()
        {
        return bmH;
        }

    /**
     * Set the maximum zoom out, or how small can the image get in the view.
     * @param mzo One of the values of the PanZoomBitmapView.MaxZoomOut enum.
     * @return The PanZoomBitmapView object on which this method is being called.
     */
    public PanZoomBitmapView setMaxZoomOut(MaxZoomOut mzo)
        {
        this.mzo = mzo;
        return this;
        }

    /**
     * Slide the bitmap image (if it's zoomed in enough that only part of the bitmap's showing).
     * @param deltaX Amount to slide in the horizontal direction (in view pixel co-ordinates).
     * @param deltaY Amount to slide in the vertical direction (in view pixel co-ordinates).
     * @return True if there was any change to the contents of the view and false if the view remains unchanged (e.g. panning past the edge of the bitmap).
     */
    public boolean pan(int deltaX, int deltaY)
        {
        if (bm == null || viewInBmR == null) return false;
        prevViewInBmR.set(viewInBmR);
        float scaleFactor = computeScaleFactor();
        viewInBmR.offset(Math.round(scaleFactor*((float)deltaX)), Math.round(scaleFactor*((float)deltaY)));
        tidyPosition();
        if (!viewInBmR.equals(prevViewInBmR))
            {
            invalidate();
            return true;
            }
        else
            return false;
        }

    /**
     * Zoom so that the bitmap image is the largest it can be and still fit the entire bitmap in the view.
     * @return True if there was any change to the contents of the view and false if the view remains unchanged (e.g. if it was already zoomed to fit).
     */
    public boolean zoomToFit()
        {
        if (bm == null) return false;
        boolean firstTime = viewInBmR == null;
        if (firstTime)
            viewInBmR = new Rect();
        else
            prevViewInBmR.set(viewInBmR);
        zoomToFitNoInvalidate();
        if (firstTime || !viewInBmR.equals(prevViewInBmR))
            {
            invalidate();
            return true;
            }
        else
            return false;
        }

    private void zoomToFitNoInvalidate()
        {
        layoutViewW = getWidth();
        layoutViewH = getHeight();
        float heightToWidth = ((float)layoutViewH)/((float)layoutViewW);
        if (layoutViewH*bmW > bmH*layoutViewW)
            {
            viewInBmR.left = 0;
            viewInBmR.right = bmW;
            int h = Math.round(heightToWidth*bmW);
            viewInBmR.top = -(h - bmH)/2;
            viewInBmR.bottom = viewInBmR.top + h;
            }
        else
            {
            viewInBmR.top = 0;
            viewInBmR.bottom = bmH;
            int w = Math.round(bmH/heightToWidth);
            viewInBmR.left = -(w - bmW)/2;
            viewInBmR.right = viewInBmR.left + w;
            }
        }

    /**
     * Test whether or not the bitmap is zoomed such that it's the largest it can be and still fit entirely inside the view.
     * @return True if it's zoomed to fit, false otherwise.
     */
    public boolean isZoomedToFit()
        {
        if (bm == null || viewInBmR == null) return false;
        if (viewInBmR.left == 0 && viewInBmR.right == bmW && viewInBmR.height() >= bmH) return true;
        if (viewInBmR.top == 0 && viewInBmR.bottom == bmH && viewInBmR.width() >= bmW) return true;
        return false;
        }

    /**
     * Zoom so that there's a 1 to 1 correspondence between pixels in the bitmap and pixels on the screen, and center on a point.
     * @param centerX The X co-ordinate of the point to center on (in view pixel co-ordinates).
     * @param centerY The Y co-ordinate of the point to center on (in view pixel co-ordinates).
     * @return True if there was any change to the contents of the view and false if the view remains unchanged (e.g. if it was already positioned and zoomed to pixel 1 to 1).
     */
    public boolean zoomToPixel1To1(int centerX, int centerY)
        {
        if (bm == null || viewInBmR == null) return false;
        layoutViewW = getWidth();
        layoutViewH = getHeight();
        if (mzo == MaxZoomOut.FITS && (bmW <= layoutViewW && bmH <= layoutViewH)) return zoomToFit();
        prevViewInBmR.set(viewInBmR);
        float scaleFactor = computeScaleFactor();
        centerX = Math.round(scaleFactor*((float)centerX)) + viewInBmR.left;
        centerY = Math.round(scaleFactor*((float)centerY)) + viewInBmR.top;
        viewInBmR.left = centerX - layoutViewW/2;
        viewInBmR.right = viewInBmR.left + layoutViewW;
        viewInBmR.top = centerY - layoutViewH/2;
        viewInBmR.bottom = viewInBmR.top + layoutViewH;
        tidyPosition();
        if (!viewInBmR.equals(prevViewInBmR))
            {
            invalidate();
            return true;
            }
        else
            return false;
        }

    /**
     * Zoom in or out about a point by the given zoom factor.
     * @param zoomFactor The amount to zoom by.
     * @param centerX The X co-ordinate of the center point of the zoom (in view pixel co-ordinates).
     * @param centerY The Y co-ordinate of the center point of the zoom (in view pixel co-ordinates).
     * @return True if there was any change to the contents of the view and false if the view remains unchanged (e.g. if it was already positioned and zoomed in or out to te max).
     */
    public boolean zoom(float zoomFactor, int centerX, int centerY)
        {
        if (bm == null || viewInBmR == null) return false;
        prevViewInBmR.set(viewInBmR);
        layoutViewW = getWidth();
        layoutViewH = getHeight();
        float heightToWidth = ((float)layoutViewH)/((float)layoutViewW);
        int maxW = computeMaxW(heightToWidth);
        int maxH = computeMaxH(heightToWidth);
        int oldH = viewInBmR.height();
        int oldW = viewInBmR.width();
        int newH;
        int newW;
        if (oldH > oldW)
            {
            newH = Math.round(oldH/zoomFactor);
            if (newH > maxH) newH = maxH;
            if (newH < minViewInBmRDimension) newH = minViewInBmRDimension;
            newW = Math.round(newH/heightToWidth);
            }
        else
            {
            newW = Math.round(oldW/zoomFactor);
            if (newW > maxW) newW = maxW;
            if (newW < minViewInBmRDimension) newW = minViewInBmRDimension;
            newH = Math.round(newW*heightToWidth);
            }
        float scaleFactor = computeScaleFactor();
        centerX = Math.round(scaleFactor*((float)centerX)) + viewInBmR.left;
        centerY = Math.round(scaleFactor*((float)centerY)) + viewInBmR.top;
        viewInBmR.left = centerX - (newW*(centerX - viewInBmR.left))/oldW;
        viewInBmR.right = viewInBmR.left + newW;
        viewInBmR.top = centerY - (newH*(centerY - viewInBmR.top))/oldH;
        viewInBmR.bottom = viewInBmR.top + newH;
        tidyPosition();
        if (!viewInBmR.equals(prevViewInBmR))
            {
            invalidate();
            return true;
            }
        else
            return false;
        }

    protected void onDraw(Canvas cvs)
        {
        super.onDraw(cvs);
        if (bm == null) return;
        int viewW = getWidth();
        int viewH = getHeight();
        if (viewInBmR == null)
            {
            viewInBmR = new Rect();
            zoomToFitNoInvalidate();
            }
        else if (layoutViewW != viewW || layoutViewH != viewH)
            {
            layoutViewW = viewW;
            layoutViewH = viewH;
            float scaleFactor = computeScaleFactor();
            int w = Math.round(scaleFactor*((float)viewW));
            int h = Math.round(scaleFactor*((float)viewH));
            float heightToWidth = ((float)viewH)/((float)viewW);
            int maxW = computeMaxW(heightToWidth);
            int maxH = computeMaxH(heightToWidth);
            if (w < minViewInBmRDimension)
                {
                w = minViewInBmRDimension;
                h = Math.round(heightToWidth*w);
                }
            else if (w > maxW)
                {
                w = maxW;
                h = Math.round(heightToWidth*w);
                }
            if (h < minViewInBmRDimension)
                {
                h = minViewInBmRDimension;
                w = Math.round(h/heightToWidth);
                }
            else if (h > maxH)
                {
                h = maxH;
                w = Math.round(h/heightToWidth);
                }
            final int SLOP = 2;
            if ((bmW - SLOP <= w && w <= bmW + SLOP && h >= bmH - SLOP) || (bmH - SLOP <= h && h <= bmH + SLOP && w >= bmW - SLOP))
                zoomToFitNoInvalidate();
            else
                {
                int centerX = (viewInBmR.left + viewInBmR.right)/2;
                int centerY = (viewInBmR.top + viewInBmR.bottom)/2;
                viewInBmR.left = centerX - w/2;
                viewInBmR.right = viewInBmR.left + w;
                viewInBmR.top = centerY - h/2;
                viewInBmR.bottom = viewInBmR.top + h;
                tidyPosition();
                }
            }
        if (viewInBmR.right > bmW)
            {
            bmShowingR.left = 0;
            bmShowingR.right = bmW;
            int w = (viewW*bmW)/viewInBmR.width();
            viewShowingR.left = (viewW - w)/2;
            viewShowingR.right = viewShowingR.left + w;
            }
        else
            {
            bmShowingR.left = viewInBmR.left;
            bmShowingR.right = viewInBmR.right;
            viewShowingR.left = 0;
            viewShowingR.right = viewW;
            }
        if (viewInBmR.bottom > bmH)
            {
            bmShowingR.top = 0;
            bmShowingR.bottom = bmH;
            int h = (viewH*bmH)/viewInBmR.height();
            viewShowingR.top = (viewH - h)/2;
            viewShowingR.bottom = viewShowingR.top + h;
            }
        else
            {
            bmShowingR.top = viewInBmR.top;
            bmShowingR.bottom = viewInBmR.bottom;
            viewShowingR.top = 0;
            viewShowingR.bottom = viewH;
            }
        cvs.drawBitmap(bm, bmShowingR, viewShowingR, null);
        }

    private int computeMaxW(float heightToWidth)
        {
        MaxZoomOut effectiveMZO = mzo;
        if (effectiveMZO == MaxZoomOut.FITS_OR_1TO1 && (bmW >= layoutViewW || bmH >= layoutViewH)) effectiveMZO = MaxZoomOut.FITS;
        switch (effectiveMZO)
            {
            case BY8:          return 8*bmW;
            case FITS:         return layoutViewH*bmW > layoutViewW*bmH ? bmW : Math.round(bmH/heightToWidth);
            case FITS_OR_1TO1: return layoutViewW;
            }
        return -1;
        }

    private int computeMaxH(float heightToWidth)
        {
        MaxZoomOut effectiveMZO = mzo;
        if (effectiveMZO == MaxZoomOut.FITS_OR_1TO1 && (bmW >= layoutViewW || bmH >= layoutViewH)) effectiveMZO = MaxZoomOut.FITS;
        switch (effectiveMZO)
            {
            case BY8:          return 8*bmH;
            case FITS:         return layoutViewH*bmW > layoutViewW*bmH ? Math.round(heightToWidth*bmW) : bmH;
            case FITS_OR_1TO1: return layoutViewH;
            }
        return -1;
        }

    private float computeScaleFactor()
        {
        if (bmShowingR.height() > bmShowingR.width())
            return ((float)bmShowingR.height())/((float)viewShowingR.height());
        else
            return ((float)bmShowingR.width())/((float)viewShowingR.width());
        }

    private void tidyPosition()
        {
        int h = viewInBmR.height();
        if (h >= bmH)
            {
            viewInBmR.top = -(h - bmH)/2;
            viewInBmR.bottom = viewInBmR.top + h;
            }
        else
            {
            if (viewInBmR.top < 0)
                {
                viewInBmR.top = 0;
                viewInBmR.bottom = h;
                }
            else if (viewInBmR.bottom > bmH)
                {
                viewInBmR.top = bmH - h;
                viewInBmR.bottom = bmH;
                }
            }
        int w = viewInBmR.width();
        if (w >= bmW)
            {
            viewInBmR.left = -(w - bmW)/2;
            viewInBmR.right = viewInBmR.left + w;
            }
        else
            {
            if (viewInBmR.left < 0)
                {
                viewInBmR.left = 0;
                viewInBmR.right = w;
                }
            else if (viewInBmR.right > bmW)
                {
                viewInBmR.left = bmW - w;
                viewInBmR.right = bmW;
                }
            }
        }
    }
