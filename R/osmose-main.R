
# osmose: main functions --------------------------------------------------

# runOsmose ---------------------------------------------------------------
#' @title Run an OSMOSE configuration
#' @description This function create a valid configuration by several input files
#' from user input parameters. 
#'
#' @param input Filename of the main configuration file
#' @param parameters Parameters to be passed to osmose (version 4 or higher).
#' @param output Output directory. If NULL, the value set in the configuration file is used.
#' @param log File to save OSMOSE execution messages.
#' @param version OSMOSE version. Integer (2, 3, etc.) or releases ('v3r2') are
#' accepted. 
#' @param osmose Path to a OSMOSE .jar executable. By default (NULL), uses the stable 
#' jar for the current version.
#' @param java Path to the java executable. The default assumes 'java' is 
#' on the search path. 
#' @param options Java options (e.g. -Xmx2048m to increase memory limit).
#' @param verbose Show messages? (output in the log file if FALSE).
#' @param clean TRUE if the output directory should be cleaned before running OSMOSE.
#'
#' @details Basic configurations may not need the use of \code{buildConfiguration},
#' but it is required for configuration using interannual inputs or fishing selectivity.
#' @author Ricardo Oliveros-Ramos
#' @examples{
#'   \donttest{
#'     filename = system.file("extdata", "gog/osm_all-parameters.csv", package="osmose")
#'     runOsmose(filename)
#'   }
#' }
#' @export
run_osmose = function(input, parameters = NULL, output = NULL, log = "osmose.log",
                      version = "4.1.0", osmose = NULL, java = "java",
                      options = NULL, verbose = TRUE, clean = TRUE) {
  
  if(isTRUE(verbose)) message(sprintf("This is OSMOSE version %s", version))
  
  # update to provide by release executables
  if(is.null(osmose)) osmose = system.file(sprintf("java/osmose_%s.jar", version),
                                           package="osmose", mustWork = TRUE)
  
  if(isTRUE(clean) & (!is.null(output))) 
    file.remove(file.path(output, dir(path=output, recursive=TRUE)))
  
  if(is.null(options)) options = ""
  if(is.null(parameters)) parameters = ""
  
  # barrier.n: redirection 
  version = .getVersion(version)
  versionRef = .getVersion("3.4")
  
  if(is.null(output)) {
    # If output is NULL, file output path is used.
    outDir = ""
  } else {
    # else, overwrites the Osmose output parameter
    if(.compareVersion(version, versionRef) < 0) {
      outDir = output
    } else {
      # changes for version 4 or higher
      outDir = paste("-Poutput.dir.path=", output, sep="")
    }
  }
  
  args = paste(options, "-jar", osmose, input, outDir, parameters)
  
  stdout = ifelse(interactive() & verbose, "", log)
  stderr = ifelse(interactive() & verbose, "", log)
  
  command = paste(c(shQuote(java), args), collapse = " ")
  
  if(isTRUE(verbose)) message(sprintf("Running: %s", command))
  
  system2(java, args=args, stdout=stdout, stderr=stderr, wait=TRUE)
  
  return(invisible(command))
  
}

#' Title
#' @export
runOsmose = function(input, parameters=NULL, output="output", log="osmose.log",
                     version="4.1.0", osmose=NULL, java="java", 
                     options=NULL, verbose=TRUE, clean=TRUE) {
  
  message("runOsmose will be deprecated, use run_osmose instead.")
  
  run_osmose(input = input, parameters = parameters, output = output,
             log = log, version = version, osmose = osmose, java = java, 
             options = options, verbose = verbose, clean = clean) 
}


