package com.netguru.arlocalizerview.arview

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.opengl.ETC1.getWidth
import android.view.animation.AccelerateInterpolator
import com.netguru.arlocalizerview.compass.CompassData
import com.netguru.arlocalizerview.util.CommFunc
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


@Suppress("MagicNumber")
internal object ARLabelUtils {

    private const val MAX_HORIZONTAL_ANGLE_VARIATION = 30f
    private const val MAX_VERTICAL_PITCH_VARIATION = 60f

    const val LOW_PASS_FILTER_ALPHA_PRECISE = 0.90f
    const val LOW_PASS_FILTER_ALPHA_NORMAL = 0.60f

    val VERTICAL_ANGLE_RANGE_MAX = 0f..MAX_VERTICAL_PITCH_VARIATION
    val VERTICAL_ANGLE_RANGE_MIN = -MAX_VERTICAL_PITCH_VARIATION..0f

    val HORIZONTAL_ANGLE_RANGE_MAX = 360f - MAX_HORIZONTAL_ANGLE_VARIATION - 10f..360f
    val HORIZONTAL_ANGLE_RANGE_MIN = 0f..10f + MAX_HORIZONTAL_ANGLE_VARIATION

    private const val PROPERTY_SIZE = "size"
    private const val ANIMATED_VALUE_MAX_SIZE = 120
    private const val ANIMATION_DURATION = 1000L
    private const val ACCELERATE_INTERPOLATOR_FACTOR = 2.5f
    const val MAX_ALPHA_VALUE = 255f
    const val ALPHA_DELTA = 155f

    private fun calculatePositionX(destinationAzimuth: Float, viewWidth: Int): Float {
        return when (destinationAzimuth) {
            in HORIZONTAL_ANGLE_RANGE_MIN -> {
                viewWidth / 2 + destinationAzimuth * viewWidth /
                        2 / MAX_HORIZONTAL_ANGLE_VARIATION
            }
            in HORIZONTAL_ANGLE_RANGE_MAX -> {
                viewWidth / 2 - (360f - destinationAzimuth) * viewWidth /
                        2 / MAX_HORIZONTAL_ANGLE_VARIATION
            }
            else -> 0f
        }
    }

    private fun calculatePositionY(currentPitch: Float, viewHeight: Int): Float {
        return when (currentPitch) {
            in VERTICAL_ANGLE_RANGE_MIN -> {
                viewHeight / 2 - currentPitch * viewHeight /
                        2 / MAX_VERTICAL_PITCH_VARIATION
            }
            in VERTICAL_ANGLE_RANGE_MAX -> {
                viewHeight / 2 - currentPitch * viewHeight /
                        2 / MAX_VERTICAL_PITCH_VARIATION
            }
            else -> 0f
        }
    }


    /**
     * 500m : 25000.0f
     * 400m :
     * 300m :
     * 200m :
     * 100m :
     * 50m : 100000.0f
     * View Size 에 따라서 값이 유동적으로 변경될 수 있도록 처리할 필요에 의해 추가.
     */
    val scale = 25000.0f // 100000.0f

    // 단순 위도 경도 받아서 X, Y로 대입하여 계산
    private fun CalculatePositionRadarX(viewWidth: Int, targetLon: Double, currentLon: Double): Float {
        return (viewWidth / 2 + (targetLon - currentLon) * scale).toFloat()
    }

    private fun CalculatePositionRadarY(viewHeight: Int, targetLat: Double, currentLat: Double): Float {
        return (viewHeight / 2 - (targetLat - currentLat) * scale).toFloat()
    }

    fun adjustLowPassFilterAlphaValue(positionX: Float, viewWidth: Int): Float {
        val centerPosition = viewWidth / 2f
        return when (positionX) {
            in 0f..centerPosition -> calculateLowPassFilterAlphaBeforeCenter(
                    centerPosition,
                    positionX
            )
            in centerPosition..viewWidth.toFloat() -> calculateLowPassFilterAlphaAfterCenter(
                    centerPosition,
                    positionX
            )
            else -> LOW_PASS_FILTER_ALPHA_NORMAL
        }
    }

