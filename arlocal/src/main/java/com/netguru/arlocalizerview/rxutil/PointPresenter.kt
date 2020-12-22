package com.netguru.arlocalizerview.rxutil

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

class PointPresenter : BasePresenter {
    override val compositeDisposable = CompositeDisposable()

    override fun addDisposable(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }

    override fun dispose() {
        compositeDisposable.dispose()
    }
}