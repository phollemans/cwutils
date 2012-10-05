/**********************************************************************/
/*
     FILE: HDFWriter.c
  PURPOSE: To implement the native portion of HDFWriter.
   AUTHOR: Peter Hollemans
     DATE: 2002/07/23
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

*/
/**********************************************************************/

/* Includes */
/* -------- */
#include "HDFWriter.h"
#include "mfhdf.h"

/* Function prototypes */
/* ------------------- */
jboolean h4raiseException (JNIEnv *env, char *message);

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_HDFWriter
 * Method:    setChunkCompress
 * Signature: (IZ[I)V
 */
JNIEXPORT void JNICALL Java_noaa_coastwatch_io_HDFWriter_setChunkCompress
  (JNIEnv *env, jclass class, jint sdsid, jboolean compressed, 
  jintArray chunk_lengths) {

  intn ret;
  HDF_CHUNK_DEF c_def;
  int32 flag;
  comp_info c_info;
  jsize length;

  /* Set chunking and possibly compression */
  /* ------------------------------------- */
  if (chunk_lengths != NULL) {

    /* Get chunk length dimensions */
    /* --------------------------- */
    length = (*env)->GetArrayLength (env, chunk_lengths);

    /* Set chunking only */
    /* ----------------- */
    if (!compressed) {
      flag = HDF_CHUNK;
      (*env)->GetIntArrayRegion (env, chunk_lengths, 0, length,
        (jint *) c_def.chunk_lengths);
    } /* if */

    /* Set chunking and compression */
    /* ---------------------------- */
    else {
      flag = HDF_CHUNK | HDF_COMP;
      (*env)->GetIntArrayRegion (env, chunk_lengths, 0, length,
        (jint *) c_def.comp.chunk_lengths);
      c_def.comp.comp_type = COMP_CODE_DEFLATE;
      c_def.comp.cinfo.deflate.level = 6;
    } /* else */

    /* Perform setchunk */
    /* ---------------- */
    ret = SDsetchunk ((int32) sdsid, c_def, flag);
    if (ret == FAIL) {
      h4raiseException (env, "SDsetchunk call failed");
      return;
    } /* if */

  } /* if */

  /* Set compression only */
  /* -------------------- */
  else if (compressed) {
    c_info.deflate.level = 6;
    ret = SDsetcompress ((int32) sdsid, COMP_CODE_DEFLATE, &c_info);
    if (ret == FAIL) {
      h4raiseException (env, "SDsetcompress call failed");
      return;
    } /* if */
  } /* else if */

} /* setChunkCompress */

/**********************************************************************/
