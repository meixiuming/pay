package com.mutao.pay.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;


/**
 * @author kongwn
 */
public class DateUtil {

	public static final String TIME_FMT = "yyyy-MM-dd HH:mm:ss";
	
	public static final String TIME_STR_FMT = "yyyyMMddHHmmss";
	
	/**
	 * Date类型转String  yyyy-MM-dd HH:mm:ss
	 * @param date
	 * @return
	 */
	public static String getTimeFmtStrByDate(Date date){
		SimpleDateFormat fmt = new SimpleDateFormat(TIME_FMT);
		String dateStr = fmt.format(date);
		return dateStr;
	}
	
	/**
	 * Date类型转String  yyyyMMddHHmmss
	 * @param date
	 * @return
	 */
	public static String getTimeStrByDate(Date date){
		SimpleDateFormat fmt = new SimpleDateFormat(TIME_STR_FMT);
		String dateStr = fmt.format(date);
		return dateStr;
	}
	
	/**
	 * String类型转Date  yyyy-MM-dd HH:mm:ss
	 * @param timeStr
	 * @return
	 */
	public static Date getDateByTimeFmtStr(String timeStr){
		SimpleDateFormat fmt = new SimpleDateFormat(TIME_FMT);
		    Date date = null;
		    try{
		    	date = fmt.parse(timeStr);
		    }catch(Exception e){
		    	return null;
		    }
		    return date;
	}
	
	/**
	 * 获取两个时间戳的差值，返回单位是秒
	 * 公式： end-start
	 * @param start
	 * @param end
	 * @return
	 */
	public static long getSecondsInterval(long start, long end){
		long milSec = getMilSecondsInterval(start, end);
		return TimeUnit.MILLISECONDS.toSeconds(milSec);
	}
	
	/**
	 * 获取两个时间戳的差值，返回单位是毫秒
	 * 公式： end-start
	 * @param start
	 * @param end
	 * @return
	 */
	public static long getMilSecondsInterval(long start, long end){
		return end-start;
	}
	
	
	/**
	 * 超时判断，单位毫秒
	 * @param start
	 * @param end
	 * @return
	 */
	public static boolean expireTm(long start, long end, long expTm){
		
		if(start > end){
			return true;
		}
		
		long interval = getMilSecondsInterval(start, end);
		if(interval > expTm){
			return true;
		}else{
			return false;
		}
		
	}
	
	
}
