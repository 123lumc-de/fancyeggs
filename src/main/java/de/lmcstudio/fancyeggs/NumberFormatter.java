package de.lmcstudio.fancyeggs;

import java.text.DecimalFormat;

public class NumberFormatter {

    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Q", "Qi", "Sx", "Sp", "Oc", "No", "Dc"};
    private static final DecimalFormat DF = new DecimalFormat("#,##0.00");

    /**
     * Formatiert eine Zahl kompakt mit Suffix (K, M, B, T, Q, ...).
     * Beispiel: 1500 -> "1.50K", 2500000 -> "2.50M"
     */
    public static String format(double value) {
        if (value < 0) return "-" + format(-value);
        if (value < 1000) return DF.format(value);

        int exp = (int) (Math.log10(value) / 3);
        if (exp >= SUFFIXES.length) exp = SUFFIXES.length - 1;

        double scaled = value / Math.pow(1000, exp);
        return DF.format(scaled) + SUFFIXES[exp];
    }

    /**
     * Formatiert mit Währungszeichen davor.
     */
    public static String formatMoney(double value) {
        return "$" + format(value);
    }
}
