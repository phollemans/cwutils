########################################################################
#    FILE: Makefile
# PURPOSE: Makefile to generate CWF shared library.
#  AUTHOR: Peter Hollemans
#    DATE: 2002/07/17
# CHANGES: 2002/08/29, PFH, moved files into src/
#          2002/10/16, PFH, added Solaris compile flags and comments
#          2003/12/06, PFH, added AIX and IRIX compile flags
#          2006/03/23, PFH, modified for Mac OS X
########################################################################

# Operating system specific flags
# -------------------------------

# RedHat Linux
# CC=gcc -shared -Wall
# ext=so
# host=linux

# Sun Solaris
# CC=cc -G
# ext=so
# host=solaris

# IBM AIX
# CC=gcc -shared -Wall
# ext=so
# host=aix

# SGI IRIX
# CC=cc -n32 -shared -no_unresolved
# ext=so
# host=irix

# Mac OS X
#arch=ppc
#arch=i386
CC=gcc -arch $(arch) -dynamiclib -DHAS_ROUNDF
ext=jnilib.$(arch)
host=macosx

# Base paths
# ----------
#javaroot=/usr/local/j2sdk1.4.2
#javainc=-I$(javaroot)/include -I$(javaroot)/include/$(host)
javainc=-I/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers
cwfroot=../../../../..

# Libraries and source
# --------------------
lib=$(cwfroot)/lib/$(host)
src=src/cwflib.c src/cwproj.c
include=src/cwflib.h src/cwproj.h
classes=$(cwfroot)/classes

# Targets
# -------
all: libCWF.$(ext)

CWF.h: $(classes)/noaa/coastwatch/io/CWF.class
	javah -classpath $(classes) -o CWF.h noaa.coastwatch.io.CWF

libCWF.$(ext): CWF.c CWF.h $(src) $(include)
	$(CC) CWF.c $(src) -o libCWF.$(ext) -Isrc $(javainc) -lm

install: libCWF.$(ext)
	cp libCWF.$(ext) $(lib)

clean:
	rm -f libCWF* CWF.h *~

distclean: clean
	rm -f $(lib)/libCWF*
