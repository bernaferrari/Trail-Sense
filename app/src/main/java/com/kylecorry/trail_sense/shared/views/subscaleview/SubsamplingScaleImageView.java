package com.kylecorry.trail_sense.shared.views.subscaleview;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import com.kylecorry.trail_sense.shared.views.subscaleview.decoder.CompatDecoderFactory;
import com.kylecorry.trail_sense.shared.views.subscaleview.decoder.DecoderFactory;
import com.kylecorry.trail_sense.shared.views.subscaleview.decoder.ImageDecoder;
import com.kylecorry.trail_sense.shared.views.subscaleview.decoder.ImageRegionDecoder;
import com.kylecorry.trail_sense.shared.views.subscaleview.decoder.SkiaImageDecoder;
import com.kylecorry.trail_sense.shared.views.subscaleview.decoder.SkiaImageRegionDecoder;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
public class SubsamplingScaleImageView extends View {
    public static final int ORIENTATION_USE_EXIF = -1;
    public static final int ORIENTATION_0 = 0;
    public static final int ORIENTATION_90 = 90;
    public static final int ZOOM_FOCUS_FIXED = 1;
    public static final int ZOOM_FOCUS_CENTER = 2;
    public static final int ZOOM_FOCUS_CENTER_IMMEDIATE = 3;
    public static final int EASE_OUT_QUAD = 1;
    public static final int EASE_IN_OUT_QUAD = 2;
    public static final int PAN_LIMIT_INSIDE = 1;
    public static final int PAN_LIMIT_OUTSIDE = 2;
    public static final int PAN_LIMIT_CENTER = 3;
    public static final int SCALE_TYPE_CENTER_INSIDE = 1;
    public static final int SCALE_TYPE_CENTER_CROP = 2;
    public static final int SCALE_TYPE_CUSTOM = 3;
    public static final int SCALE_TYPE_START = 4;
    public static final int ORIGIN_ANIM = 1;
    public static final int ORIGIN_TOUCH = 2;
    public static final int ORIGIN_FLING = 3;
    public static final int ORIGIN_DOUBLE_TAP_ZOOM = 4;
    private Bitmap bitmap;
    private boolean bitmapIsPreview;
    private boolean bitmapIsCached;
    private Uri uri;
    private int fullImageSampleSize;
    private Map<Integer, List<Tile>> tileMap;
    private boolean debug;
    private int orientation;
    private float maxScale;
    private float minScale;
    private int minimumTileDpi;
    private int panLimit;
    private int minimumScaleType;
    public static final int TILE_SIZE_AUTO = Integer.MAX_VALUE;
    private int maxTileWidth;
    private int maxTileHeight;
    private Executor executor;
    private boolean eagerLoadingEnabled;
    private boolean panEnabled;
    private boolean zoomEnabled;
    private boolean quickScaleEnabled;
    private float doubleTapZoomScale;
    private float doubleTapZoomBy;
    private boolean alwaysZoomDoubleTap;
    private int doubleTapZoomStyle;
    private int doubleTapZoomDuration;
    private float scale;
    private float scaleStart;
    public PointF vTranslate;
    private PointF vTranslateStart;
    private PointF vTranslateBefore;
    private Float pendingScale;
    private PointF sPendingCenter;
    private PointF sRequestedCenter;
    private int sWidth;
    private int sHeight;
    private int sOrientation;
    private Rect sRegion;
    private Rect pRegion;
    private boolean isZooming;
    private boolean isPanning;
    private boolean isQuickScaling;
    private int maxTouchCount;
    private GestureDetector detector;
    private GestureDetector singleDetector;
    private ImageRegionDecoder decoder;
    private final ReadWriteLock decoderLock;
    private DecoderFactory<? extends ImageDecoder> bitmapDecoderFactory;
    private DecoderFactory<? extends ImageRegionDecoder> regionDecoderFactory;
    private PointF vCenterStart;
    private float vDistStart;
    private final float quickScaleThreshold;
    private float quickScaleLastDistance;
    private boolean quickScaleMoved;
    private PointF quickScaleVLastPoint;
    private PointF quickScaleSCenter;
    private PointF quickScaleVStart;
    private Anim anim;
    private boolean readySent;
    private boolean imageLoadedSent;
    private OnImageEventListener onImageEventListener;
    private OnStateChangedListener onStateChangedListener;
    private View.OnLongClickListener onLongClickListener;
    private final Handler handler;
    private static final int MESSAGE_LONG_CLICK = 1;
    private Paint bitmapPaint;
    private Paint debugTextPaint;
    private Paint debugLinePaint;
    private Paint tileBgPaint;
    private ScaleAndTranslate satTemp;
    private Matrix matrix;
    private RectF sRect;
    private final float[] srcArray;
    private final float[] dstArray;
    private final float density;
    private static Bitmap.Config preferredBitmapConfig;
    private static final String TAG = SubsamplingScaleImageView.class.getSimpleName();
    public static final int ORIENTATION_180 = 180;
    public static final int ORIENTATION_270 = 270;
    private static final List<Integer> VALID_ORIENTATIONS = Arrays.asList(0, 90, Integer.valueOf(ORIENTATION_180), Integer.valueOf(ORIENTATION_270), -1);
    private static final List<Integer> VALID_ZOOM_STYLES = Arrays.asList(1, 2, 3);
    private static final List<Integer> VALID_EASING_STYLES = Arrays.asList(2, 1);
    private static final List<Integer> VALID_PAN_LIMITS = Arrays.asList(1, 2, 3);
    private static final List<Integer> VALID_SCALE_TYPES = Arrays.asList(2, 1, 3, 4);
    public interface OnAnimationEventListener {
        void onComplete();

        void onInterruptedByUser();

        void onInterruptedByNewAnim();
    }
    public interface OnImageEventListener {
        void onReady();

        void onImageLoaded();

        void onPreviewLoadError(Exception exc);

        void onImageLoadError(Exception exc);

        void onTileLoadError(Exception exc);

        void onPreviewReleased();
    }
    public interface OnStateChangedListener {
        void onScaleChanged(float f, int i);

        void onCenterChanged(PointF pointF, int i);
    }

    public SubsamplingScaleImageView(Context context, AttributeSet attr) {
        super(context, attr);
        this.orientation = 0;
        this.maxScale = 2.0f;
        this.minScale = minScale();
        this.minimumTileDpi = -1;
        this.panLimit = 1;
        this.minimumScaleType = 1;
        this.maxTileWidth = TILE_SIZE_AUTO;
        this.maxTileHeight = TILE_SIZE_AUTO;
        this.executor = AsyncTask.THREAD_POOL_EXECUTOR;
        this.eagerLoadingEnabled = true;
        this.panEnabled = true;
        this.zoomEnabled = true;
        this.quickScaleEnabled = true;
        this.doubleTapZoomScale = 1.0f;
        this.doubleTapZoomBy = 2.0f;
        this.alwaysZoomDoubleTap = false;
        this.doubleTapZoomStyle = 1;
        this.doubleTapZoomDuration = 500;
        this.decoderLock = new ReentrantReadWriteLock(true);
        this.bitmapDecoderFactory = new CompatDecoderFactory(SkiaImageDecoder.class);
        this.regionDecoderFactory = new CompatDecoderFactory(SkiaImageRegionDecoder.class);
        this.srcArray = new float[8];
        this.dstArray = new float[8];
        this.density = getResources().getDisplayMetrics().density;
        setMinimumDpi(160);
        setDoubleTapZoomDpi(160);
        setMinimumTileDpi(320);
        setGestureDetector(context);
        this.handler = new Handler(new Handler.Callback() { // from class: com.kylecorry.trail_sense.shared.views.subscaleview.SubsamplingScaleImageView.1
            @Override // android.os.Handler.Callback
            public boolean handleMessage(Message message) {
                if (message.what == 1 && SubsamplingScaleImageView.this.onLongClickListener != null) {
                    SubsamplingScaleImageView.this.maxTouchCount = 0;
                    SubsamplingScaleImageView.super.setOnLongClickListener(SubsamplingScaleImageView.this.onLongClickListener);
                    SubsamplingScaleImageView.this.performLongClick();
                    SubsamplingScaleImageView.super.setOnLongClickListener(null);
                    return true;
                }
                return true;
            }
        });
        this.quickScaleThreshold = TypedValue.applyDimension(1, 20.0f, context.getResources().getDisplayMetrics());
    }

    public SubsamplingScaleImageView(Context context) {
        this(context, null);
    }

    public static Bitmap.Config getPreferredBitmapConfig() {
        return preferredBitmapConfig;
    }

    public static void setPreferredBitmapConfig(Bitmap.Config preferredBitmapConfig2) {
        preferredBitmapConfig = preferredBitmapConfig2;
    }

    public final void setOrientation(int orientation) {
        if (!VALID_ORIENTATIONS.contains(Integer.valueOf(orientation))) {
            throw new IllegalArgumentException("Invalid orientation: " + orientation);
        }
        this.orientation = orientation;
        reset(false);
        invalidate();
        requestLayout();
    }

    public final void setImage(@NonNull ImageSource imageSource) {
        setImage(imageSource, null, null);
    }

    public final void setImage(@NonNull ImageSource imageSource, ImageViewState state) {
        setImage(imageSource, null, state);
    }

    public final void setImage(@NonNull ImageSource imageSource, ImageSource previewSource) {
        setImage(imageSource, previewSource, null);
    }

    public final void setImage(@NonNull ImageSource imageSource, ImageSource previewSource, ImageViewState state) {
        if (imageSource == null) {
            throw new NullPointerException("imageSource must not be null");
        }
        reset(true);
        if (state != null) {
            restoreState(state);
        }
        if (previewSource != null) {
            if (imageSource.getBitmap() != null) {
                throw new IllegalArgumentException("Preview image cannot be used when a bitmap is provided for the main image");
            }
            if (imageSource.getSWidth() <= 0 || imageSource.getSHeight() <= 0) {
                throw new IllegalArgumentException("Preview image cannot be used unless dimensions are provided for the main image");
            }
            this.sWidth = imageSource.getSWidth();
            this.sHeight = imageSource.getSHeight();
            this.pRegion = previewSource.getSRegion();
            if (previewSource.getBitmap() != null) {
                this.bitmapIsCached = previewSource.isCached();
                onPreviewLoaded(previewSource.getBitmap());
            } else {
                Uri uri = previewSource.getUri();
                if (uri == null && previewSource.getResource() != null) {
                    uri = Uri.parse("android.resource://" + getContext().getPackageName() + "/" + previewSource.getResource());
                }
                BitmapLoadTask task = new BitmapLoadTask(this, getContext(), this.bitmapDecoderFactory, uri, true);
                execute(task);
            }
        }
        if (imageSource.getBitmap() != null && imageSource.getSRegion() != null) {
            onImageLoaded(Bitmap.createBitmap(imageSource.getBitmap(), imageSource.getSRegion().left, imageSource.getSRegion().top, imageSource.getSRegion().width(), imageSource.getSRegion().height()), 0, false);
            return;
        }
        if (imageSource.getBitmap() != null) {
            onImageLoaded(imageSource.getBitmap(), 0, imageSource.isCached());
            return;
        }
        this.sRegion = imageSource.getSRegion();
        this.uri = imageSource.getUri();
        if (this.uri == null && imageSource.getResource() != null) {
            this.uri = Uri.parse("android.resource://" + getContext().getPackageName() + "/" + imageSource.getResource());
        }
        if (imageSource.getTile() || this.sRegion != null) {
            TilesInitTask task2 = new TilesInitTask(this, getContext(), this.regionDecoderFactory, this.uri);
            execute(task2);
        } else {
            BitmapLoadTask task3 = new BitmapLoadTask(this, getContext(), this.bitmapDecoderFactory, this.uri, false);
            execute(task3);
        }
    }

    public boolean isZooming() {
        return this.isZooming;
    }

    public boolean isPanning() {
        return this.isPanning;
    }

    public boolean isQuickScaling() {
        return this.isQuickScaling;
    }

