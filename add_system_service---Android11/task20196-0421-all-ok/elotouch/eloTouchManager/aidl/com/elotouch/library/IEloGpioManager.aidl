package com.elotouch.library;

/**
 * System private API for test.
 *
 * {@hide}
 */
interface IEloGpioManager {
    boolean setModeToGpio(int gpioNum, int gpioMode);
}
