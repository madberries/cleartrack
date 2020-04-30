# ClearTrack

ClearTrack automatically hardens Java bytecode to make it resistant to various
attacks, e.g. SQL injection.

This directory contains the ClearTrack instrumenter, i.e. the component of the
ClearTrack tool that hardens Java bytecode.  This file contains information about
building and testing the instrumenter, as well as a description of the contents
of this directory.


## 1. SETUP

This section describes how to set up a computer to build the instrumenter and
run instrumented code.


### 1.1 Mac OS X Setup

Mac OS X version 10.8.5 (Mountain Lion build 5) is known to work; similar
versions are likely to work as well.  

We recommend checking out this repository in a case-sensitive filesystem
since there can be case collisions between some file names.

You will obviously need `git`, and other tools as well.

One way to get them is to install one of the following: 
- Xcode from the Mac App Store.
- Command Line Tools from [here](https://developer.apple.com/downloads/index.action).

Then you will also need to install `libmagic` via either homebrew or macports (i.e.
`brew install libmagic`).


### 1.2 Linux Setup

You will need the `libmagic` header files.  In Ubuntu you can obtain these by
executing the following command:

    sudo apt-get install libmagic-dev

*NOTE: I am not sure why some linux distributions have this problem, but if you
see a message that looks like this while trying to run the Instrumentation JUnit
tests:*

    java: symbol lookup error: /usr/lib/jvm/java-7-oracle/jre/lib/amd64/libfstat.so: undefined symbol: magic_open
 
Then you may need to additionally set this in your bash environment:

    export LD_PRELOAD=/path/to/libmagic.so


### 1.3 Remaining Setup for All Computers

Make sure that the environment variable `LANG` is set to `en_US.UTF-8` to avoid Java
compilation problems.  You will also need to install OpenJDK 8 SDK.  It could work on
other JVMs, but we have only tested the instrumentation on this VM.


### 1.4 Optional Setup

Additional setup is required if you want to run the tests that use servers.  See
section 6 below for instructions.


## 2. BUILDING AND TESTING

Prior to building the instrumenter, you will need to build the required native
routines under `$CLEARTRACK_HOME/src-native`.  You will need `libmagic` and your `JAVA_HOME`
set.  Obtaining `libmagic` is described in setup above.

Make sure your `JAVA_HOME` is set, which will probably look like this:

    export JAVA_HOME=/usr/lib/jvm/java-8-openjdk
    
On the mac it will probably look something like this:

    export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_06.jdk/Contents/Home

Build the JNI library and copy it to the appropriate lib directory,
since these routines are invoked by classes loaded into the bootclass loader.
The appropriate lib directory structure differs on different OSes.
On a 64-bit Ubuntu linux system, it is likely to be here:  `$JAVA_HOME/jre/lib/amd64`.
On other linux versions, please determine the appropriate location.  On a Mac,
it is likely to be under `$JAVA_HOME/jre/lib`.

Execute the following commands:

    cd ../Instrumentation/src-native
    make
    # on a 64-bit Ubuntu linux:
    sudo cp libfstat.so $JAVA_HOME/jre/lib/amd64
    # or on the mac:
    sudo cp libfstat.jnilib $JAVA_HOME/jre/lib

To build the standalone instrumenter simply run:

    ant jar

If you want to build as well as perform unit testing, then run:

    ant test


## 3. INSTRUMENTING

First, I would recommend sourcing configure.bash into your bash environment:

    source configure.bash

Next, ClearTrack expects the `config_inst` file to live under your current working
directory (see `config_instr` under the root directory for an example).  You can
simply copy this file as a starting point.

You may wish to edit `config_inst` to change some repair or reporting policies.
The contents of `config_inst` will (in most cases) affect the instrumented bytecode.
You should therefore always remember to reinstrument when altering this file.

To instrument a set a java classfiles/jarfiles, run:

    cleartrack <options> <files>

The following options are available:

```
-a, --agent:
 The instrumenter will only preprocess the classes passed to the command for
 generating the 'ss-config.jar' under the output directory, which contains
 both the runtime configuration and hierarchy.obj (the serialized hierarchy
 of both the entire JDK and application).

-c, --confinement-off:
 By default, ClearTrack instruments bytecode to track meta data (e.g. taint)
 dynamically and to apply confinement actions to potential attacks (which are
 detected based on the meta data).  This option turns off confinement.  Normally
 this option would be used in conjunction with the -d option, otherwise the
 bytecode is not going to be hardened in any way against attacks (see
 documentation for the -d option).

-exts, --extension-dirs <directories>:
 If the bytecode being instrumented uses Java extensions
 (http://docs.oracle.com/javase/7/docs/technotes/guides/extensions/), the
 directories where the extensions reside must be passed to the instrumenter,
 using this option.

-o, --output <directory>:
 The instrumented bytecode (application and libraries), along with the
 instrumented JRE libraries (whose location is identified by the "java.home"
 property), are written into the directory, which is created if it doesn't
 exist.  If this option is omitted, the default directory is 'output-inst'.
 The directory will also contain 'javai', a convenience script for running the
 instrumented application.

-ow, --overwrite:
 Tells the instrumenter to overwrite the original bytecode with the instrumented
 bytecode, and saves the original under 'output-inst'.

-p, --prefix:
 This option prefixes 'pac.' to all the package names of the bytecode being
 processed.  No other change is made to the bytecode; in particular, the bytecode
 is not instrumented.  This option is used for libraries used by the instrumenter,
 as explained in the section below about libraries used by the instrumenter.

-q, --quiet:
 Runs the instrumenter in silent mode (i.e. no output).

-v, --verbose:
 Runs the instrumenter in verbose mode.

-V, --version:
 Only print out version information.
 ```

Typical usage of the instrumenter is:

    cleartrack Class1.class Class2.class ...

or:

    cleartrack myapp.jar ...

When an application and its libraries are instrumented, the instrumenter also
creates a modified version of the JRE libraries (whose location is identified
via the "`java.home`" property), and writes this modified version into the
directory `~/.cleartrack/jre`.  This only needs to be performed once.  However,
if you update your JDK version, then you should `rm -rf ~/.cleartrack` and
re-run `cleartrack` to instrument the new runtime.


### 3.1. Agent Mode

The ClearTrack instrumenter can also be run using the Java agent (see
[here](docs.oracle.com/javase/6/docs/api/java/lang/instrument/package-summary.html)).
This mode of instrumentation is meant for debugging and is not "officially"
supported, and in fact should just be considered deprecated.

Having said that, to run the ClearTrack in agent mode you must first preprocess
the application classes via the `-a` flag described above:

    cleartrack -a MainClass.class

OR

    cleartrack -a myapp.jar

This will produce the runtime configuration needed by the agent.  Note that the
JDK must be statically instrumented, so be sure to run the cleartrack command
with no arguments first (if you do not yet have an instrumented runtime).

Now to instrument dynamically with the agent, run the following command:

    javai -javaagent:cleartrack.jar MainClass

OR

    javai -javaagent:cleartrack.jar -cp myapp.jar my.main.MainClass

The advantage running ClearTrack in agent mode is that all of the classes loaded
into the JVM will be instrumented, regardless of whether they are part of the
application jar, downloaded from the network, constructed on the fly, etc.  But
as was mentioned earlier, this mode of operation is not fully supported at the
moment (though could be in the future).


## 4. LIBRARIES USED BY THE INSTRUMENTER

The instrumenter uses libraries that the bytecode being instrumented may also
use.  The instrumented bytecode must use an instrumented version of those
libraries, but the instrumenter itself must use the original (uninstrumented)
version of those libraries.  To avoid a runtime conflict, the instance of the
libraries used by the instrumenter are remapped to a different namespace.  This
is done by prepending the prefix 'pac.' to the former instance (this prefix is
also used in other package names of the instrumenter itself).

The `lib/` directory contains the renamed libraries used by the instrumenter.
These are the only libraries that this directory must contain.  The libraries
used by the tests are located under `lib-test/`.

As the instrumenter is further developed, it may use additional libraries.  The
following steps should be followed to add a new library to the instrumenter:

  1) Execute "`cleartrack --prefix lib/newlib.jar`"
     (This will will prefix all the package names in newlib with "`pac.`")

  2) cd to `output-inst/lib`
     (This is where the modified newlib gets placed.)

  3) Uncompress `newlib.jar`, `mkdir pac`, and move all package base dirs to `pac/`
     (This restructures the class files according to the new package names.)

  4) Repackage `newlib.jar` and replace it under `../../lib`.
     (Now the new library can be safely used by the instrumenter.  It will not be
     instrumented by the instrumenter, and if the application being instrumented
     happens to use the same library, it will be seen ad different due to the
     different package names.)


