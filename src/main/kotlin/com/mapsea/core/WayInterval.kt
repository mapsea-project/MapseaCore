package com.mapsea.core

import com.mapsea.core.MSFINAL.Companion.DDG
import java.lang.Math.toRadians
import kotlin.math.*


/** 항로 정보를 담는 클래스
 * @param _nvgPt1 : 항로 상의 위치 판단을 위한 2개의 기준점
 * @param _nvgPt2 : 항로 상의 위치 판단을 위한 2개의 기준점
 * @param _portsideXTD : 해당 항로의 왼쪽 한계 거리(km)
 * @param _starboardXTD : 해당 항로의 오른쪽 한계 거리(km)
 * @param _speed : 운항 속력(knot)
 * @param _turnRadius : 선회율(deg/min)
 * @param _bearing : 항로가 뻗은 각도(rad)
 * */
class WayInterval(lat1: Double, lon1: Double, lat2: Double, lon2: Double) {
    /** 항로가 뻗은 각도 (rad)  */
    private var _bearing = RouteUtiles.getBearing1(lat1, lon1, lat2, lon2)

    /** 항로 간의 거리, 단위 km  */
    private var _distance = RouteUtiles.geoDistanceGreatCircle1(lat1, lon1, lat2, lon2)

    /** 해당 항로의 왼쪽 한계 거리, 단위 km  */
    private var _portsideXTD = 0.0

    /** 해당 항로의 오른쪽 한계 거리, 단위 km  */
    private var _starboardXTD = 0.0

    /** 운항 속력, 단위 knot  */
    private var _speed = 0.0

    /** 선회율, 단위 degree per minute  */
    private var _turnRadius = 0.0

    /** 항로 상의 위치 판단을 위한 2개의 기준점  */
    private var _nvgPt1: Point2D? = null

    /** 항로 상의 위치 판단을 위한 2개의 기준점  */
    private var _nvgPt2: Point2D? = null

    fun getPortsideXTD(): Double { return _portsideXTD.toDouble() }

    fun getStarboardXTD(): Double { return _starboardXTD.toDouble() }

    fun getSpeed(): Double { return _speed }

    fun getTurnRadius(): Double { return _turnRadius }

    private fun setPortsideXTD(xtd: Double) { _portsideXTD = xtd }

    private fun setStarboardXTD(xtd: Double) { _starboardXTD = xtd }

    fun setSpeed(speed: Double) { _speed = speed }

    fun setTurnRadius(radius: Double) { _turnRadius = radius }

    fun getNVGPT1(): Point2D { return _nvgPt1!! }

    fun getNVGPT2(): Point2D { return _nvgPt2!! }

    fun setNVGPT1(pt: Point2D) { _nvgPt1 = pt}

    fun setNVGPT2(pt: Point2D) { _nvgPt2 = pt}

    //생성자
    init {
        setPortsideXTD(1.0) // 1km
        setStarboardXTD(1.0) // 1km
        setSpeed(20.0) // 20 knot
        setTurnRadius(1.0) // 1 degree per minute
        calculateNVGPT(lat1, lon1, lat2, lon2)
    }

    //지정한 위치로 WI를 갱신
    fun refresh(lat1: Double, lon1: Double, lat2: Double, lon2: Double) {
        _bearing = RouteUtiles.getBearing1(lat1, lon1, lat2, lon2)
        _distance = RouteUtiles.geoDistanceGreatCircle1(lat1, lon1, lat2, lon2)
        calculateNVGPT(lat1, lon1, lat2, lon2)
    }

    // 항로 상의 위치 판단을 위한 2개의 기준점 계산
    private fun calculateNVGPT(lat1: Double, lon1: Double, lat2: Double, lon2: Double) {
        when {
            lon1 == lon2 -> {
                if (lat1 < lat2) {
                    _nvgPt1 = Point2D(lon1, lat1 + DDG)
                    _nvgPt2 = Point2D(lon2, lat2 - DDG)
                } else {
                    _nvgPt1 = Point2D(lon1, lat1 - DDG)
                    _nvgPt2 = Point2D(lon2, lat2 + DDG)
                }
            }
            lat1 == lat2 -> {
                if (lon1 < lon2) {
                    _nvgPt1 = Point2D(lon1 + DDG, lat1)
                    _nvgPt2 = Point2D(lon2 - DDG, lat2)
                } else {
                    _nvgPt1 = Point2D(lon1 - DDG, lat1)
                    _nvgPt2 = Point2D(lon2 + DDG, lat2)
                }
            }
            else -> {
                val angle: Double = RouteUtiles.getBearing1(lat1, lon1, lat2, lon2)
                val xCos: Double = cos(angle)
                val ySin: Double = sin(angle)
                _nvgPt1 = Point2D(lon1 + xCos * DDG, lat1 + ySin * DDG)
                _nvgPt2 = Point2D(lon2 - xCos * DDG, lat2 - ySin * DDG)
            }
        }
    }

