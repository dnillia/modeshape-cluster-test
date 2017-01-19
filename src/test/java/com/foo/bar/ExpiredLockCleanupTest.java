package com.foo.bar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

import org.junit.Test;

/**
 * The tests to verify expiration policies for the JCR locks.
 * 
 * @author Illia Khokholkov
 *
 */
public class ExpiredLockCleanupTest extends AbstractModeShapeClusterTest {

    /**
     * Checks cleanup of the shallow open-scoped lock. Note, that an attempt to unlock the node
     * is intentionally not performed anywhere in the code. 
     * 
     * @throws Exception
     *             if an error occurred
     */
    @Test
    public void cleanUpExpiredShallowOpenScopedLock() throws Exception {
        Session session = createSession(repositoryIterator.next());
        
        try {
            // Create a new lockable node
            Node node = session.getRootNode().addNode(UUID.randomUUID().toString());
            node.addMixin("mix:lockable");
            node.getSession().save();
            
            // Lock the node
            assertThat(lockNode(node)).isNotNull();
            assertThat(node.isLocked()).isTrue();
            
            // Wait enough time for the lock to be expired
            Thread.sleep(TimeUnit.MINUTES.toMillis(1));
            
            // Check if the lock can be obtained
            try {
                assertThat(lockNode(node)).isNotNull();
                assertThat(node.isLocked()).isTrue();
                
            } catch (LockException e) {
                fail("Unable to lock the node, because existing expired lock has not been removed", e);
            }
            
        } finally {
            session.logout();
        }
    }
    
    /**
     * Obtains a shallow open-scoped lock on the node. The lock is valid for {@code 1} second. 
     * 
     * @param node
     *            the node to lock
     *            
     * @return the created lock
     * 
     * @throws RepositoryException
     *             if an error occurred
     */
    private static Lock lockNode(Node node) throws RepositoryException {
        return node.getSession().getWorkspace().getLockManager().lock(node.getPath(), false, false, 1, null);
    }
}
