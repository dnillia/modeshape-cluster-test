# ModeShape 5.x Clustering Test

## Overview

This project helps to verify the behavior of the ModeShape 5.x in the clustered environment. The H2
database backed by the filesystem is used as a storage medium for the cluster members. The layout
of the nodes used for testing (gets created automatically):

```
<jcrRepositoryRoot>
- <applicationRoot>
-- <parentNode1>
--- <childNode1>
-- <parentNode2>
--- <childNode2>

...

-- <parentNodeN>
--- <childNodeN>
```

Note, that a given parent node `<parentNodeN>` (consider it as a "folder") has exactly one child
node `<childNodeN>` (consider it as a "file"). In addition, all created nodes are versionable.

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
java -jar ./target/modeshape-cluster-test-1.0-SNAPSHOT-with-dependencies.jar
```

### Logging

The Logback configuration file is located under `src/main/resources/logback.xml`. To change the log
level or the log file, update the file accordingly.


## JUnit Tests

### Overview

The following system properties are available for the test run:
* `child.node.count` - the number of child nodes to create, defaults to `5` (each child node will be under its own parent)
* `thread.count` - the number of threads to use when performing parallel node creation/update, defaults to `5`

### How to Run

To run the JUnit tests, execute the following command:

```bash
mvn clean verify
```

To supply a supported system property for the test run, use `-D<propertyName>`, e.g.:

```
mvn clean verify -Dchild.node.count=5 -Dthread.count=5
```

### Logging

The Logback configuration file is located under `src/test/resources/logback-test.xml`. To change the log
level, update the file as necessary.

## Known Problems

With at least two JGroups cluster members, the `NodeNotFoundInParentException` gets thrown when multiple
threads perform write (create or update) operation on the child nodes (one thread per child node and that
child node is under a unique parent node) targeting either member of the cluster. The JCR locking is
not necessary (and it would not help anyway), because no two threads update the same subtree at the same time.

```
Caused by: org.modeshape.jcr.cache.NodeNotFoundInParentException: Cannot locate child node: 4a78950317f1e70dfda2f7-53d6-4272-8154-6f4482eb745b within parent: 4a78950317f1e74befdecd-f8de-476d-a8af-9ffcc89c4ce3
    at org.modeshape.jcr.cache.document.SessionNode.getSegment(SessionNode.java:435)
    at org.modeshape.jcr.cache.document.SessionNode.getPath(SessionNode.java:466)
    at org.modeshape.jcr.JcrSession.node(JcrSession.java:553)
    at org.modeshape.jcr.JcrSession.node(JcrSession.java:518)
    at org.modeshape.jcr.JcrVersionManager.checkin(JcrVersionManager.java:378)
    at org.modeshape.jcr.JcrVersionManager.checkin(JcrVersionManager.java:295)
    at com.foo.bar.NodeHelper.checkinNode(NodeHelper.java:107)
    at com.foo.bar.NodeHelper.updateNode(NodeHelper.java:87)
    at com.foo.bar.NodeHelper$UpdateChildNodeCallable.call(NodeHelper.java:151)
    at com.foo.bar.NodeHelper$UpdateChildNodeCallable.call(NodeHelper.java:1)

```

### How To Reproduce

Use one of the following options:

* Run JUnit tests without supplying any custom system properties. The tests will set up a ModeShape
  engine with two repository instances deployed on it, which represent cluster members. In this scenario,
  cluster members function in the same JVM. For example:
  
  ```
  mvn clean verify
  ```
  
  Will result in:
  
  ```
  Tests in error:
  ChildNodeCreationTest.createLeafNodesInParallel:51 » Execution org.modeshape.j...
  ChildNodeUpdateTest.updateLeafNodesInParallel:73 » Execution org.modeshape.jcr...

  Tests run: 4, Failures: 0, Errors: 2, Skipped: 0
  ```
  
* Stand up two instances of the command line tool, create nodes from the first one, make sure they
  are visible from the second one, and after that within the first instance attempt to update the nodes.
  In this scenario, cluster members are running on different JVMs, but they are still communicating
  with each other. For example (assuming you have three terminal windows opened):
  
  In window 1, start H2 server:
  
  ```
  java -cp ./lib/h2-1.4.191.jar org.h2.tools.Server -baseDir ./target/h2
  ```
  
  In windows 2 and 3, start the command line tool to bring both cluster members online:
  
  ```
  java -jar ./target/modeshape-cluster-test-1.0-SNAPSHOT-with-dependencies.jar
  ```
  
  In window 2 (the output will look similar to the one provided below):
  
  ```
  Action to execute (create/read/update/none): create

  The [5] node(s) have been affected as a result of [CREATE] action:

    [id=809ef663-8fdc-40b1-b11a-233ca5d562ab, path=/appRoot/folder-0/file-0, content=1b1c5895-c8e0-47b8-80e9-84c09ef0b4df]
    [id=3ecf07cc-e56d-48b5-ab91-edc002028c98, path=/appRoot/folder-1/file-1, content=00326ecb-bb5a-4469-920a-e3a119daa580]
    [id=b0eab2f1-07a3-458a-a16d-8cf184c587f8, path=/appRoot/folder-2/file-2, content=92b1e8a4-a73c-4a9c-b8b5-96b8c5126d87]
    [id=fef7d241-cb13-4fd5-88aa-7e2ce3ef0cc6, path=/appRoot/folder-3/file-3, content=6afa523c-859a-447f-933a-35628c2b6848]
    [id=307f83a9-6f5d-4659-9b0a-bc3bb15326ca, path=/appRoot/folder-4/file-4, content=3fd7a246-9160-4e0d-9f0c-050b1eea25f9]
  ```
  
  In window 3 (ensure that nodes created in another JVM are visible):
  
  ```
  Action to execute (create/read/update/none): read

  The [5] node(s) have been affected as a result of [READ] action:

    [id=809ef663-8fdc-40b1-b11a-233ca5d562ab, path=/appRoot/folder-0/file-0, content=1b1c5895-c8e0-47b8-80e9-84c09ef0b4df]
    [id=3ecf07cc-e56d-48b5-ab91-edc002028c98, path=/appRoot/folder-1/file-1, content=00326ecb-bb5a-4469-920a-e3a119daa580]
    [id=b0eab2f1-07a3-458a-a16d-8cf184c587f8, path=/appRoot/folder-2/file-2, content=92b1e8a4-a73c-4a9c-b8b5-96b8c5126d87]
    [id=fef7d241-cb13-4fd5-88aa-7e2ce3ef0cc6, path=/appRoot/folder-3/file-3, content=6afa523c-859a-447f-933a-35628c2b6848]
    [id=307f83a9-6f5d-4659-9b0a-bc3bb15326ca, path=/appRoot/folder-4/file-4, content=3fd7a246-9160-4e0d-9f0c-050b1eea25f9]
  ```
  
  In window 2, request the update and observe an error:
  
  ```
  Action to execute (create/read/update/none): update

  Failed to perform [UPDATE] action using [jdbc:h2:tcp://localhost/./target/h2/test] DB connection (see [./target/run.log]
  file for details): [org.modeshape.jcr.cache.NodeNotFoundInParentException: Cannot locate child node:
  497b1b6317f1e70d3d5c5b-53b9-469e-bab4-3cea2a25a41b within parent: 497b1b6317f1e7809ef663-8fdc-40b1-b11a-233ca5d562ab]
  ```
  
### Possible Cause
  
The problem demonstrated in this project may be related to the potentially incorrect JGroups configuration file, even though
it is possible to see send/receive messages from JGroups cluster members.
  
