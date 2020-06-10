package com.ffudp.utils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Calendar;

/**
 * 工具类，用于处理日期格式化
 * @author fengzejiang
 */
public class Tools {
    /**
     * 日期转化成字符串，参数中指定格式化方式(YM,YMD,YMDT,T,...在常量表中定义)
     */
    public static String dateToString(Calendar calrd, boolean bdiv, int fmt) {
        if (calrd == null)
            return null;
        if (fmt == com.ffudp.cl.ICL.DF_Y)
            return String.valueOf(calrd.get(Calendar.YEAR));//--年
        if (fmt == com.ffudp.cl.ICL.DF_M)
            return String.valueOf(calrd.get(Calendar.MONTH) + 1);//--月
        int xs[] = dateToAry(calrd);
        int x0 = 0, x1 = 6;
        if (fmt == com.ffudp.cl.ICL.DF_YM)
            x1 = 2;//-年月
        else if (fmt == com.ffudp.cl.ICL.DF_YMD)
            x1 = 3;//-年月日
        else if (fmt == com.ffudp.cl.ICL.DF_YMDHM)
            x1 = 5;//-年月日时分秒
        else if (fmt == com.ffudp.cl.ICL.DF_T)
            x0 = 3;//时分秒
        else if (fmt == com.ffudp.cl.ICL.DF_HM) {
            x0 = 3;//时分
            x1 = 5;
        } else if (fmt == com.ffudp.cl.ICL.DF_DYNC)
            x1 = xs[6];//--时分秒一项不为0时,包括时分秒,否则只有日期。
        return dateToString(xs, x0, x1, bdiv);
    }
    /**
     * 日期数组转化成日期格式
     */
    public static String dateToString(int[] xs, int x0, int x1, boolean bdiv) {
        int iv;
        StringBuffer sb = new StringBuffer(20);
        if (bdiv) {
            //带分隔符。
            String sdiv = "--- :: ";
            while (x0 < x1) {
                sb.append(sdiv.charAt(x0));
                iv = xs[x0];
                if (iv < 10)
                    sb.append("0");
                sb.append(iv);
                x0++;
            }
            return sb.toString().substring(1);//--带分隔符,如:2011-10-10
        }
        while (x0 < x1) {
            iv = xs[x0];
            if (iv < 10)
                sb.append("0");
            sb.append(iv);
            x0++;
        }
        return sb.toString();//--不带分隔符,如:20111010
    }

    /**
     * 日期对象转化成整数,数组的长度为7,最后一项=6时表示有时分秒,=3时表示只有年月日。
     */
    public static int[] dateToAry(Calendar calrd) {
        if (calrd == null)
            return null;
        int[] xs = new int[7];
        xs[0] = calrd.get(Calendar.YEAR);
        xs[1] = calrd.get(Calendar.MONTH) + 1;
        xs[2] = calrd.get(Calendar.DATE);
        int h = calrd.get(Calendar.HOUR_OF_DAY), m = calrd.get(Calendar.MINUTE), s = calrd.get(Calendar.SECOND);
        boolean b0 = h > 0 || m > 0 || s > 0;
        if (b0) {
            xs[3] = h;
            xs[4] = m;
            xs[5] = s;
        }
        xs[6] = b0 ? 6 : 3;//;--标识是否有时分秒。
        return xs;
    }

    public static int getIntValue(byte[] bc,int offset){
        byte[] b1 = new byte[4];
        System.arraycopy(bc, offset, b1, 0, 4);
        return ByteBuffer.wrap(b1).getInt();
    }

    public static long getLongValue(byte[] bc,int offset){
        byte[] b1 = new byte[8];
        System.arraycopy(bc, offset, b1, 0, 8);
        return ByteBuffer.wrap(b1).getLong();
    }

    /**
     * 时间按照5s分成12等分
     * @param cal
     * @return
     */
    public static String getSplitZu(Calendar cal) {
        String s1 = dateToString(cal,false,8);
        int ss = cal.get(Calendar.SECOND);
        int kk = ss/4;
        s1 = s1.substring(0,s1.length()-2);
        s1+=(kk<10?"0"+kk:kk);
        return  s1;
    }

    /**
     * 时间按照5s分成12等分
     * @return
     */
    public static synchronized String getSplitZu() {
        Calendar cal = Calendar.getInstance();
        String s1 = dateToString(cal,false,8);
        int ss = cal.get(Calendar.SECOND);
        int kk = ss/4;
        s1 = s1.substring(0,s1.length()-2);
        s1+=(kk<10?"0"+kk:kk);
        return  s1;
    }

    /**
     * 高低位数据转换
     * @param src
     * @return
     */
    public static byte[] hightLowTrans(byte[] src){
        byte[] bs = new byte[src.length];
        int k = src.length/2;
        int len = bs.length-1;
        for(int i=k;i>0;i--){
            int _i1 = i*2-1;//7
            int _s = _i1-1;//6
            byte b1 = src[_i1];
            byte b2 = src[_s];
            bs[len-_i1] = b2;
            bs[len-_s] = b1;
        }
        return bs;
    }


    public static Double bytes2Double(byte[] bs){
        return Double.longBitsToDouble(new BigInteger(bytesToHexString(hightLowTrans(bs)),16).longValue());
    }

    public static Integer bytes2Integer(byte[] bs){
        return new BigInteger(bytesToHexString(bs),16).intValue();
    }

    public static Float bytes2Float(byte[] bs){
        return Float.intBitsToFloat(new BigInteger(bytesToHexString(hightLowTrans(bs)), 16).intValue());

    }

    /**
     * 将二进制转16进制字符串
     * @param src
     * @return
     */
    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (byte aSrc : src) {
            int v = aSrc & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public static synchronized Long getId(){
        return System.currentTimeMillis();
    }
}
