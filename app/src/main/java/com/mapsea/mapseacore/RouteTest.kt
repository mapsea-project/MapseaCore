package com.mapsea.mapseacore

import com.mapsea.mapseacore.WayInterval.SideOfWay
import kotlin.math.PI
import kotlin.random.Random

// longitude: x, latitude: y
// coordinates of cities in Korea

// 리스트 형태의 여러 웨이포인트 추가
fun addWayPointsList(route: Route, coorList: List<Point2D>) {
    for (coor in coorList) {
        route.Add(coor)
    }
}

fun main() {
    val route = Route()

    // 웨이포인트 기본 기능 테스트
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
    val coorInterOrder = listOf<Point2D>(
        Point2D(58.7136, 24.9664), // 24.9664, 58.7136
        Point2D(57.1574, 25.1046), // 25.1046, 57.1574
        Point2D(56.7689, 26.5513), // 26.5513, 56.7689
        Point2D(55.7296, 26.4480), // 26.4480, 55.7296
        Point2D(55.2266, 25.2391), // 25.2391, 55.2266
    )
    var coorTest = listOf<Point2D>(
        Point2D(55.22660, 25.23910, ), // Center // 25.23910, 55.22660 //0
        Point2D(55.22660, 25.24423, ), // 0     // 25.24423, 55.22660 //1
        Point2D(55.23107, 25.24416, ), // 38.6  // 25.24416, 55.23107 //2
        Point2D(55.23107, 25.23910, ), // 90    // 25.23910, 55.23107 //3
        Point2D(55.23265, 25.23381, ), // 134   // 25.23381, 55.23265 //4
        Point2D(55.22660, 25.22000, ), // 180   // 25.22000, 55.22660 //5
        Point2D(55.22141, 25.24440, ), // -42   // 25.24440, 55.22141 //6
        Point2D(55.21000, 25.23910, ), // -90   // 25.23910, 55.21000 //7
        Point2D(55.22145, 25.23528, ), // -130  // 23.23528, 55.22145 //8
    )
    route.Add(coorTest[0]) // Center
    route.Add(coorTest[4]) //
    val testPoint = coorTest[8]

    /*
    // 웨이포인트 경로 항해 테스트
    // 경로 순서 테스트(from 오만 만 to 두바이)
    val testPoint = Point2D(55.7296, 26.4480) // 경로 중간 지점
    addWayPointsList(route, coorInterOrder)*/

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
    val (_,_,hours, minutes, seconds) = getTimeUnits(time)
    // timeH: integer part of time

    println("Average Speed: ${fN(speed)} knots")
    println("Total Distance: ${fN(dist)} km")
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
        val timeUnits = getTimeUnits(expectedTime) // Months, Days, Hours, Minutes, Seconds
        println("WayInterval $i : Distance = ${fN(distance)} km" +
                ", Bearing = [${fN(bearing)} deg, ${fN(Math.toRadians(bearing))} rad], ")
        println("Bearing2 = ${MainActivity.Bearing2(wayInterval.GetNVGPT1(), wayInterval.GetNVGPT2())}")
        // expectedTime: double, timeUnits: int array Months:Days:Hours:Minutes:Seconds
        println("Expected Time to Go = ${fN(expectedTime)} h, " +
                "${fN(timeUnits[0])}:${fN(timeUnits[1])}:${fN(timeUnits[2])}:${fN(timeUnits[3])}:${fN(timeUnits[4])}")
    }

    println("------------------ XTD ------------------")
    println("Test Point: ${testPoint.X}, ${testPoint.Y}")
    // 경로 너비 조회
    for (i in 0 until route.WayPointsLength() - 1) {
        val wayInterval = route.GetWayInterval(i)
        println("portXTD(left) ${wayInterval._portsideXTD} m, starboardXTD(right) ${wayInterval._starboardXTD} m")
        println("WayInterval $i : XTD = ${fN(wayInterval.GetXTD(testPoint))} m")
    }

    println("------------------ SideOfWay ------------------")
    println("${SideOfWay.PORTOUT}: -2, ${SideOfWay.PORTIN}: -1, " +
            "${SideOfWay.STARBOARDOUT}: 2, ${SideOfWay.STARBOARDIN}: 1, " +
            "${SideOfWay.NONE}: 0")
    println("SideOfWay: ${getSideOfWayInterval(route.GetWayInterval(0), testPoint)}")

    println("------------------ Get WayInterval Order In Route by Point ------------------")
    println("WayInterval Order: ${route.WayIntervalOrderInRoute(testPoint)}")
}

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

