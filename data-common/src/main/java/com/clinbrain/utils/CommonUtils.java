package com.clinbrain.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class CommonUtils {
    private static ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<>();
    private static final String DATETIME_FORMAT_MILLISECOND = "yyyy-MM-dd HH:mm:ss.SSS";

    public static DateFormat getDateFormat() {
        DateFormat df = DATE_FORMAT.get();
        if (df == null) {
            df = new SimpleDateFormat(DATETIME_FORMAT_MILLISECOND);
            DATE_FORMAT.set(df);
        }
        return df;
    }

    public static DateTimeFormatter getLocalDateTimeFormat() {
        DateTimeFormatter df = DateTimeFormatter.ofPattern(DATETIME_FORMAT_MILLISECOND);
        return df;
    }

    public static int byteArray2Int(byte[] byteArray) throws Exception {
        ByteArrayInputStream byteInput = new ByteArrayInputStream(byteArray);
        DataInputStream dataInput = new DataInputStream(byteInput);
        return dataInput.readInt();
    }

    public static List<Map<String, Object>> buildList(ResultSet rs) throws Exception {
        List<Map<String, Object>> result = Lists.newArrayList();
        ResultSetMetaData md = rs.getMetaData();
        int columnCount = md.getColumnCount();
        while (rs.next()) {
            Map<String, Object> map = Maps.newHashMap();
            for (int i = 1; i <= columnCount; i++) {
                map.put(md.getColumnLabel(i), rs.getObject(i));
            }
            result.add(map);
        }
        return result;
    }

    public static Date longToDate(long time) throws Exception {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        return c.getTime();
    }

    public static Date stringToDate(String strTime, String dateFormat) throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        Date date = null;
        date = formatter.parse(strTime);
        return date;
    }
}
