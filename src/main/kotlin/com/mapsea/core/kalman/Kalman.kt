package com.route.lib.kalman

import com.route.lib.RouteUtiles.Companion.fN
import com.route.lib.Point2D

class Kalman {
    companion object {
        /** Calculates the great circle distance between two points on the Earth
         * @param state the current state of the particle
         * @param dt the time step
         * @param acceleration the acceleration of the particle
         * @param measurement the measurement
         * @param measurementNoise the measurement noise
         * @param processNoise the process noise
         * @return the new state of the particle(position, velocity)
         */
        fun kalmanFilter(
            state: ParticleState,
            dt: Double,
            acceleration: Double,
            measurement: Double,
            measurementNoise: Double,
            processNoise: Double
        ): ParticleState {
            val predictedState = updateState(state, dt, acceleration)
            val residual = measurement - predictedState.position
            val residualVariance = measurementNoise + processNoise
            val kalmanGain = processNoise / residualVariance
            val newPosition = fN(predictedState.position + kalmanGain * residual).toDouble()
            val newVelocity = fN(predictedState.velocity).toDouble()
            return ParticleState(newPosition, newVelocity)
        }

        fun kalmanFilter(
            state: Point2D,
            dt: Double,
            acceleration: Double,
            measurement: Point2D,
            measurementNoise: Double,
            processNoise: Double,
            bearing: Double
        ): Point2D {
            // Predict the next state
            val predictedState = updateState(state, dt, acceleration, bearing)

            // Calculate the measurement residual
            val residual = measurement - predictedState

            // Calculate the Kalman gain
            val kalmanGain = processNoise / measurementNoise

            // Calculate the new state based on the Kalman gain
            val newPosition = predictedState + (residual * kalmanGain)
            val newVelocity = state.velocity

            return Point2D(newPosition.lon, newPosition.lat, newVelocity)
        }


        /** Calculates the great circle distance between two points on the Earth
         * @param state the current state of the particle
         * @param dt the time step
         * @return acceleration(m/s^2)
         */
        fun calAcceleration(state: Point2D, dt: Double): Double {
            // Calculate the distance traveled during the time interval
            val distanceTraveled = state.velocity * dt

            // Calculate the average speed during the time interval
            val averageSpeed = distanceTraveled / dt

            // Calculate the acceleration
            val acceleration = (averageSpeed - state.velocity) / dt

            return acceleration
        }

        /** Calculates the great circle distance between two points on the Earth
         * @param state the current state of the particle
         * @param dt the time step
         * @return acceleration(m/s^2)
         */
        fun calAcceleration(state: ParticleState, dt: Double): Double {
            val distanceTraveled = state.velocity * dt
            val averageSpeed = distanceTraveled / dt
            val acceleration = (averageSpeed - state.velocity) / dt
            return acceleration
        }


        /** Calculates the great circle distance between two points on the Earth
         * @param state the current state of the particle
         * @param dt the time step
         * @param acceleration the acceleration of the particle
         * @return distance in kilometers
         */
        fun updateState(state: ParticleState, dt: Double, acceleration: Double): ParticleState {
            val newPosition = fN(state.position + state.velocity * dt + 0.5 * acceleration * dt * dt).toDouble()
            val newVelocity = fN(state.velocity + acceleration * dt).toDouble()
            return ParticleState(newPosition, newVelocity)
        }

        /** Updates the state of the particle based on the acceleration and time increment
         * @param state the current state of the particle
         * @param dt the time step
         * @param acceleration the acceleration of the particle
         * @param bearing the bearing of the particle
         * @return the new state of the particle
         */
        fun updateState(state: Point2D, dt: Double, acceleration: Double, bearing: Double): Point2D {
            // Calculate the new velocity based on the acceleration and time increment
            val newVelocity = state.velocity + acceleration * dt
            // Calculate the distance traveled during the time increment
            val distanceTraveled = newVelocity * dt
            val bearingRadians = Math.toRadians(bearing)

            // Calculate the change in latitude and longitude based on the bearing and distance traveled
            val deltaLat = distanceTraveled * Math.cos(bearingRadians)
            val deltaLon = distanceTraveled * Math.sin(bearingRadians)

            // Create a new Point2D object with the updated latitude, longitude, and velocity
            return Point2D(state.lon + deltaLon, state.lat + deltaLat, newVelocity)
        }
    }
}