## 5. RUNNING

To run the application that you have just instrumented (in non-agent mode),
first source the configure.bash script in your current bash shell, which, among
other things, creates the alias `javai` to be used below.

Then invoke the instrumented application with the following command:

    javai -cp output-inst MyApp <param1> <param2> ...

OR

    javai -cp output-inst/somejar.jar my.main.MainClass

This will start the JVM with the modified JRE libraries that were generated
under `~/.cleartrack/jre`.


## 6. OPTIONAL SETUP FOR TESTS THAT USE SERVERS

Prior to running the JUnit tests that use servers, you will need to set up the
following environment variables to point to the machine of the respective
server:

- MYSQL_SERVER  (MySQL Server)
- MSSQL_SERVER  (Microsoft SQL Server)
- PSQL_SERVER   (PostgreSQL Server)
- XQUERY_SERVER (XQuery Server)

If an environment variable is unset, the respective tests will not be run.  The
only indication of this when you run the JUnit tests is a message to stdout like
this:

     [echo] IF ANY SERVERS SHOW UP IN THE FORM ${XXXX}, THEN THE ENVIRONMENT VARIABLE 
     [echo] FOR THAT SERVER HAS NOT BEEN SET AND THEREFORE WILL NOT BE TESTED:
     [echo]     MSSQL_SERVER = ${env.MSSQL_SERVER}
     [echo]     MYSQL_SERVER = ${env.MYSQL_SERVER}
     [echo]      PSQL_SERVER = ${env.PSQL_SERVER}
     [echo]    XQUERY_SERVER = ${env.XQUERY_SERVER}

