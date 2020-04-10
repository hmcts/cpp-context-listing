# Instructions for running the jar

1. create datasource file from sample (standalone-ds) provided. Make sure to change with correct host, database user and password. 

2. Run with the following command
        1. touch processFile
        2. java -jar -Dorg.wildfly.swarm.mainProcessFile=/path/to/processFile -Devent.transformation.jar=/path/to/listing-domain-transformations-for-corechanges-x.x.x-SNAPSHOT.jar /path/to/event-tool-1.2.0-SNAPSHOT-swarm.jar -c /path/to/standalone-ds.xml

3. Run with debug option
     java -jar -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -Dorg.wildfly.swarm.mainProcessFile=/path/to/processFile -Devent.transformation.jar=/path/to/listing-domain-transformations-for-corechanges-x.x.x-SNAPSHOT.jar /path/to/event-tool-1.2.0-SNAPSHOT-swarm.jar -c /path/to/standalone-ds.xml


Note: For event-tool-1.2.0-SNAPSHOT-swarm.jar artefact you need to build *stream-transformation-tool* project and the artefact would be available from event-tool module.



