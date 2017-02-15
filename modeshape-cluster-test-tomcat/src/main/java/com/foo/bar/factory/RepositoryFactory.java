package com.foo.bar.factory;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.glassfish.hk2.api.Factory;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;

import com.foo.bar.ArjunaTransactionManagerLookup;

public class RepositoryFactory implements Factory<Repository> {

    private static final String DB_URL = System.getProperty("db.url", "jdbc:h2:file:./target/content/db;DB_CLOSE_DELAY=-1");
    private static final String DB_USERNAME = System.getProperty("db.username", "sa");
    private static final String DB_PASSWORD = System.getProperty("db.password", "");
    private static final String CLUSTER_NAME = "test-cluster";
    private static final String JGROUPS_CONFIGURATION_FILE = System.getProperty("jgroups.location", "test-jgroups.xml");
    private static final String REPOSITORY_LOCATION = "/test-repository-h2.json";
    
    private final ModeShapeEngine engine;
    
    public RepositoryFactory() {
        this.engine = new ModeShapeEngine();
        this.engine.start();
    }
    
    @Override
    public Repository provide() {
        System.setProperty("db.url", DB_URL);
        System.setProperty("db.username", DB_USERNAME);
        System.setProperty("db.password", DB_PASSWORD);
        
        System.setProperty("repository.uuid", UUID.randomUUID().toString());
        System.setProperty("cluster.name", CLUSTER_NAME);
        System.setProperty("jgroups.location", JGROUPS_CONFIGURATION_FILE);
        System.setProperty("transaction.manager.lookup", ArjunaTransactionManagerLookup.class.getCanonicalName());
        
        try (InputStream stream = RepositoryFactory.class.getResourceAsStream(REPOSITORY_LOCATION)) {
            return engine.deploy(RepositoryConfiguration.read(stream, REPOSITORY_LOCATION));
            
        } catch (IOException | RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void dispose(Repository instance) {
        engine.shutdown();
    }
}
