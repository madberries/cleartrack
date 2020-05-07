#!/bin/bash

# load instrumentation shortcuts (i.e. aliases)
shopt -s expand_aliases
source configure.bash

#appjars=`find output-inst/lib-test -name "*.jar" | tr "\n" ":"`
appjars="output-inst/lib-test/junit-4.12-SNAPSHOT.jar:output-inst/lib-test/esapi-2.0.1.jar:output-inst/lib-test/jackson-all-1.9.1.jar:output-inst/lib-test/postgresql-9.1-901.jdbc4.jar:output-inst/lib-test/guava-r08.jar:output-inst/lib-test/h2-1.2.144.jar:output-inst/lib-test/mysql-connector-java-5.1.13-bin.jar:output-inst/lib-test/sqljdbc4.jar:output-inst/lib-test/sedna-xqj-beta-5.jar:output-inst/lib-test/selenium/selenium-java-2.15.0.jar:output-inst/lib-test/hibernate/hibernate3.jar:output-inst/lib-test/hibernate/dom4j-1.6.1.jar:output-inst/lib-test/hibernate/slf4j-api-1.6.1.jar:output-inst/lib-test/hibernate/javassist-3.12.0.GA.jar:output-inst/lib-test/hibernate/jta-1.1.jar:output-inst/lib-test/hibernate/hibernate-jpa-2.0-api-1.0.1.Final.jar:output-inst/lib-test/hibernate/commons-collections-3.1.jar:output-inst/lib-test/asm-debug-all-4.0.jar:output-inst/lib-test/antlr-3.4-runtime.jar:output-inst/lib-test/JarBean.jar:output-inst/lib-test/jlibs-core.jar"

runall=0
classes=""

# set up the java arguments
java_args="-Xmx1500M -Dorg.owasp.esapi.resources=."
java_args="$java_args -Dpac.test.testProperty=tainted_property"
java_args="$java_args -Duser.name=root"

if [[ $# = 0 ]]
then
   echo "USAGE: junit.sh [-debug|-all] <test> ..."
   exit 1
else
   for arg in $@
   do
      if [[ "$arg" = "-debug" ]]
      then
         java_args="$java_args -Xdebug -Xrunjdwp:transport=dt_socket,address=4143,server=y,suspend=y"
      else
         echo $arg | grep -q "*"
         if [[ $? == 0 ]];
         then
            tests="test/"`echo $arg | tr "." "/"`"Test.java"
            for test in `ls $tests`
            do
               test=`echo ${test:5} | sed -e "s/\.java$//g" | tr "/" "."`
               classes="$classes $test"
            done
         else
            classes="$classes $arg"
         fi
      fi
   done
fi

# make sure there are junit tests to run
if [[ -z "$classes" ]]
then
   echo "ERROR: no junit tests found"
   exit 1
fi

# Some CPUs throttle the clock speed dynamically (based on CPU temps), for example
# the Macbook Pro.  Run osx-cpu-temp to ensure we have low enough temps to get
# consistent benchmark results (presuming this command is installed).
which osx-cpu-temp &>/dev/null
if [[ $? == 0 ]]; then
   echo "Waiting for CPU to cool down..."
   max_temp="51.0"
   unset cpu_temp
   temp_chk=1
   # Loop until cpu temp is < 51.0 celsius
   while [[ $temp_chk == 1 ]]; do
      if [[ ! -z $cpu_temp ]]; then
         sleep 1
      fi
      cpu_temp=$(osx-cpu-temp -C | sed -e 's/°C$//g')
      temp_chk=$(echo "$cpu_temp > $max_temp" | bc)
   done
fi

# run the tests
#classes=`echo $classes | tr " " "\n" | shuf | tr "\n" " "`
javai $java_args -cp output-inst/bin:$appjars pac.test.JUnitRunner ${classes}
exit $?
