(osm-inst)=

# Installing Osmose

The Osmose model is provided as a combination of a Java numerical core (referred to as Osmose Java), associated with an Osmose R package (referred to as Osmose R). The code is available on a [GitHub repository](https://github.com/osmose-model/osmose).

## Osmose without R

In order to use Osmose without R, the only thing to do is to
download the Java Osmose binary files.

They are available on the [Releases](https://github.com/osmose-model/osmose/releases) web page of the Github repository. The Java executable files are named ``osmose-X.Y.Z-jar-with-dependencies.jar``, with ``X.Y.Z`` the Osmose version. These Java executables contain all the external Java libraries required by Osmose and.

## Osmose from R

In order to use Osmose from the R package, you need to install the Osmose package. This can be in several ways.

### Install from GitHub

It is possible to install the Osmose package using the ``install_github``
function as follows:

```R
library(devtools)
install_github("osmose-model/osmose/")
```

This will download the latest commit of the ``master`` branch and use it to compile the Osmose package.

### Install from sources

To install the




## Manual install

In order to use the development version of the Osmose model, it must be installed manually

### Install from source files

The first way is to clone or download the source code and to install the code manually. To clone the directory, type in a Terminal:

To clone the Osmose repository:

```bash
# using HTTPS:
git clone https://github.com/osmose-model/osmose-private.git

# using SSH
git clone git@github.com:osmose-model/osmose-private.git
```

When a new version of the code is released, it can be updated as follows:

```bash
git pull
```

:::{note}
When using the SSH, it is necessary to generate a RSA key that will connect the computer and the remote repository
(see [Github Help](https://help.github.com/en/github/authenticating-to-github/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent) for details)
:::

When the code has been downloaded, it must be installed as follows:

```bash
R CMD INSTALL osmose
```

:::{warning}
The code must be reinstalled after each upgrade
:::

### Using devtools or RStudio

The Osmose package can also be by using the `devtools` R package or RStudio.

### devtools

Open a R session and type the following lines:

```R
library("devtools")
install_github("osmose-model/osmose")
```

### RStudio

To install Osmose using RStudio, click on the {guilabel}`File --> New Project` menu and
open the {guilabel}`Version Control --> Git` menu.
Set the package URL ([https://github.com/osmose-model/osmose-private.git](https://github.com/osmose-model/osmose)). When the project is opened, click on the
{guilabel}`Build & Reload` button to install the package.
