# Instructions for creating the database

Run with the following command:
   mvn -Dliquibase.url=jdbc:postgresql://localhost:5432/listingviewstore -Dliquibase.username=listing -Dliquibase.password=listing -Dliquibase.logLevel=info resources:resources liquibase:update
   
OR go to target directory and run:
   java -jar listing-viewstore-liquibase-0.0.1-SNAPSHOT.jar --url=jdbc:postgresql://localhost:5432/listingviewstore --username=listing --password=listing --logLevel=info --defaultSchemaName=public  update

