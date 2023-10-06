(osm-inst)=

# Installing Osmose

The Osmose model is provided as a combination of a Java numerical core (referred to as Osmose Java), associated with an Osmose R package (referred to as Osmose R). The code is available on a [GitHub repository](https://github.com/osmose-model/osmose).

It is strongly advised to install Osmose from the R, in order to have access to all the features provided
by the package. There are several possibilities to install Osmose from the Git repository.

```{contents} Table of Contents
:local:
:depth: 1
```

## Install using ``devtools``

It is possible to install the Osmose package using the ``install_github`` function of the ``devtools`` package.
First, open a R session (by typing ``R`` from a Terminal or CMD prompt). In the R console, type:

```R
library(devtools)
install_github("osmose-model/osmose/")
```

This will download the latest commit of the ``master`` branch and use it to compile the Osmose package.

:::{note}
The ``devtools`` package need to be installed
:::

## Install from RStudio

To install Osmose using RStudio, click on the {guilabel}`File --> New Project` menu and
open the {guilabel}`Version Control --> Git` menu.

Set the package URL to https://github.com/osmose-model/osmose.

When the project is opened, click on the
{guilabel}`Build & Reload` button to install the package.

:::{note}
The ``devtools`` package need to be installed.
:::

## Install from source files

To install the model from the source files, first download the code. You can either dowload it from the [Archive file](https://github.com/osmose-model/osmose/archive/refs/heads/master.zip) or via the ``git clone`` command, as follows:

```bash
git clone https://github.com/osmose-model/osmose.git
```

When the code has been downloaded, it must be installed as follows:

```bash
R CMD INSTALL osmose
```

:::{tip}
In the case of a manual install, we strongly advise to favor the ``git clone`` method. Indeed, update of the code
can be simply done by pulling changes from the remote folder.
:::

:::{danger}
Note that the code must be reinstalled using the ``R CMD INSTALL osmose`` command anytime the sources are updated.
:::

(java-executable)=

## Java executable

When installing the Osmose R package, no java executable is provided. The latter will be automatically downloaded by
the R package when the ``run_osmose`` package function is called.

The location where the executable files will be downloaded must be specified in a ``.Renviron`` file (see
[this article](https://support.posit.co/hc/en-us/articles/360047157094-Managing-R-with-Rprofile-Renviron-Rprofile-site-Renviron-site-rsession-conf-and-repos-conf) for a description of ``.Renviron``
file).

Create or edit the ``.Renviron`` file and set the ``OSMOSE_DIR`` variable to the destination folder:

```bash
OSMOSE_DIR=/home/barrier/Work/codes/osmose-executables
```

If the ``OSMOSE_DIR`` variables does not exist, the java executables will
be downloaded in a temporary folder. It will be downloaded anytime the ``run_osmose``
function is called. This is summarized in {numref}`mermaid-java-exe`.

Note that the Java executable are available on the [Releases](https://github.com/osmose-model/osmose/releases) section
of the Github repository. The Java executable files are named ``osmose-X.Y.Z-jar-with-dependencies.jar``,
with ``X.Y.Z`` the Osmose version. These Java executables contain all the external Java libraries required
by Osmose.

(mermaid-java-exe)=

```{mermaid}
:align: center
:caption: Process in the recovery of the Osmose Java executable for version X.Y.Z
graph TD;
    id0("run_osmose(vX.Y.Z) function called")
    id1{Is OSMOSE_DIR defined?}
    id2("Download executable for vX.Y.Z\n in temporary folder")
    id3("Download executable for vX.Y.Z\n in OSMOSE_DIR folder")

    id4{Does executable for vX.Y.Z exist?}
    id5(Use executable for vX.Y.Z)
    id0 --> id1
    id1 -->|no| id2
    id1 -->|yes| id4
    id4 -->|no| id3

    id3 --> id5
    id2 --> id5
    id4 -->|yes| id5

```

## Datarmor install

In this section, the easiest way to install Osmose on the Datarmor HPC is described.

### Set-up

From the Terminal, first load the necessary modules:

```bash
module load R/3.6.3-intel-cc-17.0.2.174
module load java/openjdk-16.0.2
module load nco
```

:::{warning}
To use parallel R features, this specific R module is required
:::

:::{note}
The ``nco`` module is needed to have access to the NetCDF library
:::

Edit the ``~/.Renviron`` file and define the ``R_LIBS_USER`` environment variables as follows:

```bash
R_LIBS_USER=/home1/datawork/nbarrier/libs/R/lib
```

It specifies the location where the R library will be insalled.

:::{caution}

When running a Job on Datarmor, you do not have access to the internet. Therefore, the Java executables cannot be downloaded as discussed in {numref}`java-executable`. Therefore, the ``OSMOSE_DIR`` environment variable is not required, since the Java code needs to be manually compiled, as discussed in {numref}`java-compile-datarmor`
:::

Next, install the required libraries as done in {numref}`sec-install-libraries`.

### Cloning the code

To clone the code, you will need to use the dedicated Conda environments. Create a `~/.condarc` file, which contains the following lines:

```bash
envs_dirs:
  - /home1/datahome/nbarrier/softwares/anaconda3-envs
  - /appli/conda-env
  - /appli/conda-env/2.7
  - /appli/conda-env/3.6
channels:
  - conda-forge
  - defaults
```

When done, type the following:

```bash
. /appli/anaconda/latest/etc/profile.d/conda.sh
```

Then, type:
```
conda activate git
```

This will give you access to a Git executable configured with the LFS support.

Finally, type the following:

```
git clone https://github.com/osmose-model/osmose.git
```

(java-compile-datarmor)=

### Compiling the code

To compile the code from Datarmor, you will to use the ``maven`` conda environment. To activate it:

```
. /appli/anaconda/latest/etc/profile.d/conda.sh
conda activate maven
```

Then, go in the ``osmose`` folder and type:

```
mvn package
```

The Java executable file will be created on the ``inst/java/`` folder of the ``osmose`` directory.