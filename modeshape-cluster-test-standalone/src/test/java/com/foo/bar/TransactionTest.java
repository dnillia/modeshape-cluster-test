package com.foo.bar;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.ModeShapeEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The tests to validate the behavior of ModeShape when transactions are utilized.
 * 
 * @author Illia Khokholkov
 *
 */
public class TransactionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionTest.class);
    
    private static final String ABSOLUTE_PARENT_NODE_PATH = "/tnxParentNode";
    private static final String RELATIVE_PARENT_NODE_PATH = "tnxParentNode";

    private static ModeShapeEngine engine;
    private static Repository repository;
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        engine = new ModeShapeEngine();
        engine.start();
        
        repository = AbstractModeShapeClusterTest.createRepository(engine);
        
        Session session = AbstractModeShapeClusterTest.createSession(repository);
        try {
            Node parentNode = session.getRootNode().addNode(RELATIVE_PARENT_NODE_PATH);
            parentNode.addMixin("mix:lockable");
            
            session.save();
            
        } finally {
            session.logout();
        }
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
        if (engine != null) {
            engine.shutdown().get();
        }
    }
    
    // Caused by: java.lang.IllegalStateException: Cannot attempt to lock documents without an existing ModeShape transaction
    //   at org.modeshape.jcr.cache.document.LocalDocumentStore.lockDocuments(LocalDocumentStore.java:160)
    //   at org.modeshape.jcr.cache.document.LocalDocumentStore.lockDocuments(LocalDocumentStore.java:153)
    //   at org.modeshape.jcr.cache.document.WritableSessionCache.lockNodes(WritableSessionCache.java:1524)
    //
    
    @Test
    public void addOneNodeAbortAfterSaveUnlockSuspendAborted() throws Exception {
        Session session = AbstractModeShapeClusterTest.createSession(repository);
        
        String absoluteChildPath = ABSOLUTE_PARENT_NODE_PATH + "/" + "child";
        String relativeChildPath = "child";
        
        try {
            Node parentNode = session.getNode(ABSOLUTE_PARENT_NODE_PATH);
            NodeLockHelper.lockNode(parentNode);
            
            try {
                TransactionExecutor.runInTransaction(() -> {

                    LOGGER.trace("Adding a child node and saving the session");
                    
                    Node childNode1 = parentNode.addNode(relativeChildPath);
                    childNode1.addMixin("mix:lockable");
                    session.save();
                    
                    LOGGER.trace("Waiting for user transaction to expire");
                    
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                        
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    
                    return null;
                });
                
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).contains("Non-active user transaction");
                
            } finally {
                NodeLockHelper.unlockSuspendNotActive(parentNode);
            }
            
        } finally {
            session.logout();
        }
        
        Session newSession = AbstractModeShapeClusterTest.createSession(repository);
        try {
            assertThat(NodeLockHelper.isNodeCorrupted(newSession.getNode(ABSOLUTE_PARENT_NODE_PATH)))
                    .as("The parent node should not be locked or corrupted")
                    .isFalse();
            
            assertThat(newSession.nodeExists(absoluteChildPath))
                    .as("The child node should not be saved, because user transaction was aborted")
                    .isFalse();
        } finally {
            newSession.logout();
        }
    }
    
    @Test
    public void addOneNodeAbortBeforeSaveUnlockSuspendAborted() throws Exception {
        Session session = AbstractModeShapeClusterTest.createSession(repository);
        
        String absoluteChildPath = ABSOLUTE_PARENT_NODE_PATH + "/" + "child";
        String relativeChildPath = "child";
        
        try {
            Node parentNode = session.getNode(ABSOLUTE_PARENT_NODE_PATH);
            NodeLockHelper.lockNode(parentNode);
            
            try {
                TransactionExecutor.runInTransaction(() -> {

                    LOGGER.trace("Waiting for user transaction to expire");
                    
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                        
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    
                    LOGGER.trace("Attempting to add a new child node when initial user transaction "
                            + "has expired");
                    
                    Node childNode1 = parentNode.addNode(relativeChildPath);
                    childNode1.addMixin("mix:lockable");
                    session.save();
                    
                    return null;
                });
                
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).contains("Non-active user transaction");
                
            } finally {
                NodeLockHelper.unlockSuspendNotActive(parentNode);
            }
            
        } finally {
            session.logout();
        }
        
        Session newSession = AbstractModeShapeClusterTest.createSession(repository);
        try {
            assertThat(NodeLockHelper.isNodeCorrupted(newSession.getNode(ABSOLUTE_PARENT_NODE_PATH)))
                    .as("The parent node should not be locked or corrupted")
                    .isFalse();
            
            assertThat(newSession.nodeExists(absoluteChildPath))
                    .as("The child node should not be saved, because user transaction was aborted")
                    .isFalse();
        } finally {
            newSession.logout();
        }
    }
    
    // Caused by: java.lang.IllegalStateException: Cannot attempt to lock documents without an existing ModeShape transaction
    //   at org.modeshape.jcr.cache.document.LocalDocumentStore.lockDocuments(LocalDocumentStore.java:160)
    //   at org.modeshape.jcr.cache.document.LocalDocumentStore.lockDocuments(LocalDocumentStore.java:153)
    //   at org.modeshape.jcr.cache.document.WritableSessionCache.lockNodes(WritableSessionCache.java:1524)
    //
    @Test
    public void addTwoNodesAbortBeforeSecondSaveUnlockSuspendAborted() throws Exception {
        Session session = AbstractModeShapeClusterTest.createSession(repository);
        
        String absoluteChildPath1 = ABSOLUTE_PARENT_NODE_PATH + "/" + "child-1";
        String relativeChildPath1 = "child-1";
        
        String absoluteChildPath2 = ABSOLUTE_PARENT_NODE_PATH + "/" + "child-2";
        String relativeChildPath2 = "child-2";
        
        try {
            Node parentNode = session.getNode(ABSOLUTE_PARENT_NODE_PATH);
            NodeLockHelper.lockNode(parentNode);
            
            try {
                TransactionExecutor.runInTransaction(() -> {
                    
                    LOGGER.trace("Adding the first child node while user transaction is still active");
                    
                    Node childNode1 = parentNode.addNode(relativeChildPath1);
                    childNode1.addMixin("mix:lockable");
                    session.save();
                    
                    LOGGER.trace("Waiting for user transaction to expire");
                    
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                        
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    
                    LOGGER.trace("Adding the second child node when user transaction was aborted");
                    
                    Node childNode2 = parentNode.addNode(relativeChildPath2);
                    childNode2.addMixin("mix:lockable");
                    session.save();
                    
                    return null;
                });
                
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).contains("Non-active user transaction");
                
            } finally {
                NodeLockHelper.unlockSuspendNotActive(parentNode);
            }
            
        } finally {
            session.logout();
        }
        
        Session newSession = AbstractModeShapeClusterTest.createSession(repository);
        try {
            assertThat(newSession.nodeExists(absoluteChildPath1))
                    .as("The first child node should not be saved, because user transaction was aborted")
                    .isFalse();
            
            assertThat(newSession.nodeExists(absoluteChildPath2))
                    .as("The seconds child node should not be saved, because user transaction was aborted")
                    .isFalse();
        } finally {
            newSession.logout();
        }
    }
    
    // Caused by: java.lang.IllegalStateException: Cannot attempt to lock documents without an existing ModeShape transaction
    //   at org.modeshape.jcr.cache.document.LocalDocumentStore.lockDocuments(LocalDocumentStore.java:160)
    //   at org.modeshape.jcr.cache.document.LocalDocumentStore.lockDocuments(LocalDocumentStore.java:153)
    //   at org.modeshape.jcr.cache.document.WritableSessionCache.lockNodes(WritableSessionCache.java:1524)
    //
    @Test
    public void addTwoNodesAbortBeforeSecondSaveUnlockNewThread() throws Exception {
        Session session = AbstractModeShapeClusterTest.createSession(repository);
        
        String absoluteChildPath1 = ABSOLUTE_PARENT_NODE_PATH + "/" + "child-1";
        String relativeChildPath1 = "child-1";
        
        String absoluteChildPath2 = ABSOLUTE_PARENT_NODE_PATH + "/" + "child-2";
        String relativeChildPath2 = "child-2";
        
        try {
            Node parentNode = session.getNode(ABSOLUTE_PARENT_NODE_PATH);
            NodeLockHelper.lockNode(parentNode);
            
            try {
                TransactionExecutor.runInTransaction(() -> {
                    
                    LOGGER.trace("Adding the first child node while user transaction is still active");
                    
                    Node childNode1 = parentNode.addNode(relativeChildPath1);
                    childNode1.addMixin("mix:lockable");
                    session.save();
                    
                    LOGGER.trace("Waiting for user transaction to expire");
                    
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                        
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    
                    LOGGER.trace("Adding the second child node when user transaction was aborted");
                    
                    Node childNode2 = parentNode.addNode(relativeChildPath2);
                    childNode2.addMixin("mix:lockable");
                    session.save();
                    
                    return null;
                });
                
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).contains("Non-active user transaction");
                
            } finally {
                NodeLockHelper.unlockInNewThread(parentNode);
            }
            
        } finally {
            session.logout();
        }
        
        Session newSession = AbstractModeShapeClusterTest.createSession(repository);
        try {
            assertThat(newSession.nodeExists(absoluteChildPath1))
                    .as("The first child node should not be saved, because user transaction was aborted")
                    .isFalse();
            
            assertThat(newSession.nodeExists(absoluteChildPath2))
                    .as("The seconds child node should not be saved, because user transaction was aborted")
                    .isFalse();
        } finally {
            newSession.logout();
        }
    }
}
