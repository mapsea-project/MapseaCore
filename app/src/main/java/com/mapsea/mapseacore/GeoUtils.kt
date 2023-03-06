package com.mapsea.mapseacore


class GeoUtils {
    companion object {
        const val EARTH_RADIUS_IN_KM = 6371.0
        const val METERS_PER_KILOMETER = 1000.0

        fun fN(number: Number, decimalPlaces: Int = 5, leadingZeros: Int = 2): String {
            return when(number) {
                is Double -> {
                    "%.${decimalPlaces}f".format(number)
                }
                is Int -> {
                    "%0${leadingZeros}d".format(number)
                }
                else -> {
                    throw IllegalArgumentException("Number must be either Double or Int.")
                }
            }
        }

        fun fN(num: Double): String {
            return fN(num, 5, 2)
        }
    }

}