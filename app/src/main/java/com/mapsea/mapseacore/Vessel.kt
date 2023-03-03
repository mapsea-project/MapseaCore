package com.mapsea.mapseacore

/** 선박 데이터<br></br>
 * args:<br></br>
 * _mmsi: 선박 고유 번호<br></br>
 * _hash: 선박 MMSI의 해시 값, mod 1021<br></br>
 * _class: (bool)선박의 클래스<br></br>
 * _sender: 송신한 서버 위치 및 구역<br></br>
 * _sta: 선박의 정적 데이터<br></br>
 * _pos: 선박 동적 위치 데이터<br></br>
 * _prevPos: 선박 동적 위치 데이터, 해시 테이블 내부에서만 사용, 해시 테이블 밖의 선박 데이터의 경우 항상 NULL<br></br>
 */
class Vessel {
    /** 선박 고유 번호  */
    @JvmField
    var _mmsi: String? = null

    /** 선박 MMSI의 해시 값, mod 1021  */
    @JvmField
    var _hash = 0
    @JvmField
    var _class = false

    /** 송신한 서버 위치 및 구역  */
    @JvmField
    var _sender: String? = null

    /** 선박의 정적 데이터  */
    @JvmField
    var _sta: StaticData? = null
    @JvmField
    var _pos: PositionData? = null

    /** 선박 동적 위치 데이터, 해시 테이블 내부에서만 사용, 해시 테이블 밖의 선박 데이터의 경우 항상 NULL  */
    @JvmField
    var _prevPos: PositionData? = null
}