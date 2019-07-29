package com.jht.heartratetest.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
    private  static  Date date;
    //获取当前时间
    public static String getTime() {
            date=new Date();
            SimpleDateFormat dateFormat= new SimpleDateFormat("hh:mm:ss");
            return dateFormat.format(date);

    }
}
