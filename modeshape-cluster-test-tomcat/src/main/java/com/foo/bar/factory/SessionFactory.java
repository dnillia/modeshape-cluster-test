package com.foo.bar.factory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.glassfish.hk2.api.Factory;

public class SessionFactory implements Factory<Session> {

    public static final String ABSOLUTE_PARENT_NODE_PATH = "/parentNode";
    public static final String RELATIVE_PARENT_NODE_PATH = "parentNode";
    
    private final Provider<Repository> repositoryProvider;

    @Inject
    public SessionFactory(Provider<Repository> repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
    }

    @Override
    public Session provide() {
        try {
            Session session = repositoryProvider.get().login("default");
            
            if (!session.nodeExists(ABSOLUTE_PARENT_NODE_PATH)) {
                Node parentNode = session.getRootNode().addNode(RELATIVE_PARENT_NODE_PATH);
                parentNode.addMixin("mix:lockable");
                session.save();
            }
            
            return session;
            
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void dispose(Session instance) {
        instance.logout();
    }
}
