package com.mutao.pay.util;


import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 线程安全的订单产生器
 *
 * @author wansishuang
 * @date 2019-08-01
 */
public class OrderNoCenter {

    public static final Logger logger = LoggerFactory.getLogger(OrderNoCenter.class);

    private OrderNoCenter() {
    }
    private static class OrderNoCenterHolder{
        private static OrderNoCenter instance = new OrderNoCenter();
    }

    public static OrderNoCenter getInstance() {
        return OrderNoCenterHolder.instance;
    }


    /**
     * 机器标识位数
     */
    private final long workerIdBits = 10L;
    /**
     *
     */
    private long lastTimestamp = -1L;

    /**
     * 节点 ID 默认取1
     */
    private long workerId = 1;

    /**
     * 毫秒内自增位
     */
    private final long sequenceBits = 12L;


    /**
     * 时间毫秒左移22位
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits;


    /**
     * 机器ID偏左移12位
     */
    private final long workerIdShift = sequenceBits;

    /**
     * 序列id 默认取1
     */
    private long sequence = 1;

    private final long sequenceMask = -1L ^ (-1L << sequenceBits);


    /**
     * 获取id 线程安全
     *
     * @return
     */
    private synchronized String nextNo() {
        long timestamp = timeGen();
        // 时间错误
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards.");
        }
        // 当前毫秒内，则+1
        if (lastTimestamp == timestamp) {
            // 当前毫秒内计数满了，则等待下一秒
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;

        long id = ((timestamp % 1000) << timestampLeftShift) | (workerId << workerIdShift) | sequence;

        // ID偏移组合生成最终的ID，并返回ID,最大十位数
        return FastDateFormat.getInstance("yyyyMMddHHmmssSSS").format(timestamp)  + String.format("%010d",id);

    }

    /**
     * 等待下一个毫秒的到来
     *
     * @param lastTimestamp
     * @return
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }
    private long timeGen() {
        return System.currentTimeMillis();
    }

    public String create() {
        return nextNo();
    }


    public static void main(String[] args){
        for (int i = 0; i < 100; i++) {
            System.out.println(OrderNoCenter.getInstance().create());
        }
    }

}
