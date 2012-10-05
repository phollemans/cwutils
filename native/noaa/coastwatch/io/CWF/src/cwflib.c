/**********************************************************************/
/*
     FILE: cwflib.c
  PURPOSE: To define the CoastWatch format (cwf) library interface and
           internal routines.  The library is based on a combination
           of an original set of IMGMAP data access routines and the
           netCDF library.
   AUTHOR: Peter Hollemans
     DATE: 01/01/1998
  CHANGES: 23/06/1998, PFH, changed compress/uncompress functions to
             work on a compressed header length of 1024 bytes (uncompressed
             headers are always 2*pixel_size)
           30/06/1998, PFH, fixed graphics plane bug (plane #8 was always on)
           20/10/1998, PFH, increased the open file limit to 100
           07/11/1998, PFH, fixed file close bug in cw_uncompress
           06/01/1999, PFH, added NOAA-15, -16, -17 codes
           30/01/1999, PFH, added f to various float constants
           08/02/1999, PFH, added test for near-zero IR values
	   28/02/1999, PFH, simplified round function
	   02/03/1999, PFH, fixed bug in cw_compress for files with 
             cols < 512
           04/03/1999, PFH, added calibration guess for older WCRN files
           07/11/1999, PFH, fixed bug in near-zero IR test
           2000/11/01, PFH, added orbit_type "both", 
             channel_number "sst_multi"
           2001/01/11, PFH, added support for flat file reading, and
             reformatted
           2001/08/02, PFH, added cw_clone stub
	   2001/11/09, PFH, fixed -Wall warnings
           2006/03/24, PFH, renamed so that it could be compiled on Mac
             OS X (cwf.h and CWF.h are indistinguishable!); added 
             HAS_ROUNDF define

  CoastWatch Format (CWF) Software Library and Utilities
  Copyright 1998-2001, USDOC/NOAA/NESDIS CoastWatch

*/
/**********************************************************************/

/* Includes */
/* -------- */
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <stdlib.h>
#include "cwflib.h"

/* Defines */
/* ------- */
#define CW_ATT_NUM				57
#define CW_SATELLITE_ID				0
#define CW_SATELLITE_TYPE			1
#define CW_DATA_SET_TYPE			2
#define CW_PROJECTION_TYPE			3
#define CW_START_LATITUDE			4
#define CW_END_LATITUDE				5
#define CW_START_LONGITUDE			6
#define CW_END_LONGITUDE			7
#define CW_RESOLUTION				8
#define CW_POLAR_GRID_SIZE			9
#define CW_POLAR_GRID_POINTS			10
#define CW_POLAR_HEMISPHERE			11
#define CW_POLAR_PRIME_LONGITUDE		12
#define CW_GRID_IOFFSET				13
#define CW_GRID_JOFFSET				14
#define CW_COMPOSITE_TYPE			15
#define CW_CALIBRATION_TYPE			16
#define CW_FILL_TYPE				17
#define CW_CHANNEL_NUMBER			18
#define CW_DATA_ID				19
#define CW_SUN_NORMALIZATION			20
#define CW_LIMB_CORRECTION			21
#define CW_NONLINEARITY_CORRECTION		22
#define CW_ORBITS_PROCESSED			23
#define CW_CHANNELS_PRODUCED			24
#define CW_CHANNEL_PIXEL_SIZE			25
#define CW_CHANNEL_START_BLOCK			26
#define CW_CHANNEL_END_BLOCK			27
#define CW_ANCILLARIES_PRODUCED			28
#define CW_ANCILLARY_PIXEL_SIZE			29
#define CW_ANCILLARY_START_BLOCK		30
#define CW_ANCILLARY_END_BLOCK			31
#define CW_IMAGE_BLOCK_SIZE			32
#define CW_COMPRESSION_TYPE			33
#define CW_PERCENT_NON_ZERO			34
#define CW_HORIZONTAL_SHIFT			35
#define CW_VERTICAL_SHIFT			36
#define CW_HORIZONTAL_SKEW			36
#define CW_VERTICAL_SKEW			38
#define CW_ORBIT_TYPE				39
#define CW_ORBIT_TIME				40
#define CW_START_ROW				41
#define CW_START_COLUMN				42
#define CW_END_ROW				43
#define CW_END_COLUMN				44
#define CW_ORBIT_START_YEAR			45
#define CW_ORBIT_START_DAY			46
#define CW_ORBIT_START_MONTH_DAY		47
#define CW_ORBIT_START_HOUR_MINUTE		48
#define CW_ORBIT_START_SECOND			49
#define CW_ORBIT_START_MILLISECOND		50
#define CW_ORBIT_END_YEAR			51
#define CW_ORBIT_END_DAY			52
#define CW_ORBIT_END_MONTH_DAY			53
#define CW_ORBIT_END_HOUR_MINUTE		54
#define CW_ORBIT_END_SECOND			55
#define CW_ORBIT_END_MILLISECOND		56

#define CW_O_SATELLITE_ID			0
#define CW_O_SATELLITE_TYPE			2
#define CW_O_DATA_SET_TYPE			4
#define CW_O_PROJECTION_TYPE			6
#define CW_O_START_LATITUDE			8
#define CW_O_END_LATITUDE			10
#define CW_O_START_LONGITUDE			12
#define CW_O_END_LONGITUDE			14
#define CW_O_RESOLUTION				16
#define CW_O_POLAR_GRID_SIZE			22
#define CW_O_POLAR_GRID_POINTS			24
#define CW_O_POLAR_HEMISPHERE			26
#define CW_O_POLAR_PRIME_LONGITUDE		28
#define CW_O_GRID_IOFFSET			30
#define CW_O_GRID_JOFFSET			32
#define CW_O_COMPOSITE_TYPE			42
#define CW_O_CALIBRATION_TYPE			44
#define CW_O_FILL_TYPE				46
#define CW_O_CHANNEL_NUMBER			48
#define CW_O_DATA_ID				50
#define CW_O_SUN_NORMALIZATION			52
#define CW_O_LIMB_CORRECTION			54
#define CW_O_NONLINEARITY_CORRECTION		56
#define CW_O_ORBITS_PROCESSED			58
#define CW_O_CHANNELS_PRODUCED			60
#define CW_O_CHANNEL_PIXEL_SIZE			62
#define CW_O_CHANNEL_START_BLOCK		64
#define CW_O_CHANNEL_END_BLOCK			66
#define CW_O_ANCILLARIES_PRODUCED		68
#define CW_O_ANCILLARY_PIXEL_SIZE		70
#define CW_O_ANCILLARY_START_BLOCK		72
#define CW_O_ANCILLARY_END_BLOCK		74
#define CW_O_IMAGE_BLOCK_SIZE			76
#define CW_O_COMPRESSION_TYPE			78
#define CW_O_PERCENT_NON_ZERO			82
#define CW_O_HORIZONTAL_SHIFT			84
#define CW_O_VERTICAL_SHIFT			86
#define CW_O_HORIZONTAL_SKEW			88
#define CW_O_VERTICAL_SKEW			90
#define CW_O_ORBIT_TYPE				100
#define CW_O_ORBIT_TIME				102
#define CW_O_START_ROW				104
#define CW_O_START_COLUMN			106
#define CW_O_END_ROW				108
#define CW_O_END_COLUMN				110
#define CW_O_ORBIT_START_YEAR			112
#define CW_O_ORBIT_START_DAY			114
#define CW_O_ORBIT_START_MONTH_DAY		116
#define CW_O_ORBIT_START_HOUR_MINUTE		118
#define CW_O_ORBIT_START_SECOND			120
#define CW_O_ORBIT_START_MILLISECOND		122
#define CW_O_ORBIT_END_YEAR			124
#define CW_O_ORBIT_END_DAY			126
#define CW_O_ORBIT_END_MONTH_DAY		128
#define CW_O_ORBIT_END_HOUR_MINUTE		130
#define CW_O_ORBIT_END_SECOND			132
#define CW_O_ORBIT_END_MILLISECOND		134

#define CW_SATELLITE_ID_NOAA_6			-10815
#define CW_SATELLITE_ID_NOAA_7			-10813
#define CW_SATELLITE_ID_NOAA_8			-10811
#define CW_SATELLITE_ID_NOAA_9			-10810
#define CW_SATELLITE_ID_NOAA_10			-10809
#define CW_SATELLITE_ID_NOAA_11			-10808
#define CW_SATELLITE_ID_NOAA_12			-10812
#define CW_SATELLITE_ID_NOAA_14			-10799
#define CW_SATELLITE_ID_NOAA_15			-10798
#define CW_SATELLITE_ID_NOAA_16			-10797
#define CW_SATELLITE_ID_NOAA_17			-10796
#define CW_SATELLITE_TYPE_MORNING		0
#define CW_SATELLITE_TYPE_AFTERNOON		1
#define CW_DATA_SET_TYPE_LAC			1
#define CW_DATA_SET_TYPE_GAC			2
#define CW_DATA_SET_TYPE_HRPT			3
#define CW_PROJECTION_TYPE_UNMAPPED		0
#define CW_PROJECTION_TYPE_MERCATOR		1
#define CW_PROJECTION_TYPE_POLAR		2
#define CW_PROJECTION_TYPE_LINEAR		3
#define CW_COMPOSITE_TYPE_NONE			0
#define CW_COMPOSITE_TYPE_NADIR			1
#define CW_COMPOSITE_TYPE_AVERAGE		2
#define CW_COMPOSITE_TYPE_LATEST		3
#define CW_COMPOSITE_TYPE_WARMEST		4
#define CW_COMPOSITE_TYPE_COLDEST		5
#define CW_CALIBRATION_TYPE_RAW			0
#define CW_CALIBRATION_TYPE_ALBEDO_TEMPERATURE	2
#define CW_FILL_TYPE_NONE			0
#define CW_FILL_TYPE_AVERAGE			1
#define CW_FILL_TYPE_ADJACENT			2
#define CW_CHANNEL_NUMBER_AVHRR1		1
#define CW_CHANNEL_NUMBER_AVHRR2		2
#define CW_CHANNEL_NUMBER_AVHRR3		3
#define CW_CHANNEL_NUMBER_AVHRR4		4
#define CW_CHANNEL_NUMBER_AVHRR5		5
#define CW_CHANNEL_NUMBER_MCSST			6
#define CW_CHANNEL_NUMBER_SCAN_ANGLE		101
#define CW_CHANNEL_NUMBER_SAT_ZENITH		102
#define CW_CHANNEL_NUMBER_SOL_ZENITH		103
#define CW_CHANNEL_NUMBER_REL_AZIMUTH		104
#define CW_CHANNEL_NUMBER_SCAN_TIME		105
#define CW_CHANNEL_NUMBER_MCSST_SPLIT		201
#define CW_CHANNEL_NUMBER_MCSST_DUAL		202
#define CW_CHANNEL_NUMBER_MCSST_TRIPLE		203
#define CW_CHANNEL_NUMBER_CPSST_SPLIT		204
#define CW_CHANNEL_NUMBER_CPSST_DUAL		205
#define CW_CHANNEL_NUMBER_CPSST_TRIPLE		206
#define CW_CHANNEL_NUMBER_NLSST_SPLIT		207
#define CW_CHANNEL_NUMBER_NLSST_DUAL		208
#define CW_CHANNEL_NUMBER_NLSST_TRIPLE		209
#define CW_CHANNEL_NUMBER_SST_MULTI		210
#define CW_CHANNEL_NUMBER_OCEAN_REFLECT		301
#define CW_CHANNEL_NUMBER_TURBIDITY		302
#define CW_CHANNEL_NUMBER_CLOUD			401
#define CW_DATA_ID_VISIBLE			0
#define CW_DATA_ID_IR				1
#define CW_DATA_ID_ANCILLARY			2
#define CW_DATA_ID_CLOUD			3
#define CW_SUN_NORMALIZATION_NO			0
#define CW_SUN_NORMALIZATION_YES		1
#define CW_LIMB_CORRECTION_NO			0
#define CW_LIMB_CORRECTION_YES			1
#define CW_NONLINEARITY_CORRECTION_NO		0
#define CW_NONLINEARITY_CORRECTION_YES		1
#define CW_COMPRESSION_TYPE_NONE		0
#define CW_COMPRESSION_TYPE_FLAT		1
#define CW_COMPRESSION_TYPE_1B			2
#define CW_ORBIT_TYPE_ASCENDING			-1
#define CW_ORBIT_TYPE_DESCENDING		1
#define CW_ORBIT_TYPE_BOTH			2
#define CW_ORBIT_TIME_DAY			0
#define CW_ORBIT_TIME_NIGHT			1
#define CW_ORBIT_TIME_BOTH			2

#define CW_LATLON_SCALE				128
#define CW_RESOLUTION_SCALE			100

#define CW_ATT_RW				0
#define CW_ATT_RO				1

#define CW_DIM_NUM				2
#define CW_ROWS					0
#define CW_COLUMNS				1

#define CW_DATA					0
#define CW_GRAPHICS				1

#define CW_O_ROWS				34
#define CW_O_COLUMNS				36

#define ZEROC					273.15f
#define KTOC(a)					((a) - ZEROC)

#define CW_MAX_FILES				100
#define CW_HEAD_MIN				136
#define CW_HEAD_COMP				1024
#define CW_MAGIC_NUM				0xd5

#define CW_FALSE				0
#define CW_TRUE					!CW_FALSE

#define CW_ERR_NUM				51
#define CW_ERR_CREATE				1
#define CW_ERR_CREATE_MODE			2
#define CW_ERR_ACCESS				3
#define CW_ERR_ACCESS_MODE			4
#define CW_ERR_NOT_DEFINE_MODE			5
#define CW_ERR_DATASET_ID			6
#define CW_ERR_ENDDEF_FAILED			7
#define CW_ERR_DIM_DEFINED			8
#define CW_ERR_DIM_LT0				9
#define CW_ERR_DIM				10
#define CW_ERR_VAR_DEFINED			11
#define CW_ERR_DATA_TYPE			12
#define CW_ERR_DIM_NUM				13
#define CW_ERR_DIM_ID				14
#define CW_ERR_VAR				15
#define CW_ERR_VAR_ID				16
#define CW_ERR_VAR_INDEX			17
#define CW_ERR_VAR_VALUE			18
#define CW_ERR_DEFINE_MODE			19
#define CW_ERR_ATT				20
#define CW_ERR_ATT_VALUE			21
#define CW_ERR_NOMEM				22
#define CW_ERR_MAX_FILES			23
#define CW_ERR_CREATE_EXISTS			24
#define CW_ERR_CREATE_HEADER			25
#define CW_ERR_MAGIC				26
#define CW_ERR_MAGIC_READ			27
#define CW_ERR_UNKNOWN				28
#define CW_ERR_ATT_ID				29
#define CW_ERR_READ_DIM				30
#define CW_ERR_READ_ATT				31
#define CW_ERR_READ_DATA			32
#define CW_ERR_WRITE_DIM			33
#define CW_ERR_WRITE_ATT			34
#define CW_ERR_WRITE_DATA			35
#define CW_ERR_DIM_UNDEFINED			36
#define CW_ERR_VAR_UNDEFINED			37
#define CW_ERR_INTERNAL				38
#define CW_ERR_UNSUP_DATA_ID			39
#define CW_ERR_UNSUP_CHANNEL_NUMBER		40
#define CW_ERR_DATASET_RO			41
#define CW_ERR_ATT_TYPE				42
#define CW_ERR_UNSUP_PIXEL_SIZE			43
#define CW_ERR_UNSUP_CALIBRATION_TYPE		44
#define CW_ERR_CFILE				45
#define CW_ERR_UFILE				46
#define CW_ERR_UNSUP_COMPRESSION_TYPE		47
#define CW_ERR_COM_BYTE0			48
#define CW_ERR_ATT_LEN				49
#define CW_ERR_WRITE_SHIFT			50
#define CW_ERR_ATT_RO				51

