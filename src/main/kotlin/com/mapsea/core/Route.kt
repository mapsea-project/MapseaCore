package com.mapsea.core

import java.time.Duration
import java.time.LocalDateTime
import java.util.*


class Route {

    private var _wayPoints = mutableListOf<Point2D>()
    fun getWayPointCount() = _wayPoints.size
    fun getWayPoint(index: Int) = _wayPoints[index]
    fun getWayPointList(): List<Point2D> = _wayPoints

    private var _intervals: MutableList<WayInterval> = mutableListOf() // 웨이포인트 간 운항 속성 클래스
    fun getWayInterval(index: Int): WayInterval = _intervals[index]
    fun getWayIntervalList(): List<WayInterval> = _intervals
    fun getWayIntervalCount(): Int = _intervals.size

    var _averageSpeed: Double = 20.0
        set(value) { field = value }

    private var _timeToGo: Double = 0.0 //예상 운항 시간 Hour 단위
    private var _totalDistance: Double = 0.0 //총 거리 Km단위
    private var _remainDistance: Double = 0.0 //남은 거리 Km단위
    private var _departureTime: LocalDateTime? = null //출발 시각
    private var _arrivalTime: LocalDateTime? = null //도착 예정 시각
    private var _name: String? = null //항로 이름



    fun getTotalDistance(): Double { return _totalDistance }
    fun getTimeToGo(): Double { return _timeToGo }
    fun setDepartureTime(time: LocalDateTime) { _departureTime = time }
    fun getDepartureTime(): LocalDateTime? { return _departureTime }

    // 생성자
    init {
        _wayPoints = ArrayList()
        _intervals = ArrayList()
    }

    fun getWayPointArray(i: Int): Point2D {
        return _wayPoints[i]
    }

    /** 항로에 웨이포인트를 추가함.
     * @param coor Point2D 또는 List<Point2D> 타입의 인자를 받음
     * @param index 추가할 위치 (선택적)
     * */
    fun addWayPoints(coor: Any, index: Int = -1) {
        when (coor) {
            is Point2D -> addSingleWayPoint(coor, index)
            is List<*> -> addMultipleWayPoints(coor.filterIsInstance<Point2D>(), index)
            else -> throw IllegalArgumentException("Argument type is not supported.")
        }
    }

    private fun addSingleWayPoint(wayPoint: Point2D, index: Int = -1) {
        if (index != -1) {
            throw IllegalArgumentException("Index is not applicable for a single waypoint.")
        }

        _wayPoints.add(wayPoint)

        if (_wayPoints.size > 1) {
            _intervals.add(
                WayInterval(
                    _wayPoints[_wayPoints.size - 2].y,
                    _wayPoints[_wayPoints.size - 2].x,
                    _wayPoints[_wayPoints.size - 1].y,
                    _wayPoints[_wayPoints.size - 1].x
                )
            )

            calculateRouteInfo()
        }
    }

    private fun addMultipleWayPoints(wayPoints: List<Point2D>, index: Int = -1) {
        if (index < -1 || index > _wayPoints.size) {
            throw IndexOutOfBoundsException("Index is out of bounds.")
        }

        wayPoints.forEachIndexed { i, wayPoint ->
            if (index == -1) {
                addSingleWayPoint(wayPoint)
            } else {
                _wayPoints.add(index + i, wayPoint)

                if (_wayPoints.size == 2) {
                    _intervals.add(
                        WayInterval(
                            _wayPoints[0].y,
                            _wayPoints[0].x,
                            _wayPoints[1].y,
                            _wayPoints[1].x
                        )
                    )
                } else if (_wayPoints.size > 2) {
                    val tmpWI = if (index >= _intervals.size) {
                        WayInterval(
                            _wayPoints[_wayPoints.size - 2].y,
                            _wayPoints[_wayPoints.size - 2].x,
                            _wayPoints[_wayPoints.size - 1].y,
                            _wayPoints[_wayPoints.size - 1].x
                        )
                    } else {
                        _intervals[index + i].clone(
                            _wayPoints[index + i].y,
                            _wayPoints[index + i].x,
                            _wayPoints[index + i + 1].y,
                            _wayPoints[index + i + 1].x
                        )
                    }

                    _intervals.add(index + i, tmpWI)
                    refreshWayIntervals(index + i)
                }

                calculateRouteInfo()
            }
        }
    }


