<div align="center">
  <img src="http://documentation.osmose-model.org/_images/logo-osmose.svg">
</div>

OSMOSE: Modelling Marine Exploited Ecosystems
=============================================

[![CRAN_Status_Badge](http://www.r-pkg.org/badges/version/osmose)](http://cran.r-project.org/package=osmose)
[![Github Issues](http://githubbadges.herokuapp.com/osmose-model/osmose/issues.svg?style=flat-square)](https://github.com/osmose-model/osmose/issues)
[![](http://cranlogs.r-pkg.org/badges/osmose)](http://cran.rstudio.com/web/packages/osmose/index.html)

## Overview

OSMOSE is a multispecies and Individual-based model (IBM) which focuses on fish species. This model assumes opportunistic predation based on spatial co-occurrence and size adequacy between a predator and its prey (size-based opportunistic predation). It represents fish individuals grouped into schools, which are characterized by their size, weight, age, taxonomy and geographical location (2D model), and which undergo major processes of fish life cycle (growth, explicit predation, natural and starvation mortalities, reproduction and migration) and fishing exploitation. The model needs basic biological parameters that are often available for a wide range of species, and which can be found in FishBase for instance, and fish spatial distribution data. This package provides tools to build a model and run simulations using the OSMOSE model. See [http://www.osmose-model.org/](http://www.osmose-model.org/) for more details.

## Installation

``` r
# The easiest way to get osmose is from CRAN:
install.packages("osmose")

# Or the development version from GitHub:
# install.packages("devtools")
devtools::install_github("osmose-model/osmose")
```

## Documentation and usage

`osmose` includes several ways to get help and test his functions: demo scripts, vignettes and help files.

### Help files

In order to get information about any function, the user just have to ask it by `?` command:

``` r
# Help file of read_osmose function
?read_osmose

# Help file of available plot methods
?plot.osmose
```

### Demo scripts

The users can test the main functions by using demo scripts (embedded on the package root). In order to access to them, they need to run the demo command:
``` r
# Check all the available topics
demo(package = "osmose")

# Select and run one of the topics (e.g. osmose.config_class)
demo(package = "osmose", topic = "osmose.config_class")
```

### Vignettes

Vignettes can be a good simple way to review all the main functions, because they will be showed as a html. The commands to call are pretty similar to demo scripts:

``` r
# Check all the available topics
vignette(package = "osmose")

# Select and run one of the topics (e.g. osmose.config_class)
vignette(package = "osmose", topic = "create_run_read")
```

## References

[Official website](http://www.osmose-model.org/) of the model, including info about the development of the project as well as references.
[Documentation website](https://documentation.osmose-model.org/index.html) with information of parameters of java model (the core).
[Github site](https://github.com/osmose-model/osmose) where the development code is placed.
