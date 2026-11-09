package com.kylecorry.trail_sense.shared.morse

import com.kylecorry.andromeda.torch.Torch

import com.kylecorry.trail_sense.shared.andromeda_temp.sound.ISoundPlayer
fun Torch.asSignal(brightness: Float = 1f): ISignalingDevice {
    return ISignalingDevice.from({ on(brightness) }, this::off)
}

fun ISoundPlayer.asSignal(): ISignalingDevice {
    return ISignalingDevice.from(this::on, this::off)
}
