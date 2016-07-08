package com.foo.bar;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The utility class to help with concurrent executions. 
 * 
 * @author Illia Khokholkov
 *
 */
public class ConcurrencyHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrencyHelper.class);
    
    public static ExecutorService createExecutorService(int threadCount, String groupName) {
        return Executors.newFixedThreadPool(
                threadCount, new DaemonThreadFactory(groupName));
    }
    
    public static void closeExecutorService(ExecutorService executorService, long timeout) throws InterruptedException {
        executorService.shutdown();

        if (!executorService.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
            List<Runnable> tasks = executorService.shutdownNow();

            LOGGER.debug("The [{}] task(s) never commenced execution due to termination of the "
                    + "executor service", tasks.size());
        }
    }
    
    private ConcurrencyHelper() {}
    
    /**
     * The custom {@link ThreadFactory} for producing daemon threads.
     * 
     * @author Illia Khokholkov
     *
     */
    private static class DaemonThreadFactory implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger(1);
        private final String groupName;
        
        public DaemonThreadFactory(String groupName) {
            this.groupName = groupName;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            
            thread.setDaemon(true);
            thread.setName(groupName + counter.getAndIncrement());

            return thread;
        }
    }
}
