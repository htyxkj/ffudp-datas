package com.ffudp.utils;

import java.lang.management.ManagementFactory;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateFormatUtils;

/**
 * 时间工具类
 */
public class DateUtils extends org.apache.commons.lang3.time.DateUtils
{
    public static String YYYY = "yyyy";

    public static String YYYY_MM = "yyyy-MM";

    public static String YYYY_MM_DD = "yyyy-MM-dd";

    public static String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";

    public static String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    
    private static String[] parsePatterns = {
            "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM", 
            "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM",
            "yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy.MM"};

    /**
     * 获取当前Date型日期
     * 
     * @return Date() 当前日期
     */
    public static Date getNowDate()
    {
        return new Date();
    }

    /**
     * 获取当前日期, 默认格式为yyyy-MM-dd
     * 
     * @return String
     */
    public static String getDate()
    {
        return dateTimeNow(YYYY_MM_DD);
    }

    public static final String getTime()
    {
        return dateTimeNow(YYYY_MM_DD_HH_MM_SS);
    }

    public static final String dateTimeNow()
    {
        return dateTimeNow(YYYYMMDDHHMMSS);
    }

    public static final String dateTimeNow(final String format)
    {
        return parseDateToStr(format, new Date());
    }

    public static final String dateTime(final Date date)
    {
        return parseDateToStr(YYYY_MM_DD, date);
    }

    public static final String parseDateToStr(final String format, final Date date)
    {
        return new SimpleDateFormat(format).format(date);
    }

