#!/bin/bash

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

# determine kernel home
KERNEL_HOME=`dirname "$SCRIPT"`/..

# make KERNEL_HOME absolute
KERNEL_HOME=`cd $KERNEL_HOME; pwd`

# setup classpath and java environment
. $KERNEL_HOME/bin/setupClasspath.sh

# execute user setenv script if needed
if [ -r $KERNEL_HOME/bin/setenv.sh ]
then
	. $KERNEL_HOME/bin/setenv.sh
fi


# Run java version check with the discovered java jvm.
. $KERNEL_HOME/bin/checkJava.sh

shopt -s extglob
	
# parse the command we executing
COMMAND=$1
shift;
	
if [ "$COMMAND" = "start" ]
then
	
	# parse the standard arguments
	CONFIG_DIR=$KERNEL_HOME/config
	CLEAN_FLAG=

	SHELL_FLAG=
	
	DEBUG_FLAG=
	DEBUG_PORT=8000
	SUSPEND=n
	if [ -z "$JMX_PORT" ]
	then
		JMX_PORT=9875
	fi
	
	if [ -z "$KEYSTORE_PASSWORD" ]
	then
		KEYSTORE_PASSWORD=changeit
	fi
	
	ADDITIONAL_ARGS=

	while (($# > 0))
		do
		case $1 in
		-debug)
				DEBUG_FLAG=1
				if [[ "$2" == +([0-9]) ]]
				then
					DEBUG_PORT=$2
					shift;
				fi
				;;
		-clean)
				CLEAN_FLAG=1
				;;
		-configDir)
				CONFIG_DIR=$2
				shift;
				;;
		-jmxport)
				JMX_PORT=$2
				shift;
				;;
		-keystore)
				KEYSTORE_PATH=$2
				shift;
				;;
		-keystorePassword)
				KEYSTORE_PASSWORD=$2
				shift;
				;;
		-suspend)
				SUSPEND=y
				;;
		-shell)
				SHELL_FLAG=1
				;;
		*)
				ADDITIONAL_ARGS="$ADDITIONAL_ARGS $1"
				;;
		esac
		shift
	done
	
	# start the kernel
	if [[ "$CONFIG_DIR" != /* ]]
	then
	    CONFIG_DIR=$KERNEL_HOME/$CONFIG_DIR
	fi

	if [ -z "$KEYSTORE_PATH" ]
	then
	    KEYSTORE_PATH=$CONFIG_DIR/keystore
	fi

	if [ "$DEBUG_FLAG" ]
	then
		DEBUG_OPTS=" \
			-Xdebug \
			-Xrunjdwp:transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=$SUSPEND"
	fi

	if [ "$CLEAN_FLAG" ]
	then
		rm -rf $KERNEL_HOME/work
		rm -rf $KERNEL_HOME/serviceability

		LAUNCH_OPTS="$LAUNCH_OPTS -Fosgi.clean=true"
	fi
	
	if [ "$SHELL_FLAG" ]
	then
	    echo "Warning: Kernel shell not supported; -shell option ignored."
		# LAUNCH_OPTS="$LAUNCH_OPTS -Forg.eclipse.virgo.kernel.shell.local=true"
	fi
	
	# Set the required permissions on the JMX configuration files
	chmod 600 $CONFIG_DIR/org.eclipse.virgo.kernel.jmxremote.access.properties

	JMX_OPTS=" \
		$JMX_OPTS \
		-Dcom.sun.management.jmxremote.port=$JMX_PORT \
		-Dcom.sun.management.jmxremote.authenticate=true \
		-Dcom.sun.management.jmxremote.login.config=virgo-kernel \
		-Dcom.sun.management.jmxremote.access.file=$CONFIG_DIR/org.eclipse.virgo.kernel.jmxremote.access.properties \
		-Djavax.net.ssl.keyStore=$KEYSTORE_PATH \
		-Djavax.net.ssl.keyStorePassword=$KEYSTORE_PASSWORD \
		-Dcom.sun.management.jmxremote.ssl=true \
		-Dcom.sun.management.jmxremote.ssl.need.client.auth=false"

	# If we get here we have the correct Java version.
	
	TMP_DIR=$KERNEL_HOME/work/tmp
	
	# Ensure that the tmp directory exists
	mkdir -p $TMP_DIR
	
	cd $KERNEL_HOME; $JAVA_HOME/bin/java \
		$JAVA_OPTS \
		$DEBUG_OPTS \
		$JMX_OPTS \
		-XX:+HeapDumpOnOutOfMemoryError \
		-XX:ErrorFile=$KERNEL_HOME/serviceability/error.log \
		-XX:HeapDumpPath=$KERNEL_HOME/serviceability/heap_dump.hprof \
		-Djava.security.auth.login.config=$CONFIG_DIR/org.eclipse.virgo.kernel.authentication.config \
		-Dorg.eclipse.virgo.kernel.authentication.file=$CONFIG_DIR/org.eclipse.virgo.kernel.users.properties \
		-Djava.io.tmpdir=$TMP_DIR \
		-Dorg.eclipse.virgo.kernel.home=$KERNEL_HOME \
		-classpath $CLASSPATH \
		org.eclipse.virgo.osgi.launcher.Launcher \
	    -config $KERNEL_HOME/lib/org.eclipse.virgo.kernel.launch.properties \
		-Forg.eclipse.virgo.kernel.home=$KERNEL_HOME \
		-Forg.eclipse.virgo.kernel.config=$CONFIG_DIR \
		-Fosgi.configuration.area=$KERNEL_HOME/work/osgi/configuration \
		-Fosgi.java.profile="file:$KERNEL_HOME/lib/java6-server.profile" \
		$LAUNCH_OPTS \
		$ADDITIONAL_ARGS
elif [ "$COMMAND" = "stop" ]
then

	#parse args for the script
	if [ -z "$TRUSTSTORE_PATH" ]
	then
		TRUSTSTORE_PATH=$KERNEL_HOME/config/keystore
	fi
	
	if [ -z "$TRUSTSTORE_PASSWORD" ]	
	then
		TRUSTSTORE_PASSWORD=changeit
	fi

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
		*)
			OTHER_ARGS+=" $1"
			;;
		esac
		shift
	done
	
	JMX_OPTS=" \
		$JMX_OPTS \
		-Djavax.net.ssl.trustStore=${TRUSTSTORE_PATH} \
		-Djavax.net.ssl.trustStorePassword=${TRUSTSTORE_PASSWORD}"

	$JAVA_HOME/bin/java $JAVA_OPTS $JMX_OPTS \
		-classpath $CLASSPATH \
		-Dorg.eclipse.virgo.kernel.home=$KERNEL_HOME \
		org.eclipse.virgo.kernel.shutdown.ShutdownClient $OTHER_ARGS
	
else
	echo "Unknown command: ${COMMAND}"
fi

