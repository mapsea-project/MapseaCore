package com.mapsea.mapseacore


import kotlin.random.Random

fun addRandomWayPoint(route: Route, num: Int = 2, seed: Int = 42): Route {
    val routeRand = Random(seed)

    if (num < 2) {
        throw IllegalArgumentException("WayPoint num must be greater than 2")
    }

    for (i in 1..num) {
        // longtitude range: -180 ~ 180, latitude range: -90 ~ 90
        route.Add(Point2D(
            routeRand.nextDouble(-180.0, 180.0), // 입력 값은 rad? or deg? ##
            routeRand.nextDouble(-90.0, 90.0)))
    }
    return route
}

fun main() {
    val route = Route()

    addRandomWayPoint(route, 5)

    println("WayPoint len: ${route.WayPointsLength()}")

    // 웨이포인트 삭제
//    val delPos = 1
//    println("Delete WayPoint $delPos")
//    route.Delete(delPos)
//    println("WayPoint remain len: ${route.WayPointsLength()}")

    // 선박 평균 속력(knot), 예상 운항 시각(h), 총 거리(km) 반환
    val speed:String = sD(route.GetAverageSpeed())
    val dist:String = sD(route.GetTotalDistance())
    println("Average Speed: $speed knots")
    println("Total Distance: $dist km")
    println("Total Time: ${sD(route.GetTimeToGo())} h") // 정확한 계산법 찾아야함 ##
    println((dist.toDouble()*0.539957)/speed.toDouble()*1/1.15078)

    // 웨이포인트 조회 반환
    for (i in 0 until route.WayPointsLength()) {
        println("WayPoint $i : X = ${sD(route.GetWayPoint(i).X)}" +
                ", Y = ${sD(route.GetWayPoint(i).Y)}")
    }


    // 웨이포인트 간 속성 조회 반환. 인덱스는 웨이포인트 - 1. output format .4f
    for (i in 0 until route.WayPointsLength() - 1) {
        println("WayInterval $i : Distance = ${sD(route.GetWayInterval(i).GetDistance())}" +
                ", Bearing = ${sD(route.GetWayInterval(i).GetBearing())}")
    }
}

/** Double format .4f */
fun sD(d: Double): String {
    return "%.4f".format(d)
}