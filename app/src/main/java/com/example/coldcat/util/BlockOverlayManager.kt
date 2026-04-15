package com.example.coldcat.util

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.example.coldcat.R

object BlockOverlayManager {

    private var view: View? = null
    private var windowManager: WindowManager? = null

    fun show(context: Context, packageName: String) {
        if (!Settings.canDrawOverlays(context)) return

        if (view != null) return // prevent duplicate overlays

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val inflater = LayoutInflater.from(context)
        view = inflater.inflate(R.layout.block_overlay, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager?.addView(view, params)
    }

    fun hide(context: Context? = null) {
        try {
            view?.let {
                windowManager?.removeView(it)
            }
        } catch (_: Exception) {}

        view = null
    }
}