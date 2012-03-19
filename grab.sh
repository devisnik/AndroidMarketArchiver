#!/bin/bash

java -cp lib/selenium-2.20.0/*:lib/selenium-2.20.0/libs/*:build/marketGrabber.jar de.devisnik.android.stats.MarketAppDataGrabber -login $1 -pass $2

