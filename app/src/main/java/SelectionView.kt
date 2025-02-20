
package com.example.myrenamerexample

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SelectionView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var startX: Float = 0f
    private var startY: Float = 0f
    private var endX: Float = 0f
    private var endY: Float = 0f
    private var baseImage: Bitmap? = null
    private var scaledBitmap: Bitmap? = null
    private var imageLeft: Float = 0f
    private var imageTop: Float = 0f
    private var imageScale: Float = 1f

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private var selectedArea: RectF? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        baseImage?.let { updateScaledBitmap(it) }
    }

    private fun updateScaledBitmap(original: Bitmap) {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val imageWidth = original.width.toFloat()
        val imageHeight = original.height.toFloat()

        imageScale = Math.min(
            viewWidth / imageWidth,
            viewHeight / imageHeight
        )

        val scaledWidth = (imageWidth * imageScale).toInt()
        val scaledHeight = (imageHeight * imageScale).toInt()

        // Center the image
        imageLeft = (viewWidth - scaledWidth) / 2
        imageTop = (viewHeight - scaledHeight) / 2

        scaledBitmap = Bitmap.createScaledBitmap(
            original,
            scaledWidth,
            scaledHeight,
            true
        )
        invalidate()
    }

    fun setBaseImage(bitmap: Bitmap) {
        baseImage = bitmap
        if (width > 0 && height > 0) {
            updateScaledBitmap(bitmap)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isPointInImage(event.x, event.y) && event.action == MotionEvent.ACTION_DOWN) {
            return false
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                endX = event.x
                endY = event.y
                selectedArea = null
            }
            MotionEvent.ACTION_MOVE -> {
                endX = event.x
                endY = event.y
                updateSelectedArea()
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                endX = event.x
                endY = event.y
                updateSelectedArea()
                invalidate()
            }
        }
        return true
    }

    private fun isPointInImage(x: Float, y: Float): Boolean {
        scaledBitmap?.let { bitmap ->
            return x >= imageLeft &&
                    x <= imageLeft + bitmap.width &&
                    y >= imageTop &&
                    y <= imageTop + bitmap.height
        }
        return false
    }

    private fun updateSelectedArea() {
        // Constrain selection to image bounds
        val left = maxOf(minOf(startX, endX), imageLeft)
        val top = maxOf(minOf(startY, endY), imageTop)
        val right = minOf(maxOf(startX, endX), imageLeft + (scaledBitmap?.width ?: 0))
        val bottom = minOf(maxOf(startY, endY), imageTop + (scaledBitmap?.height ?: 0))

        selectedArea = RectF(left, top, right, bottom)
    }

    fun getSelectedArea(): RectF? {
        return selectedArea?.let { rect ->
            RectF(
                (rect.left - imageLeft) / imageScale,
                (rect.top - imageTop) / imageScale,
                (rect.right - imageLeft) / imageScale,
                (rect.bottom - imageTop) / imageScale
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the image
        scaledBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, imageLeft, imageTop, null)
        }

        // Draw selection rectangle
        selectedArea?.let { rect ->
            canvas.drawRect(rect, paint)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}