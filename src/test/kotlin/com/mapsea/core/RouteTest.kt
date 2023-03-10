package com.route.lib

import org.junit.jupiter.api.Test

class RouteTest: RouteTestData() {
    @Test // 포인트 2개 추가 테스트
    fun startRoute() {
        route.addWayPoints(Point2D(0.0, 0.0))
        route.addWayPoints(Point2D(1.0, 1.0))

        assert(route.getWayPointCount() == 2)
        assert(route.getWayPoint(0).lon == 0.0)
        assert(route.getWayPoint(1).lon == 1.0)
        assert(route.getWayIntervalCount() == 1)
    }

    @Test // 포인트 3개를 담은 리스트 추가 테스트
    fun addListPoint2D() {

        route.addWayPoints(coorTest1)
        for (i in 0 until route.getWayPointCount()) {
            val wayPoint = route.getWayPoint(i)
            println("WayPoint $i: ${wayPoint.lon}, ${wayPoint.lat}")
        }
        assert(route.getWayPointCount() == 3) // 에러 통과 시 3 출력
        assert(route.getWayPoint(0).lon == 0.0)
        assert(route.getWayPoint(0).lat == 0.0)
        assert(route.getWayIntervalCount() == 2)
    }

    @Test // 포인트 3개를 담고 3개를 담은 리스트를 인덱스 1번 위치에 추가 테스트
    fun addListPoint2DIndex() {

        route.addWayPoints(coorTest1)
        route.addWayPoints(coorTest2, 1)
        for (i in 0 until route.getWayPointCount()) {
            val wayPoint = route.getWayPoint(i)
            println("WayPoint $i: ${wayPoint.lon}, ${wayPoint.lat}")
        }
        assert(route.getWayPointCount() == 6) // 6이면 통과
        assert(route.getWayPoint(0).lon == 0.0)
        assert(route.getWayPoint(1).lon == 3.0)
        assert(route.getWayIntervalCount() == 5)
    }

    @Test
    fun checkShipConfig() {
        route.addWayPoints(coorTest1)
        route._averageSpeed = 10.0 // default: 20.0
        val distanceTotal = route.getTotalDistance()
        val timeTotal = route.getTimeToGo()
        println("averageSpeed: ${route._averageSpeed} km/h")
        println("distanceTotal: $distanceTotal km")
        println("timeTotal: $timeTotal h")
    }

}