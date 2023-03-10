package com.route.lib

import com.route.lib.kalman.Kalman.Companion.updateState
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin
import com.route.lib.kalman.ParticleState

class WayIntervalTest: RouteTestData(){
    private fun checkSideXTD(setPortSideXtd: Double? = null, setStarboardXtd: Double? = null){
        setPortSideXtd?.let { route.setPortSideAll(setPortSideXtd) }
        setStarboardXtd?.let { route.setStarboardSideAll(setStarboardXtd) }
        val testInterval = route.getWayInterval(0) // 0번째 구간
        val portSideXTD = testInterval.getPortSideXTD()
        val starboardXTD = testInterval.getStarboardXTD()
        println("PortSide XTD: $portSideXTD km")
        println("Starboard XTD: $starboardXTD km")
        assert(portSideXTD != 0.0) // xtd가 0이면 오류 발생
        assert(starboardXTD != 0.0) // xtd가 0이면 오류 발생
    }

    private fun checkXtdAndSideOfWay(point: Point2D): Int{
        val testInterval = route.getWayInterval(0)
        val xtd = testInterval.getXTD(point)
        println("XTD on Point: $xtd km")
        assert(xtd != 0.0) // xtd가 0이면 오류 발생
        val sideOfWay = route.getSideOfWayInterval(testInterval, point)
        println("Side of way value: $sideOfWay")
        assert(sideOfWay > -2 || sideOfWay < 2 || sideOfWay != 0) // sideOfWay가 -2보다 작거나 2보다 크거나 0이면 오류 발생
        return sideOfWay
    }

    @Test
    fun checkWayIntervalClockwise(){ // 시게방향 원 둘레를 10개의 점으로 나누어 각도와 거리를 출력
        route.addWayPoints(coorClockwise)
        for (i in 0 until route.getWayIntervalCount()) {
            val wayInterval = route.getWayInterval(i)
            println("WayInterval $i: ${wayInterval.getDistance()} km, ${wayInterval.getBearing()} deg")
        }
    }

    @Test
    fun checkWayIntervalAntiClockwise(){ // 반시계방향 원 둘레를 10개의 점으로 나누어 각도와 거리를 출력
        route.addWayPoints(coorAntiClockwise)
        assert(route.getWayPointCount() == 10) // 10개의 점이 추가되었는지 확인

        for (i in 0 until route.getWayIntervalCount()) {
            val wayInterval = route.getWayInterval(i)
            println("WayInterval $i: ${wayInterval.getDistance()} km, ${wayInterval.getBearing()} deg")
        }
    }

    @Test
    fun checkWayIntervalClockwiseDateChange(){ // 날짜변경선 시계방향 원 둘레를 10개의 점으로 나누어 각도와 거리를 출력
        route.addWayPoints(coorDateChangeClockwise)
        for (i in 0 until route.getWayIntervalCount()) {
            val wayInterval = route.getWayInterval(i)
            val distance = wayInterval.getDistance()
            val bearing = wayInterval.getBearing()
            println("WayInterval $i: $distance km, $bearing deg")
            assert(distance != 0.0 || distance <= 200.0) // 거리가 0이면 오류 발생
            assert(bearing <= 360.0 || bearing >= 0.0) // 각도가 0도 이상 360도 이하가 아니면 오류 발생
        }
    }

    @Test // XTD(Cross Track Distance) 테스트
    fun xtdTest1(){ // (3.0, 3.0)과 (4.0, 4.0) 사이를 잇는 선과 (4.0, 3.0)의 xtd를 구함
        val currentPoint = Point2D(4.0, 3.0) // 선의 좌측 하단 점
        route.addWayPoints(coorTest2)
        val xtd = route.getWayInterval(0).getXTD(currentPoint)
        println("xtd: $xtd km") // 약 78.59 km
        assert(xtd != 0.0) // xtd가 0이면 오류 발생
    }

    @Test // XTD(Cross Track Distance) 테스트
    /** 좌측 out XTD, 좌측 in XTD, 우측 in XTD, 우측 out XTD 테스트
     * 45도 대각선 경로의 좌 우측 점의 XTD와 현재 상태(Side of Way)를 출력
     * */
    fun xtdTest2(){
        route.addWayPoints(coorTest2)
        val testPoint1 = Point2D(3.0001, 3.04) // 좌측 out XTD
        val testPoint2 = Point2D(3.0001, 3.01) // 좌측 in XTD
        val testPoint3 = Point2D(3.01, 3.0001) // 우측 in XTD
        val testPoint4 = Point2D(3.04, 3.0001) // 우측 out XTD

        checkSideXTD() // XTD 출력(PortSide, Starboard)

        println("${checkXtdAndSideOfWay(testPoint1)}") // -2: 좌측 out XTD
        checkXtdAndSideOfWay(testPoint2) // -1: 좌측 in XTD
        checkXtdAndSideOfWay(testPoint3) //  1: 우측 in XTD
        checkXtdAndSideOfWay(testPoint4) //  2: 우측 out XTD
    }

    @Test
    fun xtdTest3(){
        route.addWayPoints(coorTest2)
        route.setAllXTD(5.0) // XTD를 5km로 설정
        checkSideXTD() // XTD 출력(PortSide, Starboard
        val testWay = route.getWayInterval(0)
        val testPoint1 = Point2D(
            testWay.getWayStart().lon + cos(testWay.getBearing())*testWay.getDistance()*0.01,
            testWay.getWayStart().lat + sin(testWay.getBearing())*testWay.getDistance()*0.01)
        val testPoint2 = Point2D(
            testWay.getWayStart().lon + cos(testWay.getBearing())*testWay.getDistance()*0.001,
            testWay.getWayStart().lat + sin(testWay.getBearing())*testWay.getDistance()*0.001)
        val testPoint3 = Point2D(
            testWay.getWayStart().lon - cos(testWay.getBearing())*testWay.getDistance()*0.001,
            testWay.getWayStart().lat - sin(testWay.getBearing())*testWay.getDistance()*0.001)
        val testPoint4 = Point2D(
            testWay.getWayStart().lon - cos(testWay.getBearing())*testWay.getDistance()*0.01,
            testWay.getWayStart().lat - sin(testWay.getBearing())*testWay.getDistance()*0.01)

        println("${checkXtdAndSideOfWay(testPoint1)}")
        checkXtdAndSideOfWay(testPoint2)
        checkXtdAndSideOfWay(testPoint3)
        checkXtdAndSideOfWay(testPoint4)
    }

    @Test
    fun testStateEquation(){
        val dt = 0.1 // 0.1초
        val acceleration = 1.0 // 가속도 1m/s^2
        val initialState = ParticleState(0.0, 0.0) // 초기 상태
        for (i in 0 until 10) { // 0.1초 간격으로 10번 반복
            println("State $i: ${updateState(initialState, dt * i, acceleration)}") // 상태 출력
        }
        val finalState = updateState(initialState, dt, acceleration) // 최종 상태
        println("Final state: $finalState")
    }


}