private const val MINUTES_PER_HOUR = 60
private const val SECONDS_PER_MINUTE = 60
private const val DAYS_PER_MONTH = 30

/**
 * @param hours: Double
 * @return List<Number>: [hours, minutes, seconds, days, month]
 */
fun getTimeUnits(hours: Double): List<Int> {
    val totalMinutes = (hours * MINUTES_PER_HOUR).toInt()
    val totalSeconds = (hours * MINUTES_PER_HOUR * SECONDS_PER_MINUTE).toInt()
    val daysInt = (hours / 24).toInt()
    val monthsInt = (daysInt / DAYS_PER_MONTH)

    val minutesInt = totalMinutes % MINUTES_PER_HOUR
    val secondsInt = totalSeconds % SECONDS_PER_MINUTE
    val hoursInt = hours.toInt() % 24

    return listOf(monthsInt, daysInt, hoursInt, minutesInt, secondsInt)
}

/**
 * @param wayInterval: WayInterval
 * @param testPoint: Point2D
 * @return Int of SideOfWay enum Value (0: NONE, -2: PORTOUT, -1: PORTIN, 1: STARBOARDIN, 2: STARBOARDOUT)
 */
fun getSideOfWayInterval(wayInterval: WayInterval, testPoint: Point2D): Int {
    val portXTD = wayInterval._portsideXTD
    val starboardXTD = wayInterval._starboardXTD
    val xtd = wayInterval.GetXTD(testPoint)

    val start = wayInterval._nvgPt1
    val end = wayInterval._nvgPt2
    val bearing = wayInterval.GetBearing()

    val angle = MainActivity.Bearing2(start, testPoint)

    val angleDiff = calAngleDiff(bearing, angle)

    println("start: ${fN(start.X)}, ${fN(start.Y)}, end: ${fN(end.X)}, ${fN(end.Y)}")
    println("bearing: ${fN(bearing)}, angle: ${fN(angle)}, angleDiff: ${fN(angleDiff)}")

    when {
        angleDiff < 0 && (wayInterval._portsideXTD < wayInterval.GetXTD(testPoint))
        -> {println("Port side && out of XTD")
            return SideOfWay.PORTOUT.value
        }
        angleDiff < 0 && (wayInterval._portsideXTD > wayInterval.GetXTD(testPoint))
        -> {println("Port side && in XTD")
            return SideOfWay.PORTIN.value
        }
        angleDiff > 0 && (wayInterval._starboardXTD < wayInterval.GetXTD(testPoint))
        -> { println("Starboard side && out of XTD")
            return SideOfWay.STARBOARDOUT.value
        }
        angleDiff > 0 && (wayInterval._starboardXTD > wayInterval.GetXTD(testPoint))
        -> { println("Starboard side && in XTD")
            return SideOfWay.STARBOARDIN.value
        }
        else -> return WayInterval.SideOfWay.NONE.value
    }
}

/**
 * @param boringAngle: Double
 * @param targetAngle: Double
 * @return angle difference: Double
 */
fun calAngleDiff(boringAngle: Double, targetAngle: Double = 0.0): Double {
    // normalize way angle
    var normWayAngle = Math.toRadians(boringAngle)
    while (normWayAngle > PI) normWayAngle -= 2 * PI
    while (normWayAngle < -PI) normWayAngle += 2 * PI

    // normalize target angle
    var normTargetAngle = Math.toRadians(targetAngle)
    while (normTargetAngle > PI) normTargetAngle -= 2 * PI
    while (normTargetAngle < -PI) normTargetAngle += 2 * PI

    // calculate angle difference
    var result =  Math.toDegrees(normTargetAngle - normWayAngle)
    if (result > 180.0) {
        result -= Math.toDegrees(2 * PI)
    } else if (result < -180.0) {
        result += Math.toDegrees(2 * PI)
    }
    return result
}