    //항로 전체의 정보를 갱신함. 일반적으로 웨이포인트가 추가, 변경 되었을 때 호출함
    private fun calculateRouteInfo() {
        if (_wayPoints.size > 1) {
            var tmpDis = 0.0
            val tmpTime = 0.0
            for (interval in _intervals) {
                tmpDis = interval.getDistance()
                //              tmpTime = interval.GetTravelTimeAsHours();
            }
            _totalDistance = tmpDis
            _timeToGo = tmpDis / _averageSpeed * MSFINAL.KMTONMRATE
            //            _timeToGo = tmpTime;
            //  _avgSpd = tmpDis / tmpTime * MSFINAL.KMTONMRATE;
            if (_departureTime != null) {
                _arrivalTime = _departureTime
                val hours = _timeToGo.toLong()
                _arrivalTime!!.plusHours(hours)
                var fpTime = _timeToGo - hours.toDouble()
                fpTime *= 3600.0
                _arrivalTime!!.plusSeconds(fpTime.toLong())
            }
        }
    }

    fun getRemainDistance(curLocation: Point2D): Double {
        _remainDistance = _totalDistance
        _remainDistance += RouteUtiles.geoDistanceAuto2(curLocation, _wayPoints[0])
        return _remainDistance
    }

    private fun refreshWayIntervals(index: Int) {
        if (index > 0) {
            _intervals[index - 1].refresh(
                _wayPoints[index - 1].y,
                _wayPoints[index - 1].x,
                _wayPoints[index].y,
                _wayPoints[index].x
            )
        }
        if (index < _wayPoints.size - 1) {
            _intervals[index].refresh(
                _wayPoints[index].y,
                _wayPoints[index].x,
                _wayPoints[index + 1].y,
                _wayPoints[index + 1].x
            )
        }
    }

    //웨이포인트 삭제
    fun deleteWaypoint(index: Int): Boolean {
        if (index >= _wayPoints.size) {
            return false
        }
        _wayPoints.removeAt(index)
        if (_intervals.size > 0) {
            val intervalIndex = if (index == _intervals.size) index - 1 else index
            _intervals.removeAt(intervalIndex)
        }
        refreshWayIntervals(index)
        calculateRouteInfo()
        return true
    }

    /** 현재 배 위치에서 첫 웨이포인트까지의 남은 거리 반환(km)  */
    fun distanceToFirstWayPoint(currentLocation: Point2D): Double {
        return RouteUtiles.geoDistanceGreatCircle2(currentLocation, _wayPoints[0])
    }

    /** 현재 배 위치에서 첫 웨이포인트까지의 방위각 반환(°, degree)  */
    fun bearingToFirstWayPoint(currentLocation: Point2D): Double {
        return RouteUtiles.getBearing2(currentLocation, _wayPoints[0])
    }

