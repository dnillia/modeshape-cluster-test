package com.foo.bar;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.assertj.core.api.Assertions;
import org.jgroups.util.UUID;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The tests for the locking behavior.
 * 
 * @author Illia Khokholkov
 *
 */
public class LockingBehaviorTest extends AbstractModeShapeClusterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockingBehaviorTest.class);
    private static final int RETRY_ATTEMPT_COUNT = 2;
    
    /**
     * Tests whether a {@link LockException} can be thrown when adding nodes to a parent node in
     * a sequential order. The parent and child nodes are versioned. Note, that lock/unlock mechanism
     * is utilized when adding child nodes (shallow, open-scoped locks).
     * 
     * <ol>
     *   <li>Create a parent node.</li>
     *   <li>In order, add {@code N} child nodes, where each next request to add a node gets sent to the
     *       next available instance of the {@link Repository}. The access is controlled via
     *       {@link AbstractModeShapeClusterTest#repositoryIterator}. The number of running
     *       {@link Repository} instances is controlled via {@code cluster.size} system property.</li>
     *   <li>Expect a {@link LockException} some time during the test run. The number of members in
     *       the cluster does play a role. The exception, typically, does not get thrown when there is
     *       only a few members. However, if we get around {@code 5-10} members, it becomes more prominent.</li>
     * </ol>
     * 
     * @throws Exception
     *             if an error occurred
     */
    @Test
    public void addNodesInOrderNoTransaction() throws Exception {
        addNodes(false);
    }
    
    /**
     * The same as {@link #addNodesInOrderNoTransaction()}, except for there are user-defined
     * transactions when working with nodes.
     * 
     * @throws Exception
     *             if an error occurred        
     */
    @Test
    public void addNodesInOrderWithTransaction() throws Exception {
        addNodes(true);
    }
    
    @Test
    public void addNodesInParallelWithTransaction() throws Exception {
        String node = createParentNodes(repositoryIterator.next(), 1).iterator().next();
        List<Callable<Object>> tasks = new ArrayList<>();
        
        for (int i = 0; i < THREAD_COUNT * 2; i++) {
            long delay = TimeUnit.SECONDS.toMillis(RandomUtils.nextInt(1, 9));
            MutableObject<Integer> index = new MutableObject<>(i);
            
            tasks.add(() -> {
                try {
                    Session session = createSession(repositoryIterator.next());
                    String childNode = "child-" + index;
                    
                    try {
                        return new RetryAction(
                                () -> {
                                    try {
                                        return NodeHelper.safeAddNodeWithTransaction(
                                                session, node, childNode, Optional.empty());
                                        
                                    } catch (Exception e) {
                                        LOGGER.debug("An exception occurred", e);
                                        
                                        return null;
                                    }
                                },
                                childNode).retry(RETRY_ATTEMPT_COUNT, delay);
                        
                    } finally {
                        session.logout();
                    }
                    
                } catch (Exception e) {
                    LOGGER.error("Execution failed", Thread.currentThread().getName(), e);
                    return null;
                }
            });
        }
        
        ExecutorService executorService = ConcurrencyHelper.createExecutorService(
                THREAD_COUNT, "add-node-parallel-");
        
        try {
            int addCount = 0;
            
            for (Future<Object> future : executorService.invokeAll(tasks)) {
                Object result = future.get(5, TimeUnit.MINUTES);
                if (result != null) {
                    addCount++;
                }
            }
            
            Assertions.assertThat(addCount).isEqualTo(THREAD_COUNT * 2);
            
        } finally {
            ConcurrencyHelper.closeExecutorService(executorService, TimeUnit.SECONDS.toMillis(60));
        }
    }
    
    @Test
    public void updateNodeInParallel() throws Exception {
        String node = createParentNodes(repositoryIterator.next(), 1).iterator().next();
        List<Callable<Object>> tasks = new ArrayList<>();
        
        for (int i = 0; i < THREAD_COUNT * 2; i++) {
            long delay = TimeUnit.SECONDS.toMillis(RandomUtils.nextInt(1, 3));
            
            tasks.add(() -> {
                try {
                    Session session = createSession(repositoryIterator.next());
                    try {
                        return new RetryAction(
                                () -> {
                                    try {
                                        return NodeHelper.safeUpdateNode(
                                                session, node, UUID.randomUUID().toString());
                                        
                                    } catch (RepositoryException e) {
                                        LOGGER.trace("An exception occurred", e);
                                        
                                        return null;
                                    }
                                },
                                node).retry(RETRY_ATTEMPT_COUNT, delay);
                        
                    } finally {
                        session.logout();
                    }
                    
                } catch (Exception e) {
                    LOGGER.error("Execution failed", Thread.currentThread().getName(), e);
                    return null;
                }
            });
        }
        
        ExecutorService executorService = ConcurrencyHelper.createExecutorService(
                THREAD_COUNT, "update-node-parallel-");
        
        try {
            int updateCount = 0;
            
            for (Future<Object> future : executorService.invokeAll(tasks)) {
                Object result = future.get(5, TimeUnit.MINUTES);
                if (result != null) {
                    updateCount++;
                }
            }
            
            Assertions.assertThat(updateCount).isEqualTo(THREAD_COUNT * 2);
            
        } finally {
            ConcurrencyHelper.closeExecutorService(executorService, TimeUnit.SECONDS.toMillis(60));
        }
    }
    
    private static void addNodes(boolean transactionPerNode) throws Exception {
        String parentNode = createParentNodes(repositoryIterator.next(), 1).iterator().next();
        
        for (int i = 0; i < LEAF_NODE_COUNT; i++) {
            Session session = createSession(repositoryIterator.next());
            String childNode = "child-" + i;
            
            try {
                final String created;
                
                if (transactionPerNode) {
                    created = NodeHelper.safeAddNodeWithTransaction(session, parentNode, childNode, Optional.empty());
                } else {
                    created = NodeHelper.safeAddNodeNoTransaction(session, parentNode, childNode, Optional.empty());
                }
                
                LOGGER.debug("Created child node [path={}]", created);
                
            } finally {
                session.logout();
            }
        }
    }
}
