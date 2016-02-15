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


How does the shortest path algorithm works
===============================================
Code first computes the edge strength between towers. To do this, first it computes the travel route of each user for each day.
If in the route two towers are adjacent, it computes the time delta of users between each tower and then computes the 
average time delta in seconds between two towers. A graph with vertices as towers and edge as the time delta between the
towers is constructed. Also for each tower the most frequent user is computed so that, you can use that
user to carry the parcel at that tower.

Dijkstr's algorithm is used to compute the shortest path.