# read_osmose -------------------------------------------------------------
#' @title Read OSMOSE outputs into an R object
#' @description This function create object of class \code{osmose} with the 
#' outputs from OSMOSE in the \code{path} folder.  
#' @param path Path to the directory containing OSMOSE outputs. 
#' @param input Path to a main osmose configuration file.
#' @param version OSMOSE version used to run the model. 
#' @param species.names Display names for species, overwrite the species names
#' provided to the OSMOSE model. Used for plots and summaries.
#' @param absolute Whether the path is absolute (TRUE) or relative (FALSE). Only used if
#' input is not NULL
#' @param ... Additional arguments
#' @details A list of class \code{osmose} is created, individual elements can be
#' extracted using the function \code{getVar}.
#' @author Ricardo Oliveros-Ramos
#' @author Laure Velez
#' @export
#' @examples{
#'   outdir = system.file("extdata", "outputs/", package="osmose")
#'   read_osmose(outdir)
#' }
#' @aliases osmose2R
read_osmose =  function(path=NULL, input=NULL, version=NULL, species.names=NULL, absolute=TRUE, ...) {
  
  if(is.null(path) & is.null(input)) stop("No output or configuration path has been provided.")
  
  config =  if(!is.null(input)) readOsmoseConfiguration(file=input, absolute=absolute) else NULL
  
  if(is.null(path)) return(config)
  if(is.null(version)) version = "v3r2"
  
  if(!dir.exists(path)) stop("The output directory does not exist.")
  
  output = switch(version, 
                  v3r2 = osmose2R.v3r2(path=path, species.names=species.names, ...),
                  v3r1 = osmose2R.v3r1(path=path, species.names=species.names, ...),
                  v3r0 = osmose2R.v3r0(path=path, species.names=species.names, ...),
                  stop(sprintf("Incorrect osmose version %s", version))
  )
  
  output = c(output, config=list(config))
  
  class(output) = "osmose"
  
  return(output)
  
}

# to keep back compatibility for a while
#' @export
osmose2R = function(path=NULL, version="v3r2", species.names=NULL, ...) {
  
  .Deprecated("read_osmose")
  read_osmose(path=path, version=version, species.names=species.names, ...)
  
}


## buildConfiguration ------------------------------------------------------
## @title Build an OSMOSE configuration
## @description This function create a valid configuration by several input files
## from user input parameters.  
## @param file Filename of the main configuration file
## @param path Path for creating the input files, by default \code{"_osmose"}
## @param config An \code{osmose.config} class object or a file path for an
## osmose configuration file. This parameters will take precedence over the ones
## specified in \code{file}.
## @param absolute Boolean, use absolute paths relative to \code{file} to build the
## configuration? If \code{FALSE}, relative paths are using for each individual
## configuration file to parse its content.
## @param newFile if \code{NULL}, the \code{file} provided is edited, otherwise
## a new file is created with the modified configuration.
## @details Basic configurations may not need the use of \code{buildConfiguration},
## but it is required for configuration using interannual inputs or fishing selectivity.
## @author Ricardo Oliveros-Ramos
## @usage buildConfiguration(file="config.csv", path="_osmose", 
##                           config=NULL, absolute=TRUE, 
##                           newFile=NULL)
#buildConfiguration = function(file="config.csv", path="_osmose", config=NULL, absolute=TRUE, newFile=NULL) {
#  # read osmose parameters
#  L1 = readOsmoseConfiguration(file=file, config=config, absolute=absolute)
#  L1 = rapply(L1, .guessType, how = "list", keep.att=TRUE)
#  
#  outputPath = file.path(dirname(file), path)
#  # create Time series files (output, configLines):
#  # in a folder created at the same level
#  # fishing
#  #   fishing = writeFishingFiles(L1, outputPath)
#  # migration
#  # plankton?
#  # other
#  # write additional config File
#  # config.csv: one line added with new parameters  
#  return(L1)
#}

#' @title Report method
#' @description This function built a report for each class including on osmose package.
#' @param object Object of class \code{osmose}.
#' @param format The format to export the report.
#' @param output Folder where the report will be saved.
#' @param ... Extra arguments passed to \code{\link{report}} function.
#' @return A report on specific format.
#' @export
report = function(x, format, output, ...) {
  UseMethod("report")
}


