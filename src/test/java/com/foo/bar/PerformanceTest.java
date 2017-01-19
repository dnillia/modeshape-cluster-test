package com.foo.bar;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Test;

/**
 * Basic tests to figure out the limitations of the ModeShape 5.x, e.g.:
 * <ul>
 *   <li>How many direct child nodes can be added to a given node?</li>
 *   <li>Is there a node removal limitation, i.e. can we remove a node with lots of direct children?</li> 
 * </ul> 
 *
 * Available system properties:
 * <ul>
 *   <li>{@code performance.create.child.count} - the number of child nodes to create, defaults to {@code 10}</li>
 *   <li>{@code performance.delete.child.count} - the number of child nodes to create for a given node and then delete that node, defaults to {@code 5}</li>
 * </ul>
 *
 * @author Illia Khokholkov
 *
 */
public class PerformanceTest extends AbstractModeShapeClusterTest {

    private static final int CREATE_CHILD_COUNT = Integer.valueOf(System.getProperty("performance.create.child.count", "10"));
    private static final int DELETE_CHILD_COUNT = Integer.valueOf(System.getProperty("performance.delete.child.count", "5"));

    /**
     * Creates a lot of direct child nodes added to the application root.
     * 
     * @throws RepositoryException
     *             if an error occurred
     */
    @Test
    public void createDirectChildren() throws RepositoryException {
        createParentNodes(repositoryIterator.next(), CREATE_CHILD_COUNT);
    }
    
    /**
     * Removes a node with a lot of direct child nodes, i.e. application root.
     * 
     * @throws RepositoryException
     *             if an error occurred
     */
    @Test
    public void deleteDirectChildren() throws RepositoryException {
        createParentNodes(repositoryIterator.next(), DELETE_CHILD_COUNT);
        
        Session session = RepositoryHelper.createSession(repositoryIterator.next());
        try {
            NodeHelper.deleteApplicationRoot(session);
            
        } finally {
            session.logout();
        }
    }
}
