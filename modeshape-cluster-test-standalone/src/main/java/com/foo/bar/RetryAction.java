package com.foo.bar;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class to facilitate the retry attempts for a given action.
 * 
 * @author Illia Khokholkov
 *
 */
public class RetryAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryAction.class); 
    
    private final Supplier<Object> actionSupplier;
    private final String description;

    public RetryAction(Supplier<Object> actionSupplier, String description) {
        this.actionSupplier = actionSupplier;
        this.description = description;
    }
    
    public Object retry(int attemptCount, long delayMillis) throws InterruptedException {
        int currentAttempt = 1;
        
        while (currentAttempt <= attemptCount) {
            
            if (actionSupplier.get() != null) {
                LOGGER.debug("Retry attempt succeeded [description={}, currentAttempt={}, "
                        + "retryDelay={}, attemptCount={}]", description, currentAttempt, delayMillis, attemptCount);
                
                return actionSupplier.get();
            }
            
            LOGGER.debug("Retry attempt failed [description={}, currentAttempt={}, "
                    + "retryDelay={}, attemptCount={}]", description, currentAttempt, delayMillis, attemptCount);
            
            Thread.sleep(delayMillis);
            currentAttempt++;
        }
        
        throw new IllegalStateException(String.format("Failed to perform requested action "
                + "[thread=%s, description=%s]", Thread.currentThread().getName(), description));
    }
}
