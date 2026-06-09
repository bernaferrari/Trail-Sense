package com.kylecorry.trail_sense.shared.views.subscaleview.decoder;

import androidx.annotation.NonNull;
import java.lang.reflect.InvocationTargetException;
public interface DecoderFactory<T> {
    @NonNull
    T make() throws IllegalAccessException, NoSuchMethodException, InstantiationException, InvocationTargetException;
}
