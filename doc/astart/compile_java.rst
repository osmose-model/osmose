(Optional) Compiling Osmose-Java
---------------------------------

**The compilation of Osmose-Java is not necessary to run Osmose, since Java excutables are provided in the package.** However, if the user wants to edit and recompile the Osmose-Java core, instructions are provided below.
 
Netbeans (Osmose <= 4.2.0)
##################################

Up to version 4.2.2, the only way to compile the Osmose Java core is by using the integrated development environment (IDE) Netbeans (`https://netbeans.org/downloads/ <https://netbeans.org/downloads/>`_)

.. note:: 

    It is highly advised to install together the JDK and the Netbeans bundle: `https://www.oracle.com/technetwork/java/javase/downloads/jdk-netbeans-jsp-3413139-esa.html <https://www.oracle.com/technetwork/java/javase/downloads/jdk-netbeans-jsp-3413139-esa.html>`_

The modification of the Java code is done as follows:

- Unzip one of the :samp:`.zip` file of the :samp:`osmose/java` directory
- Open Netbeans
- Click on :guilabel:`Open a project`
- Select the folder that has been extracted (should have a coffee cup icon)
- Edit the code
- Clean and build the project by pressing :guilabel:`Maj + F11`

This new :samp:`.jar` file can be used in the :samp:`run_osmose` function of the Osmose R package.

Maven (Osmose >= 4.3.0)
###################################

From version 4.3.0, the code can be compiled independently of the Netbeans IDE by using the Apache Maven software project management and comprehension tool (`https://maven.apache.org/index.html <https://maven.apache.org/index.html>`_). When Maven is installed: 

- Unzip one of the :samp:`.zip` file of the :samp:`osmose/java` directory
- Navigate to the directory via the Terminal (Linux/Mac) or Cmd (Windows) panel.
- Type :samp:`mvn install`
  
The sources will be built in the :guilabel:`target` directory.

.. note:: 

    Maven projects can also be built with Netbeans, Eclipse or Visual Studio
