#!/bin/sh
JAVA="`which java`"
CONFIGFILE="simple-web-spider.properties"
JAVANMAIN="simplespider.simplespider.Main"
OS="`uname`"

#check if OS is Sun Solaris or one of the OpenSolaris distributions and use different version of id if necessary
if [ $OS = "SunOS" ]
then
    # only this version of id supports the parameter -u
    ID="/usr/xpg4/bin/id"
else
    # regular id for any other case (especially Linux and OSX)
    ID="id"
fi

if [ "`${ID} -u`" -eq 0 ]
then
	echo
	echo "For security reasons you should not run this script as root!"
	echo
	exit 1
elif [ ! -x "${JAVA}" ]
then
	echo "The java command is not executable."
	echo "Either you have not installed java or it is not in your PATH"
	exit 1
fi

usage() {
	cat - <<USAGE
startscript for simple web spider on UNIX-like systems
Options
  -h, --help		show this help
  -p, --print-only	only print the command, which would be executed to start Simple Web Spider
  -- HOST PORT		set proxy host and port 
USAGE
}

#startup simple web spider directory
cd "`dirname $0`"

options="`getopt -u -n SimpleWebSpider -o -h,-p -l help,print-only -- $@`"
if [ $? -ne 0 ];then
	usage
	exit 1
fi

isparameter=0; #options or parameter part of getopts?
PARAMETER="" #parameters will be collected here

LOGGING=0
DEBUG=0
PRINTONLY=0
TAILLOG=0
for option in ${options};do
	if [ ${isparameter} -ne 1 ];then #option
		case ${option} in
			-h|--help) 
				usage
				exit 3
				;;
			-p|--print-out)
				PRINTONLY=1
				;;
			--)
				isparameter=1;
				;;
			*)
				echo "Invalid paramater ${option}"
				usage
				exit 1
				;;
		esac #case option 
	else #parameter
		PARAMETER="${PARAMETER} ${option}"
	fi #parameter or option?
done

#echo $options;exit 0 #DEBUG for getopts

#get javastart args
JAVA_ARGS="-server -XX:+UseAdaptiveSizePolicy";
#JAVA_ARGS="-verbose:gc -XX:+PrintGCTimeStamps -XX:+PrintGCDetails $JAVA_ARGS";

#check if Linux system supports large memory pages or if OS is Solaris which 
#supports large memory pages since version 9 
#(according to http://java.sun.com/javase/technologies/hotspot/largememory.jsp)
ENABLEHUGEPAGES=0;

if [ ${OS} = "Linux" ]
then
    HUGEPAGESTOTAL="`cat /proc/meminfo | grep 'HugePages_Total' | sed 's/[^0-9]//g'`"
    if [ -n "${HUGEPAGESTOTAL}" ] && [ ${HUGEPAGESTOTAL} -ne 0 ]
    then 
        ENABLEHUGEPAGES=1
    fi
elif [ ${OS} = "SunOS" ]
then
	# the UseConcMarkSweepGC option caused a full CPU usage - bug on Darwin.
	# It was reported that the same option causes good performance on solaris.
    JAVA_ARGS="${JAVA_ARGS} -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode"
    ENABLEHUGEPAGES=1
fi 

#turn on support for large memory pages if supported by OS
if [ ${ENABLEHUGEPAGES} -eq 1 ]
then
    JAVA_ARGS="${JAVA_ARGS} -XX:+UseLargePages"
fi

if [ -f ${CONFIGFILE} ]
then
	# Priority
	j="`grep '^javastart_priority' ${CONFIGFILE} | sed 's/^[^=]*=//'`";
	if [ ! -z "$j" ];then
		if [ -n $j ]; then JAVA="nice -n $j ${JAVA}"; fi;
	fi

	# All java setting but priority
	for i in `grep '^javastart_' ${CONFIGFILE} | grep -v '^javastart_priority' | sed 's/^[^=]*=//'`;do
		i="${i#javastart_*=}";
		JAVA_ARGS="-$i $JAVA_ARGS";
	done
else
    JAVA_ARGS="-Xmx16m -Xms64m ${JAVA_ARGS}";
fi

#echo "JAVA_ARGS: $JAVA_ARGS"
#echo "JAVA: $JAVA"

# generating the proper classpath
CLASSPATH=""
for i in lib/*.jar; do CLASSPATH="${CLASSPATH}$i:"; done

cmdline="${JAVA} ${JAVA_ARGS} -classpath ${CLASSPATH} ${JAVANMAIN} ${PARAMETER}";
if [ ${PRINTONLY} -eq 1 ];then
	echo ${cmdline}
else
	echo "****************** Simple Web Spider **********************************"
	echo "**** (C) by Michael Decker, usage granted under the GPL Version 3  ****"
	echo "****   USE AT YOUR OWN RISK! Project home and releases:            ****"
	echo "****   simplewebspider.sf.net                                      ****"
	echo "************************************************************************"
	eval ${cmdline}
fi
