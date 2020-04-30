ifndef JAVA_HOME
$(error JAVA_HOME is not set)
endif

CC=gcc
OS=$(shell uname | tr '[:upper:]' '[:lower:]')
CFLAGS=-L/usr/local/Cellar/libmagic/5.20/lib -I/usr/local/Cellar/libmagic/5.20/include -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(OS) -m64 -fPIC -c
ifeq ($(OS),linux)
	LDFLAGS=-shared -lmagic
	EXECUTABLE=libfstat.so
else
	LDFLAGS=-dynamiclib -lmagic
	EXECUTABLE=libfstat.jnilib
endif
SOURCES=pac_util_FileStat.c
OBJECTS=$(SOURCES:.c=.o)

all: $(SOURCES) $(EXECUTABLE)

$(EXECUTABLE): $(OBJECTS)
	$(CC) -L/usr/local/Cellar/libmagic/5.20/lib $(LDFLAGS) $(OBJECTS) -o $@

.c.o: 
	$(CC) $(CFLAGS) $< -o $@

clean:
	rm -f $(EXECUTABLE) $(OBJECTS)
