# ModeShape 5.x Clustering Test

## Command Line Tool

### Overview

The command line tool allows to test a communication among cluster members when each member resides
in a separate JVM (i.e. a running instance of a command line tool represents a single member of the cluster).

### Usage

```
usage: java -jar
       modeshape-cluster-test-<jar_version>-with-dependencies.jar [-dbUrl
       <arg>] [-help] [-nodeCount <arg>] [-threadCount <arg>]
 -dbUrl <arg>         The DB connection URL. Defaults to:
                      jdbc:h2:tcp://localhost/./target/h2/test
 -help                Displays help documentation
 -nodeCount <arg>     The number of child nodes the root of the
                      application should have. Defaults to: 5
 -threadCount <arg>   The number of threads to use (applies only to the
                      [UPDATE] action). Defaults to: 5
```

Supported actions (prompted when application is started):

* `create` - using a single thread of execution, creates an initial layout that would have `2 x nodeCount` nodes (`nodeCount` parent nodes and `nodeCount` child nodes, one for each parent)
* `read` - using a single thread of execution, reads all leaf nodes, i.e. all `<childNodeN>`
* `update` - using `threadCount` threads, updates `nodeCount` leaf nodes, i.e. all applicable `<childNodeN>`
* `none` - terminates the program

### How to Run

Build the project (skip tests intentionally, because otherwise they would fail the build):

```bash
mvn clean package -DskipTests
```

Start the H2 server (needs to be started only once for all instances of the application to use):

```bash
java -cp ./lib/h2-1.4.191.jar org.h2.tools.Server -baseDir ./target/h2
```

Run the tool:

```
java -jar ./target/modeshape-cluster-test-standalone-1.0-SNAPSHOT-with-dependencies.jar
```

## JUnit Tests

To run JUnit tests using Oracle DBMS, run the following command (do not forget to update property values):

```
mvn clean verify -Pperformance \
  -Ddb.url=jdbc:oracle:thin:@//test.test:1521/test \
  -Ddb.username=test \
  -Ddb.password=test \
  -Drepository.configuration.file=/test-repository-oracle.json \
  -Dojdbc6.jar.path=/Users/test/.m2/repository/com/oracle/ojdbc6/12.1.0.2/ojdbc6-12.1.0.2.jar
```