    private void reset(boolean newImage) {
        debug("reset newImage=" + newImage, new Object[0]);
        this.scale = 0.0f;
        this.scaleStart = 0.0f;
        this.vTranslate = null;
        this.vTranslateStart = null;
        this.vTranslateBefore = null;
        this.pendingScale = Float.valueOf(0.0f);
        this.sPendingCenter = null;
        this.sRequestedCenter = null;
        this.isZooming = false;
        this.isPanning = false;
        this.isQuickScaling = false;
        this.maxTouchCount = 0;
        this.fullImageSampleSize = 0;
        this.vCenterStart = null;
        this.vDistStart = 0.0f;
        this.quickScaleLastDistance = 0.0f;
        this.quickScaleMoved = false;
        this.quickScaleSCenter = null;
        this.quickScaleVLastPoint = null;
        this.quickScaleVStart = null;
        this.anim = null;
        this.satTemp = null;
        this.matrix = null;
        this.sRect = null;
        if (newImage) {
            this.uri = null;
            this.decoderLock.writeLock().lock();
            try {
                if (this.decoder != null) {
                    this.decoder.recycle();
                    this.decoder = null;
                }
                if (this.bitmap != null && !this.bitmapIsCached) {
                    this.bitmap.recycle();
                }
                if (this.bitmap != null && this.bitmapIsCached && this.onImageEventListener != null) {
                    this.onImageEventListener.onPreviewReleased();
                }
                this.sWidth = 0;
                this.sHeight = 0;
                this.sOrientation = 0;
                this.sRegion = null;
                this.pRegion = null;
                this.readySent = false;
                this.imageLoadedSent = false;
                this.bitmap = null;
                this.bitmapIsPreview = false;
                this.bitmapIsCached = false;
            } finally {
                this.decoderLock.writeLock().unlock();
            }
        }
        if (this.tileMap != null) {
            for (Map.Entry<Integer, List<Tile>> tileMapEntry : this.tileMap.entrySet()) {
                for (Tile tile : tileMapEntry.getValue()) {
                    tile.visible = false;
                    if (tile.bitmap != null) {
                        tile.bitmap.recycle();
                        tile.bitmap = null;
                    }
                }
            }
            this.tileMap = null;
        }
        setGestureDetector(getContext());
    }

