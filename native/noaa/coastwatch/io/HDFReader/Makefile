########################################################################
#    FILE: Makefile
# PURPOSE: Makefile to generate HDFReader shared library.
#  AUTHOR: Peter Hollemans
#    DATE: 2002/07/23
# CHANGES: 2002/10/16, PFH, added Solaris compile flags and comments
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
# CC=xlc -qlanglvl=ansi -qmkshrobj -brtl
# CC=gcc -fpic -Wall
# ext=so
# host=aix

# SGI IRIX
# CC=cc -n32 -shared -no_unresolved
# ext=so
# host=irix

# Mac OS X
CC=gcc -dynamiclib
ext=jnilib
host=macosx

# Base paths
# ----------
#javaroot=/usr/local/j2sdk1.4.2
#javainc=-I$(javaroot)/include -I$(javaroot)/include/$(host)
javainc=-I/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers
cwfroot=../../../../..
hdfroot=$(HOME)/Documents/CoastWatch\ Projects/CoastWatch\ Utilities/hdf/HDF4.2r1/NewHDF

# Libraries and source
# --------------------
hdfinc=-I$(hdfroot)/include
lib=$(cwfroot)/lib/$(host)
classes=$(cwfroot)/classes

# Targets
# -------
all: libHDFReader.$(ext)

HDFReader.h: $(classes)/noaa/coastwatch/io/HDFReader.class
	javah -classpath $(classes) -o HDFReader.h \
	noaa.coastwatch.io.HDFReader 

libHDFReader.$(ext): HDFReader.c HDFReader.h 
	$(CC) HDFReader.c -o libHDFReader.$(ext) $(javainc) $(hdfinc) \
	-L$(lib) -ljhdf

install: libHDFReader.$(ext)
	cp libHDFReader.$(ext) $(lib)

clean:
	rm -f libHDFReader.$(ext) HDFReader.o HDFReader.h *~

distclean: clean
	rm -f $(lib)/libHDFReader.$(ext)
