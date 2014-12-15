package com.flat.util;

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
        public static final SimpleDateFormat LOG_MS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US);
        public static final SimpleDateFormat LOG_FILENAME = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.US);

        public static final DecimalFormat BASIC_0DEC = new DecimalFormat("##############0");
        public static final DecimalFormat newBasic2dec() { return new DecimalFormat("##############0.00"); }
        public static final DecimalFormat SEPARATOR_0DEC = new DecimalFormat("###,###,###,###,##0");
        public static final DecimalFormat SEPARATOR_2DEC = new DecimalFormat("###,###,###,###,##0.00");
        public static final DecimalFormat SCIENTIFIC_3SIG = new DecimalFormat("+0.00E+0;-0");
        public static final DecimalFormat SCIENTIFIC_5SIG = new DecimalFormat("0.####E0");

        public static final DecimalFormat newBasic6dec() { return new DecimalFormat("#####0.000000"); }
    }

    public static final class Calc {
        private Calc() { throw new AssertionError("Non-instantiable"); }



    }
}
