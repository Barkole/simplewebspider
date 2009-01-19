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
PIDFILE="simple-web-spider.pid"
NAME="Simple Web Spider"
WAITFORDAEMON=120

wait_for_deaddaemon () {
	pid=$1
	echo -n "${NAME} stopping"
	sleep 1
	if test -n "${pid}" ; then
		if kill -0 ${pid} 2>/dev/null ; then
			echo -n "."
			cnt=0
			while kill -0 ${pid} 2>/dev/null ; do
				cnt=`expr ${cnt} + 1`
				if [ ${cnt} -gt ${WAITFORDAEMON} ] ; then
					echo " FAILED."
					echo "Killing now ${NAME} (PID ${pid})..."
					kill -KILL ${pid}
					rm -f ${PIDFILE}
					return 1
				fi
				sleep 1
				echo -n "."
			done
		fi
	fi
	echo " DONE"
	rm -f ${PIDFILE}
	return 0
}

usage() {
	cat - <<USAGE
stopscript for simple web spider on UNIX-like systems
Options
  -h, --help		show this help
  -n, --no-wait		does not wait for stop (and causes no kill after timeout)
USAGE
}



#startup simple web spider directory
cd "`dirname $0`"

options="`getopt -u -n SimpleWebSpider -o -h,-n -l help,no-wait -- $@`"
if [ $? -ne 0 ];then
	usage
	exit 1
fi

isparameter=0; #options or parameter part of getopts?
PARAMETER="" #parameters will be collected here

NOWAIT=0
for option in ${options};do
	if [ ${isparameter} -ne 1 ];then #option
		case ${option} in
			-h|--help) 
				usage
				exit 3
				;;
			-n|--no-wait) 
				NOWAIT=1
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
		echo "Invalid parameter ${option}"
		usage
		exit 1
		#PARAMETER="${PARAMETER} ${option}"
	fi #parameter or option?
done


pid=`cat ${PIDFILE} 2>/dev/null` || true

if [ -f ${PIDFILE} -a -n "${pid}" ]; then
	if kill -0 ${pid} 2>/dev/null ; then
		echo "Sending HUP signal to PID ${pid}"
		kill -HUP ${pid}
		if [ ${NOWAIT} -eq 0 ];then
			wait_for_deaddaemon ${pid}
		fi
	else
		echo "No running instance with PID ${pid} available"
		exit 1
	fi
else
	echo "No running instance available"
	exit 1
fi
