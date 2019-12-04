[StreamingBatchTask](https://github.com/synpore/StreamingBatchTask.git)

### component to dealing with task which depends on a large amount of data easily and efficiently

#architecture

![](https://github.com/synpore/StreamingBatchTask/blob/master/batchTaskArchetecture.png?raw=true)

#what we solve？

1、too much data can't pull successfully at once

2、duplicate coding with foreach

3、control thread resource to avoid application crash

4、make sure only one task of same task name can run  at the same time

