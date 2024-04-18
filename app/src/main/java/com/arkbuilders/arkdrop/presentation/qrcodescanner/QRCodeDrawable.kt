package com.arkbuilders.arkdrop.presentation.qrcodescanner

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable

class QRCodeDrawable(qrCodeViewModel: QRCodeScannerViewModel) : Drawable() {
    private val boundingRectPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.YELLOW
        strokeWidth = 5F
        alpha = 200
    }

    private val contentRectPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.YELLOW
        alpha = 255
    }

    private val contentTextPaint = Paint().apply {
        color = Color.DKGRAY
        alpha = 255
        textSize = 36F
    }

    private val qrCodeViewModel = qrCodeViewModel
    private val contentPadding = 25
    private var textWidth = contentTextPaint.measureText(qrCodeViewModel.qrContent).toInt()

    override fun draw(canvas: Canvas) {
        canvas.drawRect(qrCodeViewModel.boundingRect, boundingRectPaint)
        canvas.drawRect(
            Rect(
                qrCodeViewModel.boundingRect.left,
                qrCodeViewModel.boundingRect.bottom + contentPadding/2,
                qrCodeViewModel.boundingRect.left + textWidth + contentPadding*2,
                qrCodeViewModel.boundingRect.bottom + contentTextPaint.textSize.toInt() + contentPadding),
            contentRectPaint
        )
        canvas.drawText(
            qrCodeViewModel.qrContent,
            (qrCodeViewModel.boundingRect.left + contentPadding).toFloat(),
            (qrCodeViewModel.boundingRect.bottom + contentPadding*2).toFloat(),
            contentTextPaint
        )
    }

    override fun setAlpha(alpha: Int) {
        boundingRectPaint.alpha = alpha
        contentRectPaint.alpha = alpha
        contentTextPaint.alpha = alpha
    }

    override fun setColorFilter(colorFiter: ColorFilter?) {
        boundingRectPaint.colorFilter = colorFilter
        contentRectPaint.colorFilter = colorFilter
        contentTextPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}