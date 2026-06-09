package com.kylecorry.trail_sense.shared.views.subscaleview.decoder;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.kylecorry.trail_sense.shared.views.subscaleview.SubsamplingScaleImageView;
import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
public class SkiaPooledImageRegionDecoder implements ImageRegionDecoder {
    private static final String TAG = SkiaPooledImageRegionDecoder.class.getSimpleName();
    private static boolean debug = false;
    private DecoderPool decoderPool;
    private final ReadWriteLock decoderLock;
    private static final String FILE_PREFIX = "file://";
    private static final String ASSET_PREFIX = "file:///android_asset/";
    private static final String RESOURCE_PREFIX = "android.resource://";
    private final Bitmap.Config bitmapConfig;
    private Context context;
    private Uri uri;
    private long fileLength;
    private final Point imageDimensions;
    private final AtomicBoolean lazyInited;

    @Keep
    public SkiaPooledImageRegionDecoder() {
        this(null);
    }

    public SkiaPooledImageRegionDecoder(@Nullable Bitmap.Config bitmapConfig) {
        this.decoderPool = new DecoderPool();
        this.decoderLock = new ReentrantReadWriteLock(true);
        this.fileLength = Long.MAX_VALUE;
        this.imageDimensions = new Point(0, 0);
        this.lazyInited = new AtomicBoolean(false);
        Bitmap.Config globalBitmapConfig = SubsamplingScaleImageView.getPreferredBitmapConfig();
        if (bitmapConfig != null) {
            this.bitmapConfig = bitmapConfig;
        } else if (globalBitmapConfig != null) {
            this.bitmapConfig = globalBitmapConfig;
        } else {
            this.bitmapConfig = Bitmap.Config.RGB_565;
        }
    }

    @Keep
    public static void setDebug(boolean debug2) {
        debug = debug2;
    }

    @Override // com.kylecorry.trail_sense.shared.views.subscaleview.decoder.ImageRegionDecoder
    @NonNull
    public Point init(Context context, @NonNull Uri uri) throws Exception {
        this.context = context;
        this.uri = uri;
        initialiseDecoder();
        return this.imageDimensions;
    }

