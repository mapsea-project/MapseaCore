package com.mapsea.mapseacore;

import java.time.LocalDateTime;

public class StaticData {
    public String _mid;
    public String _imo;
    public String _callsign;
    /** 선박 이름 */
    public String _shipname;
    /** 선박 종류 */
    public int _shiptype;
    /** 선박 종류 텍스트 */
    public String _shiptype_text;
    /** 선박의 크기 및 AIS 안테나 위치A(2번 문서 참조) */
    public int _to_bow;
    /** 선박의 크기 및 AIS 안테나 위치B(2번 문서 참조) */
    public int _to_stern;
    /** 선박의 크기 및 AIS 안테나 위치C(2번 문서 참조) */
    public int _to_port;
    /** 선박의 크기 및 AIS 안테나 위치D(2번 문서 참조) */
    public int _to_starboard;
    /** 사용하는 위성 항법 시스템의 종류 */
    public int _epfd;
    /** 도착 예정 시각 */
    public LocalDateTime _eta;
    /** 선박의 최대 흘수 */
    public int _draught;
    /** 선박의 목적지 */
    public String _destination;
    public int _dte;
}
