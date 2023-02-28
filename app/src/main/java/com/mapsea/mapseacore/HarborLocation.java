package com.mapsea.mapseacore;

import java.util.ArrayList;

public class HarborLocation {
    private static final Point2D yeosu = new Point2D(127.7509, 34.7520);
    private static final Point2D pyeongdang = new Point2D(126.8450, 36.9575);
    private static final Point2D ulsan = new Point2D(129.3729, 35.5175);
    private static final Point2D busan = new Point2D(129.0487, 35.1171);
    private static final Point2D pohang = new Point2D(129.4163, 36.0329);
    private static final Point2D boryeong = new Point2D(126.4861, 36.4067);
    private static final Point2D incheon = new Point2D(126.6128, 37.4659);
    private static ArrayList<Point2D> _harbors;
    private static ArrayList<String> _names;;

    public static void initHarborList()
    {
        _harbors = new ArrayList<Point2D>();

        _harbors.add(yeosu);//0
        _names.add("yeosu");
        _harbors.add(pyeongdang);//1
        _names.add("pyeongdang");
        _harbors.add(ulsan);//2
        _names.add("ulsan");
        _harbors.add(busan);//3
        _names.add("busan");
        _harbors.add(pohang);//4
        _names.add("pohang");
        _harbors.add(boryeong);//5
        _names.add("boryeong");
        _harbors.add(incheon);//6
        _names.add("incheon");
    }

    public static String GetNearestHarbor(Point2D location)
    {
        double minDist = Double.MAX_VALUE;
        int minIdx = -1;

        for (int i = 0; i < _harbors.size(); i++)
        {
            double tmpDist = MainActivity.GeoDistanceAuto(location.Y, location.X
                    , _harbors.get(i).Y, _harbors.get(i).X);
            if (tmpDist < minDist)
            {
                minDist = tmpDist;
                minIdx = i;
            }
        }
        return _names.get(minIdx);
    }
}
