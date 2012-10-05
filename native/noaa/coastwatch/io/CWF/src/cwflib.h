/**********************************************************************/
/*
     FILE: cwflib.h
  PURPOSE: To define the CoastWatch format (cwf) library interface
           routines.  The library is based on a combination of an
           original set of IMGMAP data access routines and the netCDF
           library.
   AUTHOR: Peter Hollemans
     DATE: 15/02/1998
  CHANGES: 2001/07/25, PFH, added cw_clone stub, reformatted
           2006/03/24, PFH, changed name for Mac OS X
    NOTES: All user functions return CW_NOERR on success or
           an error code on exit.  The error code can be used with
           cw_strerr for a verbose error description.

  CoastWatch Format (CWF) Software Library and Utilities
  Copyright 1998-2001, USDOC/NOAA/NESDIS CoastWatch

*/
/**********************************************************************/

#ifndef CWFLIB_H
#define CWFLIB_H

/* Defines */
/* ------- */
#define CW_CLOBBER		0
#define CW_NOCLOBBER		1
#define CW_NOWRITE		0
#define CW_WRITE		1
#define CW_MAX_NAME		30
#define CW_BYTE			0
#define CW_CHAR			1
#define CW_SHORT       		2
#define CW_FLOAT		3
#define CW_MAX_VAR_DIMS		2
#define CW_NOERR		0
#define CW_BADVAL		-999.0f

/* Macros */
/* ------ */
#define CW_GET_G(a,b)		((a) & (0x01 << ((b)-1)))
#define CW_PUT_G(a,b)		((a) | (0x01 << ((b)-1)))

/* Types */
/* ----- */
typedef short cw_type;

/* Dataset functions */
/* ----------------- */
int cw_create (const char *path, int cmode, int *cwidp);
int cw_clone (int cwid, int cmode, int *cwidp);
int cw_open (const char *path, int omode, int *cwidp);
int cw_enddef (int cwid);
int cw_close (int cwid);
const char *cw_strerror (int cwerr);

/* Dimension functions */
/* ------------------- */
int cw_def_dim (int cwid, const char *name, size_t len, int *dimidp);
int cw_inq_dimid (int cwid, const char *name, int *dimidp);
int cw_inq_dim (int cwid, int dimid, char *name, size_t *lengthp);

/* Variable functions */
/* ------------------ */
int cw_def_var (int cwid, const char *name, cw_type xtype, int ndims,
  const int dimids[], int *varidp);
int cw_inq_varid (int cwid, const char *name, int *varidp);
int cw_inq_var (int cwid, int varid, char *name, cw_type *xtypep,
  int *ndimsp, int dimids[], int *nattsp);
int cw_put_vara_float (int cwid, int varid, const size_t start[], 
  const size_t count[], const float *fp);
int cw_put_vara_uchar (int cwid, int varid, const size_t start[], 
  const size_t count[], const unsigned char *ucp);
int cw_get_vara_float (int cwid, int varid, const size_t start[],
  const size_t count[], float *fp);
int cw_get_vara_uchar (int cwid, int varid, const size_t start[],
  const size_t count[], unsigned char *ucp);

/* Attribute functions */
/* ------------------- */
int cw_inq_attname (int cwid, int varid, int attid, char *name);
int cw_inq_att (int cwid, int varid, const char *name, cw_type *xtypep, 
  size_t *lenp);
int cw_inq_attid (int cwid, int varid, const char *name, int *attidp);
int cw_put_att_text (int cwid, int varid, const char *name,
  size_t len, const char *tp);
int cw_put_att_short (int cwid, int varid, const char *name,
  cw_type xtype, size_t len, const short *sp);
int cw_put_att_float (int cwid, int varid, const char *name, 
  cw_type xtype, size_t len, const float *fp);
int cw_get_att_text (int cwid, int varid, const char *name, char *tp);
int cw_get_att_short (int cwid, int varid, const char *name, short *sp);
int cw_get_att_float (int cwid, int varid, const char *name, float *fp);

#endif

/**********************************************************************/
