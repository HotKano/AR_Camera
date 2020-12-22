package com.netguru.arlocalizerview.rxutil

import com.netguru.arlocalizerview.compass.CompassData
import io.reactivex.subjects.PublishSubject

/**
 * CompassData(lat,lon) 전달용 이벤트 버스.
 * 20201204 @김종우
 * unSubscribe 는 Presenter 담당.
 */
object RxEventBusLocation {
    val location = PublishSubject.create<CompassData>()

    fun sendData(data: CompassData) {
        location.onNext(data)
    }

}