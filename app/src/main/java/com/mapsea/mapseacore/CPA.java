package com.mapsea.mapseacore;

import java.time.Duration;

public class CPA {
    private double _cpa;
    private double _tcpa;


    //두 선박을 입력으로 받아서 v1을 기준으로 CPA와 TCPA를 계산
    public boolean CalculateCPA(Vessel v1, Vessel v2) {
        Point2D proxyV1Prev = MainActivity.GetProxyGeoPoint(v1._pos._lon, v1._prevPos._lon, v1._prevPos._lat);
        Point2D proxyV2 = MainActivity.GetProxyGeoPoint(v1._pos._lon, v2._pos._lon, v2._pos._lat);
        Point2D proxyV2Prev = MainActivity.GetProxyGeoPoint(v1._pos._lon, v2._prevPos._lon, v2._prevPos._lat);

        double brgVecX, brgVecY;
        double objVecX, objVecY;

        brgVecX = proxyV2.getX() - v1._pos._lon;
        brgVecY = proxyV2.getY() - v1._pos._lat;

        double v1MoveX = v1._pos._lon - proxyV1Prev.getX();
        double v1MoveY = v1._pos._lat - proxyV1Prev.getY();
        Duration v1MoveD = Duration.between(v1._prevPos._stp, v1._pos._stp);
        long v1MoveNS = v1MoveD.toNanos();
        double v1MoveS = (double) v1MoveNS / MSFINAL.NS2S;
        double v1STDV = MSFINAL.CPASTDTI / v1MoveS;
        v1MoveX = v1STDV * v1MoveX;
        v1MoveY = v1STDV * v1MoveY;

        double v2MoveX = proxyV2.getX() - proxyV2Prev.getX();
        double v2MoveY = proxyV2.getY() - proxyV2Prev.getY();
        Duration v2MoveD = Duration.between(v2._prevPos._stp, v2._pos._stp);
        long v2MoveNS = v2MoveD.toNanos();
        double v2MoveS = (double) v2MoveNS / MSFINAL.NS2S;
        double v2STDV = MSFINAL.CPASTDTI / v2MoveS;
        v2MoveX = v2STDV * v2MoveX;
        v2MoveY = v2STDV * v2MoveY;

        Point2D predictedV2 = new Point2D(proxyV2.getX(), proxyV2.getY());

        if(MainActivity.IsVesselMoving(v2)) {

            if(v1._pos._stp.isAfter(v2._pos._stp))
            {// v2 이동 보정
                Duration v1tov2 = Duration.between(v2._pos._stp, v1._pos._stp);
                long ns = v1tov2.toNanos();
                double second = (double)ns / MSFINAL.NS2S;
                predictedV2.setX(proxyV2.getX() + v2MoveX / MSFINAL.CPASTDTI * second);
                predictedV2.setY(proxyV2.getY() + v2MoveY / MSFINAL.CPASTDTI * second);
            }



            if (MainActivity.IsVesselMoving(v1)) {
                objVecX = v2MoveX - v1MoveX;
                objVecY = v2MoveY - v1MoveY;
            }
            else
            {
                objVecX = v2MoveX;
                objVecY = v2MoveY;
            }
        }
        else
        {
            if (MainActivity.IsVesselMoving(v1)) {
                objVecX = -v1MoveX;
                objVecY = -v1MoveY;
            }
            else
            {
                return false;
            }
        }
        double relativeVelocity = MainActivity.GeoDistanceKmByHaversine(predictedV2.getY(), predictedV2.getX()
                , predictedV2.getY() + objVecY, predictedV2.getX() + objVecX) / MSFINAL.CPASTDTI; //1초 동안 이동한 거리 km로
        //해당 벡터의 노트단위 속력 구하기

        if(relativeVelocity == 0)
        {
            return false;
        }

        //두벡터의 내적 구하기
        double dotProduct = brgVecX * objVecX + brgVecY * objVecY;

        if (dotProduct > 0.0) {
            if(objVecX == 0.0 && objVecY == 0.0)
            {
                return false;
            }

            if(objVecX == 0.0)
            {
                double px = predictedV2.getX();
                double py = v1._pos._lat;

                _cpa = MainActivity.GeoDistanceKmByHaversine(v1._pos._lat, v1._pos._lon, v1._pos._lat, px);

                _tcpa = MainActivity.GeoDistanceKmByHaversine(predictedV2.getY(), predictedV2.getX(), py, px)
                        / relativeVelocity;
            }
            else if(objVecY == 0.0)
            {
                double px = v1._pos._lon;
                double py = predictedV2.getY();

                _cpa = MainActivity.GeoDistanceKmByHaversine(v1._pos._lat, v1._pos._lon, v1._pos._lat, px);

                _tcpa = MainActivity.GeoDistanceKmByHaversine(predictedV2.getY(), predictedV2.getX(), py, px)
                        / relativeVelocity;
            }
            else {
                double a2 = objVecY / objVecX;
                double b2 = predictedV2.getY() - a2 * predictedV2.getX();

                double a1 = -objVecX / objVecY;
                double b1 = v1._pos._lat - a1 * v1._pos._lon;

                double px = (b2 - b1) / (a1 - a2);
                double py = a1 * px + b1;

                _cpa = MainActivity.GeoDistanceKmByHaversine(v1._pos._lat, v1._pos._lon, py, px);

                _tcpa = MainActivity.GeoDistanceKmByHaversine(predictedV2.getY(), predictedV2.getX(), py, px)
                        / relativeVelocity;
            }
            return true;
        } else {
            return false;
        }
    }

    //미사용 메소드
    private boolean IsSubtractLessThanHalfPI(double vec1x, double vec1y, double vec2x, double vec2y)
    {
        double theta1 = Math.atan(vec1y / vec1x);
        double theta2 = Math.atan(vec2y / vec2x);
        double thetaDiff = theta2 - theta1;

        return (thetaDiff < Math.PI / 2) && (thetaDiff > -Math.PI / 2);
    }

    public double GetCPA()
    {
        return _cpa;//반환값 단위 Km
    }

    public double GetTCPA()
    {
        return _tcpa;//반환값 단위 second
    }
}
