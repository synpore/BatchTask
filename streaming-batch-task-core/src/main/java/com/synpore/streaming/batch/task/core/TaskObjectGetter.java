package com.synpore.streaming.batch.task.core;


/**
 * @Description:
 * @Author:renxian
 * @Date:2019-12-04
 */
@Deprecated
public interface TaskObjectGetter {
    
    Long getIndex();

    String getIdentifier();
}
