package com.netguru.arlocalizerview.rxutil

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

interface BasePresenter {
    val compositeDisposable: CompositeDisposable
    fun addDisposable(disposable: Disposable)
    fun dispose()
}