package com.foo.bar;

import java.net.URL;
import java.util.UUID;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.modeshape.jcr.ConfigurationException;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.schematic.document.ParsingException;

/**
 * The utility class to work with JCR repository.
 * 
 * @author Illia Khokholkov
 *
 */
public class RepositoryHelper {

    private static final String CLUSTER_NAME = "main-cluster";
    private static final String JGROUPS_LOCATION = "main-jgroups.xml";
    private static final String REPOSITORY_CONFIGURATION_FILE = "/main-repository.json";
    
    public static Repository createRepository(ModeShapeEngine engine, String dbUrl)
            throws ConfigurationException, ParsingException, RepositoryException {
        
        System.setProperty("db.url", dbUrl);
        System.setProperty("repository.uuid", UUID.randomUUID().toString());
        System.setProperty("cluster.name", CLUSTER_NAME);
        System.setProperty("jgroups.location", JGROUPS_LOCATION);
        System.setProperty("transaction.manager.lookup", ArjunaTransactionManagerLookup.class.getCanonicalName());
        
        URL configurationFile = RepositoryHelper.class.getResource(REPOSITORY_CONFIGURATION_FILE);
        
        return engine.deploy(RepositoryConfiguration.read(configurationFile)); 
    }
    
    public static Session createSession(Repository repository) throws RepositoryException {
        return repository.login("default");
    }
    
    private RepositoryHelper() {}
}
