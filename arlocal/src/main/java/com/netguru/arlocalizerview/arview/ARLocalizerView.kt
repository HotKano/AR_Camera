package com.netguru.arlocalizerview.arview

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Rational
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.camera.core.CameraX
import androidx.camera.core.PreviewConfig
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.netguru.arlocalizerview.*
import com.netguru.arlocalizerview.common.ViewState
import com.netguru.arlocalizerview.compass.CompassData
import com.netguru.arlocalizerview.databinding.ArLocalizerLayoutBinding
import com.netguru.arlocalizerview.location.LocationData
import com.netguru.arlocalizerview.rxutil.LocationPresenter
import com.netguru.arlocalizerview.rxutil.RxEventBusLocation
import com.netguru.arlocalizerview.rxutil.RxEventBusPoint
import com.netguru.arlocalizerview.util.CommAnimate
import com.netguru.arlocalizerview.util.CommFunc
import io.reactivex.disposables.Disposable

@Suppress("UnusedPrivateMember", "TooManyFunctions")
class ARLocalizerView : FrameLayout, LifecycleObserver {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, attributeSetId: Int) : super(
            context,
            attrs,
            attributeSetId
    )

    companion object {
        private const val SAVED_STATE = "saved_state"
    }

    private lateinit var viewModel: IARLocalizerViewModel
    private lateinit var arLocalizerComponent: ARLocalizerComponent

    /**
     * View Binding 사용법 변경으로 변경
     * MVVM Pattern
     * 2020-12-22 김종우
     */
    private val bindingTest = ArLocalizerLayoutBinding.inflate(LayoutInflater.from(context), this)

    // Location 이벤트 버스 관련
    private var busSendFlag = false // // 이벤트 버스 전달 1회용 flag.
    private lateinit var locationPresenter: LocationPresenter  // unSubscriber 용
    private lateinit var disposable: Disposable // subscribe
    private lateinit var initLensFacing: CameraX.LensFacing

    fun onCreate(arLocalizerDependencyProvider: ARLocalizerDependencyProvider) {
        arLocalizerComponent =
                DaggerARLocalizerComponent.factory().create(arLocalizerDependencyProvider)
        viewModel = arLocalizerComponent.arLocalizerViewModel()
        arLocalizerComponent.arLocalizerDependencyProvider().getARViewLifecycleOwner()
                .lifecycle.addObserver(this)
        checkPermissions()
    }

    @SuppressLint("CheckResult", "LogNotTimber")
    private fun eventBusStation() {
        // PointName 이벤트 버스 정거장 도착
        // 김종우
        locationPresenter = LocationPresenter()
        disposable = RxEventBusPoint.pointName.subscribe {
            bindingTest.toastTest.text = resources.getString(R.string.ar_nearGuide)
            CommAnimate.leftToastAnimated(bindingTest.toastTest)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun onActivityCreate() {
        if (context != null) {
            val layoutParams: MarginLayoutParams = bindingTest.arTitleZone.layoutParams as MarginLayoutParams
            layoutParams.setMargins(0, CommFunc.getStatusBarHeight(context), 0, 0)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onActivityDestroy() {
        bindingTest.arLabelView.setLowPassFilterAlphaListener(null)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onActivityResume() {
        bindingTest.arLabelView.setLowPassFilterAlphaListener(null)
        eventBusStation()
        locationPresenter.addDisposable(disposable) // EventBus 등록.
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun onActivityPause() {
        locationPresenter.dispose() // EventBus 해제.
    }

    fun setDestinations(destinations: List<LocationData>) {
        viewModel.setDestinations(destinations)
    }

    private fun observeCompassState() {
        bindingTest.arLabelView.setLowPassFilterAlphaListener {
            viewModel.setLowPassFilterAlpha(it)
        }
        viewModel.compassState().observe(
                arLocalizerComponent.arLocalizerDependencyProvider().getARViewLifecycleOwner(),
                Observer { viewState ->
                    when (viewState) {
                        is ViewState.Success<CompassData> -> handleSuccessData(viewState.data)
                        is ViewState.Error -> showErrorDialog(viewState.message)
                    }
                })
    }

    private fun checkPermissions() {
        viewModel.permissionState.observe(
                arLocalizerComponent.arLocalizerDependencyProvider().getARViewLifecycleOwner(),
                Observer { permissionState ->
                    when (permissionState) {
                        PermissionResult.GRANTED -> {
                            bindingTest.textureView.post { startCameraPreview() }
                            observeCompassState()
                        }
                        PermissionResult.SHOW_RATIONALE -> showRationaleSnackbar()
                        PermissionResult.NOT_GRANTED -> Unit
                    }
                })
        viewModel.checkPermissions()
    }

    // 현재 위치, 방향 등 전달
    private fun handleSuccessData(compassData: CompassData) {
        bindingTest.arLabelView.setCompassData(compassData)
        bindingTest.arRaiderView.setCompassData(compassData)
        if (!busSendFlag) {
            // 이벤트 버스 현재 위치 1회 확인용 출발 -> ARApiTestKo Bus Station
            RxEventBusLocation.sendData(compassData)
            busSendFlag = true
        }

    }

    @SuppressLint("RestrictedApi")
    private fun startCameraPreview() {
        initLensFacing = CameraX.LensFacing.BACK
        val previewConfig = PreviewConfig.Builder().apply {
            // 사용할 렌즈 CameraX.LensFacing.BACK, CameraX.LensFacing.FRONT
            setLensFacing(initLensFacing)

            // 보여질 해상도의 비율을 설정
            setTargetAspectRatio(Rational(16, 9))
            // 보여질 비디오 해상도를 설정
            setTargetResolution(Size(1920, 1080))
            // 보여지는 view의 rotation을 지정
            setTargetRotation(bindingTest.textureView.display.rotation)
        }.build()

        val preview = AutoFitPreviewBuilder.build(
                previewConfig,
                bindingTest.textureView
        )

        CameraX.bindToLifecycle(
                arLocalizerComponent.arLocalizerDependencyProvider().getARViewLifecycleOwner(),
                preview
        )
    }

    // 전면, 후면 전환용 Method @김종우
    // 그냥 필요할지도 몰라서 만든 것 현재 아직은 사용 안함.
    private fun switchCameraSight() {
        initLensFacing = if (initLensFacing == CameraX.LensFacing.FRONT) {
            CameraX.LensFacing.BACK
        } else {
            CameraX.LensFacing.FRONT
        }

        // 카매라 전환을 위한 자원 해제
        CameraX.unbindAll()

        val previewConfig = PreviewConfig.Builder().apply {
            // 사용할 렌즈 CameraX.LensFacing.BACK, CameraX.LensFacing.FRONT
            setLensFacing(initLensFacing)

            // 보여질 해상도의 비율을 설정
            setTargetAspectRatio(Rational(16, 9))
            // 보여질 비디오 해상도를 설정
            setTargetResolution(Size(1920, 1080))
            // 보여지는 view의 rotation을 지정
            setTargetRotation(bindingTest.textureView.display.rotation)
        }.build()

        val preview = AutoFitPreviewBuilder.build(
                previewConfig,
                bindingTest.textureView
        )

        CameraX.bindToLifecycle(
                arLocalizerComponent.arLocalizerDependencyProvider().getARViewLifecycleOwner(),
                preview
        )
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(context)
                .setTitle(R.string.error_title)
                .setMessage(resources.getString(R.string.error_message, message))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    observeCompassState()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->

                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
    }

    fun onRequestPermissionResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        viewModel.onRequestPermissionResult(requestCode, permissions, grantResults)
    }

    private fun showRationaleSnackbar() {
        Snackbar.make(
                this,
                R.string.essential_permissions_not_granted_info,
                Snackbar.LENGTH_SHORT
        )
                .setAction(R.string.permission_recheck_question) { viewModel.checkPermissions() }
                .setDuration(BaseTransientBottomBar.LENGTH_LONG)
                .show()
    }

    override fun onSaveInstanceState(): Parcelable? {
        return Bundle().apply {
            putParcelable(SAVED_STATE, super.onSaveInstanceState())
            viewModel.onSaveInstanceState(this)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        var newState = state
        if (newState is Bundle) {
            viewModel.onRestoreInstanceState(newState)
            newState = newState.getParcelable(SAVED_STATE)
        }
        super.onRestoreInstanceState(newState)
    }

}
