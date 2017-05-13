# weatherstation
A simple android things project to display temparature and pressure
This project displays temperature and pressure, in a 14 segment display. It supports a button two switch between 
temperature and pressure. There is another button to switch units (e.g. kPa and Atm for pressure, Celcius and 
Fahrenhite for temperature).

# Parts
- Raspberry Pi 3 Mode B with Android Things
- BMP280 temperature and pressure sensor with breakout board
- HT16K33 0.54" 14 segment display with breakout board
- 2 push buttons
- jumper wires
- 2 resistors
- bread board

![Diagram](/diagram/weatherstation.png)

My implementation depends on
- [Button driver](https://github.com/androidthings/contrib-drivers/tree/master/button)
- [BMX280 driver](https://github.com/androidthings/contrib-drivers/tree/master/bmx280)
- [HT16K33 display driver](https://github.com/androidthings/contrib-drivers/tree/master/ht16k33)
- [RxJava2](https://github.com/ReactiveX/RxJava)

Written in Kotlin
