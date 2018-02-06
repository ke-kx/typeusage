#!/bin/bash


if [ $# -ne 1 ]; then
    echo "USAGE: $0 [databaseName]"
    exit 1
fi

WORKSPACE_HOME='/home/tesuji/Dropbox/Uni/MA/workspace/typeusage/'
echo "Workspace home: "$WORKSPACE_HOME
HSQLDB_HOME=$WORKSPACE_HOME/util/hsqldb-2.4.0
echo "HSQLDB home: "$HSQLDB_HOME

java -Xms512m -Xmx15g -jar $HSQLDB_HOME/lib/sqltool.jar --inlineRc=url=jdbc:hsqldb:file:$WORKSPACE_HOME/output/$1,user=SA,password= $WORKSPACE_HOME/util/buildMMCTables.sql
