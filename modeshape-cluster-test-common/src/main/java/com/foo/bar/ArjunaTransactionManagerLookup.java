package com.foo.bar;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.modeshape.jcr.api.txn.TransactionManagerLookup;

/**
 * The custom {@link TransactionManagerLookup} that will always return an instance of
 * {@link com.arjuna.ats.jta.TransactionManager}.
 * 
 * @author Illia Khokholkov
 *
 */
public class ArjunaTransactionManagerLookup implements TransactionManagerLookup {

    public static final String TIMEOUT_PROPERTY = "transaction.timeout";
    
    @Override
    public TransactionManager getTransactionManager() {
        TransactionManager instance = com.arjuna.ats.jta.TransactionManager.transactionManager();
        if (instance == null) {
            throw new IllegalStateException("Failed to instantiate JBoss JTA transaction manager");
        }
        
        try {
            instance.setTransactionTimeout(Integer.parseInt(System.getProperty(TIMEOUT_PROPERTY, "3")));
        } catch (SystemException e) {
            throw new IllegalStateException("Failed to set global transaction timeout", e);
        }
        
        return instance;
    }
}
