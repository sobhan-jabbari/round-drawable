package ir.afraapps.kotlin.basic.component

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.Size
import androidx.core.graphics.withTranslation
import ir.afraapps.kotlin.basic.R
import ir.afraapps.kotlin.basic.core.allIsSameValue
import ir.afraapps.kotlin.basic.core.color
import ir.afraapps.kotlin.basic.core.getColorStateListCompat
import ir.afraapps.kotlin.basic.core.isLocaleRTL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.colorAttr
import java.lang.Float.max
import kotlin.math.min

/**
 * In the name of Allah
 *
 * Created by Ali Jabbari on 5/22/20.
 */

class RoundDrawable(val context: Context) : Drawable() {

    private val paint: Paint
    private val boundRound: RectF
    private val path: Path
    private var alphaColor = -1


    constructor(context: Context, init: RoundDrawable.() -> Unit) : this(context) {
        apply(init)
    }

    var drawableState: RoundDrawableState? = null
    private var mutated = false


    init {
        drawableState = mutateRoundConstantState()
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        boundRound = RectF()
        path = Path()
    }

    var width: Int = -1
    var height: Int = -1

    var strokeWidth: Float = 0f

    @ColorInt
    var strokeColor: Int = Color.TRANSPARENT

    @ColorRes
    var strokeColorResource: Int = 0
        set(value) {
            strokeColor = context.color(value)
        }

    var strokeColorStateList: ColorStateList? = null

    @ColorRes
    var strokeColorStateListResource: Int = 0
        set(value) {
            strokeColorStateList = context.getColorStateListCompat(value)
        }


    @ColorInt
    var fillColor: Int = Color.TRANSPARENT

    @ColorRes
    var fillColorResource: Int = 0
        set(value) {
            fillColor = context.color(value)
            fillColorStateList = null
        }

    var fillColorStateList: ColorStateList? = null

    @ColorRes
    var fillColorStateListResource: Int = 0
        set(value) {
            fillColorStateList = context.getColorStateListCompat(value)
        }


    var radius = 0f
        set(value) {
            if (value >= 0f) {
                field = value
                radiuses = floatArrayOf(value, value, value, value)
            }
        }

    /**
     * [tl, tr, br, bl]
     */
    @Size(4)
    var radiuses: FloatArray = floatArrayOf(radius, radius, radius, radius)


    fun topCorner(radius: Float) {
        radiuses = floatArrayOf(radius, radius, 0f, 0f)
    }

    fun bottomCorner(radius: Float) {
        radiuses = floatArrayOf(0f, 0f, radius, radius)
    }

    fun rightCorner(radius: Float) {
        radiuses = floatArrayOf(0f, radius, radius, 0f)
    }

    fun leftCorner(radius: Float) {
        radiuses = floatArrayOf(radius, 0f, 0f, radius)
    }

    fun startLocaleCorner(radius: Float) {
        if (isLocaleRTL()) rightCorner(radius) else leftCorner(radius)
    }

    fun endLocaleCorner(radius: Float) {
        if (isLocaleRTL()) leftCorner(radius) else rightCorner(radius)
    }

    var isCircle: Boolean = false

    var shadowType: Int = ShadowType.BLUR
    var shadowColor: Int = 0x30000000
    var shadowSize = 0f
    var shadowX = 2f
    var shadowY = 5f

    var paddingLeft = 0
    var paddingTop = 0
    var paddingRight = 0
    var paddingBottom = 0

    fun setPadding(padding: Int) {
        paddingLeft = padding
        paddingTop = padding
        paddingRight = padding
        paddingBottom = padding
    }

    private val bitmapMatrix = Matrix()

    var bitmap: Bitmap? = null
        set(value) {
            field = value
            val b = bounds
            if (b.isEmpty.not()) {
                prepareBoundAndPath(b)
            }
        }

    private fun mutateRoundConstantState(): RoundDrawableState {
        return RoundDrawableState(context, drawableState)
    }

    override fun getConstantState(): ConstantState? {
        return drawableState?.let {
            it.stateChangingConfigurations = changingConfigurations
            it.strokeWidth = strokeWidth
            it.strokeColor = strokeColor
            it.strokeColorStateList = strokeColorStateList
            it.fillColor = fillColor
            it.fillColorStateList = fillColorStateList
            it.radiuses = radiuses
            it.shadowColor = shadowColor
            it.shadowSize = shadowSize
            it.shadowX = shadowX
            it.shadowY = shadowY
            it.paddingLeft = paddingLeft
            it.paddingTop = paddingTop
            it.paddingRight = paddingRight
            it.paddingBottom = paddingBottom
            it
        }
    }

    override fun mutate(): Drawable {
        if (!mutated && super.mutate() == this) {
            drawableState = mutateRoundConstantState()
            mutated = true
        }
        return this
    }

