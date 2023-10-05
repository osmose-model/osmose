# Download requirements


```{contents} Table of Contents
:local:
```

## Download Java

Since Osmose numerical JAVA core is coded in JAVA, Java need to be installed. Beforehand, let us clarify some of the acronyms regarding the Java
technologies.

``JVM``: Java Virtual Machine. It is a set of software programs that interprets the Java byte code.

``JRE``: Java Runtime Environment. It is a kit distributed by Sun to execute Java programs. A ``JRE`` provides a ``JVM`` and some basic Java libraries. **A JRE is needed to run Osmose**. It can be downloaded from [https://www.java.com/fr/download/](https://www.java.com/fr/download/).

``JDK`` or ``SDK``: Java (or Software) Development Kit bound to the programmer. It provides a ``JRE``, a compiler, useful programs, examples and the source of the API (Application Programming Interface: some standard libraries). **A JDK is needed in order to modify the Osmose Java code**.

We strongly advise to use the OpenJDK suite, which is available at https://jdk.java.net/. Download the archive file (``.tar.gz`` for ``.zip`` extension) and extract the files in a given folder.

Then set the ``JAVA_HOME`` environment variable to the folder location.

:::{tip}
To see how to set environment variables in Windows, click
[here](https://docs.oracle.com/en/database/oracle/machine-learning/oml4r/1.5.1/oread/creating-and-modifying-environment-variables-on-windows.html#GUID-DD6F9982-60D5-48F6-8270-A27EC53807D0). For Linux/Mac Os users, click [here](https://www.serverlab.ca/tutorials/linux/administration-linux/how-to-set-environment-variables-in-linux/)
:::

## Download R

The Osmose-Java core is embedded in an Osmose-R package, which allows to pre-process, run and post-process Osmose outputs. Therefore, it is strongly advised that R be installed. Download instructions are available for [Windows](https://cran.r-project.org/bin/windows/base/), [Linux](https://cran.r-project.org/bin/linux/ubuntu/README.html) and [Mac Os X](https://cran.r-project.org/bin/macosx/).

It is also recommended to install the RStudio GUI ([https://rstudio.com/](https://rstudio.com/)).





## Defining the Osmose target directory

Since Osmose version 3.3.4, Java executables and demo configuration files have been moved out of the R build to meet CRAN requirements on size package. These files are now downloaded from the Internet and moved to a local directory.

By default, a temporary directory is used; but in this case, the Java code will be downloaded at each new session. To define a directory where to put these downloads, the user need to edit or create a ``~/.Renviron`` file and to define the {samp}``OSMOSE_DIR`` environment variable. More defined can be found on the [Osmose-R CRAN page](https://cran.r-project.org/web/packages/osmose/vignettes/create_run_read.html)
