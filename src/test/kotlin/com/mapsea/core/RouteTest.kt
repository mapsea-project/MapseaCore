package com.mapsea.core

import org.junit.jupiter.api.Test

class RouteTest {
    @Test
    fun getRoute() {
        val route = Route()
        route.addWayPoints(Point2D(0.0, 0.0))
        route.addWayPoints(Point2D(1.0, 1.0))

        for (i in 0 until route.getWayPointCount()) {
            val wayPoint = route.getWayPoint(i)
            println("WayPoint $i: ${wayPoint.x}, ${wayPoint.y}")
        }
    }

    @Test
    fun getRoute2() {
        val route = Route()
        route.addWayPoints(Point2D(0.0, 0.0))
        route.addWayPoints(Point2D(1.0, 1.0))

        val wayPointTest = route.getWayPoint(0)
        println("WayPoint 0 - LAT: ${wayPointTest.y}, LON: ${wayPointTest.x}")
    }
}