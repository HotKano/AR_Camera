package com.netguru.arlocalizerview.arview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.netguru.arlocalizerview.R
import com.netguru.arlocalizerview.arview.ARLabelUtils.adjustLowPassFilterAlphaValue
import com.netguru.arlocalizerview.arview.ARLabelUtils.getShowUpAnimation
import com.netguru.arlocalizerview.compass.CompassData
import com.netguru.arlocalizerview.rxutil.RxEventBusPoint
import com.netguru.arlocalizerview.test.TestView
import kotlin.math.min

internal class ARLabelView : View {

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
        color = ContextCompat.getColor(context, R.color.ar_label_background)
        style = Paint.Style.FILL
    }

    private var animatedRectanglePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.ar_label_background)
        style = Paint.Style.STROKE
        strokeWidth =
                ANIMATED_RECTANGLE_STROKE_WIDTH
    }

    private var arLabels: List<ARLabelProperties>? = null
    private var animators = mutableMapOf<Int, ARLabelAnimationData>()
    private var lowPassFilterAlphaListener: ((Float) -> Unit)? = null

    companion object {
        private const val TEXT_HORIZONTAL_PADDING = 50f
        private const val TEXT_VERTICAL_PADDING = 35f
        private const val LABEL_CORNER_RADIUS = 20f
        private const val TEXT_SIZE = 50f
        private const val ANIMATED_RECTANGLE_STROKE_WIDTH = 10f
        private const val ANIMATED_VALUE_MAX_SIZE = 120
        private const val MAX_ALPHA_VALUE = 255f
        private const val CIRCLE_SIZE = 125f
        private const val CIRCLE_SIZE_DIS_200 = 85f
        private const val SERVICE_DISTANCE = 50
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        arLabels
                ?.forEach {
                    drawArLabel(canvas, it)
                }
    }

    // Label Output 처리.
    private fun drawArLabel(canvas: Canvas, arLabelProperties: ARLabelProperties) {

        val labelText = "${arLabelProperties.distance}m"
        val textWidthHalf = textPaint.measureText(labelText) / 2
        val textSize = textPaint.textSize

        val left = arLabelProperties.positionX - textWidthHalf - TEXT_HORIZONTAL_PADDING
        val top = arLabelProperties.positionY - textSize - TEXT_VERTICAL_PADDING
        val right = arLabelProperties.positionX + textWidthHalf + TEXT_HORIZONTAL_PADDING
        val bottom = arLabelProperties.positionY + TEXT_VERTICAL_PADDING

        /*canvas.drawRoundRect(
                left, top, right, bottom
                ,
                LABEL_CORNER_RADIUS,
                LABEL_CORNER_RADIUS, rectanglePaint.apply { *//*alpha = arLabelProperties.alpha*//* }
        )*/

        // 거리에 따른 분기 처리 나중에 커스텀할때 참조. ex) 200m 기준
        // 2020-12-03 김종우.
        if (arLabelProperties.distance > 200) {
            canvas.drawCircle(arLabelProperties.positionX, arLabelProperties.positionY, CIRCLE_SIZE_DIS_200, rectanglePaint)
        } else {
            canvas.drawCircle(arLabelProperties.positionX, arLabelProperties.positionY, CIRCLE_SIZE, rectanglePaint)
        }

        canvas.drawText(
                labelText,
                arLabelProperties.positionX,
                arLabelProperties.positionY,
                textPaint.apply { /*alpha = arLabelProperties.alpha*/ }
        )


        // 애니메이션 처리 삭제.
        /*      if (animators[arLabelProperties.id]?.valueAnimator?.isRunning == true) {
                  applyAnimationValues(canvas, left, top, right, bottom,
                          animators[arLabelProperties.id]?.animatedSize ?: 0)
              }*/

    }

    // 애니메이션 처리
    private fun applyAnimationValues(
            canvas: Canvas?,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            animatedRectangleSize: Int
    ) {
        animatedRectanglePaint.alpha =
                getAnimatedAlphaValue(animatedRectangleSize)
        canvas?.drawRoundRect(
                left - animatedRectangleSize, top - animatedRectangleSize,
                right + animatedRectangleSize, bottom + animatedRectangleSize,
                LABEL_CORNER_RADIUS,
                LABEL_CORNER_RADIUS, animatedRectanglePaint
        )
    }

    private fun getAnimatedAlphaValue(animatedRectangleSize: Int) =
            (MAX_ALPHA_VALUE - animatedRectangleSize * MAX_ALPHA_VALUE / ANIMATED_VALUE_MAX_SIZE).toInt()

    fun setCompassData(compassData: CompassData) {
        val labelsThatShouldBeShown =
                ARLabelUtils.prepareLabelsProperties(compassData, width, height)

        //showAnimationIfNeeded(arLabels, labelsThatShouldBeShown)

        arLabels = labelsThatShouldBeShown

        adjustAlphaFilterValue()

        invalidate()
    }

    private fun adjustAlphaFilterValue() {
        arLabels
                ?.find { isInView(it.positionX) }
                ?.let {
                    lowPassFilterAlphaListener?.invoke(
                            adjustLowPassFilterAlphaValue(it.positionX, width)
                    )
                }
    }

    private fun showAnimationIfNeeded(
            labelsShownBefore: List<ARLabelProperties>?,
            labelsThatShouldBeShown: List<ARLabelProperties>
    ) {
        labelsShownBefore?.let { checkForShowingUpLabels(labelsThatShouldBeShown, it) }
                ?: labelsThatShouldBeShown.forEach {
                    animators[it.id] = getShowUpAnimation()
                }
    }

    private fun checkForShowingUpLabels(
            labelsThatShouldBeShown: List<ARLabelProperties>,
            labelsShownBefore: List<ARLabelProperties>
    ) {
        labelsThatShouldBeShown
                .filterNot { newlabel -> labelsShownBefore.any { oldLabel -> newlabel.id == oldLabel.id } }
                .forEach { newLabels ->
                    animators[newLabels.id] = getShowUpAnimation()
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

    /**
     * Canvas는 View가 아니므로 onclick을 붙일 수 없다.
     * X,Y 좌표를 받아 원의 사이즈를 기반으로 범위 안에 들어오면 해당 객체(Canvas 상의 도형)를 클릭처리한다.
     * 2020-12-03
     * 김종우
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x: Float = event.x
        val y: Float = event.y

        if (arLabels == null || arLabels!!.isEmpty())
            return false

        for (item in this!!.arLabels!!) {
            if (item.positionX - CIRCLE_SIZE < x && x < item.positionX + CIRCLE_SIZE) {
                if (item.positionY - CIRCLE_SIZE < y && y < item.positionY + CIRCLE_SIZE) {
                    if (item.distance > SERVICE_DISTANCE) {
                        // Toast.makeText(context, "${item.pointName} 지점은 50m 까지는 도달하여야 서비스를 받을 수 있습니다.", Toast.LENGTH_SHORT).show()

                        // 이벤트 버스 출발 정거장 -> ARLocalizerView
                        RxEventBusPoint.sendData(item.pointName)
                    } else {
                        val nextIntent: Intent = Intent(context, TestView::class.java)
                        nextIntent.putExtra("data", item.pointName)
                        context.startActivity(nextIntent)
                    }
                }
            }
        }

        return false
    }

}
