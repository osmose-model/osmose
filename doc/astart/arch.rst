Code architecture
-----------------------

Osmose <= 4.2.0
@@@@@@@@@@@@@@@@@@@@

For Osmose <= 4.2.0, the directory contains the following folders, related to the R package

- The :samp:`inst/java` contains the Osmose Java core, provided as :samp:`.jar`.
- The :samp:`java` directory contains the Osmose Java source files associated with the :samp:`.jar` files.
- The :samp:`R` folder contains the R functions (pre/post processing tools, call to the JAVA core, etc.)
- The :samp:`data-raw` directory contains input files that can be used to run the model for the first time.
- The :samp:`man` and :samp:`vignettes` directories contain help files.

Osmose >= 4.3.0
@@@@@@@@@@@@@@@@@@@@

For Osmose >= 4.3.0, the directory also contains the Java source files and compilation tools of the Osmose-Java core:

- The :samp:`src` directory contains the Java source files of the current Osmose version.
- The :samp:`.xml` files are Maven compilation files.
- The :samp:`local` directory contains external Java libraries needed by Osmose
