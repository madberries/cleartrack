JFLAGS = -g
JC = javac

.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) -d bin $*.java

CLASSES = ./pac/example/BmpImageDecoder.java \
					./pac/example/ImageConverter.java

default: instr

bin:
	mkdir $@

classes: bin $(CLASSES:.java=.class)

instr: classes
	rm -rf output-inst
	java -javaagent:../lib/inliner.jar -Xbootclasspath/a:../lib/inliner.jar:../lib/asm-debug-all-5.0.3.jar -Xmx3g -jar ../cleartrack.jar bin

clean:
	$(RM) -r bin output-inst
