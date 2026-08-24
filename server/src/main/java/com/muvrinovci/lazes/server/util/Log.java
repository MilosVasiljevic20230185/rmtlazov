package com.muvrinovci.lazes.server.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Jednostavno logovanje na konzolu sa vremenskom oznakom.
 * Svaka konekcija, soba i potez se loguju - vidi US-19.
 */
public final class Log {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Log() {
    }

    public static void info(String format, Object... args) {
        print("INFO ", format, args);
    }

    public static void warn(String format, Object... args) {
        print("WARN ", format, args);
    }

    public static void error(String format, Object... args) {
        print("ERROR", format, args);
    }

    private static void print(String level, String format, Object... args) {
        String message = args.length == 0 ? format : String.format(format, args);
        System.out.println(LocalTime.now().format(TIME) + " [" + level + "] " + message);
    }
}
