package com.foo.bar;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.Session;
import org.junit.Before;
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
    
    static {
        System.setProperty("cluster.size", "1");
        System.setProperty("transaction.timeout", "15");
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
    public void abortedTransactionSameThread() throws Exception {
        Session session = createSession(repositoryIterator.next());
        String childName = "childNode";
        
        LOGGER.trace("Starting adding child [name={}]", childName);
        
        try {
            Node parentNode = session.getNode(ABSOLUTE_PARENT_NODE_PATH);
            NodeHelper.lockNode(parentNode);
            
            try {
                assertThat(TransactionExecutor.runInTransaction(() -> {
                    
                    LOGGER.trace("Sleeping to abort the transaction");
                    
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(15));
                        
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    
                    Node childNode = parentNode.addNode(childName);
                    childNode.addMixin("mix:lockable");
                    session.save();
                    
                    LOGGER.trace("Finished adding child profile, the transaction is about to be committed");
                    
                    return childNode;
                    
                })).isNotNull();
                
            } finally {
                NodeHelper.unlockNode(parentNode);
            }
            
        } finally {
            session.logout();
        }
    }
}
