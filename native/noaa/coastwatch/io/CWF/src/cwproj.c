/**********************************************************************/
/*
     FILE: cwproj.c
  PURPOSE: To define various CoastWatch projection routines.
   AUTHOR: Peter Hollemans
     DATE: 29/04/1998
  CHANGES: 05/11/1998, PFH, added return statement to cw_init_proj
           13/01/1999, PFH, added corrections for Alaska's polar
             stereographic files, cw_proj_info function
           14/01/1999, PFH, changed cw_polar_ijll / llij definition
	   28/02/1999, PFH, simplified round function
           04/03/1999, PFH, added corrections for zero ioff/joff linear
           2000/06/29, PFH, added corrections for zero resolution linear
	   2000/12/21, PFH, changed cw_init_proj to detect a southern
             hemisphere mercator only when the upper edge latitude is
             south of the equator (was causing problems in large Caribbean 
             regions)
           2006/02/24, PFH, changed name of cwf.h to cwflib.h; added
             HAS_ROUNDF test

  CoastWatch Format (CWF) Software Library and Utilities
  Copyright 1998-2001, USDOC/NOAA/NESDIS CoastWatch

*/
/**********************************************************************/

/* include files */

#include <stdio.h>
#include <math.h>
#include <string.h>

#include "cwflib.h"
#include "cwproj.h"

/* defined constants */

#define PI	     3.141592654
#define R	     6371.2
#define B	     4.14159203
#define JMAX	     24385
#define ICEN	     12193
#define DTOR(a)	     ((a)*PI/180)
#define RTOD(a)	     ((a)*180/PI)

/* defined macros */

#define LONR(a)	     {\
  if ((a) >= 180)\
    (a) -= 360;\
  else if ((a) < -180)\
    (a) += 360;\
}

static int cwerr;
#define CHK(r) { \
  if ((cwerr = (r)) != CW_NOERR) \
    return (cwerr); \
} 

/* global variables */

static short hem, splon, ioff, joff; 
static float end_lat, res, plon;
static int ptype = -1;

/**********************************************************************/

#ifndef HAS_ROUNDF

static float roundf (float f) {

/* Rounds a float to the nearest integer. */

  return ((float) (f > 0 ? floor (f+0.5) : ceil (f-0.5)));

} /* roundf */

#endif

/**********************************************************************/

static void polar_correct (void) {

/* Corrects parameters from the polar stereographic image header. */

  if (res == 1.5f) {				/* correct 1.5 km params */
    ioff = (short) roundf (ioff*1.5/1.47);
    joff = (short) roundf (joff*1.5/1.47);
    res = 1.47;
  } /* if */
  else if (res == 2.9f) {			/* correct 2.9 km params */
    ioff = (short) roundf (ioff*2.9/2.94);
    joff = (short) roundf (joff*2.9/2.94);
    res = 2.94;
  } /* if */

  if (splon == -132)				/* correct Alaska X,S,G,J,V */
    plon = -132.5;
  else if (splon == 180)			/* correct Alaska A */
    plon = -179.07;
  else if (splon == 179)			/* correct Alaska Z,N,C,B,T */
    plon = 179.65;
  else
    plon = splon;  

} /* polar_correct */

/**********************************************************************/

static int linear_correct (int cwid, int varid) {

/* Corrects parameters from the linear image header. */

float ul_lat, ul_lon;
float start_lat, end_lat, start_lon, end_lon;

  if (res == 0)					/* guess resolution */
    res = 0.01;

  if (ioff == 0 && joff ==0) {			/* correct ioff, joff */
    CHK (cw_get_att_float (cwid, varid, "start_latitude", &start_lat));
    CHK (cw_get_att_float (cwid, varid, "end_latitude", &end_lat));
    CHK (cw_get_att_float (cwid, varid, "start_longitude", &start_lon));
    CHK (cw_get_att_float (cwid, varid, "end_longitude", &end_lon));
    ul_lat = (start_lat > end_lat ? start_lat : end_lat);
    ul_lon = (start_lon < end_lon ? start_lon : end_lon);
    ioff = (short) roundf (ul_lon/res);
    joff = (short) - roundf (ul_lat/res);
  } /* if */

  return (CW_NOERR);				/* finish up */

} /* linear_correct */

/**********************************************************************/

