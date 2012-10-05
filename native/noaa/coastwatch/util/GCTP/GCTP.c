/**********************************************************************/
/*
     FILE: GCTP.c
  PURPOSE: To implement the native C portion for the JNI interface to 
           the GCTP library.
   AUTHOR: Mark Robinson
     DATE: 2002/04/15
  CHANGES: 2002/05/13, PFH, added standard C includes, fixed
             compiler warnings
           2002/05/15, PFH, recommented, added package name
           2002/10/02, PFH, modified forward and inverse to error check
             after resources are released
           2002/10/16, PFH, removed non-static array size in 
             throw_general_exception

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

*/
/**********************************************************************/

/* Includes */
/* -------- */
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "GCTP.h"
#include "proj.h"

/* Globals */
/* ------- */
static long (*forward[MAXPROJ+1])();
static long (*inverse[MAXPROJ+1])();

/**********************************************************************/

void throw_general_exception (	/*** THROW A JAVA EXCEPTION ***/
  const char *error_message,	/* string for error messages */
  JNIEnv *env			/* pointer to the JNI environment */
) {

  const char *header = "GCTP: ";
  char error[256];
  jclass exception;

  /* Try to print Java exception */
  /* --------------------------- */
  if ((*env)->ExceptionOccurred (env)) {
    (*env)->ExceptionDescribe (env);
    exit (-1);
  } /* if */

  /* Throw user exception */
  /* -------------------- */
  strcpy (error, header);
  strncat (error, error_message, 256-strlen(header)-1);
  exception = (*env)->FindClass(env, "java/lang/Exception");
  if (exception == 0) {
    fprintf (stderr, "Error finding java/lang/Exception, bailing\n");
    exit (-1);
  } /* if */
  (*env)->ThrowNew(env, exception, error);

} /* throw_general_exception */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_util_GCTP
 * Method:    gctp
 * Signature: ([DII[DIIILjava/lang/String;ILjava/lang/String;II[DIILjava/lang/String;Ljava/lang/String;)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_noaa_coastwatch_util_GCTP_gctp
  (JNIEnv *env, jclass class, jdoubleArray input_coord, jint input_system,
  jint input_zone, jdoubleArray input_parameters, jint input_units, 
  jint input_datum,
  jint error_message_flag, jstring error_file, jint jpr, jstring pfile,
  jint output_system, jint output_zone, jdoubleArray output_parameters,
  jint output_unit, jint output_datum, jstring NAD1927_zonefile, 
  jstring NAD1983_zonefile) {

  jdouble *input_coord_array;
  jdouble *output_coord_array;
  jdouble *input_parameters_array;
  jdouble *output_parameters_array;
  jdoubleArray output_coord;
  char *error_file_string;
  char *pfile_string;
  char *nad_27zone;
  char *nad_83zone;
  long error;

  /* Access double array elements */
  /* ---------------------------- */
  output_coord = (*env)->NewDoubleArray (env, 2);
  input_coord_array = (*env)->GetDoubleArrayElements(env, input_coord, NULL);
  output_coord_array = (*env)->GetDoubleArrayElements (env, output_coord, 
    NULL);
  input_parameters_array = (*env)->GetDoubleArrayElements (env, 
    input_parameters, NULL);
  output_parameters_array = (*env)->GetDoubleArrayElements (env, 
    output_parameters, NULL);

  /* Access strings */
  /* -------------- */
  error_file_string = (char*)(*env)->GetStringUTFChars (env, error_file, NULL);
  pfile_string = (char*)(*env)->GetStringUTFChars (env, pfile, NULL);
  nad_27zone = (char*)(*env)->GetStringUTFChars (env, NAD1927_zonefile, NULL);
  nad_83zone = (char*)(*env)->GetStringUTFChars (env, NAD1983_zonefile, NULL);

  /* Run the function */
  /* ---------------- */
  gctp (input_coord_array, &input_system, &input_zone, input_parameters_array,
    &input_units, &input_datum, &error_message_flag, error_file_string, &jpr,
    pfile_string, output_coord_array, &output_system, &output_zone,
    output_parameters_array, &output_unit, &output_datum, nad_27zone,
    nad_83zone, &error);

  /* Release the strings */
  /* ------------------- */
  (*env)->ReleaseStringUTFChars (env, error_file, error_file_string);
  (*env)->ReleaseStringUTFChars (env, pfile, pfile_string);
  (*env)->ReleaseStringUTFChars (env, NAD1927_zonefile, nad_27zone);
  (*env)->ReleaseStringUTFChars (env, NAD1983_zonefile, nad_83zone);

  /* Release the arrays */
  /* ------------------ */
  (*env)->ReleaseDoubleArrayElements (env, input_coord, input_coord_array, 0);
  (*env)->ReleaseDoubleArrayElements (env, output_coord, 
    output_coord_array, 0);
  (*env)->ReleaseDoubleArrayElements (env, input_parameters, 
    input_parameters_array, 0);
  (*env)->ReleaseDoubleArrayElements (env, output_parameters, 
    output_parameters_array, 0);

  /* Check for errors */
  /* ---------------- */
  if (error != 0) {
    char buf[100];
    memset (buf, 0, 100);
    sprintf (buf, "error calling gctp, error number = %ld", error);
    throw_general_exception (buf, env);
  } /* if */

  /* Return created array */
  /* -------------------- */
  return output_coord;

} /* Java_noaa_coastwatch_util_GCTP_gctp */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_util_GCTP
 * Method:    init_forward
 * Signature: (II[DILjava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_noaa_coastwatch_util_GCTP_init_1forward
  (JNIEnv *env, jclass class, jint output_system,
  jint output_zone, jdoubleArray output_parameters, jint output_datum,
  jstring NAD1927_zonefile, jstring NAD1983_zonefile) {

  char *nad83;
  char *nad27;
  jdouble *output_params_array;
  long error = 0;

  /* Access complex Java types */
  /* ------------------------- */
  nad83 = (char*)(*env)->GetStringUTFChars (env, NAD1983_zonefile, NULL);
  nad27 = (char*)(*env)->GetStringUTFChars (env, NAD1927_zonefile, NULL);
  output_params_array = (*env)->GetDoubleArrayElements (env, 
    output_parameters, NULL);

  /* Init function pointers */
  /* ---------------------- */
  for_init (output_system, output_zone, output_params_array, output_datum,
    nad27, nad83, &error, forward);

  /* Release Java resources */
  /* ---------------------- */
  (*env)->ReleaseStringUTFChars (env, NAD1927_zonefile, nad27);
  (*env)->ReleaseStringUTFChars (env, NAD1983_zonefile, nad83);
  (*env)->ReleaseDoubleArrayElements (env, output_parameters,
    output_params_array, 0);

  /* Check for errors */
  /* ---------------- */
  if (error != 0) {
    char buf[100];
    memset (buf, 0, 100);
    sprintf (buf, "error calling for_init, error number = %ld", error);
    throw_general_exception (buf, env);
  } /* if */

  /* Check for NULL pointers */
  /* ----------------------- */
  if (forward[output_system] == NULL) {
    throw_general_exception ("null function pointer after for_init", env);
  } /* if */

} /* Java_noaa_coastwatch_util_GCTP_init_1forward */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_util_GCTP
 * Method:    init_inverse
 * Signature: (II[DILjava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_noaa_coastwatch_util_GCTP_init_1inverse
  (JNIEnv *env, jclass class, jint input_system, jint input_zone,
  jdoubleArray input_parameters, jint input_datum, jstring NAD1927_zonefile,
  jstring NAD1983_zonefile) {

  char *nad83;
  char *nad27;
  jdouble *input_params_array;
  long error = 0;

  /* Access complex Java Types */
  /* ------------------------- */
  nad83 = (char*)(*env)->GetStringUTFChars (env, NAD1983_zonefile, NULL);
  nad27 = (char*)(*env)->GetStringUTFChars (env, NAD1927_zonefile, NULL);
  input_params_array = (*env)->GetDoubleArrayElements (env, 
    input_parameters, NULL);

  /* Load function pointer for inverse functions */
  /* ------------------------------------------- */
  inv_init (input_system, input_zone, input_params_array, input_datum, 
    nad27, nad83,  &error, inverse);

  /* Release Java Resources */
  /* ---------------------- */
  (*env)->ReleaseStringUTFChars (env, NAD1983_zonefile, nad83);
  (*env)->ReleaseStringUTFChars (env, NAD1927_zonefile, nad27);
  (*env)->ReleaseDoubleArrayElements (env, input_parameters,
    input_params_array, 0);

  /* Check for errors */
  /* ---------------- */
  if (error != 0) {
    char buf[100];
    memset (buf, 0, 100);
    sprintf (buf, "error calling inv_init, error number = %ld", error);
    throw_general_exception (buf, env);
  } /* if */

  /* Check for NULL pointers */
  /* ----------------------- */
  if(inverse[input_system] == NULL) {
    throw_general_exception ("null function pointer after inv_init", env);
  } /* if */

} /* Java_noaa_coastwatch_util_GCTP_init_1inverse */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_util_GCTP
 * Method:    forward
 * Signature: ([DI)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_noaa_coastwatch_util_GCTP_forward
  (JNIEnv *env, jclass class, jdoubleArray pos, jint output_system) {

  double x, y;
  jdouble *pos_array;
  jdoubleArray output;
  jdouble *output_array;
  int error = 0;

  /* Access input array */
  /* ------------------ */
  pos_array = (*env)->GetDoubleArrayElements (env, pos, NULL);

  /* Check for projection sanity */
  /* --------------------------- */
  if (output_system >= MAXPROJ || output_system < 0)
    throw_general_exception ("invalid projection system code", env);

  /* Check for initialization */
  /* ------------------------ */
  if (forward[output_system] == NULL)
    throw_general_exception ("uninitialized function pointer", env);

  /* Do transformation */
  /* ----------------- */
  error = forward[output_system](pos_array[0], pos_array[1], &x, &y);

  /* Create array to return */
  /* ---------------------- */
  output = (*env)->NewDoubleArray (env, 2);
  output_array = (*env)->GetDoubleArrayElements (env, output, 0);

  /* Load array with transformed data */
  /* -------------------------------- */
  output_array[0] = x;
  output_array[1] = y;

  /* Release the array, and writeback the changes */
  /* -------------------------------------------- */
  (*env)->ReleaseDoubleArrayElements (env, output, output_array, 0);
  (*env)->ReleaseDoubleArrayElements (env, pos, pos_array, 0);

  /* Check for errors */
  /* ---------------- */
  if (error != 0) {
    throw_general_exception ("error in forward transform", env);
  } /* if */

  /* Return the array we created above */
  /* --------------------------------- */
  return output;

} /* Java_noaa_coastwatch_util_GCTP_forward */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_util_GCTP
 * Method:    inverse
 * Signature: ([DI)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_noaa_coastwatch_util_GCTP_inverse
  (JNIEnv *env, jclass class, jdoubleArray pos, jint input_system) {

  double lat, lon;
  jdouble *pos_array;
  jdoubleArray output;
  jdouble *output_array;
  int error = 0;

  /* Access array elements */
  /* --------------------- */
  pos_array = (*env)->GetDoubleArrayElements (env, pos, NULL);

  /* Check for projection type sanity */
  /* -------------------------------- */
  if (input_system >= MAXPROJ || input_system < 0)
    throw_general_exception ("invalid projection system code", env);

  /* Check for initialization */
  /* ------------------------ */
  if (inverse[input_system] == NULL)
    throw_general_exception ("uninitialized function pointer", env);

  /* Perform the projection transform */
  /* -------------------------------- */
  error = inverse[input_system](pos_array[0], pos_array[1], &lon, &lat);

  /* Create an array of 2 doubles */
  /* ---------------------------- */
  output = (*env)->NewDoubleArray (env, 2);
  output_array = (*env)->GetDoubleArrayElements (env, output, 0);

  /* Copy the data */
  /* ------------- */
  output_array[0] = lon;
  output_array[1] = lat;

  /* Release the arrays and copy back the data */
  /* ----------------------------------------- */
  (*env)->ReleaseDoubleArrayElements (env, output, output_array, 0);
  (*env)->ReleaseDoubleArrayElements (env, pos, pos_array, 0);

  /* Check for Errors */
  /* ---------------- */
  if (error != 0) {
    throw_general_exception ("error in inverse transform", env);
  } /* if */

  /* Return the created array */
  /* ------------------------ */
  return output;

} /* Java_noaa_coastwatch_util_GCTP_inverse */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_util_GCTP
 * Method:    pakr2dm
 * Signature: (D)D
 */
JNIEXPORT jdouble JNICALL Java_noaa_coastwatch_util_GCTP_pakr2dm
  (JNIEnv *env, jclass class, jdouble angle) {

  return pakr2dm (angle);

} /* Java_noaa_coastwatch_util_GCTP_pakr2dm */

/**********************************************************************/
