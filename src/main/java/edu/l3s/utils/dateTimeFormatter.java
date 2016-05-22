package edu.l3s.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Created by ranyu on 3/2/16.
 */
public class dateTimeFormatter {
    public static String dateTimeFormatter(String in) {
        //formatting date and time
        //String in = "Tue, 3 Jun 2008 11:05:30 GMT   ";
        LocalDate date;
        try {
            date = LocalDate.parse(in, DateTimeFormatter.BASIC_ISO_DATE);
            //System.out.printf(date.toString());
            return date.toString();
        } catch (DateTimeParseException exc) {
            // System.out.printf("%s is not parsable!%n", in);
            //throw exc;      // Rethrow the exception.
        }
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_LOCAL_DATE);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_OFFSET_DATE);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_DATE);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_LOCAL_TIME);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_OFFSET_TIME);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_TIME);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_ZONED_DATE_TIME);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_DATE_TIME);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_ORDINAL_DATE);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_WEEK_DATE);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_INSTANT);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.RFC_1123_DATE_TIME);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        return in;
    }
}
