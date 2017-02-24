package com.foo.bar;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The tests to validate the behavior of ModeShape when transactions are utilized.
 * 
 * @author Illia Khokholkov
 *
 */
public class TransactionTest extends AbstractModeShapeClusterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionTest.class);
    
    private static final String ABSOLUTE_PARENT_NODE_PATH = "/tnxParentNode";
    private static final String RELATIVE_PARENT_NODE_PATH = "tnxParentNode";
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        System.setProperty("cluster.size", "1");
        AbstractModeShapeClusterTest.setUpClass();
    }
    
    @BeforeClass
    public static void resetClusterSize() throws Exception {
        System.clearProperty("cluster.size");
        AbstractModeShapeClusterTest.tearDownClass();
    }
    
    @Before
    public void setUpParent() throws Exception {
        Session session = createSession(repositoryIterator.next());
        
        try {
            if (session.nodeExists(ABSOLUTE_PARENT_NODE_PATH)) {
                Node parentNode = session.getNode(ABSOLUTE_PARENT_NODE_PATH);
                parentNode.remove();
                
                session.save();
            }
            
            Node parentNode = session.getRootNode().addNode(RELATIVE_PARENT_NODE_PATH);
            parentNode.addMixin("mix:lockable");
                
            session.save();
            
        } finally {
            session.logout();
        }
    }

    @Test
    public void addOneNode() throws Exception {
        Session session = createSession(repositoryIterator.next());
        MutableObject<String> childPath1 = new MutableObject<>();
        
        try {
            Node parentNode = session.getNode(ABSOLUTE_PARENT_NODE_PATH);
            NodeHelper.lockNode(parentNode);
            
            // Attempt to add one child nodes inside a user transaction that is aborted before
            // a call to save a session is made.
            
            try {
                TransactionExecutor.runInTransaction(() -> {

                    // Wait for "com.arjuna.ats.arjuna.coordinator.TransactionReaper" to abort the
                    // transaction. The timeout is set to "3" seconds, but we will wait here for
                    // "5" seconds to ensure that enough time has passed.
                    
                    LOGGER.trace("Waiting for user transaction to expire");
                    
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                        
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    
                    // Add the child node and invoke save on the session after transaction is aborted.
                    // Since initial transaction is no longer active, ideally, no changes should
                    // be persisted at this point.
                    
                    LOGGER.trace("Attempting to add a new child node when initial user transaction "
                            + "has expired");
                    
                    Node childNode1 = parentNode.addNode(UUID.randomUUID().toString());
                    childNode1.addMixin("mix:lockable");
                    session.save();
                    
                    childPath1.setValue(childNode1.getPath());
                    
                    return null;
                });
                
            } finally {
                NodeHelper.unlockNode(parentNode);
            }
            
        } finally {
            session.logout();
        }
        
        assertThat(childPath1.getValue()).isNotNull();
        Session newSession = createSession(repositoryIterator.next());
        
        try {
            assertThat(newSession.nodeExists(childPath1.getValue()))
                    .as("The child node should not be saved, because user transaction was aborted")
                    .isFalse();
        } finally {
            newSession.logout();
        }
    }
    
    @Test
    public void addTwoNodes() throws Exception {
        Session session = createSession(repositoryIterator.next());
        
        MutableObject<String> childPath1 = new MutableObject<>();
        MutableObject<String> childPath2 = new MutableObject<>();
        
        try {
            Node parentNode = session.getNode(ABSOLUTE_PARENT_NODE_PATH);
            NodeHelper.lockNode(parentNode);
            
            // Attempt to add two child nodes inside a user transaction that is aborted in the
            // middle of the run. Ideally, no nodes should be persisted.
            
            try {
                TransactionExecutor.runInTransaction(() -> {
                    
                    // Add the first child node and invoke save on the session before transaction
                    // is aborted. Since initial transaction is active, no changes should be
                    // persisted at this point.
                    
                    LOGGER.trace("Adding the first child node while user transaction is still active");
                    
                    Node childNode1 = parentNode.addNode(UUID.randomUUID().toString());
                    childNode1.addMixin("mix:lockable");
                    session.save();
                    
                    // Wait for "com.arjuna.ats.arjuna.coordinator.TransactionReaper" to abort the
                    // transaction. The timeout is set to "3" seconds, but we will wait here for
                    // "5" seconds to ensure that enough time has passed.
                    
                    LOGGER.trace("Waiting for user transaction to expire");
                    
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                        
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    
                    // At this point the initial transaction should be aborted and disassociated
                    // with the thread. Add a new child node.
                    
                    LOGGER.trace("Adding the second child node when user transaction was aborted");
                    
                    Node childNode2 = parentNode.addNode(UUID.randomUUID().toString());
                    childNode2.addMixin("mix:lockable");
                    
                    // This call results in a significant delay which eventually leads to an exception
                    // stating that the lock could not be acquired after specified timeout.
                    
                    session.save();
                    
                    childPath1.setValue(childNode1.getPath());
                    childPath2.setValue(childNode2.getPath());
                    
                    return null;
                });
                
            } finally {
                NodeHelper.unlockNode(parentNode);
            }
            
        } finally {
            session.logout();
        }
        
        // This part of the test is never reached, due to timeout exception on the attempt to save
        // the second added node.
        
        assertThat(childPath1.getValue()).isNotNull();
        assertThat(childPath2.getValue()).isNotNull();
        Session newSession = createSession(repositoryIterator.next());
        
        try {
            assertThat(newSession.nodeExists(childPath1.getValue()))
                    .as("The first child node should not be saved, because user transaction was aborted")
                    .isFalse();
            
            assertThat(newSession.nodeExists(childPath2.getValue()))
                    .as("The seconds child node should not be saved, because user transaction was aborted")
                    .isFalse();
        } finally {
            newSession.logout();
        }
    }
}
