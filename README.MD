# Automatic generation of comparison notebooks for interactive data exploration

## Instruction to build
Building the project requires maven 3 and java version 17 or higher
```shell
git pull https://github.com/patrickmarcel/sqlEDAqueryGenerator
cd sqlEDAqueryGenerator
mvn clean compile assembly:single
cp ./target/sqlEDAqueryGenerator-1.0-SNAPSHOT-jar-with-dependencies.jar ./tap-eda.jar
```

## Loading the example data
Right now we only support postgresql but we are working on adding support for monetdb.
We recommend using a dedicated user and database to avoid messing with your own databases 
Login as the postgres user on your dbms and run the following (we obviously recommend changing the password)
````sql
postgres=# create database tap;
postgres=# create user tapuser with encrypted password '1234';
postgres=# grant all privileges on database tap to tapuser;
````
### Electricity consumption data
You can load the small electricity dataset by first running `./sql/conso_iris.sql` as tap user and then loading the `./sql/conso_iris.csv` file into the table
this can be done in a shell or using a GUI tool such as PGAdmin or DataGrip.
```shell
psql -U tapuser -d tap < ./sql/conso_iris.sql
psql -U tapuser -d tap #Log into psql
\copy conso_iris FROM 'absolute/path/sql/conso_iris.csv' DELIMITER ',' CSV
```

### Flights data
First you need to download a copy of the data from the internet it's available on [kaggle](https://www.kaggle.com/usdot/flight-delays)
You can create the table as show previously with `./sql/flights.sql`
```shell
psql -U tapuser -d tap < ./sql/flights.sql
```
The csv file from kaggle has a header and must be ignored
```shell
psql -U tapuser -d tap #Log into psql
\copy flights  from '/path/to/your/download/flights.csv' delimiter ',' CSV HEADER ;
```

## Running the experiments
Assuming you want to run the code only on the provided data you will only need to copy two config files and input you username and password for the postgres user you created.
```shell
cp ./src/main/resources/flights.properties ./flights.properties
cp ./src/main/resources/enedis.properties ./enedis.properties
```

Edit the two files to match your own database namely those three lines, the rest of the file namely table and column names should be good assuming you followed previous instructions
```properties
datasource.url=jdbc:postgresql://localhost:5432/tap
datasource.user=tapuser
datasource.password=1234
```

You can now run the various shell scripts used to produce the results:
```shell
./scale.sh # Runs multi-threading scaling test
./sampling.sh # Runs Sampling tests
```

### Notes
#### User study
The notebooks presented to the users where generated without of the `Nom Commune` to allow small enough instance for the exact solver

#### Using CPLEX
To produce exact solution (only for small datasets) you would need to build a specific binary and use the -c parameter to specify it's path.
Instructions can be found on a separate [repo](https://github.com/Blobfish-LIFAT/Cplex-TAP)