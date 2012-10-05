/**********************************************************************/
/*
     FILE: CWF.c
  PURPOSE: To implement the native C portion for the JNI interface to 
           the CWF library.
   AUTHOR: Mark Robinson
     DATE: 2002/03/19
  CHANGES: 2002/03/29, MSR, fixed loop bug in put_variable and get_variable
           2002/05/14, PFH, modified projection class, recommented, added
             package name, modified throw_general_exception
           2002/10/16, PFH, removed non-static array size in 
             throw_general_exception
           2006/03/24, PFH, changed name from cwf.h to cwflib.h to avoid
             name conflict on Mac OS X; modified slightly to reduce warnings

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

*/
/**********************************************************************/

/* Includes */
/* -------- */
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "CWF.h"
#include "cwflib.h"
#include "cwproj.h"

/**********************************************************************/

void throw_general_exception (	/*** THROW A JAVA EXCEPTION ***/
  const char *error_message,	/* string for error messages */
  JNIEnv *env			/* pointer to the JNI environment */
) {

  const char *header = "CWF: ";
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

void throw_exception (		/*** THROW A JAVA EXCEPTION FOR CWF ERROR ***/
  int cwf_error_id,		/* the cwf error code */
  JNIEnv *env			/* pointer to the JNI environment */
) {

  const char *str_error;

  /* Setup error message */
  /* ------------------- */
  str_error = cw_strerror (cwf_error_id);

  /* Pass error to general handler */
  /* ----------------------------- */
  throw_general_exception (str_error, env);

} /*  throw_exception */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    create
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL 
  Java_noaa_coastwatch_io_CWF_create(JNIEnv *env, jclass class,
  jstring string, jint mode) {

  int error;
  int cw_id;
  const char *str;

  /* Get file name */
  /* ------------- */
  str = (*env)->GetStringUTFChars(env, string, 0);

  /* Create and record error */
  /* -------------------------- */
  error = cw_create(str, mode, &cw_id);

  /* Release the string back to the VM */
  /* --------------------------------- */
  (*env)->ReleaseStringUTFChars(env, string, str);

  /* Check for error */
  /* --------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

  return cw_id;
  
} /* Java_noaa_coastwatch_io_CWF_create */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    open
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL 
  Java_noaa_coastwatch_io_CWF_open(JNIEnv *env, jclass class, 
  jstring string, jint mode) {

  int error;
  int cw_id;
  const char *filename;

  /* Get file name */
  /* ------------- */
  filename = (*env)->GetStringUTFChars(env, string, 0);

  /* Open file and record error */
  /* -------------------------- */
  error = cw_open(filename, mode, &cw_id);

  /* Release the string back to the VM */
  /* --------------------------------- */
  (*env)->ReleaseStringUTFChars(env, string, filename);

  /* Check for error */
  /* --------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

  return cw_id;

} /* Java_noaa_coastwatch_io_CWF_open */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    enddef
 * Signature: (I)V
 */
JNIEXPORT void JNICALL 
  Java_noaa_coastwatch_io_CWF_enddef(JNIEnv *env, jclass class, 
  jint cw_id) {

  int error;

  /* Leave file definition mode */
  /* -------------------------- */
  error = cw_enddef(cw_id);

  /* Check for error */
  /* --------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

} /* Java_noaa_coastwatch_io_CWF_enddef */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    close
 * Signature: (I)V
 */
JNIEXPORT void JNICALL 
  Java_noaa_coastwatch_io_CWF_close(JNIEnv *env, jclass class, 
  jint cw_id) {

  int error;

  /* Close our file */
  /* -------------- */
  error = cw_close(cw_id);

  /* Check for error */
  /* --------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

} /* Java_noaa_coastwatch_io_CWF_close */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    define_dimension
 * Signature: (ILjava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_noaa_coastwatch_io_CWF_define_1dimension
  (JNIEnv *env, jclass class, jint cw_id, jstring dimension_name, 
  jint size) {

  int error;
  const char *dimension;
  int dimension_id;

  /* Acess Java data members */
  /* ----------------------- */
  dimension = (*env)->GetStringUTFChars(env, dimension_name, 0);

  /* Define the new dimension */
  /* ------------------------ */
  error = cw_def_dim(cw_id, dimension, size, &dimension_id);

  /* Release the string back to the VM */
  /* --------------------------------- */
  (*env)->ReleaseStringUTFChars(env, dimension_name, dimension);

  /* Check for errors */
  /* ---------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

  return dimension_id;

} /* Java_noaa_coastwatch_io_CWF_define_1dimension */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    inquire_dimension_id
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_noaa_coastwatch_io_CWF_inquire_1dimension_1id
  (JNIEnv *env, jclass class, jint cw_id, jstring dimension_name) {

  int dimension_id;
  int error;
  const char *name;

  /* Acess Java data members */
  /* ----------------------- */
  name = (*env)->GetStringUTFChars(env, dimension_name, 0);

  /* Get the dimension id */
  /* -------------------- */
  error = cw_inq_dimid(cw_id, name, &dimension_id);

  /* clean-up and return */
  /* ------------------- */
  (*env)->ReleaseStringUTFChars(env, dimension_name, name);

  /* Check for errors */
  /* ---------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

  return dimension_id;

} /* Java_noaa_coastwatch_io_CWF_inquire_1dimension_1id */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    inquire_dimension_length
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_noaa_coastwatch_io_CWF_inquire_1dimension_1length
  (JNIEnv *env, jclass class, jint cw_id, jint dimension_id) {

  int error;
  size_t length;

  /* Get the dimension id */
  /* -------------------- */
  error =  cw_inq_dim(cw_id, dimension_id, NULL, &length);

  /* Check for errors */
  /* ---------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

  return length;

} /* Java_noaa_coastwatch_io_CWF_inquire_1dimension */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    inquire_dimension_name
 * Signature: (II)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_noaa_coastwatch_io_CWF_inquire_1dimension_1name
  (JNIEnv *env, jclass class, jint cw_id, jint dimension_id) {

  int error;
  char name[CW_MAX_NAME];
  jstring string;

  /* Get the dimension id */
  /* -------------------- */
  error =  cw_inq_dim(cw_id, dimension_id, name, NULL);

  /* Check for errors */
  /* ---------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

  /* Create a new string from name */
  /* ----------------------------- */
  string = (*env)->NewStringUTF(env, name);

  return string;

} /* Java_noaa_coastwatch_io_CWF_inquire_1dimension */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    define_variable
 * Signature: (ILjava/lang/String;[I)I
 */
JNIEXPORT jint JNICALL Java_noaa_coastwatch_io_CWF_define_1variable
  (JNIEnv *env, jclass class, jint cw_id, jstring variable_name, 
  jintArray dimension_array) {

  int error;
  int variable_id;
  const char *variable;
  cw_type type;
  int array_length;
  jint *dimensions;

  /* Acess Java data members */
  /* ----------------------- */
  variable = (*env)->GetStringUTFChars(env, variable_name, 0);
  array_length = (*env)->GetArrayLength(env, dimension_array);

  /* Check to see if array is of the right size */
  /* ------------------------------------------ */
  if(array_length != 2) {
    (*env)->ReleaseStringUTFChars(env, variable_name, variable);
    throw_general_exception("Wrong number of array dimensions, should be 2", 
      env);
  } /* if */

  /* Acess Java data members */
  /* ----------------------- */
  dimensions = (*env)->GetIntArrayElements(env, dimension_array, 0);

  /* check to see if we're using cloud or graphics data */
  /* -------------------------------------------------- */
  if(strcmp(variable, "cloud") == 0)
    type = CW_BYTE;
  else if(strcmp(variable, "graphics") == 0)
    type = CW_BYTE;
  else
    type = CW_FLOAT;

  /* Define the variable id */
  /* ---------------------- */
  error = cw_def_var(cw_id, variable, type, 2, (int*)dimensions, &variable_id);

  /* Release the string back to the VM */
  /* --------------------------------- */
  (*env)->ReleaseStringUTFChars(env, variable_name, variable);
  (*env)->ReleaseIntArrayElements(env, dimension_array, dimensions, 0);

  /* Check for errors */
  /* ---------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

  return variable_id;

} /* Java_noaa_coastwatch_io_CWF_define_1variable */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    inquire_variable_id
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_noaa_coastwatch_io_CWF_inquire_1variable_1id
  (JNIEnv *env, jclass class, jint cw_id, jstring variable_name) {

  int error;
  const char *name;
  int variable_id;

  /* Acess Java data members */
  /* ----------------------- */
  name = (*env)->GetStringUTFChars(env, variable_name, 0);

  /* Get the variable id */
  /* ------------------- */
  error = cw_inq_varid(cw_id, name, &variable_id);

  /* clean-up and return */
  /* ------------------- */
  (*env)->ReleaseStringUTFChars(env, variable_name, name);

  /* Check for errors */
  /* ---------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

  return variable_id;

} /* Java_noaa_coastwatch_io_CWF_inquite_1variable_1id */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    inquire_variable_name
 * Signature: (II)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_noaa_coastwatch_io_CWF_inquire_1variable_1name
  (JNIEnv *env, jclass class, jint cw_id, jint var_id) {

  int error;
  jstring string;
  char name[30];

  error = cw_inq_var(cw_id, var_id, name, NULL, NULL, NULL, NULL);

  if(error != CW_NOERR)
    throw_exception(error, env);

  string = (*env)->NewStringUTF(env, name);

  return string;

} /* Java_noaa_coastwatch_io_CWF_inquire_1variable_1name */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    inquire_variable_type
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_noaa_coastwatch_io_CWF_inquire_1variable_1type
  (JNIEnv *env, jclass class, jint cw_id, jint var_id) {

  int error;
  cw_type type;

  /* Get the type of the variable */
  /* ---------------------------- */
  error = cw_inq_var(cw_id, var_id, NULL, &type, NULL, NULL, NULL);

  /* Check for errors */
  /* ---------------- */
  if(error !=  CW_NOERR)
    throw_exception(error, env);

  return type;
} /* Java_noaa_coastwatch_io_CWF_inquire_1variable_1type */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    inquire_variable_attributes
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL 
  Java_noaa_coastwatch_io_CWF_inquire_1variable_1attributes
  (JNIEnv *env, jclass class, jint cw_id, jint var_id) {

  int error;
  int attribute_count;

  /* Get the number of attribute for the vaiable */
  /* ------------------------------------------- */
  error = cw_inq_var(cw_id, var_id, NULL, NULL, NULL, NULL, &attribute_count);

  /* Check for errors */
  /* ---------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

  return attribute_count;

} /* Java_noaa_coastwatch_io_CWF_inquire_1variable_1attributes */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    inquire_variable_dimension_ids
 * Signature: (II)[I
 */
JNIEXPORT jintArray JNICALL 
  Java_noaa_coastwatch_io_CWF_inquire_1variable_1dimension_1ids
  (JNIEnv *env, jclass class, jint cw_id, jint var_id) {

  int error;
  int dims[2];
  jintArray array;
  int *jarray;
  int i;

  /* Get the dimensions of the variable */
  /* ---------------------------------- */
  error = cw_inq_var (cw_id, var_id, NULL, NULL, NULL, dims, NULL);

  /* Check for errors */
  /* ---------------- */
  if(error != CW_NOERR)
    throw_exception (error, env);

  /* Create an array and get a pointer to it */
  /* --------------------------------------- */
  array = (*env)->NewIntArray(env, 2);
  jarray = (int*)(*env)->GetIntArrayElements (env, array, NULL);

  /* Populate the elements in an array */
  /* --------------------------------- */
  for(i = 0; i < 2; i++)
    jarray[i] = dims[i];

  /* Release the array back to the VM */
  /* -------------------------------- */
  (*env)->ReleaseIntArrayElements(env, array, (jint*)jarray, 0);

  return array;

} /* Java_noaa_coastwatch_io_CWF_inquire_1variable_1dimension_1ids */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    put_variable
 * Signature: (II[I[I[[F)V
 */
JNIEXPORT void JNICALL Java_noaa_coastwatch_io_CWF_put_1variable__II_3I_3I_3_3F
  (JNIEnv *env, jclass class, jint cw_id, jint var_id, jintArray start_point,
  jintArray size, jobjectArray data) {

  int error;
  jint *start_j;
  jint *size_j;
  jfloat *array;
  jobject data_to_copy;
  int rows;
  int i;
  int size_array[2];
  int start_array[2];

  /* acquire the pointers to the arrays */
  /* ---------------------------------- */
  start_j = (*env)->GetIntArrayElements(env, start_point, NULL);
  size_j = (*env)->GetIntArrayElements(env, size, NULL);

  /* Copy size/start data into temporaries */
  /* ------------------------------------- */
  for(i = 0; i < 2; i++) {
    size_array[i] = size_j[i];
    start_array[i] = start_j[i];
  }

  /* Get the size of the array */
  /* ------------------------- */
  rows = size_array[0];
  size_array[0] = 1;

  for(i = 0; i < rows; i++, start_array[0]++) {

    /* Access the correct Array elements */
    /* --------------------------------- */
    data_to_copy = (*env)->GetObjectArrayElement(env, data, i);
    array = (*env)->GetFloatArrayElements(env, data_to_copy, 0);

    /* Read our data into the CWF file */
    /* ------------------------------- */
    error = cw_put_vara_float(cw_id, var_id, (size_t *)start_array, 
      (size_t *)size_array, array);

    /* Check for errors */
    /* ---------------- */
    if(error != CW_NOERR)
      throw_exception(error, env);

    /* Release our array */
    /* ----------------- */
    (*env)->ReleaseFloatArrayElements(env, data_to_copy, array, 0);
  } /* for */

  /* Clean up */
  /* -------- */
  (*env)->ReleaseIntArrayElements(env, start_point, start_j, JNI_ABORT);
  (*env)->ReleaseIntArrayElements(env, size, size_j, JNI_ABORT);

} /* Java_noaa_coastwatch_io_CWF_put_1variable__II_3I_3I_3_3F */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    put_variable
 * Signature: (II[I[I[[B)V
 */
JNIEXPORT void JNICALL Java_noaa_coastwatch_io_CWF_put_1variable__II_3I_3I_3_3B
  (JNIEnv *env, jclass class, jint cw_id, jint var_id, jintArray start_point,
  jintArray size, jobjectArray data) {

  int error;
  jint *start_j;
  jint *size_j;
  jbyte *array;
  jobject data_to_copy;
  int rows;
  int i;
  int size_array[2];
  int start_array[2];

  /* acquire the pointers to the arrays */
  /* ---------------------------------- */
  start_j = (*env)->GetIntArrayElements(env, start_point, NULL);
  size_j = (*env)->GetIntArrayElements(env, size, NULL);

  /* Copy size/start data into temporaries */
  /* ------------------------------------- */
  for(i = 0; i < 2; i++) {
    size_array[i] = size_j[i];
    start_array[i] = start_j[i];
  } /* for */

  /* Get the size of the array */
  /* ------------------------- */
  rows = size_array[0];
  size_array[0] = 1;

  for(i = 0; i < rows; i++, start_array[0]++) {

    /* Access the correct Array elements */
    /* --------------------------------- */
    data_to_copy = (*env)->GetObjectArrayElement(env, data, i);
    array = (*env)->GetByteArrayElements(env, data_to_copy, 0);

    /* Read our data into the CWF file */
    /* ------------------------------- */
    error = cw_put_vara_uchar(cw_id, var_id, (size_t *)start_array,
      (size_t *)size_array, array);

    /* Check for errors */
    /* ---------------- */
    if(error != CW_NOERR)
      throw_exception(error, env);

    /* Release our array */
    /* ----------------- */
    (*env)->ReleaseByteArrayElements(env, data_to_copy, array, 0);
  } /* for */

  /* Clean up */
  /* -------- */
  (*env)->ReleaseIntArrayElements(env, start_point, start_j, JNI_ABORT);
  (*env)->ReleaseIntArrayElements(env, size, size_j, JNI_ABORT);

} /* Java_noaa_coastwatch_io_CWF_put_1variable__II_3I_3I_3_3B */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    get_variable_float
 * Signature: (II[I[I)[[F
 */
JNIEXPORT jobjectArray JNICALL Java_noaa_coastwatch_io_CWF_get_1variable_1float
  (JNIEnv *env, jclass class, jint cw_id, jint var_id, jintArray start_point, 
  jintArray size) {

  int error;
  jint *start_j;
  jint *size_j;
  jobjectArray data_received;
  jfloatArray row_array;
  int rows, columns;
  jfloat *mem;
  int i;
  int size_array[2];
  int start_array[2];

  /* acquire the pointers to the arrays */
  /* ---------------------------------- */
  start_j = (*env)->GetIntArrayElements(env, start_point, NULL);
  size_j = (*env)->GetIntArrayElements(env, size, NULL);

  /* Copy size/start data into temporaries */
  /* ------------------------------------- */
  for(i = 0; i < 2; i++) {
    size_array[i] = size_j[i];
    start_array[i] = start_j[i];
  } /* for */

  /* Calculate how large our array has to be */
  /* --------------------------------------- */
  rows = size_array[0];
  columns = size_array[1];

  /* Create our arrays */
  /* ----------------- */
  row_array = (*env)->NewFloatArray(env, columns);
  data_received = (*env)->NewObjectArray(env, rows, 
    (*env)->GetObjectClass(env, row_array), 0);

  /* Setup to copy one row at a time */
  /* ------------------------------- */
  size_array[0] = 1;

  for(i = 0; i < rows; i++,start_array[0]++) {

    /* Acquire a pointer to the array */
    /* ------------------------------ */
    row_array = (*env)->NewFloatArray(env, columns);
    mem = (*env)->GetFloatArrayElements(env, row_array, 0);

    /* Read in one row of data */
    /* ----------------------- */
    error = cw_get_vara_float(cw_id, var_id, (size_t*)start_array, 
      (size_t*)size_array, mem);

    /* Check for errors */
    /* ---------------- */
    if(error != CW_NOERR)
      throw_exception(error, env);

    /* Release array and load it into the object array */
    /* ----------------------------------------------- */
    (*env)->ReleaseFloatArrayElements(env, row_array, mem, 0);
    (*env)->SetObjectArrayElement(env, data_received, i, row_array);

  } /* for */

  /* Clean up */
  /* -------- */
  (*env)->ReleaseIntArrayElements(env, start_point, start_j, JNI_ABORT);
  (*env)->ReleaseIntArrayElements(env, size, size_j, JNI_ABORT);

  return data_received;

} /* Java_noaa_coastwatch_io_CWF_get_1variable_1float */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    get_variable_byte
 * Signature: (II[I[I)[[B
 */
JNIEXPORT jobjectArray JNICALL Java_noaa_coastwatch_io_CWF_get_1variable_1byte
  (JNIEnv *env, jclass class, jint cw_id, jint var_id, jintArray start_point, 
  jintArray size) {

  int error;
  jint *start_j;
  jint *size_j;
  jobjectArray data_received;
  jbyteArray row_array;
  int rows, columns;
  jbyte *mem;
  int i;
  int size_array[2];
  int start_array[2];

  /* acquire the pointers to the arrays */
  /* ---------------------------------- */
  start_j = (*env)->GetIntArrayElements(env, start_point, NULL);
  size_j = (*env)->GetIntArrayElements(env, size, NULL);

  /* Copy size/start data into temporaries */
  /* ------------------------------------- */
  for(i = 0; i < 2; i++) {
    size_array[i] = size_j[i];
    start_array[i] = start_j[i];
  }

  /* Calculate how large our array has to be */
  /* --------------------------------------- */
  rows = size_array[0];
  columns = size_array[1];

  /* Create our arrays */
  /* ----------------- */
  row_array = (*env)->NewByteArray(env, columns);
  data_received = (*env)->NewObjectArray(env, rows, 
    (*env)->GetObjectClass(env, row_array), 0);

  /* Setup to copy one row at a time */
  /* ------------------------------- */
  size_array[0] = 1;

  for(i = 0; i < rows; i++,start_array[0]++) {

    /* Acquire a pointer to the array */
    /* ------------------------------ */
    row_array = (*env)->NewByteArray(env, columns);
    mem = (*env)->GetByteArrayElements(env, row_array, 0);

    /* Read in one row of data */
    /* ----------------------- */
    error = cw_get_vara_uchar(cw_id, var_id, (size_t*)start_array, 
      (size_t*)size_array, mem);

    /* Check for errors */
    /* ---------------- */
    if(error != CW_NOERR)
      throw_exception(error, env);

    /* Release array and load it into the object array */
    /* ----------------------------------------------- */
    (*env)->ReleaseByteArrayElements(env, row_array, mem, 0);
    (*env)->SetObjectArrayElement(env, data_received, i, row_array);

  } /* for */

  /* Clean up */
  /* -------- */
  (*env)->ReleaseIntArrayElements(env, start_point, start_j, JNI_ABORT);
  (*env)->ReleaseIntArrayElements(env, size, size_j, JNI_ABORT);

  return data_received;

} /* Java_noaa_coastwatch_io_CWF_get_1variable_1byte */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    inquire_attribute_name
 * Signature: (III)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_noaa_coastwatch_io_CWF_inquire_1attribute_1name
  (JNIEnv *env, jclass class, jint cw_id, jint var_id, jint attribute_id) {

  int error;
  char name[CW_MAX_NAME];
  jstring string;

  /* Get the attribute name */
  /* ---------------------- */
  error = cw_inq_attname(cw_id, var_id, attribute_id, name);

  /* Check for errors */
  /* ---------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

  /* Create a string containing the attribute name */
  /* --------------------------------------------- */
  string = (*env)->NewStringUTF(env, name);

  return string;

} /* Java_noaa_coastwatch_io_CWF_inquire_1attribute_1name */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    inquire_attribute_type
 * Signature: (IILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_noaa_coastwatch_io_CWF_inquire_1attribute_1type
  (JNIEnv *env, jclass class, jint cw_id, jint var_id, jstring attribute) {

  int error;
  const char *name;
  cw_type type;

  /* Acquire pointer to char array */
  /* ----------------------------- */
  name = (*env)->GetStringUTFChars(env, attribute, NULL);

  /* get the type of the attribute */
  /* ----------------------------- */
  error = cw_inq_att(cw_id, var_id, name, &type, NULL);

  /* Release char pointer back to JVM */
  /* -------------------------------- */
  (*env)->ReleaseStringUTFChars(env, attribute, name);

  /* Check for errors */
  /* ---------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

  return type;

} /* Java_noaa_coastwatch_io_CWF_inquire_1attribute_1type */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    inquire_attribute_num
 * Signature: (IILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_noaa_coastwatch_io_CWF_inquire_1attribute_1num
  (JNIEnv *env, jclass class, jint cw_id, jint var_id, jstring attribute) {

  int error;
  const char *name;
  size_t number;

  /* Access the attribute string */
  /* --------------------------- */
  name = (*env)->GetStringUTFChars(env, attribute, 0);

  /* Get the number of attributes */
  /* ---------------------------- */
  error = cw_inq_att(cw_id, var_id, (const char*)name, NULL, &number);

  /* Release our string to the VM */
  /* ---------------------------- */
  (*env)->ReleaseStringUTFChars(env, attribute, name);

  /* Check for errors */
  /* ---------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

  return ((jint) number);

} /* Java_noaa_coastwatch_io_CWF_inquire_1attribute_1num */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    inquire_attribute_id
 * Signature: (IILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_noaa_coastwatch_io_CWF_inquire_1attribute_1id
  (JNIEnv *env, jclass class, jint cw_id, jint var_id, jstring attribute) {

  int error;
  const char *name;
  int id;

  /* Access the attribute string */
  /* --------------------------- */
  name = (*env)->GetStringUTFChars(env, attribute, 0);

  /* Get the ID of the attribute */
  /* --------------------------- */
  error = cw_inq_attid(cw_id, var_id, (const char*)name, &id);

  /* Release our string to the VM */
  /* ---------------------------- */
  (*env)->ReleaseStringUTFChars(env, attribute, name);

  /* Check for errors */
  /* ---------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

  return id;

} /* Java_noaa_coastwatch_io_CWF_inquire_1attribute_1id */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    get_attribute_string
 * Signature: (IILjava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_noaa_coastwatch_io_CWF_get_1attribute_1string
  (JNIEnv *env, jclass class, jint cw_id, jint var_id, jstring attribute) {

  int error;
  jstring string;
  const char *att;
  char string_data[100];

  /* Access the attribute string */
  /* --------------------------- */
  att = (*env)->GetStringUTFChars(env, attribute, 0);

  /* Get the text of the attribute */
  /* ----------------------------- */
  error = cw_get_att_text(cw_id, var_id, att, string_data);

  /* Release our string to the VM */
  /* ---------------------------- */
  (*env)->ReleaseStringUTFChars(env, attribute, att);

  /* Check for errors */
  /* ---------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

  /* Create a new string from the text of the attribute */
  /* -------------------------------------------------- */
  string = (*env)->NewStringUTF(env, string_data);

  return string;

} /* Java_noaa_coastwatch_io_CWF_get_1attribute_1string */

/**********************************************************************/
/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    get_attribute_float
 * Signature: (IILjava/lang/String;)F
 */
JNIEXPORT jfloat JNICALL Java_noaa_coastwatch_io_CWF_get_1attribute_1float
  (JNIEnv *env, jclass class, jint cw_id, jint var_id, jstring attribute) {

  int error;
  float value;
  const char *att;

  /* Access the attribute string */
  /* --------------------------- */
  att = (*env)->GetStringUTFChars(env, attribute, 0);

  /* Get the attribute value */
  /* ----------------------- */
  error = cw_get_att_float(cw_id, var_id, att, &value);

  /* Release our string to the VM */
  /* ---------------------------- */
  (*env)->ReleaseStringUTFChars(env, attribute, att);

  /* Check for errors */
  /* ---------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

  return value;

} /* Java_noaa_coastwatch_io_CWF_get_1attribute_1float */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    get_attribute_short
 * Signature: (IILjava/lang/String;)S
 */
JNIEXPORT jshort JNICALL Java_noaa_coastwatch_io_CWF_get_1attribute_1short
  (JNIEnv *env, jclass class, jint cw_id, jint var_id, jstring attribute) {

  int error;
  short value;
  const char *att;

  /* Access the attribute string */
  /* --------------------------- */
  att = (const char*)(*env)->GetStringUTFChars(env, attribute, 0);

  /* Get the attribute value */
  /* ----------------------- */
  error = cw_get_att_short(cw_id, var_id, att, &value);

  /* Release our string to the VM */
  /* ---------------------------- */
  (*env)->ReleaseStringUTFChars(env, attribute, att);

  /* Check for errors */
  /* ---------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

  return value;

} /* Java_noaa_coastwatch_io_CWF_get_1attribute_1short */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    put_attribute
 * Signature: (IILjava/lang/String;S)V
 */
JNIEXPORT void JNICALL 
  Java_noaa_coastwatch_io_CWF_put_1attribute__IILjava_lang_String_2S
  (JNIEnv *env, jclass class, jint cw_id, jint var_id, jstring attribute, 
  jshort value) {

  int error;
  const char *att;

  /* Access the attribute string */
  /* --------------------------- */
  att = (*env)->GetStringUTFChars(env, attribute, 0);

  /* Get the attribute value */
  /* ----------------------- */
  error = cw_put_att_short(cw_id, var_id, att, CW_SHORT, 1, &value);

  /* Release our string to the VM */
  /* ---------------------------- */
  (*env)->ReleaseStringUTFChars(env, attribute, att);

  /* Check for errors */
  /* ---------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

} /* Java_noaa_coastwatch_io_CWF_put_1attribute__IILjava_lang_String_2S */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    put_attribute
 * Signature: (IILjava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL 
  Java_CWF_put_1attribute__IILjava_lang_String_2Ljava_lang_String_2
  (JNIEnv *env, jclass class, jint cw_id, jint var_id, jstring attribute, 
  jstring value) {

  int error;
  const char *att;
  const char *val;

  /* Access the attribute and new value strings */
  /* ------------------------------------------ */
  att = (*env)->GetStringUTFChars(env, attribute, 0);
  val = (*env)->GetStringUTFChars(env, value, 0);

  /* Get the attribute value */
  /* ----------------------- */
  error = cw_put_att_text(cw_id, var_id, att, strlen(val), val);

  /* Release our strings to the VM */
  /* ----------------------------- */
  (*env)->ReleaseStringUTFChars(env, attribute, att);
  (*env)->ReleaseStringUTFChars(env, value, val);

  /* Check for errors */
  /* ---------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

} /* Java_noaa_coastwatch_io_CWF_put_1attribute__IILjava_lang_String_2Ljava_lang_String_2 */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    put_attribute
 * Signature: (IILjava/lang/String;F)V
 */
JNIEXPORT void JNICALL 
  Java_noaa_coastwatch_io_CWF_put_1attribute__IILjava_lang_String_2F
  (JNIEnv *env, jclass class, jint cw_id, jint var_id, jstring attribute, 
  jfloat value){

  int error;
  const char *att;

  /* Access the attribute string */
  /* --------------------------- */
  att = (*env)->GetStringUTFChars(env, attribute, 0);

  /* Get the attribute value */
  /* ----------------------- */
  error = cw_put_att_float(cw_id, var_id, att, CW_FLOAT, 1, &value);

  /* Release our string to the VM */
  /* ---------------------------- */
  (*env)->ReleaseStringUTFChars(env, attribute, att);

  /* Check for errors */
  /* ---------------- */
  if(error != CW_NOERR) {
    printf("error\n");
    throw_exception(error, env);
  } /* if */

} /* Java_noaa_coastwatch_io_CWF_put_1attribute__IILjava_lang_String_2F */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    init_projection
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_noaa_coastwatch_io_CWF_init_1projection
  (JNIEnv *env, jclass class, jint cw_id) {

  int error;

  /* Initialize the project */
  /* ---------------------- */
  error = cw_init_proj(cw_id);

  /* Check for error */
  /* --------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

} /* Java_noaa_coastwatch_io_CWF_init_1projection */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    projection_info
 * Signature: ()Lnoaa/coastwatch/io/CWFProjectionInfo;
 */
JNIEXPORT jobject JNICALL Java_noaa_coastwatch_io_CWF_projection_1info
  (JNIEnv *env, jclass class) {

  int error;
  proj_info info;
  jclass cls;
  jfieldID fid;
  jobject info_object;

  /* Get the project info */
  /* -------------------- */
  error = cw_proj_info(&info);

  /* Check for error */
  /* --------------- */
  if(error != CW_NOERR)
    throw_exception(error, env);

  /* Find the right class */
  /* -------------------- */
  cls = (*env)->FindClass(env, "noaa/coastwatch/io/CWFProjectionInfo");
  if(cls == NULL)
    throw_general_exception (
      "Can't find class: noaa/coastwatch/io/CWFProjectionInfo", env);

  /* Create a new CWFProjectionInfo object */
  /* ------------------------------------- */
  info_object = (*env)->AllocObject(env, cls);
  if(info_object == NULL)
    throw_general_exception("Can't create CWFProjectionInfo object", env);

  /* Get the FieldID and set the data */
  /* -------------------------------- */
  fid = (*env)->GetFieldID(env, cls, "projection_type", "I");
  if (fid == NULL)
    throw_general_exception ("Can't get field ID", env);
  (*env)->SetIntField (env, info_object, fid, info.ptype);

  fid = (*env)->GetFieldID(env, cls, "prime_longitude", "F");
  if (fid == NULL)
    throw_general_exception ("Can't get field ID", env);
  (*env)->SetFloatField (env, info_object, fid, info.plon);

  fid = (*env)->GetFieldID(env, cls, "resolution", "F");
  if (fid == NULL)
    throw_general_exception ("Can't get field ID", env);
  (*env)->SetFloatField (env, info_object, fid, info.res);

  fid = (*env)->GetFieldID(env, cls, "hemisphere", "S");
  if (fid == NULL)
    throw_general_exception ("Can't get field ID", env);
  (*env)->SetShortField (env, info_object, fid, info.hem);

  fid = (*env)->GetFieldID(env, cls, "iOffset", "S");
  if (fid == NULL)
    throw_general_exception ("Can't get field ID", env);
  (*env)->SetShortField (env, info_object, fid, info.ioff);

  fid = (*env)->GetFieldID(env, cls, "jOffset", "S");
  if (fid == NULL)
    throw_general_exception ("Can't get field ID", env);
  (*env)->SetShortField (env, info_object, fid, info.joff);

  /* Object has copied all the data and we now return it */
  /* --------------------------------------------------- */
  return info_object;

} /* Java_noaa_coastwatch_io_CWF_projection_1info */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    get_latitiude_longitude
 * Signature: (DD)[D
 */
JNIEXPORT jdoubleArray JNICALL 
  Java_noaa_coastwatch_io_CWF_get_1latitiude_1longitude
  (JNIEnv *env, jclass class, jdouble i, jdouble j) {

  double lat, lon;
  jdoubleArray array;
  jdouble *array_access;

  /* Get the latitude, longitude */
  /* --------------------------- */
  cw_get_ll(i, j, &lat, &lon);

  /* Create and acces an array of 2 doubles */
  /* -------------------------------------- */
  array = (*env)->NewDoubleArray(env, 2);
  array_access = (*env)->GetDoubleArrayElements(env, array, 0);

  /* Set the array */
  /* ------------- */
  array_access[0] = lat;
  array_access[1] = lon;

  /* Release the array */
  /* ----------------- */
  (*env)->ReleaseDoubleArrayElements(env, array, array_access, 0);
  
  return array;

} /* Java_noaa_coastwatch_io_CWF_get_1latitiude_1longitude */

/**********************************************************************/

/*
 * Class:     noaa_coastwatch_io_CWF
 * Method:    get_pixel
 * Signature: (DD)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_noaa_coastwatch_io_CWF_get_1pixel
  (JNIEnv *env, jclass class, jdouble latitude, jdouble longitude){

  double i, j;
  jdoubleArray array;
  jdouble *array_access;

  /* Get the latitude, longitude */
  /* --------------------------- */
  cw_get_ij(&i, &j, latitude, longitude);

  /* Create and acces an array of 2 doubles */
  /* -------------------------------------- */
  array = (*env)->NewDoubleArray(env, 2);
  array_access = (*env)->GetDoubleArrayElements(env, array, 0);

  /* Set the array */
  /* ------------- */
  array_access[0] = i;
  array_access[1] = j;

  /* Release the array */
  /* ----------------- */
  (*env)->ReleaseDoubleArrayElements(env, array, array_access, 0);

  return array;

} /* Java_noaa_coastwatch_io_CWF_get_1pixel */

/**********************************************************************/
