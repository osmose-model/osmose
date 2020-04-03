# osmose package: Modelling Marine Exploited Ecosystems

#' OSMOSE: Modelling Marine Exploited Ecosystems
#' 
#' OSMOSE is a multispecies and Individual-based model (IBM) which 
#' focuses on fish species. This model assumes opportunistic predation based on 
#' spatial co-occurrence and size adequacy between a predator and its prey 
#' (size-based opportunistic predation). It represents fish individuals grouped 
#' into schools, which are characterized by their size, weight, age, taxonomy and 
#' geographical location (2D model), and which undergo major processes of fish 
#' life cycle (growth, explicit predation, natural and starvation mortalities, 
#' reproduction and migration) and a fishing mortality distinct for each species 
#' (Shin and Cury 2001, 2004). The model needs basic parameters that are often 
#' available for a wide range of species, and which can be found in FishBase 
#' for instance. This package provides tools to build a model and run simulations
#' using the OSMOSE model. See the 
#' \href{http://www.osmose-model.org/}{Official website} for more details.
#' 
#' \code{osmose} package is well documented by help descriptions, demos and 
#' vignettes.
#' 
#' \strong{Demo scripts:}
#' 
#' \code{# Check all the available topics}
#' 
#' \code{demo(package = "osmose")}
#' 
#' \code{# Select and run one of the topics (e.g. osmose.config_class)}
#' 
#' \code{demo(package = "osmose", topic = "osmose.config_class")}
#' 
#' \strong{Vignettes:}
#' 
#' \code{# Check all the available topics}
#' 
#' \code{vignette(package = "osmose")}
#' 
#' \code{# Select and run one of the topics (e.g. create_run_read)}
#' 
#' \code{vignette(package = "osmose", topic = "create_run_read")}
#' 
#' 
#' @name osmose-package
#' @aliases osmose-package osmose
#' @docType package
#' @author Yunne-Jai Shin
#' @author Ricardo Oliveros-Ramos 
#' @author Laure Velez
#' @author Criscely Luján
#' @author Philippe Verley
#' @author Maintainer: Nicolas Barrier <nicolas.barrier@ird.fr>
#' @references \href{http://www.osmose-model.org/}{Official website}
#' \href{https://documentation.osmose-model.org/index.html}{Documentation website}
#' @keywords modelling marine ecosystems
NULL