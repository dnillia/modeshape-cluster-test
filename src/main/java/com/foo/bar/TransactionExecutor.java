package com.foo.bar;

import java.util.concurrent.Callable;

import javax.transaction.TransactionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class to allow for an execution of an action in the transactional context.
 * 
 * @author Illia Khokholkov
 *
 */
public class TransactionExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionExecutor.class);
    private static final TransactionManager MANAGER = new CustomTransactionManagerLookup().getTransactionManager();
    
    public static <T> T runInTransaction(Callable<T> callable) {
        try {
            LOGGER.trace("Starting transaction...");
            MANAGER.begin();
            
            T result = callable.call();
            
            LOGGER.trace("Committing transaction...");
            MANAGER.commit();
            
            return result;
            
        } catch (Exception e1) {
            try {
                LOGGER.trace("Rolling back transaction...", e1);
                MANAGER.rollback();
                
            } catch (Exception e2) {
                throw new RuntimeException(e2);
            }
            
            throw new RuntimeException(e1);
        }
    }
    
    private TransactionExecutor() {}
}
