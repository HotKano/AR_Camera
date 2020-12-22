package com.netguru.arlocalizerview.rxutil

import io.reactivex.subjects.PublishSubject

/**
 * PointName 전달용 이벤트 버스.
 * 20201204 @김종우
 * unSubscribe 는 Presenter 담당.
 */
object RxEventBusPoint {
    val pointName = PublishSubject.create<String>()

    fun sendData(data: String) {
        pointName.onNext(data)
    }

}