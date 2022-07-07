package com.synpore.streaming.batch.task.core;

/**
 * @Description:
 * @Author:renxian
 * @Date:2019-12-04
 */
public enum PooledResourceStrategy {
    /**
     * 通用线程池,所有任务通用,建议低级别的任务使用
     */
    COMMON(1),
    /**
     * 自定义线程池任务完成后销毁
     */
    CUSTOM(2),
    /**
     * 缓存线程池,线程不会频繁的创建和销毁,由demon线程10分钟清理一次休息时间超过一个小时的任务线程池,建议执行频率较高的任务使用此策略
     */
    CACHED(3),
    /**
     *  即时创建和销毁,建议一天只跑几次的任务使用此策略
     */
    CREATE_DESTROY(4),

    ;

    private int strategyType;

    PooledResourceStrategy(int strategyType) {
        this.strategyType = strategyType;
    }
}
