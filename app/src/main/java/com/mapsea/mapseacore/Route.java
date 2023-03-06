package com.mapsea.mapseacore;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.mapsea.mapseacore.GeoUtils.Companion.*;

/** (맵시 구현)배의 항로에 관련된 정보와 기능을 구현한 클래스 */
public class Route {

    private final ArrayList<Point2D> _wayPoints;//웨이포인트 위경도 지점
    private final ArrayList<WayInterval> _intervals;//웨이포인트 간 운항 속성 클래스
    private double _avgSpd = 20.0;//평균 속력 knots단위
    private double _timeToGo = 0.0;//예상 운항 시간 Hour 단위
    private double _ttlDis = 0.0;//총 거리 Km단위
    private LocalDateTime _departureTime;//출발 시각
    private LocalDateTime _arrivalTime;//도착 예정 시각

    public String _name;//항로 이름

    // 생성자
    public Route()
    {
        _wayPoints = new ArrayList<Point2D>();
        _intervals = new ArrayList<WayInterval>();
    }

    /** 웨이포인트 추가함.
     * @param coor Point2D 또는 List<Point2D> 타입의 인자를 받음
     * */
    public void addWayPoints(Object coor) {
        if (coor instanceof Point2D) {
            addWayPoint((Point2D) coor);
        } else if (coor instanceof List) {
            for (Object obj : (List) coor) {
                if (obj instanceof Point2D) {
                    addWayPoint((Point2D) obj);
                }
            }
        }
    }

    /** 항로의 특정 위치에 웨이포인트를 추가 */
    public void addWayPoints(List<Point2D> coor, int index) {
        for (Point2D obj : coor) {
            if (obj != null) {
                addWayPoint(obj, index);
                index++;
            } else {
                throw new IllegalArgumentException("WayPoint is null");
            }
        }
    }

    /** 항로의 끝에 웨이포인트를 추가 */
    public void addWayPoint(Point2D wayPoint)
    {
        _wayPoints.add(wayPoint);
        int ws =_wayPoints.size();
        if(ws > 1) {
            _intervals.add(new WayInterval(_wayPoints.get(ws - 2).getY(), _wayPoints.get(ws - 2).getX()
                    ,_wayPoints.get(ws - 1).getY(), _wayPoints.get(ws - 1).getX()));
            CalculateRouteInfo();
        }
    }


    //항로 전체의 정보를 갱신함. 일반적으로 웨이포인트가 추가, 변경 되었을 때 호출함
    private void CalculateRouteInfo()
    {
        if(_wayPoints.size() > 1) {
            double tmpDis = 0.0;
            double tmpTime = 0.0;
            for (WayInterval interval : _intervals) {
                tmpDis = interval.GetDistance();
//                tmpTime = interval.GetTravelTimeAsHours();
            }
            _ttlDis = tmpDis;
            _timeToGo = tmpDis / _avgSpd * MSFINAL.KMTONMRATE;
//            _timeToGo = tmpTime;
//            _avgSpd = tmpDis / tmpTime * MSFINAL.KMTONMRATE;

            if(_departureTime != null)
            {
                _arrivalTime = _departureTime;

                long hours = (long)_timeToGo;
                _arrivalTime.plusHours(hours);
                double fpTime = _timeToGo - (double)hours;
                fpTime = fpTime * 3600.0;
                _arrivalTime.plusSeconds((long)fpTime);
            }
        }
    }