If an environment variable is set but the named server is not working, you will
get a test failure.

If the servers are installed on the same local machine where the tests run, the
environment variables can be set to localhost.  The following subsections explain
how to set up the servers on the local machine.  If the servers run on external
machines, the instructions in the following subsections apply to those external
machines.

The environment variables above are only relevant to the JUnit tests.


### 6.1. MySQL Setup

#### 6.1.1. Mac

1) __Install MySQL:__

You can get MySQL using Homebrew or Macports, which are package managers.

To get MySQL without using a package manager, an installation disk image
(dmg file) of the "MySQL Community Server" can be downloaded from
[here](http://dev.mysql.com/downloads/mysql).  As of 2014-03-13, the newest
Generally Available (GA) release is labeled for OSX 10.7 but it seems to work
fine on OSX 10.8 and 10.9.

Also install `MySQLStartupItem.pkg` and `MySQL.prefPane`, in that order.  Installation
instructions for these are on dev.mysql.com as well.  The preference pane makes
it easy to start and stop the MySQL server and to turn on or off automatic startup
when your Mac boots.  Note that MySQL uses port 3306.

Add to your path the directory containing the mysql executable.  In the most
common case this will live under `/usr/local/mysql/bin`.

2) __Set the MySQL root password:__

*Note: this is not the same as the root or admin password of OS X -- this is a
unique password to the MySQL root user.  Use one and remember/jot down somewhere
what it is.  We call it MySQLrootPassword below.*