    override fun draw(canvas: Canvas) {

        //----------------------------------------------------------
        paint.style = if (strokeWidth > 0f) Paint.Style.FILL_AND_STROKE else Paint.Style.FILL
        val nPath = path

        if (shadowSize > 0f && nPath.isEmpty.not()) {
            paint.color = shadowColor
            if (shadowType == ShadowType.BLUR) {
                paint.maskFilter = BlurMaskFilter(shadowSize, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.withTranslation(shadowX, shadowY) {
                if (bitmap != null) {
                    canvas.drawPath(path, paint)
                } else {
                    drawRoundDrawable(canvas)
                }
            }
            paint.maskFilter = null
        }

        //----------------------------------------------------------
        if (bitmap != null) {
            if (nPath.isEmpty.not()) {
                canvas.drawBitmap(bitmap!!, bitmapMatrix, null)
            }
            return
        }

        //----------------------------------------------------------
        paint.color = fillColorStateList?.getColorForState(state, fillColor) ?: fillColor
        drawRoundDrawable(canvas)

        //----------------------------------------------------------
        if (strokeWidth <= 0f) return
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth

        strokeColorStateList?.let {
            paint.color = it.getColorForState(state, strokeColor)
        } ?: let { paint.color = strokeColor }

        drawRoundDrawable(canvas)
    }


    private fun drawRoundDrawable(canvas: Canvas) {
        if (alphaColor > -1) {
            paint.alpha = alphaColor
        }
        if (isCircle) {
            val r = min(boundRound.height(), boundRound.width()) / 2f
            canvas.drawCircle(boundRound.centerX(), boundRound.centerY(), r, paint)

        } else {
            canvas.drawPath(path, paint)
        }
    }


    override fun setAlpha(alpha: Int) {
        alphaColor = alpha
        invalidateSelf()
    }


    override fun getAlpha(): Int {
        return paint.alpha
    }


    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    /*var xfermode
        get() = paint.xfermode
        set(value) = let { paint.xfermode = value }*/

    private fun hasPadding(): Boolean {
        return paddingLeft > 0 || paddingTop > 0 || paddingRight > 0 || paddingBottom > 0
    }


    override fun getPadding(padding: Rect): Boolean {
        var consumed = false
        padding.set(0, 0, 0, 0)
        if (shadowSize > 0) {
            consumed = true
            padding.left += Math.max(shadowSize - shadowX, 0f).toInt()
            padding.right += (shadowSize + shadowX).toInt()
            padding.top += Math.max(shadowSize - shadowY, 0f).toInt()
            padding.bottom += (shadowSize + shadowY).toInt()
        }
        if (hasPadding()) {
            consumed = true
            padding.left += paddingLeft
            padding.right += paddingRight
            padding.top += paddingTop
            padding.bottom += paddingBottom
        }
        return consumed
    }


    override fun onStateChange(state: IntArray): Boolean {
        if (strokeColorStateList == null && fillColorStateList == null) return false
        invalidateSelf()
        return true
    }


    override fun isStateful(): Boolean {
        return true
    }

    override fun getIntrinsicWidth(): Int {
        return if (width > 0) width else super.getIntrinsicWidth()
    }

    override fun getIntrinsicHeight(): Int {
        return if (height > 0) height else super.getIntrinsicHeight()
    }

    override fun onBoundsChange(bounds: Rect) {
        prepareBoundAndPath(bounds)
    }

    private fun prepareBoundAndPath(bounds: Rect) {
        boundRound.set(bounds)
        if (strokeWidth > 0f) {
            val borderHalf = strokeWidth / 2f
            boundRound.left += borderHalf
            boundRound.top += borderHalf
            boundRound.right -= borderHalf
            boundRound.bottom -= borderHalf
        }
        if (shadowSize > 0f) {
            boundRound.left += max(shadowSize - shadowX, 0f)
            boundRound.top += max(shadowSize - shadowY, 0f)
            boundRound.right -= min(shadowSize + shadowX, bounds.width().toFloat())
            boundRound.bottom -= min(shadowSize + shadowY, bounds.height().toFloat())
        }
        generatePath(boundRound)
    }


    private fun generatePath(bounds: RectF) {
        path.reset()
        bitmapMatrix.reset()
        if (bitmap == null) {
            path.addRoundRect(bounds, getCorrectRadius(), Path.Direction.CW)
        } else {
            val b = bitmap!!
            val nPath = Path()
            GlobalScope.launch(Dispatchers.IO) {
                bitmapMatrix.setScale(bounds.width() / b.width.toFloat(), bounds.height() / b.height.toFloat())
                bitmapMatrix.postTranslate(bounds.left, bounds.top)
                for (y in 0 until b.height) {
                    for (x in 0 until b.width) {
                        val pixel = b.getPixel(x, y)
                        if (pixel == 0) continue
                        nPath.addRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, Path.Direction.CCW)
                    }
                }
                nPath.transform(bitmapMatrix)
                path.set(nPath)
                withContext(Dispatchers.Main) {
                    invalidateSelf()
                }
            }
        }
    }

    private fun getCorrectRadius(): FloatArray {
        val tl = radiuses[0].takeIf { it >= 0f } ?: 0f
        val tr = radiuses[1].takeIf { it >= 0f } ?: 0f
        val br = radiuses[2].takeIf { it >= 0f } ?: 0f
        val bl = radiuses[3].takeIf { it >= 0f } ?: 0f
        return floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)
    }

