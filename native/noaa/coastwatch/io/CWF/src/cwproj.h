/**********************************************************************/
/*
     FILE: cwproj.h
  PURPOSE: To define various CoastWatch projection routines.
   AUTHOR: Peter Hollemans
     DATE: 29/04/1998
  CHANGES: 07/06/1998, PFH, added cw_init_proj, cw_get_ll, and cw_get_ij
           13/01/1999, PFH, added cw_proj_info, various constants and
             typedefs
           14/01/1999, PFH, changed cw_polar_ijll / llij definition
    NOTES: The image coordinates passed to these routines are (i, j) =
           (column, row), where (1, 1) is the top-left corner pixel of the 
           image.

  CoastWatch Format (CWF) Software Library and Utilities
  Copyright 1998-2001, USDOC/NOAA/NESDIS CoastWatch

*/
/**********************************************************************/

#ifndef CWPROJ_H
#define CWPROJ_H

/* defined constants */

#define	UNMAPPED     0				/* projection types */
#define	MERCATOR     1
#define	POLAR        2
#define	LINEAR	     3

#define NORTH	     1				/* hemispheres */
#define SOUTH	     -1

/* type declarations */

typedef struct {
  int ptype;					/* projection type */
  float res;					/* resolution */
  float plon;					/* prime longitude (polar) */
  short hem;					/* hemisphere */
  short ioff, joff;				/* i, j grid offsets */
} proj_info;

/* function prototypes */

int cw_init_proj (int cwid);
int cw_proj_info (proj_info *pinfo);
void cw_get_ll (double i, double j, double *lat, double *lon);
void cw_get_ij (double *i, double *j, double lat, double lon);

void cw_polar_ijll (double i, double j, double *lat, double *lon, 
  short hem, float plon, float res, short ioff, short joff);
void cw_polar_llij (double *i, double *j, double lat, double lon, 
  short hem, float plon, float res, short ioff, short joff); 

void cw_mercator_ijll (double i, double j, double *lat, double *lon, 
  short hem, float res, short ioff, short joff);
void cw_mercator_llij (double *i, double *j, double lat, double lon, 
  short hem, float res, short ioff, short joff);

void cw_linear_ijll (double i, double j, double *lat, double *lon, 
  float res, short ioff, short joff);
void cw_linear_llij (double *i, double *j, double lat, double lon, 
  float res, short ioff, short joff);
 
#endif

/**********************************************************************/
