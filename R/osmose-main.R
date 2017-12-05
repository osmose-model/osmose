
# osmose: main functions --------------------------------------------------

# runOsmose ---------------------------------------------------------------
#' @title Run an OSMOSE configuration
#' @description This function create a valid configuration by several input files
#' from user input parameters. 
#' @param input Filename of the main configuration file
#' @param parameters Parameters to be passed to osmose (version 4 or higher).
#' @param output Output directory
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
#' @details Basic configurations may not need the use of \code{buildConfiguration},
#' but it is required for configuration using interannual inputs or fishing selectivity.
#' @author Ricardo Oliveros-Ramos
#' @export
runOsmose = function(input, parameters=NULL, output="output", log="osmose.log",
                     version=3, osmose=NULL, java="java", 
                     options=NULL, verbose=TRUE, clean=TRUE) {
  
  # barrier.n: redirection 
  
  version = .getVersion(version)
  
  if(isTRUE(verbose)) message(sprintf("This is OSMOSE version %s", version))
  
  # update to provide by release executables
  if(is.null(osmose)) osmose = system.file(sprintf("osmose_stable_%s.jar", version),
                                            package="osmose", mustWork = TRUE)
  
  if(isTRUE(clean)) 
    file.remove(file.path(output, dir(path=output, recursive=TRUE)))
  
  if(is.null(options)) options = ""
  if(is.null(parameters)) parameters = ""
  
  if(version > 3) {
    # changes for version 4 or higher
    outDir = paste("-Poutput.dir.path=", output, sep="")
  } else {
    outDir = output
  }
  
  args = paste(options, "-jar", osmose, input, outDir, parameters)
  
  stdout = ifelse(interactive() & verbose, "", log)
  stderr = ifelse(interactive() & verbose, "", log)
  
  command = paste(c(shQuote(java), args), collapse = " ")
  
  if(isTRUE(verbose)) message(sprintf("Running: %s", command))
  
  system2(java, args=args, stdout=stdout, stderr=stderr, wait=TRUE)
  
  return(invisible(command))
  
}


# read_osmose -------------------------------------------------------------
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
#' @aliases osmose2R
read_osmose =  function(path=NULL, version="v3r2", species.names=NULL, ...) {
  if(is.null(path) & interactive()) {
    path = readline(prompt="Select OSMOSE outputs folder")
  }
  if(!dir.exists(path)) stop("The input directory does not exist.")
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

# to keep back compatibility for a while
#' @export
osmose2R = function(path=NULL, version="v3r2", species.names=NULL, ...) {
  
  .Deprecated("read_osmose")
  read_osmose(path=path, version=version, species.names=species.names, ...)

}


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



