#!/bin/bash
if [ $# -ne 1 ]; then
    echo "USAGE: $0 [databaseName]"
    exit 1
fi

WORKSPACE_HOME='/home/tesuji/Dropbox/Uni/MA/workspace/typeusage/'
echo $WORKSPACE_HOME
HSQLDB_HOME=$WORKSPACE_HOME/util/hsqldb-2.4.0
echo $HSQLDB_HOME

java -cp $HSQLDB_HOME/lib/hsqldb.jar org.hsqldb.util.DatabaseManagerSwing --driver org.hsqldb.jdbcDriver --url jdbc:hsqldb:file:$WORKSPACE_HOME/output/$1 --user SA