int cw_init_proj (int cwid) {

/* Initializes the internal projection information.  Must be called before
   cw_get_ll or cw_get_ij.  Returns CW_NOERR on success, or a CWF error 
   status on failure. */

char text_ptype[20];
int varid;

						/* get projection type */
  varid = 0;
  CHK (cw_get_att_text (cwid, varid, "projection_type", text_ptype));

  if (strcmp (text_ptype, "unmapped") == 0) {	/* read attributes */
    ptype = UNMAPPED;
  } /* if */
  else if (strcmp (text_ptype, "mercator") == 0) {
    CHK (cw_get_att_float (cwid, varid, "end_latitude", &end_lat));
    if (end_lat > 0.0)
      hem = 1;
    else
      hem = -1;
    ptype = MERCATOR;
  } /* else if */
  else if (strcmp (text_ptype, "polar") == 0) {
    CHK (cw_get_att_short (cwid, varid, "polar_hemisphere", &hem));
    CHK (cw_get_att_short (cwid, varid, "polar_prime_longitude", &splon));
    ptype = POLAR;
  } /* if */
  else if (strcmp (text_ptype, "linear") == 0) {
    ptype = LINEAR;
  } /* else if */  
  
  if (ptype != UNMAPPED) {
    CHK (cw_get_att_float (cwid, varid, "resolution", &res));
    CHK (cw_get_att_short (cwid, varid, "grid_ioffset", &ioff));
    CHK (cw_get_att_short (cwid, varid, "grid_joffset", &joff));
  } /* if */

  if (ptype == LINEAR)				/* correct linear data */
    CHK (linear_correct (cwid, varid));

  if (ptype == POLAR)				/* correct polar data */
    polar_correct ();

  return (CW_NOERR);				/* finish up */

} /* cw_init_proj */

/**********************************************************************/

int cw_proj_info (proj_info *pinfo) {

/* Returns various projection info.  Not all fields in the pinfo struct
   apply to all projection types. */

  pinfo->ptype = ptype;				/* fill in fields */
  pinfo->res = res;
  pinfo->plon = plon;
  pinfo->hem = hem;
  pinfo->ioff = ioff;
  pinfo->joff = joff;

  return (CW_NOERR);				/* done */

} /* cw_proj_info */

/**********************************************************************/

void cw_get_ll (double i, double j, double *lat, double *lon) {

/* Calculate (latitude, longitude) from image (i, j).  If cw_init_proj has
   not been called, no calculation is performed.  For unmapped projections,
   i and j are simply copied into lon and lat. */

  switch (ptype) {
    case -1:
      break;
    case UNMAPPED:
      *lon = i;
      *lat = j;
      break;
    case MERCATOR:
      cw_mercator_ijll (i, j, lat, lon, hem, res, ioff, joff);
      break;
    case POLAR:
      cw_polar_ijll (i, j, lat, lon, hem, plon, res, ioff, joff);
      break;
    case LINEAR:
      cw_linear_ijll (i, j, lat, lon, res, ioff, joff);
      break;
  } /* switch */      

} /* cw_get_ll */

/**********************************************************************/

void cw_get_ij (double *i, double *j, double lat, double lon) {

/* Calculate image (i, j) from (latitude, longitude).  If cw_init_proj has
   not been called, no calculation is performed.  For unmapped projections,
   lon and lat are simply copied into i and j. */

  switch (ptype) {
    case -1:
      break;
    case UNMAPPED:
      *i = lon;
      *j = lat;
      break;
    case MERCATOR:
      cw_mercator_llij (i, j, lat, lon, hem, res, ioff, joff);
      break;
    case POLAR:
      cw_polar_llij (i, j, lat, lon, hem, plon, res, ioff, joff);
      break;
    case LINEAR:
      cw_linear_llij (i, j, lat, lon, res, ioff, joff);
      break;
  } /* switch */      

} /* cw_get_ij */

/**********************************************************************/

static void ijxy (double i, double j, double *x, double *y, float res, 
  short ioff, short joff) {

/* Calculate grid (x, y) from image (i, j) - this is the same for all
   projections. */

  *x = (i + ioff - 1)*res;
  *y = (j + joff - 1)*res;

} /* ijxy */

/**********************************************************************/

