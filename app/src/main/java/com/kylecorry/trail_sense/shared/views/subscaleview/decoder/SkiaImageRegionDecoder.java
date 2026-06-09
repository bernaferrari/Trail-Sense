package com.kylecorry.trail_sense.shared.views.subscaleview.decoder;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.kylecorry.trail_sense.shared.views.subscaleview.SubsamplingScaleImageView;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
public class SkiaImageRegionDecoder implements ImageRegionDecoder {
    private BitmapRegionDecoder decoder;
    private final ReadWriteLock decoderLock;
    private static final String FILE_PREFIX = "file://";
    private static final String ASSET_PREFIX = "file:///android_asset/";
    private static final String RESOURCE_PREFIX = "android.resource://";
    private final Bitmap.Config bitmapConfig;

    @Keep
    public SkiaImageRegionDecoder() {
        this(null);
    }

    public SkiaImageRegionDecoder(@Nullable Bitmap.Config bitmapConfig) {
        this.decoderLock = new ReentrantReadWriteLock(true);
        Bitmap.Config globalBitmapConfig = SubsamplingScaleImageView.getPreferredBitmapConfig();
        if (bitmapConfig != null) {
            this.bitmapConfig = bitmapConfig;
        } else if (globalBitmapConfig != null) {
            this.bitmapConfig = globalBitmapConfig;
        } else {
            this.bitmapConfig = Bitmap.Config.RGB_565;
        }
    }

    @Override // com.kylecorry.trail_sense.shared.views.subscaleview.decoder.ImageRegionDecoder
    @NonNull
    public Point init(Context context, @NonNull Uri uri) throws Exception {
        Resources res;
        String uriString = uri.toString();
        if (uriString.startsWith(RESOURCE_PREFIX)) {
            String packageName = uri.getAuthority();
            if (context.getPackageName().equals(packageName)) {
                res = context.getResources();
            } else {
                PackageManager pm = context.getPackageManager();
                res = pm.getResourcesForApplication(packageName);
            }
            int id = 0;
            List<String> segments = uri.getPathSegments();
            int size = segments.size();
            if (size == 2 && segments.get(0).equals("drawable")) {
                String resName = segments.get(1);
                id = res.getIdentifier(resName, "drawable", packageName);
            } else if (size == 1 && TextUtils.isDigitsOnly(segments.get(0))) {
                try {
                    id = Integer.parseInt(segments.get(0));
                } catch (NumberFormatException e) {
                }
            }
            this.decoder = BitmapRegionDecoder.newInstance(context.getResources().openRawResource(id), false);
        } else if (uriString.startsWith(ASSET_PREFIX)) {
            String assetName = uriString.substring(ASSET_PREFIX.length());
            this.decoder = BitmapRegionDecoder.newInstance(context.getAssets().open(assetName, 1), false);
        } else if (uriString.startsWith(FILE_PREFIX)) {
            this.decoder = BitmapRegionDecoder.newInstance(uriString.substring(FILE_PREFIX.length()), false);
        } else {
            InputStream inputStream = null;
            try {
                ContentResolver contentResolver = context.getContentResolver();
                InputStream inputStream2 = contentResolver.openInputStream(uri);
                if (inputStream2 == null) {
                    throw new Exception("Content resolver returned null stream. Unable to initialise with uri.");
                }
                this.decoder = BitmapRegionDecoder.newInstance(inputStream2, false);
                if (inputStream2 != null) {
                    try {
                        inputStream2.close();
                    } catch (Exception e2) {
                    }
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        inputStream.close();
                    } catch (Exception e3) {
                    }
                }
                throw th;
            }
        }
        return new Point(this.decoder.getWidth(), this.decoder.getHeight());
    }

    @Override // com.kylecorry.trail_sense.shared.views.subscaleview.decoder.ImageRegionDecoder
    @NonNull
    public Bitmap decodeRegion(@NonNull Rect sRect, int sampleSize) {
        getDecodeLock().lock();
        try {
            if (this.decoder != null && !this.decoder.isRecycled()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = sampleSize;
                options.inPreferredConfig = this.bitmapConfig;
                Bitmap bitmap = this.decoder.decodeRegion(sRect, options);
                if (bitmap == null) {
                    throw new RuntimeException("Skia image decoder returned null bitmap - image format may not be supported");
                }
                return bitmap;
            }
            throw new IllegalStateException("Cannot decode region after decoder has been recycled");
        } finally {
            getDecodeLock().unlock();
        }
    }

    @Override // com.kylecorry.trail_sense.shared.views.subscaleview.decoder.ImageRegionDecoder
    public synchronized boolean isReady() {
        return (this.decoder == null || this.decoder.isRecycled()) ? false : true;
    }

    @Override // com.kylecorry.trail_sense.shared.views.subscaleview.decoder.ImageRegionDecoder
    public synchronized void recycle() {
        this.decoderLock.writeLock().lock();
        try {
            this.decoder.recycle();
            this.decoder = null;
        } finally {
            this.decoderLock.writeLock().unlock();
        }
    }

    private Lock getDecodeLock() {
        if (Build.VERSION.SDK_INT < 21) {
            return this.decoderLock.writeLock();
        }
        return this.decoderLock.readLock();
    }
}
