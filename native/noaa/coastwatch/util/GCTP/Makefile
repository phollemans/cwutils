########################################################################
#    FILE: Makefile
# PURPOSE: Makefile to generate GCTP shared library.
#  AUTHOR: Peter Hollemans
#    DATE: 2002/07/17
# CHANGES: 2002/08/29, PFH, moved files into src/
#          2002/10/16, PFH, added Solaris compile flags and comments
#          2003/12/06, PFH, added AIX and IRIX compile flags
#          2006/03/24, PFH, modified for Mac OS X
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
CC=gcc -arch $(arch) -dynamiclib
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
src=src/gctp.c src/alberfor.c src/alberinv.c src/alconfor.c \
  src/alconinv.c src/azimfor.c src/aziminv.c src/eqconfor.c \
  src/eqconinv.c src/equifor.c src/equiinv.c src/gnomfor.c \
  src/gnominv.c src/goodfor.c src/goodinv.c src/gvnspfor.c \
  src/gvnspinv.c src/hamfor.c src/haminv.c src/imolwfor.c \
  src/imolwinv.c src/lamazfor.c src/lamazinv.c src/merfor.c \
  src/merinv.c src/millfor.c src/millinv.c src/molwfor.c src/molwinv.c \
  src/obleqfor.c src/obleqinv.c src/omerfor.c src/omerinv.c \
  src/orthfor.c src/orthinv.c src/polyfor.c src/polyinv.c \
  src/psfor.c src/psinv.c src/robfor.c src/robinv.c src/sinfor.c \
  src/sininv.c src/somfor.c src/sominv.c src/sterfor.c src/sterinv.c \
  src/stplnfor.c src/stplninv.c src/tmfor.c src/tminv.c src/utmfor.c \
  src/utminv.c src/vandgfor.c src/vandginv.c src/wivfor.c \
  src/wivinv.c src/wviifor.c src/wviiinv.c src/for_init.c \
  src/inv_init.c src/cproj.c src/report.c src/lamccfor.c \
  src/lamccinv.c src/paksz.c src/untfz.c src/sphdz.c src/br_gctp.c
include=src/cproj.h src/proj.h
classes=$(cwfroot)/classes

# Targets
# -------
all: libGCTP.$(ext)

GCTP.h: $(classes)/noaa/coastwatch/util/GCTP.class
	javah -classpath $(classes) -o GCTP.h noaa.coastwatch.util.GCTP

libGCTP.$(ext): GCTP.c GCTP.h $(src) $(include)
	$(CC) GCTP.c $(src) -Isrc -o libGCTP.$(ext) $(javainc) -lm

install: libGCTP.$(ext)
	cp libGCTP.$(ext) $(lib)

clean:
	rm -f libGCTP* GCTP.h *~

distclean: clean
	rm -f $(lib)/libGCTP*
