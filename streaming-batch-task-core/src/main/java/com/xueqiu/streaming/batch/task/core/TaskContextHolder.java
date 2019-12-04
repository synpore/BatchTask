package com.xueqiu.streaming.batch.task.core;


/**
 * @Description:
 * @Author:renxian
 * @Date:2019-12-04
 */
public class TaskContextHolder {

    public static ThreadLocal<TaskContext> taskContextThreadLocal = new ThreadLocal() {
        @Override
        protected TaskContext initialValue() {
            return TaskContext.builder().taskStatus(GlobalBatchTaskConfig.TaskStatus.NORMAL).build();
        }
    };


    public static void setStatus(GlobalBatchTaskConfig.TaskStatus taskStatus) {
        taskContextThreadLocal.get().setTaskStatus(taskStatus);
    }

    public static TaskContext get() {
        return taskContextThreadLocal.get();
    }

    public static void clear() {
        taskContextThreadLocal.remove();
    }

}
