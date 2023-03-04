package com.mapsea.mapseacore;

import static com.mapsea.mapseacore.MSFINAL.DDG;
import static java.lang.Math.abs;

/** 항로 간의 간격을 나타내는 클래스
 * args:<br>
 * _bearing: 항로가 뻗은 각도<br>
 * _distance: 항로 간의 거리, 단위 km<br>
 * _portXTD: 해당 항로의 왼쪽 한계 거리, 단위 m<br>
 * _stbdXTD: 해당 항로의 오른쪽 한계 거리, 단위 m<br>
 * _speed: 운항 속력, 단위 knot<br>
 * _turnRadius: 선회율, 단위 degree per minute<br>
 * _nvgPt1, _nvgPt2: 항로 상의 위치 판단을 위한 2개의 기준점<br>
 * */
public class WayInterval {
    /** 항로가 뻗은 각도 (rad) */
    private double _bearing;

    /** 항로 간의 거리, 단위 km */
    private double _distance;

    /** 해당 항로의 왼쪽 한계 거리, 단위 m */
    public int _portsideXTD;

    /** 해당 항로의 오른쪽 한계 거리, 단위 m */
    public int _starboardXTD;

    /** 운항 속력, 단위 knot */
    public double _speed;

    /** 선회율, 단위 degree per minute */
    public double _turnRadius;

    /** 항로 상의 위치 판단을 위한 2개의 기준점 */
    public Point2D _nvgPt1;
    /** 항로 상의 위치 판단을 위한 2개의 기준점 */
    public Point2D _nvgPt2;

    public double GetPortsideXTD() { return _portsideXTD; }
    public double GetStarboardXTD() { return _starboardXTD; }
    public double GetSpeed() { return _speed; }
    public double GetTurnRadius() { return _turnRadius; }

    public void SetPortsideXTD(int xtd) { _portsideXTD = xtd; }
    public void SetStarboardXTD(int xtd) { _starboardXTD = xtd; }
    public void SetSpeed(double speed) { _speed = speed; }
    public void SetTurnRadius(double radius) { _turnRadius = radius; }

    public Point2D GetNVGPT1() { return _nvgPt1; }
    public Point2D GetNVGPT2() { return _nvgPt2; }

    public void SetNVGPT1(Point2D pt) { _nvgPt1 = pt; }
    public void SetNVGPT2(Point2D pt) { _nvgPt2 = pt; }

    //생성자
    public WayInterval(double lat1, double lon1, double lat2, double lon2)
    {
        SetPortsideXTD(1000);
        SetStarboardXTD(1000);
        SetSpeed(20.0);
        SetTurnRadius(1.0);

        _bearing = MainActivity.Bearing1(lat1, lon1, lat2, lon2);
        _distance = MainActivity.GeoDistanceKmByHaversine(lat1, lon1, lat2, lon2);
        CalculateNVGPT(lat1, lon1, lat2, lon2);
    }

    //지정한 위치로 WI를 갱신
    public void Refresh(double lat1, double lon1, double lat2, double lon2)
    {
        _bearing = MainActivity.Bearing1(lat1, lon1, lat2, lon2);
        _distance = MainActivity.GeoDistanceKmByHaversine(lat1, lon1, lat2, lon2);
        CalculateNVGPT(lat1, lon1, lat2, lon2);
    }

    // 항로 상의 위치 판단을 위한 2개의 기준점 계산
    private void CalculateNVGPT(double lat1, double lon1, double lat2, double lon2)
    {
        if(lon1 == lon2)
        {
            if(lat1 < lat2)
            {
                _nvgPt1 = new Point2D(lon1, lat1 + DDG);
                _nvgPt2 = new Point2D(lon2, lat2 - DDG);
            }
            else
            {
                _nvgPt1 = new Point2D(lon1, lat1 - DDG);
                _nvgPt2 = new Point2D(lon2, lat2 + DDG);
            }
        }
        else if(lat1 == lat2)
        {
            if(lon1 < lon2)
            {
                _nvgPt1 = new Point2D(lon1 + DDG, lat1);
                _nvgPt2 = new Point2D(lon2 - DDG, lat2);
            }
            else
            {
                _nvgPt1 = new Point2D(lon1 - DDG, lat1);
                _nvgPt2 = new Point2D(lon2 + DDG, lat2);
            }
        }
        else {
            //tangent = (lat2 - lat1) / (lon2 - lon1);
            //yInter = tangent * (-lon1) + lat1;
            double angle = MainActivity.Bearing1(lat1, lon1, lat2, lon2);
            double xCos = Math.cos(angle);
            double ySin = Math.sin(angle);

            _nvgPt1 = new Point2D(lon1 + xCos * DDG, lat1 + ySin * DDG);
            _nvgPt2 = new Point2D(lon2 - xCos * DDG, lat2 - ySin * DDG);
        }
    }

