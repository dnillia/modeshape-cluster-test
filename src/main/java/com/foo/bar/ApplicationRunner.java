package com.foo.bar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.modeshape.jcr.ModeShapeEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.foo.bar.CommandLineHelper.CustomOption;

/**
 * The entry point of the application. Refer to the {@link CustomOption} for the list of supported
 * command line options.
 * 
 * @author Illia Khokholkov
 *
 */
public class ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);
    private static final String LOG_FILE = "./target/run.log";
    
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        boolean successfulCompletion = true;
        
        CommandLineHelper commandLineHelper = new CommandLineHelper(args);
        if (commandLineHelper.getCommandLine().hasOption(CustomOption.HELP.getName())) {
            CommandLineHelper.printHelp(commandLineHelper.getOptions());
            
            System.exit(0);
        }
        
        ModeShapeEngine engine = new ModeShapeEngine();
        engine.start();
        
        ActionType actionType = null;
        
        try {
            Repository repository = RepositoryHelper.createRepository(engine, commandLineHelper.getDbUrl());
            checkConnectivity(repository);
            
            try (Scanner scanner = new Scanner(System.in)) {
                
                while (true) {
                    System.out.print("\nAction to execute (create/read/update/none): ");
                    actionType = Enum.valueOf(ActionType.class, scanner.nextLine().toUpperCase(Locale.US));
                    
                    if (actionType == ActionType.NONE) {
                        break;
                    }
                    
                    if (!handleAction(repository, actionType, commandLineHelper.getNodeCount(),
                            commandLineHelper.getThreadCount())) {
                        
                        successfulCompletion = false;
                        break;
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println(String.format("\nFailed to perform [%s] action using [%s] DB connection "
                    + "(see [%s] file for details): [%s]", actionType, commandLineHelper.getDbUrl(),
                    LOG_FILE, e.getMessage()));
            
            LOGGER.error("An unexpected error occurred", e);
            successfulCompletion = false;
            
        } finally {
            engine.shutdown().get();
        }
        
        System.exit(successfulCompletion ? 0 : -1);
    }
    
    private static boolean canPerformAction(Repository repository, ActionType actionType)
            throws RepositoryException {
        
        Session session = RepositoryHelper.createSession(repository);
        
        try {
            boolean parentNodeExists = session.nodeExists(NodeHelper.ABSOLUTE_APP_ROOT_NODE_PATH);
            
            if (!parentNodeExists && (actionType == ActionType.READ || actionType == ActionType.UPDATE)) {
                System.out.println(String.format("Unable to perform [%s] action, because the "
                        + "required nodes do not exist", actionType));
                
                return false;
            }
            
            return true;
            
        } finally {
            session.logout();
        }
    }
    
    private static void checkConnectivity(Repository repository) throws RepositoryException {
        Session session = null;
        
        try {
            session = RepositoryHelper.createSession(repository);
            
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }
    
    private static boolean handleAction(
            Repository repository,
            ActionType actionType,
            int nodeCount,
            int threadCount) throws RepositoryException, InterruptedException, ExecutionException {
        
        if (!canPerformAction(repository, actionType)) {
            return false;
        }
        
        switch (actionType) {
        case CREATE:
            printAffectedNodes(
                    repository,
                    handleSequentialLeafNodeCreation(repository, nodeCount),
                    actionType);
            break;
            
        case UPDATE:
            printAffectedNodes(
                    repository,
                    handleParallelLeafNodeUpdate(repository, nodeCount, threadCount),
                    actionType);
            break;
        
        case READ:
            printAffectedNodes(
                    repository,
                    handleSequentialLeafNodeRead(repository, nodeCount),
                    actionType);
            break;
        
        default:
            throw new IllegalStateException(String.format("Unsupported action type [%s]",
                    actionType));
        }
        
        return true;
    }
    
    private static List<String> handleSequentialLeafNodeRead(Repository repository, int nodeCount)
            throws RepositoryException {

        Session session = RepositoryHelper.createSession(repository);
        
        try {
            List<String> affectedNodes = new ArrayList<>(nodeCount);
            for (int i = 0; i < nodeCount; i++) {
                affectedNodes.add(session.getNode(NodeHelper.getLeafAbsolutePath(i)).getPath());
            }
    
            return affectedNodes;
            
        } finally {
            session.logout();
        }
    }
    
    private static List<String> handleSequentialLeafNodeCreation(Repository repository, int nodeCount)
            throws RepositoryException {
        
        Session session = RepositoryHelper.createSession(repository);
        
        try {
            NodeHelper.deleteApplicationRoot(session);
            String appRootNode = NodeHelper.createApplicationRoot(session);
            
            List<String> affectedNodes = new ArrayList<>(nodeCount);
    
            for (int i = 0; i < nodeCount; i++) {
                String parentNode = NodeHelper.addNode(session, appRootNode,
                        NodeHelper.getLeafParentRelativePath(i), Optional.empty());
                
                String childNode = NodeHelper.addNode(session, parentNode,
                        NodeHelper.getLeafRelativePath(i), Optional.of(UUID.randomUUID().toString()));
    
                affectedNodes.add(childNode);
            }
    
            return affectedNodes;
            
        } finally {
            session.logout();
        }
    }
    
    private static List<String> handleParallelLeafNodeUpdate(Repository repository, int nodeCount,
            int threadCount) throws InterruptedException, ExecutionException {
        
        List<String> affectedNodes = new ArrayList<>(nodeCount);
        ExecutorService executorService = ConcurrencyHelper.createExecutorService(threadCount, "update-child-parallel-");
        
        try {
            List<Callable<String>> tasks = new ArrayList<>(nodeCount);
            
            for (int i = 0; i < nodeCount; i++) {
                tasks.add(NodeHelper.getUpdateChildNodeCallable(
                        repository,
                        NodeHelper.getLeafAbsolutePath(i)));
            }
            
            for (Future<String> updatedNode : executorService.invokeAll(tasks)) {
                affectedNodes.add(updatedNode.get());
            }
            
        } finally {
            ConcurrencyHelper.closeExecutorService(executorService, TimeUnit.SECONDS.toMillis(30));
        }
        
        return affectedNodes;
    }
    
    private static void printAffectedNodes(Repository repository, List<String> nodePathList,
            ActionType actionType) throws RepositoryException {
        
        System.out.println(String.format("\n  The [%s] node(s) have been affected as a result of "
                + "[%s] action:\n", nodePathList.size(), actionType));
        
        Session session = RepositoryHelper.createSession(repository);
        
        try {
            for (String absoluteNodePath : nodePathList) {
                Node node = session.getNode(absoluteNodePath);
                
                System.out.println(String.format("    [id=%s, path=%s, content=%s]",
                        node.getIdentifier(),
                        node.getPath(),
                        node.getProperty(NodeHelper.NODE_CONTENT_PROPERTY).getString()));
            }
            
        } finally {
            session.logout();
        }
    }
    
    /**
     * The supported actions.
     * 
     * @author Illia Khokholkov
     *
     */
    private enum ActionType {
        
        CREATE,
        READ,
        UPDATE,
        NONE
    }
}
