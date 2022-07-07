package com.synpore.streaming.batch.task.core;

import lombok.Builder;
import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * @Description:
 * @Author:renxian
 * @Date:2019-12-04
 */
@Data
@Builder
public class TaskContext {

    @Builder.Default
    private GlobalBatchTaskConfig.TaskStatus taskStatus=GlobalBatchTaskConfig.TaskStatus.NORMAL;
    //资源初始化标识
    @Builder.Default
    private Boolean hasInitialized=false;
    //获取到任务执行权限
    @Builder.Default
    private Boolean isTaskOwner=false;
    @Builder.Default
    private AtomicInteger successNum=new AtomicInteger();
    @Builder.Default
    private AtomicInteger failedNum=new AtomicInteger();

}
