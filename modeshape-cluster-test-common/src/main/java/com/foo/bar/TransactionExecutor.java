package com.foo.bar;

import java.util.concurrent.Callable;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * A utility class to allow for an execution of an action in the transactional context.
 * 
 * @author Illia Khokholkov
 *
 */
public class TransactionExecutor {

    private static final TransactionManager MANAGER = new ArjunaTransactionManagerLookup().getTransactionManager();
    
    public static <T> T runInTransaction(Callable<T> callable) {
        boolean commit = true;

        try {
            if (!isActive()) {
                MANAGER.begin();
            } else {
                commit = false;
            }

            T result = callable.call();
            
            if (isActive() && commit) {
                MANAGER.commit();
            }

            return result;

        } catch (RuntimeException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(e);

        } finally {
            try {
                if (isActive() && commit) {
                    MANAGER.rollback();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private static boolean isActive() throws SystemException {
        Transaction transaction = MANAGER.getTransaction();
        return transaction != null && transaction.getStatus() == Status.STATUS_ACTIVE;
    }
    
    private TransactionExecutor() {}
}
