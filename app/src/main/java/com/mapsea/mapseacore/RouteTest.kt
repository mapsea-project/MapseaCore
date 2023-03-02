package com.mapsea.mapseacore

import kotlin.math.PI
import kotlin.random.Random

// longitude: x, latitude: y
// coordinates of cities in Korea

fun main() {
    val coorKorea = listOf(
        Point2D(126.977966, 37.566536), // Seoul
        Point2D(129.075638, 35.179554), // Busan
        Point2D(128.87, 37.75), // Gangneung
        Point2D(126.70, 37.45), // Incheon
        Point2D(129.31, 35.53), // Ulsan
        Point2D(126.85, 35.15), // Gwangju
        Point2D(128.60, 35.87), // Daegu
    )

    val coorWorld = listOf(
        Point2D(121.47, 31.23), // Shanghai
        Point2D(139.76, 35.70), // Tokyo
        Point2D(-122.26, 38.22), // San Francisco
        Point2D(151.60, -32.95), // Sydney
        Point2D(179.58, 33.83), // 태평양 자오선 왼쪽
        Point2D(-178.01, 29.21),
    )
    val route = Route()

//    addRandomWayPoint(route, 5)
    route.Add(coorWorld[1]) // Tokyo
    route.Add(coorWorld[2]) // San Francisco
//    val testPoint = coorKorea[0] // Seoul
    val testPoint = Point2D(coorWorld[1].x +5, coorWorld[1].y +5)
    println("WayPoint Counts: ${route.WayPointsLength()}") // 웨이포인트 개수

    // 웨이포인트 삭제
//    val delPos = 1 // 삭제할 포인트 인덱스 0 ~ WayPointsLength()-1
//    println("Delete WayPoint $delPos")
//    route.Delete(delPos)
//    println("WayPoint remain len: ${route.WayPointsLength()}")

    // 선박 평균 속력(knot), 예상 운항 시각(h), 총 거리(km) 반환
    val speed: Double = route.GetAverageSpeed()
    val dist: Double = route.GetTotalDistance()
    val time: Double = route.GetTimeToGo() // 예상 운항 시간(h)
    val (hours, minutes, seconds) = getTimeUnits(time)
    // timeH: integer part of time

    println("Average Speed: ${sD(speed)} knots")
    println("Total Distance: ${sD(dist)} km")
    println("Total Time: ${"%.5f".format(time)} h, $hours hours, $minutes minutes, $seconds seconds")

    println("------------------ WayPoints ------------------")
    // 웨이포인트 조회 반환
    for (i in 0 until route.WayPointsLength()) {
        val latitude = route.GetWayPoint(i).Y
        val longitude = route.GetWayPoint(i).X
        println("WayPoint $i : longitude:latitude = $longitude:$latitude")
    }

    println("------------------ WayIntervals ------------------")
    // 웨이포인트 간 속성 조회 반환. 인덱스는 웨이포인트 - 1. output format .4f
    for (i in 0 until route.WayPointsLength() - 1) {
        val wayInterval = route.GetWayInterval(i)
        val distance = wayInterval.GetDistance()
        val bearing = wayInterval.GetBearing()
        val expectedTime = wayInterval.GetTravelTimeAsHours()
        val expectedTimeUnits = getTimeUnits(expectedTime)
        println("WayInterval $i : Distance = ${sD(distance)} km" +
                ", Bearing = [${sD(bearing)} deg, ${sD(Math.toRadians(bearing))} rad], "+
        "Expected Time to Go = ${sD(expectedTime)} h, " +
                "${"%02d".format(expectedTimeUnits[0])}:" +
                "${"%02d".format(expectedTimeUnits[1])}:" +
                "%02d".format(expectedTimeUnits[2]))
    }

    println("------------------ XTD ------------------")
    println("Test Point: ${testPoint.X}, ${testPoint.Y}")
    // 경로 너비 조회
    for (i in 0 until route.WayPointsLength() - 1) {
        val wayInterval = route.GetWayInterval(i)
        println("portXTD(left) ${wayInterval._portsideXTD} m, starboardXTD(right) ${wayInterval._starboardXTD} m")
        println("WayInterval $i : XTD = ${sD(wayInterval.GetXTD(testPoint))} m")
    }

    getSideOfWayInterval(route.GetWayInterval(0), testPoint)

//    val currentVessel = Vessel()
//    currentVessel._pos = PositionData()
//    currentVessel._pos._lon = -1.0
//    currentVessel._pos._lat = 0.0
//    currentVessel._pos._stp = LocalDateTime.now()
//    currentVessel._pos._status = 1
//    currentVessel._prevPos = PositionData()
//    currentVessel._prevPos._lon = -1.0
//    currentVessel._prevPos._lat = 0.0
//    currentVessel._prevPos._stp = currentVessel._pos._stp.plusSeconds(30)

}