    private void setGestureDetector(final Context context) {
        this.detector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() { // from class: com.kylecorry.trail_sense.shared.views.subscaleview.SubsamplingScaleImageView.2
            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (SubsamplingScaleImageView.this.panEnabled && SubsamplingScaleImageView.this.readySent && SubsamplingScaleImageView.this.vTranslate != null && e1 != null && e2 != null && ((Math.abs(e1.getX() - e2.getX()) > 50.0f || Math.abs(e1.getY() - e2.getY()) > 50.0f) && ((Math.abs(velocityX) > 500.0f || Math.abs(velocityY) > 500.0f) && !SubsamplingScaleImageView.this.isZooming))) {
                    PointF vTranslateEnd = new PointF(SubsamplingScaleImageView.this.vTranslate.x + (velocityX * 0.25f), SubsamplingScaleImageView.this.vTranslate.y + (velocityY * 0.25f));
                    float sCenterXEnd = ((SubsamplingScaleImageView.this.getWidth() / 2) - vTranslateEnd.x) / SubsamplingScaleImageView.this.scale;
                    float sCenterYEnd = ((SubsamplingScaleImageView.this.getHeight() / 2) - vTranslateEnd.y) / SubsamplingScaleImageView.this.scale;
                    SubsamplingScaleImageView.this.new AnimationBuilder(new PointF(sCenterXEnd, sCenterYEnd)).withEasing(1).withPanLimited(false).withOrigin(3).start();
                    return true;
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnDoubleTapListener
            public boolean onSingleTapConfirmed(MotionEvent e) {
                SubsamplingScaleImageView.this.performClick();
                return true;
            }

            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnDoubleTapListener
            public boolean onDoubleTap(MotionEvent e) {
                if (SubsamplingScaleImageView.this.zoomEnabled && SubsamplingScaleImageView.this.readySent && SubsamplingScaleImageView.this.vTranslate != null) {
                    SubsamplingScaleImageView.this.setGestureDetector(context);
                    if (SubsamplingScaleImageView.this.quickScaleEnabled) {
                        SubsamplingScaleImageView.this.vCenterStart = new PointF(e.getX(), e.getY());
                        SubsamplingScaleImageView.this.vTranslateStart = new PointF(SubsamplingScaleImageView.this.vTranslate.x, SubsamplingScaleImageView.this.vTranslate.y);
                        SubsamplingScaleImageView.this.scaleStart = SubsamplingScaleImageView.this.scale;
                        SubsamplingScaleImageView.this.isQuickScaling = true;
                        SubsamplingScaleImageView.this.isZooming = true;
                        SubsamplingScaleImageView.this.quickScaleLastDistance = -1.0f;
                        SubsamplingScaleImageView.this.quickScaleSCenter = SubsamplingScaleImageView.this.viewToSourceCoord(SubsamplingScaleImageView.this.vCenterStart);
                        SubsamplingScaleImageView.this.quickScaleVStart = new PointF(e.getX(), e.getY());
                        SubsamplingScaleImageView.this.quickScaleVLastPoint = new PointF(SubsamplingScaleImageView.this.quickScaleSCenter.x, SubsamplingScaleImageView.this.quickScaleSCenter.y);
                        SubsamplingScaleImageView.this.quickScaleMoved = false;
                        return false;
                    }
                    SubsamplingScaleImageView.this.doubleTapZoom(SubsamplingScaleImageView.this.viewToSourceCoord(new PointF(e.getX(), e.getY())), new PointF(e.getX(), e.getY()));
                    return true;
                }
                return super.onDoubleTapEvent(e);
            }
        });
        this.singleDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() { // from class: com.kylecorry.trail_sense.shared.views.subscaleview.SubsamplingScaleImageView.3
            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnDoubleTapListener
            public boolean onSingleTapConfirmed(MotionEvent e) {
                SubsamplingScaleImageView.this.performClick();
                return true;
            }
        });
    }

    @Override // android.view.View
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        debug("onSizeChanged %dx%d -> %dx%d", Integer.valueOf(oldw), Integer.valueOf(oldh), Integer.valueOf(w), Integer.valueOf(h));
        PointF sCenter = getCenter();
        if (this.readySent && sCenter != null) {
            this.anim = null;
            this.pendingScale = Float.valueOf(this.scale);
            this.sPendingCenter = sCenter;
        }
    }

    @Override // android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int parentWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        boolean resizeWidth = widthSpecMode != 1073741824;
        boolean resizeHeight = heightSpecMode != 1073741824;
        int width = parentWidth;
        int height = parentHeight;
        if (this.sWidth > 0 && this.sHeight > 0) {
            if (resizeWidth && resizeHeight) {
                width = sWidth();
                height = sHeight();
            } else if (resizeHeight) {
                height = (int) ((((double) sHeight()) / ((double) sWidth())) * ((double) width));
            } else if (resizeWidth) {
                width = (int) ((((double) sWidth()) / ((double) sHeight())) * ((double) height));
            }
        }
        setMeasuredDimension(Math.max(width, getSuggestedMinimumWidth()), Math.max(height, getSuggestedMinimumHeight()));
    }

    @Override // android.view.View
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (this.anim != null && !this.anim.interruptible) {
            requestDisallowInterceptTouchEvent(true);
            return true;
        }
        if (this.anim != null && this.anim.listener != null) {
            try {
                this.anim.listener.onInterruptedByUser();
            } catch (Exception e) {
                Log.w(TAG, "Error thrown by animation listener", e);
            }
        }
        this.anim = null;
        if (this.vTranslate == null) {
            if (this.singleDetector != null) {
                this.singleDetector.onTouchEvent(event);
                return true;
            }
            return true;
        }
        if (!this.isQuickScaling && (this.detector == null || this.detector.onTouchEvent(event))) {
            this.isZooming = false;
            this.isPanning = false;
            this.maxTouchCount = 0;
            return true;
        }
        if (this.vTranslateStart == null) {
            this.vTranslateStart = new PointF(0.0f, 0.0f);
        }
        if (this.vTranslateBefore == null) {
            this.vTranslateBefore = new PointF(0.0f, 0.0f);
        }
        if (this.vCenterStart == null) {
            this.vCenterStart = new PointF(0.0f, 0.0f);
        }
        float scaleBefore = this.scale;
        this.vTranslateBefore.set(this.vTranslate);
        boolean handled = onTouchEventInternal(event);
        sendStateChanged(scaleBefore, this.vTranslateBefore, 2);
        return handled || super.onTouchEvent(event);
    }

    private boolean onTouchEventInternal(@NonNull MotionEvent event) {
        int touchCount = event.getPointerCount();
        switch (event.getAction()) {
            case ORIENTATION_0 /* 0 */:
            case 5:
            case 261:
                this.anim = null;
                requestDisallowInterceptTouchEvent(true);
                this.maxTouchCount = Math.max(this.maxTouchCount, touchCount);
                if (touchCount >= 2) {
                    if (this.zoomEnabled) {
                        float distance = distance(event.getX(0), event.getX(1), event.getY(0), event.getY(1));
                        this.scaleStart = this.scale;
                        this.vDistStart = distance;
                        this.vTranslateStart.set(this.vTranslate.x, this.vTranslate.y);
                        this.vCenterStart.set((event.getX(0) + event.getX(1)) / 2.0f, (event.getY(0) + event.getY(1)) / 2.0f);
                    } else {
                        this.maxTouchCount = 0;
                    }
                    this.handler.removeMessages(1);
                } else if (!this.isQuickScaling) {
                    this.vTranslateStart.set(this.vTranslate.x, this.vTranslate.y);
                    this.vCenterStart.set(event.getX(), event.getY());
                    this.handler.sendEmptyMessageDelayed(1, 600L);
                }
                break;
            case 1:
            case 6:
            case 262:
                this.handler.removeMessages(1);
                if (this.isQuickScaling) {
                    this.isQuickScaling = false;
                    if (!this.quickScaleMoved) {
                        doubleTapZoom(this.quickScaleSCenter, this.vCenterStart);
                    }
                }
                if (this.maxTouchCount > 0 && (this.isZooming || this.isPanning)) {
                    if (this.isZooming && touchCount == 2) {
                        this.isPanning = true;
                        this.vTranslateStart.set(this.vTranslate.x, this.vTranslate.y);
                        if (event.getActionIndex() == 1) {
                            this.vCenterStart.set(event.getX(0), event.getY(0));
                        } else {
                            this.vCenterStart.set(event.getX(1), event.getY(1));
                        }
                    }
                    if (touchCount < 3) {
                        this.isZooming = false;
                    }
                    if (touchCount < 2) {
                        this.isPanning = false;
                        this.maxTouchCount = 0;
                    }
                    refreshRequiredTiles(true);
                } else if (touchCount == 1) {
                    this.isZooming = false;
                    this.isPanning = false;
                    this.maxTouchCount = 0;
                }
                break;
            case 2:
                boolean consumed = false;
                if (this.maxTouchCount > 0) {
                    if (touchCount >= 2) {
                        float vDistEnd = distance(event.getX(0), event.getX(1), event.getY(0), event.getY(1));
                        float vCenterEndX = (event.getX(0) + event.getX(1)) / 2.0f;
                        float vCenterEndY = (event.getY(0) + event.getY(1)) / 2.0f;
                        if (this.zoomEnabled && (distance(this.vCenterStart.x, vCenterEndX, this.vCenterStart.y, vCenterEndY) > 5.0f || Math.abs(vDistEnd - this.vDistStart) > 5.0f || this.isPanning)) {
                            this.isZooming = true;
                            this.isPanning = true;
                            consumed = true;
                            double previousScale = this.scale;
                            this.scale = Math.min(this.maxScale, (vDistEnd / this.vDistStart) * this.scaleStart);
                            if (this.scale <= minScale()) {
                                this.vDistStart = vDistEnd;
                                this.scaleStart = minScale();
                                this.vCenterStart.set(vCenterEndX, vCenterEndY);
                                this.vTranslateStart.set(this.vTranslate);
                            } else if (this.panEnabled) {
                                float vLeftStart = this.vCenterStart.x - this.vTranslateStart.x;
                                float vTopStart = this.vCenterStart.y - this.vTranslateStart.y;
                                float vLeftNow = vLeftStart * (this.scale / this.scaleStart);
                                float vTopNow = vTopStart * (this.scale / this.scaleStart);
                                this.vTranslate.x = vCenterEndX - vLeftNow;
                                this.vTranslate.y = vCenterEndY - vTopNow;
                                if ((previousScale * ((double) sHeight()) < getHeight() && this.scale * sHeight() >= getHeight()) || (previousScale * ((double) sWidth()) < getWidth() && this.scale * sWidth() >= getWidth())) {
                                    fitToBounds(true);
                                    this.vCenterStart.set(vCenterEndX, vCenterEndY);
                                    this.vTranslateStart.set(this.vTranslate);
                                    this.scaleStart = this.scale;
                                    this.vDistStart = vDistEnd;
                                }
                            } else if (this.sRequestedCenter != null) {
                                this.vTranslate.x = (getWidth() / 2) - (this.scale * this.sRequestedCenter.x);
                                this.vTranslate.y = (getHeight() / 2) - (this.scale * this.sRequestedCenter.y);
                            } else {
                                this.vTranslate.x = (getWidth() / 2) - (this.scale * (sWidth() / 2));
                                this.vTranslate.y = (getHeight() / 2) - (this.scale * (sHeight() / 2));
                            }
                            fitToBounds(true);
                            refreshRequiredTiles(this.eagerLoadingEnabled);
                        }
                    } else if (this.isQuickScaling) {
                        float dist = (Math.abs(this.quickScaleVStart.y - event.getY()) * 2.0f) + this.quickScaleThreshold;
                        if (this.quickScaleLastDistance == -1.0f) {
                            this.quickScaleLastDistance = dist;
                        }
                        boolean isUpwards = event.getY() > this.quickScaleVLastPoint.y;
                        this.quickScaleVLastPoint.set(0.0f, event.getY());
                        float spanDiff = Math.abs(1.0f - (dist / this.quickScaleLastDistance)) * 0.5f;
                        if (spanDiff > 0.03f || this.quickScaleMoved) {
                            this.quickScaleMoved = true;
                            float multiplier = 1.0f;
                            if (this.quickScaleLastDistance > 0.0f) {
                                multiplier = isUpwards ? 1.0f + spanDiff : 1.0f - spanDiff;
                            }
                            double previousScale2 = this.scale;
                            this.scale = Math.max(minScale(), Math.min(this.maxScale, this.scale * multiplier));
                            if (this.panEnabled) {
                                float vLeftStart2 = this.vCenterStart.x - this.vTranslateStart.x;
                                float vTopStart2 = this.vCenterStart.y - this.vTranslateStart.y;
                                float vLeftNow2 = vLeftStart2 * (this.scale / this.scaleStart);
                                float vTopNow2 = vTopStart2 * (this.scale / this.scaleStart);
                                this.vTranslate.x = this.vCenterStart.x - vLeftNow2;
                                this.vTranslate.y = this.vCenterStart.y - vTopNow2;
                                if ((previousScale2 * ((double) sHeight()) < getHeight() && this.scale * sHeight() >= getHeight()) || (previousScale2 * ((double) sWidth()) < getWidth() && this.scale * sWidth() >= getWidth())) {
                                    fitToBounds(true);
                                    this.vCenterStart.set(sourceToViewCoord(this.quickScaleSCenter));
                                    this.vTranslateStart.set(this.vTranslate);
                                    this.scaleStart = this.scale;
                                    dist = 0.0f;
                                }
                            } else if (this.sRequestedCenter != null) {
                                this.vTranslate.x = (getWidth() / 2) - (this.scale * this.sRequestedCenter.x);
                                this.vTranslate.y = (getHeight() / 2) - (this.scale * this.sRequestedCenter.y);
                            } else {
                                this.vTranslate.x = (getWidth() / 2) - (this.scale * (sWidth() / 2));
                                this.vTranslate.y = (getHeight() / 2) - (this.scale * (sHeight() / 2));
                            }
                        }
                        this.quickScaleLastDistance = dist;
                        fitToBounds(true);
                        refreshRequiredTiles(this.eagerLoadingEnabled);
                        consumed = true;
                    } else if (!this.isZooming) {
                        float dx = Math.abs(event.getX() - this.vCenterStart.x);
                        float dy = Math.abs(event.getY() - this.vCenterStart.y);
                        float offset = this.density * 5.0f;
                        if (dx > offset || dy > offset || this.isPanning) {
                            consumed = true;
                            this.vTranslate.x = this.vTranslateStart.x + (event.getX() - this.vCenterStart.x);
                            this.vTranslate.y = this.vTranslateStart.y + (event.getY() - this.vCenterStart.y);
                            float lastX = this.vTranslate.x;
                            float lastY = this.vTranslate.y;
                            fitToBounds(true);
                            boolean atXEdge = lastX != this.vTranslate.x;
                            boolean atYEdge = lastY != this.vTranslate.y;
                            boolean edgeXSwipe = atXEdge && dx > dy && !this.isPanning;
                            boolean edgeYSwipe = atYEdge && dy > dx && !this.isPanning;
                            boolean yPan = lastY == this.vTranslate.y && dy > offset * 3.0f;
                            if (!edgeXSwipe && !edgeYSwipe && (!atXEdge || !atYEdge || yPan || this.isPanning)) {
                                this.isPanning = true;
                            } else if (dx > offset || dy > offset) {
                                this.maxTouchCount = 0;
                                this.handler.removeMessages(1);
                                requestDisallowInterceptTouchEvent(false);
                            }
                            if (!this.panEnabled) {
                                this.vTranslate.x = this.vTranslateStart.x;
                                this.vTranslate.y = this.vTranslateStart.y;
                                requestDisallowInterceptTouchEvent(false);
                            }
                            refreshRequiredTiles(this.eagerLoadingEnabled);
                        }
                    }
                }
                if (consumed) {
                    this.handler.removeMessages(1);
                    invalidate();
                }
                break;
        }
        return true;
    }

    private void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    private void doubleTapZoom(PointF sCenter, PointF vFocus) {
        float targetScale;
        if (!this.panEnabled) {
            if (this.sRequestedCenter != null) {
                sCenter.x = this.sRequestedCenter.x;
                sCenter.y = this.sRequestedCenter.y;
            } else {
                sCenter.x = sWidth() / 2;
                sCenter.y = sHeight() / 2;
            }
        }
        float doubleTapZoomScale = Math.min(this.maxScale, this.doubleTapZoomScale);
        boolean zoomIn = (this.alwaysZoomDoubleTap && this.doubleTapZoomBy > 1.0f) || ((double) this.scale) <= ((double) doubleTapZoomScale) * 0.9d || this.scale == this.minScale;
        if (this.alwaysZoomDoubleTap) {
            targetScale = Math.max(Math.min(this.scale * this.doubleTapZoomBy, this.maxScale), minScale());
        } else {
            targetScale = zoomIn ? doubleTapZoomScale : minScale();
        }
        if (this.doubleTapZoomStyle == 3) {
            setScaleAndCenter(targetScale, sCenter);
        } else if (this.doubleTapZoomStyle == 2 || !zoomIn || !this.panEnabled) {
            new AnimationBuilder(targetScale, sCenter).withInterruptible(false).withDuration(this.doubleTapZoomDuration).withOrigin(4).start();
        } else if (this.doubleTapZoomStyle == 1) {
            new AnimationBuilder(targetScale, sCenter, vFocus).withInterruptible(false).withDuration(this.doubleTapZoomDuration).withOrigin(4).start();
        }
        invalidate();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        createPaints();
        if (this.sWidth == 0 || this.sHeight == 0 || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        if (this.tileMap == null && this.decoder != null) {
            initialiseBaseLayer(getMaxBitmapDimensions(canvas));
        }
        if (!checkReady()) {
            return;
        }
        preDraw();
        if (this.anim != null && this.anim.vFocusStart != null) {
            float scaleBefore = this.scale;
            if (this.vTranslateBefore == null) {
                this.vTranslateBefore = new PointF(0.0f, 0.0f);
            }
            this.vTranslateBefore.set(this.vTranslate);
            long scaleElapsed = System.currentTimeMillis() - this.anim.time;
            boolean finished = scaleElapsed > this.anim.duration;
            long scaleElapsed2 = Math.min(scaleElapsed, this.anim.duration);
            this.scale = ease(this.anim.easing, scaleElapsed2, this.anim.scaleStart, this.anim.scaleEnd - this.anim.scaleStart, this.anim.duration);
            float vFocusNowX = ease(this.anim.easing, scaleElapsed2, this.anim.vFocusStart.x, this.anim.vFocusEnd.x - this.anim.vFocusStart.x, this.anim.duration);
            float vFocusNowY = ease(this.anim.easing, scaleElapsed2, this.anim.vFocusStart.y, this.anim.vFocusEnd.y - this.anim.vFocusStart.y, this.anim.duration);
            this.vTranslate.x -= sourceToViewX(this.anim.sCenterEnd.x) - vFocusNowX;
            this.vTranslate.y -= sourceToViewY(this.anim.sCenterEnd.y) - vFocusNowY;
            fitToBounds(finished || this.anim.scaleStart == this.anim.scaleEnd);
            sendStateChanged(scaleBefore, this.vTranslateBefore, this.anim.origin);
            refreshRequiredTiles(finished);
            if (finished) {
                if (this.anim.listener != null) {
                    try {
                        this.anim.listener.onComplete();
                    } catch (Exception e) {
                        Log.w(TAG, "Error thrown by animation listener", e);
                    }
                }
                this.anim = null;
            }
            invalidate();
        }
        if (this.tileMap != null && isBaseLayerReady()) {
            int sampleSize = Math.min(this.fullImageSampleSize, calculateInSampleSize(this.scale));
            boolean hasMissingTiles = false;
            for (Map.Entry<Integer, List<Tile>> tileMapEntry : this.tileMap.entrySet()) {
                if (tileMapEntry.getKey().intValue() == sampleSize) {
                    for (Tile tile : tileMapEntry.getValue()) {
                        if (tile.visible && (tile.loading || tile.bitmap == null)) {
                            hasMissingTiles = true;
                        }
                    }
                }
            }
            for (Map.Entry<Integer, List<Tile>> tileMapEntry2 : this.tileMap.entrySet()) {
                if (tileMapEntry2.getKey().intValue() == sampleSize || hasMissingTiles) {
                    for (Tile tile2 : tileMapEntry2.getValue()) {
                        sourceToViewRect(tile2.sRect, tile2.vRect);
                        if (!tile2.loading && tile2.bitmap != null) {
                            if (this.tileBgPaint != null) {
                                canvas.drawRect(tile2.vRect, this.tileBgPaint);
                            }
                            if (this.matrix == null) {
                                this.matrix = new Matrix();
                            }
                            this.matrix.reset();
                            setMatrixArray(this.srcArray, 0.0f, 0.0f, tile2.bitmap.getWidth(), 0.0f, tile2.bitmap.getWidth(), tile2.bitmap.getHeight(), 0.0f, tile2.bitmap.getHeight());
                            if (getRequiredRotation() == 0) {
                                setMatrixArray(this.dstArray, tile2.vRect.left, tile2.vRect.top, tile2.vRect.right, tile2.vRect.top, tile2.vRect.right, tile2.vRect.bottom, tile2.vRect.left, tile2.vRect.bottom);
                            } else if (getRequiredRotation() == 90) {
                                setMatrixArray(this.dstArray, tile2.vRect.right, tile2.vRect.top, tile2.vRect.right, tile2.vRect.bottom, tile2.vRect.left, tile2.vRect.bottom, tile2.vRect.left, tile2.vRect.top);
                            } else if (getRequiredRotation() == 180) {
                                setMatrixArray(this.dstArray, tile2.vRect.right, tile2.vRect.bottom, tile2.vRect.left, tile2.vRect.bottom, tile2.vRect.left, tile2.vRect.top, tile2.vRect.right, tile2.vRect.top);
                            } else if (getRequiredRotation() == 270) {
                                setMatrixArray(this.dstArray, tile2.vRect.left, tile2.vRect.bottom, tile2.vRect.left, tile2.vRect.top, tile2.vRect.right, tile2.vRect.top, tile2.vRect.right, tile2.vRect.bottom);
                            }
                            this.matrix.setPolyToPoly(this.srcArray, 0, this.dstArray, 0, 4);
                            canvas.drawBitmap(tile2.bitmap, this.matrix, this.bitmapPaint);
                            if (this.debug) {
                                canvas.drawRect(tile2.vRect, this.debugLinePaint);
                            }
                        } else if (tile2.loading && this.debug) {
                            canvas.drawText("LOADING", tile2.vRect.left + px(5), tile2.vRect.top + px(35), this.debugTextPaint);
                        }
                        if (tile2.visible && this.debug) {
                            canvas.drawText("ISS " + tile2.sampleSize + " RECT " + tile2.sRect.top + "," + tile2.sRect.left + "," + tile2.sRect.bottom + "," + tile2.sRect.right, tile2.vRect.left + px(5), tile2.vRect.top + px(15), this.debugTextPaint);
                        }
                    }
                }
            }
        } else if (this.bitmap != null && !this.bitmap.isRecycled()) {
            float xScale = this.scale;
            float yScale = this.scale;
            if (this.bitmapIsPreview) {
                xScale = this.scale * (this.sWidth / this.bitmap.getWidth());
                yScale = this.scale * (this.sHeight / this.bitmap.getHeight());
            }
            if (this.matrix == null) {
                this.matrix = new Matrix();
            }
            this.matrix.reset();
            this.matrix.postScale(xScale, yScale);
            this.matrix.postRotate(getRequiredRotation());
            this.matrix.postTranslate(this.vTranslate.x, this.vTranslate.y);
            if (getRequiredRotation() == 180) {
                this.matrix.postTranslate(this.scale * this.sWidth, this.scale * this.sHeight);
            } else if (getRequiredRotation() == 90) {
                this.matrix.postTranslate(this.scale * this.sHeight, 0.0f);
            } else if (getRequiredRotation() == 270) {
                this.matrix.postTranslate(0.0f, this.scale * this.sWidth);
            }
            if (this.tileBgPaint != null) {
                if (this.sRect == null) {
                    this.sRect = new RectF();
                }
                this.sRect.set(0.0f, 0.0f, this.bitmapIsPreview ? this.bitmap.getWidth() : this.sWidth, this.bitmapIsPreview ? this.bitmap.getHeight() : this.sHeight);
                this.matrix.mapRect(this.sRect);
                canvas.drawRect(this.sRect, this.tileBgPaint);
            }
            canvas.drawBitmap(this.bitmap, this.matrix, this.bitmapPaint);
        }
        if (this.debug) {
            canvas.drawText("Scale: " + String.format(Locale.ENGLISH, "%.2f", Float.valueOf(this.scale)) + " (" + String.format(Locale.ENGLISH, "%.2f", Float.valueOf(minScale())) + " - " + String.format(Locale.ENGLISH, "%.2f", Float.valueOf(this.maxScale)) + ")", px(5), px(15), this.debugTextPaint);
            canvas.drawText("Translate: " + String.format(Locale.ENGLISH, "%.2f", Float.valueOf(this.vTranslate.x)) + ":" + String.format(Locale.ENGLISH, "%.2f", Float.valueOf(this.vTranslate.y)), px(5), px(30), this.debugTextPaint);
            PointF center = getCenter();
            canvas.drawText("Source center: " + String.format(Locale.ENGLISH, "%.2f", Float.valueOf(center.x)) + ":" + String.format(Locale.ENGLISH, "%.2f", Float.valueOf(center.y)), px(5), px(45), this.debugTextPaint);
            if (this.anim != null) {
                PointF vCenterStart = sourceToViewCoord(this.anim.sCenterStart);
                PointF vCenterEndRequested = sourceToViewCoord(this.anim.sCenterEndRequested);
                PointF vCenterEnd = sourceToViewCoord(this.anim.sCenterEnd);
                canvas.drawCircle(vCenterStart.x, vCenterStart.y, px(10), this.debugLinePaint);
                this.debugLinePaint.setColor(-65536);
                canvas.drawCircle(vCenterEndRequested.x, vCenterEndRequested.y, px(20), this.debugLinePaint);
                this.debugLinePaint.setColor(-16776961);
                canvas.drawCircle(vCenterEnd.x, vCenterEnd.y, px(25), this.debugLinePaint);
                this.debugLinePaint.setColor(-16711681);
                canvas.drawCircle(getWidth() / 2, getHeight() / 2, px(30), this.debugLinePaint);
            }
            if (this.vCenterStart != null) {
                this.debugLinePaint.setColor(-65536);
                canvas.drawCircle(this.vCenterStart.x, this.vCenterStart.y, px(20), this.debugLinePaint);
            }
            if (this.quickScaleSCenter != null) {
                this.debugLinePaint.setColor(-16776961);
                canvas.drawCircle(sourceToViewX(this.quickScaleSCenter.x), sourceToViewY(this.quickScaleSCenter.y), px(35), this.debugLinePaint);
            }
            if (this.quickScaleVStart != null && this.isQuickScaling) {
                this.debugLinePaint.setColor(-16711681);
                canvas.drawCircle(this.quickScaleVStart.x, this.quickScaleVStart.y, px(30), this.debugLinePaint);
            }
            this.debugLinePaint.setColor(-65281);
        }
    }

    private void setMatrixArray(float[] array, float f0, float f1, float f2, float f3, float f4, float f5, float f6, float f7) {
        array[0] = f0;
        array[1] = f1;
        array[2] = f2;
        array[3] = f3;
        array[4] = f4;
        array[5] = f5;
        array[6] = f6;
        array[7] = f7;
    }

    private boolean isBaseLayerReady() {
        if (this.bitmap != null && !this.bitmapIsPreview) {
            return true;
        }
        if (this.tileMap != null) {
            boolean baseLayerReady = true;
            for (Map.Entry<Integer, List<Tile>> tileMapEntry : this.tileMap.entrySet()) {
                if (tileMapEntry.getKey().intValue() == this.fullImageSampleSize) {
                    for (Tile tile : tileMapEntry.getValue()) {
                        if (tile.loading || tile.bitmap == null) {
                            baseLayerReady = false;
                        }
                    }
                }
            }
            return baseLayerReady;
        }
        return false;
    }

    private boolean checkReady() {
        boolean ready = getWidth() > 0 && getHeight() > 0 && this.sWidth > 0 && this.sHeight > 0 && (this.bitmap != null || isBaseLayerReady());
        if (!this.readySent && ready) {
            preDraw();
            this.readySent = true;
            onReady();
            if (this.onImageEventListener != null) {
                this.onImageEventListener.onReady();
            }
        }
        return ready;
    }

    private boolean checkImageLoaded() {
        boolean imageLoaded = isBaseLayerReady();
        if (!this.imageLoadedSent && imageLoaded) {
            preDraw();
            this.imageLoadedSent = true;
            onImageLoaded();
            if (this.onImageEventListener != null) {
                this.onImageEventListener.onImageLoaded();
            }
        }
        return imageLoaded;
    }

    private void createPaints() {
        if (this.bitmapPaint == null) {
            this.bitmapPaint = new Paint();
            this.bitmapPaint.setAntiAlias(true);
            this.bitmapPaint.setFilterBitmap(true);
            this.bitmapPaint.setDither(true);
        }
        if ((this.debugTextPaint == null || this.debugLinePaint == null) && this.debug) {
            this.debugTextPaint = new Paint();
            this.debugTextPaint.setTextSize(px(12));
            this.debugTextPaint.setColor(-65281);
            this.debugTextPaint.setStyle(Paint.Style.FILL);
            this.debugLinePaint = new Paint();
            this.debugLinePaint.setColor(-65281);
            this.debugLinePaint.setStyle(Paint.Style.STROKE);
            this.debugLinePaint.setStrokeWidth(px(1));
        }
    }

    private synchronized void initialiseBaseLayer(@NonNull Point maxTileDimensions) {
        debug("initialiseBaseLayer maxTileDimensions=%dx%d", Integer.valueOf(maxTileDimensions.x), Integer.valueOf(maxTileDimensions.y));
        this.satTemp = new ScaleAndTranslate(0.0f, new PointF(0.0f, 0.0f));
        fitToBounds(true, this.satTemp);
        this.fullImageSampleSize = calculateInSampleSize(this.satTemp.scale);
        if (this.fullImageSampleSize > 1) {
            this.fullImageSampleSize /= 2;
        }
        if (this.fullImageSampleSize == 1 && this.sRegion == null && sWidth() < maxTileDimensions.x && sHeight() < maxTileDimensions.y) {
            this.decoder.recycle();
            this.decoder = null;
            BitmapLoadTask task = new BitmapLoadTask(this, getContext(), this.bitmapDecoderFactory, this.uri, false);
            execute(task);
            return;
        }
        initialiseTileMap(maxTileDimensions);
        List<Tile> baseGrid = this.tileMap.get(Integer.valueOf(this.fullImageSampleSize));
        for (Tile baseTile : baseGrid) {
            TileLoadTask task2 = new TileLoadTask(this, this.decoder, baseTile);
            execute(task2);
        }
        refreshRequiredTiles(true);
    }

    protected void refreshRequiredTiles(boolean load) {
        if (this.decoder == null || this.tileMap == null) {
            return;
        }
        int sampleSize = Math.min(this.fullImageSampleSize, calculateInSampleSize(this.scale));
        for (Map.Entry<Integer, List<Tile>> tileMapEntry : this.tileMap.entrySet()) {
            for (Tile tile : tileMapEntry.getValue()) {
                if (tile.sampleSize < sampleSize || (tile.sampleSize > sampleSize && tile.sampleSize != this.fullImageSampleSize)) {
                    tile.visible = false;
                    if (tile.bitmap != null) {
                        tile.bitmap.recycle();
                        tile.bitmap = null;
                    }
                }
                if (tile.sampleSize == sampleSize) {
                    if (tileVisible(tile)) {
                        tile.visible = true;
                        if (!tile.loading && tile.bitmap == null && load) {
                            TileLoadTask task = new TileLoadTask(this, this.decoder, tile);
                            execute(task);
                        }
                    } else if (tile.sampleSize != this.fullImageSampleSize) {
                        tile.visible = false;
                        if (tile.bitmap != null) {
                            tile.bitmap.recycle();
                            tile.bitmap = null;
                        }
                    }
                } else if (tile.sampleSize == this.fullImageSampleSize) {
                    tile.visible = true;
                }
            }
        }
    }

    protected boolean tileVisible(Tile tile) {
        float sVisLeft = viewToSourceX(0.0f);
        float sVisRight = viewToSourceX(getWidth());
        float sVisTop = viewToSourceY(0.0f);
        float sVisBottom = viewToSourceY(getHeight());
        return sVisLeft <= ((float) tile.sRect.right) && ((float) tile.sRect.left) <= sVisRight && sVisTop <= ((float) tile.sRect.bottom) && ((float) tile.sRect.top) <= sVisBottom;
    }

    private void preDraw() {
        if (getWidth() == 0 || getHeight() == 0 || this.sWidth <= 0 || this.sHeight <= 0) {
            return;
        }
        if (this.sPendingCenter != null && this.pendingScale != null) {
            this.scale = this.pendingScale.floatValue();
            if (this.vTranslate == null) {
                this.vTranslate = new PointF();
            }
            this.vTranslate.x = (getWidth() / 2) - (this.scale * this.sPendingCenter.x);
            this.vTranslate.y = (getHeight() / 2) - (this.scale * this.sPendingCenter.y);
            this.sPendingCenter = null;
            this.pendingScale = null;
            fitToBounds(true);
            refreshRequiredTiles(true);
        }
        fitToBounds(false);
    }

    private int calculateInSampleSize(float scale) {
        if (this.minimumTileDpi > 0) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            float averageDpi = (metrics.xdpi + metrics.ydpi) / 2.0f;
            scale = (this.minimumTileDpi / averageDpi) * scale;
        }
        int reqWidth = (int) (sWidth() * scale);
        int reqHeight = (int) (sHeight() * scale);
        int inSampleSize = 1;
        if (reqWidth == 0 || reqHeight == 0) {
            return 32;
        }
        if (sHeight() > reqHeight || sWidth() > reqWidth) {
            int heightRatio = Math.round(sHeight() / reqHeight);
            int widthRatio = Math.round(sWidth() / reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        int i = 1;
        while (true) {
            int power = i;
            if (power * 2 < inSampleSize) {
                i = power * 2;
            } else {
                return power;
            }
        }
    }

    private void fitToBounds(boolean center, ScaleAndTranslate sat) {
        float maxTx;
        float maxTy;
        if (this.panLimit == 2 && isReady()) {
            center = false;
        }
        PointF vTranslate = sat.vTranslate;
        float scale = limitedScale(sat.scale);
        float scaleWidth = scale * sWidth();
        float scaleHeight = scale * sHeight();
        if (this.panLimit == 3 && isReady()) {
            vTranslate.x = Math.max(vTranslate.x, (getWidth() / 2) - scaleWidth);
            vTranslate.y = Math.max(vTranslate.y, (getHeight() / 2) - scaleHeight);
        } else if (center) {
            vTranslate.x = Math.max(vTranslate.x, getWidth() - scaleWidth);
            vTranslate.y = Math.max(vTranslate.y, getHeight() - scaleHeight);
        } else {
            vTranslate.x = Math.max(vTranslate.x, -scaleWidth);
            vTranslate.y = Math.max(vTranslate.y, -scaleHeight);
        }
        float xPaddingRatio = (getPaddingLeft() > 0 || getPaddingRight() > 0) ? getPaddingLeft() / (getPaddingLeft() + getPaddingRight()) : 0.5f;
        float yPaddingRatio = (getPaddingTop() > 0 || getPaddingBottom() > 0) ? getPaddingTop() / (getPaddingTop() + getPaddingBottom()) : 0.5f;
        if (this.panLimit == 3 && isReady()) {
            maxTx = Math.max(0, getWidth() / 2);
            maxTy = Math.max(0, getHeight() / 2);
        } else if (center) {
            maxTx = Math.max(0.0f, (getWidth() - scaleWidth) * xPaddingRatio);
            maxTy = Math.max(0.0f, (getHeight() - scaleHeight) * yPaddingRatio);
        } else {
            maxTx = Math.max(0, getWidth());
            maxTy = Math.max(0, getHeight());
        }
        vTranslate.x = Math.min(vTranslate.x, maxTx);
        vTranslate.y = Math.min(vTranslate.y, maxTy);
        sat.scale = scale;
    }

    private void fitToBounds(boolean center) {
        boolean init = false;
        if (this.vTranslate == null) {
            init = true;
            this.vTranslate = new PointF(0.0f, 0.0f);
        }
        if (this.satTemp == null) {
            this.satTemp = new ScaleAndTranslate(0.0f, new PointF(0.0f, 0.0f));
        }
        this.satTemp.scale = this.scale;
        this.satTemp.vTranslate.set(this.vTranslate);
        fitToBounds(center, this.satTemp);
        this.scale = this.satTemp.scale;
        this.vTranslate.set(this.satTemp.vTranslate);
        if (init && this.minimumScaleType != 4) {
            this.vTranslate.set(vTranslateForSCenter(sWidth() / 2, sHeight() / 2, this.scale));
        }
    }

    private void initialiseTileMap(Point maxTileDimensions) {
        debug("initialiseTileMap maxTileDimensions=%dx%d", Integer.valueOf(maxTileDimensions.x), Integer.valueOf(maxTileDimensions.y));
        this.tileMap = new LinkedHashMap();
        int sampleSize = this.fullImageSampleSize;
        int xTiles = 1;
        int yTiles = 1;
        while (true) {
            int sTileWidth = sWidth() / xTiles;
            int sTileHeight = sHeight() / yTiles;
            int subTileWidth = sTileWidth / sampleSize;
            int subTileHeight = sTileHeight / sampleSize;
            while (true) {
                if (subTileWidth + xTiles + 1 <= maxTileDimensions.x && (subTileWidth <= ((double) getWidth()) * 1.25d || sampleSize >= this.fullImageSampleSize)) {
                    break;
                }
                xTiles++;
                sTileWidth = sWidth() / xTiles;
                subTileWidth = sTileWidth / sampleSize;
            }
            while (true) {
                if (subTileHeight + yTiles + 1 <= maxTileDimensions.y && (subTileHeight <= ((double) getHeight()) * 1.25d || sampleSize >= this.fullImageSampleSize)) {
                    break;
                }
                yTiles++;
                sTileHeight = sHeight() / yTiles;
                subTileHeight = sTileHeight / sampleSize;
            }
            List<Tile> tileGrid = new ArrayList<>(xTiles * yTiles);
            int x = 0;
            while (x < xTiles) {
                int y = 0;
                while (y < yTiles) {
                    Tile tile = new Tile();
                    tile.sampleSize = sampleSize;
                    tile.visible = sampleSize == this.fullImageSampleSize;
                    tile.sRect = new Rect(x * sTileWidth, y * sTileHeight, x == xTiles - 1 ? sWidth() : (x + 1) * sTileWidth, y == yTiles - 1 ? sHeight() : (y + 1) * sTileHeight);
                    tile.vRect = new Rect(0, 0, 0, 0);
                    tile.fileSRect = new Rect(tile.sRect);
                    tileGrid.add(tile);
                    y++;
                }
                x++;
            }
            this.tileMap.put(Integer.valueOf(sampleSize), tileGrid);
            if (sampleSize != 1) {
                sampleSize /= 2;
            } else {
                return;
            }
        }
    }
    private static class TilesInitTask extends AsyncTask<Void, Void, int[]> {
        private final WeakReference<SubsamplingScaleImageView> viewRef;
        private final WeakReference<Context> contextRef;
        private final WeakReference<DecoderFactory<? extends ImageRegionDecoder>> decoderFactoryRef;
        private final Uri source;
        private ImageRegionDecoder decoder;
        private Exception exception;

        TilesInitTask(SubsamplingScaleImageView view, Context context, DecoderFactory<? extends ImageRegionDecoder> decoderFactory, Uri source) {
            this.viewRef = new WeakReference<>(view);
            this.contextRef = new WeakReference<>(context);
            this.decoderFactoryRef = new WeakReference<>(decoderFactory);
            this.source = source;
        }
        @Override // android.os.AsyncTask
        public int[] doInBackground(Void... params) {
            try {
                String sourceUri = this.source.toString();
                Context context = this.contextRef.get();
                DecoderFactory<? extends ImageRegionDecoder> decoderFactory = this.decoderFactoryRef.get();
                SubsamplingScaleImageView view = this.viewRef.get();
                if (context != null && decoderFactory != null && view != null) {
                    view.debug("TilesInitTask.doInBackground", new Object[0]);
                    this.decoder = decoderFactory.make();
                    Point dimensions = this.decoder.init(context, this.source);
                    int sWidth = dimensions.x;
                    int sHeight = dimensions.y;
                    int exifOrientation = view.getExifOrientation(context, sourceUri);
                    if (view.sRegion != null) {
                        view.sRegion.left = Math.max(0, view.sRegion.left);
                        view.sRegion.top = Math.max(0, view.sRegion.top);
                        view.sRegion.right = Math.min(sWidth, view.sRegion.right);
                        view.sRegion.bottom = Math.min(sHeight, view.sRegion.bottom);
                        sWidth = view.sRegion.width();
                        sHeight = view.sRegion.height();
                    }
                    return new int[]{sWidth, sHeight, exifOrientation};
                }
                return null;
            } catch (Exception e) {
                Log.e(SubsamplingScaleImageView.TAG, "Failed to initialise bitmap decoder", e);
                this.exception = e;
                return null;
            }
        }
        @Override // android.os.AsyncTask
        public void onPostExecute(int[] xyo) {
            SubsamplingScaleImageView view = this.viewRef.get();
            if (view != null) {
                if (this.decoder != null && xyo != null && xyo.length == 3) {
                    view.onTilesInited(this.decoder, xyo[0], xyo[1], xyo[2]);
                } else if (this.exception != null && view.onImageEventListener != null) {
                    view.onImageEventListener.onImageLoadError(this.exception);
                }
            }
        }
    }

    private synchronized void onTilesInited(ImageRegionDecoder decoder, int sWidth, int sHeight, int sOrientation) {
        debug("onTilesInited sWidth=%d, sHeight=%d, sOrientation=%d", Integer.valueOf(sWidth), Integer.valueOf(sHeight), Integer.valueOf(this.orientation));
        if (this.sWidth > 0 && this.sHeight > 0 && (this.sWidth != sWidth || this.sHeight != sHeight)) {
            reset(false);
            if (this.bitmap != null) {
                if (!this.bitmapIsCached) {
                    this.bitmap.recycle();
                }
                this.bitmap = null;
                if (this.onImageEventListener != null && this.bitmapIsCached) {
                    this.onImageEventListener.onPreviewReleased();
                }
                this.bitmapIsPreview = false;
                this.bitmapIsCached = false;
            }
        }
        this.decoder = decoder;
        this.sWidth = sWidth;
        this.sHeight = sHeight;
        this.sOrientation = sOrientation;
        checkReady();
        if (!checkImageLoaded() && this.maxTileWidth > 0 && this.maxTileWidth != Integer.MAX_VALUE && this.maxTileHeight > 0 && this.maxTileHeight != Integer.MAX_VALUE && getWidth() > 0 && getHeight() > 0) {
            initialiseBaseLayer(new Point(this.maxTileWidth, this.maxTileHeight));
        }
        invalidate();
        requestLayout();
    }
    private static class TileLoadTask extends AsyncTask<Void, Void, Bitmap> {
        private final WeakReference<SubsamplingScaleImageView> viewRef;
        private final WeakReference<ImageRegionDecoder> decoderRef;
        private final WeakReference<Tile> tileRef;
        private Exception exception;

        TileLoadTask(SubsamplingScaleImageView view, ImageRegionDecoder decoder, Tile tile) {
            this.viewRef = new WeakReference<>(view);
            this.decoderRef = new WeakReference<>(decoder);
            this.tileRef = new WeakReference<>(tile);
            tile.loading = true;
        }
        @Override // android.os.AsyncTask
        public Bitmap doInBackground(Void... params) {
            try {
                SubsamplingScaleImageView view = this.viewRef.get();
                ImageRegionDecoder decoder = this.decoderRef.get();
                Tile tile = this.tileRef.get();
                if (decoder != null && tile != null && view != null && decoder.isReady() && tile.visible) {
                    view.debug("TileLoadTask.doInBackground, tile.sRect=%s, tile.sampleSize=%d", tile.sRect, Integer.valueOf(tile.sampleSize));
                    view.decoderLock.readLock().lock();
                    try {
                        if (decoder.isReady()) {
                            view.fileSRect(tile.sRect, tile.fileSRect);
                            if (view.sRegion != null) {
                                tile.fileSRect.offset(view.sRegion.left, view.sRegion.top);
                            }
                            Bitmap bitmapDecodeRegion = decoder.decodeRegion(tile.fileSRect, tile.sampleSize);
                            view.decoderLock.readLock().unlock();
                            return bitmapDecodeRegion;
                        }
                        tile.loading = false;
                        view.decoderLock.readLock().unlock();
                    } catch (Throwable th) {
                        view.decoderLock.readLock().unlock();
                        throw th;
                    }
                } else if (tile != null) {
                    tile.loading = false;
                }
                return null;
            } catch (Exception e) {
                Log.e(SubsamplingScaleImageView.TAG, "Failed to decode tile", e);
                this.exception = e;
                return null;
            } catch (OutOfMemoryError e2) {
                Log.e(SubsamplingScaleImageView.TAG, "Failed to decode tile - OutOfMemoryError", e2);
                this.exception = new RuntimeException(e2);
                return null;
            }
        }
        @Override // android.os.AsyncTask
        public void onPostExecute(Bitmap bitmap) {
            SubsamplingScaleImageView subsamplingScaleImageView = this.viewRef.get();
            Tile tile = this.tileRef.get();
            if (subsamplingScaleImageView != null && tile != null) {
                if (bitmap != null) {
                    tile.bitmap = bitmap;
                    tile.loading = false;
                    subsamplingScaleImageView.onTileLoaded();
                } else if (this.exception != null && subsamplingScaleImageView.onImageEventListener != null) {
                    subsamplingScaleImageView.onImageEventListener.onTileLoadError(this.exception);
                }
            }
        }
    }

    private synchronized void onTileLoaded() {
        debug("onTileLoaded", new Object[0]);
        checkReady();
        checkImageLoaded();
        if (isBaseLayerReady() && this.bitmap != null) {
            if (!this.bitmapIsCached) {
                this.bitmap.recycle();
            }
            this.bitmap = null;
            if (this.onImageEventListener != null && this.bitmapIsCached) {
                this.onImageEventListener.onPreviewReleased();
            }
            this.bitmapIsPreview = false;
            this.bitmapIsCached = false;
        }
        invalidate();
    }
    private static class BitmapLoadTask extends AsyncTask<Void, Void, Integer> {
        private final WeakReference<SubsamplingScaleImageView> viewRef;
        private final WeakReference<Context> contextRef;
        private final WeakReference<DecoderFactory<? extends ImageDecoder>> decoderFactoryRef;
        private final Uri source;
        private final boolean preview;
        private Bitmap bitmap;
        private Exception exception;

        BitmapLoadTask(SubsamplingScaleImageView view, Context context, DecoderFactory<? extends ImageDecoder> decoderFactory, Uri source, boolean preview) {
            this.viewRef = new WeakReference<>(view);
            this.contextRef = new WeakReference<>(context);
            this.decoderFactoryRef = new WeakReference<>(decoderFactory);
            this.source = source;
            this.preview = preview;
        }
        @Override // android.os.AsyncTask
        public Integer doInBackground(Void... params) {
            try {
                String sourceUri = this.source.toString();
                Context context = this.contextRef.get();
                DecoderFactory<? extends ImageDecoder> decoderFactory = this.decoderFactoryRef.get();
                SubsamplingScaleImageView view = this.viewRef.get();
                if (context != null && decoderFactory != null && view != null) {
                    view.debug("BitmapLoadTask.doInBackground", new Object[0]);
                    this.bitmap = decoderFactory.make().decode(context, this.source);
                    return Integer.valueOf(view.getExifOrientation(context, sourceUri));
                }
                return null;
            } catch (Exception e) {
                Log.e(SubsamplingScaleImageView.TAG, "Failed to load bitmap", e);
                this.exception = e;
                return null;
            } catch (OutOfMemoryError e2) {
                Log.e(SubsamplingScaleImageView.TAG, "Failed to load bitmap - OutOfMemoryError", e2);
                this.exception = new RuntimeException(e2);
                return null;
            }
        }
        @Override // android.os.AsyncTask
        public void onPostExecute(Integer orientation) {
            SubsamplingScaleImageView subsamplingScaleImageView = this.viewRef.get();
            if (subsamplingScaleImageView != null) {
                if (this.bitmap != null && orientation != null) {
                    if (this.preview) {
                        subsamplingScaleImageView.onPreviewLoaded(this.bitmap);
                        return;
                    } else {
                        subsamplingScaleImageView.onImageLoaded(this.bitmap, orientation.intValue(), false);
                        return;
                    }
                }
                if (this.exception != null && subsamplingScaleImageView.onImageEventListener != null) {
                    if (this.preview) {
                        subsamplingScaleImageView.onImageEventListener.onPreviewLoadError(this.exception);
                    } else {
                        subsamplingScaleImageView.onImageEventListener.onImageLoadError(this.exception);
                    }
                }
            }
        }
    }

    private synchronized void onPreviewLoaded(Bitmap previewBitmap) {
        debug("onPreviewLoaded", new Object[0]);
        if (this.bitmap != null || this.imageLoadedSent) {
            previewBitmap.recycle();
            return;
        }
        if (this.pRegion != null) {
            this.bitmap = Bitmap.createBitmap(previewBitmap, this.pRegion.left, this.pRegion.top, this.pRegion.width(), this.pRegion.height());
        } else {
            this.bitmap = previewBitmap;
        }
        this.bitmapIsPreview = true;
        if (checkReady()) {
            invalidate();
            requestLayout();
        }
    }

    private synchronized void onImageLoaded(Bitmap bitmap, int sOrientation, boolean bitmapIsCached) {
        debug("onImageLoaded", new Object[0]);
        if (this.sWidth > 0 && this.sHeight > 0 && (this.sWidth != bitmap.getWidth() || this.sHeight != bitmap.getHeight())) {
            reset(false);
        }
        if (this.bitmap != null && !this.bitmapIsCached) {
            this.bitmap.recycle();
        }
        if (this.bitmap != null && this.bitmapIsCached && this.onImageEventListener != null) {
            this.onImageEventListener.onPreviewReleased();
        }
        this.bitmapIsPreview = false;
        this.bitmapIsCached = bitmapIsCached;
        this.bitmap = bitmap;
        this.sWidth = bitmap.getWidth();
        this.sHeight = bitmap.getHeight();
        this.sOrientation = sOrientation;
        boolean ready = checkReady();
        boolean imageLoaded = checkImageLoaded();
        if (ready || imageLoaded) {
            invalidate();
            requestLayout();
        }
    }

    @AnyThread
    private int getExifOrientation(Context context, String sourceUri) {
        int exifOrientation = 0;
        if (sourceUri.startsWith("content")) {
            Cursor cursor = null;
            try {
                try {
                    String[] columns = {"orientation"};
                    cursor = context.getContentResolver().query(Uri.parse(sourceUri), columns, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int orientation = cursor.getInt(0);
                        if (VALID_ORIENTATIONS.contains(Integer.valueOf(orientation)) && orientation != -1) {
                            exifOrientation = orientation;
                        } else {
                            Log.w(TAG, "Unsupported orientation: " + orientation);
                        }
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not get orientation of image from media store");
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
                throw th;
            }
        } else if (sourceUri.startsWith("file:///") && !sourceUri.startsWith("file:///android_asset/")) {
            try {
                ExifInterface exifInterface = new ExifInterface(sourceUri.substring("file:///".length() - 1));
                int orientationAttr = exifInterface.getAttributeInt("Orientation", 1);
                if (orientationAttr == 1 || orientationAttr == 0) {
                    exifOrientation = 0;
                } else if (orientationAttr == 6) {
                    exifOrientation = 90;
                } else if (orientationAttr == 3) {
                    exifOrientation = 180;
                } else if (orientationAttr == 8) {
                    exifOrientation = 270;
                } else {
                    Log.w(TAG, "Unsupported EXIF orientation: " + orientationAttr);
                }
            } catch (Exception e2) {
                Log.w(TAG, "Could not get EXIF orientation of image");
            }
        }
        return exifOrientation;
    }

    private void execute(AsyncTask<Void, Void, ?> asyncTask) {
        asyncTask.executeOnExecutor(this.executor, new Void[0]);
    }
    protected static class Tile {
        public Rect sRect;
        public int sampleSize;
        public Bitmap bitmap;
        public boolean loading;
        public boolean visible;
        public Rect vRect;
        public Rect fileSRect;

        protected Tile() {
        }
    }
    private static class Anim {
        private float scaleStart;
        private float scaleEnd;
        private PointF sCenterStart;
        private PointF sCenterEnd;
        private PointF sCenterEndRequested;
        private PointF vFocusStart;
        private PointF vFocusEnd;
        private long duration = 500;
        private boolean interruptible = true;
        private int easing = 2;
        private int origin = 1;
        private long time = System.currentTimeMillis();
        private OnAnimationEventListener listener;

        private Anim() {
        }
    }
    private static class ScaleAndTranslate {
        private float scale;
        private final PointF vTranslate;

        private ScaleAndTranslate(float scale, PointF vTranslate) {
            this.scale = scale;
            this.vTranslate = vTranslate;
        }
    }

    private void restoreState(ImageViewState state) {
        if (state != null && VALID_ORIENTATIONS.contains(Integer.valueOf(state.getOrientation()))) {
            this.orientation = state.getOrientation();
            this.pendingScale = Float.valueOf(state.getScale());
            this.sPendingCenter = state.getCenter();
            invalidate();
        }
    }

    public void setMaxTileSize(int maxPixels) {
        this.maxTileWidth = maxPixels;
        this.maxTileHeight = maxPixels;
    }

    public void setMaxTileSize(int maxPixelsX, int maxPixelsY) {
        this.maxTileWidth = maxPixelsX;
        this.maxTileHeight = maxPixelsY;
    }

    @NonNull
    private Point getMaxBitmapDimensions(Canvas canvas) {
        return new Point(Math.min(canvas.getMaximumBitmapWidth(), this.maxTileWidth), Math.min(canvas.getMaximumBitmapHeight(), this.maxTileHeight));
    }

    private int sWidth() {
        int rotation = getRequiredRotation();
        if (rotation == 90 || rotation == 270) {
            return this.sHeight;
        }
        return this.sWidth;
    }

    private int sHeight() {
        int rotation = getRequiredRotation();
        if (rotation == 90 || rotation == 270) {
            return this.sWidth;
        }
        return this.sHeight;
    }

    @AnyThread
    private void fileSRect(Rect sRect, Rect target) {
        if (getRequiredRotation() == 0) {
            target.set(sRect);
            return;
        }
        if (getRequiredRotation() == 90) {
            target.set(sRect.top, this.sHeight - sRect.right, sRect.bottom, this.sHeight - sRect.left);
        } else if (getRequiredRotation() == 180) {
            target.set(this.sWidth - sRect.right, this.sHeight - sRect.bottom, this.sWidth - sRect.left, this.sHeight - sRect.top);
        } else {
            target.set(this.sWidth - sRect.bottom, sRect.left, this.sWidth - sRect.top, sRect.right);
        }
    }

    @AnyThread
    private int getRequiredRotation() {
        if (this.orientation == -1) {
            return this.sOrientation;
        }
        return this.orientation;
    }

    private float distance(float x0, float x1, float y0, float y1) {
        float x = x0 - x1;
        float y = y0 - y1;
        return (float) Math.sqrt((x * x) + (y * y));
    }

    public void recycle() {
        reset(true);
        this.bitmapPaint = null;
        this.debugTextPaint = null;
        this.debugLinePaint = null;
        this.tileBgPaint = null;
    }

    protected float viewToSourceX(float vx) {
        if (this.vTranslate == null) {
            return Float.NaN;
        }
        return (vx - this.vTranslate.x) / this.scale;
    }

    protected float viewToSourceY(float vy) {
        if (this.vTranslate == null) {
            return Float.NaN;
        }
        return (vy - this.vTranslate.y) / this.scale;
    }

    public void viewToFileRect(Rect vRect, Rect fRect) {
        if (this.vTranslate == null || !this.readySent) {
            return;
        }
        fRect.set((int) viewToSourceX(vRect.left), (int) viewToSourceY(vRect.top), (int) viewToSourceX(vRect.right), (int) viewToSourceY(vRect.bottom));
        fileSRect(fRect, fRect);
        fRect.set(Math.max(0, fRect.left), Math.max(0, fRect.top), Math.min(this.sWidth, fRect.right), Math.min(this.sHeight, fRect.bottom));
        if (this.sRegion != null) {
            fRect.offset(this.sRegion.left, this.sRegion.top);
        }
    }

    public void visibleFileRect(Rect fRect) {
        if (this.vTranslate == null || !this.readySent) {
            return;
        }
        fRect.set(0, 0, getWidth(), getHeight());
        viewToFileRect(fRect, fRect);
    }

    @Nullable
    public final PointF viewToSourceCoord(PointF vxy) {
        return viewToSourceCoord(vxy.x, vxy.y, new PointF());
    }

    @Nullable
    public final PointF viewToSourceCoord(float vx, float vy) {
        return viewToSourceCoord(vx, vy, new PointF());
    }

    @Nullable
    public final PointF viewToSourceCoord(PointF vxy, @NonNull PointF sTarget) {
        return viewToSourceCoord(vxy.x, vxy.y, sTarget);
    }

    @Nullable
    public final PointF viewToSourceCoord(float vx, float vy, @NonNull PointF sTarget) {
        if (this.vTranslate == null) {
            return null;
        }
        sTarget.set(viewToSourceX(vx), viewToSourceY(vy));
        return sTarget;
    }

    protected float sourceToViewX(float sx) {
        if (this.vTranslate == null) {
            return Float.NaN;
        }
        return (sx * this.scale) + this.vTranslate.x;
    }

    protected float sourceToViewY(float sy) {
        if (this.vTranslate == null) {
            return Float.NaN;
        }
        return (sy * this.scale) + this.vTranslate.y;
    }

    @Nullable
    public final PointF sourceToViewCoord(PointF sxy) {
        return sourceToViewCoord(sxy.x, sxy.y, new PointF());
    }

    @Nullable
    public final PointF sourceToViewCoord(float sx, float sy) {
        return sourceToViewCoord(sx, sy, new PointF());
    }

    @Nullable
    public final PointF sourceToViewCoord(PointF sxy, @NonNull PointF vTarget) {
        return sourceToViewCoord(sxy.x, sxy.y, vTarget);
    }

    @Nullable
    public final PointF sourceToViewCoord(float sx, float sy, @NonNull PointF vTarget) {
        if (this.vTranslate == null) {
            return null;
        }
        vTarget.set(sourceToViewX(sx), sourceToViewY(sy));
        return vTarget;
    }

    private void sourceToViewRect(@NonNull Rect sRect, @NonNull Rect vTarget) {
        vTarget.set((int) sourceToViewX(sRect.left), (int) sourceToViewY(sRect.top), (int) sourceToViewX(sRect.right), (int) sourceToViewY(sRect.bottom));
    }

    @NonNull
    private PointF vTranslateForSCenter(float sCenterX, float sCenterY, float scale) {
        int vxCenter = getPaddingLeft() + (((getWidth() - getPaddingRight()) - getPaddingLeft()) / 2);
        int vyCenter = getPaddingTop() + (((getHeight() - getPaddingBottom()) - getPaddingTop()) / 2);
        if (this.satTemp == null) {
            this.satTemp = new ScaleAndTranslate(0.0f, new PointF(0.0f, 0.0f));
        }
        this.satTemp.scale = scale;
        this.satTemp.vTranslate.set(vxCenter - (sCenterX * scale), vyCenter - (sCenterY * scale));
        fitToBounds(true, this.satTemp);
        return this.satTemp.vTranslate;
    }

    @NonNull
    private PointF limitedSCenter(float sCenterX, float sCenterY, float scale, @NonNull PointF sTarget) {
        PointF vTranslate = vTranslateForSCenter(sCenterX, sCenterY, scale);
        int vxCenter = getPaddingLeft() + (((getWidth() - getPaddingRight()) - getPaddingLeft()) / 2);
        int vyCenter = getPaddingTop() + (((getHeight() - getPaddingBottom()) - getPaddingTop()) / 2);
        float sx = (vxCenter - vTranslate.x) / scale;
        float sy = (vyCenter - vTranslate.y) / scale;
        sTarget.set(sx, sy);
        return sTarget;
    }

    private float minScale() {
        int vPadding = getPaddingBottom() + getPaddingTop();
        int hPadding = getPaddingLeft() + getPaddingRight();
        if (this.minimumScaleType == 2 || this.minimumScaleType == 4) {
            return Math.max((getWidth() - hPadding) / sWidth(), (getHeight() - vPadding) / sHeight());
        }
        if (this.minimumScaleType == 3 && this.minScale > 0.0f) {
            return this.minScale;
        }
        return Math.min((getWidth() - hPadding) / sWidth(), (getHeight() - vPadding) / sHeight());
    }

    private float limitedScale(float targetScale) {
        return Math.min(this.maxScale, Math.max(minScale(), targetScale));
    }

    private float ease(int type, long time, float from, float change, long duration) {
        switch (type) {
            case 1:
                return easeOutQuad(time, from, change, duration);
            case 2:
                return easeInOutQuad(time, from, change, duration);
            default:
                throw new IllegalStateException("Unexpected easing type: " + type);
        }
    }

    private float easeOutQuad(long time, float from, float change, long duration) {
        float progress = time / duration;
        return ((-change) * progress * (progress - 2.0f)) + from;
    }

    private float easeInOutQuad(long time, float from, float change, long duration) {
        float timeF = time / (duration / 2.0f);
        if (timeF < 1.0f) {
            return ((change / 2.0f) * timeF * timeF) + from;
        }
        float timeF2 = timeF - 1.0f;
        return (((-change) / 2.0f) * ((timeF2 * (timeF2 - 2.0f)) - 1.0f)) + from;
    }

    @AnyThread
    private void debug(String message, Object... args) {
        if (this.debug) {
            Log.d(TAG, String.format(message, args));
        }
    }

    private int px(int px) {
        return (int) (this.density * px);
    }

    public final void setRegionDecoderClass(@NonNull Class<? extends ImageRegionDecoder> regionDecoderClass) {
        if (regionDecoderClass == null) {
            throw new IllegalArgumentException("Decoder class cannot be set to null");
        }
        this.regionDecoderFactory = new CompatDecoderFactory(regionDecoderClass);
    }

    public final void setRegionDecoderFactory(@NonNull DecoderFactory<? extends ImageRegionDecoder> regionDecoderFactory) {
        if (regionDecoderFactory == null) {
            throw new IllegalArgumentException("Decoder factory cannot be set to null");
        }
        this.regionDecoderFactory = regionDecoderFactory;
    }

    public final void setBitmapDecoderClass(@NonNull Class<? extends ImageDecoder> bitmapDecoderClass) {
        if (bitmapDecoderClass == null) {
            throw new IllegalArgumentException("Decoder class cannot be set to null");
        }
        this.bitmapDecoderFactory = new CompatDecoderFactory(bitmapDecoderClass);
    }

    public final void setBitmapDecoderFactory(@NonNull DecoderFactory<? extends ImageDecoder> bitmapDecoderFactory) {
        if (bitmapDecoderFactory == null) {
            throw new IllegalArgumentException("Decoder factory cannot be set to null");
        }
        this.bitmapDecoderFactory = bitmapDecoderFactory;
    }

    public final void getPanRemaining(RectF vTarget) {
        if (!isReady()) {
            return;
        }
        float scaleWidth = this.scale * sWidth();
        float scaleHeight = this.scale * sHeight();
        if (this.panLimit == 3) {
            vTarget.top = Math.max(0.0f, -(this.vTranslate.y - (getHeight() / 2)));
            vTarget.left = Math.max(0.0f, -(this.vTranslate.x - (getWidth() / 2)));
            vTarget.bottom = Math.max(0.0f, this.vTranslate.y - ((getHeight() / 2) - scaleHeight));
            vTarget.right = Math.max(0.0f, this.vTranslate.x - ((getWidth() / 2) - scaleWidth));
            return;
        }
        if (this.panLimit == 2) {
            vTarget.top = Math.max(0.0f, -(this.vTranslate.y - getHeight()));
            vTarget.left = Math.max(0.0f, -(this.vTranslate.x - getWidth()));
            vTarget.bottom = Math.max(0.0f, this.vTranslate.y + scaleHeight);
            vTarget.right = Math.max(0.0f, this.vTranslate.x + scaleWidth);
            return;
        }
        vTarget.top = Math.max(0.0f, -this.vTranslate.y);
        vTarget.left = Math.max(0.0f, -this.vTranslate.x);
        vTarget.bottom = Math.max(0.0f, (scaleHeight + this.vTranslate.y) - getHeight());
        vTarget.right = Math.max(0.0f, (scaleWidth + this.vTranslate.x) - getWidth());
    }

    public final void setPanLimit(int panLimit) {
        if (!VALID_PAN_LIMITS.contains(Integer.valueOf(panLimit))) {
            throw new IllegalArgumentException("Invalid pan limit: " + panLimit);
        }
        this.panLimit = panLimit;
        if (isReady()) {
            fitToBounds(true);
            invalidate();
        }
    }

    public final void setMinimumScaleType(int scaleType) {
        if (!VALID_SCALE_TYPES.contains(Integer.valueOf(scaleType))) {
            throw new IllegalArgumentException("Invalid scale type: " + scaleType);
        }
        this.minimumScaleType = scaleType;
        if (isReady()) {
            fitToBounds(true);
            invalidate();
        }
    }

    public final void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
    }

    public final void setMinScale(float minScale) {
        this.minScale = minScale;
    }

    public final void setMinimumDpi(int dpi) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float averageDpi = (metrics.xdpi + metrics.ydpi) / 2.0f;
        setMaxScale(averageDpi / dpi);
    }

    public final void setMaximumDpi(int dpi) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float averageDpi = (metrics.xdpi + metrics.ydpi) / 2.0f;
        setMinScale(averageDpi / dpi);
    }

    public float getMaxScale() {
        return this.maxScale;
    }

    public final float getMinScale() {
        return minScale();
    }

    public void setMinimumTileDpi(int minimumTileDpi) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float averageDpi = (metrics.xdpi + metrics.ydpi) / 2.0f;
        this.minimumTileDpi = (int) Math.min(averageDpi, minimumTileDpi);
        if (isReady()) {
            reset(false);
            invalidate();
        }
    }

    @Nullable
    public final PointF getCenter() {
        int mX = getWidth() / 2;
        int mY = getHeight() / 2;
        return viewToSourceCoord(mX, mY);
    }

    public final float getScale() {
        return this.scale;
    }

    public final void setScaleAndCenter(float scale, @Nullable PointF sCenter) {
        this.anim = null;
        this.pendingScale = Float.valueOf(scale);
        this.sPendingCenter = sCenter;
        this.sRequestedCenter = sCenter;
        invalidate();
    }

    public final void requestScale(float scale) {
        this.anim = null;
        this.pendingScale = Float.valueOf(scale);
        if (this.sPendingCenter == null) {
            this.sPendingCenter = getCenter();
        }
        invalidate();
    }

    public final void requestCenter(@Nullable PointF sCenter) {
        this.anim = null;
        this.sPendingCenter = sCenter;
        this.sRequestedCenter = sCenter;
        if (this.pendingScale == null) {
            this.pendingScale = Float.valueOf(getScale());
        }
        invalidate();
    }

    public final void resetScaleAndCenter() {
        this.anim = null;
        this.pendingScale = Float.valueOf(limitedScale(0.0f));
        if (isReady()) {
            this.sPendingCenter = new PointF(sWidth() / 2, sHeight() / 2);
        } else {
            this.sPendingCenter = new PointF(0.0f, 0.0f);
        }
        invalidate();
    }

    public final boolean isReady() {
        return this.readySent;
    }

    protected void onReady() {
    }

    public final boolean isImageLoaded() {
        return this.imageLoadedSent;
    }

    protected void onImageLoaded() {
    }

    public final int getSWidth() {
        return this.sWidth;
    }

    public final int getSHeight() {
        return this.sHeight;
    }

    public final int getOrientation() {
        return this.orientation;
    }

    public final int getAppliedOrientation() {
        return getRequiredRotation();
    }

    @Nullable
    public final ImageViewState getState() {
        if (this.vTranslate != null && this.sWidth > 0 && this.sHeight > 0) {
            return new ImageViewState(getScale(), getCenter(), getOrientation());
        }
        return null;
    }

    public final boolean isZoomEnabled() {
        return this.zoomEnabled;
    }

    public final void setZoomEnabled(boolean zoomEnabled) {
        this.zoomEnabled = zoomEnabled;
    }

    public final boolean isQuickScaleEnabled() {
        return this.quickScaleEnabled;
    }

    public final void setQuickScaleEnabled(boolean quickScaleEnabled) {
        this.quickScaleEnabled = quickScaleEnabled;
    }

    public final boolean isPanEnabled() {
        return this.panEnabled;
    }

    public final void setPanEnabled(boolean panEnabled, boolean resetCenter) {
        this.panEnabled = panEnabled;
        if (resetCenter && !panEnabled && this.vTranslate != null) {
            this.vTranslate.x = (getWidth() / 2) - (this.scale * (sWidth() / 2));
            this.vTranslate.y = (getHeight() / 2) - (this.scale * (sHeight() / 2));
            if (isReady()) {
                refreshRequiredTiles(true);
                invalidate();
            }
        }
    }

    public final void setPanEnabled(boolean panEnabled) {
        setPanEnabled(panEnabled, true);
    }

    public final void setTileBackgroundColor(int tileBgColor) {
        if (Color.alpha(tileBgColor) == 0) {
            this.tileBgPaint = null;
        } else {
            this.tileBgPaint = new Paint();
            this.tileBgPaint.setStyle(Paint.Style.FILL);
            this.tileBgPaint.setColor(tileBgColor);
        }
        invalidate();
    }

    public final void setDoubleTapZoomScale(float doubleTapZoomScale) {
        this.doubleTapZoomScale = doubleTapZoomScale;
    }

    public final void setDoubleTapZoomDpi(int dpi) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float averageDpi = (metrics.xdpi + metrics.ydpi) / 2.0f;
        setDoubleTapZoomScale(averageDpi / dpi);
    }

    public final void setAlwaysZoomDoubleTap(boolean alwaysZoomDoubleTap) {
        this.alwaysZoomDoubleTap = alwaysZoomDoubleTap;
    }

    public final boolean getAlwaysZoomDoubleTap() {
        return this.alwaysZoomDoubleTap;
    }

    public final void setAlwaysZoomDoubleTapZoomScale(float alwaysZoomDoubleTapZoomScale) {
        this.doubleTapZoomBy = alwaysZoomDoubleTapZoomScale;
    }

    public final float getAlwaysZoomDoubleTapZoomScale() {
        return this.doubleTapZoomBy;
    }

    public final void setDoubleTapZoomStyle(int doubleTapZoomStyle) {
        if (!VALID_ZOOM_STYLES.contains(Integer.valueOf(doubleTapZoomStyle))) {
            throw new IllegalArgumentException("Invalid zoom style: " + doubleTapZoomStyle);
        }
        this.doubleTapZoomStyle = doubleTapZoomStyle;
    }

    public final void setDoubleTapZoomDuration(int durationMs) {
        this.doubleTapZoomDuration = Math.max(0, durationMs);
    }

    public void setExecutor(@NonNull Executor executor) {
        if (executor == null) {
            throw new NullPointerException("Executor must not be null");
        }
        this.executor = executor;
    }

    public void setEagerLoadingEnabled(boolean eagerLoadingEnabled) {
        this.eagerLoadingEnabled = eagerLoadingEnabled;
    }

    public final void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean hasImage() {
        return (this.uri == null && this.bitmap == null) ? false : true;
    }

    @Override // android.view.View
    public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
    }

    public void setOnImageEventListener(OnImageEventListener onImageEventListener) {
        this.onImageEventListener = onImageEventListener;
    }

    public void setOnStateChangedListener(OnStateChangedListener onStateChangedListener) {
        this.onStateChangedListener = onStateChangedListener;
    }

    private void sendStateChanged(float oldScale, PointF oldVTranslate, int origin) {
        if (this.onStateChangedListener != null && this.scale != oldScale) {
            this.onStateChangedListener.onScaleChanged(this.scale, origin);
        }
        if (this.onStateChangedListener != null && !this.vTranslate.equals(oldVTranslate)) {
            this.onStateChangedListener.onCenterChanged(getCenter(), origin);
        }
    }

    @Nullable
    public AnimationBuilder animateCenter(PointF sCenter) {
        if (!isReady()) {
            return null;
        }
        return new AnimationBuilder(sCenter);
    }

    @Nullable
    public AnimationBuilder animateScale(float scale) {
        if (!isReady()) {
            return null;
        }
        return new AnimationBuilder(scale);
    }

    @Nullable
    public AnimationBuilder animateScaleAndCenter(float scale, PointF sCenter) {
        if (!isReady()) {
            return null;
        }
        return new AnimationBuilder(scale, sCenter);
    }
    public final class AnimationBuilder {
        private final float targetScale;
        private final PointF targetSCenter;
        private final PointF vFocus;
        private long duration;
        private int easing;
        private int origin;
        private boolean interruptible;
        private boolean panLimited;
        private OnAnimationEventListener listener;

        private AnimationBuilder(PointF sCenter) {
            this.duration = 500L;
            this.easing = 2;
            this.origin = 1;
            this.interruptible = true;
            this.panLimited = true;
            this.targetScale = SubsamplingScaleImageView.this.scale;
            this.targetSCenter = sCenter;
            this.vFocus = null;
        }

        private AnimationBuilder(float scale) {
            this.duration = 500L;
            this.easing = 2;
            this.origin = 1;
            this.interruptible = true;
            this.panLimited = true;
            this.targetScale = scale;
            this.targetSCenter = SubsamplingScaleImageView.this.getCenter();
            this.vFocus = null;
        }

        private AnimationBuilder(float scale, PointF sCenter) {
            this.duration = 500L;
            this.easing = 2;
            this.origin = 1;
            this.interruptible = true;
            this.panLimited = true;
            this.targetScale = scale;
            this.targetSCenter = sCenter;
            this.vFocus = null;
        }

        private AnimationBuilder(float scale, PointF sCenter, PointF vFocus) {
            this.duration = 500L;
            this.easing = 2;
            this.origin = 1;
            this.interruptible = true;
            this.panLimited = true;
            this.targetScale = scale;
            this.targetSCenter = sCenter;
            this.vFocus = vFocus;
        }

        @NonNull
        public AnimationBuilder withDuration(long duration) {
            this.duration = duration;
            return this;
        }

        @NonNull
        public AnimationBuilder withInterruptible(boolean interruptible) {
            this.interruptible = interruptible;
            return this;
        }

        @NonNull
        public AnimationBuilder withEasing(int easing) {
            if (!SubsamplingScaleImageView.VALID_EASING_STYLES.contains(Integer.valueOf(easing))) {
                throw new IllegalArgumentException("Unknown easing type: " + easing);
            }
            this.easing = easing;
            return this;
        }

        @NonNull
        public AnimationBuilder withOnAnimationEventListener(OnAnimationEventListener listener) {
            this.listener = listener;
            return this;
        }

        @NonNull
        private AnimationBuilder withPanLimited(boolean panLimited) {
            this.panLimited = panLimited;
            return this;
        }

        @NonNull
        private AnimationBuilder withOrigin(int origin) {
            this.origin = origin;
            return this;
        }

        public void start() {
            if (SubsamplingScaleImageView.this.anim != null && SubsamplingScaleImageView.this.anim.listener != null) {
                try {
                    SubsamplingScaleImageView.this.anim.listener.onInterruptedByNewAnim();
                } catch (Exception e) {
                    Log.w(SubsamplingScaleImageView.TAG, "Error thrown by animation listener", e);
                }
            }
            int vxCenter = SubsamplingScaleImageView.this.getPaddingLeft() + (((SubsamplingScaleImageView.this.getWidth() - SubsamplingScaleImageView.this.getPaddingRight()) - SubsamplingScaleImageView.this.getPaddingLeft()) / 2);
            int vyCenter = SubsamplingScaleImageView.this.getPaddingTop() + (((SubsamplingScaleImageView.this.getHeight() - SubsamplingScaleImageView.this.getPaddingBottom()) - SubsamplingScaleImageView.this.getPaddingTop()) / 2);
            float targetScale = SubsamplingScaleImageView.this.limitedScale(this.targetScale);
            PointF targetSCenter = this.panLimited ? SubsamplingScaleImageView.this.limitedSCenter(this.targetSCenter.x, this.targetSCenter.y, targetScale, new PointF()) : this.targetSCenter;
            SubsamplingScaleImageView.this.anim = new Anim();
            SubsamplingScaleImageView.this.anim.scaleStart = SubsamplingScaleImageView.this.scale;
            SubsamplingScaleImageView.this.anim.scaleEnd = targetScale;
            SubsamplingScaleImageView.this.anim.time = System.currentTimeMillis();
            SubsamplingScaleImageView.this.anim.sCenterEndRequested = targetSCenter;
            SubsamplingScaleImageView.this.anim.sCenterStart = SubsamplingScaleImageView.this.getCenter();
            SubsamplingScaleImageView.this.anim.sCenterEnd = targetSCenter;
            SubsamplingScaleImageView.this.anim.vFocusStart = SubsamplingScaleImageView.this.sourceToViewCoord(targetSCenter);
            SubsamplingScaleImageView.this.anim.vFocusEnd = new PointF(vxCenter, vyCenter);
            SubsamplingScaleImageView.this.anim.duration = this.duration;
            SubsamplingScaleImageView.this.anim.interruptible = this.interruptible;
            SubsamplingScaleImageView.this.anim.easing = this.easing;
            SubsamplingScaleImageView.this.anim.origin = this.origin;
            SubsamplingScaleImageView.this.anim.time = System.currentTimeMillis();
            SubsamplingScaleImageView.this.anim.listener = this.listener;
            if (this.vFocus != null) {
                float vTranslateXEnd = this.vFocus.x - (targetScale * SubsamplingScaleImageView.this.anim.sCenterStart.x);
                float vTranslateYEnd = this.vFocus.y - (targetScale * SubsamplingScaleImageView.this.anim.sCenterStart.y);
                ScaleAndTranslate satEnd = new ScaleAndTranslate(targetScale, new PointF(vTranslateXEnd, vTranslateYEnd));
                SubsamplingScaleImageView.this.fitToBounds(true, satEnd);
                SubsamplingScaleImageView.this.anim.vFocusEnd = new PointF(this.vFocus.x + (satEnd.vTranslate.x - vTranslateXEnd), this.vFocus.y + (satEnd.vTranslate.y - vTranslateYEnd));
            }
            SubsamplingScaleImageView.this.invalidate();
        }
    }
    public static class DefaultOnAnimationEventListener implements OnAnimationEventListener {
        @Override // com.kylecorry.trail_sense.shared.views.subscaleview.SubsamplingScaleImageView.OnAnimationEventListener
        public void onComplete() {
        }

        @Override // com.kylecorry.trail_sense.shared.views.subscaleview.SubsamplingScaleImageView.OnAnimationEventListener
        public void onInterruptedByUser() {
        }

        @Override // com.kylecorry.trail_sense.shared.views.subscaleview.SubsamplingScaleImageView.OnAnimationEventListener
        public void onInterruptedByNewAnim() {
        }
    }
    public static class DefaultOnImageEventListener implements OnImageEventListener {
        @Override // com.kylecorry.trail_sense.shared.views.subscaleview.SubsamplingScaleImageView.OnImageEventListener
        public void onReady() {
        }

        @Override // com.kylecorry.trail_sense.shared.views.subscaleview.SubsamplingScaleImageView.OnImageEventListener
        public void onImageLoaded() {
        }

        @Override // com.kylecorry.trail_sense.shared.views.subscaleview.SubsamplingScaleImageView.OnImageEventListener
        public void onPreviewLoadError(Exception e) {
        }

        @Override // com.kylecorry.trail_sense.shared.views.subscaleview.SubsamplingScaleImageView.OnImageEventListener
        public void onImageLoadError(Exception e) {
        }

        @Override // com.kylecorry.trail_sense.shared.views.subscaleview.SubsamplingScaleImageView.OnImageEventListener
        public void onTileLoadError(Exception e) {
        }

        @Override // com.kylecorry.trail_sense.shared.views.subscaleview.SubsamplingScaleImageView.OnImageEventListener
        public void onPreviewReleased() {
        }
    }
    public static class DefaultOnStateChangedListener implements OnStateChangedListener {
        @Override // com.kylecorry.trail_sense.shared.views.subscaleview.SubsamplingScaleImageView.OnStateChangedListener
        public void onCenterChanged(PointF newCenter, int origin) {
        }

        @Override // com.kylecorry.trail_sense.shared.views.subscaleview.SubsamplingScaleImageView.OnStateChangedListener
        public void onScaleChanged(float newScale, int origin) {
        }
    }
}
