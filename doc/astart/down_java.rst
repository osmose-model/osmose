Setup of the Osmose environment
---------------------------------

Download Java
++++++++++++++++++++++++++++++++++

Since Osmose numerical JAVA core is coded in JAVA, Java need to be installed. Beforehand, let us clarify some of the acronyms regarding the Java
technologies.

:samp:`JVM`: Java Virtual Machine. It is a set of software programs that interprets the Java byte code.

:samp:`JRE`: Java Runtime Environment. It is a kit distributed by Sun to execute Java programs. A :samp:`JRE` provides a :samp:`JVM` and some basic Java libraries. **A JRE is needed to run Osmose**. It can be downloaded from `https://www.java.com/fr/download/ <https://www.java.com/fr/download/>`_.

:samp:`JDK` or :samp:`SDK`: Java (or Software) Development Kit bound to the programmer. It provides a :samp:`JRE`, a compiler, useful programs, examples and the source of the API (Application Programming Interface: some standard libraries). **A JDK is needed in order to modify the Osmose Java code**. 

Download R
+++++++++++++++++++++++++++

The Osmose-Java core is embedded in an Osmose-R package, which allows to pre-process, run and post-process Osmose outputs. Therefore, it is strongly advised that R be installed. Download instructions are available for `Windows <https://cran.r-project.org/bin/windows/base/>`_, `Linux <https://cran.r-project.org/bin/linux/ubuntu/README.html>`_ and `Mac Os X <https://cran.r-project.org/bin/macosx/>`_. 

It is also recommended to install the RStudio GUI (`https://rstudio.com/ <https://rstudio.com/>`_).

Defining the Osmose target directory
+++++++++++++++++++++++++++++++++++++

Since Osmose version 3.3.4, Java executables and demo configuration files have been moved out of the R build to meet CRAN requirements on size package. These files are now downloaded from the Internet and moved to a local directory. 

By default, a temporary directory is used; but in this case, the Java code will be downloaded at each new session. To define a directory where to put these downloads, the user need to edit or create a :samp:`~/.Renviron` file and to define the :samp:`OSMOSE_DIR` environment variable. More defined can be found on the `Osmose-R CRAN page <https://cran.r-project.org/web/packages/osmose/vignettes/create_run_read.html>`_