    /**
     * 특정 위치에 웨이포인트를 추가
     *
     * @param wayPoint 추가할 웨이포인트
     * @param index    웨이포인트 인덱스. 0부터 시작
     */
    public void addWayPoint(Point2D wayPoint, int index) {
        if(index > _wayPoints.size() + 1) {
            throw new IllegalArgumentException("WayPoint index is out of range");
        }

        _wayPoints.add(index, wayPoint);

        if(_wayPoints.size() == 2) {
            _intervals.add(new WayInterval(
                    _wayPoints.get(0).getY(),
                    _wayPoints.get(0).getX(),
                    _wayPoints.get(1).getY(),
                    _wayPoints.get(1).getX()));
        }
        else if(_wayPoints.size() > 2) {
            if(index < _intervals.size()) {
                WayInterval tmpWI = _intervals.get(index)
                        .Clone(
                                _wayPoints.get(index).getY(),
                                _wayPoints.get(index).getX(),
                                _wayPoints.get(index + 1).getY(),
                                _wayPoints.get(index + 1).getX());

                _intervals.add(index, tmpWI);
            }
            else {
                WayInterval tmpWI = (new WayInterval(
                        _wayPoints.get(_wayPoints.size() - 2).getY(),
                        _wayPoints.get(_wayPoints.size() - 2).getX(),
                        _wayPoints.get(_wayPoints.size() - 1).getY(),
                        _wayPoints.get(_wayPoints.size() - 1).getX()));

                _intervals.add(index - 1, tmpWI);
            }

            refreshWayIntervals(index);
        }
        CalculateRouteInfo();
    }

    private void refreshWayIntervals(int index) {
        if(index > 0) {
            _intervals.get(index - 1).Refresh(
                    _wayPoints.get(index - 1).getY(),
                    _wayPoints.get(index - 1).getX(),
                    _wayPoints.get(index).getY(),
                    _wayPoints.get(index).getX());
        }

        if(index < _wayPoints.size() - 1) {
            _intervals.get(index).Refresh(
                    _wayPoints.get(index).getY(),
                    _wayPoints.get(index).getX(),
                    _wayPoints.get(index + 1).getY(),
                    _wayPoints.get(index + 1).getX());
        }
    }

    /** 총 웨이포인트 개수
     * @return (int)way point count
     */
    public int WayPointsLength(){
        return _wayPoints.size();
    }

    /** 총 WayInterval 개수
     * @return (int)way interval count
     */
    public int WayIntervalsLength(){
        return _intervals.size();
    }

    //웨이포인트 삭제
    public boolean Delete(int index){
        if(index < _wayPoints.size()){
            _wayPoints.remove(index);

            if(_intervals.size() > 0){
                if(index == _intervals.size()) {
                    _intervals.remove(index - 1);
                }
                else {
                    _intervals.remove(index);
                }
            }

            refreshWayIntervals(index);
            CalculateRouteInfo();
            return true;
        }
        else {
            return false;
        }
    }

    //웨이포인트 조회 반환
    public Point2D getWayPoint(int index){
        return _wayPoints.get(index);
    }

    //웨이포인트 간 속성 조회 반환
    public WayInterval getWayInterval(int index) {
        return _intervals.get(index);
    }

    /** 평균 속력 반환(knots) */
    public double getAverageSpeed() {
        return _avgSpd;
    }

    /** 평균 속력 설정(knots)
     * @param speed 평균 속력(knots)
     * @return 예상 운항 시간(h)
     * */
    public double setAverageSpeed(double speed) {
        _avgSpd = speed;
        CalculateRouteInfo();
        System.out.println("setAverageSpeed : " + _avgSpd + " / " + _timeToGo);
        return _timeToGo;
    }

    /** 예상 운항 시간 반환(h) */
    public double getTimeToGo() {
        return _timeToGo;
    }

    /** 총 거리 반환(km) */
    public double getTotalDistance() {
        return _ttlDis;
    }

    //출발 시각 지정
    public void SetDepartureTime(LocalDateTime dt) {
        _departureTime = dt;
    }

    //출발 시각 반환
    public LocalDateTime GetDepartureTime() {
        return _departureTime;
    }

    //도착 시각 반환
    public LocalDateTime GetArrivalTime() {
        return _arrivalTime;
    }

