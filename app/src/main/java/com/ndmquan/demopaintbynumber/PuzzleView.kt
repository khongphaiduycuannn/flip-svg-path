package com.ndmquan.demopaintbynumber

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Region
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.PathParser
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withClip
import org.xmlpull.v1.XmlPullParser
import androidx.core.graphics.get

class PuzzleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val DEFAULT_IMAGE_SIZE = 1024
    }

    private val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = "#CCCCCC".toColorInt()
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 2f
    }
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val shapePaths = mutableMapOf<String, Path>()
    private val shapeRegions = mutableMapOf<String, Region>()
    private val shapeColors = mutableMapOf<String, Int>()
    private var selectedShapeName = mutableSetOf<String>()

    private var scaledBitmap: Bitmap? = null

    private var selectedColor: Int? = null


    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.PuzzleView,
            0, 0
        ).apply {
            try {
                val maskResId = getResourceId(R.styleable.PuzzleView_puzzleShapes, 0)
                val sourceImageResId = getResourceId(R.styleable.PuzzleView_puzzleSourceImage, 0)

                if (maskResId != 0) {
                    parseVectorDrawable(maskResId)
                }

                if (sourceImageResId != 0) {
                    scaledBitmap = getBitmapFromDrawable(context, sourceImageResId)?.scale(
                        DEFAULT_IMAGE_SIZE,
                        DEFAULT_IMAGE_SIZE
                    )
                    initListColor()
                }
            } finally {
                recycle()
            }
        }
    }

    private fun parseVectorDrawable(drawableId: Int) {
        val parser: XmlPullParser = resources.getXml(drawableId)
        var eventType = parser.eventType
        var index = 0

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "path") {
                val name = "shape_$index"
                val androidNamespace = "http://schemas.android.com/apk/res/android"
                val pathData = parser.getAttributeValue(androidNamespace, "pathData")

                if (!pathData.isNullOrBlank()) {
                    shapePaths[name] = PathParser.createPathFromPathData(pathData)
                }
                index++
            }
            eventType = parser.next()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        shapePaths.forEach { (name, path) ->
            val region = Region()
            region.setPath(path, Region(0, 0, DEFAULT_IMAGE_SIZE, DEFAULT_IMAGE_SIZE))
            shapeRegions[name] = region
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val scaledBitmap = scaledBitmap ?: return

        shapePaths.forEach { (name, path) ->
            if (selectedShapeName.contains(name)) {
                canvas.withClip(path) {
                    drawBitmap(scaledBitmap, 0f, 0f, imagePaint)
                }
            } else {
                canvas.drawPath(path, placeholderPaint)
            }

            canvas.drawPath(path, borderPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x.toInt()
            val y = event.y.toInt()

            var tappedShapeName: String? = null
            for (name in shapeRegions.keys.reversed()) {
                if (shapeRegions[name]?.contains(x, y) == true) {
                    tappedShapeName = name
                    break
                }
            }

            tappedShapeName?.let { shapeName ->
                if (!selectedShapeName.contains(shapeName)) {
                    selectedShapeName.add(shapeName)
                    invalidate()
                }
            }

            return true
        }
        return super.onTouchEvent(event)
    }

    private fun getBitmapFromDrawable(context: Context, drawableId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 512
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 512

        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun initListColor() {
        val scaledBitmap = scaledBitmap ?: return
        val colorList = mutableListOf<Int>()

        shapePaths.forEach { shapeName, path ->
            val region = Region()
            region.setPath(path, Region(0, 0, DEFAULT_IMAGE_SIZE, DEFAULT_IMAGE_SIZE))

            val bounds = region.bounds
            val colorFrequency = mutableMapOf<Int, Int>()

            val step = 3

            for (x in bounds.left until bounds.right step step) {
                for (y in bounds.top until bounds.bottom step step) {
                    if (region.contains(
                            x,
                            y
                        ) && x < scaledBitmap.width && y < scaledBitmap.height
                    ) {
                        val pixel = scaledBitmap[x, y]
                        if (Color.alpha(pixel) > 0) {
                            val roundedColor = roundColor(pixel)
                            colorFrequency[roundedColor] =
                                colorFrequency.getOrDefault(roundedColor, 0) + 1
                        }
                    }
                }
            }

            val dominantColor = colorFrequency.maxByOrNull { it.value }?.key
            if (dominantColor != null) {
                shapeColors[shapeName] = dominantColor
                colorList.add(dominantColor)
            }
        }
    }

    private fun roundColor(color: Int, factor: Int = 32): Int {
        val r = (Color.red(color) / factor) * factor
        val g = (Color.green(color) / factor) * factor
        val b = (Color.blue(color) / factor) * factor
        return Color.rgb(r, g, b)
    }


    fun getListColor(): List<Int> {
        return shapeColors.values.toSet().toList()
    }

    fun setSelectedColor(color: Int) {
        selectedColor = color
    }
}