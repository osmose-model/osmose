# osmose package: Modelling Marine Exploited Ecosystems

#' OSMOSE: Object-oriented Simulator of Marine biOdiverSity Exploitation
#' 
#' \tabular{ll}{ Package: \tab osmose\cr Type: \tab Package\cr Version: \tab
#' 0.1\cr Date: \tab 2016-02-01\cr License: \tab GPL-2\cr } 
#' 
#' @name osmose-package
#' @aliases osmose-package osmose
#' @docType package
#' @author Yunne-Jai Shin
#' @author Philippe Verley
#' @author Morgane Travers
#' @author Laure Velez
#' @author Ricardo Oliveros-Ramos 
#' Maintainer: Ricardo Oliveros-Ramos <ricardo.oliveros@@gmail.com>
#' @references osmose: Modelling Marine Exploited Ecosystems
#' @keywords modelling marine ecosystems
#' @examples
#' 
#' osmose()
#' 
NULL


# buildConfiguration ------------------------------------------------------
#' @title Build an OSMOSE configuration
#' @description This function create a valid configuration by several input files
#' from user input parameters.  
#' @param file Filename of the main configuration file
#' @param path Path for creating the input files, by default \code{"_osmose"}
#' @param config An \code{osmose.config} class object or a file path for an
#' osmose configuration file. This parameters will take precedence over the ones
#' specified in \code{file}.
#' @param absolute Boolean, use absolute paths relative to \code{file} to build the
#' configuration? If \code{FALSE}, relative paths are using for each individual
#' configuration file to parse its content.
#' @param newFile if \code{NULL}, the \code{file} provided is edited, otherwise
#' a new file is created with the modified configuration.
#' @details Basic configurations may not need the use of \code{buildConfiguration},
#' but it is required for configuration using interannual inputs or fishing selectivity.
#' @author Ricardo Oliveros-Ramos
#' @examples
#' \dontrun{
#' buildConfiguration("config.csv")
#' }
#' @export
buildConfiguration = function(file, path="_osmose", config=NULL, absolute=TRUE, newFile=NULL) {
  # read osmose parameters
  L1 = readOsmoseConfiguration(file=file, config=config, absolute=absolute)
  L1 = rapply(L1, .guessType, how = "list", keep.att=TRUE)
  
  outputPath = file.path(dirname(file), path)
  # create Time series files (output, configLines):
  # in a folder created at the same level
  # fishing
#   fishing = writeFishingFiles(L1, outputPath)
  # migration
  # plankton?
  # other
  # write additional config File
  # config.csv: one line added with new parameters  
  return(L1)
}


# runOsmose ---------------------------------------------------------------
#' @title Run an OSMOSE configuration
#' @description This function create a valid configuration by several input files
#' from user input parameters.  
#' @param file Filename of the main configuration file
#' a new file is created with the modified configuration.
#' @details Basic configurations may not need the use of \code{buildConfiguration},
#' but it is required for configuration using interannual inputs or fishing selectivity.
#' @author Ricardo Oliveros-Ramos
#' @export
runOsmose = function(osmose=NULL, java="java", input="input/config.csv", output="output/",
                     options=NULL, log="osmose.log", verbose=NULL, clean=TRUE) {
  
  if(is.null(verbose))  verbose = interactive()
  
  if(is.null(osmose) & interactive())  stop("No default OSMOSE java executable")
  if(is.null(osmose) & !interactive()) osmose = "osmose.jar"
  
  if(isTRUE(clean)) file.remove(file.path(output, dir(path=output, recursive=TRUE)))
  
  if(is.null(options)) options = ""
  
  run.osmose = paste(java, options, "-jar", osmose, input, output)
  if(!isTRUE(verbose)) run.osmose = paste(run.osmose, ">", log, "2>", log)
  
  system(run.osmose, wait=TRUE)
  
  return(invisible(run.osmose))
  
}


# osmose2R ----------------------------------------------------------------
#' @title Read OSMOSE outputs into an R object
#' @description This function create object of class \code{osmose} with the 
#' outputs from OSMOSE in the \code{path} folder.  
#' @param path Path to the directory containing OSMOSE outputs. 
#' @param version OSMOSE version used to run the model. 
#' @param species.names Display names for species, overwrite the species names
#' provided to the OSMOSE model. Used for plots and summaries.
#' @param ... Additional arguments
#' @details A list of class \code{osmose} is created, individual elements can be
#' extracted using the function \code{getVar}.
#' @author Ricardo Oliveros-Ramos
#' @author Laure Velez
#' @export
osmose2R =  function(path=NULL, version="v3r2", species.names=NULL, ...) {
  if(is.null(path) & interactive()) {
    path = choose.dir(caption="Select OSMOSE outputs folder")
  }
  if(is.null(path)) stop("No path has been provided.")
  
  output = switch(version, 
                  v3r0 = osmose2R.v3r0(path=path, species.names=species.names, ...),
                  v3r1 = osmose2R.v3r1(path=path, species.names=species.names, ...),
                  v3r2 = osmose2R.v3r2(path=path, species.names=species.names, ...),
                  stop(sprintf("Incorrect osmose version %s", version))
  )
  class(output) = "osmose"
  return(output)
}

