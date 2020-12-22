package com.netguru.arlocalizerview.location

import android.os.Parcel
import android.os.Parcelable

data class LocationData(val id: Int, val pointName: String, val latitude: Double, val longitude: Double) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readString().toString(),
            parcel.readDouble(),
            parcel.readDouble()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(pointName)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LocationData> {
        override fun createFromParcel(parcel: Parcel): LocationData {
            return LocationData(parcel)
        }

        override fun newArray(size: Int): Array<LocationData?> {
            return arrayOfNulls(size)
        }
    }
}

