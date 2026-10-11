package io.github.yiannosparanomos.undertone.spike

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/** Minimal launcher: brings the probe out of the "stopped" state so broadcasts reach it. */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            TextView(this).apply {
                text = "UnderTone M1 platform probe\nDrive it with adb broadcasts; read Logcat tag UTSPIKE."
                textSize = 16f
                setPadding(48, 96, 48, 48)
            },
        )
        Probe.log("boot", "sdk" to android.os.Build.VERSION.SDK_INT, "device" to android.os.Build.DEVICE)
    }
}
