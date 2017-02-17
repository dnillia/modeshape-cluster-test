package com.foo.bar;

import java.nio.file.Paths;

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
    public static final String RECOVERY_DIR = "transaction.recovery.dir";
    
    static {
        System.setProperty(
                "com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean.defaultTimeout",
                System.getProperty(TIMEOUT_PROPERTY, "3"));
        
        System.setProperty(
                "com.arjuna.ats.arjuna.objectstore.objectStoreDir",
                System.getProperty(
                        RECOVERY_DIR,
                        Paths.get(System.getProperty("user.home"), "arjuna-jta-recovery").toString()));
    }
    
    @Override
    public TransactionManager getTransactionManager() {
        TransactionManager instance = com.arjuna.ats.jta.TransactionManager.transactionManager();
        
        if (instance == null) {
            throw new IllegalStateException("Failed to instantiate JBoss JTA transaction manager");
        }
        
        return instance;
    }
}
