package com.foo.bar;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The utility class to work with JCR nodes.
 * 
 * @author Illia Khokholkov
 *
 */
public class NodeHelper {
    
    static final String NODE_CONTENT_PROPERTY = "testContent";
    static final String RELATIVE_APP_ROOT_NODE_PATH = "appRoot";
    static final String ABSOLUTE_APP_ROOT_NODE_PATH = "/" + RELATIVE_APP_ROOT_NODE_PATH;
    static final String LEAF_PARENT_NODE_PREFIX = "folder-";
    static final String LEAF_NODE_PREFIX = "file-";
    
    static final String MIXIN_VERSIONABLE = "mix:versionable";
    
    public static String createApplicationRoot(Session session) throws RepositoryException {
        if (session.nodeExists(ABSOLUTE_APP_ROOT_NODE_PATH)) {
            return session.getNode(ABSOLUTE_APP_ROOT_NODE_PATH).getPath();
        }
        
        Node node = session.getRootNode().addNode(RELATIVE_APP_ROOT_NODE_PATH);
        node.addMixin(MIXIN_VERSIONABLE);
        node.getSession().save();

        checkoutNode(node);
        checkinNode(node);

        return node.getPath();
    }
    
    public static void deleteApplicationRoot(Session session) throws RepositoryException {
        if (!session.nodeExists(ABSOLUTE_APP_ROOT_NODE_PATH)) {
            return;
        }
        
        Node node = session.getNode(ABSOLUTE_APP_ROOT_NODE_PATH);
        node.remove();
    }
    
    public static String addNode(Session session, String parentAbsolutePath,
            String relativePath, Optional<String> content) throws RepositoryException {
        
        Node parent = session.getNode(parentAbsolutePath);
        checkoutNode(parent);
        
        Node child = parent.addNode(relativePath);
        child.addMixin(MIXIN_VERSIONABLE);
        
        if (content.isPresent()) {
            child.setProperty(NODE_CONTENT_PROPERTY, content.get());
        }
        
        child.getSession().save();
        checkoutNode(child);
        checkinNode(child);
        
        checkinNode(parent);
        
        return child.getPath();
    }

    public static String updateNode(Session session, String absolutePath, String content) throws RepositoryException {
        Node node = session.getNode(absolutePath);
        
        Node parent = node.getParent();
        checkoutNode(parent);
        
        checkoutNode(node);
        node.setProperty(NODE_CONTENT_PROPERTY, content);
        node.getSession().save();
        checkinNode(node);
        
        checkinNode(parent);
        
        return node.getPath();
    }
    
    public static Callable<String> getUpdateChildNodeCallable(Repository repository, String childAbsolutePath) {
        return new UpdateChildNodeCallable(repository, childAbsolutePath);
    }
    
    public static Callable<String> getCreateChildNodeCallable(Repository repository, String parentAbsolutePath, String childRelativePath) {
        return new CreateChildNodeCallable(repository, parentAbsolutePath, childRelativePath);
    }
    
    public static void checkoutNode(Node node) throws RepositoryException {
        node.getSession().getWorkspace().getVersionManager().checkout(node.getPath());
    }
    
    public static Version checkinNode(Node node) throws RepositoryException {
        return node.getSession().getWorkspace().getVersionManager().checkin(node.getPath());
    }
    
    public static String getLeafAbsolutePath(int index) {
        return String.format("%s/%s/%s", ABSOLUTE_APP_ROOT_NODE_PATH,
                getLeafParentRelativePath(index), getLeafRelativePath(index));
    }
    
    public static String getLeafRelativePath(int index) {
        return String.format("%s%s", LEAF_NODE_PREFIX, index);
    }
    
    public static String getLeafParentRelativePath(int index) {
        return String.format("%s%s", LEAF_PARENT_NODE_PREFIX, index);
    }
    
    private NodeHelper() {}
    
    /**
     * The {@link Callable} to update a child node.
     * 
     * @author Illia Khokholkov
     *
     */
    private static class UpdateChildNodeCallable implements Callable<String> {

        private static final Logger LOGGER = LoggerFactory.getLogger(UpdateChildNodeCallable.class);
        
        private final Repository repository;
        private final String absolutePath;
        
        private UpdateChildNodeCallable(Repository repository, String absolutePath) {
            this.repository = repository;
            this.absolutePath = absolutePath;
        }

        @Override
        public String call() {
            LOGGER.debug("Updating child node at [{}]", absolutePath);
            
            try {
                Session session = RepositoryHelper.createSession(repository);
                
                try {
                    return NodeHelper.updateNode(session, absolutePath,
                            UUID.randomUUID().toString());
                    
                } finally {
                    session.logout();
                }
                
            } catch (RepositoryException e) {
                throw new IllegalStateException(e);
            }
        }
    }
    
    /**
     * The {@link Callable} to create a child node.
     * 
     * @author Illia Khokholkov
     *
     */
    private static class CreateChildNodeCallable implements Callable<String> {

        private static final Logger LOGGER = LoggerFactory.getLogger(CreateChildNodeCallable.class);
        
        private final Repository repository;
        private final String parentAbsolutePath;
        private final String childRelativePath;
        
        private CreateChildNodeCallable(Repository repository, String parentAbsolutePath, String childRelativePath) {
            this.repository = repository;
            this.parentAbsolutePath = parentAbsolutePath;
            this.childRelativePath = childRelativePath;
        }

        @Override
        public String call() {
            LOGGER.debug("Creating child node at [{}/{}]", parentAbsolutePath, childRelativePath);
            
            try {
                Session session = RepositoryHelper.createSession(repository);
                
                try {
                    return NodeHelper.addNode(session, parentAbsolutePath, childRelativePath,
                            Optional.of(UUID.randomUUID().toString()));
                    
                } finally {
                    session.logout();
                }
                
            } catch (RepositoryException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
