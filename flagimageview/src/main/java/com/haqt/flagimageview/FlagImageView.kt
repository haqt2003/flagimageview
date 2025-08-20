package com.haqt.flagimageview

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import kotlin.math.min
import kotlin.math.roundToInt

class FlagImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ShapeableImageView(context, attrs, defStyleAttr) {

    companion object {
        /**
         * The default size in dp to use when both width and height are set to wrap_content.
         */
        private const val DEFAULT_SIZE_DP = 60
    }

    /**
     * Private variable to store the cornerRadius value provided by the user
     */
    private var desiredCornerRadius: Float = 0f

    var countryCode: String? = null
        set(value) {
            field = value?.lowercase()
            updateFlag()
        }

    var cornerRadius: Float
        get() = desiredCornerRadius
        set(value) {
            desiredCornerRadius = value
            applyShape()
        }

    var shape: Shape = Shape.RECTANGLE
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
                if (countryCode != null) {
                    updateFlag()
                }
            }
        }

    init {
        val initialDrawable: Drawable? = drawable
        scaleType = ScaleType.CENTER_CROP

        attrs?.let {
            context.withStyledAttributes(it, R.styleable.FlagImageView) {
                countryCode = getString(R.styleable.FlagImageView_countryCode)
                // Gán giá trị từ XML vào biến private
                desiredCornerRadius = getDimension(R.styleable.FlagImageView_cornerRadius, 0f)
                val shapeValue = getInt(R.styleable.FlagImageView_shape, 1)
                this@FlagImageView.shape = Shape.fromValue(shapeValue)
            }
        }

        if (countryCode != null) {
            updateFlag()
        } else if (initialDrawable != null) {
            setImageDrawable(initialDrawable)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyShape()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        var width = measuredWidth
        var height = measuredHeight
        val aspectRatio = shape.ratio

        if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST) {
            val defaultSizeInPixels = (DEFAULT_SIZE_DP * resources.displayMetrics.density).roundToInt()
            width = defaultSizeInPixels
            height = (width / aspectRatio).roundToInt()
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = (width / aspectRatio).roundToInt()
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = (height * aspectRatio).roundToInt()
        }

        setMeasuredDimension(width, height)
    }


    private fun updateFlag() {
        val code = countryCode ?: return
        val resourceName = "flag_${code}_${shape.suffix}"
        val resId = context.resources.getIdentifier(
            resourceName, "drawable", context.packageName
        )

        if (resId != 0) {
            setImageResource(resId)
        } else {
            setImageDrawable(null)
        }
    }

    private fun applyShape() {
        if (width == 0 && height == 0) return

        /**
         * Calculate the maximum possible corner radius.
         * To create a circle/pill shape, the radius must be equal to half of the shortest side.
         */
        val maxRadius = min(width, height) / 2.0f

        /**
         * Determine the effective radius.
         * Take the smaller value between the user’s desired value and the maximum possible value.
         * Example: View width = 100px.
         *    - If the user wants cornerRadius = 10 -> use 10.
         *    - If the user wants cornerRadius = 999 -> use 50 (maxRadius) -> creates a circle.
         */
        val effectiveRadius = min(desiredCornerRadius, maxRadius)

        /**
         * Apply the shape with the calculated radius.
         */
        shapeAppearanceModel = ShapeAppearanceModel().toBuilder()
            .setAllCorners(CornerFamily.ROUNDED, effectiveRadius).build()
    }

    enum class Shape(val value: Int, val suffix: String, val ratio: Float) {
        SQUARE(0, "1x1", 1.0f),
        RECTANGLE(1, "4x3", 4.0f / 3.0f);

        companion object {
            fun fromValue(value: Int) = Shape.entries.find { it.value == value } ?: RECTANGLE
        }
    }
}