First ensure that the MySQL server is running.  Start it from the preference pane,
or execute the command:

    sudo /Library/StartupItems/MySQLCOM/MySQLCOM start

Then run:

    mysqladmin -u root password 'MySQLrootPassword'

3) __Create a user role used by the unit tests (ensuring the server is running):__

```
$ mysql -u root -p
<enter root password>
mysql> CREATE USER 'cleartrack'@'localhost';
mysql> SET PASSWORD FOR 'cleartrack'@'localhost'=PASSWORD('test');
mysql> GRANT ALL ON *.* TO 'cleartrack'@'localhost';
mysql> EXIT
```

4) __As mentioned above, if you want to run the MySQL unit tests you will
first need to set the environment variable MYSQL_SERVER, for example:__

```
export MYSQL_SERVER=localhost
```


#### 6.1.1. Linux

*TODO*


### 6.2. PostgreSQL Setup

#### 6.2.1. Mac

1) __Install PostgreSQL:__

There are several ways to install PostgreSQL (see [here](http://www.postgresql.org/download/macosx),
for example).  You can get PostgreSQL using Homebrew or Macports package managers.  For example,
type `brew install postgresql`.

To get PostgreSQL without using a package manager, the PostgreSQL.app can be
downloaded standalone [here](http://postgresapp.com).  It runs in the menu bar.

2) __Create the user role used by the tests:__

```
Start PostgreSQL (e.g. "Open psql" from the menu bar .app)
...=# CREATE USER cleartrack WITH PASSWORD 'test';
...=# CREATE DATABASE cleartrack_testing;
...=# GRANT ALL PRIVILEGES ON DATABASE cleartrack_testing TO cleartrack;
...=# ALTER USER cleartrack CREATEDB;
...=# ALTER DATABASE cleartrack_testing OWNER TO cleartrack;
...=# \q
(the "..." may vary in different systems)
```

#### 6.2.2. Linux

1) __Install PostgreSQL.  On Debian/Ubuntu distros:__

```
apt-get update
apt-get install postgresql postgresql-client
```

2) __Create the user role used by the tests:__

```
$ useradd -m cleartrack
$ sudo su - postgres
$ psql postgres
postgres=# CREATE USER cleartrack WITH PASSWORD 'test';
postgres=# CREATE DATABASE cleartrack_testing;
postgres=# GRANT ALL PRIVILEGES ON DATABASE cleartrack_testing TO cleartrack;
postgres=# ALTER USER cleartrack CREATEDB;
postgres=# ALTER DATABASE cleartrack_testing OWNER TO cleartrack;
postgres=# \q
```


### 6.3. XQuery Setup

1) __Download and install the Sedna XML database:__
   - [download](http://www.sedna.org/download.html)
   - [install instructions](http://www.sedna.org/install.html)

2) __Run the server, create test DB, and start test DB:__

```
$ sudo se_gov
$ sudo se_cdb test
$ sudo se_sm test
```

3) `wget http://people.csail.mit.edu/jeikenberry/sedna_db.tar.gz`
   (or just download it using your browser [here](http://people.csail.mit.edu/jeikenberry/sedna_db.tar.gz))

4) __Extract the tar file__

5) `sudo se_exp import test sedna_db`

6) __The XQuery server will now be running, but you will need to restart the
server and reload the database if you reboot your machine:__

```
$ sudo se_gov
$ sudo se_sm test
```

7) __As mentioned above, if you want to run the XQuery unit tests you will
first need to set the environment variable XQUERY_SERVER, for example:__

```
$ export XQUERY_SERVER=localhost
```

8) __To shutdown the test database:__

```
$ sudo se_smsd test
```

9) __To stop the server:__

```
$ sudo se_stop
```


### 6.4. Microsoft SQL Setup

1) __Download MSSQL Server 2008 from [here](http://www.microsoft.com/en-us/download/details.aspx?id=1695).__

2) __Open the setup executable and once it is finished unpacking click on
"Installation" then click on "New SQL Server stand-alone installation..."__

