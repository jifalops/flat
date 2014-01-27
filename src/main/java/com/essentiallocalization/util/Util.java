package com.essentiallocalization.util;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Miscellaneous utility functions and constants.
 */
public final class Util {
    private Util() { throw new AssertionError("Non-instantiable"); }


    public static final class Format {
        private Format() { throw new AssertionError("Non-instantiable"); }

        public static final SimpleDateFormat LOG = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        public static final SimpleDateFormat LOG_MS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    }



    public static final class Const {
        private Const() { throw new AssertionError("Non-instantiable"); }

        public static final int
                SPEED_OF_LIGHT_VACUUM = 299792458; // m/s
    }



    public static final class Calc {
        private Calc() { throw new AssertionError("Non-instantiable"); }

        public static float timeOfFlightDistance1(long aSent, long bReceived, long bSent, long aReceived) {
            long roundTrip = (aReceived - aSent) - (bSent - bReceived);
            double distance = (Const.SPEED_OF_LIGHT_VACUUM * (roundTrip * 1E-9)) / 2;
            return Math.round(distance * 100) / 100;
        }
    }
}
