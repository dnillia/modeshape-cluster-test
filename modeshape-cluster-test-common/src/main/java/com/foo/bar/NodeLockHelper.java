package com.foo.bar;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.modeshape.jcr.JcrLexicon;

public class NodeLockHelper {

    public static final String LOCK_HOLD_TIMEOUT_PROPERTY = "lock.hold.timeout";
    private static final String LOCK_HOLD_TIMEOUT = System.getProperty(LOCK_HOLD_TIMEOUT_PROPERTY, "180");
    
    public static void lockNode(Node node) {
        TransactionExecutor.forceRunInTransaction(() -> {
            return node.getSession().getWorkspace().getLockManager().lock(
                    node.getPath(),
                    false,
                    false,
                    TimeUnit.SECONDS.toSeconds(Integer.parseInt(LOCK_HOLD_TIMEOUT)),
                    null);
        });
    }
    
    public static void unlockSuspendNotActive(Node node) throws RepositoryException {
        Session session = node.getSession();
        session.refresh(false);
        
        TransactionExecutor.forceRunInTransaction(() -> {
            session.getWorkspace().getLockManager().unlock(node.getPath());
            
            return null;
        });
    }
    
    public static void unlockInNewThread(Node node) throws RepositoryException {
        Session session = node.getSession();
        session.refresh(false);
        
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        
        Future<?> result = executorService.submit(() -> {
            TransactionExecutor.runInTransaction(() -> {
                session.getWorkspace().getLockManager().unlock(node.getPath());
                
                return null;
            }); 
        });
        
        try {
            result.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static boolean isNodeCorrupted(Node node) throws RepositoryException {
        return !node.isLocked() && (node.hasProperty(JcrLexicon.LOCK_OWNER.toString())
                || node.hasProperty(JcrLexicon.IS_DEEP.toString()));
    }
    
    private NodeLockHelper() {}
}
