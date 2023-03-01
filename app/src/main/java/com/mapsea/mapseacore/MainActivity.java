package com.mapsea.mapseacore;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Debug;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import net.sf.marineapi.ais.message.AISMessage;
import net.sf.marineapi.ais.message.AISPositionReport;
import net.sf.marineapi.ais.parser.*;
import net.sf.marineapi.ais.parser.AISMessageFactory;
import net.sf.marineapi.ais.util.ManeuverIndicator;
import net.sf.marineapi.ais.util.NavigationalStatus;
import net.sf.marineapi.ais.util.ShipType;
import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.parser.VDMParser;
import net.sf.marineapi.nmea.parser.VDOParser;
import net.sf.marineapi.nmea.sentence.AISSentence;

import java.io.ByteArrayInputStream;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.spec.ECField;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Consumer;


public class MainActivity extends AppCompatActivity {
    /** 자선 정보 */
    private Vessel _myVessel;

    /** 선박 해시 테이블 */
    private VesselHash _vesselHash;

    /** 화면 중심 경도 126.5000 */
    private double _sLongitude = 126.5000;//126.5833;

    /** 화면 중심 위도 36.0833 */
    private double _sLatitude = 36.0833;//37.3166;

    /** 화면 전자 해도 축척 */
    private int _sScale = 10000;

    /** 화면 전자 해도 회전각 */
    private double _sRotation = 0;

    /** 전자 해도 Safety Contour */
    private double _safecon = 10;

    /** (int)전자 해도 언어 설정 */
    private int _language = 0;

    /** 1픽셀 당 위경도 차이, 이미지 서버에서 받아옴 */
    private double _geoPerPixel = 1.0;

    private double _screenWidth = 1920;
    private double _screenHeight = 1080;

    /** 미니맵 배율, 축척 단위와 같음 */
    private double _minimapGeoOffset = 10000;


    private double _tmpLon;
    private double _tmpLat;

    private Route _curRoute;
    private Route _navigationalOrder;
    private List<MaritimeObject> _maritimeObjects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        _screenWidth = displayMetrics.widthPixels;
        _screenHeight = displayMetrics.heightPixels;

