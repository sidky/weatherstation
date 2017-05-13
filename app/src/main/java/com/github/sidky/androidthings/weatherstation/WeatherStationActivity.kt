package com.github.sidky.androidthings.weatherstation

import android.app.Activity
import android.content.Context
import android.hardware.SensorManager
import android.os.Bundle
import android.view.KeyEvent
import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.contrib.driver.button.ButtonInputDriver
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import java.io.IOException

class WeatherStationActivity : Activity() {

    lateinit var bmx280Observable: Bmx280Observable

    var alphaNumericDisplay: AlphanumericDisplay? = null
    var selectionButton: ButtonInputDriver? = null
    var preferredUnitToggle: ButtonInputDriver? = null
    private val selectionSubject: BehaviorSubject<ReadingType> = BehaviorSubject.create()
    private val preferredUnitSubject: BehaviorSubject<Boolean> = BehaviorSubject.create();

    val sensorReadingObservable: Observable<SensorReading> = Observable
            .combineLatest(bmx280Observable.asObservable,
                    selectionSubject,
                    BiFunction({
                        reading: SensorReading, type: ReadingType ->
                        Pair(reading, type)
                    })
            )
            .filter {
                val (reading, type) = it
                reading.type == type
            }
            .map {
                it.first
            }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())

        initializeDisplay()

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        bmx280Observable = Bmx280Observable(sensorManager)

        selectionButton = getButton(BoardDefaults.SELECTION_PIN)
        selectionButton?.register()

        preferredUnitToggle = getButton(BoardDefaults.UNIT_PIN)
        preferredUnitToggle?.register()

        selectionSubject.onNext(ReadingType.TEMPERATURE)
        preferredUnitSubject.onNext(true)

        Observable.combineLatest(sensorReadingObservable, preferredUnitSubject, BiFunction({
            reading: SensorReading, preferred: Boolean ->
                displayString(reading, preferred)
        })).subscribe(object : Consumer<String> {
            override fun accept(p0: String?) {
                Timber.i("Reading: %s", p0)
                alphaNumericDisplay?.display(p0)
            }
        })

    }

    override fun onDestroy() {
        super.onDestroy()

        bmx280Observable.destroy()

        try {
            alphaNumericDisplay?.close()
        } catch (ex: IOException) {
            Timber.e(ex, "Unable to close display")
        }

        selectionButton?.unregister()
        try {
            selectionButton?.close()
        } catch (ex: IOException) {
            Timber.e(ex, "Unable to close reading toggle button")
        }

        preferredUnitToggle?.unregister()
        try {
            preferredUnitToggle?.close()
        } catch (ex: IOException) {
            Timber.e(ex, "Unable to close unit toggle button")
        }

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_SPACE -> {
                val current = selectionSubject.value
                if (current == ReadingType.TEMPERATURE) {
                    selectionSubject.onNext(ReadingType.PRESSURE)
                } else {
                    selectionSubject.onNext(ReadingType.TEMPERATURE)
                }
                true
            }
            KeyEvent.KEYCODE_ENTER -> {
                val current = preferredUnitSubject.value
                preferredUnitSubject.onNext(!current)
                true
            }
            else -> false
        }
    }

    private fun initializeDisplay() {
        alphaNumericDisplay = try {
            AlphanumericDisplay(BoardDefaults.I2C)
        } catch (ex: IOException) {
            Timber.e(ex, "Unable to initialize alphanumeric display")
            null
        }
        alphaNumericDisplay?.setBrightness(1.0f)
        alphaNumericDisplay?.setEnabled(true)
        alphaNumericDisplay?.clear()
    }

    private fun displayString(reading: SensorReading, preferredUnit: Boolean): String {
        val value = when (reading.type) {
            ReadingType.TEMPERATURE -> {
                if (preferredUnit) {
                    reading.value
                } else {
                    (9.0f / 5.0f) * reading.value + 32
                }
            }
            ReadingType.PRESSURE -> {
                if (preferredUnit) {
                    reading.value / 10.0f
                } else {
                    reading.value / 1013.25f
                }
            }
            else -> 0.0f
        }
        return value.toString()
    }

    private fun getButton(pin: String) = ButtonInputDriver(
            pin,
            Button.LogicState.PRESSED_WHEN_LOW,
            KeyEvent.KEYCODE_ENTER)
}
