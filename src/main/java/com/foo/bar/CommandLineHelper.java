package com.foo.bar;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * The utility class to process command line arguments.
 * 
 * @author Illia Khokholkov
 *
 */
public class CommandLineHelper {

    private static final String DEFAULT_DB_URL = "jdbc:h2:tcp://localhost/./target/h2/test";
    private static final int DEFAULT_ROOT_CHILD_COUNT = 5;
    private static final int DEFAULT_THREAD_COUNT = 5;
    
    private final Options options;
    private final CommandLine commandLine;
    
    private final String dbUrl;
    private final int nodeCount;
    private final int threadCount;
    
    public CommandLineHelper(String[] args) {
        this.options = createOptions();
        this.commandLine = createCommandLine(args, options);
        
        this.dbUrl = parseDbUrl(commandLine);
        this.nodeCount = parseNodeCount(commandLine);
        this.threadCount = parseThreadCount(commandLine);
    }
    
    public static void printHelp(Options options) {
        new HelpFormatter().printHelp("java -jar modeshape-cluster-test-<jar_version>-with-dependencies.jar",
                options, true);
    }
    
    public String getDbUrl() {
        return dbUrl;
    }
    
    public int getNodeCount() {
        return nodeCount;
    }
    
    public int getThreadCount() {
        return threadCount;
    }
    
    public CommandLine getCommandLine() {
        return commandLine;
    }
    
    public Options getOptions() {
        return options;
    }
    
    private static Options createOptions() {
        Options options = new Options();
        
        options.addOption(CustomOption.DB_URL.getName(), true, CustomOption.DB_URL.getDescription());
        options.addOption(CustomOption.NODE_COUNT.getName(), true, CustomOption.NODE_COUNT.getDescription());
        options.addOption(CustomOption.THREAD_COUNT.getName(), true, CustomOption.THREAD_COUNT.getDescription());
        options.addOption(CustomOption.HELP.getName(), false, CustomOption.HELP.getDescription());
        
        return options;
    }
    
    private static String parseDbUrl(CommandLine commandLine) {
        return commandLine.hasOption(CustomOption.DB_URL.getName())
                ? commandLine.getOptionValue(CustomOption.DB_URL.getName())
                : DEFAULT_DB_URL;
    }
    
    private static int parseNodeCount(CommandLine commandLine) {
        return commandLine.hasOption(CustomOption.NODE_COUNT.getName())
                ? Integer.valueOf(commandLine.getOptionValue(CustomOption.NODE_COUNT.getName()))
                : DEFAULT_ROOT_CHILD_COUNT;
    }
    
    private static int parseThreadCount(CommandLine commandLine) {
        return commandLine.hasOption(CustomOption.THREAD_COUNT.getName())
                ? Integer.valueOf(commandLine.getOptionValue(CustomOption.THREAD_COUNT.getName()))
                : DEFAULT_THREAD_COUNT;
    }
    
    private static CommandLine createCommandLine(String[] args, Options options) {
        CommandLineParser parser = new DefaultParser();
        
        try {
            return parser.parse(options, args.clone());
            
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }
    
    /**
     * The supported command line options.
     * 
     * @author Illia Khokholkov
     *
     */
    public enum CustomOption {
        
        DB_URL("dbUrl", "The DB connection URL. Defaults to: " + DEFAULT_DB_URL),
        NODE_COUNT("nodeCount", "The number of child nodes the root of the application should have. Defaults to: " + DEFAULT_ROOT_CHILD_COUNT),
        THREAD_COUNT("threadCount", "The number of threads to use (applies only to the [UPDATE] action). Defaults to: " + DEFAULT_THREAD_COUNT),
        HELP("help", "Displays help documentation");
        
        private final String name;
        private final String description;

        private CustomOption(String name, String description) {
            this.name = name;
            this.description = description;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