#define BUFSIZE					512

#ifdef __SUNPRO_C
  FILE *tmpfile (void);
#endif

/* Macros */
/* ------ */
#define CW_UNC_RVAL(a)		((short) (((a) & 0x7FF0) >> 4) * ((a) & 0x8000 ? -1 : 1))
#define CW_UNC_RGRA(a)		((unsigned char) ((a) & 0x000F))
#define CW_UNC_W(a,b)		((unsigned short) abs ((a)) << 4 | ((a) < 0 ? 0x8000 : 0x0000) | ((unsigned short) (b) & 0x000F))

#define CW_COM_ISMVAL(a)	((a) & 0x80)
#define CW_COM_RMVAL(a,b)	((((short) ((a) & 0x0F) << 8) + (short) (b)) * ((a) & 0x08 ? -1 : 1))
#define CW_COM_RVAL(l,a)	((l) + ((short) ((a) & 0x3F) * ((a) & 0x40 ? -1 : 1)))
#define CW_COM_RGRA(a)		(a)
#define CW_COM_WVAL_B1(a)	((unsigned char) ((unsigned short) abs ((a)) >> 8) | ((a) < 0 ? 0x08 : 0x00) | 0x80)
#define CW_COM_WVAL_B2(a)	((unsigned char) ((unsigned short) abs ((a)) & 0x00FF))
#define CW_COM_WVAL(a)		(((unsigned char) abs ((a)) | ((a) < 0 ? 0x40 : 0x00)) & 0x7F)
#define CW_COM_WGRA_B1(a)	(a)
#define CW_COM_WGRA_B2(a)	((unsigned char) (a))

#define MIN(a,b)		((a) < (b) ? (a) : (b))
#define MAX(a,b)		((a) > (b) ? (a) : (b))

/* Types */
/* ----- */
typedef struct {
  char *code_name;
  short code;
} cw_att_code_type;

typedef struct {
  char *att_name;
  short att_offset;
  short att_code_num;
  cw_att_code_type *att_code;
  short att_mode;
  short att_scale;
  short att_type;
} cw_att_type;

typedef struct {
  char *dim_name;
  short dim_offset;
} cw_dim_type;

typedef struct {
  FILE *fp, *ufp;
  char *path;
  short defmode;
  short wmode;
  short data_id;
  short graphics;
  short dims[CW_DIM_NUM];
  short pixel_size;
} cw_file_type;

/* Globals */
/* ------- */
static cw_file_type *cw_files[CW_MAX_FILES];

static int cw_byteswap = -1;

static char *cw_error_table[] = {
  "no error",
  "cannot create dataset",
  "invalid creation mode",
  "cannot access dataset",
  "invalid access mode",
  "dataset not in define mode",
  "invalid dataset id",
  "call to cw_enddef failed",
  "dimension already defined",
  "dimension must be greater than 0",
  "invalid dimension",
  "variable already defined (only 1 allowed)",
  "invalid data type",
  "invalid number of dimensions",
  "invalid dimension id",
  "invalid variable",
  "invalid variable id",
  "variable index is out of range",
  "variable value is out of range", 
  "dataset in define mode",
  "invalid attribute",
  "invalid attribute value",
  "failed to allocate memory",
  "maximum open file limit reached",
  "cannot create, dataset exists",
  "header creation failed",
  "wrong magic number, unrecognized format",
  "cannot read magic number",
  "unknown error",
  "invalid attribute id",
  "error reading dimension",
  "error reading attribute",
  "error reading data",
  "error writing dimension",
  "error writing attribute",
  "error writing data",
  "dimension must be defined",
  "data variable must be defined",
  "internal consistency error",
  "unsupported data id in header",
  "unsupported channel number in header",
  "dataset opened read-only",
  "attribute type mismatch",
  "unsupported pixel size in header",
  "unsupported calibration type in header",
  "error manipulating uncompressed file",
  "error manipulating compressed file",
  "unsupported compression type in header",
  "error in compressed file, byte 0",
  "invalid attribute length",
  "cannot write data to file with non-zero navigational shifts",
  "attribute is read-only"
};
  
static cw_att_code_type satellite_id[] = {
  {"noaa-6", CW_SATELLITE_ID_NOAA_6},
  {"noaa-7", CW_SATELLITE_ID_NOAA_7},
  {"noaa-8", CW_SATELLITE_ID_NOAA_8},
  {"noaa-9", CW_SATELLITE_ID_NOAA_9},
  {"noaa-10", CW_SATELLITE_ID_NOAA_10},
  {"noaa-11", CW_SATELLITE_ID_NOAA_11},
  {"noaa-12", CW_SATELLITE_ID_NOAA_12},
  {"noaa-14", CW_SATELLITE_ID_NOAA_14},
  {"noaa-15", CW_SATELLITE_ID_NOAA_15},
  {"noaa-16", CW_SATELLITE_ID_NOAA_16},
  {"noaa-17", CW_SATELLITE_ID_NOAA_17}
};

static cw_att_code_type satellite_type[] = {
  {"morning", CW_SATELLITE_TYPE_MORNING},
  {"afternoon", CW_SATELLITE_TYPE_AFTERNOON}
};

static cw_att_code_type data_set_type[] = {
  {"lac", CW_DATA_SET_TYPE_LAC},
  {"gac", CW_DATA_SET_TYPE_GAC},
  {"hrpt", CW_DATA_SET_TYPE_HRPT}
};

static cw_att_code_type projection_type[] = {
  {"unmapped", CW_PROJECTION_TYPE_UNMAPPED},
  {"mercator", CW_PROJECTION_TYPE_MERCATOR},
  {"polar", CW_PROJECTION_TYPE_POLAR},
  {"linear", CW_PROJECTION_TYPE_LINEAR}
};

static cw_att_code_type composite_type[] = {
  {"none", CW_COMPOSITE_TYPE_NONE},
  {"nadir", CW_COMPOSITE_TYPE_NADIR},
  {"average", CW_COMPOSITE_TYPE_AVERAGE},
  {"latest", CW_COMPOSITE_TYPE_LATEST},
  {"warmest", CW_COMPOSITE_TYPE_WARMEST},
  {"coldest", CW_COMPOSITE_TYPE_COLDEST}
};

static cw_att_code_type calibration_type[] = {
  {"raw", CW_CALIBRATION_TYPE_RAW},
  {"albedo_temperature", CW_CALIBRATION_TYPE_ALBEDO_TEMPERATURE}
};

static cw_att_code_type fill_type[] = {
  {"none", CW_FILL_TYPE_NONE},
  {"average", CW_FILL_TYPE_AVERAGE},
  {"adjacent", CW_FILL_TYPE_ADJACENT}
};

static cw_att_code_type channel_number[] = {
  {"avhrr_ch1", CW_CHANNEL_NUMBER_AVHRR1},
  {"avhrr_ch2", CW_CHANNEL_NUMBER_AVHRR2},
  {"avhrr_ch3", CW_CHANNEL_NUMBER_AVHRR3},
  {"avhrr_ch4", CW_CHANNEL_NUMBER_AVHRR4},
  {"avhrr_ch5", CW_CHANNEL_NUMBER_AVHRR5},
  {"mcsst", CW_CHANNEL_NUMBER_MCSST},
  {"scan_angle", CW_CHANNEL_NUMBER_SCAN_ANGLE},
  {"sat_zenith", CW_CHANNEL_NUMBER_SAT_ZENITH},
  {"solar_zenith", CW_CHANNEL_NUMBER_SOL_ZENITH},
  {"rel_azimuth", CW_CHANNEL_NUMBER_REL_AZIMUTH},
  {"scan_time", CW_CHANNEL_NUMBER_SCAN_TIME},
  {"mcsst_split", CW_CHANNEL_NUMBER_MCSST_SPLIT},
  {"mcsst_dual", CW_CHANNEL_NUMBER_MCSST_DUAL},
  {"mcsst_triple", CW_CHANNEL_NUMBER_MCSST_TRIPLE},
  {"cpsst_split", CW_CHANNEL_NUMBER_CPSST_SPLIT}, 
  {"cpsst_dual", CW_CHANNEL_NUMBER_CPSST_DUAL},
  {"cpsst_triple", CW_CHANNEL_NUMBER_CPSST_TRIPLE},
  {"nlsst_split", CW_CHANNEL_NUMBER_NLSST_SPLIT},
  {"nlsst_dual", CW_CHANNEL_NUMBER_NLSST_DUAL},
  {"nlsst_triple", CW_CHANNEL_NUMBER_NLSST_TRIPLE},
  {"sst_multi", CW_CHANNEL_NUMBER_SST_MULTI},
  {"ocean_reflect", CW_CHANNEL_NUMBER_OCEAN_REFLECT},
  {"turbidity", CW_CHANNEL_NUMBER_TURBIDITY},
  {"cloud", CW_CHANNEL_NUMBER_CLOUD}
};

static cw_att_code_type data_id[] = {
  {"visible", CW_DATA_ID_VISIBLE},
  {"infrared", CW_DATA_ID_IR},
  {"ancillary", CW_DATA_ID_ANCILLARY},
  {"cloud", CW_DATA_ID_CLOUD},
};

static cw_att_code_type sun_normalization[] = {
  {"no", CW_SUN_NORMALIZATION_NO},
  {"yes", CW_SUN_NORMALIZATION_YES}
};

static cw_att_code_type limb_correction[] = {
  {"no", CW_LIMB_CORRECTION_NO},
  {"yes", CW_LIMB_CORRECTION_YES}
};

static cw_att_code_type nonlinearity_correction[] = {
  {"no", CW_NONLINEARITY_CORRECTION_NO},
  {"yes", CW_NONLINEARITY_CORRECTION_YES}
};

static cw_att_code_type compression_type[] = {
  {"none", CW_COMPRESSION_TYPE_NONE},
  {"flat", CW_COMPRESSION_TYPE_FLAT},
  {"1b", CW_COMPRESSION_TYPE_1B}
};

static cw_att_code_type orbit_type[] = {
  {"ascending", CW_ORBIT_TYPE_ASCENDING},
  {"descending", CW_ORBIT_TYPE_DESCENDING},
  {"both", CW_ORBIT_TYPE_BOTH}
};

static cw_att_code_type orbit_time[] = {
  {"day", CW_ORBIT_TIME_DAY},
  {"night", CW_ORBIT_TIME_NIGHT},
  {"both", CW_ORBIT_TIME_BOTH}
};

