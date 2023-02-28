package com.mapsea.mapseacore;

import java.time.LocalDateTime;

public class PositionData implements Cloneable{
    public int _seqMsgId;
    /** * 배의 운항 상태 */
    public int _status; //배의 운항 상태
    /** * 배의 운항 상태 텍스트 */
    public String _status_text;
    /** * 분당 선회율 */
    public String _turn;
    /** 배의 속력 */
    public double _speed;
    /** 위치 정확도 */
    public boolean _accuracy;//위치 정확도
    /** 경도 longitude */
    public double _lon;
    /** 위도 latitude */
    public double _lat;
    /** 배가 이동하는 방향 각도 */
    public double _course;
    /** 뱃머리가 향하는 방향 각도 */
    public int _heading;
    /** UTC 타임스탬프 초 */
    public int _second;

    public int _maneuver;
    public String _maneuver_text;
    public boolean _raim;
    public int _radio;
    /** 메시지 발생 시각 */
    public LocalDateTime _stp;

    @Override
    public PositionData clone() {
        try{
            return (PositionData) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            return null;
        }
    }
}