    /** XTD: Cross-track Distance, 설정한 항로 중앙과 자선과의 거리, 반환값 m
     * @param location 현재 위치(위도, 경도)
     * @return XTD km 단위
     */
    // Calculates the length of a perpendicular line from a point to a line connecting two other points
    public double getXTD(Point2D location) {
        if (isLongitudeDifferenceGreaterThan180Degrees()) {
            adjustLongitude();
        }

        double distanceBetweenPoints = calculateDistanceBetweenPoints(_nvgPt1, _nvgPt2);
        double azimuthAngle12 = calculateAzimuthAngle(_nvgPt1, _nvgPt2);
        double distanceBetweenPointAndLocation = calculateDistanceBetweenPoints(_nvgPt1, location);
        double azimuthAngle13 = calculateAzimuthAngle(_nvgPt1, location);

        return Math.abs(Math.asin(Math.sin(distanceBetweenPointAndLocation / EARTH_RADIUS_IN_KM)
                * Math.sin(azimuthAngle13 - azimuthAngle12)) * EARTH_RADIUS_IN_KM * METERS_PER_KILOMETER);
    }

    private boolean isLongitudeDifferenceGreaterThan180Degrees() {
        return Math.abs(_nvgPt2.getX() - _nvgPt1.getX()) > 180;
    }

    private void adjustLongitude() {
        if (_nvgPt2.getX() > _nvgPt1.getX()) {
            _nvgPt1.setX(_nvgPt1.getX() - 360);
        } else {
            _nvgPt2.setX(_nvgPt2.getX() + 360);
        }
    }

    private double calculateDistanceBetweenPoints(Point2D point1, Point2D point2) {
        return MainActivity.GeoDistanceGreateCircle(point1, point2);
    }

    private double calculateAzimuthAngle(Point2D point1, Point2D point2) {
        return Math.atan2(
                Math.sin(Math.toRadians(point2.getX() - point1.getX()))
                        * Math.cos(Math.toRadians(point2.getY())),
                Math.cos(Math.toRadians(point1.getY()))
                        * Math.sin(Math.toRadians(point2.getY()))
                        - Math.sin(Math.toRadians(point1.getY()))
                        * Math.cos(Math.toRadians(point2.getY()))
                        * Math.cos(Math.toRadians(point2.getX() - point1.getX()))
        );
    }

    private static final double EARTH_RADIUS_IN_KM = 6371.0;
    private static final double METERS_PER_KILOMETER = 1000.0;

    //클론 생성자
    //현재 WI의 속성을 복사하고 지정한 새로운 위치의 WI를 생성하여 반환
    public WayInterval Clone(double lat1, double lon1, double lat2, double lon2)
    {
        WayInterval wi = new WayInterval(lat1, lon1, lat2, lon2);
        //_bearing = wi.GetBearing();
        //_distance = wi.GetDistance();
        wi._portsideXTD = _portsideXTD;
        wi._starboardXTD = _starboardXTD;
        wi._speed = _speed;
        wi._turnRadius = _turnRadius;

        return wi;
    }

    /** 항로 각도 반환 */
    public double GetBearing() { return _bearing; }

    /** 항로 간 거리 반환(Km) */
    public double GetDistance() { return _distance; }

    /** 예상 운항 시간 반환 */
    public double GetTravelTimeAsHours() {
        return _distance / (_speed / MSFINAL.KMTONMRATE);
    }

    public enum SideOfWay {
        PORTOUT(-2), PORTIN(-1), NONE(0), STARBOARDIN(1), STARBOARDOUT(2);

        private final int value;

        SideOfWay(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
