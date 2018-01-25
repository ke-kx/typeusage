#!/bin/bash
#java -cp /home/tesuji/Dropbox/Uni/MA/workspace/typeusage/util/hsqldb-2.4.0/lib/hsqldb.jar org.hsqldb.util.DatabaseManagerSwing --driver org.hsqldb.jdbcDriver --url jdbc:hsqldb:file:/home/tesuji/Dropbox/Uni/MA/workspace/typeusage/output/test --user SA --script /home/tesuji/Dropbox/Uni/MA/workspace/typeusage/util/setupDB.sql

WORKSPACE_HOME='/home/tesuji/Dropbox/Uni/MA/workspace/typeusage/'
echo $WORKSPACE_HOME
HSQLDB_HOME=$WORKSPACE_HOME/util/hsqldb-2.4.0
echo $HSQLDB_HOME
java -jar $HSQLDB_HOME/lib/sqltool.jar --inlineRc=url=jdbc:hsqldb:file:$WORKSPACE_HOME/output/test,user=SA,password= $WORKSPACE_HOME/util/setupDB.sql