/** Double format .5f */
fun sD(d: Double, num: Int = 5): String {
    return "%.${num}f".format(d)
}
/** Int format %.02d */
fun sI(i: Int, num: Int = 2): String {
    return "%.0${num}d".format(i)
}

fun addRandomWayPoint(route: Route, num: Int = 2, seed: Int = 42): Route {
    val routeRand = Random(seed)

    if (num < 2) {
        throw IllegalArgumentException("WayPoint num must be greater than 2")
    }

    for (i in 1..num) {
        // x(longitude): -180 ~ 180, y(latitude): -90 ~ 90
        route.Add(Point2D(
            routeRand.nextDouble(-180.0, 180.0),
            routeRand.nextDouble(-90.0, 90.0)))
    }
    return route
}

/**
 * @param hours: Double
 * @return List<Number>: [hours, minutes, seconds, days, month]
 */
fun getTimeUnits(hours: Double): List<Number> {
    val hourInt = hours.toInt()
    val minutes = ((hours - hours.toInt())*60)
    val minutesInt = minutes.toInt()
    val seconds = ((minutes - minutes.toInt())*60)
    val secondsInt = seconds.toInt()
    val daysInt = (hours/24).toInt()
    val monthsInt = daysInt/30
    return listOf(hourInt, minutesInt, secondsInt, daysInt, monthsInt)
}

fun getSideOfWayInterval(wayInterval: WayInterval, testPoint: Point2D) {
    val portXTD = wayInterval._portsideXTD
    val starboardXTD = wayInterval._starboardXTD
    val xtd = wayInterval.GetXTD(testPoint)

    val start = wayInterval._nvgPt1
    val end = wayInterval._nvgPt2
    val bearing = wayInterval.GetBearing()

//    val dx = testPoint.X - start.X
//    val dy = testPoint.Y - start.Y
//    val dx = MainActivity.GeoDistanceAuto(start.Y, start.X, testPoint.Y, testPoint.X)
//    val dy = MainActivity.GeoDistanceAuto(start.Y, start.X, testPoint.Y, testPoint.X)
//    val angle = atan2(dy, dx)
    val angle = MainActivity.Bearing(start.Y, start.X, testPoint.Y, testPoint.X)

    val angleDiff = normalizeAngle(bearing, angle)

    println("start: ${sD(start.X)}, ${sD(start.Y)}, end: ${sD(end.X)}, ${sD(end.Y)}")
    println("bearing: $bearing, angle: $angle, angleDiff: $angleDiff")



    when {
        angleDiff < 0 && (wayInterval._starboardXTD < wayInterval.GetXTD(testPoint))
        ->  println("starboard side && out of XTD")
        angleDiff < 0 && (wayInterval._starboardXTD > wayInterval.GetXTD(testPoint))
        ->  println("starboard side && in XTD")
        angleDiff > 0 && (wayInterval._portsideXTD < wayInterval.GetXTD(testPoint))
        ->  println("port side && out of XTD")
        angleDiff > 0 && (wayInterval._portsideXTD > wayInterval.GetXTD(testPoint))
        ->  println("port side && in XTD")
        else -> println("error")
    }
}

fun normalizeAngle(boringAngle: Double, targetAngle: Double = 0.0): Double {
    // normalize way angle
    var normWayAngle = boringAngle
    while (normWayAngle > PI) normWayAngle -= 2 * PI
    while (normWayAngle < -PI) normWayAngle += 2 * PI

    // normalize target angle
    var normTargetAngle = targetAngle
    while (normTargetAngle > PI) normTargetAngle -= 2 * PI
    while (normTargetAngle < -PI) normTargetAngle += 2 * PI

    // calculate angle difference
    val angleDiff = normTargetAngle - normWayAngle
    println("normWayAngle: $normWayAngle, normTargetAngle: $normTargetAngle")
    println("angleDiff: ${Math.toDegrees(angleDiff)}")
    return angleDiff
}