    /** 해당 위경도 위치가 속하는 웨이포인트의 순서를 반환<br>
    내비게이션 중에 항로 상의 어디에 위치에 있는지 판단함 */
    fun wayIntervalOrderInRoute(location: Point2D): Int {
        if (_intervals.size > 0) {
            var wiIndex = 0
            var minDis: Double = RouteUtiles.geoDistanceAuto1(
                location.y, location.x,
                _intervals[0].getNVGPT1().y, _intervals[0].getNVGPT1().x
            )
            for (i in _intervals.indices) {
                val pt1Dis: Double = RouteUtiles.geoDistanceAuto1(
                    location.y, location.x,
                    _intervals[i].getNVGPT1().y, _intervals[i].getNVGPT1().x
                )
                val pt2Dis: Double = RouteUtiles.geoDistanceAuto1(
                    location.y, location.x,
                    _intervals[i].getNVGPT2().y, _intervals[i].getNVGPT2().x
                )
                if (pt1Dis < minDis) {
                    minDis = pt1Dis
                    wiIndex = i
                }
                if (pt2Dis < minDis) {
                    minDis = pt2Dis
                    wiIndex = i
                }
                if (i == _intervals.size - 1 && wiIndex == i) {
                    val arvDis: Double = RouteUtiles.geoDistanceAuto1(
                        location.y, location.x,
                        _wayPoints[_wayPoints.size - 1].y,
                        _wayPoints[_wayPoints.size - 1].x
                    )
                    if (arvDis < MSFINAL.NAVIGATION_ARRIVAL_DISTANCE) {
                        return -2 //목적지 도착 내비게이션 종료
                    }
                }
            }
            return wiIndex
        }
        return -1
    }


    //해당 위치의 XTD 값을 반환 반환값 단위 Km
    fun getXTD(wayIntervalOrder: Int, location: Point2D): Double {
        return _intervals[wayIntervalOrder].getXTD(location)
    }


    //지정 웨이포인트 도착 예정 시각
    fun WPETA(wayIntervalOrder: Int): LocalDateTime? {
        if (_departureTime != null) {
            var tmpTime = _departureTime as LocalDateTime
            var tmpHours = 0.0
            for (i in 0..wayIntervalOrder) {
                tmpHours += _intervals[i].getDistance() / _intervals[i].getSpeed() * MSFINAL.KMTONMRATE
            }
            val hours = tmpHours.toLong()
            tmpTime = tmpTime.plusHours(hours).plusSeconds(((tmpHours - hours) * 3600.0).toLong())
            return tmpTime
        }
        return null
    }



    //임시로 지정한 속력과 도착 예정 시각을 이용한 총 운항 시간 계산, 반환값 Hour 단위
    fun getTTA(STG: Double): Double {
        return _totalDistance / STG * MSFINAL.KMTONMRATE
    }

    //임시로 지정한 속력과 목적지 위치를 이용한 도착 예정 시각 계산
    fun STGToPTA(STG: Double): LocalDateTime {
        val tmpTime = getDepartureTime() as LocalDateTime
        val tmpHours = getTTA(STG)
        val hours = tmpHours.toLong()
        tmpTime.plusHours(hours)
        var fpTime = tmpHours - hours.toDouble()
        fpTime *= 3600.0
        tmpTime.plusSeconds(fpTime.toLong())
        return tmpTime
    }

    //임시로 지정한 목적지 위치와 도착 예정 시각을 이용한 속력 계산, 반환값 단위 Km/h
    fun PTAToSTG(PTA: LocalDateTime?): Double {
        val duration: Duration = Duration.between(_departureTime, PTA)
        return _totalDistance / (duration.seconds.toDouble() / 3600.0)
    }

    //항로 상의 목적지까지 남은 거리, 반환값 단위 Km
    fun getRemainedDistance(wayIntervalOrder: Int, latitude: Double, longitude: Double): Double {
        var ttlDis: Double = RouteUtiles.geoDistanceGreatCircle1(
            latitude, longitude,
            _wayPoints[wayIntervalOrder + 1].y, _wayPoints[wayIntervalOrder + 1].x
        )
        for (i in wayIntervalOrder + 1 until _intervals.size) {
            ttlDis += _intervals[i].getDistance()
        }
        return ttlDis
    }