static void xyij (double *i, double *j, double x, double y, float res, 
  short ioff, short joff) {

/* Calculate image (i, j) from grid (x, y) - this is the same for all
   projections. */

  *i = x/res - ioff + 1;
  *j = y/res - joff + 1;

} /* xyij */

/**********************************************************************/

void cw_polar_ijll (double i, double j, double *lat, double *lon, short hem, 
  float plon, float res, short ioff, short joff) {

/* Calculate (latitude, longitude) from image (i, j) for polar sterographic
   projection. */

double x, y;
static double scale = -1;
double dist;
short signx;

  ijxy (i, j, &x, &y, res, ioff, joff);		/* convert to (x, y) */
  if (hem == -1)
    y = (JMAX + 1) - y;    

  if (scale == -1)				/* convert to (lat, lon) */
    scale = (1 + sin (DTOR (60.0)))*R;
  dist = sqrt (pow (x-ICEN, 2) + pow (y-ICEN, 2));
  *lat = 90.0 - RTOD (2*atan (dist/scale));
  LONR (plon);
  signx = (x-ICEN < 0 ? -1 : 1);
  *lon = RTOD (acos ((y-ICEN)/dist))*signx + plon;
  LONR (*lon);

} /* cw_polar_ijll */

/**********************************************************************/

void cw_polar_llij (double *i, double *j, double lat, double lon, short hem, 
  float plon, float res, short ioff, short joff) {

/* Calculate image (i, j) from (latitude, longitude) for polar sterographic
   projection. */

double x, y;
static double scale = -1;
double dist;

  if (scale == -1)				/* convert to (x, y) */
    scale = (1 + sin (DTOR (60.0)))*R;
  dist = scale*(cos (DTOR (lat))/(1 + sin (DTOR (lat))));
  LONR (lon);
  LONR (plon);
  x = ICEN + hem*dist*sin (DTOR (lon - plon));
  y = ICEN + hem*dist*cos (DTOR (lon - plon));
  if (hem == -1)
    y = (JMAX + 1) - y; 

  xyij (i, j, x, y, res, ioff, joff);		/* convert to (i, j) */

} /* polar_llij */

/**********************************************************************/

void cw_mercator_ijll (double i, double j, double *lat, double *lon, 
  short hem, float res, short ioff, short joff) {

/* Calculate (latitude, longitude) from image (i, j) for mercator 
   projection. */

double x, y;

  ijxy (i, j, &x, &y, res, ioff, joff);		/* convert to (x, y) */

						/* convert to (lat, lon) */
  *lat = RTOD (2*(atan (exp (fabs (y/R - B))) - PI/4));
  if (hem == -1)
    *lat = -fabs (*lat);
  else
    *lat = fabs (*lat);
  *lon = RTOD (x/R);

} /* mercator_ijll */

/**********************************************************************/

void cw_mercator_llij (double *i, double *j, double lat, double lon, 
  short hem, float res, short ioff, short joff) {

/* Calculate image (i, j) from (latitude, longitude) for mercator
   projection. */

double x, y, ycor;

  x = R*DTOR (lon);				/* convert to (x, y) */
  ycor = log (tan (PI/4 + fabs (DTOR (lat))/2));
  if (hem == -1)
    ycor = fabs (ycor);
  else
    ycor = -fabs (ycor);  
  y = R*(ycor + B);

  xyij (i, j, x, y, res, ioff, joff);		/* convert to (i, j) */

} /* mercator_llij */

/**********************************************************************/

void cw_linear_ijll (double i, double j, double *lat, double *lon, 
  float res, short ioff, short joff) {

/* Calculate (latitude, longitude) from image (i, j) for linear 
   projection. */

double x, y;

  ijxy (i, j, &x, &y, res, ioff, joff);		/* convert to (x, y) */

  *lat = -y;					/* convert to (lat, lon) */
  *lon = x;

} /* linear_ijll */

/**********************************************************************/

void cw_linear_llij (double *i, double *j, double lat, double lon, 
  float res, short ioff, short joff) {

/* Calculate image (i, j) from (latitude, longitude) for linear
   projection. */

double x, y;

  x = lon;					/* convert to (x, y) */
  y = -lat;
  
  xyij (i, j, x, y, res, ioff, joff);		/* convert to (i, j) */

} /* linear_llij */

/**********************************************************************/
