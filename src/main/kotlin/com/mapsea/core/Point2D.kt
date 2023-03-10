package com.route.lib

/** 좌표 데이터<br>
 * @param lon 경도
 * @param lat 위도
 * */
data class Point2D(var lon: Double, var lat: Double, var velocity: Double = 0.0) {
    constructor() : this(0.0, 0.0, 0.0)
    constructor(lon: Double, lat: Double) : this(lon, lat, 0.0)

    operator fun minus(other: Point2D): Point2D {
        return Point2D(lon - other.lon, lat - other.lat)
    }

    operator fun plus(other: Point2D): Point2D {
        return Point2D(lon + other.lon, lat + other.lat)
    }

    operator fun times(other: Double): Point2D {
        return Point2D(lon * other, lat * other)
    }

    operator fun times(other: Point2D): Point2D {
        return Point2D(lon * other.lon, lat * other.lat)
    }

    operator fun div(other: Double): Point2D {
        return Point2D(lon / other, lat / other)
    }

    operator fun div(other: Point2D): Point2D {
        return Point2D(lon / other.lon, lat / other.lat)
    }

    fun distanceTo(other: Point2D): Double {
        return RouteUtiles.distance(this, other)
    }

    fun bearingTo(other: Point2D): Double {
        return RouteUtiles.bearing(this, other)
    }
}