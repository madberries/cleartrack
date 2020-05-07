ifndef JAVA_HOME
$(error JAVA_HOME is not set)
endif

CC=gcc
OS=$(shell uname | tr '[:upper:]' '[:lower:]')
CFLAGS=-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(OS) -m64 -fPIC -c
ifeq ($(OS),linux)
	LDFLAGS=-shared -lmagic
	EXECUTABLE=libfstat.so
else
	CFLAGS+=-I/usr/local/Cellar/libmagic/5.38/include
	LDFLAGS=-dynamiclib -lmagic
	EXECUTABLE=libfstat.jnilib
endif
SOURCES=pac_util_FileStat.c
OBJECTS=$(SOURCES:.c=.o)

all: $(SOURCES) $(EXECUTABLE)

$(EXECUTABLE): $(OBJECTS)
	$(CC) $(LDFLAGS) $(OBJECTS) -o $@

.c.o: 
	$(CC) $(CFLAGS) $< -o $@

clean:
	rm -f $(EXECUTABLE) $(OBJECTS)
