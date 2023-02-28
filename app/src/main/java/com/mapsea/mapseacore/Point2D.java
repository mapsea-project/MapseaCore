package com.mapsea.mapseacore;

/** 좌표 데이터(double)<br>
 * x: longitude
 * y: latitude
 * */
public class Point2D {
    public double X;
    public double Y;

    /**
     * Creates a 2D point with the given longitude and latitude coordinates.
     * @param x the longitude coordinate
     * @param y the latitude coordinate
     */
    public Point2D(double x, double y)

    {
        X = x;
        Y = y;
    }
}
