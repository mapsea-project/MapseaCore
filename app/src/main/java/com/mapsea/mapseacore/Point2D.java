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
     * @param x the (경도)longitude coordinate
     * @param y the (위도)latitude coordinate
     */
    public Point2D(double x, double y)
    {
        this.X = x;
        this.Y = y;
    }

    public double getX() {
        return X;
    }

    public double getY() {
        return Y;
    }

    public void setX(double x) {
        this.X = x;
    }

    public void setY(double y) {
        this.Y = y;
    }
}
