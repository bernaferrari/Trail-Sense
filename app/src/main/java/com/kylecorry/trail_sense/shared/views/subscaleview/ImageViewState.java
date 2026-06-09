package com.kylecorry.trail_sense.shared.views.subscaleview;

import android.graphics.PointF;
import androidx.annotation.NonNull;
import java.io.Serializable;
public class ImageViewState implements Serializable {
    private final float scale;
    private final float centerX;
    private final float centerY;
    private final int orientation;

    public ImageViewState(float scale, @NonNull PointF center, int orientation) {
        this.scale = scale;
        this.centerX = center.x;
        this.centerY = center.y;
        this.orientation = orientation;
    }

    public float getScale() {
        return this.scale;
    }

    @NonNull
    public PointF getCenter() {
        return new PointF(this.centerX, this.centerY);
    }

    public int getOrientation() {
        return this.orientation;
    }
}
