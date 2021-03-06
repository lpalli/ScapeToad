Welcome to ScapeToad
====================


ScapeToad is an easy to use Java application for cartogram creation. A cartogram is a 
transformed map where the polygons are proportional to the value of a given statistical 
variable, e.g. the population. More information is available on the project web site at 
chorogram.choros.ch/scapetoad or on the Github project site.


Requirements
------------

ScapeToad is an application which requires quite big computation resources. We have made quite an 
effort to make the cartogram making accessible. However, ScapeToad needs at least 512 MB of memory. 
If you want to produce high quality cartograms, it is preferable to have a fast processor. ScapeToad 
needs at least Java 1.4.


License
-------

ScapeToad is an open-source project released under the GNU Public License. You can find the complete 
license in the separate LICENSE.txt file.


Contents of this package
------------------------

Even if ScapeToad is a Java application, we have decided to release three different versions. This 
is due to the quite big memory requirement for the application. There is an application package for 
Mac OS X, an executable file for Windows, and a multi-platform JAR file with a startup script for 
the Unix platform.


Compilation
-----------

Compile process should be straightforward by using Ant. With Ant installed, simply cd to
the src directory and run ant. Resulting JAR file will be in dist directory.

There is also a jsmooth project file inside win directory for creating a Windows 
executable using JSmooth (http://jsmooth.sourceforge.net/).


Installation
------------

Mac OS X: Just drag the ScapeToad.app file inside the OSX folder to your hard drive. Normally, 
programs should be installed into the Applications folder at the root level of your hard drive. 
But this is not a pre-requisite for running ScapeToad.

Windows : Just drag the ScapeToad.exe file inside the Windows folder on your hard drive. Normally, programs should be installed into the Program files folder on the C: drive, but this is not a pre-requisite for running ScapeToad.

Unix : For the Unix platform, and other platforms not mentioned, there is a multi-platform JAR file in the Unix folder. Install the program by copying the program with the startup script at your preferred location. You can start this applications using the launch script ScapeToad.sh like this :

cd /your/install/folder/
./ScapeToad.sh

or by launching directly the JAR file like this :

java -Xmx512m -jar /your/install/folder/ScapeToad.jar

 

Christian Kaiser - Project administrator
chri.kais@gmail.com
Last updated: 5 June 2012