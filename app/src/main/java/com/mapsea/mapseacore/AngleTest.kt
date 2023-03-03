package com.mapsea.mapseacore

fun printTimeUnits(timeUnits: List<Int>) {
    println("timeUnits: ${fN(timeUnits[0])}:${fN(timeUnits[1])}:${fN(timeUnits[2])}:${fN(timeUnits[3])}:${fN(timeUnits[4])}")
}

fun main(){
    // 웨이포인트 기본 기능 테스트
    // Point2D 클래스 x는 longitude, y는 latitude 이다.

    // 웨이포인트 기본 클래스 생성
    val route = Route()

    // 웨이포인트 리스트 생성
    val coorTest = listOf<Point2D>(
        Point2D(58.7136, 24.9664), // Start // 24.9664, 58.7136 //0
        Point2D(57.1574, 25.1046), // Second // 25.24423, 55.22660 //1
        Point2D(56.7689, 26.5513), // Third // 25.24416, 55.23107 //2
    )

    // 웨이포인트를 리스트로 여러 개 추가
    route.addWayPointsList(coorTest)

    // 배의 평균 속력(knot), 모든 웨이포인트 간 거리(km), 예상 운항 시간(h, M:D:h:m:s) 반환
    val speed: Double = route.GetAverageSpeed()
    val distance: Double = route.GetTotalDistance()
    val time: Double = route.GetTimeToGo() // 예상 운항 시간(h)
    val timeUnits: List<Int> = getTimeUnits(time) // 예상 운항 시간(M,D,h,m,s) 반환
    println("speed: $speed knot, distance: $distance km, total time: $time h")
    printTimeUnits(timeUnits)


    // 웨이포인트 리스트 출력
    for (i in 0 until route.WayPointsLength()) {
        val wayPoint = route.GetWayPoint(i)
        println("WayPoint $i: ${wayPoint.x}, ${wayPoint.y}")
    }

    // WayInterval(두 웨이포인트 사이의 속성) 정보 출력
    // WayInterval은 웨이포인트 개수 - 1 만큼 존재한다. (웨이포인트 최소 2개 필요)
    // Distance는 두 웨이포인트 사이의 거리로 단위는 km이다.
    // Bearing은 두 웨이포인트 사이의 방위각으로 단위는 degree. 북쪽이 0도 (0~360)
    for (i in 0 until route.WayIntervalsLength()) {
        val wayInterval = route.GetWayInterval(i)
        println("WayInterval $i: ${wayInterval.GetDistance()}, ${wayInterval.GetBearing()}")
    }
}
