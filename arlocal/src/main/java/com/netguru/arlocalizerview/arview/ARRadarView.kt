package com.netguru.arlocalizerview.arview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.netguru.arlocalizerview.R
import com.netguru.arlocalizerview.compass.CompassData
import kotlin.collections.set
import kotlin.math.min

class ARRadarView : View {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, attributeSetId: Int) : super(
            context,
            attrs,
            attributeSetId
    )

    private var textPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.ar_label_text)
        textSize = TEXT_SIZE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private var rectanglePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.red)
        style = Paint.Style.FILL
    }

    private var arLabels: List<ARRadarProperties>? = null
    private var animators = mutableMapOf<Int, ARLabelAnimationData>()
    private var lowPassFilterAlphaListener: ((Float) -> Unit)? = null
    private var limitDistance: Int = 250

    companion object {
        private const val TEXT_SIZE = 50f
        private const val CIRCLE_SIZE = 8.0f // 레이더 상의 object 사이즈
        private const val ACC_VALUE = 0f // 회전 보정치
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 현재 위치
        canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), CIRCLE_SIZE, rectanglePaint.apply { color = ContextCompat.getColor(context, R.color.ar_label_text) })

        arLabels
                ?.forEach {
                    drawArLabel(canvas, it)
                }
    }

    // Label Output 처리.
    private fun drawArLabel(canvas: Canvas, arRadarProperties: ARRadarProperties) {

        val labelText = arRadarProperties.pointName

        // 대상물들 위치
        canvas.drawCircle(arRadarProperties.positionX, arRadarProperties.positionY, CIRCLE_SIZE, rectanglePaint.apply { color = ContextCompat.getColor(context, R.color.red) })

    }

    // 위치 데이터 받는 곳.
    fun setCompassData(compassData: CompassData) {
        val allLabels =
                ARLabelUtils.prepareLabelsRadar(compassData, width, height, limitDistance)

        //showAnimationIfNeeded(arLabels, allLabels)

        // allLables의 갯수가 혜택의 갯수가 된다.
        arLabels = allLabels
        // 회전처리 @김종우
        this.rotation = -compassData.orientationData.currentAzimuth
        adjustAlphaFilterValue()

        invalidate()
    }

    private fun showAnimationIfNeeded(
            labelsShownBefore: List<ARRadarProperties>?,
            labelsThatShouldBeShown: List<ARRadarProperties>
    ) {
        labelsShownBefore?.let { checkForShowingUpLabels(labelsThatShouldBeShown, it) }
                ?: labelsThatShouldBeShown.forEach {
                    animators[it.id] = ARLabelUtils.getShowUpAnimation()
                }
    }

    private fun checkForShowingUpLabels(
            labelsThatShouldBeShown: List<ARRadarProperties>,
            labelsShownBefore: List<ARRadarProperties>
    ) {
        labelsThatShouldBeShown
                .filterNot { newlabel -> labelsShownBefore.any { oldLabel -> newlabel.id == oldLabel.id } }
                .forEach { newLabels ->
                    animators[newLabels.id] = ARLabelUtils.getShowUpAnimation()
                }
    }

    private fun adjustAlphaFilterValue() {
        arLabels
                ?.find { isInView(it.positionX) }
                ?.let {
                    lowPassFilterAlphaListener?.invoke(
                            ARLabelUtils.adjustLowPassFilterAlphaValue(it.positionX, width)
                    )
                }
    }

    fun setLowPassFilterAlphaListener(lowPassFilterAlphaListener: ((Float) -> Unit)?) {
        this.lowPassFilterAlphaListener = lowPassFilterAlphaListener
    }

    private fun isInView(positionX: Float) = positionX > 0 && positionX < width

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val desiredHeight = suggestedMinimumHeight + paddingTop + paddingBottom

        setMeasuredDimension(
                measureDimension(desiredWidth, widthMeasureSpec),
                measureDimension(desiredHeight, heightMeasureSpec)
        )
    }

    private fun measureDimension(desiredSize: Int, measureSpec: Int): Int {
        var result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = desiredSize
            if (specMode == MeasureSpec.AT_MOST) {
                result = min(result, specSize)
            }
        }
        return result
    }


}