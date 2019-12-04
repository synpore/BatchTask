package com.xueqiu.streaming.batch.task.core;

import lombok.Builder;
import lombok.Data;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * @Description:
 * @Author:renxian
 * @Date:2019-12-04
 */
@Builder
@Data
public class SimpleTaskConfig<T> {

    private SimpleBatchTask.PullData<T> pullData;

    private SimpleBatchTask.JobContent<T> jobContent;

    private Function<T, Long> indexInfo;

    private Function<T, String> identifier;

    private int size;

    private String taskName;

    private int threadNum;

    private ExecutorService executorService;

    @Builder.Default
    private PooledResourceStrategy strategy=PooledResourceStrategy.COMMON;
}
