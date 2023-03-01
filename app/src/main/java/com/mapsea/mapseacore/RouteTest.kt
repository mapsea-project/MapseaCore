package com.mapsea.mapseacore

import com.mapsea.mapseacore.Route
import com.mapsea.mapseacore.Point2D
import com.mapsea.mapseacore.WayInterval
import com.mapsea.mapseacore.Vessel
import com.mapsea.mapseacore.SubMapDistance

fun main() {
    val route = Route()

    println(route.WayPointsLength())
    route.Add(Point2D(127.7509, 34.7520))
    route.Add(Point2D(128.7509, 35.7520))
    route.Add(Point2D(129.7509, 36.7520))
    println(route.WayPointsLength())

    // 웨이포인트 삭제
    route.Delete(1)
    println(route.WayPointsLength())

    // 웨이포인트 조회 반환
    for (i in 0 until route.WayPointsLength()) {
        println("WayPoint $i : x = ${route.GetWayPoint(i).X}, y = ${route.GetWayPoint(i).Y}")
    }


//    // generate 10 points
//    // this point draw `U` shape
//    for (i in 1..10) {
//        route.Add(Point2D(127.7509 + i * 0.1, 34.7520 + i * 0.1), 0)
//        route.Add(Point2D(127.7509 + i * 0.1, 34.7520 - i * 0.1), 0)
//    }
}