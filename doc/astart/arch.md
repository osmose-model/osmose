# Code architecture

## Osmose \<= 4.2.0

For Osmose \<= 4.2.0, the directory contains the following folders, related to the R package

- The `inst/java` contains the Osmose Java core, provided as `.jar`.
- The `java` directory contains the Osmose Java source files associated with the `.jar` files.
- The `R` folder contains the R functions (pre/post processing tools, call to the JAVA core, etc.)
- The `data-raw` directory contains input files that can be used to run the model for the first time.
- The `man` and `vignettes` directories contain help files.

## Osmose >= 4.3.0

For Osmose >= 4.3.0, the directory also contains the Java source files and compilation tools of the Osmose-Java core:

- The `src` directory contains the Java source files of the current Osmose version.
- The `.xml` files are Maven compilation files.
- The `local` directory contains external Java libraries needed by Osmose
