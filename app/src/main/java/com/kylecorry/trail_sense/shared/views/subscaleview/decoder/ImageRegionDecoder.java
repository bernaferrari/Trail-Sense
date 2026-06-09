package com.kylecorry.trail_sense.shared.views.subscaleview.decoder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import androidx.annotation.NonNull;
public interface ImageRegionDecoder {
    @NonNull
    Point init(Context context, @NonNull Uri uri) throws Exception;

    @NonNull
    Bitmap decodeRegion(@NonNull Rect rect, int i);

    boolean isReady();

    void recycle();
}
