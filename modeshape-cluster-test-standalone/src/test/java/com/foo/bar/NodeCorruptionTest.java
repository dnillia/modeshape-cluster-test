package com.foo.bar;

import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.ModeShapeEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The test to reproduce a permanent corruption of the node, when it cannot be locked anymore.
 * 
 * @author Illia Khokholkov
 *
 */
public class NodeCorruptionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeCorruptionTest.class);
    
    private static final int LOCK_HOLD_SECONDS = 15;
    private static final int CLUSTER_SIZE = 5;
    private static final int THREAD_COUNT = 25;
    private static final int RETRY_ATTEMPT_COUNT = 5;
    private static final int TARGETED_UPDATE_COUNT = 25;
    
    private static final String PARENT_RELATIVE_PATH = "parent";
    private static final String PARENT_ABSOLUTE_PATH = "/parent";
    
    private static final List<Integer> DELAYS = Arrays.asList(
            4000, 3000, 1000, 7000, 6000,
            1000, 3000, 5000, 3000, 6000,
            5000, 6000, 6000, 1000, 6000,
            7000, 5000, 5000, 2000, 7000,
            1000, 4000, 1000, 2000, 4000);
    
    private static ModeShapeEngine engine;
    private static CircularRepositoryIterator iterator;
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        System.setProperty("lock.hold.timeout", Integer.toString(LOCK_HOLD_SECONDS));
        
        if (DELAYS.size() != THREAD_COUNT) {
            throw new IllegalStateException("The thread count should match the number of delays");
        }
        
        engine = new ModeShapeEngine();
        engine.start();
        
        List<Repository> repositories = new ArrayList<>();
        for (int i = 0; i < CLUSTER_SIZE; i++) {
            repositories.add(AbstractModeShapeClusterTest.createRepository(engine));
        }
        
        iterator = new CircularRepositoryIterator(repositories);
        Session session = AbstractModeShapeClusterTest.createSession(iterator.next());
        
        try {
            if (!session.nodeExists(PARENT_ABSOLUTE_PATH)) {
                Node parent = session.getRootNode().addNode(PARENT_RELATIVE_PATH);
                parent.addMixin("mix:lockable");
                session.save();
            }
        } finally {
            session.logout();
        }
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
        if (engine != null) {
            engine.shutdown();
        }
    }
    
    @Test
    public void nodeCannotBeCorrupted() throws Exception {
        Session startupSession = AbstractModeShapeClusterTest.createSession(iterator.next());
        boolean isNodeCorrupted = false;
        
        try {
            Node parent = startupSession.getNode(PARENT_ABSOLUTE_PATH);
            isNodeCorrupted = NodeLockHelper.isNodeCorrupted(parent);
            
            NodeLockHelper.lockNode(parent);
            NodeLockHelper.unlockSuspendNotActive(parent);
            
        } catch (Exception e) {
            fail(String.format("In the beginning of the run, the parent node should be lockable "
                    + "[nodeCorrupted=%s]", isNodeCorrupted), e);
            
        } finally {
            startupSession.logout();
        }
        
        List<Callable<Object>> tasks = new ArrayList<>();
        
        for (int i = 0; i < TARGETED_UPDATE_COUNT; i++) {
            MutableObject<Integer> index = new MutableObject<>(i);
            
            tasks.add(() -> {
                try {
                    Session session = AbstractModeShapeClusterTest.createSession(iterator.next());
                    String childNode = String.format("child-%s", index.getValue() + 1);
                    
                    try {
                        return new RetryAction(
                                () -> {
                                    try {
                                        Node parent = session.getNode(PARENT_ABSOLUTE_PATH);
                                        NodeLockHelper.lockNode(parent);
                                        
                                        try {
                                            return TransactionExecutor.runInTransaction(() -> {
                                                Node child = parent.addNode(childNode);
                                                child.addMixin("mix:lockable");
                                                
                                                session.save();
                                                
                                                return child;
                                            });
                                            
                                        } finally {
                                            NodeLockHelper.unlockSuspendNotActive(parent);
                                        }
                                    } catch (Exception e) {
                                        LOGGER.trace("An exception occurred", e);
                                        
                                        return null;
                                    }
                                },
                                String.format("Adding a child node [relativePath=%s]", childNode)
                                
                        ).retry(RETRY_ATTEMPT_COUNT, DELAYS.get(index.getValue()));
                        
                    } finally {
                        session.logout();
                    }
                    
                } catch (Exception e) {
                    LOGGER.trace("Execution failed", Thread.currentThread().getName(), e);
                    return null;
                }
            });
        }
        
        ExecutorService executorService = ConcurrencyHelper.createExecutorService(
                THREAD_COUNT, "in-memory-corruption-");
        
        try {
            for (Future<Object> future : executorService.invokeAll(tasks)) {
                future.get();
            }
        } finally {
            ConcurrencyHelper.closeExecutorService(executorService, TimeUnit.SECONDS.toMillis(60));
        }
        
        long sleepTimeSeconds = LOCK_HOLD_SECONDS + 5;
        
        LOGGER.info("Sleeping for [{}] seconds before attempting to perform final lock/unlock of "
                + "the parent node", sleepTimeSeconds);
        
        Thread.sleep(TimeUnit.SECONDS.toMillis(sleepTimeSeconds));
        
        new RetryAction(
                () -> {
                    Session session = null;
                    try {
                        session = AbstractModeShapeClusterTest.createSession(iterator.next());
                        
                        Node parent = session.getNode(PARENT_ABSOLUTE_PATH);
                        NodeLockHelper.lockNode(parent);
                        NodeLockHelper.unlockSuspendNotActive(parent);
                        
                        return "";
                        
                    } catch (Exception e) {
                        LOGGER.trace("An exception occurred", e);
                        return null;
                        
                    } finally {
                        if (session != null) {
                            session.logout();
                        }
                    }
                },
                String.format("Lock/unlock parent node in the end of the test run [path=%s]",
                        PARENT_ABSOLUTE_PATH)
                
        ).retry(RETRY_ATTEMPT_COUNT, 1_000);
    }
}
