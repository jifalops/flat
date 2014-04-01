package com.essentiallocalization.util;

import java.text.DecimalFormat;
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

        public static final DecimalFormat BASIC_0DEC = new DecimalFormat("##############0");
        public static final DecimalFormat newBasic2dec() { return new DecimalFormat("##############0.00"); }
        public static final DecimalFormat SEPARATOR_0DEC = new DecimalFormat("###,###,###,###,##0");
        public static final DecimalFormat SEPARATOR_2DEC = new DecimalFormat("###,###,###,###,##0.00");
        public static final DecimalFormat SCIENTIFIC_3SIG = new DecimalFormat("0.##E0");
        public static final DecimalFormat SCIENTIFIC_5SIG = new DecimalFormat("0.####E0");
    }



    public static final class Const {
        private Const() { throw new AssertionError("Non-instantiable"); }

        public static final int
                SPEED_OF_LIGHT_VACUUM = 299792458; // m/s
    }



    public static final class Calc {
        private Calc() { throw new AssertionError("Non-instantiable"); }

        public static double timeOfFlightDistanceNano(long aSent, long bReceived, long bSent, long aReceived) {
            long roundTrip = (aReceived - aSent) - (bSent - bReceived);
            double distance = (Const.SPEED_OF_LIGHT_VACUUM * (roundTrip * 1E-9)) / 2;
            return distance;
        }

        public static double timeOfFlightDistanceMicro(long aSent, long bReceived, long bSent, long aReceived) {
            long roundTrip = (aReceived - aSent) - (bSent - bReceived);
            double distance = (Const.SPEED_OF_LIGHT_VACUUM * (roundTrip * 1E-6)) / 2;
            return distance;
        }
    }
}
