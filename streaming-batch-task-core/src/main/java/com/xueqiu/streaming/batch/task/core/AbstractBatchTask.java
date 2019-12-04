package com.xueqiu.streaming.batch.task.core;




import cn.hutool.core.lang.Assert;
import com.xueqiu.streaming.batch.task.common.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Description:
 * @Author:renxian
 * @Date:2019-12-04
 */
public abstract class AbstractBatchTask implements BatchTask {

    public static final Logger logger = LoggerFactory.getLogger(AbstractBatchTask.class);

    private static final TaskManageContainer TASK_CONTAINER = TaskManageContainer.TaskManageContainerFactory.getInstance();

    private static  final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE;

    protected static final ExecutorService COMMON_THREAD_POOL;

    protected String taskName;

    protected int threadCount;

    protected ExecutorService executorService;

    protected PooledResourceStrategy pooledResourceStrategy;

    static {
        COMMON_THREAD_POOL = Executors.newFixedThreadPool(8, new NamedThreadFactory("batchTask-common-pool"));
        SCHEDULED_EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("batchTask-scheduled"));
        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(() -> TASK_CONTAINER.clearTaskResource(), 5, 10, TimeUnit.MINUTES);
    }


    public AbstractBatchTask(String taskName, int threadCount, PooledResourceStrategy strategy,ExecutorService executorService) {
        this.taskName = taskName;
        this.threadCount = threadCount <= 0 ? GlobalBatchTaskConfig.DEFAULT_THREAD_NUM : threadCount;
        this.pooledResourceStrategy = strategy;
        this.executorService=executorService;
    }

    @Override
    public void beginTask() {
        try {
            if (TASK_CONTAINER.checkTask(taskName) && hasData()) {
                init();
                execute();
                notice();
            }
        } catch (Exception e){
                handleException(e);
                logger.info(getTaskSymbolStr()+" occur exception",e);
        }finally {
            finish();
        }
    }

    protected abstract void execute();

    protected abstract boolean hasData();

    protected void notice() {
    }

    protected String getTaskSymbolStr() {
        return taskName;
    }

    private void handleException(Exception e) {
        if (e instanceof NoResourceException) {
            TaskContextHolder.setStatus(GlobalBatchTaskConfig.TaskStatus.NO_RESOURCE);
        } else {
            TaskContextHolder.setStatus(GlobalBatchTaskConfig.TaskStatus.END_EXCEPTION);
        }
    }

    //检查线程资源
    private void checkThreadResource() throws RuntimeException {
        if (!GlobalBatchTaskConfig.getThreadResource(threadCount))
            throw new NoResourceException(getTaskSymbolStr() + " get thread resource failed");
    }

    private void init() {
        TaskManageContainer.TaskWrapper taskWrapper = TASK_CONTAINER.getTaskWrapper(taskName);
        switch (pooledResourceStrategy) {
            case CUSTOM:
                Assert.notNull(executorService,"executorService can't be null when choose custom pool strategy");
            case COMMON:
                executorService = COMMON_THREAD_POOL;
                break;
            case CREATE_DESTROY:
                checkThreadResource();
                executorService = Executors.newFixedThreadPool(threadCount, new NamedThreadFactory(taskName));
                break;
            case CACHED:
                checkThreadResource();
                executorService = taskWrapper.getExecutorService();
                if (executorService == null) {
                    //maybe has be cleared by demon thread
                    executorService = Executors.newFixedThreadPool(threadCount, new NamedThreadFactory(taskName));
                    taskWrapper.setExecutorService(executorService);
                }
        }
        taskWrapper.start();
        TaskContextHolder.get().setHasInitialized(true);
        logger.info(getTaskSymbolStr()+" complete initialization");
    }

    private void finish() {
        if(TaskContextHolder.get().getHasInitialized()){
            switch (pooledResourceStrategy) {
                //自定义的线程池资源不在框架控制范围内
                case CUSTOM:
                    executorService.shutdown();
                    break;
                case CREATE_DESTROY:
                    GlobalBatchTaskConfig.releaseThreadResource(threadCount);
                    executorService.shutdown();
                    break;
                case CACHED:
                    GlobalBatchTaskConfig.releaseThreadResource(threadCount);

            }
        }
        TASK_CONTAINER.finishTask(taskName);
        TaskContextHolder.clear();
        logger.info(getTaskSymbolStr()+" complete finish method");
    }
}
