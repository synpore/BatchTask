package com.xueqiu.streaming.batch.task.core;


import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Description:
 * @Author:renxian
 * @Date:2019-12-04
 */
public class TaskManageContainer {

    public static final Logger logger = LoggerFactory.getLogger(AbstractBatchTask.class);

    private ConcurrentHashMap<String, TaskWrapper> cachedTask = new ConcurrentHashMap<>();

    private TaskManageContainer() {
    }

    public TaskWrapper getTaskWrapper(String taskName) {
        return cachedTask.get(taskName);
    }

    public boolean checkTask(String taskName) {
        boolean result = false;
        TaskWrapper taskWrapper = cachedTask.get(taskName);
        try {
            if (taskWrapper == null) {
                TaskWrapper newTaskWrapper = TaskWrapper.builder().taskName(taskName).build();
                taskWrapper = newTaskWrapper;
                TaskWrapper previousTaskWrapper = cachedTask.putIfAbsent(taskName, newTaskWrapper);
                if (previousTaskWrapper != null) {
                    //use the first wrapper
                    taskWrapper = previousTaskWrapper;
                }
            }
            taskWrapper.getTaskLock().lock();
            if (taskWrapper.isRunning()) {
                TaskContextHolder.setStatus(GlobalBatchTaskConfig.TaskStatus.IS_RUNNING);
                logger.info("BatchTask-" + taskName + " is in processing");
                return result;
            }
            taskWrapper.setRunning(true);
            TaskContextHolder.get().setIsTaskOwner(true);
        } finally {
            taskWrapper.getTaskLock().unlock();
        }
        return true;
    }

    public void finishTask(String taskName) {
        cachedTask.get(taskName).finish();
    }

    public void clearTaskResource() {
        if (logger.isDebugEnabled()) {
            logger.debug("BatchTask clear task start.");
        }
        Map<String, TaskWrapper> snapShotMap = Collections.unmodifiableMap(cachedTask);
        long nowTime = Calendar.getInstance().getTimeInMillis();
        snapShotMap.forEach((k, v) -> {
            v.getTaskLock().lock();
            try {
                if (!v.isRunning() && nowTime - v.getLastFinishTime() > GlobalBatchTaskConfig.POOL_RESOURCE_MAX_IDLE_TIME) {
                    v.clearResource();
                    if (logger.isDebugEnabled()) {
                        logger.debug("BatchTask cleared task[" + v.getTaskName() + "] pool resource");
                    }
                }
                List<TaskResultInfo> waitToDelete = new ArrayList<>();
                v.getStatisticData().stream().forEach((e) -> {
                    if (nowTime - e.getStartTime() > GlobalBatchTaskConfig.STATISTICS_KEEP_TIME) {
                        waitToDelete.add(e);
                    }
                });
                v.getStatisticData().removeAll(waitToDelete);
            } finally {
                v.getTaskLock().unlock();
            }
        });
    }


    static class TaskManageContainerFactory {

        public static final TaskManageContainer INSTANCE = new TaskManageContainer();

        public static TaskManageContainer getInstance() {
            return INSTANCE;
        }
    }

    @Data
    @Builder
    static class TaskWrapper {
        private ExecutorService executorService;
        private String taskName;
        private long startTime;
        private long lastFinishTime;
        @Builder.Default
        private ReentrantLock taskLock = new ReentrantLock();
        @Builder.Default
        private volatile boolean running = false;
        @Builder.Default
        private AtomicInteger executeTimes = new AtomicInteger();
        @Builder.Default
        private final List<TaskResultInfo> statisticData = new ArrayList<>();


        public void start() {
            taskLock.lock();
            try {
                this.running = true;
                this.startTime = Calendar.getInstance().getTimeInMillis();
            } finally {
                taskLock.unlock();
            }
        }

        public void finish() {
            taskLock.lock();
            try {
                this.lastFinishTime = Calendar.getInstance().getTimeInMillis();
                this.executeTimes.getAndIncrement();
                TaskResultInfo taskResultInfo = TaskResultInfo.builder()
                        .startTime(this.startTime)
                        .costTime(this.lastFinishTime - this.startTime)
                        .taskStatus(TaskContextHolder.get().getTaskStatus())
                        .successCount(TaskContextHolder.get().getSuccessNum().intValue())
                        .failedCount(TaskContextHolder.get().getFailedNum().intValue())
                        .build();
                this.statisticData.add(taskResultInfo);
                //初始化了的任务获得了执行权
                if(TaskContextHolder.get().getIsTaskOwner()){
                    this.running = false;
                }
            } finally {
                taskLock.unlock();
            }
        }

        public void clearResource() {
            taskLock.lock();
            try {
                if (this.executorService != null) {
                    ExecutorService executorService = this.executorService;
                    executorService.shutdown();
                    if (this.executorService.isShutdown()) {
                        this.executorService = null;//help gc
                    }
                }
            } finally {
                taskLock.unlock();
            }
        }
    }

    @Data
    @Builder
    static class TaskResultInfo {
        private long startTime;
        private long costTime;
        private int successCount;
        private int failedCount;
        private GlobalBatchTaskConfig.TaskStatus taskStatus;
    }
}
