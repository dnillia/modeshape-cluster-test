package com.foo.bar;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.modeshape.jcr.ConfigurationException;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.schematic.document.ParsingException;

/**
 * The tests to verify ModeShape clustering capabilities when multiple {@link Repository repositories}
 * are running in a single JVM. By default, H2 database backed by the filesystem will be used.
 * If another DBMS is necessary, utilize the following system properties:
 * 
 * <ul>
 *   <li>{@code repository.configuration.file} - the repository configuration file, defaults to {@code /test-repository-h2.json}</li>
 *   <li>{@code db.url} - the DB connection URL, defaults to {@code jdbc:h2:file:./target/content/db;DB_CLOSE_DELAY=-1}</li>
 *   <li>{@code db.username} - the DB username, defaults to {@code sa}</li>
 *   <li>{@code db.password} - the DB password, defaults to an empty string</li>
 * </ul>
 * 
 * @author Illia Khokholkov
 *
 */
public abstract class AbstractModeShapeClusterTest {

    static final int LEAF_NODE_COUNT = Integer.valueOf(System.getProperty("leaf.node.count", "5"));
    static final int THREAD_COUNT = Integer.valueOf(System.getProperty("thread.count", "5"));
    
    static final String CLUSTER_NAME = "test-cluster";
    static final String JGROUPS_LOCATION = "test-jgroups.xml";
    static final String REPOSITORY_CONFIGURATION_FILE = System.getProperty("repository.configuration.file", "/test-repository-h2.json");
    
    static final String DB_URL = System.getProperty("db.url", "jdbc:h2:file:./target/content/db;DB_CLOSE_DELAY=-1");
    static final String DB_USERNAME = System.getProperty("db.username", "sa");
    static final String DB_PASSWORD = System.getProperty("db.password", "");
    static final String ORACLE_DB_DRIVER_JAR_PROPERTY = "ojdbc6.jar.path";
    
    static ModeShapeEngine engine;
    
    static Repository repository1;
    static Repository repository2;
    
    @BeforeClass
    public static void setUpClass() throws ConfigurationException, ParsingException, RepositoryException, ClassNotFoundException {
        
        if (System.getProperty(ORACLE_DB_DRIVER_JAR_PROPERTY) != null) {
            Class.forName("oracle.jdbc.OracleDriver");
        }
        
        engine = new ModeShapeEngine();
        engine.start();
        
        repository1 = createRepository(engine);
        repository2 = createRepository(engine);
    }
    
    @AfterClass
    public static void tearDownClass() throws InterruptedException, ExecutionException {
        if (engine != null) {
            engine.shutdown().get();
        }
    }
    
    @Before
    public void setUp() throws RepositoryException {
        checkConnectivity(repository1);
        checkConnectivity(repository2);
    }
    
    static List<String> createParentNodes(Repository repository, int nodeCount) throws RepositoryException {
        Session session = createSession(repository);
        
        try {
            NodeHelper.deleteApplicationRoot(session);
            String appRootNode = NodeHelper.createApplicationRoot(session);
            List<String> affectedNodes = new ArrayList<>(nodeCount);
            
            for (int i = 0; i < nodeCount; i++) {
                String parentNode = NodeHelper.addNode(session, appRootNode,
                        NodeHelper.getLeafParentRelativePath(i), Optional.empty());
                
                affectedNodes.add(parentNode);
            }
            
            assertThat(affectedNodes).hasSize(nodeCount);
            
            return affectedNodes;
            
        } finally {
            session.logout();
        }
    }
    
    static List<String> createChildNodes(Repository repository, List<String> parentNodes)
            throws RepositoryException {
        
        Session session = createSession(repository);
        
        try {
            List<String> affectedNodes = new ArrayList<>(parentNodes.size());
            
            for (int i = 0; i < parentNodes.size(); i++) {
                String childNode = NodeHelper.addNode(
                        session,
                        parentNodes.get(i),
                        NodeHelper.getLeafRelativePath(i),
                        Optional.of(UUID.randomUUID().toString()));
                
                affectedNodes.add(childNode);
            }
            
            assertThat(affectedNodes).hasSize(parentNodes.size());
            
            return affectedNodes;
            
        } finally {
            session.logout();
        }
    }
    
    static void verifyChildNodes(Repository repository, List<String> expectedNodes) throws RepositoryException {
        Session session = createSession(repository);
        
        try {
            List<String> affectedNodes = new ArrayList<>(expectedNodes.size());
            for (int i = 0; i < expectedNodes.size(); i++) {
                affectedNodes.add(session.getNode(NodeHelper.getLeafAbsolutePath(i)).getPath());
            }
    
            List<String> expectedLeafContent = getContentProperty(session, expectedNodes);
            List<String> actualLeafContent = getContentProperty(session, affectedNodes);
            
            assertThat(actualLeafContent).isEqualTo(expectedLeafContent);
            
        } finally {
            session.logout();
        }
    }
    
    static List<String> getContentProperty(Session session, List<String> nodes) throws RepositoryException {
        List<String> output = new ArrayList<>(nodes.size());
        
        for (String node : nodes) {
            output.add(session.getNode(node)
                    .getProperty(NodeHelper.NODE_CONTENT_PROPERTY).getString());
        }
        
        return output;
    }
    
    static void checkConnectivity(Repository repository) throws RepositoryException {
        Session session = null;
        
        try {
            session = createSession(repository);
            
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }
    
    static Repository createRepository(ModeShapeEngine engine)
            throws ConfigurationException, ParsingException, RepositoryException {
        
        System.setProperty("db.url", DB_URL);
        System.setProperty("db.username", DB_USERNAME);
        System.setProperty("db.password", DB_PASSWORD);
        
        System.setProperty("repository.uuid", UUID.randomUUID().toString());
        System.setProperty("cluster.name", CLUSTER_NAME);
        System.setProperty("jgroups.location", JGROUPS_LOCATION);
        
        URL configurationFile = AbstractModeShapeClusterTest.class.getResource(REPOSITORY_CONFIGURATION_FILE);
        
        return engine.deploy(RepositoryConfiguration.read(configurationFile)); 
    }
    
    static Session createSession(Repository repository) throws RepositoryException {
        return repository.login("default");
    }
}