    //항로 상의 목적지까지 남은 운항 시간, 반환값 단위 Hour
    fun getRemainedTimeToGo(wayIntervalOrder: Int, latitude: Double, longitude: Double): Double {
        var ttlTime: Double = (RouteUtiles.geoDistanceGreatCircle1(
            latitude, longitude,
            _wayPoints[wayIntervalOrder + 1].y, _wayPoints[wayIntervalOrder + 1].x
        )
                / _intervals[wayIntervalOrder].getSpeed() / MSFINAL.KMTONMRATE)
        for (i in wayIntervalOrder + 1 until _intervals.size) {
            ttlTime += _intervals[i].getTravelTimeAsHours()
        }
        return ttlTime
    }

    fun getSideOfWayInterval(wayInterval: WayInterval, testPoint: Point2D): Int {
        val portXTD = wayInterval.getPortsideXTD().toDouble()
        val starboardXTD = wayInterval.getStarboardXTD().toDouble()
        val xtd = wayInterval.getXTD(testPoint)
        val start = wayInterval.getNVGPT1()
        val bearing = wayInterval.getBearing()
        val angle: Double = RouteUtiles.getBearing2(p1 = start, p2 = testPoint)
        val angleDiff = calAngleDiff(boringAngle = bearing, targetAngle = angle)
//        println("start: %.5f, %.5f, end: %.5f, %.5f\n", start!!.x, start.y, end!!.x, end.y)
//        println("bearing: %.5f, angle: %.5f, angleDiff: %.5f\n", bearing, angle, angleDiff)
        return when {
            (angleDiff < 0) && (portXTD < xtd) -> {
                println("Port side && out of XTD")
                WayInterval.SideOfWay.PORTOUT.value
            }
            (angleDiff < 0) && (portXTD > xtd) -> {
                println("Port side && in XTD")
                WayInterval.SideOfWay.PORTIN.value
            }
            (angleDiff > 0) && (starboardXTD < xtd) -> {
                println("Starboard side && out of XTD")
                WayInterval.SideOfWay.STARBOARDOUT.value
            }
            (angleDiff > 0) && (starboardXTD > xtd) -> {
                println("Starboard side && in XTD")
                WayInterval.SideOfWay.STARBOARDIN.value
            }
            else -> {
                WayInterval.SideOfWay.NONE.value
            }
        }
    }

    fun getSideOfWayInterval(location: Point2D): Int {
        return when (val currentWayIntervalOrder: Int = wayIntervalOrderInRoute(location)) {
            -1, -2 -> {
                currentWayIntervalOrder
            }
            else -> getSideOfWayInterval(_intervals[currentWayIntervalOrder], location)
        }
    }

    private fun calAngleDiff(boringAngle: Double, targetAngle: Double): Double {
        // normalize way angle
        var normWayAngle = Math.toRadians(boringAngle)
        while (normWayAngle > Math.PI) normWayAngle -= 2 * Math.PI
        while (normWayAngle < -Math.PI) normWayAngle += 2 * Math.PI

        // normalize target angle
        var normTargetAngle = Math.toRadians(targetAngle)
        while (normTargetAngle > Math.PI) normTargetAngle -= 2 * Math.PI
        while (normTargetAngle < -Math.PI) normTargetAngle += 2 * Math.PI

        // calculate angle difference
        var result = Math.toDegrees(normTargetAngle - normWayAngle)
        if (result > 180.0) {
            result -= Math.toDegrees(2 * Math.PI)
        } else if (result < -180.0) {
            result += Math.toDegrees(2 * Math.PI)
        }
        return result
    }

    fun addRandomWayPoint(num: Int, seed: Int) {
        val routeRand = Random(seed.toLong())
        require(num >= 2) { "WayPoint num must be greater than 2" }
        for (i in 1..num) {
            // x(longitude): -180 ~ 180, y(latitude): -90 ~ 90
            addWayPoints(
                Point2D(
                    routeRand.doubles(-180.0, 180.0).findFirst().asDouble,
                    routeRand.doubles(-90.0, 90.0).findFirst().asDouble
                )
            )
            val size = _wayPoints.size
            println("WayPoint " + (size - 1) + ": " + _wayPoints[size - 1].x + ", " + _wayPoints[size - 1].y)
        }
    }
}