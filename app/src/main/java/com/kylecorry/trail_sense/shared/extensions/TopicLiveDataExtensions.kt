package com.kylecorry.trail_sense.shared.extensions

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kylecorry.luna.topics.ITopic
import com.kylecorry.luna.topics.generic.ITopic as GenericTopic

fun <T : ITopic> T.asLiveData(): LiveData<T> {
    lateinit var liveData: MutableLiveData<T>
    val handler = Handler(Looper.getMainLooper())
    val lock = Any()
    val callback = {
        handler.post {
            synchronized(lock) {
                liveData.value = this
            }
        }
        true
    }
    liveData = object : MutableLiveData<T>() {
        override fun onActive() {
            super.onActive()
            subscribe(callback)
        }

        override fun onInactive() {
            super.onInactive()
            unsubscribe(callback)
        }
    }
    return liveData
}

fun <K : Any, T : GenericTopic<K>> T.asLiveData(): LiveData<K> {
    lateinit var liveData: MutableLiveData<K>
    val handler = Handler(Looper.getMainLooper())
    val lock = Any()
    val callback = { value: K ->
        handler.post {
            synchronized(lock) {
                liveData.value = value
            }
        }
        true
    }
    liveData = object : MutableLiveData<K>() {
        override fun onActive() {
            super.onActive()
            subscribe(callback)
        }

        override fun onInactive() {
            super.onInactive()
            unsubscribe(callback)
        }
    }
    return liveData
}
