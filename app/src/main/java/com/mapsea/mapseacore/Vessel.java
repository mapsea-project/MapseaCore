package com.mapsea.mapseacore;


public class Vessel {
    /** 선박 고유 번호 */
    String _mmsi;
    /** 선박 MMSI의 해시 값, mod 1021 */
    int _hash;

    boolean _class;
    /** 송신한 서버 위치 및 구역 */
    String _sender;
    /** 선박의 정적 데이터 */
    StaticData _sta;
    PositionData _pos;
    /** 선박 동적 위치 데이터, 해시 테이블 내부에서만 사용, 해시 테이블 밖의 선박 데이터의 경우 항상 NULL */
    PositionData _prevPos;
}