How to install
=================

you can download the zip file from gip and unzip the folder

How to build (optional)
==================
install maven
https://maven.apache.org/install.html

Go to the folder parceldelivery where the zip file is extracted
run
mvn clean install


How to run
=================
1) install java 8 if you already don't have java
http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

2) copy input file to a location.

from command line
java -jar -Xmx2g parceldelivery-0.1.jar <input file location>
eg: on windows
java -jar -Xmx2g parceldelivery-0.1.jar C:\\projects\\upwork\\PhilipBanda\\cellphone.csv

Note - Depending on how much RAM you have in machine add corresponding -Xmx parametr. 
For example if you cn allocate 4g to the process type java -jar -Xmx4g parceldelivery-0.1.jar <input file location>