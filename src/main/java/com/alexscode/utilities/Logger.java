package com.alexscode.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Logger {

    public static final boolean ACTIVE = false;

    private static boolean activeWarningIssued = false;

    private static ArrayList<LogEvent> logs;

    static {
        if (ACTIVE) {
            logs = new ArrayList<>();
        }
    }

    public static final String[] LOGLEVEL_STRING = {"Info", "Warning", "Important", "Critical", "Error"};


    public enum LogLevel {
        INFO(1),
        WARNING(2),
        IMPORTANT(3),
        CRITICAL(4),
        ERROR(5);

        private int level;

        LogLevel(int level) {
            this.level = level;
        }

        public int getLevel() {
            return this.level;
        }

        @Override
        public String toString() {
            return Logger.LOGLEVEL_STRING[this.getLevel()-1];
        }
    }

    public static class LogEvent {
        public final Object emitter;
        public final LogLevel logLevel;
        public final Object[] objects;

        public LogEvent( Object emitter, LogLevel logLevel, Object[] objects) {
            this.emitter = emitter;
            this.logLevel = logLevel;
            this.objects = objects;
        }

        public String subjectToString() {
            StringBuilder sb = new StringBuilder();

            for (Object arg_obj : this.objects ) {
                sb.append(arg_obj == null ? "null" : arg_obj.toString());
            }

            return sb.toString();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append(this.logLevel.toString());
            sb.append(":\t");
            sb.append(this.emitter.toString());
            sb.append(" : \t");

            sb.append(this.subjectToString());

            return sb.toString();
        }

        public void print() {
            System.out.println(this.toString());
        }

    }

    public static ArrayList<LogEvent> getLogs() {
        if (!Logger.ACTIVE && !Logger.activeWarningIssued) {
            System.err.println("Warning: logger deactivated!");
            Logger.activeWarningIssued = true;
        }
        return logs;
    }

    public static void log(Object obj, LogLevel logLevel, Object... args) {
        if (Logger.ACTIVE) {
            logs.add(new LogEvent(obj, logLevel, args));
        }
    }

    public static void logInfo(Object obj, Object... args) {
        Logger.log(obj, LogLevel.INFO, args);
    }

    public static void logWarning(Object obj, Object... args) {
        Logger.log(obj, LogLevel.WARNING, args);
    }

    public static void logImportant(Object obj, Object... args) {
        Logger.log(obj, LogLevel.IMPORTANT, args);
    }

    public static void logCritical(Object obj, Object... args) {
        Logger.log(obj, LogLevel.CRITICAL, args);
    }

    public static void logError(Object obj, Object... args) {
        Logger.log(obj, LogLevel.ERROR, args);
    }

    public static void printLogs() {
        for (LogEvent logEvent : Logger.getLogs()) {
            logEvent.print();
        }
    }

    public static void printLogs(ArrayList<LogEvent> logs) {
        for (LogEvent logEvent : logs) {
            logEvent.print();
        }
    }

    public static ArrayList<LogEvent> filterLogs(Predicate<LogEvent> predicate) {
        return Logger.getLogs()
                .stream()
                .filter(predicate)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static ArrayList<LogEvent> atLeastLevel(int level) {
        return filterLogs(l -> l.logLevel.getLevel() >= level);
    }

    public static ArrayList<LogEvent> fromObject(Object obj) {
        return filterLogs(le -> le == obj);
    }

    public static ArrayList<LogEvent> ofLevel(int level) {
        return filterLogs(le -> le.logLevel.getLevel() == level);
    }

    public static ArrayList<LogEvent> fromClass(Class c) {
        return filterLogs(le -> le.emitter.getClass() == c);
    }

    public static ArrayList<LogEvent> fromEqualObject(Object obj) {
        return filterLogs(le -> le.emitter.equals(obj));
    }

    public static ArrayList<LogEvent> objectInSubject(Object obj) {
        return filterLogs(le -> Arrays.asList(le.objects).contains(obj));
    }

    public static ArrayList<LogEvent> searchSubject(String pattern) {
        return filterLogs(le -> le.subjectToString().matches(pattern));
    }

}