        _vesselHash = new VesselHash();
    }

    /** 두개의 경도가 동경 180, 서경 180의 자오선을 지나는 경로인지 판단
     * @param lon1 경도1
     * @param lon2 경도2
     * @return true: 지나는 경로, false: 지나지 않는 경로
     * */
    public static boolean IsCrossDateMeridian(double lon1, double lon2)
    {
        //double dis1 = GeoDistance(0.0, lon1, 0.0, lon2);
        //double dis2 = GeoDistance(0.0, lon1, 0.0, lon2);
        //double dis1 = Math.abs(lon2 - lon1);
        //double dis2 = 360 - dis1;
        return Math.abs(lon2 - lon1) > 180;
    }

    /** 선형순환계에서 정상 동작하도록 stdLon의 경도를 기준으로 proxyLon을 일반화한 포인트 반환
     * @param stdLon 기준 경도
     * @param proxyLon 대상 경도
     * @param proxyLat 대상 위도
     * @return 대상 경도를 일반화한 Point2D
     */
    public static Point2D GetProxyGeoPoint(double stdLon, double proxyLon, double proxyLat){
        if(MainActivity.IsCrossDateMeridian(stdLon, proxyLon))
        {
            if(stdLon > 0)
            {
                return new Point2D(proxyLon + 360.0, proxyLat);
            }
            else
            {
                return new Point2D(proxyLon - 360.0, proxyLat);
            }
        }
        else
        {
            return new Point2D(proxyLon, proxyLat);
        }
    }

    /**
     * 두개의 경도가 동경 180, 서경 180의 자오선을 지나는 경로인지 판단
     * @param lat 위도
     * @param lon 경도
     * @return true: 화면 안, false: 화면 밖
     */
    public boolean IsInScreen(double lat, double lon) {
        //해당 위치(위경도)가 현재 스크린 안인지 판단

        double latTop = _sLatitude + _screenHeight / 2.0 * _geoPerPixel;
        double latBottom = _sLatitude - _screenHeight / 2.0 * _geoPerPixel;
        double lonLeft = _sLongitude - _screenWidth / 2.0 * _geoPerPixel;
        double lonRight = _sLongitude + _screenWidth / 2.0 * _geoPerPixel;

        //날짜자오선 넘어간 경우 A시나리오
        if (lonRight > 180.0) {
            return (lat > latBottom)
                    && (lat < latTop)
                    && (
                    ((lon > lonLeft) && (lon <= 180.0))
                            ||
                            ((lon < lonRight - 360.0) && (lon >= -180.0))
            );
        }

        //날짜자오선 넘어간 경우 B시나리오
        if (lonLeft < -180.0) {
            return (lat > latBottom)
                    && (lat < latTop)
                    && (
                    ((lon > lonLeft + 360.0) && (lon <= 180.0))
                            ||
                            ((lon < lonRight) && (lon >= -180.0))
            );
        }

        return (lat > latBottom)
                && (lat < latTop)
                && (lon > lonLeft)
                && (lon < lonRight);
    }

    /** 지정한 스크린 상의 좌표를 위경도로 변환
     * @param x 스크린 x좌표
     * @param y 스크린 y좌표
     * @return Point2D(위경도)
     * */
    public Point2D ScreenToGeo(double x, double y)
    {
        double lat = _sLatitude + (y - _screenHeight / 2.0) * _geoPerPixel;
        double lon = _sLongitude + (x - _screenWidth / 2.0) * _geoPerPixel;

        if(lon > 180.0)
        {
            lon = lon - 360.0;
        }
        else if(lon < -180.0)
        {
            lon = lon + 360.0;
        }

        return new Point2D(lon, lat);
    }

    /** 위경도 좌표를 스크린 좌표로 변환
     * @param x 위경도 x좌표
     * @param y 위경도 y좌표
     * @return Point2D(스크린 좌표)
     */
    public Point2D GeoToScreen(double x, double y) {
        double tmpX = x;
        if (IsCrossDateMeridian(_sLongitude - _screenWidth / 2.0 * _geoPerPixel,
                _sLongitude + _screenWidth / 2.0 * _geoPerPixel)) {
            if (_sLongitude > 0) {
                if (x < 0) {
                    tmpX = x + 360;
                    }
            } else {
                if (x > 0) {
                    tmpX = x - 360;
                }
            }
        }

        return new Point2D((tmpX - _sLongitude) / _geoPerPixel + _screenWidth / 2.0
                , (y - _sLatitude) / _geoPerPixel + _screenHeight / 2.0);
    }

    public Double MeterToPixel(double m)
    {
        //미터값을 픽셀로 변환
        double latTop = _sLatitude + _screenHeight / 2.0 * _geoPerPixel;
        double latBottom = _sLatitude - _screenHeight / 2.0 * _geoPerPixel;

        double distKm = GeoDistanceKmByHaversine(latTop, 0.0, latBottom, 0.0);
        double mPerPixel = distKm / _screenHeight * 1000.0;

        return m / mPerPixel;
    }


    public boolean isInMinimap(double lat, double lon)
    {
        //해당 위치(위경도)가 미니맵의 안인지 판단
        double dist = GeoDistanceAuto(_myVessel._pos._lat, _myVessel._pos._lon, lat, lon);
        if((dist < _screenWidth / 2.0 * _geoPerPixel) || (dist < _screenHeight / 2.0 * _geoPerPixel)) {
            return true;
        }
        return false;
    }

    public Point2D GeoToMinimap(double x, double y)
    {
        //위경도 좌표를 미니맵 좌표로 변환
        //미니맵

        double tmpX = x;
        if (IsCrossDateMeridian(_sLongitude - _screenWidth / 2.0 * _geoPerPixel,
                _sLongitude + _screenWidth / 2.0 * _geoPerPixel)) {
            if (_sLongitude > 0) {
                if (x < 0) {
                    tmpX = x + 360;
                }
            } else {
                if (x > 0) {
                    tmpX = x - 360;
                }
            }
        }

        double thrsh = _screenWidth / 2.0 * _geoPerPixel;
        if(thrsh > _screenHeight / 2.0 * _geoPerPixel) {
            thrsh = _screenHeight / 2.0 * _geoPerPixel;
        }

        return new Point2D((tmpX - _myVessel._pos._lon) / thrsh, (y - _myVessel._pos._lat) / thrsh);
        //미니맵의 왼쪽 위를 (-1.0, -1.0), 자선을 (0.0, 0.0) 오른쪽 아래를 (1.0, 1.0)으로 한 상대 좌표계
    }

    //두 위경도 지점 간의 거리를 킬로미터로 변환
    public static double GeoDistanceKmByHaversine(double lat1, double lon1, double lat2, double lon2) {
//        double deltaLatitude = Math.abs(lat1 - lat2) * MSFINAL.TORADIAN;
//        double deltaLongitude = 0.0;
//        if (IsCrossDateMeridian(lon1, lon2)) {
//            deltaLongitude = (360 - Math.abs(lon1 - lon2)) * MSFINAL.TORADIAN;
//        } else {
//            deltaLongitude = Math.abs(lon1 - lon2) * MSFINAL.TORADIAN;
//        }
//        double sinDeltaLat = Math.sin(deltaLatitude / 2);
//        double sinDeltaLng = Math.sin(deltaLongitude / 2);
//        double a = sinDeltaLat * sinDeltaLat + Math.cos(lat1 * MSFINAL.TORADIAN) * Math.cos(lat2 * MSFINAL.TORADIAN) * sinDeltaLng * sinDeltaLng;
//
//        double distance = 2 * MSFINAL.RADIUS * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
//
//        return distance;
        double earthRadius = 6371; // Radius of the earth in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c; // Distance in km
        return distance;
    }

    //두 위경도 지점 간의 각도를 반환
    public static double Bearing(double lat1, double lon1, double lat2, double lon2){
        return Math.atan2(lat2 - lat1, lon2 - lon1);// - Math.PI / 2.0;
    }

    //두 위경도 지점 간의 거리를 위경도(각도) 단위로 반환
    //날짜변경선을 횡단하지 않는 방향으로 계산
    public static double GeoDistance(double lat1, double lon1, double lat2, double lon2)
    {
        return Math.sqrt((lat2 - lat1) * (lat2 - lat1) + (lon2 - lon1) * (lon2 - lon1));
    }

    //두 위경도 지점 간의 거리를 위경도(각도) 단위로 반환
    //날짜변경선을 횡단하는 방향으로 계산
    public static double GeoDistance2(double lat1, double lon1, double lat2, double lon2)
    {
        double diffLon = 360.0 - Math.abs(lon2 - lon1);
        return Math.sqrt((lat2 - lat1) * (lat2 - lat1) + diffLon * diffLon);
    }

    //두 위경도 지점 간의 거리를 위경도(각도) 단위로 반환, 자동으로 날짜변경선 횡단 여부 판단
    public static double GeoDistanceAuto(double lat1, double lon1, double lat2, double lon2)
    {
        if(IsCrossDateMeridian(lon1, lon2))
        {
            return GeoDistance2(lat1, lon1, lat2, lon2);
        }
        else
        {
            return GeoDistance(lat1, lon1, lat2, lon2);
        }
    }

    public VesselDisplayMode DetermineVesselDisplayMode(Vessel vessel, boolean isSelf) {
        //선박의 크기와 해도의 축척에 따른 선박 표기 방법 판단
        //isSelf에 자선 여부를 넘겨 줌

        double vesselPixel = MeterToPixel((double)(vessel._sta._to_bow + vessel._sta._to_stern));

        if (isSelf) {
            if (vesselPixel < 25.0) {
                return VesselDisplayMode.PENTAGON;
            } else {
                return VesselDisplayMode.TRIANGLE;
            }
        }
        else
        {
            if (vesselPixel < 25.0) {
                return VesselDisplayMode.PENTAGON;
            }
            else if(_sScale < 500000)
            {
                return VesselDisplayMode.TRIANGLE;
            }
            else
            {
                return VesselDisplayMode.DOT;
            }
        }

    }

    //선박의 움직임 여부를 반환
    public static boolean IsVesselMoving(Vessel vessel)
    {
        switch (vessel._pos._status)
        {
            //선박이 움직이지 않는 경우
            case 1://묘박 중
            case 2://표류 중
            case 5://정박 중
            case 6://좌초 중
                return false;

            //선박이 움직이는 경우
            case 0://엔진을 사용하여 항해 중
            case 3://제한적인 운항 중
            case 4://흘수로 인한 제한적인 운항 중
            case 7://어업 중
            case 8://(엔진동력 없이)바람을 타고 항해 중
            case 9://고속정
            case 10://위그선
            default:
                return true;
        }
    }

    //디버깅용 코드
    public void onTestCPA(View view) {
        CPA cpa4test = new CPA();

        Vessel self = new Vessel();
        self._pos = new PositionData();
        self._pos._lon = -1;
        self._pos._lat = 0;
        self._pos._stp = LocalDateTime.now();
        self._pos._status = 1;
        self._prevPos = new PositionData();
        self._prevPos._lon = -1;
        self._prevPos._lat = 0;
        self._prevPos._stp = self._pos._stp.plusSeconds(30);

        Vessel obj = new Vessel();
        obj._pos = new PositionData();
        obj._pos._lon = 0;
        obj._pos._lat = 0;
        obj._pos._stp = self._pos._stp;
        obj._pos._status = 0;
        obj._prevPos = new PositionData();
        obj._prevPos._lon = 1;
        obj._prevPos._lat = 1;
        obj._prevPos._stp = self._pos._stp.plusSeconds(60);

        cpa4test.CalculateCPA(self, obj);
    }

    //디버깅용 코드
    public void onTestHash(View view) {
        /*
        Vessel testv = new Vessel();
        testv._mmsi = "384575332";
        testv._hash = VesselHash.Hashing(testv._mmsi);
        testv._pos = new PositionData();
        Vessel testv2 = new Vessel();
        testv2._mmsi = "384575398";
        testv2._hash = VesselHash.Hashing(testv2._mmsi);
        testv2._pos = new PositionData();
        for(int i = 0; i < 1000; i++)
        {
            Vessel tmpv = new Vessel();
            double tmpd = Math.random();
            tmpv._mmsi = String.valueOf(tmpd).substring(2, 11);
            tmpv._hash = VesselHash.Hashing(tmpv._mmsi);
            tmpv._sta = new StaticData();

            if(i == 567)
            {
                _vesselHash.AddOrRefresh(testv);
                _vesselHash.AddOrRefresh(testv2);
            }
            _vesselHash.AddOrRefresh(tmpv);
        }

        _vesselHash.Reset();

        Vessel tmpn;
        while( null != (tmpn = _vesselHash.Next()))
        {
            Log.i("", tmpn._mmsi);
        }
        //_vesselHash.Seek("293901999");


        _vesselHash.AddOrRefresh(testv);

        int j = _vesselHash.Seek(testv);
        Log.i("", String.valueOf(j));*/

        //Point2D p2d = GetNearestCollisionPoint(0, 0, _sLatitude, _sLongitude);
        //int d = 0;
    }

    //디버깅용 코드
    public void onTestRoute(View view) {
        /*
        _curRoute = new Route();
        _curRoute.Add(new Point2D(1.0, 1.0));
        _curRoute.Add(new Point2D(0.0, 0.0), 0);
        _curRoute.Add(new Point2D(3.0, 1.0));
        _curRoute.Add(new Point2D(2.0, 0.0), 2);
        _curRoute.Add(new Point2D(6.0, 0.0), 4);
        _curRoute.Add(new Point2D(4.0, 0.0), 4);
        _curRoute.Add(new Point2D(5.0, 1.0), 5);
        int out = _curRoute.WayIntervalOrderInRoute(new Point2D(4.9, 0));
        double out2 = _curRoute.GetXTD(0, new Point2D(0, 1));
        */
        //_curRoute.Delete(0);
        //_curRoute.Delete(4);
        /*
        out2 = GeoDistanceKmByHaversine(0, 0, 0, 1);
         */
        //double out3 = GeoDistanceKmByHaversine(0.0, 0.0, 0.0001, 0.0);
        //double out4 = GeoDistanceKmByHaversine(0.0, 0.0, 0.0, 0.0001);
        //double out5 = GeoDistanceKmByHaversine(37.0, 126.0, 37.0001, 126.0);
        //double out6 = GeoDistanceKmByHaversine(37.0, 126.0, 37.0, 126.0001);


        /*
        AISMessageFactory factory = AISMessageFactory.getInstance();

        String EXAMPLE = "!AIVDM,1,1,,A,";
        String PART1 = "!AIVDM,2,1,4,B,59NScP`2G82cDhESR21<4q@4p@E:0dqEA<Dr221@MhuQ>5Fa,0*12";
        String PART2 = "!AIVDM,2,2,4,B,0`24j2KN<>LH4i@KE4R@C80,2*60";


        AISSentence vdm;
        AISSentence frag1;
        AISSentence frag2;


        vdm = new VDMParser(EXAMPLE);
        frag1 = new VDMParser(PART1);
        frag2 = new VDMParser(PART2);

        AISMessage message = factory.create(vdm);
        AISMessage message1 = factory.create(frag1, frag2);

        if (message.getClass() == AISMessage01Parser.class)
        {
            Log.i(this.getClass().getName(), AISMessage01Parser.class.getName());
        }*/

        InitPilotPlug();
        GetAISDataFromPilotPlug();

        //DownloadDepthMap(_sLatitude, _sLongitude);

    }

    private Thread _pilotPlugThread;
    //private final String PILOT_PLUG_IP = "211.219.112.1";
    //private final String PILOT_PLUG_IP = "192.168.0.8";
    //private final int PILOT_PLUG_PORT = 8794;
    //private final int PILOT_PLUG_PORT = 25500;
    private final String PILOT_PLUG_IP = "192.168.1.1";
    private final int PILOT_PLUG_PORT = 8888;
    private Socket _pilotPlugSocket;
    private AISMessageFactory aismf;


    public void InitPilotPlug()
    {
        aismf = AISMessageFactory.getInstance();
        _pilotPlugThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    _pilotPlugSocket = new Socket(PILOT_PLUG_IP, PILOT_PLUG_PORT);

                    InputStream inputStream = _pilotPlugSocket.getInputStream();

                    AISSentence fragStc = null;

                    boolean hasPrev = false;
                    String prevStr = "";

                    while (true) {
                        byte[] buffer = new byte[1024];
                        int endidx = inputStream.read(buffer, 0, buffer.length);
                        String aisStr = new String(buffer, 0, endidx);
                        Log.i(this.getClass().getName(), aisStr);

                        aisStr = aisStr.replace("\n", "");
                        aisStr = aisStr.replace("\t", "");
                        aisStr = aisStr.replace("\r", "");
                        StringTokenizer stk = new StringTokenizer(aisStr, "!", false);
                        //String[] aisTkn = aisStr.split("\n\t");

                        String tmpStr = "";


                        while (stk.hasMoreTokens()) {
                            try {

                                AISSentence tmpstc = null;
                                tmpStr = stk.nextToken();
                                char[] aisca = tmpStr.toCharArray();

                                if(hasPrev)
                                {
                                    hasPrev = false;
                                    tmpStr = prevStr + tmpStr;
                                    prevStr = "";
                                }

                                if(aisca[0] == 'A'
                                    && aisca[1] == 'I'
                                    && aisca[2] == 'V'
                                    && aisca[3] == 'D')
                                {
                                    tmpStr = "!" + tmpStr;
                                }


                                aisca = tmpStr.toCharArray();
                                if (aisca[5] == 'M') {
                                    tmpstc = new VDMParser(tmpStr);
                                } else if (aisca[5] == 'O') {
                                    tmpstc = new VDOParser(tmpStr);
                                }

                                if (tmpstc != null) {
                                    AISMessage msg = null;
                                    if (aisca[7] == '2') {
                                        if (aisca[9] == '1') {
                                            fragStc = tmpstc;
                                            continue;
                                        } else {
                                            msg = aismf.create(fragStc, tmpstc);
                                            //Log.i(this.getClass().getName(), msg.toString());

                                        }
                                    } else {

                                        msg = aismf.create(tmpstc);
                                    }
                                    //Log.i(this.getClass().getName(), msg.toString());
                                    Vessel tmpvsl = null;
                                    if (msg != null) {
                                        tmpvsl = AIStoVessel(msg);
                                    }

                                    if (tmpvsl != null) {
                                        if (aisca[5] == 'M') {
                                            _vesselHash.AddOrRefresh(tmpvsl);
                                        } else if (aisca[5] == 'O') {
                                            if (_myVessel == null) {
                                                _myVessel = tmpvsl;
                                            } else {
                                                if (tmpvsl._pos != null) {
                                                    _myVessel._prevPos = _myVessel._pos;
                                                    _myVessel._pos = tmpvsl._pos;
                                                }
                                                if (tmpvsl._sta != null) {
                                                    _myVessel._sta = tmpvsl._sta;
                                                }
                                            }
                                        }
                                        Log.i(this.getClass().getName(), tmpvsl._mmsi);
                                    }
                                }
                            } catch (IllegalStateException ex) {
                                ex.printStackTrace();
                            } catch (DataNotAvailableException ex) {
                                ex.printStackTrace();
                                hasPrev = true;
                                prevStr = tmpStr;
                            } catch (IllegalArgumentException ex) {
                                ex.printStackTrace();
                                hasPrev = true;
                                prevStr = tmpStr;
                            } catch (IndexOutOfBoundsException ex) {
                                ex.printStackTrace();
                                hasPrev = true;
                                prevStr = tmpStr;
                            }

                        }
                    }
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
            }
        });
    }

    public void GetAISDataFromPilotPlug()
    {
        _pilotPlugThread.start();
    }

    public Vessel AIStoVessel(AISMessage msg)
    {
        Vessel tmpvsl = new Vessel();
        tmpvsl._mmsi = Integer.toString(msg.getMMSI());
        tmpvsl._hash = VesselHash.Hashing(tmpvsl._mmsi);
        tmpvsl._sender = "pilotPlug";
        switch (msg.getClass().getName()) {
            case "net.sf.marineapi.ais.parser.AISMessage01Parser":
            case "net.sf.marineapi.ais.parser.AISMessage02Parser":
            case "net.sf.marineapi.ais.parser.AISMessage03Parser":
                AISPositionReportParser prp = (AISPositionReportParser) msg;

                tmpvsl._class = true;

                PositionData tmppd = new PositionData();
                tmppd._accuracy = prp.isAccurate();
                tmppd._lat = prp.getLatitudeInDegrees();
                tmppd._lon = prp.getLongitudeInDegrees();
                tmppd._stp = LocalDateTime.now();
                tmppd._course = prp.getCourseOverGround();
                tmppd._heading = prp.getTrueHeading();
                tmppd._maneuver = prp.getManouverIndicator();
                tmppd._maneuver_text = ManeuverIndicator.toString(tmppd._maneuver);
                //tmppd._radio
                //tmppd._raim
                tmppd._second = prp.getTimeStamp();
                //tmppd._seqMsgId
                tmppd._speed = prp.getSpeedOverGround();
                tmppd._status = prp.getNavigationalStatus();
                tmppd._status_text = NavigationalStatus.toString(tmppd._status);
                tmppd._turn = Double.toString(prp.getRateOfTurn());

                tmpvsl._pos = tmppd;
                tmpvsl._sta = null;
                return tmpvsl;

            case "net.sf.marineapi.ais.parser.AISMessage05Parser":
                AISMessage05Parser p05 = (AISMessage05Parser) msg;

                tmpvsl._class = true;

                StaticData tmp05 = new StaticData();
                tmp05._callsign = p05.getCallSign();
                tmp05._destination = p05.getDestination();
                tmp05._draught = (int)p05.getMaximumDraught();
                //tmp05._dte
                tmp05._epfd = p05.getTypeOfEPFD();
                try {
                    tmp05._eta = LocalDateTime.of(LocalDateTime.now().getYear(), p05.getETAMonth(), p05.getETADay()
                            , p05.getETAHour(), p05.getETAMinute());
                }
                catch(java.time.DateTimeException de)
                {
                    de.printStackTrace();
                }
                tmp05._imo = Integer.toString(p05.getIMONumber());
                //tmp05._mid
                tmp05._shipname = p05.getName();
                tmp05._shiptype = p05.getTypeOfShipAndCargoType();
                tmp05._shiptype_text = ShipType.shipTypeToString(tmp05._shiptype);
                tmp05._to_bow = p05.getBow();
                tmp05._to_port = p05.getPort();
                tmp05._to_starboard = p05.getStarboard();
                tmp05._to_stern = p05.getStern();

                tmpvsl._pos = null;
                tmpvsl._sta = tmp05;
                return tmpvsl;

            case "net.sf.marineapi.ais.parser.AISMessage09Parser":
                AISMessage09Parser p09 = (AISMessage09Parser) msg;

                tmpvsl._class = false;

                PositionData tmp09 = new PositionData();
                tmp09._accuracy = p09.isAccurate();
                tmp09._lat = p09.getLatitudeInDegrees();
                tmp09._lon = p09.getLongitudeInDegrees();
                tmp09._stp = LocalDateTime.now();
                tmp09._course = p09.getCourseOverGround();
                tmp09._second = p09.getTimeStamp();
                tmp09._speed = p09.getSpeedOverGround();
                tmp09._raim = p09.getRAIMFlag();
                tmp09._radio = p09.getRadioStatus();

                tmpvsl._pos = tmp09;
                tmpvsl._sta = null;
                return tmpvsl;

            case "net.sf.marineapi.ais.parser.AISMessage18Parser":
                AISMessage18Parser p18 = (AISMessage18Parser) msg;

                tmpvsl._class = false;

                PositionData tmp18 = new PositionData();
                tmp18._accuracy = p18.isAccurate();
                tmp18._lat = p18.getLatitudeInDegrees();
                tmp18._lon = p18.getLongitudeInDegrees();
                tmp18._stp = LocalDateTime.now();
                tmp18._course = p18.getCourseOverGround();
                tmp18._heading = p18.getTrueHeading();
                tmp18._second = p18.getTimeStamp();
                tmp18._speed = p18.getSpeedOverGround();

                tmpvsl._pos = tmp18;
                tmpvsl._sta = null;

                return tmpvsl;

            case "net.sf.marineapi.ais.parser.AISMessage19Parser":
                AISMessage19Parser p19 = (AISMessage19Parser) msg;

                tmpvsl._class = false;

                PositionData tmp19 = new PositionData();
                tmp19._accuracy = p19.isAccurate();
                tmp19._lat = p19.getLatitudeInDegrees();
                tmp19._lon = p19.getLongitudeInDegrees();
                tmp19._stp = LocalDateTime.now();
                tmp19._course = p19.getCourseOverGround();
                tmp19._heading = p19.getTrueHeading();
                tmp19._second = p19.getTimeStamp();
                tmp19._speed = p19.getSpeedOverGround();

                StaticData tmp19s = new StaticData();
                tmp19s._epfd = p19.getTypeOfEPFD();
                tmp19s._shipname = p19.getName();
                tmp19s._shiptype = p19.getTypeOfShipAndCargoType();
                tmp19s._shiptype_text = ShipType.shipTypeToString(tmp19s._shiptype);
                tmp19s._to_bow = p19.getBow();
                tmp19s._to_port = p19.getPort();
                tmp19s._to_starboard = p19.getStarboard();
                tmp19s._to_stern = p19.getStern();

                tmpvsl._pos = tmp19;
                tmpvsl._sta = tmp19s;

                return tmpvsl;

            case "net.sf.marineapi.ais.parser.AISMessage21Parser":
                AISMessage21Parser p21 = (AISMessage21Parser) msg;

                tmpvsl._class = true;

                PositionData tmp21 = new PositionData();
                tmp21._accuracy = p21.isAccurate();
                tmp21._lat = p21.getLatitudeInDegrees();
                tmp21._lon = p21.getLongitudeInDegrees();
                tmp21._stp = LocalDateTime.now();
                tmp21._raim = p21.getRAIMFlag();

                StaticData tmp21s = new StaticData();
                tmp21s._epfd = p21.getTypeOfEPFD();
                tmp21s._shipname = p21.getName();
                tmp21s._to_bow = p21.getBow();
                tmp21s._to_port = p21.getPort();
                tmp21s._to_starboard = p21.getStarboard();
                tmp21s._to_stern = p21.getStern();

                tmpvsl._pos = tmp21;
                tmpvsl._sta = tmp21s;
                return tmpvsl;

            case "net.sf.marineapi.ais.parser.AISMessage24Parser":
                AISMessage24Parser p24 = (AISMessage24Parser) msg;

                tmpvsl._class = true;

                StaticData tmp24 = new StaticData();
                tmp24._callsign = p24.getCallSign();
                tmp24._shipname = p24.getName();
                tmp24._shiptype = p24.getTypeOfShipAndCargoType();
                tmp24._shiptype_text = ShipType.shipTypeToString(tmp24._shiptype);
                tmp24._to_bow = p24.getBow();
                tmp24._to_port = p24.getPort();
                tmp24._to_starboard = p24.getStarboard();
                tmp24._to_stern = p24.getStern();

                tmpvsl._pos = null;
                tmpvsl._sta = tmp24;
                return tmpvsl;

            case "net.sf.marineapi.ais.parser.AISMessage04Parser":
            default:
                return null;
        }
    }

    private Thread _depthMapThread;
    private final String DEPTH_MAP_IP = "211.219.112.1";
    //private final String DEPTH_MAP_IP = "192.168.0.8";
    private final int DEPTH_MAP_PORT = 13939;
    private Socket _depthMapSocket;
    private byte[][] depthMap;
    private byte[][] subMap1;
    private byte[][] subMap2;
    private byte[][] subMap3;

    public void DownloadDepthMap(double latitude, double longitude)
    {
        if(depthMap != null)
        {
            depthMap = null;
        }
        _pilotPlugThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    _depthMapSocket = new Socket(DEPTH_MAP_IP, DEPTH_MAP_PORT);

                    OutputStream outputStream = _depthMapSocket.getOutputStream();
                    InputStream inputStream = _depthMapSocket.getInputStream();

                    String latlonStr = longitude + "&" + latitude;

                    outputStream.write(latlonStr.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();

                    depthMap = new byte[MSFINAL.DEPTH_MAP_SIZE][];
                    for(int i = 0; i < MSFINAL.DEPTH_MAP_SIZE; i++)
                    {
                        depthMap[i] = new byte[MSFINAL.DEPTH_MAP_SIZE];

                        int receivedBytes = 0;
                        //byte[] buffer = new byte[10000];
                        while(receivedBytes < MSFINAL.DEPTH_MAP_SIZE)
                        {
                           int tmpBytes = inputStream.read(depthMap[i], receivedBytes, MSFINAL.DEPTH_MAP_SIZE - receivedBytes);
                           if(tmpBytes > 0) {
                               receivedBytes += tmpBytes;
                               Log.i(this.getClass().getName(), String.valueOf(i) + ": " + String.valueOf(receivedBytes));
                           }
                        }
                    }

                    subMap1 = new byte[MSFINAL.SUB1_SIZE][];
                    for(int j = 0; j < MSFINAL.SUB1_SIZE; j++)
                    {
                        subMap1[j] = new byte[MSFINAL.SUB1_SIZE];

                        int receivedBytes2 = 0;
                        //byte[] buffer = new byte[10000];
                        while(receivedBytes2 < MSFINAL.SUB1_SIZE)
                        {
                            int tmpBytes2 = inputStream.read(subMap1[j], receivedBytes2, MSFINAL.SUB1_SIZE - receivedBytes2);
                            if(tmpBytes2 > 0) {
                                receivedBytes2 += tmpBytes2;
                                Log.i(this.getClass().getName(), String.valueOf(j) + ": " + String.valueOf(receivedBytes2));
                            }
                        }
                    }

                    subMap2 = new byte[MSFINAL.SUB2_SIZE][];
                    for(int k = 0; k < MSFINAL.SUB2_SIZE; k++)
                    {
                        subMap2[k] = new byte[MSFINAL.SUB2_SIZE];

                        int receivedBytes3 = 0;
                        //byte[] buffer = new byte[10000];
                        while(receivedBytes3 < MSFINAL.SUB2_SIZE)
                        {
                            int tmpBytes3 = inputStream.read(subMap2[k], receivedBytes3, MSFINAL.SUB2_SIZE - receivedBytes3);
                            if(tmpBytes3 > 0) {
                                receivedBytes3 += tmpBytes3;
                                Log.i(this.getClass().getName(), String.valueOf(k) + ": " + String.valueOf(receivedBytes3));
                            }
                        }
                    }

                    subMap3 = new byte[MSFINAL.SUB3_SIZE][];
                    for(int m = 0; m < MSFINAL.SUB3_SIZE; m++)
                    {
                        subMap3[m] = new byte[MSFINAL.SUB3_SIZE];

                        int receivedBytes4 = 0;
                        //byte[] buffer = new byte[10000];
                        while(receivedBytes4 < MSFINAL.SUB3_SIZE)
                        {
                            int tmpBytes4 = inputStream.read(subMap3[m], receivedBytes4, MSFINAL.SUB3_SIZE - receivedBytes4);
                            if(tmpBytes4 > 0) {
                                receivedBytes4 += tmpBytes4;
                                Log.i(this.getClass().getName(), String.valueOf(m) + ": " + String.valueOf(receivedBytes4));
                            }
                        }
                    }

                    outputStream.write("end".getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    int d = 0;
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
            }
        });

        _pilotPlugThread.start();
    }

    public Point2D GetNearestCollisionPoint(int heading, int point, double lat, double lon)
    {
        int tmpHdg = heading;

        switch (point)
        {
            case 0:
                break;
            case 1:
                tmpHdg += 68;
                if(tmpHdg > 359)
                    tmpHdg -= 360;
                break;
            case 2:
                tmpHdg += 90;
                if(tmpHdg > 359)
                    tmpHdg -= 360;
                break;
            case 3:
                tmpHdg += 135;
                if(tmpHdg > 359)
                    tmpHdg -= 360;
                break;
            case 4:
                tmpHdg += 180;
                if(tmpHdg > 359)
                    tmpHdg -= 360;
                break;
            case 5:
                tmpHdg += 225;
                if(tmpHdg > 359)
                    tmpHdg -= 360;
                break;
            case 6:
                tmpHdg += 1270;
                if(tmpHdg > 359)
                    tmpHdg -= 360;
                break;
            case 7:
                tmpHdg += 292;
                if(tmpHdg > 359)
                    tmpHdg -= 360;
                break;
            default:
                return null;
        }

        char[][] mask = GetAngleMask(tmpHdg);

        int intLon = (int)lon;
        int intLat = (int)lat;
        double lowPointLon = lon - (double)intLon;
        double lowPointLat = lat - (double)intLat;
        int lonIndex = (int)(lowPointLon * 10000.0);
        int latIndex = (int)(lowPointLat * 10000.0);
        int lonSub1 = lonIndex / 7;
        int latSub1 = latIndex / 7;
        int lonSub2 = lonSub1 / 7;
        int latSub2 = latSub1 / 7;
        int lonSub3 = lonSub2 / 7;
        int latSub3 = latSub2 / 7;


        double tLat = lat - 0.25;
        int tIntLat = (int)tLat;
        double tLowPointLat = tLat - (double)tIntLat;
        if(tLowPointLat >= 0.5)
        {
            double hLat = lat - 0.5;
            intLat = (int)hLat;
            lowPointLat = hLat - (double)intLat;
            latIndex = (int)(lowPointLat * 10000.0);
            latSub1 = latIndex / 7;
            latSub2 = latSub1 / 7;
            latSub3 = latSub2 / 7;
        }

        double tLon = lon - 0.25;
        int tIntLon = (int)tLon;
        double tLowPointLon = tLon - (double)tIntLon;
        if(tLowPointLon >= 0.5)
        {
            double hLon = lon - 0.5;
            intLon = (int)hLon;
            lowPointLon = hLon - (double)intLon;
            lonIndex = (int)(lowPointLon * 10000.0);
            lonSub1 = lonIndex / 7;
            lonSub2 = lonSub1 / 7;
            lonSub3 = lonSub2 / 7;
        }

        SubMapDistance smd = new SubMapDistance();
        smd.latIndex = -1;
        smd.lonIndex = -1;
        smd.distance = Integer.MAX_VALUE;

        smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 1, lonSub3, - 1, smd);
        smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 1, lonSub3, 0, smd);
        smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 1, lonSub3, + 1, smd);
        smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, 0, lonSub3, - 1, smd);
        smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, 0, lonSub3, 0, smd);
        smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3,0, lonSub3, + 1, smd);
        smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 1, lonSub3, - 1, smd);
        smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 1, lonSub3, 0, smd);
        smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 1, lonSub3, + 1, smd);


        if(smd.distance == Integer.MAX_VALUE) {
            smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 2, lonSub3, - 1, smd);
            smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 2, lonSub3, 0, smd);
            smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 2, lonSub3, + 1, smd);
            smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 1, lonSub3, - 2, smd);
            smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, 0, lonSub3, - 2, smd);
            smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 1, lonSub3, - 2, smd);
            smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 2, lonSub3, - 1, smd);
            smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 2, lonSub3, 0, smd);
            smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 2, lonSub3, + 1, smd);
            smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 1, lonSub3, + 2, smd);
            smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, 0, lonSub3, + 2, smd);
            smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 1, lonSub3, + 2, smd);

            if(smd.distance == Integer.MAX_VALUE) {
                smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 3, lonSub3, - 1, smd);
                smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 3, lonSub3, 0, smd);
                smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 3, lonSub3, + 1, smd);
                smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 1, lonSub3, - 3, smd);
                smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, 0, lonSub3, - 3, smd);
                smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 1, lonSub3, - 3, smd);
                smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 3, lonSub3, - 1, smd);
                smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 3, lonSub3, 0, smd);
                smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 3, lonSub3, + 1, smd);
                smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 1, lonSub3, + 3, smd);
                smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, 0, lonSub3, + 3, smd);
                smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 1, lonSub3, + 3, smd);
                smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 2, lonSub3, - 2, smd);
                smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 2, lonSub3, + 2, smd);
                smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 2, lonSub3, - 2, smd);
                smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 2, lonSub3, + 2, smd);

                if(smd.distance == Integer.MAX_VALUE) {
                    smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 3, lonSub3, - 3, smd);
                    smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 2, lonSub3, - 3, smd);
                    smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 3, lonSub3, - 2, smd);
                    smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 3, lonSub3, - 3, smd);
                    smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 2, lonSub3, - 3, smd);
                    smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 3, lonSub3, - 2, smd);
                    smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 3, lonSub3, + 3, smd);
                    smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 2, lonSub3, + 3, smd);
                    smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, - 3, lonSub3, + 2, smd);
                    smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 3, lonSub3, + 3, smd);
                    smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 2, lonSub3, + 3, smd);
                    smd = GetCollisionSubMap(mask, latIndex, lonIndex, latSub3, + 3, lonSub3, + 2, smd);
                }
            }
        }

        double lowPointLatC = 0.0;
        double lowPointLonC = 0.0;

        if(tLowPointLat >= 0.5) {
            lowPointLatC = 0.5;
        }
        if(tLowPointLon >= 0.5){
            lowPointLonC = 0.5;
        }

        if(smd.distance == Integer.MAX_VALUE)
            return null;

        return new Point2D((double)intLat + (double)smd.latIndex / 10000.0 + lowPointLatC, (double)intLon + (double)smd.lonIndex / 10000.0 + lowPointLonC);
    }

    public SubMapDistance GetCollisionSubMap(char[][] mask, int latIndex, int lonIndex, int latSub3, int latSubMask, int lonSub3, int lonSubMask, SubMapDistance smd) {

        SubMapDistance tmpSmd = new SubMapDistance();
        tmpSmd.latIndex = smd.latIndex;
        tmpSmd.lonIndex = smd.lonIndex;
        tmpSmd.distance = smd.distance;
        try {
            if ((subMap3[latSub3 + latSubMask][lonSub3 + lonSubMask] == 0) &&
                    (mask[3 + latSubMask][3 + lonSubMask] == 1)) {
                for (int i = (latSub3 + latSubMask) * 7; i < (latSub3 + latSubMask + 1) * 7; i++) {
                    for (int j = (lonSub3 + lonSubMask) * 7; j < (lonSub3 + lonSubMask + 1) * 7; j++) {
                        if (subMap2[i][j] == 0) {
                            for (int k = i * 7; k < (i + 1) * 7; k++) {
                                for (int m = j * 7; m < (j + 1) * 7; m++) {
                                    if (subMap1[k][m] == 0) {
                                        for (int o = k * 7; o < (k + 1) * 7; o++) {
                                            for (int p = m * 7; p < (m + 1) * 7; p++) {
                                                if (depthMap[o][p] == 0) {
                                                    int tmpDist = (latIndex - o) * (latIndex - o) + (lonIndex - p) * (lonIndex - p);
                                                    if (tmpDist < tmpSmd.distance) {
                                                        tmpSmd.distance = tmpDist;
                                                        tmpSmd.latIndex = o;
                                                        tmpSmd.lonIndex = p;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            ex.printStackTrace();
            tmpSmd.latIndex = smd.latIndex;
            tmpSmd.lonIndex = smd.lonIndex;
            tmpSmd.distance = smd.distance;
            return tmpSmd;
        }
        return tmpSmd;
    }

    public char[][] GetAngleMask(int heading)
    {
        if((heading >= -5.625 && heading < 5.625) || (heading >= 354.375 && heading < 365.625))
            return MSFINAL.AngleMask1;
        else if(heading >= 5.625 && heading < 16.875)
            return MSFINAL.AngleMask2;
        else if(heading >= 16.875 && heading < 28.125)
            return MSFINAL.AngleMask3;
        else if(heading >= 28.125 && heading < 39.375)
            return MSFINAL.AngleMask4;
        else if(heading >= 39.375 && heading < 50.625)
            return MSFINAL.AngleMask5;
        else if(heading >= 50.625 && heading < 61.875)
            return MSFINAL.AngleMask6;
        else if(heading >= 61.875 && heading < 73.125)
            return MSFINAL.AngleMask7;
        else if(heading >= 73.125 && heading < 84.375)
            return MSFINAL.AngleMask8;
        else if(heading >= 84.375 && heading < 95.625)
            return MSFINAL.AngleMask9;
        else if(heading >= 95.625 && heading < 106.875)
            return MSFINAL.AngleMask10;
        else if(heading >= 106.875 && heading < 118.125)
            return MSFINAL.AngleMask11;
        else if(heading >= 118.125 && heading < 129.375)
            return MSFINAL.AngleMask12;
        else if(heading >= 129.375 && heading < 140.625)
            return MSFINAL.AngleMask13;
        else if(heading >= 140.625 && heading < 151.875)
            return MSFINAL.AngleMask14;
        else if(heading >= 151.875 && heading < 163.125)
            return MSFINAL.AngleMask15;
        else if(heading >= 163.125 && heading < 174.375)
            return MSFINAL.AngleMask16;
        else if(heading >= 174.375 && heading < 185.625)
            return MSFINAL.AngleMask17;
        else if(heading >= 185.625 && heading < 196.875)
            return MSFINAL.AngleMask18;
        else if(heading >= 196.875 && heading < 208.125)
            return MSFINAL.AngleMask19;
        else if(heading >= 208.125 && heading < 219.375)
            return MSFINAL.AngleMask20;
        else if(heading >= 219.375 && heading < 230.625)
            return MSFINAL.AngleMask21;
        else if(heading >= 230.625 && heading < 241.875)
            return MSFINAL.AngleMask22;
        else if(heading >= 241.875 && heading < 253.125)
            return MSFINAL.AngleMask23;
        else if(heading >= 253.125 && heading < 264.375)
            return MSFINAL.AngleMask24;
        else if(heading >= 264.375 && heading < 275.625)
            return MSFINAL.AngleMask25;
        else if(heading >= 275.625 && heading < 286.875)
            return MSFINAL.AngleMask26;
        else if(heading >= 286.875 && heading < 298.125)
            return MSFINAL.AngleMask27;
        else if(heading >= 298.125 && heading < 309.375)
            return MSFINAL.AngleMask28;
        else if(heading >= 309.375 && heading < 320.625)
            return MSFINAL.AngleMask29;
        else if(heading >= 320.625 && heading < 331.875)
            return MSFINAL.AngleMask30;
        else if(heading >= 331.875 && heading < 343.125)
            return MSFINAL.AngleMask31;
        else if(heading >= 343.125 && heading < 354.375)
            return MSFINAL.AngleMask32;
        return null;
    }

    public double prevDepthUpdatedLat = 1000.0;
    public double prevDepthUpdatedLon = 1000.0;

    public boolean IsNeedToUpdateDepth(double lat, double lon)
    {
        if((prevDepthUpdatedLat - lat) * (prevDepthUpdatedLat - lat)
                + (prevDepthUpdatedLon - lon) * (prevDepthUpdatedLon - lon)
                > 0.25)
        {
            prevDepthUpdatedLat = lat;
            prevDepthUpdatedLon = lon;
            return true;
        }
        return false;
    }
}