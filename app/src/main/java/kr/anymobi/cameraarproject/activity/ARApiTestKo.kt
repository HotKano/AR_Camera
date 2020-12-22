package kr.anymobi.cameraarproject.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.netguru.arlocalizerview.ARLocalizerDependencyProvider
import com.netguru.arlocalizerview.PermissionManager
import com.netguru.arlocalizerview.arview.ARLocalizerView
import com.netguru.arlocalizerview.location.LocationData
import com.netguru.arlocalizerview.rxutil.PointPresenter
import com.netguru.arlocalizerview.rxutil.RxEventBusLocation
import io.reactivex.disposables.Disposable
import kr.anymobi.cameraarproject.R
import kr.anymobi.cameraarproject.util.CommAnimate
import kr.anymobi.cameraarproject.util.CommFunc


class ARApiTestKo : AppCompatActivity(), ARLocalizerDependencyProvider, LifecycleOwner {

    // AR 관련
    private lateinit var arLocalizerView: ARLocalizerView
    private lateinit var locationData: ArrayList<LocationData> // 데이터 (id, pointName, lat, lon)

    // 이벤트 버스 관련
    private lateinit var pointDisposable: Disposable
    private lateinit var pointPresenter: PointPresenter  // unSubscriber 용
    private var addressData: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_api_test)
        initMap()
        reConnectedWidget()
    }

    override fun onResume() {
        super.onResume()
        eventBusStation()
        pointPresenter.addDisposable(pointDisposable) // 이벤트버스 등록
    }

    override fun onPause() {
        super.onPause()
        pointPresenter.dispose() // 이벤트 버스 해제.
    }

    @SuppressLint("CheckResult")
    private fun eventBusStation() {
        pointPresenter = PointPresenter()
        pointDisposable = RxEventBusLocation.location.subscribe {
            if (it != null) {
                addressData = CommFunc.findAddress(this, it.currentLocation.latitude, it.currentLocation.longitude)
                Log.d("jongwoo", "${it.currentLocation} test data from ARApiTestKo $addressData")

                if (addressData.isNotEmpty())
                    findViewById<TextView>(R.id.addressFlowText).text = addressData
            }
        }
    }

    private fun reConnectedWidget() {
        val mapBtn: ImageButton = findViewById(R.id.mapBtn)
        val backBtn: ImageButton = findViewById(R.id.ar_backBtn)
        val addressText: TextView = findViewById(R.id.addressFlowText) // 주소 확인 TextView.
        CommAnimate.leftToRightFlowAnimated(addressText)
        // TODO DTO 객체를 통한 데이터 정립 후 initMap 에서 넣고 있는 더미데이터와 동일화 로직 필요. 전달 방식은 별로.. -> 발전 서버에서 jsonString. @김종우
        // Test 용 더미데이터 전달.
        val intent: Intent = Intent(this, MapActivity::class.java)

        if (locationData.isNotEmpty())
            intent.putExtra("mapDataList", locationData)

        mapBtn.setOnClickListener { startActivity(intent) }
        backBtn.setOnClickListener { onBackPressed() }
    }

    private fun initMap() {
        arLocalizerView = findViewById(R.id.arLocalizer)
        arLocalizerView.onCreate(this)
        locationData = ArrayList<LocationData>()

        //TODO 더미데이터

        // #1 E동
        locationData.add(LocationData(1, "R&D", 37.4042345, 127.0969063))
        // #2 R&D
        locationData.add(LocationData(2, "E동", 37.4038725, 127.1006017))
        // #3 웹젠
        locationData.add(LocationData(3, "웹젠", 37.4025197, 127.0993873))
        // #4 W시티
        locationData.add(LocationData(4, "W시티", 37.40385123442209, 127.09999017195578))
        // #5 하늘도서
        locationData.add(LocationData(5, "하늘도서", 37.4076208, 127.0954965)) //600m
        // #6 unknown 500m point
        locationData.add(LocationData(6, "500", 37.40008779462526, 127.10489559783551))

        arLocalizerView.setDestinations(locationData)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionManager.ESSENTIAL_PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    for (item in grantResults) {
                        if (item != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "모든 사용 권한이 있어야 가능합니다.", Toast.LENGTH_SHORT).show()
                            onBackPressed()
                            break
                        }
                    }
                    // 사용 처리(?)
                    // 2020-12-03 김종우
                    Log.d("jongwoo", "All permission is accepted :D")
                    initMap()
                }
            }
        }

    }

    override fun getARViewLifecycleOwner() = this
    override fun getPermissionActivity() = this
    override fun getSensorsContext() = this

}