    private fun calculateLowPassFilterAlphaAfterCenter(
            centerPosition: Float,
            positionX: Float
    ) =
            (LOW_PASS_FILTER_ALPHA_NORMAL - LOW_PASS_FILTER_ALPHA_PRECISE) /
                    centerPosition * positionX + 2 * LOW_PASS_FILTER_ALPHA_PRECISE - LOW_PASS_FILTER_ALPHA_NORMAL

    private fun calculateLowPassFilterAlphaBeforeCenter(
            centerPosition: Float,
            positionX: Float
    ) =
            (LOW_PASS_FILTER_ALPHA_PRECISE - LOW_PASS_FILTER_ALPHA_NORMAL) /
                    centerPosition * positionX + LOW_PASS_FILTER_ALPHA_NORMAL

    // For PreView Canvas
    fun prepareLabelsProperties(
            compassData: CompassData, viewWidth: Int,
            viewHeight: Int
    ): List<ARLabelProperties> {

        return compassData.destinations
                .filter { shouldShowLabel(it.currentDestinationAzimuth) && it.distanceToDestination < 250 }
                .sortedByDescending { it.distanceToDestination }
                .map { destinationData ->
                    ARLabelProperties(
                            destinationData.distanceToDestination,
                            destinationData.destinationLocation.pointName,
                            calculatePositionX(destinationData.currentDestinationAzimuth, viewWidth),
                            calculatePositionY(compassData.orientationData.currentPitch, viewHeight),
                            getAlphaValue(
                                    compassData.maxDistance,
                                    compassData.minDistance,
                                    destinationData.distanceToDestination
                            ),
                            id = destinationData.destinationLocation.hashCode()
                    )
                }
    }

    // For Radar Canvas @김종우
    // -> limitDistance 범위 제한용. cf) radar distance 범위 50, 100, 200, 300, 400, 500
    fun prepareLabelsRadar(compassData: CompassData, viewWidth: Int,
                           viewHeight: Int, limitDistance: Int): List<ARRadarProperties> {

        return compassData.destinations
                .filter { it.distanceToDestination < limitDistance }
                .sortedByDescending { it.distanceToDestination }
                .map { destinationData ->
                    val data = CommFunc.betweenBearing(compassData.currentLocation.latitude, compassData.currentLocation.longitude, destinationData.destinationLocation.latitude, destinationData.destinationLocation.longitude)
                    ARRadarProperties(
                            destinationData.distanceToDestination,
                            destinationData.destinationLocation.pointName,
                            CalculatePositionRadarX(viewWidth, destinationData.destinationLocation.longitude, compassData.currentLocation.longitude),
                            CalculatePositionRadarY(viewHeight, destinationData.destinationLocation.latitude, compassData.currentLocation.latitude),
                            destinationData.currentDestinationAzimuth,
                            id = destinationData.destinationLocation.hashCode()
                    )
                }
    }

    private fun getAlphaValue(maxDistance: Int, minDistance: Int, distanceToDestination: Int): Int {
        return when (maxDistance) {
            minDistance -> MAX_ALPHA_VALUE.toInt()
            else -> (-ALPHA_DELTA / (maxDistance - minDistance) * distanceToDestination +
                    MAX_ALPHA_VALUE - ALPHA_DELTA + ALPHA_DELTA / (maxDistance - minDistance) * maxDistance
                    ).roundToInt()
        }
    }

    private fun shouldShowLabel(destinationAzimuth: Float) =
            (destinationAzimuth in HORIZONTAL_ANGLE_RANGE_MIN
                    || destinationAzimuth in HORIZONTAL_ANGLE_RANGE_MAX)

    fun getShowUpAnimation(): ARLabelAnimationData {
        val propertySize = PropertyValuesHolder.ofInt(
                PROPERTY_SIZE, 0,
                ANIMATED_VALUE_MAX_SIZE
        )
        val arLabelAnimationData = ARLabelAnimationData(ValueAnimator().apply {
            setValues(propertySize)
            duration = ANIMATION_DURATION
            interpolator = AccelerateInterpolator(ACCELERATE_INTERPOLATOR_FACTOR)
            start()
        })

        arLabelAnimationData.valueAnimator?.addUpdateListener { animation ->
            arLabelAnimationData.animatedSize =
                    animation.getAnimatedValue(PROPERTY_SIZE) as? Int ?: 0
        }

        return arLabelAnimationData
    }
}
