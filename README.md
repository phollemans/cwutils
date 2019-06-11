# CoastWatch Utilities

The CoastWatch Utilities let you work with earth science data created by NOAA CoastWatch / OceanWatch, a program within 
the U.S. Department of Commerce.  You can easily view and convert data in various formats: HDF 4, NOAA 1b, and NetCDF 4
with CF metadata (the last of which is a generic format used by many organizations).

The code is written in Java and broken down into a set of command line tools for batch processing, and 
interactive tools that use Java/Swing for displaying data.  The main GUI tool is the CoastWatch Data Analysis 
Tool (CDAT).

The user's guide and installable builds of the CoastWatch Utilities for Linux, Windows, and Mac are distributed from the 
http://coastwatch.noaa.gov website, with beta and past builds available at http://www.terrenus.ca/download/cwutils. 

Currently OpenJDK 11 from http://openjdk.java.net is used for development and runtime.  The easiest way to start 
tinkering with the code and creating your own custom builds is to:

* Install a JDK on your local machine (11 or higher).
* Install the latest CoastWatch Utilities release from the CoastWatch or Terrenus site.  Note the installation directory, 
you'll need this later.
* Create a copy of this git repository on your local machine.
* Edit or add various Java source files in the src/ directory in your local copy.
* Run ant (currently tested with Ant 1.10.5) in the root direcotory.  This creates a new lib/java/cwutils.jar file.
* Copy lib/java/cwutils.jar to lib/java in your CoastWatch Utilities installation directory.
* Run whatever tool as normal in your installation, either using the command line launcher scripts or the GUI 
launchers under Windows and Mac.

Your new code should then be run by the launchers in the existing installation.  Some other useful ant commands (ant -p lists
all of these):

* ant api - Builds the Javadoc API and puts it into doc/api.  
* ant doc - Creates the Javadoc API, manual pages, and user's guide PDF file in doc/.  Requires that Latex is
installed on your machine.
* ant test-jar - Builds the new cwutils.jar file and copies it into directory named by the install.dir property in the
cwutils.properties file.  This is as easy way to compile and test repeatedly.
