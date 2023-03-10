package com.route.lib

abstract class RouteTestData {
    internal val route = Route()

    /** (0.0, 0.0) -> (1.0, 1.0) -> (2.0, 2.0)  */
    internal val coorTest1: List<Point2D> = listOf(
        Point2D(0.0, 0.0),
        Point2D(1.0, 1.0),
        Point2D(2.0, 2.0),
    )

    /** (3.0, 3.0) -> (4.0, 4.0) -> (5.0, 5.0)  */
    internal val coorTest2: List<Point2D> = listOf(
        Point2D(3.0, 3.0),
        Point2D(4.0, 4.0),
        Point2D(5.0, 5.0),
    )

    /** 원형 좌표 리스트(시계 방향)  */
    internal val coorClockwise: List<Point2D> = listOf(
        Point2D(0.0, 0.0),
        Point2D(0.0, 1.5),
        Point2D(1.0, 1.0),
        Point2D(1.5, 0.0),
        Point2D(1.0, -1.0),
        Point2D(0.0, -1.5),
        Point2D(-1.0, -1.0),
        Point2D(-1.5, 0.0),
        Point2D(-1.0, 1.0),
        Point2D(0.0, 1.5),
    )

    /** 원형 좌표 리스트(반시계 방향)  */
    internal val coorAntiClockwise: List<Point2D> = listOf(
        Point2D(0.0, 0.0),
        Point2D(0.0, 1.5),
        Point2D(-1.0, 1.0),
        Point2D(-1.5, 0.0),
        Point2D(-1.0, -1.0),
        Point2D(0.0, -1.5),
        Point2D(1.0, -1.0),
        Point2D(1.5, 0.0),
        Point2D(1.0, 1.0),
        Point2D(0.0, 1.5),
    )

    /** 원형 좌표 리스트 at 날짜변경선(시계 방향)  */
    internal val coorDateChangeClockwise: List<Point2D> = listOf(
        Point2D(179.5, 0.0),
        Point2D(179.5, 1.5),
        Point2D(-179.5, 1.0),
        Point2D(-179.0, 0.0),
        Point2D(-179.5, -1.0),
        Point2D(179.5, -1.5),
        Point2D(178.5, -1.0),
        Point2D(178.0, 0.0),
        Point2D(178.5, 1.0),
        Point2D(179.5, 1.5),
    )
}
