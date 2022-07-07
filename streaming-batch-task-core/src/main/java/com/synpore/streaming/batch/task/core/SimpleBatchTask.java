package com.synpore.streaming.batch.task.core;



import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;


/**
 * @Description:
 * @Author:renxian
 * @Date:2019-12-04
 */
public class SimpleBatchTask<T> extends AbstractBatchTask {

    public static final Logger logger = LoggerFactory.getLogger(SimpleBatchTask.class);

    private int size;

    private Long startIndex = 0l;

    private PullData<T> pullData;

    private JobContent<T> jobContent;

    private Function<T, Long> indexInfo;

    private Function<T, String> identifier;

    private CountDownLatch countDownLatch = new CountDownLatch(0);

    protected Vector<String> failedRecord=new Vector<>();

    public static SimpleBatchTask buildTask(SimpleTaskConfig taskConfig) {
        Assert.notNull(taskConfig.getJobContent(),"SimpleTaskConfig.jobContent");
        Assert.notNull(taskConfig.getPullData(),"SimpleTaskConfig.pullData");
        Assert.notNull(taskConfig.getTaskName(),"SimpleTaskConfig.taskName");
        Assert.notNull(taskConfig.getIndexInfo(),"SimpleTaskConfig.indexInfo");
        Assert.notNull(taskConfig.getIdentifier(),"SimpleTaskConfig.identifier");

        int size = taskConfig.getSize();
        if (size <= 0 || size > GlobalBatchTaskConfig.MAX_BATCH_SIZE) {
            size = GlobalBatchTaskConfig.DEFAULT_BATCH_SIZE;
        }
        int threadNum = taskConfig.getThreadNum();
        if (threadNum <= 0) {
            threadNum = GlobalBatchTaskConfig.DEFAULT_THREAD_NUM;
        } else if (threadNum > GlobalBatchTaskConfig.MAX_THREAD_NUM) {
            threadNum = GlobalBatchTaskConfig.MAX_THREAD_NUM;
        }
        SimpleBatchTask simpleBatchTask;
        if(PooledResourceStrategy.CUSTOM.equals(taskConfig.getStrategy())&&taskConfig.getExecutorService()==null){
            throw new IllegalArgumentException("executorService can't be null when choose custom pool strategy");
        }

        simpleBatchTask = new SimpleBatchTask(size, taskConfig.getPullData(), taskConfig.getJobContent(), taskConfig.getTaskName(), threadNum, taskConfig.getStrategy(),taskConfig.getExecutorService());
        simpleBatchTask.indexInfo = taskConfig.getIndexInfo();
        simpleBatchTask.identifier = taskConfig.getIdentifier();
        return simpleBatchTask;
    }

    private SimpleBatchTask(int size, PullData<T> pullData, JobContent<T> jobContent, String taskName, int threadNum, PooledResourceStrategy strategy,ExecutorService executorService) {
        super(taskName, threadNum, strategy,executorService);
        this.size = size;
        this.pullData = pullData;
        this.jobContent = jobContent;
    }

    @Override
    protected String getTaskSymbolStr() {
        return "SimpleBatchTask " + taskName;
    }

    @Override
    protected void execute() {
        Long index = startIndex;
        AtomicInteger successNum=TaskContextHolder.get().getSuccessNum();
        AtomicInteger failedNum=TaskContextHolder.get().getFailedNum();
        while (true) {
            List<T> data = null;
            try {
                data = pullData.doNow(index, size);
            } catch (Exception e) {
                logger.info("{} pull data exception,index={},size={}", getTaskSymbolStr(),index,size);
            }
            if (CollectionUtil.isEmpty(data)) {
                break;
            }
            this.countDownLatch = new CountDownLatch(data.size());
            data.stream().forEach((e) -> executorService.submit(() -> {
                        try {
                            if(logger.isDebugEnabled()){
                                logger.debug(getTaskSymbolStr()+"handle data "+identifier.apply(e));
                            }
                            if (jobContent.doNow(e)) {
                                successNum.getAndIncrement();
                            } else {
                                failedNum.getAndIncrement();
                                failedRecord.add(identifier.apply(e));
                            }
                        } catch (Exception ex) {
                            failedRecord.add(identifier.apply(e));
                            failedNum.getAndIncrement();
                        } finally {
                            this.countDown();
                        }
                    })
            );
            this.await();
            index = indexInfo.apply(data.get(data.size() - 1));
            if (data.size() < size)
                break;
        }
        this.await();
        logger.info("{} finished,failed_num={}" , getTaskSymbolStr(),failedRecord.size());
    }


    //拉取的第一批数据非空
    @Override
    protected boolean hasData() {
        return !CollectionUtil.isEmpty(pullData.doNow(startIndex, size));
    }

    private void countDown() {
        this.countDownLatch.countDown();
    }

    private void await() {
        try {
            this.countDownLatch.await();
        } catch (InterruptedException e) {
        }
    }

    @FunctionalInterface
    public interface PullData<T> {
        List<T> doNow(Long index, int size);
    }

    @FunctionalInterface
    public interface JobContent<T> {
        boolean doNow(T obj);
    }

}
