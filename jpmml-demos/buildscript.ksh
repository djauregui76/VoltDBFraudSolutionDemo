#!/bin/ksh
RESTART=$2
echo RESTART=$RESTART
export COMPUTERNAME=`hostname`
TIMESTAMP=`date +%Y%m%d%H%M`
TEMPDIR=/tmp/ant-build-$TIMESTAMP
mkdir $TEMPDIR
PROJECT=$1
cp -pr /repo_export/projects/$PROJECT $TEMPDIR/
cp -pr /repo_export/projects/spe-appserverlibs $TEMPDIR/
cd $TEMPDIR/$PROJECT/src
/opt/bea/weblogic81/server/bin/ant all bea_deploy
cd /
rm -R $TEMPDIR
if [ $RESTART = "TRUE" ]
then
/opt/bea/user_projects/domains/mydomain/stopsmallapps.sh
/opt/bea/user_projects/domains/mydomain/startsmallapps.sh
fi


