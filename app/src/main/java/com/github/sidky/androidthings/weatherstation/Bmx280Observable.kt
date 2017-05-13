package com.github.sidky.androidthings.weatherstation

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.google.android.things.contrib.driver.bmx280.Bmx280SensorDriver
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import java.io.IOException

enum class ReadingType {
    TEMPERATURE, PRESSURE, UNKNOWN
}

data class SensorReading(val type: ReadingType, val value: Float)

class Bmx280Observable(val sensorManager: SensorManager) : SensorEventListener {

    private val sensorManagerCallback = object : SensorManager.DynamicSensorCallback() {
        override fun onDynamicSensorConnected(sensor: Sensor?) {
            when (sensor?.type) {
                Sensor.TYPE_PRESSURE, Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                    sensorManager.registerListener(
                            this@Bmx280Observable,
                            sensor,
                            SensorManager.SENSOR_DELAY_NORMAL)
                }
            }
        }
    }

    private val bmx280Driver = try {
        Bmx280SensorDriver(BoardDefaults.I2C)
    } catch (ex: IOException) {
        Timber.e(ex, "Unable to open bmx280 sensor driver on I2C1")
        null
    }


    init {
        sensorManager.registerDynamicSensorCallback(sensorManagerCallback)

        bmx280Driver?.registerTemperatureSensor()
        bmx280Driver?.registerPressureSensor()
    }

    fun destroy() {
        subject.onComplete()
        sensorManager.unregisterDynamicSensorCallback(sensorManagerCallback)

        bmx280Driver?.unregisterPressureSensor()
        bmx280Driver?.unregisterTemperatureSensor()

        try {
            bmx280Driver?.close()
        } catch (ex: IOException) {
            Timber.e(ex, "Unable to close bmx280 sensor driver")
        }
    }

    private val subject: Subject<SensorReading> = PublishSubject.create<SensorReading>().toSerialized()

    val asObservable: Observable<SensorReading>
        get() = subject.hide().share()

    override fun onSensorChanged(event: SensorEvent?) {
        val type = when (event?.sensor?.type) {
            Sensor.TYPE_PRESSURE -> ReadingType.PRESSURE
            Sensor.TYPE_AMBIENT_TEMPERATURE -> ReadingType.TEMPERATURE
            else -> ReadingType.UNKNOWN
        }

        val value = event?.values?.get(0)

        if (type != ReadingType.UNKNOWN && value != null) {
            subject.onNext(SensorReading(type, value))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Timber.i("Accuracy of sensor: %s changed to %d", sensor?.type, accuracy)
    }
}