    private void lazyInit() {
        if (this.lazyInited.compareAndSet(false, true) && this.fileLength < Long.MAX_VALUE) {
            debug("Starting lazy init of additional decoders");
            Thread thread = new Thread() { // from class: com.kylecorry.trail_sense.shared.views.subscaleview.decoder.SkiaPooledImageRegionDecoder.1
                @Override // java.lang.Thread, java.lang.Runnable
                public void run() {
                    while (SkiaPooledImageRegionDecoder.this.decoderPool != null && SkiaPooledImageRegionDecoder.this.allowAdditionalDecoder(SkiaPooledImageRegionDecoder.this.decoderPool.size(), SkiaPooledImageRegionDecoder.this.fileLength)) {
                        try {
                            if (SkiaPooledImageRegionDecoder.this.decoderPool != null) {
                                long start = System.currentTimeMillis();
                                SkiaPooledImageRegionDecoder.this.debug("Starting decoder");
                                SkiaPooledImageRegionDecoder.this.initialiseDecoder();
                                long end = System.currentTimeMillis();
                                SkiaPooledImageRegionDecoder.this.debug("Started decoder, took " + (end - start) + "ms");
                            }
                        } catch (Exception e) {
                            SkiaPooledImageRegionDecoder.this.debug("Failed to start decoder: " + e.getMessage());
                        }
                    }
                }
            };
            thread.start();
        }
    }
    private void initialiseDecoder() throws Exception {
        BitmapRegionDecoder decoder;
        Resources res;
        String uriString = this.uri.toString();
        long fileLength = Long.MAX_VALUE;
        if (uriString.startsWith(RESOURCE_PREFIX)) {
            String packageName = this.uri.getAuthority();
            if (this.context.getPackageName().equals(packageName)) {
                res = this.context.getResources();
            } else {
                PackageManager pm = this.context.getPackageManager();
                res = pm.getResourcesForApplication(packageName);
            }
            int id = 0;
            List<String> segments = this.uri.getPathSegments();
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
            try {
                fileLength = this.context.getResources().openRawResourceFd(id).getLength();
            } catch (Exception e2) {
            }
            decoder = BitmapRegionDecoder.newInstance(this.context.getResources().openRawResource(id), false);
        } else if (uriString.startsWith(ASSET_PREFIX)) {
            String assetName = uriString.substring(ASSET_PREFIX.length());
            try {
                fileLength = this.context.getAssets().openFd(assetName).getLength();
            } catch (Exception e3) {
            }
            decoder = BitmapRegionDecoder.newInstance(this.context.getAssets().open(assetName, 1), false);
        } else if (uriString.startsWith(FILE_PREFIX)) {
            decoder = BitmapRegionDecoder.newInstance(uriString.substring(FILE_PREFIX.length()), false);
            try {
                File file = new File(uriString);
                if (file.exists()) {
                    fileLength = file.length();
                }
            } catch (Exception e4) {
            }
        } else {
            InputStream inputStream = null;
            try {
                ContentResolver contentResolver = this.context.getContentResolver();
                inputStream = contentResolver.openInputStream(this.uri);
                decoder = BitmapRegionDecoder.newInstance(inputStream, false);
                try {
                    AssetFileDescriptor descriptor = contentResolver.openAssetFileDescriptor(this.uri, "r");
                    if (descriptor != null) {
                        fileLength = descriptor.getLength();
                    }
                } catch (Exception e5) {
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e6) {
                    }
                }
            } catch (Throwable th) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e7) {
                    }
                }
                throw th;
            }
        }
        this.fileLength = fileLength;
        this.imageDimensions.set(decoder.getWidth(), decoder.getHeight());
        this.decoderLock.writeLock().lock();
        try {
            if (this.decoderPool != null) {
                this.decoderPool.add(decoder);
            }
        } finally {
            this.decoderLock.writeLock().unlock();
        }
    }

    @Override // com.kylecorry.trail_sense.shared.views.subscaleview.decoder.ImageRegionDecoder
    @NonNull
    public Bitmap decodeRegion(@NonNull Rect sRect, int sampleSize) {
        debug("Decode region " + sRect + " on thread " + Thread.currentThread().getName());
        if (sRect.width() < this.imageDimensions.x || sRect.height() < this.imageDimensions.y) {
            lazyInit();
        }
        this.decoderLock.readLock().lock();
        try {
            if (this.decoderPool != null) {
                BitmapRegionDecoder decoder = this.decoderPool.acquire();
                if (decoder != null) {
                    try {
                        if (!decoder.isRecycled()) {
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inSampleSize = sampleSize;
                            options.inPreferredConfig = this.bitmapConfig;
                            Bitmap bitmap = decoder.decodeRegion(sRect, options);
                            if (bitmap == null) {
                                throw new RuntimeException("Skia image decoder returned null bitmap - image format may not be supported");
                            }
                            return bitmap;
                        }
                    } finally {
                        if (decoder != null) {
                            this.decoderPool.release(decoder);
                        }
                    }
                }
                if (decoder != null) {
                    this.decoderPool.release(decoder);
                }
            }
            throw new IllegalStateException("Cannot decode region after decoder has been recycled");
        } finally {
            this.decoderLock.readLock().unlock();
        }
    }

    @Override // com.kylecorry.trail_sense.shared.views.subscaleview.decoder.ImageRegionDecoder
    public synchronized boolean isReady() {
        return (this.decoderPool == null || this.decoderPool.isEmpty()) ? false : true;
    }

    @Override // com.kylecorry.trail_sense.shared.views.subscaleview.decoder.ImageRegionDecoder
    public synchronized void recycle() {
        this.decoderLock.writeLock().lock();
        try {
            if (this.decoderPool != null) {
                this.decoderPool.recycle();
                this.decoderPool = null;
                this.context = null;
                this.uri = null;
            }
        } finally {
            this.decoderLock.writeLock().unlock();
        }
    }

    protected boolean allowAdditionalDecoder(int numberOfDecoders, long fileLength) {
        if (numberOfDecoders >= 4) {
            debug("No additional decoders allowed, reached hard limit (4)");
            return false;
        }
        if (((long) numberOfDecoders) * fileLength > 20971520) {
            debug("No additional encoders allowed, reached hard memory limit (20Mb)");
            return false;
        }
        if (numberOfDecoders >= getNumberOfCores()) {
            debug("No additional encoders allowed, limited by CPU cores (" + getNumberOfCores() + ")");
            return false;
        }
        if (isLowMemory()) {
            debug("No additional encoders allowed, memory is low");
            return false;
        }
        debug("Additional decoder allowed, current count is " + numberOfDecoders + ", estimated native memory " + ((fileLength * ((long) numberOfDecoders)) / 1048576) + "Mb");
        return true;
    }
    private static class DecoderPool {
        private final Semaphore available = new Semaphore(0, true);
        private final Map<BitmapRegionDecoder, Boolean> decoders = new ConcurrentHashMap();

        private DecoderPool() {
        }

        private synchronized boolean isEmpty() {
            return this.decoders.isEmpty();
        }

        private synchronized int size() {
            return this.decoders.size();
        }

        private BitmapRegionDecoder acquire() {
            this.available.acquireUninterruptibly();
            return getNextAvailable();
        }

        private void release(BitmapRegionDecoder decoder) {
            if (markAsUnused(decoder)) {
                this.available.release();
            }
        }

        private synchronized void add(BitmapRegionDecoder decoder) {
            this.decoders.put(decoder, false);
            this.available.release();
        }

        private synchronized void recycle() {
            while (!this.decoders.isEmpty()) {
                BitmapRegionDecoder decoder = acquire();
                decoder.recycle();
                this.decoders.remove(decoder);
            }
        }

        private synchronized BitmapRegionDecoder getNextAvailable() {
            for (Map.Entry<BitmapRegionDecoder, Boolean> entry : this.decoders.entrySet()) {
                if (!entry.getValue().booleanValue()) {
                    entry.setValue(true);
                    return entry.getKey();
                }
            }
            return null;
        }

        private synchronized boolean markAsUnused(BitmapRegionDecoder decoder) {
            for (Map.Entry<BitmapRegionDecoder, Boolean> entry : this.decoders.entrySet()) {
                if (decoder == entry.getKey()) {
                    if (entry.getValue().booleanValue()) {
                        entry.setValue(false);
                        return true;
                    }
                    return false;
                }
            }
            return false;
        }
    }

    private int getNumberOfCores() {
        if (Build.VERSION.SDK_INT >= 17) {
            return Runtime.getRuntime().availableProcessors();
        }
        return getNumCoresOldPhones();
    }

    private int getNumCoresOldPhones() {
        try {
            File dir = new File("/sys/devices/system/cpu/");
            File[] files = dir.listFiles(new FileFilter() { // from class: com.kylecorry.trail_sense.shared.views.subscaleview.decoder.SkiaPooledImageRegionDecoder.1CpuFilter
                @Override // java.io.FileFilter
                public boolean accept(File pathname) {
                    return Pattern.matches("cpu[0-9]+", pathname.getName());
                }
            });
            return files.length;
        } catch (Exception e) {
            return 1;
        }
    }

    private boolean isLowMemory() {
        ActivityManager activityManager = (ActivityManager) this.context.getSystemService("activity");
        if (activityManager != null) {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            return memoryInfo.lowMemory;
        }
        return true;
    }

    private void debug(String message) {
        if (debug) {
            Log.d(TAG, message);
        }
    }
}
