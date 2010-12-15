#!/bin/bash

if [ -z "$JAVA_HOME" ]
then
    echo The JAVA_HOME environment variable is not defined
    exit 1
fi

SCRIPT="$0"

# SCRIPT may be an arbitrarily deep series of symlinks. Loop until we have the concrete path.
while [ -h "$SCRIPT" ] ; do 
  ls=`ls -ld "$SCRIPT"`
  # Drop everything prior to ->
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    SCRIPT="$link"
  else
    SCRIPT=`dirname "$SCRIPT"`/"$link"
  fi
done
 
KERNEL_HOME=`dirname "$SCRIPT"`/..
KERNEL_HOME=`cd $KERNEL_HOME; pwd`

#parse args for the script
TRUSTSTORE_PATH=$KERNEL_HOME/config/keystore
TRUSTSTORE_PASSWORD=changeit

shopt -s extglob

while (($# > 0))
	do
	case $1 in
	-truststore)
			TRUSTSTORE_PATH=$2
			shift;
			;;
	-truststorePassword)
			TRUSTSTORE_PASSWORD=$2
			shift;
			;;
	esac
	shift
done

JMX_OPTS=" \
	$JMX_OPTS \
	-J-Dcom.sun.tools.jconsole.mbeans.keyPropertyList=category,type \
	-J-Djavax.net.ssl.trustStore=$TRUSTSTORE_PATH \
	-J-Djavax.net.ssl.trustStorePassword=$TRUSTSTORE_PASSWORD"

$JAVA_HOME/bin/jconsole $JMX_OPTS