    // 해당 위경도 위치가 속하는 웨이포인트의 순서를 반환
    // 내비게이션 중에 항로 상의 어디에 위치에 있는지 판단함
    public int WayIntervalOrderInRoute(Point2D location) {
        if(_intervals.size() > 0) {
            int wiIndex = 0;
            double minDis = MainActivity.GeoDistanceAuto(
                    location.getY(), location.getX(),
                    _intervals.get(0)._nvgPt1.getY(), _intervals.get(0)._nvgPt1.getX());

            for (int i = 0; i < _intervals.size(); i++) {
                double pt1Dis = MainActivity.GeoDistanceAuto(
                        location.getY(), location.getX(),
                        _intervals.get(i)._nvgPt1.getY(), _intervals.get(i)._nvgPt1.getX());
                double pt2Dis = MainActivity.GeoDistanceAuto(
                        location.getY(), location.getX(),
                        _intervals.get(i)._nvgPt2.getY(), _intervals.get(i)._nvgPt2.getX());
                if(pt1Dis < minDis) {
                    minDis = pt1Dis;
                    wiIndex = i;
                }
                if(pt2Dis < minDis) {
                    minDis = pt2Dis;
                    wiIndex = i;
                }

                if ((i == _intervals.size() - 1) && (wiIndex == i)) {
                    double arvDis = MainActivity.GeoDistanceAuto(location.getY(), location.getX(),
                            _wayPoints.get(_wayPoints.size() - 1).getY(),
                            _wayPoints.get(_wayPoints.size() - 1).getX());
                    
                    if(arvDis < pt2Dis) {
                        return -2;//목적지 도착 내비게이션 종료
                    }
                }
            }
            return wiIndex;
        }
        return -1;
    }

    //해당 위치의 XTD 값을 반환 반환값 단위 Km
    public double GetXTD(int wayIntervalOrder, Point2D location) {
        return _intervals.get(wayIntervalOrder).getXTD(location);
    }

    //지정 웨이포인트 도착 예정 시각
    public LocalDateTime WPETA(int wayIntervalOrder) {
        if(_departureTime != null)
        {
            LocalDateTime tmpTime = _departureTime;

            double tmpHours = 0.0;
            for(int i = 0; i < wayIntervalOrder + 1; i++)
            {
                tmpHours = _intervals.get(i).GetDistance() / _intervals.get(i)._speed * MSFINAL.KMTONMRATE;
            }
            long hours = (long)tmpHours;
            tmpTime.plusHours(hours);
            double fpTime = tmpHours - (double)hours;
            fpTime = fpTime * 3600.0;
            tmpTime.plusSeconds((long)fpTime);

            return tmpTime;
        }
        return null;
    }

    //임시로 지정한 속력과 도착 예정 시각을 이용한 총 운항 시간 계산, 반환값 Hour 단위
    public Double GetTTA(double STG) {
        return _ttlDis / STG * MSFINAL.KMTONMRATE;
    }

    //임시로 지정한 속력과 목적지 위치를 이용한 도착 예정 시각 계산
    public LocalDateTime STGToPTA(double STG) {
        LocalDateTime tmpTime = _departureTime;
        double tmpHours = GetTTA(STG);
        long hours = (long)tmpHours;
        tmpTime.plusHours(hours);
        double fpTime = tmpHours - (double)hours;
        fpTime = fpTime * 3600.0;
        tmpTime.plusSeconds((long)fpTime);

        return tmpTime;
    }

    //임시로 지정한 목적지 위치와 도착 예정 시각을 이용한 속력 계산, 반환값 단위 Km/h
    public double PTAToSTG(LocalDateTime PTA) {
        Duration duration = Duration.between(_departureTime, PTA);
        return _ttlDis/((double)duration.getSeconds() / 3600.0);
    }

    //항로 상의 목적지까지 남은 거리, 반환값 단위 Km
    public double GetRemainedDistance(int wayIntervalOrder, double latitude, double longitude) {
        double ttlDis;

        ttlDis = MainActivity.GeoDistanceKmByHaversine(latitude, longitude,
                _wayPoints.get(wayIntervalOrder + 1).getY(), _wayPoints.get(wayIntervalOrder + 1).getX());

        for(int i = wayIntervalOrder + 1; i < _intervals.size(); i++)
        {
            ttlDis = ttlDis + _intervals.get(i).GetDistance();
        }
        return ttlDis;
    }

