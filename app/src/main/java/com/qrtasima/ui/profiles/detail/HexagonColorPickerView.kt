package com.qrtasima.ui.profiles.detail

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class HexagonColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var hexSize = 28f
    private val hexGridRadius = 12
    private var bufferedBitmap: Bitmap? = null
    private val hexPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hexPath = Path()

    private val tintZoneFactor = 0.5f

    var onColorSelected: ((colorHex: String) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        hexSize = min(w, h) / ((hexGridRadius * 2f) * 1.6f)
        createHexGridImage()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bufferedBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x.toInt()
            val y = event.y.toInt()

            bufferedBitmap?.let {
                if (x >= 0 && x < it.width && y >= 0 && y < it.height) {
                    val pixel = it.getPixel(x, y)
                    if (Color.alpha(pixel) != 0) {
                        val hex = String.format("#%06X", (0xFFFFFF and pixel))
                        onColorSelected?.invoke(hex)
                    }
                }
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun createHexGridImage() {
        if (width <= 0 || height <= 0) return

        bufferedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bufferedBitmap!!)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val centerX = width / 2f
        val centerY = height / 2f

        for (q in -hexGridRadius..hexGridRadius) {
            for (r in -hexGridRadius..hexGridRadius) {
                val s = -q - r
                if (max(abs(q), max(abs(r), abs(s))) > hexGridRadius) continue

                val hexColor: Int

                if (q == 0 && r == 0) {
                    hexColor = Color.WHITE
                } else {
                    val relativePixelX = hexSize * (sqrt(3f) * q + sqrt(3f) / 2f * r)
                    val relativePixelY = hexSize * (3f / 2f * r)
                    val angle = atan2(relativePixelY, relativePixelX)
                    val hue = ((angle / (2 * PI)) + 1.0).toFloat() % 1.0f

                    val distanceFromCenter = (abs(q) + abs(r) + abs(s)) / 2f
                    val normalizedDistance = (distanceFromCenter / hexGridRadius).coerceIn(0f, 1f)

                    val saturation: Float
                    val brightness: Float

                    if (normalizedDistance < tintZoneFactor) {
                        brightness = 1.0f
                        saturation = normalizedDistance / tintZoneFactor
                    } else {
                        saturation = 1.0f
                        val shadeProgress = (normalizedDistance - tintZoneFactor) / (1.0f - tintZoneFactor)
                        brightness = 1.0f - shadeProgress
                    }

                    hexColor = Color.HSVToColor(floatArrayOf(hue * 360f, saturation, brightness))
                }

                val pixelX = centerX + hexSize * (sqrt(3f) * q + sqrt(3f) / 2f * r)
                val pixelY = centerY + hexSize * (3f / 2f * r)
                drawHexagon(canvas, pixelX, pixelY, hexColor)
            }
        }
    }

    private fun drawHexagon(canvas: Canvas, centerX: Float, centerY: Float, color: Int) {
        hexPath.reset()
        for (i in 0..5) {
            val angleDeg = 60 * i + 30
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val x = centerX + hexSize * cos(angleRad).toFloat()
            val y = centerY + hexSize * sin(angleRad).toFloat()
            if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
        }
        hexPath.close()
        hexPaint.color = color
        hexPaint.style = Paint.Style.FILL
        canvas.drawPath(hexPath, hexPaint)
    }
}