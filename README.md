<div align="center">
  <img src="http://documentation.osmose-model.org/_images/logo-osmose.svg">
</div>

OSMOSE: Modelling Marine Exploited Ecosystems
=============================================

<!-- [![CRAN_Status_Badge](https://www.r-pkg.org/badges/version/osmose)](https://cran.r-project.org/package=osmose) -->
[![DOI](https://zenodo.org/badge/48296200.svg)](https://zenodo.org/badge/latestdoi/48296200)
[![Latest Release](https://img.shields.io/github/release/osmose-model/osmose.svg)](https://github.com/osmose-model/osmose/releases)
[![R Build Status](https://github.com/osmose-model/osmose-private/workflows/r-build/badge.svg)](https://github.com/osmose-model/osmose-private/actions)
[![Java Build Status](https://github.com/osmose-model/osmose-private/workflows/java-build/badge.svg)](https://github.com/osmose-model/osmose-private/actions)
[![GitHub issues](https://img.shields.io/github/issues/osmose-model/osmose.svg)](https://github.com/osmose-model/osmose/issues)

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

## Using documentation plugins

Some documentation tools (Javadoc, PlantUML diagrams) can be generated using Maven plugins, which are defined in the `pom.xml` file.

### Building Javadoc

```
mvn javadoc:javadoc
```

The Javadoc will be stored on the `doc/_static/javadoc/apidocs/` folder.

### Generate PlantUML diagrams

To generate PlantUML diagrams for the full Osmose project:

```
mvn plantuml-generator:generate@osmose-full
```

The PlantUML diagram will be stored on `doc/_static/puml`

To convert the resulting diagram in an image format (SVG for instance), the [PlantUML](https://plantuml.com/fr/) tool is required. When
the diagram has been generated, type:

```
plantuml -tsvg doc/_static/puml/osmose-full.puml
```

## Acknowledgements

<div align="center">
<img src="https://osmose-model.org/wp-content/uploads/2020/10/logo_ird.png" height=60pt style="margin-right: 30px;">
<img src="https://osmose-model.org/wp-content/uploads/2020/10/logo-marbec-1024x549.png" height=60pt>
</div>
<br>
<div align="center">
<img src="https://osmose-model.org/wp-content/uploads/2020/10/h2020.png" height=50pt style="margin-right: 20px;">
<img src="https://osmose-model.org/wp-content/uploads/2020/10/belmont-forum-logos-partenaires-removebg-preview.png" height=60pt>
</div>
<br>

<div align="center">
<img src="https://osmose-model.org/wp-content/uploads/2020/10/biodiversa-logos-partenaires-removebg-preview.png" height=70pt style="margin-right: 30px;">
<img src="https://osmose-model.org/wp-content/uploads/2020/11/Institut_francais_de_recherche_pour_lexploitation_de_la_mer_logo.svg_-1024x190.png" height=30pt>
</div>