    public static final Date dateTime(final String format, final String ts)
    {
        try
        {
            return new SimpleDateFormat(format).parse(ts);
        }
        catch (ParseException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * 日期路径 即年/月/日 如2018/08/08
     */
    public static final String datePath()
    {
        Date now = new Date();
        return DateFormatUtils.format(now, "yyyy/MM/dd");
    }

    /**
     * 日期路径 即年/月/日 如20180808
     */
    public static final String dateTime()
    {
        Date now = new Date();
        return DateFormatUtils.format(now, "yyyyMMdd");
    }

    /**
     * 日期型字符串转化为日期 格式
     */
    public static Date parseDate(Object str)
    {
        if (str == null)
        {
            return null;
        }
        try
        {
            return parseDate(str.toString(), parsePatterns);
        }
        catch (ParseException e)
        {
            return null;
        }
    }
    /**
     * 日期型字符串转化为日期 格式
     */
    public static Date parseDate(long str)
    {
        Calendar cal =Calendar.getInstance();
        cal.setTimeInMillis(str);
        return cal.getTime();
    }
    /**
     * 获取服务器启动时间
     */
    public static Date getServerStartDate()
    {
        long time = ManagementFactory.getRuntimeMXBean().getStartTime();
        return new Date(time);
    }

    /**
     * 计算两个时间差
     */
    public static String getDatePoor(Date endDate, Date nowDate)
    {
        long nd = 1000 * 24 * 60 * 60;
        long nh = 1000 * 60 * 60;
        long nm = 1000 * 60;
        // long ns = 1000;
        // 获得两个时间的毫秒时间差异
        long diff = endDate.getTime() - nowDate.getTime();
        // 计算差多少天
        long day = diff / nd;
        // 计算差多少小时
        long hour = diff % nd / nh;
        // 计算差多少分钟
        long min = diff % nd % nh / nm;
        // 计算差多少秒//输出结果
        // long sec = diff % nd % nh % nm / ns;
        return day + "天" + hour + "小时" + min + "分钟";
    }

    /**
     * 获取当前年份
     */
    public static String getCurrentYear()
    {
        Date now = new Date();
        return DateFormatUtils.format(now, YYYY);
    }

    /**
     * 获取当前年份
     */
    public static String getYear(Date date)
    {
        if(date == null){
            return "";
        }
        return DateFormatUtils.format(date, YYYY);
    }


    /**
     * 获取当前年份
     */
    public static Date getYear()
    {
        try {
            return parseDate(getCurrentYear(),YYYY);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> findDates(Date dStart, Date dEnd) {
        Calendar cStart = Calendar.getInstance();
        cStart.setTime(dStart);

        List<String> dateList = new ArrayList();
        // 把起始日期加上
        dateList.add(DateUtils.dateTime(dStart));
        // 此日期是否在指定日期之后
        while (dEnd.after(cStart.getTime())) {
            // 根据日历的规则，为给定的日历字段添加或减去指定的时间量
            cStart.add(Calendar.DAY_OF_MONTH, 1);
            dateList.add(DateUtils.dateTime(cStart.getTime()));
        }
        return dateList;
    }


    /**
     * 获得当天的开始时间
     *
     * @return
     */
    public static Date getCurrentDayStartTime() {
        return getSomeDayStartTime(getTime());
    }

    /**
     * 获得当天的结束时间
     *
     * @return
     */
    public static Date getCurrentDayEndTime() {
        return getSomeDayEndTime(getTime());
    }

    /**
     * 获得某天的结束时间
     *
     * @return
     */
    public static Date getSomeDayEndTime(String date) {
        return getSomeDayEndTime(parseDate(date));
    }

    /**
     * 获得日期的开始或结束时间
     *
     * @return
     */
    public static Date getSomeDayStartOrEndTime(Date date,boolean bend) {
        try {
            date = parseDate((dateTime(date) + (bend?" 23:59:59":" 00:00:00")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;
    }


    /**
     * 获得本周的第一天，周一
     *
     * @return
     */
    public static Date getCurrentWeekDayStartTime() {
        Calendar c = Calendar.getInstance();
        int weekday = c.get(Calendar.DAY_OF_WEEK) - 2;
        c.add(Calendar.DATE, -weekday);
        return getSomeDayStartTime(c.getTime());
    }

    /**
     * 获得日期的开始时间
     *
     * @return
     */
    public static Date getSomeDayStartTime(String date) {
        return getSomeDayStartOrEndTime(parseDate(date), false);
    }

    /**
     * 获得日期的开始时间
     *
     * @return
     */
    public static Date getSomeDayStartTime(Date date) {
        return getSomeDayStartOrEndTime(date, false);
    }
    /**
     * 获得日期的结束时间
     *
     * @return
     */
    public static Date getSomeDayEndTime(Date date) {

        return getSomeDayStartOrEndTime(date, true);
    }

    /**
     * 获得本周的最后一天，周日
     *
     * @return
     */
    public static Date getCurrentWeekDayEndTime() {
        Calendar c = Calendar.getInstance();
        int weekday = c.get(Calendar.DAY_OF_WEEK);
        c.add(Calendar.DATE, 8 - weekday);
        return getSomeDayEndTime(c.getTime());
    }

    /**
     * 获得本月的开始时间
     *
     * @return
     */
    public static Date getCurrentMonthStartTime() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DATE, 1);
        return getSomeDayStartTime(c.getTime());
    }
    /**
     * 本月的结束时间
     *
     * @return
     */
    public static Date getCurrentMonthEndTime() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DATE, 1);
        c.add(Calendar.MONTH, 1);
        c.add(Calendar.DATE, -1);
        return getSomeDayEndTime(c.getTime());
    }

    /**
     * 获取前/后半年的开始时间
     *
     * @return
     */
    public static Date getHalfYearStartTime() {
        Calendar c = Calendar.getInstance();
        int currentMonth = c.get(Calendar.MONTH) + 1;
        if (currentMonth >= 1 && currentMonth <= 6) {
            c.set(Calendar.MONTH, 0);
        } else if (currentMonth >= 7 && currentMonth <= 12) {
            c.set(Calendar.MONTH, 6);
        }
        c.set(Calendar.DATE, 1);
        return getSomeDayStartTime(c.getTime());

    }

    /**
     * 获取前/后半年的结束时间
     *
     * @return
     */
    public static Date getHalfYearEndTime() {
        Calendar c = Calendar.getInstance();
        int currentMonth = c.get(Calendar.MONTH) + 1;
        if (currentMonth >= 1 && currentMonth <= 6) {
            c.set(Calendar.MONTH, 5);
            c.set(Calendar.DATE, 30);
        } else if (currentMonth >= 7 && currentMonth <= 12) {
            c.set(Calendar.MONTH, 11);
            c.set(Calendar.DATE, 31);
        }
        return getSomeDayEndTime(c.getTime());
    }




    public static void main(String[] args) {
        long l1 = 1588608000000L;

        Date date2 = DateUtils.parseDate(l1);
        System.out.println(date2);
        System.out.println(parseDateToStr("yyyy-MM-dd HH:mm:ss",date2));

//        Date date = DateUtils.parseDate("2020-05-01");
//        List<String> list = findDates(date,new Date());
//        for (String date1 : list) {
//            System.out.println(date1);
//        }
//        System.out.println(parseDateToStr("yyyy-MM-dd HH:mm:ss",getSomeDayStartOrEndTime(date,false)));
//        System.out.println(parseDateToStr("yyyy-MM-dd HH:mm:ss",getCurrentWeekDayStartTime()));
//        System.out.println(parseDateToStr("yyyy-MM-dd HH:mm:ss",getCurrentMonthStartTime()));
//        System.out.println(parseDateToStr("yyyy-MM-dd HH:mm:ss",getCurrentMonthEndTime()));
    }
}
