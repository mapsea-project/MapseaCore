package com.route.lib

class MSFINAL {
    companion object {
        const val DIV_PRIME = 1021          //mod 기반 hash를 위한 소수
        const val MMSI_LENGTH = 9
        const val EARTH_RADIUS_IN_KM = 6371.0           //km radius of earth
        const val KMTONMRATE = 0.5399568    //km to nm
        const val TORADIAN = Math.PI / 180.0
        const val DDG = 0.0001              //웨이포인트 간의 거리 측정을 보조하기 위한 상수
        const val NS2S = 1000000000.0       //나노초 단위 변경, CPA를 구하기 위한 상수
        const val CPASTDTI = 12.0           //CPA를 구하기 위한 시간 간격 표준
        const val DEPTH_MAP_SIZE = 10000
        const val SUB1_SIZE = 1428
        const val SUB2_SIZE = 204
        const val SUB3_SIZE = 29
        const val GEODIV = 600000.0
        const val COGDIV = 10.0
        const val SOGDIV = 10.0
        const val ROTMUL = 4.733
        const val NAVIGATION_ARRIVAL_DISTANCE = 0.2 // 0.2 km
    }
}