    /** XTD: Cross-track Distance, 설정한 항로 중앙과 자선과의 거리, 반환값 m
     * @param location 현재 위치(위도, 경도)
     * @return XTD km 단위
     */
    // Calculates the length of a perpendicular line from a point to a line connecting two other points
    fun getXTD(location: Point2D): Double {
        if (_nvgPt1 == null || _nvgPt2 == null) {
            return 0.0
        } else if (location.x == _nvgPt1!!.x && location.y == _nvgPt1!!.y || location.x == _nvgPt2!!.x && location.y == _nvgPt2!!.y) {
            return 0.0
        }

        // Check if the longitude difference between _nvgPt1!! and _nvgPt2 exceeds 180 degrees
        if (abs(_nvgPt2!!.x - _nvgPt1!!.x) > 180) {
            if (_nvgPt2!!.x > _nvgPt1!!.x) {
                _nvgPt1!!.x = _nvgPt1!!.x - 360
            } else {
                _nvgPt2!!.x = _nvgPt2!!.x + 360
            }
        }

        // Calculate the azimuth angle from _nvgPt1 to _nvgPt2
        val theta12 = atan2(
            sin(toRadians(_nvgPt2!!.x - _nvgPt1!!.x)) * cos(
                toRadians(
                    _nvgPt2!!.y
                )
            ),
            cos(toRadians(_nvgPt1!!.y)) * sin(toRadians(_nvgPt2!!.y)) -
                    sin(toRadians(_nvgPt1!!.y)) * cos(toRadians(_nvgPt2!!.y)) * cos(
                toRadians(
                    _nvgPt2!!.x - _nvgPt1!!.x
                )
            )
        )

        // Calculate the great circle distance and azimuth angle from _nvgPt1 to location
        val d13: Double = RouteUtiles.geoDistanceGreatCircle2(_nvgPt1!!, location)
        val theta13 = atan2(
            sin(toRadians(location.x - _nvgPt1!!.x)) * cos(toRadians(location.y)),
            cos(toRadians(_nvgPt1!!.y)) * sin(toRadians(location.y)) -
                    sin(toRadians(_nvgPt1!!.y)) * cos(toRadians(location.y)) * cos(
                toRadians(
                    location.x - _nvgPt1!!.x
                )
            )
        )

        // Calculate the perpendicular distance from location to the line connecting _nvgPt1 and _nvgPt2
        return abs(asin(sin(d13 / 6371) * sin(theta13 - theta12)) * 6371)
    }

    companion object{

    }
    //클론 생성자
    //현재 WI의 속성을 복사하고 지정한 새로운 위치의 WI를 생성하여 반환
    fun clone(lat1: Double, lon1: Double, lat2: Double, lon2: Double): WayInterval {
        val wi = WayInterval(lat1, lon1, lat2, lon2)
        wi._portsideXTD = _portsideXTD
        wi._starboardXTD = _starboardXTD
        wi._speed = _speed
        wi._turnRadius = _turnRadius
        return wi
    }


    /** 항로 각도 반환  */
    fun getBearing(): Double { return _bearing }

    /** 항로 간 거리 반환(Km)  */
    fun getDistance(): Double { return _distance }

    /** 예상 운항 시간 반환  */
    fun getTravelTimeAsHours(): Double {
        return _distance / (_speed / MSFINAL.KMTONMRATE)
    }

    enum class SideOfWay(val value: Int) {
        PORTOUT(-2), PORTIN(-1), NONE(0), STARBOARDIN(1), STARBOARDOUT(2)

    }
}