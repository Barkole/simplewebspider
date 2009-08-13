#!/bin/sh
#
# Simple Web Spider - <http://simplewebspider.sourceforge.net/>
# Copyright (C) 2009  <berendona@users.sourceforge.net>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
JAVA="`which java`"
CONFIGFILE="simple-web-spider.properties"
JAVANMAIN="simplespider.simplespider.Main"
LOGFILE="simple-web-spider.log"
PIDFILE="simple-web-spider.pid"
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
  -t, --tail-log	show the output of "tail -f ${LOGFILE}" after starting (enables "-l")
  -l, --logging		save the output to ${LOGFILE}
  -d, --debug		show the output on the console
  -p, --print-out	only print the command, which would be executed to start
  -- HOST PORT		set proxy host and port 
USAGE
}

check_already_runnning () {
	pid=`cat ${PIDFILE} 2>/dev/null` || true
	if [ -f ${PIDFILE} -a -n "${pid}" ]; then
		if kill -0 ${pid} 2>/dev/null; then
			echo "Alread running (there is ${PIDFILE} - PID ${pid})."
			exit 1
		fi
    fi
}


#startup simple web spider directory
cd "`dirname $0`"

options="`getopt -u -n SimpleWebSpider -o -h,-d,-l,-p,-t -l help,debug,logging,print-out,tail-log -- $@`"
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
			-l|--logging) 
				LOGGING=1
				if [ ${DEBUG} -eq 1 ];then
					echo "can not combine -l and -d"
					usage
					exit 1;
				fi
				;;
			-d|--debug)
				DEBUG=1
				if [ ${LOGGING} -eq 1 ];then
					echo "can not combine -l and -d"
					usage
					exit 1;
				fi
				;;
			-p|--print-out)
				PRINTONLY=1
				;;
			-t|--tail-log)
				LOGGING=1
				TAILLOG=1
				if [ ${DEBUG} -eq 1 ];then
					echo "can not combine -t and -d"
					usage
					exit 1;
				fi
				;;
			--)
				isparameter=1;
				;;
			*)
				echo "Invalid parameter ${option}"
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
JAVA_ARGS="";
#JAVA_ARGS="-server -XX:+UseAdaptiveSizePolicy -verbose:gc -XX:+PrintGCTimeStamps -XX:+PrintGCDetails $JAVA_ARGS";

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

if [ ${DEBUG} -eq 1 ] #debug
then
	cmdline=${cmdline}
elif [ $LOGGING -eq 1 ];then #logging
	cmdline="${cmdline} >> ${LOGFILE} 2>>${LOGFILE} & echo \$! > ${PIDFILE}"
else
	cmdline="$cmdline >/dev/null 2>/dev/null & echo \$! > ${PIDFILE}"
fi

if [ ${PRINTONLY} -eq 1 ];then
	echo "${cmdline}"
else
	echo "****************** Simple Web Spider **********************************"
	echo "**** (C) by Michael Decker, usage granted under the GPL Version 3  ****"
	echo "****   USE AT YOUR OWN RISK! Project home and releases:            ****"
	echo "****   simplewebspider.sf.net                                      ****"
	echo "***********************************************************************"
	
	check_already_runnning
	
	eval "${cmdline}"
	if [ ${TAILLOG} -eq 1 ];then
		sleep 1
		tail -f "${LOGFILE}"
	fi
fi