    override fun getOutline(outline: Outline) {
        if (isCircle) {
            val borderHalf = strokeWidth / 2f
            outline.setOval(
                (boundRound.left - borderHalf).toInt(),
                (boundRound.top - borderHalf).toInt(),
                (boundRound.right + borderHalf).toInt(),
                (boundRound.bottom + borderHalf).toInt()
            )

        } else if (bitmap != null || radiuses.allIsSameValue().not()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                outline.setPath(path)
            } else {
                @Suppress("DEPRECATION")
                outline.setConvexPath(path)
            }
        } else {
            outline.setRoundRect(boundRound.left.toInt(), boundRound.top.toInt(), boundRound.right.toInt(), boundRound.bottom.toInt(), radius)
        }

        outline.alpha = alpha / 255.0f
    }


    object ShadowType {
        const val BLUR = 1
        const val SOLID = 2
    }


    class RoundDrawableState(val context: Context, origin: RoundDrawableState? = null) : ConstantState() {

        var stateChangingConfigurations = 0

        var strokeWidth: Float = 0f

        @ColorInt
        var strokeColor: Int = Color.TRANSPARENT

        var strokeColorStateList: ColorStateList? = null

        @ColorInt
        var fillColor: Int = Color.TRANSPARENT

        var fillColorStateList: ColorStateList? = null

        /**
         * [tl, tr, br, bl]
         */
        @Size(4)
        var radiuses: FloatArray = floatArrayOf(0f, 0f, 0f, 0f)


        var isCircle: Boolean = false

        var shadowColor: Int = 0x30000000
        var shadowSize = 0f
        var shadowX = 2f
        var shadowY = 5f

        var paddingLeft = 0
        var paddingTop = 0
        var paddingRight = 0
        var paddingBottom = 0

        init {
            origin?.let {
                stateChangingConfigurations = it.stateChangingConfigurations
            }
        }

        override fun newDrawable(): Drawable {
            return RoundDrawable(context).also {
                it.strokeWidth = strokeWidth
                it.strokeColor = strokeColor
                it.strokeColorStateList = strokeColorStateList
                it.fillColor = fillColor
                it.fillColorStateList = fillColorStateList
                it.isCircle = isCircle
                it.radiuses = radiuses
                it.shadowSize = shadowSize
                it.shadowColor = shadowColor
                it.shadowX = shadowX
                it.shadowY = shadowY
                it.paddingLeft = paddingLeft
                it.paddingTop = paddingTop
                it.paddingRight = paddingRight
                it.paddingBottom = paddingBottom
            }
        }

        override fun newDrawable(res: Resources?): Drawable {
            return newDrawable()
        }

        override fun newDrawable(res: Resources?, theme: Resources.Theme?): Drawable {
            return newDrawable()
        }

        override fun getChangingConfigurations(): Int {
            return stateChangingConfigurations
        }

    }


}

fun Context.roundDrawable(init: RoundDrawable.() -> Unit = {}): RoundDrawable {
    return RoundDrawable(this, init)
}

fun Context.circleDrawable(init: RoundDrawable.() -> Unit = {}): RoundDrawable {
    return RoundDrawable(this, init).apply {
        isCircle = true
    }
}

fun Context.rippleDrawable(isUnbounded: Boolean = false, highlight: ColorStateList? = null, init: RoundDrawable.() -> Unit = {}): Drawable {
    val drawable = RoundDrawable(this, init)
    val h = highlight ?: ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_pressed),
            intArrayOf()
        ),
        intArrayOf(
            colorAttr(R.attr.colorControlHighlight),
            Color.TRANSPARENT
        )
    )
    return RippleDrawable(h, if (isUnbounded) null else drawable, if (isUnbounded) null else drawable)
}

fun Context.rippleCircleDrawable(isUnbounded: Boolean = false, highlight: ColorStateList? = null, init: RoundDrawable.() -> Unit = {}): Drawable {
    val drawable = circleDrawable(init)
    val h = highlight ?: ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_pressed),
            intArrayOf()
        ),
        intArrayOf(
            colorAttr(R.attr.colorControlHighlight),
            Color.TRANSPARENT
        )
    )
    return RippleDrawable(h, if (isUnbounded) null else drawable, if (isUnbounded) null else drawable)
}

fun View.roundDrawable(init: RoundDrawable.() -> Unit = {}): RoundDrawable {
    return context.roundDrawable(init)
}

fun View.circleDrawable(init: RoundDrawable.() -> Unit = {}): RoundDrawable {
    return context.circleDrawable(init)
}

fun View.rippleDrawable(isUnbounded: Boolean = false, highlight: ColorStateList? = null, init: RoundDrawable.() -> Unit = {}): Drawable {
    return context.rippleDrawable(isUnbounded, highlight, init)
}

fun View.rippleCircleDrawable(isUnbounded: Boolean = false, highlight: ColorStateList? = null, init: RoundDrawable.() -> Unit = {}): Drawable {
    return context.rippleCircleDrawable(isUnbounded, highlight, init)
}