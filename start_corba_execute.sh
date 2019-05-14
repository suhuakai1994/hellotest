#!/bin/sh
if [ -z "$JAVA_HOME" ] ; then
  JAVA=`which java`
  if [ -z "$JAVA" ] ; then
    echo "Cannot find JAVA. Please set your PATH."
    exit 1
  fi
  JAVA_BIN=`dirname $JAVA`
  JAVA_HOME=$JAVA_BIN/..
fi

JAVA=$JAVA_HOME/bin/java
export TCOO_HOME=/export/home/apitest/corba-execute-service/OpenORB-1.4.0

PROP="-Dimap.sslCertificatesPath=$TCOO_HOME/conf/ssl"
PROP="$PROP -Dopenorb.home.path=${TCOO_HOME}"
BOOTCLASSPATH=$TCOO_HOME/OpenORB/lib/endorsed/openorb_orb_omg-1.4.0.jar;

VM_ARGS="-Xbootclasspath/p:$BOOTCLASSPATH -Dopenorb.home.path=$TCOO_HOME -Dopenorb.env.cp=$CLASSPATH $PROP"

nohup $JAVA $VM_ARGS -jar ./corba-execute-service-0.0.1-SNAPSHOT.jar >/dev/null 2>&1 &
