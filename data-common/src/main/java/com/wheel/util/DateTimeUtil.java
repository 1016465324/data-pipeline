package com.wheel.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class DateTimeUtil {

    private static Logger logger= LoggerFactory.getLogger(DateTimeUtil.class);
    public static final String dateString = "1840-12-31";

    public static Date IntToDate(int day) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date Converted=new Date();
        try {
            Date date =sdf.parse(dateString);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DATE,day);
            Converted=calendar.getTime();

        } catch (ParseException e) {
            logger.info("IntToDate failed: {}", e);

        }
        return Converted;

    }

    public  static String IntToShortDate(int day){
        Date date=IntToDate(day);
        String formatDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        return formatDate;

    }

    public  static String StringToShortDate(String dayStr){
        int index = dayStr.indexOf(',');
        int day = -1 == index ? Integer.parseInt(dayStr) : Integer.parseInt(dayStr.substring(0, index));
        return IntToShortDate(day);
    }

    public  static String IntToTime(int second){
        int sec=second%60;
        int totMin=second/60;
        int min=totMin%60;
        int hours=totMin/60;
        String time="";
        return time.format("%s:%s:%s",ToTime(hours),ToTime(min),ToTime(sec));

    }

    public  static String StringToTime(String secondStr){
        int index = secondStr.indexOf(',');
        int second = -1 == index ? Integer.parseInt(secondStr) : Integer.parseInt(secondStr.substring(index + 1));
        return IntToTime(second);
    }

    private static String ToTime(int num){
        String time="";
        if (num<10){
            return time.format("0%s",num);
        }
        return time.format("%s",num);

    }


    //2016-08-03 00:00:00.000 64133
    public static void main(String[] args){
        String date =IntToShortDate(64243);
        String time=IntToTime(32761);
        System.out.println(date);
        System.out.println(time);
    }
}
