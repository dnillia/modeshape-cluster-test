package com.foo.bar;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests to verify the update of the child nodes.
 * 
 * @author Illia Khokholkov
 *
 */
public class ChildNodeUpdateTest extends AbstractModeShapeClusterTest {

    @Before
    public void setUpTest() throws RepositoryException {
        List<String> parentNodes = createParentNodes(repositoryIterator.next(), LEAF_NODE_COUNT);
        List<String> childNodes = createChildNodes(repositoryIterator.next(), parentNodes);
        
        verifyChildNodes(childNodes);
    }
    
    @Test
    public void updateLeafNodesInOrder() throws RepositoryException {
        Session session = createSession(repositoryIterator.next());
        
        try {
            List<String> affectedNodes = new ArrayList<>(LEAF_NODE_COUNT);
            
            for (int i = 0; i < LEAF_NODE_COUNT; i++) {
                affectedNodes.add(NodeHelper.updateNode(session, NodeHelper.getLeafAbsolutePath(i),
                        UUID.randomUUID().toString()));
            }
            
            verifyChildNodes(affectedNodes);
            
        } finally {
            session.logout();
        }
    }
    
    @Test
    public void updateLeafNodesInParallel() throws InterruptedException, ExecutionException, RepositoryException {
        List<String> affectedNodes = new ArrayList<>(LEAF_NODE_COUNT);
        ExecutorService executorService = ConcurrencyHelper.createExecutorService(THREAD_COUNT, "update-child-parallel-");
        
        try {
            List<Callable<String>> tasks = new ArrayList<>(LEAF_NODE_COUNT);
            
            for (int i = 0; i < LEAF_NODE_COUNT; i++) {
                tasks.add(NodeHelper.getUpdateChildNodeCallable(
                        repositoryIterator.next(),
                        NodeHelper.getLeafAbsolutePath(i)));
            }
            
            for (Future<String> updatedNode : executorService.invokeAll(tasks)) {
                affectedNodes.add(updatedNode.get());
            }
            
            assertThat(affectedNodes).hasSize(LEAF_NODE_COUNT);
            
            verifyChildNodes(affectedNodes);
            
        } finally {
            ConcurrencyHelper.closeExecutorService(executorService, TimeUnit.SECONDS.toMillis(30));
        }
    }
}
