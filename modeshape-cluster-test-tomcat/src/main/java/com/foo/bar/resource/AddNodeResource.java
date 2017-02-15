package com.foo.bar.resource;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.foo.bar.NodeLockHelper;
import com.foo.bar.TransactionExecutor;
import com.foo.bar.factory.SessionFactory;

@Path("/")
public class AddNodeResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddNodeResource.class);
    
    private final Provider<Session> sessionProvider;

    @Inject
    public AddNodeResource(Provider<Session> sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    @GET
    public Response showRepository() {
        Session session = sessionProvider.get();
        
        try {
            Node parentNode = session.getNode(SessionFactory.ABSOLUTE_PARENT_NODE_PATH);
            NodeIterator nodeIterator = parentNode.getNodes();
            
            StringBuilder builder = new StringBuilder();
            while (nodeIterator.hasNext()) {
                builder.append(nodeIterator.nextNode().getPath()).append("\n");
            }
            
            return Response.ok(builder.toString()).build();
            
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
    
    @PUT
    @Path("add/{nodeName}")
    public Response addNode(@PathParam("nodeName") String nodeName) {
        try {
            Session session = sessionProvider.get();
            Node parentNode = session.getNode(SessionFactory.ABSOLUTE_PARENT_NODE_PATH);
            NodeLockHelper.lockNode(parentNode);
            
            try {
                return TransactionExecutor.runInTransaction(() -> {
                    
                    LOGGER.trace("Sleeping for the transaction to expire");
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(10));
                        
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    
                    Node childNode = parentNode.addNode(nodeName);
                    childNode.addMixin("mix:lockable");
                    session.save();
                    
                    return Response.ok(childNode.getPath()).build();
                    
                });
                
            } finally {
                NodeLockHelper.unlockNode(parentNode);
            }
            
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
