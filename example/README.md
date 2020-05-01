# Example

Be sure to first build the instrumenter before running this example.  Once
built, simply run `make` in this directory.  This will build and instrument
this example program that simply takes an image from the command line, converts
it to grayscale, and then writes the resulting image to a location also
specified on the command line.


## Attack Input

```
$ javai -cp output-inst/bin pac.example.ImageConverter cat.bmp bin/../cat_bw.bmp
Unable to write file: bin/../cat_bw.vmp
```

Notice that the program is unable to save the file as `bin/../cat_bw.bmp`.  If
you check the `cleartrack.log` file, you should see that the instrumentation
intervened, and replaced the file path with `bin/__/cat_bw.vmp` (See the log
output below).

```
ClearTrack Agent. Version information: Built by jeikenberry at Tue Apr 14 17:01:44 EDT 2020
    M       pac/agent/CleartrackClassWriter.java
    M       pac/agent/CleartrackMain.java
    M       pac/agent/CleartrackVerifier.java
    M       pac/agent/hierarchy/ClassHierarchy.java
    M       pac/agent/hierarchy/ClassType.java
    M       pac/agent/hierarchy/MethodType.java
    M       pac/config/RunChecks.java
    M       pac/inst/taint/SystemInstrumentation.java
    ?       pac/inst/taint/VMInstrumentation.java
    M       pac/util/SSVersion.java
Configuration parsed from ./config_inst
Agent arguments = [ CONFINEMENT, TAINT_UNKNOWN ]
CleartrackAgent loaded

***
19:10:03 05/01/2020
WARN: CWE-23: check: filename_check  directive: internal_black  regular expression: [.]{2}|[/]{2}|;  matched: "bin/../cat_bw.vmp"
replacement: bin/__/cat_bw.vmp

Executing: java/io/File.<init>(Ljava/lang/String;)
aaaaaaaaaaaaaaaaa
_________________
bin/../cat_bw.vmp
Action: replace
  pac.config.Notify.getApplicationStackTrace  line:310
  pac.config.Notify.appendToLogFile  line:353
  pac.config.Notify.appendToLogFile  line:360
  pac.config.Notify.notifyAndRespond  line:305
  pac.config.Notify.run_checks  line:555
  pac.example.ImageConverter.convertBmpFileToGrayScale  line:51
  pac.example.ImageConverter.main  line:34
  pac.example.ImageConverter.main  line:-1

Total Execution Time: 0.284 seconds
```

As you can see, this prevented the path traversal attack from succeeding since
the underlying `FileOutputStream` writing the altered `File` object threw a
`FileNotFoundException` in this case, thus producing the error message that you
see above.


## Benign Input

```
$ javai -cp output-inst/bin pac.example.ImageConverter cat.bmp cat_bw.bmp
RESULT: success
```

Notice that the program successfully generates the black and white image of
`cat.bmp`, and writes the file to `cat_bw.bmp`.  Also, if you check the
`cleartrack.log` file you should see that this file is free of policy check
failures (as it was specified by the `config_inst` file).