3) __Continue through the installation using the default settings, with the
following exceptions:__
  - On the "Feature Selection" page click on the "Database Engine Services"
    checkbox.
  - On the "Server Configuration" page select "NT AUTHORITY\NETWORK SERVICE" from
    the "Account Name" drop down menu.
  - On the "Database Engine Configuration" page click on "Add Current User".  Be
    sure to select mixed mode and enter an admin password.

4) __Once installed, open "SQL Server Configuration Manager" under `All Programs >
Microsoft SQL Server 2008` > Configuration Tools.__

5) __Expand "SQL Server Network Configuration" and select "Protocols for
SQLEXPRESS".__
  - Change TCP/IP to Enabled
  - Change Named Pipes to Enabled
  - Right click on TCP/IP and select properties.  Under IPAll set TCP Port to 1433

6) __Click on "SQL Server Services", right click on "SQL Server (SQLEXPRESS)", and
select restart.  Be sure that your Windows firewall is disabled.__

7) __Open a command prompt and execute `sqlcmd -S localhost -P sa` and enter your
admin password at the prompt:__

```
1> CREATE LOGIN cleartrack WITH PASSWORD = 'test'
2> CREATE USER cleartrack FOR LOGIN cleartrack;
3> GRANT ALTER To cleartrack;
4> GRANT CONTROL To cleartrack;
6> GO
1> SP_ADDSRVROLEMEMBER 'cleartrack', 'sysadmin'
2> GO
1> quit
```


## 7. BUILD PROCESS

Both the instrumenter and the instrumented bytecode use a modified version of
the JRE libraries.  The modified JRE libraries are generated as part of the build
process.

The modifications to the JRE libraries include instrumenting them, along with
other modifications.  But the instrumenter needs the modified JRE libraries to
compile and run.  To break this cycle, the build process first builds a subset of
the instrumenter that suffices to instrument the JRE libraries (using the
unmodified JRE libraries), and then builds the complete instrumenter with the
modified JRE libraries.

See the `build.xml` ant file for details.


## 8. CONTENTS OF THIS DIRECTORY

```
build.xml:
 Ant build file for instrumenter, tests, etc.

src:
 Source code of the instrumenter.

src-boot:
 Source code of the instrumenter, after preprocessing with instrumentation
 dependencies.  Dependencies include all code that refers to JDK taint fields/methods
 (or otherwise added fields/methods).  These are things that we add to the JDK but
 we do not exist at compilation time.  This directory is generated by the ant build.

src-opts:
 Source code of the instrumenter, after preprocessing with build options.  This is
 the second preprocess stage that is executed after ClearTrack has been properly
 bootstrapped.  Essentially this contains any flags to features we may want to turn
 off in a CPU efficient manner.  For example, by setting log_overflow to true you will
 enable that Java code to log overflow information on all calls at runtime.  This
 directory is generated by the ant build.

lib:
 Libraries used by the instrumenter, some of which may be used by the
 instrumented code as well.

lib-test:
 Libraries used by the tests.

lib-src:
 Source code for some libraries.  These are useful to the developers of the
 instrumenter, but are not used to build the instrumenter or the tests.

dangerous:
 Superficial source code required for the instrumenter to compile and generate
 instrumented code without errors.  This is only needed on dangerous types that
 have instrumentation wrappers.  It is required since the instrumenter needs to
 know about the dangerous type at instrumentation time before it is ever generated.

reports:
 All xml reports of tests generated from the junit.sh script go here.

system-test:
 Contains apache, daikon, and perhaps other tests.

jsqlparse:
 No longer used.

configure.bash:
 Convenience script for setting the javai alias to include the instrumented boot-
 classpath.  Also, sets the cleartrack alias so that you can run "cleartrack" in
 place of "java -jar cleartrack.jar".
```

## 9. OTHER FILES AND DIRECTORIES

When the instrumenter is run, it creates a modified version of the JRE
libraries, and writes it into `~/.cleartrack/jre`.  The directory `~/.cleartrack` is
deleted when `ant clean` is executed.
