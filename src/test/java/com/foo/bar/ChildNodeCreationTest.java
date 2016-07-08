package com.foo.bar;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.jcr.RepositoryException;
import org.junit.Test;

/**
 * Tests to verify the creation of the child nodes.
 * 
 * @author Illia Khokholkov
 *
 */
public class ChildNodeCreationTest extends AbstractModeShapeClusterTest {

    @Test
    public void createLeafNodesInOrder() throws RepositoryException {
        List<String> parentNodes = createParentNodes(repository1, LEAF_NODE_COUNT);
        List<String> childNodes = createChildNodes(repository1, parentNodes);
        
        verifyChildNodes(repository1, childNodes);
        verifyChildNodes(repository2, childNodes);
    }
    
    @Test
    public void createLeafNodesInParallel() throws InterruptedException, ExecutionException, RepositoryException {
        List<String> parentNodes = createParentNodes(repository1, LEAF_NODE_COUNT);
        List<String> affectedNodes = new ArrayList<>(parentNodes.size());
        ExecutorService executorService = ConcurrencyHelper.createExecutorService(THREAD_COUNT, "create-child-parallel-");
        
        try {
            List<Callable<String>> tasks = new ArrayList<>(parentNodes.size());
            
            for (int i = 0; i < parentNodes.size(); i++) {
                tasks.add(NodeHelper.getCreateChildNodeCallable(
                        repository1,
                        parentNodes.get(i),
                        NodeHelper.getLeafRelativePath(i)));
            }
            
            for (Future<String> createdNode : executorService.invokeAll(tasks)) {
                affectedNodes.add(createdNode.get());
            }
            
            assertThat(affectedNodes).hasSize(parentNodes.size());
            
            verifyChildNodes(repository1, affectedNodes);
            verifyChildNodes(repository2, affectedNodes);
            
        } finally {
            ConcurrencyHelper.closeExecutorService(executorService, TimeUnit.SECONDS.toMillis(30));
        }
    }
}