    //항로 상의 목적지까지 남은 운항 시간, 반환값 단위 Hour
    public double GetRemainedTimeToGo(int wayIntervalOrder, double latitude, double longitude) {
        double ttlTime;

        ttlTime = MainActivity.GeoDistanceKmByHaversine(latitude, longitude,
                _wayPoints.get(wayIntervalOrder + 1).getY(), _wayPoints.get(wayIntervalOrder + 1).getX())
                 / _intervals.get(wayIntervalOrder)._speed / MSFINAL.KMTONMRATE;

        for(int i = wayIntervalOrder + 1; i < _intervals.size(); i++)
        {
            ttlTime = ttlTime + _intervals.get(i).GetTravelTimeAsHours();
        }
        return ttlTime;
    }

    public int getSideOfWayInterval(WayInterval wayInterval, Point2D testPoint) {
        double portXTD = wayInterval._portsideXTD;
        double starboardXTD = wayInterval._starboardXTD;
        double xtd = wayInterval.getXTD(testPoint);

        Point2D start = wayInterval._nvgPt1;
        Point2D end = wayInterval._nvgPt2;
        double bearing = wayInterval.GetBearing();

        double angle = MainActivity.getBearing2(start, testPoint);

        double angleDiff = calAngleDiff(bearing, angle);

        System.out.printf("start: %.5f, %.5f, end: %.5f, %.5f\n", start.getX(), start.getY(), end.getX(), end.getY());
        System.out.printf("bearing: %.5f, angle: %.5f, angleDiff: %.5f\n", bearing, angle, angleDiff);

        if (angleDiff < 0 && (portXTD < xtd)) {
            System.out.println("Port side && out of XTD");
            return WayInterval.SideOfWay.PORTOUT.getValue();
        } else if (angleDiff < 0 && (portXTD > xtd)) {
            System.out.println("Port side && in XTD");
            return WayInterval.SideOfWay.PORTIN.getValue();
        } else if (angleDiff > 0 && (starboardXTD < xtd)) {
            System.out.println("Starboard side && out of XTD");
            return WayInterval.SideOfWay.STARBOARDOUT.getValue();
        } else if (angleDiff > 0 && (starboardXTD > xtd)) {
            System.out.println("Starboard side && in XTD");
            return WayInterval.SideOfWay.STARBOARDIN.getValue();
        } else {
            return WayInterval.SideOfWay.NONE.getValue();
        }
    }

    public int getSideOfWayInterval(Point2D location) {
        int _currentWayIntervalOrder = WayIntervalOrderInRoute(location);
        if ((_currentWayIntervalOrder == -1) || (_currentWayIntervalOrder == -2)) {
            return _currentWayIntervalOrder;
        }
        return getSideOfWayInterval(_intervals.get(_currentWayIntervalOrder), location);
    }

    public static double calAngleDiff(double boringAngle, double targetAngle) {
        // normalize way angle
        double normWayAngle = Math.toRadians(boringAngle);
        while (normWayAngle > Math.PI) normWayAngle -= 2 * Math.PI;
        while (normWayAngle < -Math.PI) normWayAngle += 2 * Math.PI;

        // normalize target angle
        double normTargetAngle = Math.toRadians(targetAngle);
        while (normTargetAngle > Math.PI) normTargetAngle -= 2 * Math.PI;
        while (normTargetAngle < -Math.PI) normTargetAngle += 2 * Math.PI;

        // calculate angle difference
        double result = Math.toDegrees(normTargetAngle - normWayAngle);
        if (result > 180.0) {
            result -= Math.toDegrees(2 * Math.PI);
        } else if (result < -180.0) {
            result += Math.toDegrees(2 * Math.PI);
        }
        return result;
    }
}
