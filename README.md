# The CoastWatch Utilities

The CoastWatch Utilities let you work with earth science data created by NOAA CoastWatch / OceanWatch, a program within 
the U.S. Department of Commerce.  You can easily view and convert data in various formats: HDF 4, NOAA 1b, and NetCDF 4
with CF metadata (the last of which is a generic format used by many organizations).

The code is written in Java and broken down into a set of command line tools for batch processing, and 
interactive tools that use Java/Swing for displaying data.  The main GUI tool is the CoastWatch Data Analysis 
Tool (CDAT).

The user's guide and installable builds of the CoastWatch Utilities for Linux, Windows, and Mac are distributed from the 
http://coastwatch.noaa.gov website, with beta and past builds available at http://www.terrenus.ca/download/cwutils. 

# Development Setup

Currently OpenJDK 11 from http://openjdk.java.net is used for development and the Java runtime.  The easiest way to start 
tinkering with the code and creating your own custom builds is to:

* Install a JDK on your local machine (11 or higher).
* Install the latest CoastWatch Utilities release from the CoastWatch or Terrenus site.  Note the installation directory, 
you'll need this later.
* Create a copy of this git repository on your local machine.
* Edit or add various Java source files in the src/ directory in your local copy.
* Type `ant` (currently tested with Ant 1.10.5) in the root direcotory.  This creates a new lib/java/cwutils.jar file.
* Copy lib/java/cwutils.jar to lib/java in your CoastWatch Utilities installation directory.
* Run the tool you've modified as normal in your installation, either using the command line launcher scripts or the GUI 
launchers under Windows and Mac.

Your new code will then be picked up and run by the launchers in the existing installation.  Some other useful 
Ant targets (`ant -p` lists all possible build targets):

* `ant api` - Builds the Javadoc API and puts it into doc/api.  
* `ant doc` - Creates the Javadoc API, manual pages, and user's guide PDF file in doc/.  Requires that Latex is
installed on your machine.  You may need to adjust the paths at the top of scripts/make_docs.sh to help find various
programs.
* `ant test-jar` - Builds the new cwutils.jar file and copies it into directory named by the install.dir property in the
cwutils.properties file.  This is as easy way to compile and test repeatedly.
* `ant test-cdat` - Builds the new cwutils.jar file and runs CDAT using only the development environment (no other CoastWatch 
Utilities installation is required).  You may need to modify the cwutils.properties file to help the Java VM find the 
correct native library linker variable and path for your machine, or you'll see native library linking errors.

# Creating Installation Packages

If you want to create your own installation packages rather than piggybacking off an existing installation as mentioned above, 
you'll need to obtain a license for the install4j software used to create Java program launchers and installation packages 
from ej-technologies available at https://www.ej-technologies.com/products/install4j/overview.html, or possibly some 
other similar software.  The CoastWatch Utilities installation packages are created using an open source non-profit 
license generously donated by ej-technologies.  To create packages using Ant:

* Modify the install4j.dir path in the cwutils.properties file to your install4j installation path.
* Modify the pkg.dir path to your preferred papckage destination.
* Type `ant packages` to build the default packages: Linux, Windows, and Mac installations.
* Type `ant packages -Dbuilds=IDS` where IDS are comma-separated package IDs that you want: linux64, linux64.novm, 
windows64, maxosx64.

# Support

General comments and questions on using the software should go to coastwatch.info@noaa.gov.  Specific software development
questions and bug/crash reports can be posted here.  Also we welcome Java developers to contribute to the project -- please
feel free to ask for help with the API if you're writing your own code and/or provide code improvements as you see them.

The user's guide in the installation directory and linked from http://coastwatch.noaa.gov contains all the information you'll 
need for using the software, and a primer for software developers as well to write your own programs that use the API.

# License

The doc/ directory contains a copy of this license statement:

CoastWatch Software Library and Utilities<br>
Copyright (c) 1998-2019 National Oceanic and Atmospheric Administration<br>
All rights reserved.

Developed by:<br>
CoastWatch / OceanWatch<br>
Center for Satellite Applications and Research<br>
http://coastwatch.noaa.gov

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the "Software"),
to deal with the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

* Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimers.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimers in the documentation
  and/or other materials provided with the distribution.

* In addition, redistributions of modified forms of the source or binary
  code must carry prominent notices stating that the original code was
  changed and the date of the change.

* Neither the names of CoastWatch / OceanWatch, Center for Satellite
  Applications and Research, nor the names of its contributors may be used
  to endorse or promote products derived from this Software without specific
  prior written permission.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
THE CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS WITH THE SOFTWARE.








