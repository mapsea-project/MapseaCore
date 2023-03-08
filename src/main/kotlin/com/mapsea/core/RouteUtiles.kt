package com.mapsea.core

import com.mapsea.core.MSFINAL.Companion.EARTH_RADIUS_IN_KM
import java.lang.Math.toRadians
import java.lang.Math.toDegrees
import kotlin.math.*


class RouteUtiles {
    companion object{

        fun fN(num: Number, f: Int = 5, d: Int = 2): Number {
            return when (num) {
                is Double, is Float -> "%.${f}f".format(num).toDouble()
                is Int, is Long -> "%0${d}d".format(num).toLong()
                else -> num
            }
        }

        //두 위경도 지점 간의 거리를 킬로미터로 변환
        fun geoDistanceGreatCircle1(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat: Double = toRadians(lat2 - lat1)
            val dLon: Double = toRadians(lon2 - lon1)
            val a: Double = (sin(dLat / 2) * sin(dLat / 2)
                    + (cos(toRadians(lat1)) * cos(toRadians(lat2))
                    * sin(dLon / 2) * sin(dLon / 2)))
            val c: Double = 2 * atan2(sqrt(a), sqrt(1 - a)) // great circle distance in radians
            return EARTH_RADIUS_IN_KM * c
        }

        /** Calculates the great circle distance between two points on the Earth
         * @param p1 Point2D(위경도)
         * @param p2 Point2D(위경도)
         * @return distance in kilometers
         */
        fun geoDistanceGreatCircle2(p1: Point2D, p2: Point2D): Double {
            val lat1: Double = toRadians(p1.y)
            val lat2: Double = toRadians(p2.y)
            val dLat: Double = toRadians(p2.y - p1.y)
            val dLon: Double = toRadians(p2.x - p1.x)
            val a: Double = sin(dLat / 2) * sin(dLat / 2) +
                    cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
            val c: Double = 2 * atan2(sqrt(a), sqrt(1 - a))
            return EARTH_RADIUS_IN_KM * c
        }

        //두 위경도 지점 간의 각도를 반환
        @JvmStatic
        fun getBearing1(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            var dLon: Double = toRadians(lon2 - lon1)
            if (crossDateMeridian(lon1, lon2)) {
                dLon = if (lon1 > 0) {
                    toRadians(lon2 - lon1 - 360)
                } else {
                    toRadians(lon2 - lon1 + 360)
                }
            }
            var bearing: Double = atan2(
                sin(dLon) * cos(toRadians(lat2)), cos(toRadians(lat1)) * sin(toRadians(lat2))
                        - (sin(toRadians(lat1))
                        * cos(toRadians(lat2)) * cos(dLon))
            )
            bearing = toDegrees(bearing)
            bearing = (bearing + 360) % 360
            return bearing
        }

        /** 두 위경도 지점 간의 각도를 반환
         * @param p1 : Point2D 기준점
         * @param p2 : Point2D 대상점
         * @return : double 각도
         */
        fun getBearing2(
            p1: Point2D,
            p2: Point2D
        ): Double { // lat1: p1.getY(), lon1: p1.getX(), lat2: p2.getY(), lon2: p2.getX()
            var dLon: Double = toRadians(p2.x - p1.x)
            if (crossDateMeridian(p1.x, p2.x)) {
                dLon = if (p1.x > 0) {
                    toRadians(p2.x - p1.x - 360)
                } else {
                    toRadians(p2.x - p1.x + 360)
                }
            }
            var bearing: Double = atan2(
                sin(dLon) * cos(toRadians(p2.y)), (
                        cos(toRadians(p1.y)) * sin(toRadians(p2.y))
                                - (sin(toRadians(p1.y))
                                * cos(toRadians(p2.y)) * cos(dLon)))
            )
            bearing = toDegrees(bearing)
            bearing = (bearing + 360) % 360
            return bearing
        }

        /** 두 위경도 지점 간의 거리를 위경도(각도) 단위로 반환
         * 날짜변경선을 횡단하지 않는 방향으로 계산  */
        private fun geoDistance1(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            return sqrt((lat2 - lat1) * (lat2 - lat1) + (lon2 - lon1) * (lon2 - lon1))
        }

        /** 두 위경도 지점 간의 거리를 위경도(각도) 단위로 반환
         * 날짜변경선을 횡단하는 방향으로 계산  */
        private fun geoDistance2(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val diffLon: Double = 360.0 - abs(lon2 - lon1)
            return sqrt((lat2 - lat1) * (lat2 - lat1) + diffLon * diffLon)
        }

        //두 위경도 지점 간의 거리를 위경도(각도) 단위로 반환, 자동으로 날짜변경선 횡단 여부 판단
        fun geoDistanceAuto1(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            return if (crossDateMeridian(lon1, lon2)) {
                geoDistance2(lat1, lon1, lat2, lon2)
            } else {
                geoDistance1(lat1, lon1, lat2, lon2)
            }
        }

        /** 두 위경도 지점 간의 거리를 위경도(각도) 단위로 반환  */
        fun geoDistanceAuto2(p1: Point2D, p2: Point2D): Double {
            return if (crossDateMeridian(p1.x, p2.x)) {
                geoDistance2(p1.y, p1.x, p2.y, p2.x)
            } else {
                geoDistance1(p1.y, p1.x, p2.y, p2.x)
            }
        }

        /** 두개의 경도가 동경 180, 서경 180의 자오선을 지나는 경로인지 판단
         * @param lon1 경도1
         * @param lon2 경도2
         * @return true: 지나는 경로, false: 지나지 않는 경로
         */
        private fun crossDateMeridian(lon1: Double, lon2: Double): Boolean {
            //double dis1 = GeoDistance(0.0, lon1, 0.0, lon2);
            //double dis2 = GeoDistance(0.0, lon1, 0.0, lon2);
            //double dis1 = Math.abs(lon2 - lon1);
            //double dis2 = 360 - dis1;
            return abs(lon2 - lon1) > 180
        }
    }
}