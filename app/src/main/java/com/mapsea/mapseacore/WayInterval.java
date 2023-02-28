package com.mapsea.mapseacore;

import static com.mapsea.mapseacore.MSFINAL.DDG;

import java.time.Duration;

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
    /** 항로가 뻗은 각도 */
    private double _bearing;

    /** 항로 간의 거리, 단위 km */
    private double _distance;

    /** 해당 항로의 왼쪽 한계 거리, 단위 m */
    public int _portXTD;

    /** 해당 항로의 오른쪽 한계 거리, 단위 m */
    public int _stbdXTD;

    /** 운항 속력, 단위 knot */
    public double _speed;

    /** 선회율, 단위 degree per minute */
    public double _turnRadius;

    /** 항로 상의 위치 판단을 위한 2개의 기준점 */
    public Point2D _nvgPt1;
    /** 항로 상의 위치 판단을 위한 2개의 기준점 */
    public Point2D _nvgPt2;

    //생성자
    public WayInterval(double lat1, double lon1, double lat2, double lon2)
    {
        _portXTD = 50;
        _stbdXTD = 50;
        _speed = 20.0;
        _turnRadius = 1.0;

        _bearing = MainActivity.Bearing(lat1, lon1, lat2, lon2);
        _distance = MainActivity.GeoDistanceKmByHaversine(lat1, lon1, lat2, lon2);
        CalculateNVGPT(lat1, lon1, lat2, lon2);
    }

    //지정한 위치로 WI를 갱신
    public void Refresh(double lat1, double lon1, double lat2, double lon2)
    {
        _bearing = MainActivity.Bearing(lat1, lon1, lat2, lon2);
        _distance = MainActivity.GeoDistanceKmByHaversine(lat1, lon1, lat2, lon2);
        CalculateNVGPT(lat1, lon1, lat2, lon2);
    }

    //항로 상의 위치 판단을 위한 2개의 기준점 계산
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
            double angle = MainActivity.Bearing(lat1, lon1, lat2, lon2);
            double xCos = Math.cos(angle);
            double ySin = Math.sin(angle);

            _nvgPt1 = new Point2D(lon1 + xCos * DDG, lat1 + ySin * DDG);
            _nvgPt2 = new Point2D(lon2 - xCos * DDG, lat2 - ySin * DDG);
        }
    }

    /** XTD: Cross-track Distance, 설정한 항로 중앙과 자선과의 거리, 반환값 Km
     *
     * @param location 현재 위치
     * @return XTD km 단위
     */
    public double GetXTD(Point2D location)
    {
        if((_nvgPt1.X == _nvgPt2.X) && (_nvgPt1.Y == _nvgPt2.Y))
        {
            return 0;
        }
        else if((_nvgPt1.X == _nvgPt2.X))
        {
            return MainActivity.GeoDistanceKmByHaversine(location.Y, location.X, location.Y, location.X - _nvgPt1.X);
        }
        else if((_nvgPt1.Y == _nvgPt2.Y))
        {
            return MainActivity.GeoDistanceKmByHaversine(location.Y, location.X, location.Y - _nvgPt1.Y, location.X);
        }

        double tangent1 = (_nvgPt2.Y - _nvgPt1.Y) / (_nvgPt2.X - _nvgPt1.X);
        double yInter1 = _nvgPt1.Y - tangent1 * _nvgPt1.X;

        double tangent2 = -(_nvgPt2.X - _nvgPt1.X) / (_nvgPt2.Y - _nvgPt1.Y);
        double yInter2 = location.Y - tangent2 * location.X;

        Point2D FoP = new Point2D((yInter2 - yInter1) / (tangent1 - tangent2),
                tangent1 * (yInter2 - yInter1) / (tangent1 - tangent2) + yInter1);

        return MainActivity.GeoDistanceKmByHaversine(location.Y, location.X, FoP.Y, FoP.X);
        /*
        if(_nvgPt1.X == _nvgPt2.X)
        {

        }
        if(_nvgPt1.Y == _nvgPt2.Y)
        {

        }

        double fA = _nvgPt2.Y - _nvgPt1.Y;
        double fB = _nvgPt1.X - _nvgPt2.X;
        double fC = (_nvgPt2.X - _nvgPt1.X) * _nvgPt1.Y - (_nvgPt2.Y - _nvgPt1.Y) * _nvgPt1.X;
        double fDis = (fA * location.X + fB * location.Y + fC) / Math.sqrt(fA * fA + fB * fB);

        return fDis;//최종 미터로 변환 필요
        */
    }

    //클론 생성자
    //현재 WI의 속성을 복사하고 지정한 새로운 위치의 WI를 생성하여 반환
    public WayInterval Clone(double lat1, double lon1, double lat2, double lon2)
    {
        WayInterval wi = new WayInterval(lat1, lon1, lat2, lon2);
        //_bearing = wi.GetBearing();
        //_distance = wi.GetDistance();
        wi._portXTD = _portXTD;
        wi._stbdXTD = _stbdXTD;
        wi._speed = _speed;
        wi._turnRadius = _turnRadius;

        return wi;
    }

    //항로 각도 반환
    public double GetBearing()
    {
        return _bearing;
    }

    //항로 간 거리 반환
    public double GetDistance()
    {
        return _distance;
    }

    //예상 운항 시간 반환
    public double GetTravelTimeAsHours() {
        return _distance / (_speed / MSFINAL.KMTONMRATE);
    }
}