#' Generates Osmose configuration files to run an Osmose demo.
#' 
#' @param path Path where to put the Osmose configuration file.
#' @param config Reference configuration to run ("gog"). 
#' @note So far, only one configuration is propose ("gog")
#' 
#'
#' @return A list containing the configuration file to use (config_file) for running the code
#' and the output directory to use when reading data.
#' 
#' @export
#' @examples
#' \dontrun{
#' rm(list=ls())
#'
#'library("osmose")
#'
#'# Copy configuration files into the proper directory
#'demo = osmose_demo(path="../", config="gog")
#'
#'# run the osmose model
#'run_osmose(demo$config_file, parameters=NULL, output=NULL, version="3.3.3", 
#'           options=NULL, verbose=TRUE, clean=TRUE)
#'
#'# reads output data
#'data = read_osmose(demo$output_dir)
#'
#'# summarize output data
#'summary(data)
#'
#'# plot output data
#'plot(data)
#'}
osmose_demo = function(path=NULL, config=c("gog", "gog_v4", "default")) {
  
  config = match.arg(config)
  
  # if no path has been provided, create a path from the working dir.
  if(is.null(path)) path = file.path(getwd(), config)
  
  # if the directory does not exist, then create
  # the directory
  if(!dir.exists(path)) {
    dir.create(path)
  }
  
  # swith for the configuration directory
  input_dir = switch(config, 
                     gog = system.file(package="osmose", "extdata", "gog"),
                     gog_v4 = system.file(package="osmose", "extdata", "gog_v4"),
                     default = system.file(package="osmose", "extdata", "gog")
  )
  
  # swith for the configuration directory and defines the configuration file
  config_file = switch(config, 
                       gog = "osm_all-parameters.csv",
                       gog_v4 = "osm_all-parameters.csv",
                       default = "osm_all-parameters.csv"
  )
  
  file.copy(from=input_dir, to=path, recursive=TRUE, overwrite = FALSE)
  config = basename(input_dir)
  
  demo = list()
  config_file = file.path(path, config, config_file)
  demo$config_file = config_file
  demo$output_dir = file.path(dirname(config_file), "output")
  
  return(demo)
  
}







#' Generates Osmose configuration files to run an Osmose demo.
#' 
#' @param path Path where to put the Osmose configuration file.
#' @param config Reference configuration to run ("gog"). 
#' @note So far, only one configuration is propose ("gog")
#' 
#' @return A list containing the configuration file to use (config_file) for running the code
#' and the output directory to use when reading data.
#' 
#' @export
#' @examples
#' \dontrun{
#' rm(list=ls())
#'
#'library("osmose")
#'
#'# Copy configuration files into the proper directory
#'demo = osmose_demo(path="../", config="gog")
#'
#'# run the osmose model
#'run_osmose(demo$config_file, parameters=NULL, output=NULL, version="3.3.3", 
#'           options=NULL, verbose=TRUE, clean=TRUE)
#'
#'# reads output data
#'data = read_osmose(demo$output_dir)
#'
#'# summarize output data
#'summary(data)
#'
#'# plot output data
#'plot(data)
#'}
osmose_calib_demo = function(path=NULL) {
  
  # if no path has been provided, create a path from the working dir.
  if(is.null(path)) path = file.path(getwd())
  
  # if the directory does not exist, then create
  # the directory
  if(!dir.exists(path)) {
    dir.create(path)
  }
  
  # copy the calibration data into the path directory
  input_dir = system.file(package="osmose", "extdata", "calib_demo")
  file.copy(from=input_dir, to=path, recursive=TRUE, overwrite = TRUE)

  # Copy the reference gog_v4 configuration in the calibration folder
  input_dir = system.file(package="osmose", "extdata", "gog_v4")
  file.copy(from=input_dir, to=file.path(path, "calib_demo"), recursive=TRUE, overwrite = TRUE)
            
  demo = list(path=file.path(path, "calib_demo"))
  demo$file = "calibrate.R"
  
  return(demo)

}






























