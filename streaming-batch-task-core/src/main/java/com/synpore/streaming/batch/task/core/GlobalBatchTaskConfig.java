package com.synpore.streaming.batch.task.core;

import java.util.concurrent.Semaphore;

/**
 * @Description:
 * @Author:renxian
 * @Date:2019-12-04
 */
public class GlobalBatchTaskConfig {
    /**
     * 默认批量执行大小
     */
    public static final int DEFAULT_BATCH_SIZE = 500;
    /**
     * 最大批量大小
     */
    public static final int MAX_BATCH_SIZE = 1000;

    public static final int DEFAULT_THREAD_NUM = 1;
    /**
     * 单任务最大线程数量
     */
    public static final int MAX_THREAD_NUM = 8;
    /**
     *线程池空闲最大时间
     */
    public static final long POOL_RESOURCE_MAX_IDLE_TIME=1000*60*60*1;

    /**
     *任务执行结果数据保存最长时间
     */
    public static final long STATISTICS_KEEP_TIME=1000*60*60*24;

    /**
     * 线程资源限制开关:true限制资源,false不做控制
     */
    public static boolean THREAD_RESOURCE_SWITCH=false;


    /**
     * 单实例控制总的运行线程数
     */
    private static final Semaphore THREAD_LIMIT = new Semaphore(100);

    public static boolean getThreadResource(int num) {
        if (!THREAD_RESOURCE_SWITCH) {
            return true;
        }
        return THREAD_LIMIT.tryAcquire(num);
    }

    public static void releaseThreadResource(int num) {
        if (THREAD_RESOURCE_SWITCH) {
            THREAD_LIMIT.release(num);
        }
    }


    enum  TaskStatus{
        NO_RESOURCE(1),
        IS_RUNNING(2),
        END_EXCEPTION(3),
        NORMAL(4)
        ;
        private int status;
        TaskStatus(int status) {
            this.status = status;
        }
    }
}
