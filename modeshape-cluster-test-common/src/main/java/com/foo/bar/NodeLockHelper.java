package com.foo.bar;

import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;

public class NodeLockHelper {

    public static final String LOCK_HOLD_TIMEOUT_PROPERTY = "lock.hold.timeout";
    private static final String LOCK_HOLD_TIMEOUT = System.getProperty(LOCK_HOLD_TIMEOUT_PROPERTY, "3");
    
    public static Lock lockNode(Node node) throws RepositoryException {
        return node.getSession().getWorkspace().getLockManager().lock(
                node.getPath(),
                false,
                true,
                TimeUnit.MINUTES.toSeconds(Integer.parseInt(LOCK_HOLD_TIMEOUT)),
                null);
    }
    
    public static void unlockNode(Node node) throws RepositoryException {
        node.getSession().getWorkspace().getLockManager().unlock(node.getPath());
    }
    
    private NodeLockHelper() {}
}
