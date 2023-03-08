/** Route plan API Test File */
package com.mapsea.core

import com.mapsea.core.RouteUtiles.Companion.fN

fun printTimeUnits(timeUnits: List<Int>) {
    println("timeUnits: ${fN(timeUnits[0])}:${fN(timeUnits[1])}:${fN(timeUnits[2])}:${fN(timeUnits[3])}:${fN(timeUnits[4])}")
}

fun main(){
    // 웨이포인트 기본 기능 테스트
    // Point2D 클래스 x는 longitude, y는 latitude 이다.

    val route = Route() // 웨이포인트 기본 클래스 생성

    // 웨이포인트 리스트 생성
    val coorTest = listOf<Point2D>(
        Point2D(58.7136, 24.9664), // Start     // 24.9664, 58.7136 //0
        Point2D(57.1574, 25.1046), // Second    // 25.1046, 57.1574 //1
        Point2D(56.7689, 26.5513), // Third     // 26.5513, 56.7689 //2
    )
    // 웨이포인트 추가
    route.addWayPoints(coorTest)

    /*
    // 두 번째 웨이포인트 리스트 생성
    val coorTest2 = listOf<Point2D>(
        Point2D(56.7689, 26.5511), // Third     // 26.5513, 56.7689 //2
        Point2D(56.7689, 26.5512), // Third     // 26.5513, 56.7689 //2
        Point2D(56.7689, 26.5513), // Third     // 26.5513, 56.7689 //2
    )
    // 1번 위치에 추가
    route.addWayPoints(coorTest2, 1)
    */

    // 배의 평균 속력(knot), 모든 웨이포인트 간 거리(km), 예상 운항 시간(h, M:D:h:m:s) 반환
    val speed: Double = route._averageSpeed         // 배의 평균 속력(knot)
    val distanceAll: Double = route.getTotalDistance()  // 모든 웨이포인트 간 거리(km)
    val time: Double = route.getTimeToGo()              // 예상 운항 시간(h)
    val timeUnits: List<Int> = getTimeUnits(time)       // 예상 운항 시간(M,D,h,m,s) 반환
    println("speed: $speed knot, distance: $distanceAll km, total time: $time h")
    printTimeUnits(timeUnits)


    // 웨이포인트 리스트 출력
    for (i in 0 until route.getWayPointCount()) {
        val wayPoint = route.getWayPoint(i)             // 웨이포인트 Point2D 클래스
        println("WayPoint $i: ${wayPoint.x}, ${wayPoint.y}")
    }

    val wayPointTest = route.getWayPoint(0) // 첫 번째 웨이포인트 Point2D 클래스
    println("WayPoint 0 - LAT: ${wayPointTest.y}, LON: ${wayPointTest.x}") // y는 latitude, x는 longitude

    route._averageSpeed = 10.0                         // 배의 평균 속력(knot) 설정

    val currentPoint = Point2D(57.98584, 26.5513)     // 현재 위치
    // 현재 위치에서 첫 번째 웨이포인트까지의 거리(km), 방위각(degree) 반환
    val distance: Double = route.distanceToFirstWayPoint(currentPoint) // 첫 번째 웨이포인트까지의 거리(km)
    val bearing: Double = route.bearingToFirstWayPoint(currentPoint)   // 첫 번째 웨이포인트까지의 방위각(degree)
    println("distance to the first waypoint: $distance km")
    println("bearing to the first waypoint: $bearing degree")

    // WayInterval(두 웨이포인트 사이의 속성) 정보 출력
    // WayInterval 은 웨이포인트 개수 - 1 만큼 존재한다. (웨이포인트 최소 2개 필요)
    // Distance 는 두 웨이포인트 사이의 거리로 단위는 km 이다.
    // Bearing 은 두 웨이포인트 사이의 방위각으로 단위는 degree. 북쪽이 0도 (0~360)
    for (i in 0 until route.getWayIntervalCount()) {
        val wayInterval = route.getWayInterval(i)       // WayInterval 클래스
        val distanceWayInterval = wayInterval.getDistance()        // 두 웨이포인트 사이의 거리(km)
        val bearingWayInterval = wayInterval.getBearing()          // 두 웨이포인트 사이의 방위각(degree)
        println("WayInterval $i: $distanceWayInterval km, $bearingWayInterval degree")
    }

    val wayInterval1 = route.getWayInterval(0)       // WayInterval 클래스
    val distanceWayInterval = wayInterval1.getDistance()        // 두 웨이포인트 사이의 거리(km)
    val bearingWayInterval = wayInterval1.getBearing()          // 두 웨이포인트 사이의 방위각(degree)
    println("WayInterval 0: $distanceWayInterval km, $bearingWayInterval degree")

    // ----------------- XTD 테스트 -----------------
    // XTD는 Cross Track Distance의 약자로, 선박이 웨이포인트를 따라가는데 있어서 선박의 현재 위치에서 웨이포인트 사이의 직선 거리를 말한다.
    val testPoint = Point2D(57.98584, 25.20867) // 선박의 현재 위치
    println("testPoint: x: ${testPoint.x}, y: ${testPoint.y}")
    for (i in 0 until route.getWayIntervalCount()) {
        val wayInterval2 = route.getWayInterval(i)
        val xtd = wayInterval2.getXTD(testPoint)         // XTD 계산
        println("XTD $i: $xtd km")
    }

//    // ----------------- Get WayInterval Order 테스트 -----------------
//    println("WayInterval Order: ${route.WayIntervalOrderInRoute(testPoint)}")
//    println("Arrival distance: ${MainActivity.GeoDistance2(testPoint, route.getWayPoint(route.WayPointsLength() - 1))} km")
}
