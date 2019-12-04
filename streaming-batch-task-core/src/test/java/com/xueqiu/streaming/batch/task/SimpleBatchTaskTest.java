package com.xueqiu.streaming.batch.task;


import com.xueqiu.streaming.batch.task.core.PooledResourceStrategy;
import com.xueqiu.streaming.batch.task.core.SimpleBatchTask;
import com.xueqiu.streaming.batch.task.core.SimpleTaskConfig;
import org.junit.Test;


import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

public class SimpleBatchTaskTest {

    //test  task get empty data
    @Test
    public void noDataTest() {
        for (int i = 0; i < 3; i++) {
            SimpleTaskConfig<String> simpleTaskConfig = SimpleTaskConfig.<String>builder()
                    .taskName("noDataTest")
                    .size(1000)
                    .threadNum(2)
                    .strategy(PooledResourceStrategy.CREATE_DESTROY)
                    .indexInfo(r -> null)
                    .identifier(r -> r)
                    .pullData((index, size) -> Collections.emptyList())
                    .jobContent((e) -> {
                        try {
                            Thread.sleep(1000 * 10);
                        } catch (InterruptedException e1) {
                        }
                        return true;
                    })
                    .build();
            SimpleBatchTask.buildTask(simpleTaskConfig).beginTask();
        }
    }

    //test  task in serial
    @Test
    public void getResourceTest() {
        for (int i = 0; i < 3; i++) {
            SimpleTaskConfig<String> simpleTaskConfig = SimpleTaskConfig.<String>builder()
                    .taskName("getResourceTest")
                    .size(1000)
                    .threadNum(2)
                    .strategy(PooledResourceStrategy.CREATE_DESTROY)
                    .indexInfo(r -> null)
                    .identifier(r -> r)
                    .pullData((index, size) -> Arrays.asList("111", "222"))
                    .jobContent((e) -> {
                        try {
                            Thread.sleep(1000 * 10);
                        } catch (InterruptedException e1) {
                        }
                        return true;
                    })
                    .build();
            SimpleBatchTask.buildTask(simpleTaskConfig).beginTask();
        }
    }

    //test  task resource restriction in parallel
    @Test
    public void checkResourceTest() {
        CountDownLatch countDownLatch = new CountDownLatch(20);
        for (int i = 0; i < 20; i++) {
            final int index=i%5;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SimpleTaskConfig<String> simpleTaskConfig = SimpleTaskConfig.<String>builder()
                            .taskName("checkResourceTest"+index)
                            .size(1000)
                            .threadNum(36)
                            .strategy(PooledResourceStrategy.CREATE_DESTROY)
                            .indexInfo(r -> null)
                            .identifier(r -> r)
                            .pullData((index, size) -> Arrays.asList("111", "222"))
                            .jobContent((e) -> {
                                try {
                                    Thread.sleep(1000 * 30);
                                } catch (InterruptedException e1) {
                                }
                                return true;
                            })
                            .build();
                    SimpleBatchTask.buildTask(simpleTaskConfig).beginTask();
                    countDownLatch.countDown();
                }
            }).start();
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {

        }
    }


}
