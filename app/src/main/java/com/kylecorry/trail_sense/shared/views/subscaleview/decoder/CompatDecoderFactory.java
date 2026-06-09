package com.kylecorry.trail_sense.shared.views.subscaleview.decoder;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
public class CompatDecoderFactory<T> implements DecoderFactory<T> {
    private final Class<? extends T> clazz;
    private final Bitmap.Config bitmapConfig;

    public CompatDecoderFactory(@NonNull Class<? extends T> clazz) {
        this(clazz, null);
    }

    public CompatDecoderFactory(@NonNull Class<? extends T> clazz, Bitmap.Config bitmapConfig) {
        this.clazz = clazz;
        this.bitmapConfig = bitmapConfig;
    }

    @Override // com.kylecorry.trail_sense.shared.views.subscaleview.decoder.DecoderFactory
    @NonNull
    public T make() throws IllegalAccessException, NoSuchMethodException, InstantiationException, InvocationTargetException {
        if (this.bitmapConfig == null) {
            return this.clazz.newInstance();
        }
        Constructor<? extends T> ctor = this.clazz.getConstructor(Bitmap.Config.class);
        return ctor.newInstance(this.bitmapConfig);
    }
}
