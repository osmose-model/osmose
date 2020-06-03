# Local repository

## Description

This is the local maven dependencies repository.
All locals libraries is contained in this folder like a maven artifactory.

## How it work ?

The project link this repository in the POM file with the line :

```
<repository>
    <id>project.local</id>
    <name>project</name>
    <url>file:${project.basedir}/local/</url>
</repository>
```

When the project download the specified dependencies, it check this folder and
copy all required library to the global .m2 folder.

Il you want to add a new local library, you can use this repository.
But it's more useful to separate your project in a sub project or to deploy it
to an externel repository.

## How to add a new library ?

The process I have used is indicated at this page : 
[sookocheff.com](https://sookocheff.com/post/java/local-maven-repository/)

The goal is to create a maven project from a jar file and place it in this folder, 
I have used this command below to generate the ml.options dependency :

mvn deploy:deploy-file -Durl=file:/home/amap/localrepo/ 
-Dfile=/home/amap/localrepo/options.jar -DgroupId=ml.options -DartifactId=options 
-Dpackaging=jar -Dversion=1.0.0

I Hope that was usefull to you, good luck ;)

Tristan Muller