static cw_att_type cw_attributes[] = {
  {"satellite_id", CW_O_SATELLITE_ID, 11, satellite_id, 
    CW_ATT_RW, 0, CW_CHAR},
  {"satellite_type", CW_O_SATELLITE_TYPE, 2, satellite_type, 
    CW_ATT_RW, 0, CW_CHAR},
  {"data_set_type", CW_O_DATA_SET_TYPE, 3, data_set_type, 
    CW_ATT_RW, 0, CW_CHAR},
  {"projection_type", CW_O_PROJECTION_TYPE, 4, projection_type, 
    CW_ATT_RW, 0, CW_CHAR},
  {"start_latitude", CW_O_START_LATITUDE, 0, NULL, 
    CW_ATT_RW, CW_LATLON_SCALE, CW_FLOAT},
  {"end_latitude", CW_O_END_LATITUDE, 0, NULL, 
    CW_ATT_RW, CW_LATLON_SCALE, CW_FLOAT},
  {"start_longitude", CW_O_START_LONGITUDE, 0, NULL, 
    CW_ATT_RW, CW_LATLON_SCALE, CW_FLOAT},
  {"end_longitude", CW_O_END_LONGITUDE, 0, NULL, 
    CW_ATT_RW, CW_LATLON_SCALE, CW_FLOAT},
  {"resolution", CW_O_RESOLUTION, 0, NULL, 
    CW_ATT_RW, CW_RESOLUTION_SCALE, CW_FLOAT},
  {"polar_grid_size", CW_O_POLAR_GRID_SIZE, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"polar_grid_points", CW_O_POLAR_GRID_POINTS, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"polar_hemisphere", CW_O_POLAR_HEMISPHERE, 0, NULL,
    CW_ATT_RW, 0, CW_SHORT},
  {"polar_prime_longitude", CW_O_POLAR_PRIME_LONGITUDE, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"grid_ioffset", CW_O_GRID_IOFFSET, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"grid_joffset", CW_O_GRID_JOFFSET,0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"composite_type", CW_O_COMPOSITE_TYPE, 6, composite_type, 
    CW_ATT_RW, 0, CW_CHAR},
  {"calibration_type", CW_O_CALIBRATION_TYPE, 2, calibration_type, 
    CW_ATT_RO, 0, CW_CHAR},
  {"fill_type", CW_O_FILL_TYPE, 3, fill_type, 
    CW_ATT_RW, 0, CW_CHAR},
  {"channel_number", CW_O_CHANNEL_NUMBER, 24, channel_number, 
    CW_ATT_RO, 0, CW_CHAR},
  {"data_id", CW_O_DATA_ID, 4, data_id, 
    CW_ATT_RO, 0, CW_CHAR},
  {"sun_normalization", CW_O_SUN_NORMALIZATION, 2, sun_normalization, 
    CW_ATT_RW, 0, CW_CHAR},
  {"limb_correction", CW_O_LIMB_CORRECTION, 2, limb_correction, 
    CW_ATT_RW, 0, CW_CHAR},
  {"nonlinearity_correction", CW_O_NONLINEARITY_CORRECTION, 2,
    nonlinearity_correction, CW_ATT_RW, 0, CW_CHAR},
  {"orbits_processed", CW_O_ORBITS_PROCESSED, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"channels_produced", CW_O_CHANNELS_PRODUCED, 0, NULL, 
    CW_ATT_RO, 0, CW_SHORT},
  {"channel_pixel_size", CW_O_CHANNEL_PIXEL_SIZE, 0, NULL, 
    CW_ATT_RO, 0, CW_SHORT},
  {"channel_start_block", CW_O_CHANNEL_START_BLOCK, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"channel_end_block", CW_O_CHANNEL_END_BLOCK, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"ancillaries_produced", CW_O_ANCILLARIES_PRODUCED, 0, NULL, 
    CW_ATT_RO, 0, CW_SHORT},
  {"ancillary_pixel_size", CW_O_ANCILLARY_PIXEL_SIZE, 0, NULL, 
    CW_ATT_RO, 0, CW_SHORT},
  {"ancillary_start_block", CW_O_ANCILLARY_START_BLOCK, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"ancillary_end_block", CW_O_ANCILLARY_END_BLOCK, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"image_block_size", CW_O_IMAGE_BLOCK_SIZE, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"compression_type", CW_O_COMPRESSION_TYPE, 3, compression_type, 
    CW_ATT_RO, 0, CW_CHAR},
  {"percent_non_zero", CW_O_PERCENT_NON_ZERO, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"horizontal_shift", CW_O_HORIZONTAL_SHIFT, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"vertical_shift", CW_O_VERTICAL_SHIFT, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"horizontal_skew", CW_O_HORIZONTAL_SKEW, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"vertical_skew", CW_O_VERTICAL_SKEW, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"orbit_type", CW_O_ORBIT_TYPE, 3, orbit_type, 
    CW_ATT_RW, 0, CW_CHAR},
  {"orbit_time", CW_O_ORBIT_TIME, 3, orbit_time, 
    CW_ATT_RW, 0, CW_CHAR},
  {"start_row", CW_O_START_ROW, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"start_column", CW_O_START_COLUMN, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"end_row", CW_O_END_ROW, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"end_column", CW_O_END_COLUMN, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"orbit_start_year", CW_O_ORBIT_START_YEAR, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"orbit_start_day", CW_O_ORBIT_START_DAY, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"orbit_start_month_day", CW_O_ORBIT_START_MONTH_DAY, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"orbit_start_hour_minute", CW_O_ORBIT_START_HOUR_MINUTE, 0, NULL,
    CW_ATT_RW, 0, CW_SHORT},
  {"orbit_start_second", CW_O_ORBIT_START_SECOND, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"orbit_start_millisecond", CW_O_ORBIT_START_MILLISECOND, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"orbit_end_year", CW_O_ORBIT_END_YEAR, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"orbit_end_day", CW_O_ORBIT_END_DAY, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"orbit_end_month_day", CW_O_ORBIT_END_MONTH_DAY, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"orbit_end_hour_minute", CW_O_ORBIT_END_HOUR_MINUTE, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"orbit_end_second", CW_O_ORBIT_END_SECOND, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
  {"orbit_end_millisecond", CW_O_ORBIT_END_MILLISECOND, 0, NULL, 
    CW_ATT_RW, 0, CW_SHORT},
};

static cw_dim_type cw_dimensions[] = {
  {"rows", CW_O_ROWS},
  {"columns", CW_O_COLUMNS}
};

/**********************************************************************/

static int byte_order (		/*** DETERMINE BYTE ORDER ***/
  void
) {				/* return: 0 = little endian, 1 = big
				   endian, 2 = inconclusive */

  union {
    long l;
    char c[sizeof (long)];
  } test;

  test.l = 1L;
  if (test.c[0] == 1)
    return (0);
  else if (test.c[sizeof(long) - 1] == 1)
    return (1);
  else
   return (2);

} /* byte_order */

/**********************************************************************/

static void byte_swap_in (	/*** PERFORM IN-SITU BYTE SWAPPING ***/
  void *bytes,			/* byte data (modified) */
  size_t n			/* number of bytes */
) {

  size_t i;
  unsigned char *b = (unsigned char *) bytes;
  unsigned char tmp;

  for (i = 0; i < n; i += 2) {
    tmp = b[i];
    b[i] = b[i+1];
    b[i+1] = tmp;
  } /* for */    

} /* byte_swap_in */

/**********************************************************************/

static void byte_swap (		/*** PERFORM BYTE SWAPPING ***/
  void *sbytes,			/* swapped bytes (modified) */
  const void *bytes,		/* byte data */
  size_t n			/* number of bytes */
) {

  size_t i;
  unsigned char *b = (unsigned char *) bytes;
  unsigned char *sb = (unsigned char *) sbytes;

  for (i = 0; i < n; i += 2) {
    sb[i] = b[i+1];
    sb[i+1] = b[i];
  } /* while */    

} /* byte_swap */

/**********************************************************************/

static int cpyfile (		/*** COPY FILE ***/
  FILE *dst,			/* destination file */
  FILE *src			/* source file */
) {				/* return: 0 on success, <0 on failure */

  size_t num_read;
  unsigned char buf[BUFSIZE];

  /* Rewind files */
  /* ------------ */
  rewind (dst);
  rewind (src);

  /* Read and write loop */
  /* ------------------- */
  num_read = fread ((void *) buf, 1, BUFSIZE, src);
  while (num_read != 0) {
    if (fwrite ((void *) buf, 1, num_read, dst) < num_read)
      return (-1);
    num_read = fread ((void *) buf, 1, BUFSIZE, src);
  } /* while */
  return (0);

} /* cpyfile */

/**********************************************************************/

static int cw_put_att (		/*** PUT ATTRIBUTE TO FILE ***/
  FILE *fp,			/* file pointer */
  short att_code,		/* attribute code */
  short att_offset		/* attribute offset */
) {				/* return: 0 on success, <0 on failure */

  /* Swap bytes */
  /* ---------- */
  if (cw_byteswap)
    byte_swap_in ((void *) &att_code, 2);

  /* Position file and write */
  /* ----------------------- */
  if (fseek (fp, (long) att_offset, SEEK_SET) != 0)
    return (-1);
  if (fwrite ((void *) &att_code, 2, 1, fp) < 1)
    return (-2);
  return (0);

} /* cw_put_att */

/**********************************************************************/

static int cw_get_att (		/*** GET ATTRIBUTE FROM FILE ***/
  FILE *fp,			/* file pointer */
  short *att_code,		/* attribute code */
  short att_offset		/* attribute offset */
) {				/* return: 0 on success, <0 on failure */

  /* Position file and read */
  /* ---------------------- */
  if (fseek (fp, (long) att_offset, SEEK_SET) != 0)
    return (-1);
  if (fread ((void *) att_code, 2, 1, fp) < 1)
    return (-2);

  /* Swap bytes */
  /* ---------- */
  if (cw_byteswap)
    byte_swap_in ((void *) att_code, 2);
  return (0);

} /* cw_get_att */

/**********************************************************************/

static int cw_lookup_attid (	/*** LOOKUP ATTRIBUTE ID ***/
  int *attidp,			/* attribute id (modified) */
  const char *name		/* attribute name */
) {				/* return: 0 if found, <0 if not */

  int i;

  /* Search for attribute id */
  /* ----------------------- */
  for (i = 0; i < CW_ATT_NUM; i++) {
    if (strcmp (name, cw_attributes[i].att_name) == 0) {
      *attidp = i;
      return (0);
    } /* if */
  } /* for */
 
  /* Search failed */
  /* ------------- */
  return (-1);

} /* cw_lookup_attid */

/**********************************************************************/

static int cw_lookup_att_code (	/*** LOOKUP ATTRIBUTE CODE ***/
  short *att_codep,		/* attribute code (modified) */
  const char *name,		/* attribute code name */
  int attid			/* attribute id */
) {				/* return: 0 if found, <0 if not */

  int i;

  /* Search for attribute code */
  /* ------------------------- */
  for (i = 0; i < cw_attributes[attid].att_code_num; i++) {
    if (strcmp (name, cw_attributes[attid].att_code[i].code_name) == 0) {
      *att_codep = cw_attributes[attid].att_code[i].code;
      return (0);
    } /* if */
  } /* for */

  /* Search failed */
  /* ------------- */
  return (-1);

} /* cw_lookup_att_code */

/**********************************************************************/

static int 
cw_lookup_att_code_name (	/*** LOOKUP ATTRIBUTE CODE NAME ***/
  char *name,			/* attribute name (modified) */
  short att_code,		/* attribute code */
  int attid			/* attribute id */
) {				/* return: 0 if found, <0 if not */

  int i;

  /* Search for attribute code name */
  /* ------------------------------ */
  for (i = 0; i < cw_attributes[attid].att_code_num; i++) {
    if (att_code == cw_attributes[attid].att_code[i].code) {
      strcpy (name, cw_attributes[attid].att_code[i].code_name);
      return (0);
    } /* if */
  } /* for */

  /* Search failed */
  /* ------------- */
  return (-1);

} /* cw_lookup_att_code */

/**********************************************************************/

static int cw_lookup_dimid (	/*** LOOKUP DIMENSION ID ***/
  int *dimidp,			/* dimension id (modified) */
  const char *name		/* dimension name */
) {				/* return: 0 if found, <0 if not */

  int i;

  /* Search for dimension id */
  /* ----------------------- */
  for (i = 0; i < CW_DIM_NUM; i++) {
    if (strcmp (name, cw_dimensions[i].dim_name) == 0) {
      *dimidp = i;
      return (0);
    } /* if */
  } /* for */

  /* Search failed */
  /* ------------- */
  return (-1);

} /* cw_lookup_dimid */

/**********************************************************************/

#ifndef HAS_ROUNDF

static float roundf (float f) {

/* Rounds a float to the nearest integer. */

  return ((float) (f > 0 ? floor (f+0.5) : ceil (f-0.5)));

} /* roundf */

#endif

/**********************************************************************/

static int cw_get_raw (		/*** GET RAW DATA FROM FILE ***/
  FILE *fp,			/* file pointer */
  void *raw,			/* raw data (modified) */
  short pixel_size,		/* pixel size */
  const short dims[],		/* file dimensions */
  const size_t start[],		/* start indices */
  const size_t count[],		/* dimension extents */
  const short shift[]		/* row/column data shifts */
) {				/* return: CW_NOERR on success */

  size_t row, rowend, cols;
  size_t len, headlen, ucpstep;
  long offset, rowstep, colstep, readstep;
  unsigned char *ucp;
  short tmp_start[2], tmp_end[2];
  size_t s_start[2], s_count[2], ucpoffset, s_ucp[2];
  int i;

  /* Zero-fill raw data */
  /* ------------------ */
  len = count[CW_ROWS]*count[CW_COLUMNS]*pixel_size;
  memset ((char *) raw, 0, len);

  /* Calculaate shift */
  /* ---------------- */
  if (shift != NULL) {
    for (i = 0; i < CW_DIM_NUM; i++) {
      tmp_start[i] = (short) start[i] - shift[i];
      tmp_end[i] = tmp_start[i] + (short) count[i] - 1;
      if (tmp_start[i] > dims[i]-1 || tmp_end[i] < 0)
        return (CW_NOERR);
      s_ucp[i] = abs (MIN (0, tmp_start[i]));
      tmp_start[i] = MAX (0, MIN (tmp_start[i], dims[i]));
      s_start[i] = tmp_start[i];
      tmp_end[i] = MAX (0, MIN (tmp_end[i], dims[i]-1));
      s_count[i] = tmp_end[i] - tmp_start[i] + 1;
    } /* for */
    ucpoffset = (s_ucp[CW_ROWS]*count[CW_COLUMNS] + 
      s_ucp[CW_COLUMNS])*pixel_size;
  } /* if */
  else {
    for (i = 0; i < CW_DIM_NUM; i++) {
      s_start[i] = start[i];
      s_count[i] = count[i];
    } /* for */
    ucpoffset = 0;
  } /* else */    

  /* Set up ucp */
  /* ---------- */
  ucp = (unsigned char *) raw + ucpoffset;
  ucpstep = count[CW_COLUMNS]*pixel_size;
  
  /* Position file */
  /* ------------- */
  cols = dims[CW_COLUMNS];
  headlen = rowstep = cols*pixel_size;
  colstep = pixel_size;
  offset = (long) headlen +
    s_start[CW_ROWS]*rowstep + 
    s_start[CW_COLUMNS]*colstep;
  if (fseek (fp, offset, SEEK_SET) != 0)
    return (CW_ERR_READ_DATA);
  readstep = rowstep - s_count[CW_COLUMNS]*colstep;

  /* Read raw bytes */
  /* -------------- */
  rowend = s_start[CW_ROWS] + s_count[CW_ROWS];
  for (row = s_start[CW_ROWS]; row < rowend; row++) {
    if (fread ((void *) ucp, pixel_size, s_count[CW_COLUMNS], fp) < 
      s_count[CW_COLUMNS])
      return (CW_ERR_READ_DATA);
    ucp += ucpstep;
    if (row != rowend-1 && fseek (fp, readstep, SEEK_CUR) != 0)
      return (CW_ERR_READ_DATA);
  } /* for */		       

  /* Swap bytes */
  /* ---------- */
  if (cw_byteswap && pixel_size == 2)
    byte_swap_in (raw, len);
  return (CW_NOERR);

} /* cw_get_raw */

/**********************************************************************/

static void cw_cal_raw (	/*** CALIBRATE TO RAW COUNTS ***/
  float *fp,			/* float data (modified) */
  const short *sp,		/* short data */
  size_t len,			/* number of data values */
  short compression		/* compression type */
) {

  size_t i;

  /* Calibrate flat file values */
  /* -------------------------- */
  if (compression == CW_COMPRESSION_TYPE_FLAT) {
    for (i = 0; i < len; i++) {
      if (sp[i] < 0 || sp[i] > 1023)
        fp[i] = CW_BADVAL;
      else
        fp[i] = sp[i];
    } /* for */
  } /* if */

  /* Calibrate non-flat file values */
  /* ------------------------------ */
  else {
    for (i = 0; i < len; i++) {
      if (sp[i] < 1 || sp[i] > 1024)
        fp[i] = CW_BADVAL;
      else
        fp[i] = sp[i] - 1.0f;
    } /* for */
  } /* else */


} /* cw_cal_raw */

/**********************************************************************/

static void cw_cal_visible (	/*** CALIBRATE TO VISIBLE ALBEDO ***/
  float *fp,			/* float data (modified) */
  const short *sp,		/* short data */
  size_t len,			/* number of data values */
  short compression		/* compression type */
) {

  size_t i;

  /* Calibrate flat file values */
  /* -------------------------- */
  if (compression == CW_COMPRESSION_TYPE_FLAT) {
    for (i = 0; i < len; i++) {
      if (sp[i] < 0 || sp[i] > 10000)
        fp[i] = CW_BADVAL;
      else
        fp[i] = sp[i]/100.0f;
    } /* for */
  } /* if */

  /* Calibrate non-flat file values */
  /* ------------------------------ */
  else {
    for (i = 0; i < len; i++) {
      if (sp[i] < 1 || sp[i] > 2047)
        fp[i] = CW_BADVAL;
      else
        fp[i] = (sp[i] - 1)/20.47f;
    } /* for */
  } /* else */

} /* cw_cal_visible */

/**********************************************************************/

static void cw_cal_ir (		/*** CALIBRATE TO IR TEMPERATURE ***/
  float *fp,			/* float data (modified) */
  const short *sp,		/* short data */
  size_t len,			/* number of data values */
  short compression,		/* compression type */
  short channel			/* channel number */
) {

  size_t i;

  /* Calibrate flat file values */
  /* -------------------------- */
  if (compression == CW_COMPRESSION_TYPE_FLAT) {
    if (channel <= CW_CHANNEL_NUMBER_AVHRR5) {
      for (i = 0; i < len; i++) {
        if (sp[i] < 0 || sp[i] > 32760)
          fp[i] = CW_BADVAL;
        else
          fp[i] = KTOC (sp[i]/100.0f);
      } /* for */
    } /* if */
    else {
      for (i = 0; i < len; i++) {
        if (sp[i] < -4000 || sp[i] > 4000)
          fp[i] = CW_BADVAL;
        else
          fp[i] = sp[i]/100.0f;
      } /* for */
    } /* else */
  } /* if */

  /* Calibrate non-flat file values */
  /* ------------------------------ */
  else {
    for (i = 0; i < len; i++) {
      if (sp[i] < 1 || sp[i] > 2047)
        fp[i] = CW_BADVAL;
      else if (sp[i] == 1)
        fp[i] = KTOC (178.0f);
      else if (sp[i] < 921)
        fp[i] = (sp[i] - 1)*0.1f + KTOC (178.0f);
      else if (sp[i] <= 1721) {
        fp[i] = (sp[i] - 921)*0.05f + KTOC (270.0f);
        if (fabs (fp[i] - 0.0f) < 0.01f)
          fp[i] = 0.0f;
      } /* else if */
      else
        fp[i] = (sp[i] - 1721)*0.1f + KTOC (310.0f);
    } /* for */
  } /* else */

} /* cw_cal_ir */

/**********************************************************************/

static void 
cw_separate_channel (		/*** SEPARATE CHANNEL DATA AND GRAPHICS ***/
  short *sp,			/* short data (modified) */
  unsigned char *ucp,		/* byte data (modified) */
  const unsigned short *usp,	/* unsigned short data */
  size_t len			/* number of data values */
) {

  size_t i;

  /* Separate data */
  /* ------------- */
  if (sp != NULL)
    for (i = 0; i < len; i++)
      sp[i] = CW_UNC_RVAL (usp[i]);

  /* Separate graphics */
  /* ----------------- */
  if (ucp != NULL)
    for (i = 0; i < len; i++)
      ucp[i] = CW_UNC_RGRA (usp[i]);

} /* cw_separate_channel */

/**********************************************************************/

static int 
cw_decode_channel (		/*** DECODE CHANNEL DATA ***/
  float *fp,			/* float data (modified) */
  unsigned char *ucp,		/* byte data (modified) */
  const unsigned short *usp,	/* unsigned short data */
  size_t len,			/* number of data values */
  short calibration,		/* calibration type */
  short data_id,		/* data id */
  short compression,		/* compression type */	
  short channel			/* channel */
) {				/* return: CW_NOERR on success */

  short *sp = NULL;

  /* Check for data decode */
  /* --------------------- */
  if (fp != NULL) {

    /* Seperate channel data */
    /* --------------------- */
    if (compression != CW_COMPRESSION_TYPE_FLAT) {
      if ((sp = (short *) malloc (len*sizeof (short))) == NULL)
        return (CW_ERR_NOMEM);
      cw_separate_channel (sp, NULL, usp, len);
    } /* if */
    else
      sp = (short *) usp;

    /* Guess calibration type */
    /* ---------------------- */
    if (calibration != CW_CALIBRATION_TYPE_RAW &&
      calibration != CW_CALIBRATION_TYPE_ALBEDO_TEMPERATURE &&
      (data_id == CW_DATA_ID_VISIBLE ||
      data_id == CW_DATA_ID_IR))
      calibration = CW_CALIBRATION_TYPE_ALBEDO_TEMPERATURE;

    /* Calibrate data */
    /* -------------- */
    switch (calibration) {
    case CW_CALIBRATION_TYPE_RAW:
      cw_cal_raw (fp, sp, len, compression);
      break;
    case CW_CALIBRATION_TYPE_ALBEDO_TEMPERATURE:
      switch (data_id) {
      case CW_DATA_ID_VISIBLE:
        cw_cal_visible (fp, sp, len, compression);
        break;
      case CW_DATA_ID_IR:
        cw_cal_ir (fp, sp, len, compression, channel);
        break;
      default:
        if (compression != CW_COMPRESSION_TYPE_FLAT) free ((void *) sp);
        return (CW_ERR_UNSUP_DATA_ID);
      } /* switch */
      break;
    default:
      if (compression != CW_COMPRESSION_TYPE_FLAT) free ((void *) sp);
      return (CW_ERR_UNSUP_CALIBRATION_TYPE);
    } /* switch */
    if (compression != CW_COMPRESSION_TYPE_FLAT) free ((void *) sp);

  } /* if */

  /* Check for graphics decode */
  /* ------------------------- */
  if (ucp != NULL && compression != CW_COMPRESSION_TYPE_FLAT)
    cw_separate_channel (NULL, ucp, usp, len);
  return (CW_NOERR);

} /* cw_decode_channel */

/**********************************************************************/

static int 
cw_decode_ancillary (		/*** DECODE ANCILLARY DATA ***/
  float *fp,			/* float data (modified) */
  const unsigned short *usp,	/* unsigned short data */
  size_t len,			/* number of data values */
  short channel,		/* channel number */
  short compression		/* compression type */
) {				/* return: CW_NOERR on success */

  size_t i;
  short hours, minutes;

  /* Check for data decode */
  /* --------------------- */
  if (fp != NULL) {

    switch (channel) {

    /* Calibrate angle data */
    /* -------------------- */
    case CW_CHANNEL_NUMBER_SCAN_ANGLE:
    case CW_CHANNEL_NUMBER_SAT_ZENITH:
    case CW_CHANNEL_NUMBER_SOL_ZENITH:
    case CW_CHANNEL_NUMBER_REL_AZIMUTH:

      /* Flat file encoding */
      /* ------------------ */
      if (compression == CW_COMPRESSION_TYPE_FLAT) {
        for (i = 0; i < len; i++)
          fp[i] = usp[i]/100.0f;
      } /* if */

      /* Non-flat file encoding */
      /* ---------------------- */
      else {     
        for (i = 0; i < len; i++)
          if (usp[i] == 0)
            fp[i] = CW_BADVAL;
          else
            fp[i] = (usp[i] - 1)/128.0f;
      } /* else */
      break;

    /* Calibrate time data */
    /* ------------------- */
    case CW_CHANNEL_NUMBER_SCAN_TIME:
      for (i = 0; i < len; i++) {
        hours = (short) usp[i]/100;
        minutes = (short) usp[i] - hours;
        fp[i] = hours + (float) minutes/60;
      } /* for */
      break;

    default:
      return (CW_ERR_UNSUP_CHANNEL_NUMBER);

    } /* switch */

  } /* if */
  return (CW_NOERR);

} /* cw_decode_ancillary */

/**********************************************************************/

static int cw_cast_frombyte (	/*** CAST BYTE DATA TO OTHER TYPES ***/
  void *data,			/* destination data (modified) */
  const unsigned char *byte,	/* byte data */
  cw_type type,			/* destination type */
  size_t len			/* number of data values */
) {				/* return: CW_NOERR on success */

  unsigned char *data_uchar;
  float *data_float;
  size_t i;
  int ret;

  switch (type) {

  /* Cast to byte */
  /* ------------ */
  case CW_BYTE:
    data_uchar = (unsigned char *) data;
    for (i = 0; i < len; i++)
      data_uchar[i] = byte[i];
    ret = CW_NOERR;
    break;

  /* Cast to float */
  /* ------------- */
  case CW_FLOAT:
    data_float = (float *) data;
    for (i = 0; i < len; i++)
      data_float[i] = byte[i];
    ret = CW_NOERR;
    break;

  default:
    ret = CW_ERR_VAR_VALUE;

  } /* switch */          

  return (ret);

} /* cw_cast_frombyte */

/**********************************************************************/

static int cw_compress (	/*** COMPRESS CW FILE (1B) ***/
  int cwid			/* CW file id */
) {				/* return: CW_NOERR on success */

/* NOTE: Assumes that the id is valid, that there is valid 2-pixel
   channel data to be compressed, and that the header indicates
   compression is on. */

  FILE *ufp, *cfp;
  unsigned char *head;
  size_t start[2], count[2], rows, columns, i, j, k;
  void *raw;
  short *data, lastd, bnum;
  unsigned char *graphics;
  unsigned char bytes[2];
  short *dims;
  size_t headlen, cheadlen, difflen;
  unsigned char zero = 0;

  /* Create new file */
  /* --------------- */
  ufp = cw_files[cwid]->ufp;
  if ((cfp = fopen (cw_files[cwid]->path, "wb")) == NULL)    
    return (CW_ERR_CFILE);

  /* Copy header */
  /* ----------- */
  dims = cw_files[cwid]->dims;
  rows = (size_t) dims[CW_ROWS];
  columns = (size_t) dims[CW_COLUMNS];
  headlen = columns*2;
  if ((head = (unsigned char *) malloc (headlen)) == NULL)
    return (CW_ERR_NOMEM);
  if (fseek (ufp, 0L, SEEK_SET) != 0)
    return (CW_ERR_READ_DATA);
  if (fread ((void *) head, 1, headlen, ufp) < headlen)
    return (CW_ERR_READ_DATA);
  if (fwrite ((void *) head, 1, headlen, cfp) < headlen)
    return (CW_ERR_WRITE_DATA);  
  free ((void *) head);

  /* Correct header size */
  /* ------------------- */
  cheadlen = CW_HEAD_COMP;
  if (headlen > cheadlen) {
    if (fseek (cfp, (long) cheadlen, SEEK_SET) != 0)
      return (CW_ERR_WRITE_DATA);
  } /* if */
  else if (headlen < cheadlen) {
    difflen = cheadlen - headlen;
    for (i = 0; i < difflen; i++)    
      if (fwrite ((void *) &zero, 1, 1, cfp) < 1)
        return (CW_ERR_WRITE_DATA);  
  } /* else */

  /* Allocate data memory */
  /* -------------------- */
  if ((raw = malloc (2*columns)) == NULL)
    return (CW_ERR_NOMEM);
  if ((data = (short *) malloc (2*columns)) == NULL)
    return (CW_ERR_NOMEM);

  /* Data read/write loop */
  /* -------------------- */
  count[CW_ROWS] = 1;
  count[CW_COLUMNS] = columns;
  start[CW_COLUMNS] = 0;
  for (i = 0; i < rows; i++) {
    start[CW_ROWS] = i;

    /* Read raw data */
    /* ------------- */
    if (cw_get_raw (ufp, raw, 2, dims, start, count, NULL) != CW_NOERR) 
      return (CW_ERR_READ_DATA);
    cw_separate_channel (data, NULL, (unsigned short *) raw, columns);

    /* Write compressed data */
    /* --------------------- */
    for (j = 0; j < columns; j++) {
      if ((i == 0 && j == 0) || abs (data[j] - lastd) > 63) {
        bytes[0] = CW_COM_WVAL_B1 (data[j]);
        bytes[1] = CW_COM_WVAL_B2 (data[j]);
        bnum = 2;
      } /* if */
      else {
        bytes[0] = CW_COM_WVAL (data[j] - lastd);
        bnum = 1;
      } /* else */
      if (fwrite ((void *) bytes, 1, bnum, cfp) < bnum) 
        return (CW_ERR_WRITE_DATA);
      lastd = data[j];
    } /* for */

  } /* for */
  free ((void *) data);   
 
  /* Allocate graphics memory */
  /* ------------------------ */
  if ((graphics = (unsigned char *) malloc (columns)) == NULL)
    return (CW_ERR_NOMEM);

  /* Graphics read/write loop */
  /* ------------------------ */
  for (i = 0; i < rows; i++) {
    start[CW_ROWS] = i;

    /* Read raw data */
    /* ------------- */
    if (cw_get_raw (ufp, raw, 2, dims, start, count, NULL) != CW_NOERR) 
      return (CW_ERR_READ_DATA);
    cw_separate_channel (NULL, graphics, (unsigned short *) raw, columns);

    /* Write compressed graphics */
    /* ------------------------- */
    for (j = 0; j < columns; j++) {
      for (k = 0; j < columns-1 && graphics[j] == graphics[j+1] &&
        k < 255; j++, k++)
      ;
      bytes[0] = CW_COM_WGRA_B1 (graphics[j]);
      bytes[1] = CW_COM_WGRA_B2 (k);
      if (fwrite ((void *) bytes, 1, 2, cfp) < 2) 
        return (CW_ERR_WRITE_DATA);
    } /* for */

  } /* for */
  free ((void *) graphics);   

  /* Close file and free memory */
  /* -------------------------- */
  if (fclose (ufp) != 0)
    return (CW_ERR_UFILE);
  cw_files[cwid]->fp = cfp;
  cw_files[cwid]->ufp = NULL;  
  free ((void *) raw);
  return (CW_NOERR);

} /* cw_compress */

/**********************************************************************/

static void 
cw_combine_channel (		/*** COMBINE CHANNEL DATA AND GRAPHICS ***/
  const short *sp,		/* channel data */
  const unsigned char *ucp,	/* graphics data */
  unsigned short *usp,		/* combined data and graphics */
  size_t len			/* number of data values */
) {

  size_t i;

  /* Combine zero data and graphics */
  /* ------------------------------ */
  if (sp == NULL)
    for (i = 0; i < len; i++)
      usp[i] = CW_UNC_W (0, ucp[i]);

  /* Combine data and zero graphics */
  /* ------------------------------ */
  else if (ucp == NULL)
    for (i = 0; i < len; i++)
      usp[i] = CW_UNC_W (sp[i], 0);

  /* Combine data and graphics */
  /* ------------------------- */
  else
    for (i = 0; i < len; i++)
      usp[i] = CW_UNC_W (sp[i], ucp[i]);

} /* cw_combine_channel */

/**********************************************************************/

static int cw_put_raw (		/*** PUT RAW DATA TO FILE ***/
  FILE *fp,			/* file pointer */
  const void *raw,		/* raw data */
  short pixel_size,		/* pixel size */
  const short dims[],		/* file dimensions */
  const size_t start[],		/* start indices */
  const size_t count[]		/* dimension extents */
) {				/* return: CW_NOERR on success */

  size_t row, rowend, cols;
  size_t len, headlen;
  size_t ucpstep;
  long offset, rowstep, colstep, writestep;
  unsigned char *ucp, *ucp_orig;

  /* Swap bytes */
  /* ---------- */
  if (cw_byteswap && pixel_size == 2) {
    len = count[CW_ROWS] * count[CW_COLUMNS];
    if ((ucp = (unsigned char *) malloc (len*pixel_size)) == NULL)
      return (CW_ERR_NOMEM);
    byte_swap (ucp, raw, len*pixel_size);
    ucp_orig = ucp;    
  } /* if */
  else 
    ucp = (unsigned char *) raw;
  ucpstep = count[CW_COLUMNS]*pixel_size;

  /* Position file */
  /* ------------- */
  cols = dims[CW_COLUMNS];
  headlen = rowstep = cols*pixel_size;
  colstep = pixel_size;
  offset = (long) headlen +
    start[CW_ROWS]*rowstep + 
    start[CW_COLUMNS]*colstep;
  if (fseek (fp, offset, SEEK_SET) != 0)
    return (CW_ERR_WRITE_DATA);
  writestep = rowstep - count[CW_COLUMNS]*colstep;

  /* Write raw bytes */
  /* --------------- */
  rowend = start[CW_ROWS] + count[CW_ROWS];
  for (row = start[CW_ROWS]; row < rowend; row++) {
    if (fwrite ((void *) ucp, pixel_size, count[CW_COLUMNS], fp) < 
      count[CW_COLUMNS])
      return (CW_ERR_WRITE_DATA);
    ucp += ucpstep;
    if (row != rowend-1 && fseek (fp, writestep, SEEK_CUR) != 0)
      return (CW_ERR_WRITE_DATA);
  } /* for */		       

  /* Free memory */
  /* ----------- */
  if (cw_byteswap && pixel_size == 2)
    free ((void *) ucp_orig);
  return (CW_NOERR);

} /* cw_put_raw */

/**********************************************************************/

static int cw_uncompress (	/*** COMPRESS CW FILE (1B) ***/
  int cwid			/* CW file id */
) {				/* return: CW_NOERR on success */

/* NOTE: Assumes that the id is valid, that there is valid 2-pixel
   channel data to be uncompressed, and that the header indicates
   compression is on. */
 
  FILE *ufp, *cfp;
  unsigned char *head;
  size_t start[2], count[2], rows, columns, i, j, k;
  void *raw;
  short *data;
  short lastd;
  unsigned char *graphics;
  unsigned char bytes[2];
  short *dims;
  unsigned char lastg;
  size_t spill;
  size_t headlen, uheadlen, difflen;
  unsigned char zero = 0;

  /* Create new file */
  /* --------------- */  
  cfp = cw_files[cwid]->fp;
  if ((ufp = tmpfile ()) == NULL)
    return (CW_ERR_UFILE);

  /* Copy header */
  /* ----------- */
  dims = cw_files[cwid]->dims;
  rows = (size_t) dims[CW_ROWS];
  columns = (size_t) dims[CW_COLUMNS];
  headlen = CW_HEAD_COMP;
  if ((head = (unsigned char *) malloc (headlen)) == NULL)
    return (CW_ERR_NOMEM);
  if (fseek (cfp, 0L, SEEK_SET) != 0)
    return (CW_ERR_READ_DATA);
  if (fread ((void *) head, 1, headlen, cfp) < headlen)
    return (CW_ERR_READ_DATA);
  if (fwrite ((void *) head, 1, headlen, ufp) < headlen)
    return (CW_ERR_WRITE_DATA);  

  /* Correct header size */
  /* ------------------- */
  uheadlen = columns*2;
  if (headlen < uheadlen) {
    difflen = uheadlen - headlen;
    for (i = 0; i < difflen; i++)    
      if (fwrite ((void *) &zero, 1, 1, ufp) < 1)
        return (CW_ERR_WRITE_DATA);  
  } /* else */

  /* Allocate data memory */
  /* -------------------- */
  if ((raw = malloc (2*columns)) == NULL)
    return (CW_ERR_NOMEM);
  if ((data = (short *) malloc (2*columns)) == NULL)
    return (CW_ERR_NOMEM);

  /* Data read/write loop */
  /* -------------------- */
  count[CW_ROWS] = 1;
  count[CW_COLUMNS] = columns;
  start[CW_COLUMNS] = 0;
  for (i = 0; i < rows; i++) {
    start[CW_ROWS] = i;

    /* Read compressed data */
    /* -------------------- */
    for (j = 0; j < columns; j++) {
      if (fread ((void *) bytes, 1, 1, cfp) < 1)
        return (CW_ERR_READ_DATA);
      if (CW_COM_ISMVAL (bytes[0])) {
        if (fread ((void *) (bytes+1), 1, 1, cfp) < 1)
          return (CW_ERR_READ_DATA);
        data[j] = CW_COM_RMVAL (bytes[0], bytes[1]);          
      } /* if */
      else if (i == 0 && j == 0)
        return (CW_ERR_COM_BYTE0);
      else
        data[j] = CW_COM_RVAL (lastd, bytes[0]);
      lastd = data[j];
    } /* for */

    /* Write raw data */
    /* -------------- */
    cw_combine_channel (data, NULL, (unsigned short *) raw, columns);
    if (cw_put_raw (ufp, raw, 2, dims, start, count) != CW_NOERR)
      return (CW_ERR_WRITE_DATA);

  } /* for */

  /* Allocate graphics memory */
  /* ------------------------ */
  if ((graphics = (unsigned char *) malloc (columns)) == NULL)
    return (CW_ERR_NOMEM);
  
  /* Graphics read/write loop */
  /* ------------------------ */
  spill = 0;
  for (i = 0; i < rows; i++) {
    start[CW_ROWS] = i;

    /* Get spilled graphics */
    /* -------------------- */
    for (j = 0; j < spill; j++)
      graphics[j] = lastg;

    /* Read compressed graphics */
    /* ------------------------ */
    for (; j < columns; j++) {
      if (fread ((void *) bytes, 1, 2, cfp) < 2)
        return (CW_ERR_READ_DATA);
      for (k = 0; k <= bytes[1] && j < columns; j++, k++)
        graphics[j] = CW_COM_RGRA (bytes[0]);
      j--;
    } /* for */
    lastg = graphics[j-1];
    spill = bytes[1]-k+1;

    /* Reread raw data */
    /* --------------- */
    if (cw_get_raw (ufp, raw, 2, dims, start, count, NULL) != CW_NOERR) 
      return (CW_ERR_READ_DATA);
    cw_separate_channel (data, NULL, (unsigned short *) raw, columns);

    /* Rewrite data and graphics */
    /* ------------------------- */
    cw_combine_channel (data, graphics, (unsigned short *) raw, columns);
    if (cw_put_raw (ufp, raw, 2, dims, start, count) != CW_NOERR)
      return (CW_ERR_WRITE_DATA);

  } /* for */

  /* Close file and free memory */
  /* -------------------------- */
  free ((void *) data);
  free ((void *) graphics);   
  free ((void *) raw);
  fclose (cfp);
  cw_files[cwid]->fp = ufp;
  cw_files[cwid]->ufp = ufp;
  return (CW_NOERR);
  
} /* cw_uncompress */

/**********************************************************************/

static int cw_get_vara (	/*** GET ARRAY OF DATA VALUES ***/
  int cwid,			/* CW file id */
  int varid,			/* variable id */
  const size_t start[],		/* start indices */
  const size_t count[],		/* dimension extents */
  void *data,			/* destination data (modified) */
  cw_type type			/* destination type */
) {				/* return: CW_NOERR on success */

  short *dims;
  short pixel_size;
  int ret;
  FILE *fp;
  void *raw = NULL;
  size_t len;
  short data_id, channel, calibration, compression;
  short shift[2];
  unsigned char *ucp = NULL;

  /* Check file id and mode */
  /* ---------------------- */
  if (cw_files[cwid] == NULL)
    return (CW_ERR_DATASET_ID);
  if (cw_files[cwid]->defmode == CW_TRUE)
    return (CW_ERR_DEFINE_MODE);

  /* Check start and extents */
  /* ----------------------- */
  dims = cw_files[cwid]->dims;
  if (start[CW_ROWS] > dims[CW_ROWS]-1 ||
    start[CW_COLUMNS] > dims[CW_COLUMNS]-1 ||
    start[CW_ROWS]+count[CW_ROWS]-1 > dims[CW_ROWS]-1 ||
    start[CW_COLUMNS]+count[CW_COLUMNS]-1 > dims[CW_COLUMNS]-1)
    return (CW_ERR_VAR_INDEX);
  len = count[CW_ROWS]*count[CW_COLUMNS];

  /* Check compression */
  /* ----------------- */
  pixel_size = cw_files[cwid]->pixel_size;
  data_id = cw_files[cwid]->data_id;
  if (cw_files[cwid]->ufp == NULL &&
    (data_id == CW_DATA_ID_VISIBLE || data_id == CW_DATA_ID_IR) &&
    pixel_size == 2) {
    fp = cw_files[cwid]->fp;
    if (cw_get_att (fp, &compression, CW_O_COMPRESSION_TYPE) != 0)
      return (CW_ERR_READ_ATT);
    switch (compression) {
    case CW_COMPRESSION_TYPE_NONE:
    case CW_COMPRESSION_TYPE_FLAT:
      break;
    case CW_COMPRESSION_TYPE_1B:
      if ((ret = cw_uncompress (cwid)) != CW_NOERR)
        return (ret);
      break;
    default:
      return (CW_ERR_UNSUP_COMPRESSION_TYPE);
    } /* switch */
  } /* if */
  fp = cw_files[cwid]->fp;

  switch (varid) {

  /* Get variable data */
  /* ----------------- */
  case CW_DATA:

    /* Get row/column data shifts */
    /* -------------------------- */
    if (cw_get_att (fp, shift+CW_ROWS, CW_O_VERTICAL_SHIFT) != 0)
      return (CW_ERR_READ_ATT);
    if (cw_get_att (fp, shift+CW_COLUMNS, CW_O_HORIZONTAL_SHIFT) != 0)
      return (CW_ERR_READ_ATT);

    /* Get raw data */
    /* ------------ */
    if ((raw = malloc (len*pixel_size)) == NULL)
      return (CW_ERR_NOMEM);
    ret = cw_get_raw (fp, raw, pixel_size, dims, start, count, shift);
    if (ret != CW_NOERR)
      break;

    switch (data_id) {

    /* Decode visible and IR */
    /* --------------------- */
    case CW_DATA_ID_VISIBLE:
    case CW_DATA_ID_IR:
      if (type != CW_FLOAT)
        ret = CW_ERR_VAR_VALUE;
      else if (cw_get_att (fp, &calibration, CW_O_CALIBRATION_TYPE) != 0 ||
        cw_get_att (fp, &compression, CW_O_COMPRESSION_TYPE) != 0 ||
        cw_get_att (fp, &channel, CW_O_CHANNEL_NUMBER) != 0)
        ret = CW_ERR_READ_ATT;
      else
        ret = cw_decode_channel ((float *) data, NULL,
          (unsigned short *) raw, len, calibration, data_id, compression,
          channel);  
      break;

    /* Decode ancillary */
    /* ---------------- */
    case CW_DATA_ID_ANCILLARY:
      if (type != CW_FLOAT)
        ret = CW_ERR_VAR_VALUE;
      else if (cw_get_att (fp, &channel, CW_O_CHANNEL_NUMBER) != 0 ||
        cw_get_att (fp, &compression, CW_O_COMPRESSION_TYPE) != 0)
        ret = CW_ERR_READ_ATT;
      else
        ret = cw_decode_ancillary ((float *) data, 
          (unsigned short *) raw, len, channel, compression);
      break;

    /* Decode cloud */
    /* ------------ */
    case CW_DATA_ID_CLOUD:
      ret = cw_cast_frombyte (data, (unsigned char *) raw, type, len); 
      break;

    default:
      ret = CW_ERR_UNSUP_DATA_ID;

    } /* switch */
    break;

  /* Get graphics data */
  /* ----------------- */
  case CW_GRAPHICS:

    /* Check for graphics */
    /* ------------------ */
    if (cw_files[cwid]->graphics == -1) {
      ret = CW_ERR_VAR_ID;
      break;
    } /* if */        

    switch (data_id) {

    /* Check for visible and IR */
    /* ------------------------ */
    case CW_DATA_ID_VISIBLE:
    case CW_DATA_ID_IR:

      /* Get raw data */
      /* ------------ */
      if ((raw = malloc (len*pixel_size)) == NULL)
        return (CW_ERR_NOMEM);
      if ((ucp = (unsigned char *) malloc (len)) == NULL) {
        free (raw);
        return (CW_ERR_NOMEM);
      } /* if */
      ret = cw_get_raw (fp, raw, pixel_size, dims, start, count, NULL);
      if (ret != CW_NOERR)
        break;

      /* Separate graphics from data */
      /* --------------------------- */
      cw_separate_channel (NULL, ucp, (unsigned short *) raw, len);
      ret = cw_cast_frombyte (data, ucp, type, len);
      break;

    default:
      ret = CW_ERR_VAR_ID;

    } /* switch */
    break;

  default:
    ret = CW_ERR_VAR_ID;

  } /* switch */

  /* Free memory */
  /* ----------- */
  if (raw != NULL)
    free (raw);
  if (ucp != NULL) 
    free ((void *) ucp);
  return (ret);

} /* cw_get_vara */

/**********************************************************************/

static void cw_uncal_raw (	/*** UNCALIBRATE RAW COUNTS ***/
  const float *fp,		/* float data */
  short *sp,			/* short data (modified) */
  size_t len			/* number of data values */
) {

  size_t i;

  for (i = 0; i < len; i++) {
    if (fp[i] == CW_BADVAL)
      sp[i] = 0;
    else {
      sp[i] = (short) roundf (fp[i]) + 1;
      if (sp[i] < 1 || sp[i] > 1024)
        sp[i] = 0;
    } /* else */
  } /* for */

} /* cw_uncal_raw */

/**********************************************************************/

static void cw_uncal_visible (	/*** UNCALIBRATE VISIBLE ALBEDO ***/
  const float *fp,		/* float data */
  short *sp,			/* short data (modified) */
  size_t len) {			/* number of data values */

  size_t i;

  for (i = 0; i < len; i++) {
    if (fp[i] == CW_BADVAL)
      sp[i] = 0;
    else {
      sp[i] = (short) roundf (fp[i]*20.47f) + 1;
      if (sp[i] < 1 || sp[i] > 2047)
        sp[i] = 0;
    } /* else */
  } /* for */

} /* cw_cal_visible */

/**********************************************************************/

static void cw_uncal_ir (	/*** UNCALIBRATE IR TEMPERATURE ***/
  const float *fp,		/* float data */
  short *sp,			/* short data (modified) */
  size_t len			/* number of data values */
) {

  size_t i;

  for (i = 0; i < len; i++) {
    if (fp[i] == CW_BADVAL || fp[i] < KTOC (178.0f))
      sp[i] = 0;
    else {
      if (fp[i] == KTOC (178.0f))
        sp[i] = 1;
      else if (fp[i] > KTOC (178.0f) && fp[i] < KTOC (270.0f))
        sp[i] = (short) roundf ((fp[i] - KTOC (178.0f))/0.1f) + 1;
      else if (fp[i] >= KTOC (270.0f) && fp[i] <= KTOC (310.0f))
        sp[i] = (short) roundf ((fp[i] - KTOC (270.0f))/0.05f) + 921;
      else
        sp[i] = (short) roundf ((fp[i] - KTOC (310.0f))/0.1f) + 1721;
      if (sp[i] < 1 || sp[i] > 2047)
        sp[i] = 0;
    } /* else */
  } /* for */

} /* cw_uncal_ir */

/**********************************************************************/

static int 
cw_encode_channel (		/*** ENCODE CHANNEL DATA ***/
  const float *fp,		/* float data */
  const unsigned char *ucp,	/* byte data */
  unsigned short *usp,		/* unsigned short data (modified) */
  size_t len,			/* number of data values */
  short calibration,		/* calibration type */
  short data_id			/* data id */
) {				/* return: CW_NOERR on success */

  short *sp;

  /* Check for data encode */
  /* --------------------- */
  if (fp != NULL) {

    /* Allocate memory */
    /* --------------- */
    if ((sp = (short *) malloc (len*sizeof (short))) == NULL)
      return (CW_ERR_NOMEM);
  
    /* Guess calibration type */
    /* ---------------------- */
    if (calibration != CW_CALIBRATION_TYPE_RAW &&
      calibration != CW_CALIBRATION_TYPE_ALBEDO_TEMPERATURE &&
      (data_id == CW_DATA_ID_VISIBLE ||
      data_id == CW_DATA_ID_IR))
      calibration = CW_CALIBRATION_TYPE_ALBEDO_TEMPERATURE;

    /* Uncalibrate data */
    /* ---------------- */
    switch (calibration) {
    case CW_CALIBRATION_TYPE_RAW:
      cw_uncal_raw (fp, sp, len);
      break;
    case CW_CALIBRATION_TYPE_ALBEDO_TEMPERATURE:
      switch (data_id) {
      case CW_DATA_ID_VISIBLE:
        cw_uncal_visible (fp, sp, len);
        break;
      case CW_DATA_ID_IR:
        cw_uncal_ir (fp, sp, len);
        break;
      default:
        free ((void *) sp);
        return (CW_ERR_UNSUP_DATA_ID);
      } /* switch */
      break;
    default:
      free ((void *) sp);
      return (CW_ERR_UNSUP_CALIBRATION_TYPE);
    } /* switch */

  } /* if */
  else
    sp = NULL;

  /* Combine channel and graphics data */
  /* --------------------------------- */
  cw_combine_channel (sp, ucp, usp, len);

  /* Free memory */
  /* ----------- */
  if (sp != NULL)
    free ((void *) sp);
  return (CW_NOERR);  

} /* cw_encode_channel */

/**********************************************************************/

static int 
cw_encode_ancillary (		/*** ENCODE ANCILLARY DATA ***/
  const float *fp,		/* float data */
  unsigned short *usp,		/* unsigned short data (modified) */
  size_t len,			/* number of data values */
  short channel			/* channel number */
) {

  size_t i;
  short hours, minutes;

  /* Check for data decode */
  /* --------------------- */
  if (fp != NULL) {

    /* Uncalibrate ancillary data */
    /* -------------------------- */
    switch (channel) {
    case CW_CHANNEL_NUMBER_SCAN_ANGLE:
    case CW_CHANNEL_NUMBER_SAT_ZENITH:
    case CW_CHANNEL_NUMBER_SOL_ZENITH:
    case CW_CHANNEL_NUMBER_REL_AZIMUTH:
      for (i = 0; i < len; i++) {
        if (fp[i] == CW_BADVAL)
          usp[i] = 0;
        else
          usp[i] = (unsigned short) roundf (fp[i]*128.0f) + 1;
      } /* for */
      break;
    case CW_CHANNEL_NUMBER_SCAN_TIME:
      for (i = 0; i < len; i++) {
        hours = fp[i];
        minutes = (short) roundf ((fp[i] - hours)*60);
        usp[i] = (unsigned short) (hours*100 + minutes);
      } /* for */
      break;
    default:
      return (CW_ERR_UNSUP_CHANNEL_NUMBER);
    } /* switch */

  } /* if */

  return (CW_NOERR);

} /* cw_encode_ancillary */

/**********************************************************************/

static int cw_cast_tofloat (	/*** CAST FROM OTHER TYPES TO FLOAT ***/
  float **fp,			/* float data (modified) */
  const void *data,		/* source data */
  cw_type type,			/* source type */
  size_t len			/* number of data values */
) {				/* return: CW_NOERR on success */

  unsigned char *data_uchar;
  size_t i;
  int ret;

  switch (type) {

  /* Cast from byte */
  /* -------------- */
  case CW_BYTE:
    if ((*fp = (float *) malloc (len*sizeof (float))) == NULL)
      return (CW_ERR_NOMEM);
    data_uchar = (unsigned char *) data;
    for (i = 0; i < len; i++)
      (*fp)[i] = data_uchar[i];
    ret = CW_NOERR;
    break;

  /* Cast from float */
  /* --------------- */
  case CW_FLOAT:
    *fp = (float *) data;
    ret = CW_NOERR;
    break;

  default:
    ret = CW_ERR_VAR_VALUE;

  } /* switch */          

  return (ret);

} /* cw_cast_tofloat */

/**********************************************************************/

static int cw_put_vara (	/*** PUT ARRAY OF DATA VALUES ***/
  int cwid,			/* CW file id */
  int varid,			/* variable id */
  const size_t start[],		/* start indices */
  const size_t count[],		/* dimension extents */
  const void *data,		/* source data */
  cw_type type			/* source type */
) {				/* return: CW_NOERR on success */

  short *dims;
  short pixel_size;
  int ret;
  FILE *fp;
  void *raw = NULL;
  size_t len, i;
  short data_id, channel, calibration, compression;
  short shift[2];
  float *fltp = NULL;
  unsigned char *ucp = NULL;
  short *sp = NULL;

  /* Check file id and mode */
  /* ---------------------- */
  if (cw_files[cwid] == NULL)
    return (CW_ERR_DATASET_ID);
  if (cw_files[cwid]->defmode == CW_TRUE)
    return (CW_ERR_DEFINE_MODE);

  /* Check start and extents */
  /* ----------------------- */
  dims = cw_files[cwid]->dims;
  if (start[CW_ROWS] > dims[CW_ROWS]-1 ||
    start[CW_COLUMNS] > dims[CW_COLUMNS]-1 ||
    start[CW_ROWS]+count[CW_ROWS]-1 > dims[CW_ROWS]-1 ||
    start[CW_COLUMNS]+count[CW_COLUMNS]-1 > dims[CW_COLUMNS]-1)
    return (CW_ERR_VAR_INDEX);
  len = count[CW_ROWS]*count[CW_COLUMNS];

  /* Check compression */
  /* ----------------- */
  pixel_size = cw_files[cwid]->pixel_size;
  data_id = cw_files[cwid]->data_id;
  if (cw_files[cwid]->ufp == NULL &&
    (data_id == CW_DATA_ID_VISIBLE || data_id == CW_DATA_ID_IR) &&
    pixel_size == 2) {
    fp = cw_files[cwid]->fp;
    if (cw_get_att (fp, &compression, CW_O_COMPRESSION_TYPE) != 0)
      return (CW_ERR_READ_ATT);
    switch (compression) {
    case CW_COMPRESSION_TYPE_NONE:
    case CW_COMPRESSION_TYPE_FLAT:
      break;
    case CW_COMPRESSION_TYPE_1B:
      ret = cw_uncompress (cwid);
      if (ret != CW_NOERR)
        return (ret);
      break;
    default:
      return (CW_ERR_UNSUP_COMPRESSION_TYPE);
    } /* switch */
  } /* if */
  fp = cw_files[cwid]->fp;

  /* Get row/column data shifts */
  /* -------------------------- */
  if (cw_get_att (fp, shift+CW_ROWS, CW_O_VERTICAL_SHIFT) != 0)
    return (CW_ERR_READ_ATT);
  if (cw_get_att (fp, shift+CW_COLUMNS, CW_O_HORIZONTAL_SHIFT) != 0)
    return (CW_ERR_READ_ATT);
  if (shift[CW_ROWS] != 0 || shift[CW_COLUMNS] != 0) 
    return (CW_ERR_WRITE_SHIFT);

  switch (varid) {

  /* Put variable data */
  /* ----------------- */
  case CW_DATA:

    /* Allocate raw data memory */
    /* ------------------------ */
    if ((raw = malloc (len*pixel_size)) == NULL)
      return (CW_ERR_NOMEM);

    switch (data_id) {

    /* Encode visible and IR */
    /* --------------------- */
    case CW_DATA_ID_VISIBLE:
    case CW_DATA_ID_IR:
      if ((ret = cw_cast_tofloat (&fltp, data, type, len)) != CW_NOERR)
        break;
      else if (cw_get_att (fp, &calibration, CW_O_CALIBRATION_TYPE) != 0)
        ret = CW_ERR_READ_ATT;
      else {
        ret = cw_get_raw (fp, raw, pixel_size, dims, start, count, NULL);
        if (ret != CW_NOERR)
          break;
        if ((ucp = (unsigned char *) malloc (len)) == NULL)
          break;
        cw_separate_channel (NULL, ucp, (unsigned short *) raw, len);
        ret = cw_encode_channel (fltp, ucp, (unsigned short *) raw, 
          len, calibration, data_id);  
      } /* else */
      break;

    /* Encode ancillary */
    /* ---------------- */
    case CW_DATA_ID_ANCILLARY:
      if ((ret = cw_cast_tofloat (&fltp, data, type, len)) != CW_NOERR)
        break;
      else if (cw_get_att (fp, &channel, CW_O_CHANNEL_NUMBER) != 0)
        ret = CW_ERR_READ_ATT;
      else
        ret = cw_encode_ancillary ((float *) data, (unsigned short *) raw, 
          len, channel);
      break;

    /* Encode cloud */
    /* ------------ */
    case CW_DATA_ID_CLOUD:
      if (type != CW_BYTE)
        ret = CW_ERR_VAR_VALUE;
      else {
        for (i = 0; i < len; i++)
          ((unsigned char *) raw)[i] = ((unsigned char *) data)[i];
        ret = CW_NOERR;
      } /* else */
      break;

    default:
      ret = CW_ERR_UNSUP_DATA_ID;

    } /* switch */
    break;

  /* Put graphics data */
  /* ----------------- */
  case CW_GRAPHICS:

    /* Check data type */
    /* --------------- */
    if (type != CW_BYTE) {
      ret = CW_ERR_VAR_VALUE;
      break;
    } /* if */

    /* Check for graphics */
    /* ------------------ */
    if (cw_files[cwid]->graphics == -1) {
      ret = CW_ERR_VAR_ID;
      break;
    } /* if */        

    switch (data_id) {

    /* Check for visible and IR */
    /* ------------------------ */
    case CW_DATA_ID_VISIBLE:
    case CW_DATA_ID_IR:

      /* Allocate memory */
      /* --------------- */
      if ((raw = malloc (len*pixel_size)) == NULL) {
        ret = CW_ERR_NOMEM;
        break;
      } /* if */
      if ((sp = (short *) malloc (len*sizeof (short))) == NULL) {
        ret = CW_ERR_NOMEM;
        break;
      } /* if */

      /* Encode graphics */
      /* --------------- */
      ret = cw_get_raw (fp, raw, pixel_size, dims, start, count, NULL);
      if (ret != CW_NOERR)
        break;
      cw_separate_channel (sp, NULL, (unsigned short *) raw, len);
      cw_combine_channel (sp, (unsigned char *) data, (unsigned short *) raw,
        len);
      ret = CW_NOERR;
      break;

    default:
      ret = CW_ERR_VAR_ID;

    } /* switch */
    break;

  default:
    ret = CW_ERR_VAR_ID;

  } /* switch */

  /* Put raw data */
  /* ------------ */
  if (ret == CW_NOERR)
    ret = cw_put_raw (fp, raw, pixel_size, dims, start, count);

  /* Free memory */
  /* ----------- */
  if (fltp != NULL && fltp != data)
    free ((void *) fltp);
  if (raw != NULL)
    free (raw);
  if (ucp != NULL)
    free ((void *) ucp);
  if (sp != NULL)
    free ((void *) sp);
  return (ret);

} /* cw_put_vara */

/**********************************************************************/

int cw_create (			/*** CREATE COASTWATCH FILE ***/
  const char *path,		/* file path and name */
  int cmode,			/* creation mode */
  int *cwidp			/* CW file id (modified) */
) {				/* return: CW_NOERR on success */

  int id;
  FILE *fp;
  long offset;
  unsigned char empty = 0;
  int i, err;
  unsigned char magic = CW_MAGIC_NUM;
 
  /* Determine byte order */
  /* -------------------- */
  if (cw_byteswap == -1)
    cw_byteswap = (byte_order () == 0);

  /* Check creation mode */
  /* ------------------- */
  switch (cmode) {
  case CW_CLOBBER: break;
  case CW_NOCLOBBER: break;
  default: return (CW_ERR_CREATE_MODE);
  } /* switch */

  /* Find file slot */
  /* -------------- */
  id = 0;
  while (cw_files[id] != NULL && id < CW_MAX_FILES)
    id++;
  if (id == CW_MAX_FILES)
    return (CW_ERR_MAX_FILES);

  /* Create file */
  /* ----------- */
  if (cmode == CW_CLOBBER) {
    if ((fp = fopen (path, "w+b")) == NULL)
      return (CW_ERR_CREATE);
  } /* if */
  else {
    if ((fp = fopen (path, "rb")) != NULL) {
      fclose (fp);
      return (CW_ERR_CREATE_EXISTS);
    } /* if */
    if ((fp = fopen (path, "w+b")) == NULL)
      return (CW_ERR_CREATE);
  } /* else */

  /* Create file structure */
  /* --------------------- */
  cw_files[id] = (cw_file_type *) malloc (sizeof (cw_file_type));
  if (cw_files[id] == NULL) {
    fclose (fp);
    remove (path);
    return (CW_ERR_NOMEM);       
  } /* if */

  /* Fill in structure */
  /* ----------------- */
  cw_files[id]->fp = fp;
  cw_files[id]->ufp = NULL;
  cw_files[id]->defmode = CW_TRUE;
  cw_files[id]->wmode = CW_WRITE;
  cw_files[id]->data_id = -1;
  cw_files[id]->graphics = -1;
  for (i = 0; i < CW_DIM_NUM; i++)
    cw_files[id]->dims[i] = -1;
  cw_files[id]->pixel_size = -1;
  if ((cw_files[id]->path = (char *) malloc (strlen (path) + 1)) == NULL)
    { err = CW_ERR_NOMEM; goto cleanup; }
  strcpy (cw_files[id]->path, path);
  
  /* Write header */
  /* ------------ */
  if (fwrite ((void *) &magic, 1, 1, fp) < 1)
    { err = CW_ERR_CREATE_HEADER; goto cleanup; } 
  for (offset = 1; offset < CW_HEAD_MIN; offset++)
    if (fwrite ((void *) &empty, 1, 1, fp) < 1)
      { err = CW_ERR_CREATE_HEADER; goto cleanup; }

  /* Assign id */
  /* --------- */
  *cwidp = id;
  return (CW_NOERR);

  /* Clean up */
  /* -------- */
  cleanup:
  free ((void *) cw_files[id]);
  cw_files[id] = NULL;
  fclose (fp);
  remove (path);
  return (err);

} /* cw_create */

/**********************************************************************/

int cw_clone (
  int cwid, 
  int cmode, 
  int *cwidp
) {


  /* unimplemented */

  return (CW_NOERR);



} /* cw_clone */

/**********************************************************************/

int cw_open (			/*** OPEN COASTWATCH FILE ***/
  const char *path,		/* file path and name */
  int omode,			/* open mode */
  int *cwidp			/* CW file id (modified) */
) {				/* return: CW_NOERR on success */

  int id;
  unsigned char magic;
  FILE *fp;
  int i, err;
  short compression;

  /* Determine byte order */
  /* -------------------- */
  if (cw_byteswap == -1)
    cw_byteswap = (byte_order () == 0);
   
  /* Check open mode */
  /* --------------- */
  switch (omode) {
  case CW_NOWRITE: break;
  case CW_WRITE: break;
  default: return (CW_ERR_ACCESS_MODE);
  } /* switch */

  /* Find file slot */
  /* -------------- */
  id = 0;
  while (cw_files[id] != NULL && id < CW_MAX_FILES)
    id++;
  if (id == CW_MAX_FILES)
    return (CW_ERR_MAX_FILES);

  /* Open file */
  /* ----------- */
  if (omode == CW_NOWRITE)
    fp = fopen (path, "rb");
  else
    fp = fopen (path, "r+b");
  if (fp == NULL)
    return (CW_ERR_ACCESS);

  /* Check magic number */
  /* ------------------ */
  if (fread ((void *) &magic, 1, 1, fp) < 1) {
    fclose (fp);
    return (CW_ERR_MAGIC_READ);
  } /* if */    
  if (magic != CW_MAGIC_NUM) {
    fclose (fp);
    return (CW_ERR_MAGIC);
  } /* if */

  /* Create file structure */
  /* --------------------- */
  cw_files[id] = (cw_file_type *) malloc (sizeof (cw_file_type));
  if (cw_files[id] == NULL) {
    fclose (fp);
    return (CW_ERR_NOMEM);       
  } /* if */

  /* Fill in structure */
  /* ----------------- */
  cw_files[id]->fp = fp;
  cw_files[id]->ufp = NULL;
  cw_files[id]->defmode = CW_FALSE;
  cw_files[id]->wmode = omode;
  if ((cw_files[id]->path = (char *) malloc (strlen (path) + 1)) == NULL)
    { err = CW_ERR_NOMEM; goto cleanup; }
  strcpy (cw_files[id]->path, path);

  if (cw_get_att (fp, (short *) &(cw_files[id]->data_id), CW_O_DATA_ID) != 0)
    { err = CW_ERR_READ_ATT; goto cleanup; }
  for (i = 0; i < CW_DIM_NUM; i++)
    if (cw_get_att (fp, &(cw_files[id]->dims[i]), 
      cw_dimensions[i].dim_offset) != 0)
      { err = CW_ERR_READ_ATT; goto cleanup; }

  switch (cw_files[id]->data_id) {
  case CW_DATA_ID_VISIBLE:
  case CW_DATA_ID_IR:
    if (cw_get_att (fp, &(cw_files[id]->pixel_size), 
      CW_O_CHANNEL_PIXEL_SIZE) != 0)
      { err = CW_ERR_READ_ATT; goto cleanup; }
    if (cw_files[id]->pixel_size != 2)
      { err = CW_ERR_UNSUP_PIXEL_SIZE; goto cleanup; }
    if (cw_get_att (fp, &compression, CW_O_COMPRESSION_TYPE) != 0)
      { err = CW_ERR_READ_ATT; goto cleanup; }
    if (compression == CW_COMPRESSION_TYPE_FLAT)
      cw_files[id]->graphics = -1;
    else
      cw_files[id]->graphics = 1;
    break;
  case CW_DATA_ID_ANCILLARY:
    if (cw_get_att (fp, &(cw_files[id]->pixel_size), 
      CW_O_ANCILLARY_PIXEL_SIZE) != 0)
      { err = CW_ERR_READ_ATT; goto cleanup; }
    if (cw_files[id]->pixel_size != 2)
      { err = CW_ERR_UNSUP_PIXEL_SIZE; goto cleanup; }
    cw_files[id]->graphics = -1;
    break;
  case CW_DATA_ID_CLOUD:
    cw_files[id]->pixel_size = 1;
    cw_files[id]->graphics = -1;
    break;
  default:
    { err = CW_ERR_UNSUP_DATA_ID; goto cleanup; }
  } /* switch */

  /* Assign id */
  /* --------- */
  *cwidp = id;
  return (CW_NOERR);

  /* Clean up */
  /* -------- */
  cleanup:
  free ((void *) cw_files[id]);
  cw_files[id] = NULL;
  fclose (fp);
  return (err);

} /* cw_open */
 
/**********************************************************************/

int cw_enddef (			/*** END DEFINE MODE ***/
  int cwid			/* CW file id */
) {				/* return: CW_NOERR on success */

  short rows, columns, pixel_size, data_id;
  short i, j, k;
  FILE *fp, *ufp;
  unsigned char empty = 0;
  short compression;
  size_t headlen;
  long offset;

  /* Check file id */
  /* ------------- */
  if (cw_files[cwid] == NULL)
    return (CW_ERR_DATASET_ID);
  fp = cw_files[cwid]->fp;
 
  /* Check define mode */
  /* ----------------- */
  if (cw_files[cwid]->defmode == CW_FALSE)
    return (CW_ERR_NOT_DEFINE_MODE);

  /* Check dims defined */
  /* ------------------ */
  rows = cw_files[cwid]->dims[CW_ROWS];
  columns = cw_files[cwid]->dims[CW_COLUMNS];
  if (rows == -1 || columns == -1)
    return (CW_ERR_DIM_UNDEFINED);

  /* Check variable defined */
  /* ---------------------- */
  if (cw_files[cwid]->data_id == -1)
    return (CW_ERR_VAR_UNDEFINED);

  /* Check compression */
  /* ----------------- */
  pixel_size = cw_files[cwid]->pixel_size;
  data_id = cw_files[cwid]->data_id;
  if ((data_id == CW_DATA_ID_VISIBLE || data_id == CW_DATA_ID_IR) &&
    pixel_size == 2) {
    if (cw_get_att (fp, &compression, CW_O_COMPRESSION_TYPE) != 0)
      return (CW_ERR_READ_ATT);
    if (compression == CW_COMPRESSION_TYPE_1B) {
      if ((ufp = tmpfile ()) == NULL)
        return (CW_ERR_UFILE);
      if (cpyfile (ufp, fp) != 0)
        return (CW_ERR_UFILE);
      if (fclose (fp) != 0)
        return (CW_ERR_UFILE);    
      cw_files[cwid]->ufp = ufp;
      cw_files[cwid]->fp = ufp;
      fp = ufp;
    } /* if */
  } /* if */  

  /* Fill in header */
  /* -------------- */
  headlen = (size_t) columns*pixel_size;
  if (fseek (fp, 0L, SEEK_END) != 0)
    return (CW_ERR_WRITE_DATA);
  for (offset = ftell (fp); offset < headlen; offset++)
    if (fwrite ((void *) &empty, 1, 1, fp) < 1)
      return (CW_ERR_WRITE_DATA);

  /* Fill in data */
  /* ------------ */
  for (i = 0; i < rows; i++)
    for (j = 0; j < columns; j++)
      for (k = 0; k < pixel_size; k++)
        if (fwrite ((void *) &empty, 1, 1, fp) < 1)
          return (CW_ERR_WRITE_DATA);

  /* End define mode */
  /* --------------- */
  cw_files[cwid]->defmode = CW_FALSE;
  return (CW_NOERR);

} /* cw_enddef */

/**********************************************************************/

int cw_close (			/*** CLOSE COASTWATCH FILE ***/
  int cwid			/* CW file id */
) {				/* return: CW_NOERR on success */

  int ret;
  short compression;
  FILE *fp;

  /* Check file id */
  /* ------------- */
  if (cw_files[cwid] == NULL)
    return (CW_ERR_DATASET_ID);

  /* Check define mode */
  /* ----------------- */
  if (cw_files[cwid]->defmode == CW_TRUE)
    if (cw_enddef (cwid) != CW_NOERR)
      return (CW_ERR_ENDDEF_FAILED);

  /* Check compression */
  /* ----------------- */
  if (cw_files[cwid]->ufp != NULL && cw_files[cwid]->wmode == CW_WRITE) {
    fp = cw_files[cwid]->fp;
    if (cw_get_att (fp, &compression, CW_O_COMPRESSION_TYPE) != 0)
      return (CW_ERR_READ_ATT);
    if (compression == CW_COMPRESSION_TYPE_1B) {
      if ((ret = cw_compress (cwid)) != CW_NOERR)
        return (ret);
    } /* if */
  } /* if */   

  /* Close file and free memory */
  /* -------------------------- */
  fclose (cw_files[cwid]->fp);
  free ((void *) cw_files[cwid]);
  cw_files[cwid] = NULL;
  return (CW_NOERR);

} /* cw_close */

/**********************************************************************/

const char *cw_strerror (	/*** GET STRING ERROR MESSAGE ***/
  int cwerr			/* error code */
) {				/* return: verbose error string */

  if (cwerr < 0 || cwerr > CW_ERR_NUM)
    return (cw_error_table[CW_ERR_UNKNOWN]);
  return (cw_error_table[cwerr]);

} /* cw_strerror */

/**********************************************************************/

int cw_def_dim (		/*** DEFINE DIMENSION ***/
  int cwid, 			/* CW file id */
  const char *name, 		/* dimension name */
  size_t len, 			/* dimension length */
  int *dimidp			/* dimension ID (modified) */
) {				/* return: CW_NOERR on success */ 

  /* Check file ID */
  /* ------------- */
  if (cw_files[cwid] == NULL)
    return (CW_ERR_DATASET_ID);

  /* Check file mode */
  /* --------------- */
  if (cw_files[cwid]->defmode == CW_FALSE)
    return (CW_ERR_NOT_DEFINE_MODE);

  /* Check dimension name */
  /* -------------------- */
  if (cw_lookup_dimid (dimidp, name) != 0)
    return (CW_ERR_DIM);

  /* Check if dimension defined */
  /* -------------------------- */
  if (cw_files[cwid]->dims[*dimidp] != -1)
    return (CW_ERR_DIM_DEFINED);

  /* Define dimension */
  /* ---------------- */
  if (cw_put_att (cw_files[cwid]->fp, (short) len, 
    cw_dimensions[*dimidp].dim_offset) != 0)
    return (CW_ERR_WRITE_DIM);
  cw_files[cwid]->dims[*dimidp] = (short) len;
  return (CW_NOERR);

} /* cw_def_dim */

/**********************************************************************/

int cw_inq_dimid (		/*** GET DIMENSION ID ***/
  int cwid, 			/* CW file id */
  const char *name, 		/* dimension name */
  int *dimidp			/* dimension ID (modified) */
) {				/* return: CW_NOERR on success */

  /* Check file ID */
  /* ------------- */  
  if (cw_files[cwid] == NULL)
    return (CW_ERR_DATASET_ID);

  /* Check dimension name */
  /* -------------------- */
  if (cw_lookup_dimid (dimidp, name) != 0)
    return (CW_ERR_DIM);

  /* Check dimension defined */
  /* ----------------------- */
  if (cw_files[cwid]->dims[*dimidp] == -1)
    return (CW_ERR_DIM);
  return (CW_NOERR);

} /* cw_inq_dimid */

/**********************************************************************/

int cw_inq_dim (		/*** GET DIMENSION NAME AND LENGTH ***/
  int cwid,			/* CW file id */
  int dimid, 			/* dimension ID */
  char *name, 			/* dimension name (modified if not NULL) */
  size_t *lengthp		/* dimension length (modified if not NULL) */
) {				/* return: CW_NOERR on success */

  short length;

  /* Check file ID */
  /* ------------- */  
  if (cw_files[cwid] == NULL)
    return (CW_ERR_DATASET_ID);

  /* Check dimension ID */
  /* ------------------ */
  if (dimid < 0 || dimid > CW_DIM_NUM-1)
    return (CW_ERR_DIM_ID);

  /* Check dimension defined */
  /* ----------------------- */
  if (cw_files[cwid]->dims[dimid] == -1)
    return (CW_ERR_DIM_ID);

  /* Get name */
  /* -------- */
  if (name != NULL)
    strcpy (name, cw_dimensions[dimid].dim_name);

  /* Get length */
  /* ---------- */
  if (lengthp != NULL) {
    if (cw_get_att (cw_files[cwid]->fp, &length, 
      cw_dimensions[dimid].dim_offset) != 0)
      return (CW_ERR_READ_DIM);
    *lengthp = (size_t) length;
  } /* if */

  return (CW_NOERR);

} /* cw_inq_dim */

/**********************************************************************/

int cw_def_var (		/*** DEFINE DATA VARIABLE ***/
  int cwid, 			/* CW file id */
  const char *name, 		/* variable name */
  cw_type xtype, 		/* external data type */
  int ndims,			/* number of dimensions */
  const int dimids[], 		/* dimension IDs (modified) */
  int *varidp			/* variable ID (modified) */
) {				/* return: CW_NOERR on success */

  short code, data_id;
  cw_type file_xtype;

  /* Check file ID */
  /* ------------- */  
  if (cw_files[cwid] == NULL)
    return (CW_ERR_DATASET_ID);

  /* Check file mode */
  /* --------------- */  
  if (cw_files[cwid]->defmode == CW_FALSE)
    return (CW_ERR_NOT_DEFINE_MODE);

  /* Check dimension number */
  /* ---------------------- */
  if (ndims != CW_DIM_NUM)
    return (CW_ERR_DIM_NUM);

  /* Check dimension IDs */
  /* ------------------- */
  if (dimids[0] != CW_ROWS || dimids[1] != CW_COLUMNS)
    return (CW_ERR_DIM_ID);

  /* Check dimension defined */
  /* ----------------------- */
  if (cw_files[cwid]->dims[CW_ROWS] == -1 ||
    cw_files[cwid]->dims[CW_COLUMNS] == -1)
    return (CW_ERR_DIM_ID);

  /* Define graphics */
  /* --------------- */
  if (strcmp (name, "graphics") == 0) {
    data_id = cw_files[cwid]->data_id;
    if (data_id == -1)
      return (CW_ERR_VAR);
    if (data_id != CW_DATA_ID_VISIBLE && data_id != CW_DATA_ID_IR)
      return (CW_ERR_VAR);      
    if (xtype != CW_BYTE)
      return (CW_ERR_DATA_TYPE);
    cw_files[cwid]->graphics = 1;
    *varidp = CW_GRAPHICS;      
    return (CW_NOERR);
  } /* if */

  /* Check variable defined */
  /* ---------------------- */
  if (cw_files[cwid]->data_id != -1)
    return (CW_ERR_VAR_DEFINED);

  /* Check variable name */
  /* ------------------- */
  if (cw_lookup_att_code (&code, name, CW_CHANNEL_NUMBER) != 0)
    return (CW_ERR_VAR);
  switch (code) {
    case CW_CHANNEL_NUMBER_AVHRR1:
    case CW_CHANNEL_NUMBER_AVHRR2:
    case CW_CHANNEL_NUMBER_OCEAN_REFLECT:
    case CW_CHANNEL_NUMBER_TURBIDITY:
      data_id = CW_DATA_ID_VISIBLE;
      file_xtype = CW_FLOAT;            
      break;
    case CW_CHANNEL_NUMBER_AVHRR3:
    case CW_CHANNEL_NUMBER_AVHRR4:
    case CW_CHANNEL_NUMBER_AVHRR5:
    case CW_CHANNEL_NUMBER_MCSST:
    case CW_CHANNEL_NUMBER_MCSST_SPLIT:
    case CW_CHANNEL_NUMBER_MCSST_DUAL:
    case CW_CHANNEL_NUMBER_MCSST_TRIPLE:
    case CW_CHANNEL_NUMBER_CPSST_SPLIT:
    case CW_CHANNEL_NUMBER_CPSST_DUAL:
    case CW_CHANNEL_NUMBER_CPSST_TRIPLE:
    case CW_CHANNEL_NUMBER_NLSST_SPLIT:
    case CW_CHANNEL_NUMBER_NLSST_DUAL:
    case CW_CHANNEL_NUMBER_NLSST_TRIPLE:
      data_id = CW_DATA_ID_IR;
      file_xtype = CW_FLOAT;
      break;
    case CW_CHANNEL_NUMBER_SCAN_ANGLE:
    case CW_CHANNEL_NUMBER_SAT_ZENITH:
    case CW_CHANNEL_NUMBER_SOL_ZENITH:
    case CW_CHANNEL_NUMBER_REL_AZIMUTH:
    case CW_CHANNEL_NUMBER_SCAN_TIME:
      data_id = CW_DATA_ID_ANCILLARY;
      file_xtype = CW_FLOAT;
      break;
    case CW_CHANNEL_NUMBER_CLOUD:
      data_id = CW_DATA_ID_CLOUD;
      file_xtype = CW_BYTE;
      break;
    default:
      return (CW_ERR_INTERNAL);
  } /* switch */

  /* Check external type */
  /* ------------------- */
  if (xtype != file_xtype)
    return (CW_ERR_DATA_TYPE);

  /* Set attributes */
  /* -------------- */
  if (cw_put_att (cw_files[cwid]->fp, data_id, CW_O_DATA_ID) != 0) 
    return (CW_ERR_WRITE_ATT);
  if (cw_put_att (cw_files[cwid]->fp, code, CW_O_CHANNEL_NUMBER) != 0) 
    return (CW_ERR_WRITE_ATT);
  cw_files[cwid]->data_id = data_id;
  
  switch (data_id) {
    case CW_DATA_ID_VISIBLE:
    case CW_DATA_ID_IR:
      if (cw_put_att (cw_files[cwid]->fp, 2, CW_O_CHANNEL_PIXEL_SIZE) != 0) 
        return (CW_ERR_WRITE_ATT);
      cw_files[cwid]->pixel_size = 2;
      if (cw_put_att (cw_files[cwid]->fp, 
        CW_CALIBRATION_TYPE_ALBEDO_TEMPERATURE, CW_O_CALIBRATION_TYPE) != 0) 
        return (CW_ERR_WRITE_ATT);
      if (cw_put_att (cw_files[cwid]->fp, 1, CW_O_CHANNELS_PRODUCED) != 0) 
        return (CW_ERR_WRITE_ATT);
      if (cw_put_att (cw_files[cwid]->fp, CW_COMPRESSION_TYPE_1B, 
        CW_O_COMPRESSION_TYPE) != 0) 
        return (CW_ERR_WRITE_ATT);
      break; 
    case CW_DATA_ID_ANCILLARY:
      if (cw_put_att (cw_files[cwid]->fp, 2, CW_O_ANCILLARY_PIXEL_SIZE) != 0) 
        return (CW_ERR_WRITE_ATT);
      cw_files[cwid]->pixel_size = 2;
      if (cw_put_att (cw_files[cwid]->fp, 1, CW_O_ANCILLARIES_PRODUCED) != 0) 
        return (CW_ERR_WRITE_ATT);
      break; 
    case CW_DATA_ID_CLOUD:
      cw_files[cwid]->pixel_size = 1;
      break;
    default:
      return (CW_ERR_INTERNAL);
  } /* switch */

  /* Finish up */
  /* --------- */
  *varidp = CW_DATA;
  return (CW_NOERR);

} /* cw_def_var */

/**********************************************************************/

int cw_inq_varid (		/*** GET VARIABLE ID ***/
  int cwid, 			/* CW file id */
  const char *name, 		/* variable name */
  int *varidp			/* variable ID (modified) */
) {				/* return: CW_NOERR on success */

  short file_code, user_code;

  /* Check file ID */
  /* ------------- */  
  if (cw_files[cwid] == NULL)
    return (CW_ERR_DATASET_ID);

  /* Graphics variable */
  /* ----------------- */
  if (strcmp (name, "graphics") == 0) {
    if (cw_files[cwid]->graphics == -1)
      return (CW_ERR_VAR);
    else
      *varidp = CW_GRAPHICS;
  } /* if */

  /* Data variable */
  /* ------------- */
  else {
    if (cw_files[cwid]->data_id == -1)
      return (CW_ERR_VAR);
    if (cw_get_att (cw_files[cwid]->fp, &file_code, CW_O_CHANNEL_NUMBER) != 0) 
      return (CW_ERR_READ_ATT);
    if (cw_lookup_att_code (&user_code, name, CW_CHANNEL_NUMBER) != 0)
      return (CW_ERR_VAR);
    if (user_code != file_code)
      return (CW_ERR_VAR);
    *varidp = CW_DATA;
  } /* else */

  return (CW_NOERR);

} /* cw_inq_varid */

/**********************************************************************/

int cw_inq_var (int cwid, int varid, char *name, cw_type *xtypep,
  int *ndimsp, int dimids[], int *nattsp) {

/* Gets the variable name, xtype, number of dims, dim ids, and number of
   attributes. Any NULL pointers are not filled. */

short channel;

  if (cw_files[cwid] == NULL)			/* check file id */
    return (CW_ERR_DATASET_ID);

  if (varid == CW_GRAPHICS) {			/* variable = graphics */

    if (cw_files[cwid]->graphics == -1)		/* check var defined */
      return (CW_ERR_VAR_ID);

    if (name != NULL)				/* get name */
      sprintf (name, "graphics");

    if (xtypep != NULL)				/* get type */
      *xtypep = CW_BYTE;

    if (nattsp != NULL)				/* get numbers of atts */
      *nattsp = 0;      

  } /* if */

  else if (varid == CW_DATA) {			/* variable = data */

    if (cw_files[cwid]->data_id == -1)		/* check var defined */
      return (CW_ERR_VAR_ID);
  
    if (name != NULL) {				/* get name */
      if (cw_get_att (cw_files[cwid]->fp, &channel, CW_O_CHANNEL_NUMBER) != 0) 
        return (CW_ERR_READ_ATT);
      if (cw_lookup_att_code_name (name, channel, CW_CHANNEL_NUMBER) != 0)
        return (CW_ERR_UNSUP_CHANNEL_NUMBER);
    } /* if */

    if (xtypep != NULL) {			/* get external type */
      switch (cw_files[cwid]->data_id) {

        case CW_DATA_ID_VISIBLE:
        case CW_DATA_ID_IR:
        case CW_DATA_ID_ANCILLARY:
          *xtypep = CW_FLOAT;  
          break;

        case CW_DATA_ID_CLOUD:
          *xtypep = CW_BYTE;
          break;

        default:
          return (CW_ERR_INTERNAL);      

      } /* switch */
    } /* if */

    if (nattsp != NULL)				/* get numbers of atts */
      *nattsp = CW_ATT_NUM;      

  } /* else if */

  else
    return (CW_ERR_VAR_ID);

  if (ndimsp != NULL)				/* get number of dims */
    *ndimsp = 2;

  if (dimids != NULL) {				/* get dimension ids */
    dimids[0] = CW_ROWS;
    dimids[1] = CW_COLUMNS;
  } /* if */

  return (CW_NOERR);  

} /* cw_inq_var */

/**********************************************************************/

int cw_put_vara_float (int cwid, int varid, const size_t start[], 
  const size_t count[], const float *fp) {

/* Puts an array of floats. */

  return (cw_put_vara (cwid, varid, start, count, (void *) fp, CW_FLOAT));

} /* cw_put_vara_float */

/**********************************************************************/

int cw_put_vara_uchar (int cwid, int varid, const size_t start[], 
  const size_t count[], const unsigned char *ucp) {

/* Puts an array of unsigned chars. */

  return (cw_put_vara (cwid, varid, start, count, (void *) ucp, CW_BYTE));

} /* cw_put_vara_uchar */

/**********************************************************************/

int cw_get_vara_float (int cwid, int varid, const size_t start[],
  const size_t count[], float *fp) {

/* Gets an array of floats. */

  return (cw_get_vara (cwid, varid, start, count, (void *) fp, CW_FLOAT));

} /* cw_get_vara_float */

/**********************************************************************/

int cw_get_vara_uchar (int cwid, int varid, const size_t start[],
  const size_t count[], unsigned char *ucp) {

/* Get an array of unsigned chars. */

  return (cw_get_vara (cwid, varid, start, count, (void *) ucp, CW_BYTE));

} /* cw_get_vara_uchar */

/**********************************************************************/
/* attribute functions                                                */
/**********************************************************************/

int cw_inq_attname (int cwid, int varid, int attid, char *name) {

/* Gets the attribute name. */

  if (cw_files[cwid] == NULL)			/* check file id */
    return (CW_ERR_DATASET_ID);

  if (varid == CW_GRAPHICS)			/* check var id */
    return (CW_ERR_ATT_ID);
  if (varid != CW_DATA)
    return (CW_ERR_VAR_ID);

  if (cw_files[cwid]->data_id == -1)		/* check var defined */
    return (CW_ERR_VAR_ID);

  if (attid < 0 || attid > CW_ATT_NUM-1)	/* check att id */
    return (CW_ERR_ATT_ID);

						/* copy name */
  strcpy (name, cw_attributes[attid].att_name);
  return (CW_NOERR);

} /* cw_inq_attname */

/**********************************************************************/

int cw_inq_att (int cwid, int varid, const char *name, cw_type *xtypep, 
  size_t *lenp) {

/* Gets the attribute external type and length. Any NULL pointers are not
   filled. */

int attid;
short att_offset, att_code;
char att_code_name[CW_MAX_NAME];

  if (cw_files[cwid] == NULL)			/* check file id */
    return (CW_ERR_DATASET_ID);

  if (varid == CW_GRAPHICS)			/* check var id */
    return (CW_ERR_ATT);
  if (varid != CW_DATA)
    return (CW_ERR_VAR_ID);

  if (cw_files[cwid]->data_id == -1)		/* check var defined */
    return (CW_ERR_VAR_ID);

  if (cw_lookup_attid (&attid, name) != 0)	/* lookup att id */
    return (CW_ERR_ATT);

  if (xtypep != NULL)				/* get external type */
    *xtypep = cw_attributes[attid].att_type;

  if (lenp != NULL) {				/* get length */
    switch (cw_attributes[attid].att_type) {
      case CW_SHORT:
      case CW_FLOAT:
        *lenp = 1;  
        break;
      case CW_CHAR:
        att_offset = cw_attributes[attid].att_offset;
        if (cw_get_att (cw_files[cwid]->fp, &att_code, att_offset) != 0) 
          return (CW_ERR_READ_ATT);
        if (cw_lookup_att_code_name (att_code_name, att_code, attid) != 0)
          return (CW_ERR_ATT_VALUE);
        *lenp = strlen (att_code_name);
        break;  
      default:
        return (CW_ERR_INTERNAL);
    } /* switch */
  } /* if */

  return (CW_NOERR);				/* finish up */

} /* cw_inq_att */

/**********************************************************************/

int cw_inq_attid (int cwid, int varid, const char *name, int *attidp) {

/* Gets the attribute id. */

  if (cw_files[cwid] == NULL)			/* check file id */
    return (CW_ERR_DATASET_ID);

  if (varid == CW_GRAPHICS)			/* check var id */
    return (CW_ERR_ATT);
  if (varid != CW_DATA)
    return (CW_ERR_VAR_ID);

  if (cw_files[cwid]->data_id == -1)		/* check var defined */
    return (CW_ERR_VAR_ID);

  if (cw_lookup_attid (attidp, name) != 0)	/* lookup att id */
    return (CW_ERR_ATT);

  return (CW_NOERR);				/* finish up */

} /* cw_inq_attid */

/**********************************************************************/

int cw_put_att_text (int cwid, int varid, const char *name,
  size_t len, const char *tp) {

/* Puts the text attribute. */

int attid;
short att_code;

  if (cw_files[cwid] == NULL)			/* check file id */
    return (CW_ERR_DATASET_ID);

  if (cw_files[cwid]->wmode == CW_NOWRITE)	/* check file mode */
    return (CW_ERR_DATASET_RO);

  if (varid == CW_GRAPHICS)			/* check var id */
    return (CW_ERR_ATT);
  if (varid != CW_DATA)
    return (CW_ERR_VAR_ID);

  if (cw_files[cwid]->data_id == -1)		/* check var defined */
    return (CW_ERR_VAR_ID);

  if (cw_lookup_attid (&attid, name) != 0)	/* lookup att id */
    return (CW_ERR_ATT);

						/* check att mode */
  if (cw_attributes[attid].att_mode == CW_ATT_RO)
    return (CW_ERR_ATT_RO);

  if (cw_attributes[attid].att_type != CW_CHAR)	/* check att type */
    return (CW_ERR_ATT_TYPE);

						/* check att value */
  if (cw_lookup_att_code (&att_code, tp, attid) != 0)
    return (CW_ERR_ATT_VALUE);
    
  if (cw_put_att (cw_files[cwid]->fp, att_code,	/* put att */
    cw_attributes[attid].att_offset) != 0)
    return (CW_ERR_WRITE_ATT);

  return (CW_NOERR);				/* finish up */

} /* cw_put_att_text */

/**********************************************************************/

int cw_put_att_short (int cwid, int varid, const char *name,
  cw_type xtype, size_t len, const short *sp) {

/* Puts the integer attribute. */

int attid;
short att_code;

  if (cw_files[cwid] == NULL)			/* check file id */
    return (CW_ERR_DATASET_ID);

  if (len != 1)					/* check att length */
    return (CW_ERR_ATT_LEN);

  if (cw_files[cwid]->wmode == CW_NOWRITE)	/* check file mode */
    return (CW_ERR_DATASET_RO);

  if (varid == CW_GRAPHICS)			/* check var id */
    return (CW_ERR_ATT);
  if (varid != CW_DATA)
    return (CW_ERR_VAR_ID);

  if (cw_files[cwid]->data_id == -1)		/* check var defined */
    return (CW_ERR_VAR_ID);

  if (cw_lookup_attid (&attid, name) != 0)	/* lookup att id */
    return (CW_ERR_ATT);
  
						/* check att mode */
  if (cw_attributes[attid].att_mode == CW_ATT_RO)
    return (CW_ERR_ATT_RO);

  if (cw_attributes[attid].att_type != xtype)	/* check att type */
    return (CW_ERR_ATT_TYPE);

  switch (cw_attributes[attid].att_type) {	/* convert att value */
    case CW_SHORT:
      att_code = *sp;
      break;
    case CW_FLOAT:
      att_code = *sp * cw_attributes[attid].att_scale;
      break;
    case CW_CHAR:
      return (CW_ERR_ATT_TYPE);
    default:
      return (CW_ERR_INTERNAL); 
  } /* switch */
   
  if (cw_put_att (cw_files[cwid]->fp, att_code,	/* put att */
    cw_attributes[attid].att_offset) != 0)
    return (CW_ERR_WRITE_ATT);

  return (CW_NOERR);				/* finish up */

} /* cw_put_att_short */

/**********************************************************************/

int cw_put_att_float (int cwid, int varid, const char *name, 
  cw_type xtype, size_t len, const float *fp) {

/* Puts the float attribute. */

int attid;
short att_code;

  if (cw_files[cwid] == NULL)			/* check file id */
    return (CW_ERR_DATASET_ID);

  if (len != 1)					/* check att length */
    return (CW_ERR_ATT_LEN);

  if (cw_files[cwid]->wmode == CW_NOWRITE)	/* check file mode */
    return (CW_ERR_DATASET_RO);

  if (varid == CW_GRAPHICS)			/* check var id */
    return (CW_ERR_ATT);
  if (varid != CW_DATA)
    return (CW_ERR_VAR_ID);

  if (cw_files[cwid]->data_id == -1)		/* check var defined */
    return (CW_ERR_VAR_ID);

  if (cw_lookup_attid (&attid, name) != 0)	/* lookup att id */
    return (CW_ERR_ATT);
  
						/* check att mode */
  if (cw_attributes[attid].att_mode == CW_ATT_RO)
    return (CW_ERR_ATT_RO);

  if (cw_attributes[attid].att_type != xtype)	/* check att type */
    return (CW_ERR_ATT_TYPE);

  switch (cw_attributes[attid].att_type) {	/* convert att value */
    case CW_SHORT:
      return (CW_ERR_ATT_TYPE); 
    case CW_FLOAT:
      att_code = (short) roundf (*fp * cw_attributes[attid].att_scale);
      break;
    case CW_CHAR:
      return (CW_ERR_ATT_TYPE);
    default:
      return (CW_ERR_INTERNAL); 
  } /* switch */
   
  if (cw_put_att (cw_files[cwid]->fp, att_code,	/* put att */
    cw_attributes[attid].att_offset) != 0)
    return (CW_ERR_WRITE_ATT);

  return (CW_NOERR);				/* finish up */

} /* cw_put_att_float */

/**********************************************************************/

int cw_get_att_text (int cwid, int varid, const char *name, char *tp) {

/* Gets the text attribute. */

int attid;
short att_code, att_offset;

  if (cw_files[cwid] == NULL)			/* check file id */
    return (CW_ERR_DATASET_ID);

  if (varid == CW_GRAPHICS)			/* check var id */
    return (CW_ERR_ATT);
  if (varid != CW_DATA)
    return (CW_ERR_VAR_ID);

  if (cw_files[cwid]->data_id == -1)		/* check var defined */
    return (CW_ERR_VAR_ID);

  if (cw_lookup_attid (&attid, name) != 0)	/* lookup att id */
    return (CW_ERR_ATT);

						/* check att type */  
  if (cw_attributes[attid].att_type != CW_CHAR)
    return (CW_ERR_ATT_TYPE);

  att_offset = cw_attributes[attid].att_offset;	/* get att */
  if (cw_get_att (cw_files[cwid]->fp, &att_code, att_offset) != 0) 
    return (CW_ERR_READ_ATT);
  if (cw_lookup_att_code_name (tp, att_code, attid) != 0)
    return (CW_ERR_ATT_VALUE);

  return (CW_NOERR);				/* finish up */

} /* cw_get_att_text */

/**********************************************************************/

int cw_get_att_short (int cwid, int varid, const char *name, short *sp) {

/* Gets the integer attribute. */

int attid;
short att_code, att_offset;

  if (cw_files[cwid] == NULL)			/* check file id */
    return (CW_ERR_DATASET_ID);

  if (varid == CW_GRAPHICS)			/* check var id */
    return (CW_ERR_ATT);
  if (varid != CW_DATA)
    return (CW_ERR_VAR_ID);

  if (cw_files[cwid]->data_id == -1)		/* check var defined */
    return (CW_ERR_VAR_ID);

  if (cw_lookup_attid (&attid, name) != 0)	/* lookup att id */
    return (CW_ERR_ATT);

  att_offset = cw_attributes[attid].att_offset;	/* get att */
  if (cw_get_att (cw_files[cwid]->fp, &att_code, att_offset) != 0) 
    return (CW_ERR_READ_ATT);

  switch (cw_attributes[attid].att_type) {	/* convert att value */
    case CW_SHORT:
      *sp = att_code;
      break;
    case CW_FLOAT:
      return (CW_ERR_ATT_TYPE);
    case CW_CHAR:
      return (CW_ERR_ATT_TYPE);
    default:
      return (CW_ERR_INTERNAL); 
  } /* switch */

  return (CW_NOERR);				/* finish up */

} /* cw_get_att_short */

/**********************************************************************/

int cw_get_att_float (int cwid, int varid, const char *name, float *fp) {

/* Gets the float attribute. */

int attid;
short att_code, att_offset;

  if (cw_files[cwid] == NULL)			/* check file id */
    return (CW_ERR_DATASET_ID);

  if (varid == CW_GRAPHICS)			/* check var id */
    return (CW_ERR_ATT);
  if (varid != CW_DATA)
    return (CW_ERR_VAR_ID);

  if (cw_files[cwid]->data_id == -1)		/* check var defined */
    return (CW_ERR_VAR_ID);

  if (cw_lookup_attid (&attid, name) != 0)	/* lookup att id */
    return (CW_ERR_ATT);

  att_offset = cw_attributes[attid].att_offset;	/* get att */
  if (cw_get_att (cw_files[cwid]->fp, &att_code, att_offset) != 0) 
    return (CW_ERR_READ_ATT);

  switch (cw_attributes[attid].att_type) {	/* convert att value */
    case CW_SHORT:
      *fp = (float) att_code;
      break;
    case CW_FLOAT:
      *fp = (float) att_code / cw_attributes[attid].att_scale;
      break;
    case CW_CHAR:
      return (CW_ERR_ATT_TYPE);
    default:
      return (CW_ERR_INTERNAL); 
  } /* switch */

  return (CW_NOERR);				/* finish up */

} /* cw_get_att_float */

/**********************************************************************/
