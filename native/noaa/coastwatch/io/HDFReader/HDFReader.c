/**********************************************************************/
/*
     FILE: HDFReader.c
  PURPOSE: To implement the native portion of HDFReader.
   AUTHOR: Peter Hollemans
     DATE: 2002/07/23
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

*/
/**********************************************************************/

/* Includes */
/* -------- */
#include "HDFReader.h"
#include "mfhdf.h"

/* Function prototypes */
/* ------------------- */
jboolean h4raiseException (JNIEnv *env, char *message);

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_HDFReader
 * Method:    getChunkLengths
 * Signature: (I)[I
 */
JNIEXPORT jintArray JNICALL Java_noaa_coastwatch_io_HDFReader_getChunkLengths
  (JNIEnv *env, jclass class, jint sdsid) {

  intn ret;
  HDF_CHUNK_DEF c_def;
  int32 flag;  
  jintArray chunk_lengths;

  /* Get the chunk information */
  /* ------------------------- */ 
  ret = SDgetchunkinfo ((int32) sdsid, &c_def, &flag);
  if (ret == FAIL) {
    h4raiseException (env, "SDgetchunkinfo call failed");
    return (NULL);
  } /* if */

  /* Get the chunk lengths */
  /* --------------------- */
  if (flag == HDF_NONE) return (NULL);
  chunk_lengths = (*env)->NewIntArray (env, (jsize) MAX_VAR_DIMS);
  (*env)->SetIntArrayRegion (env, chunk_lengths, 0, MAX_VAR_DIMS, 
    (jint *) c_def.chunk_lengths);
  return (chunk_lengths);

} /* getChunkLengths */

/**********************************************************************/
