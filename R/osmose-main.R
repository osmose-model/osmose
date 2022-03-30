# OSMOSE (Object-oriented Simulator of Marine Ecosystems)
# http://www.osmose-model.org
#
# Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
#
# Osmose is a computer program whose purpose is to simulate fish
# populations and their interactions with their biotic and abiotic environment.
# OSMOSE is a spatial, multispecies and individual-based model which assumes
# size-based opportunistic predation based on spatio-temporal co-occurrence
# and size adequacy between a predator and its prey. It represents fish
# individuals grouped into schools, which are characterized by their size,
# weight, age, taxonomy and geographical location, and which undergo major
# processes of fish life cycle (growth, explicit predation, additional and
# starvation mortalities, reproduction and migration) and fishing mortalities
# (Shin and Cury 2001, 2004).
#
# Contributor(s):
# Yunne SHIN (yunne.shin@ird.fr),
# Morgane TRAVERS (morgane.travers@ifremer.fr)
# Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
# Philippe VERLEY (philippe.verley@ird.fr)
# Laure VELEZ (laure.velez@ird.fr)
# Nicolas Barrier (nicolas.barrier@ird.fr)
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation (version 3 of the License). Full description
# is provided on the LICENSE file.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.


# osmose: main functions --------------------------------------------------

# run_osmose --------------------------------------------------------------
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
#' @param force TRUE if an outdated configuration can be run without being updated.
#'
#' @details Basic configurations may not need the use of \code{buildConfiguration},
#' but it is required for configuration using interannual inputs or fishing selectivity.
#' @author Ricardo Oliveros-Ramos
#' @examples{
#'   \dontrun{
#'     path = cacheManager("eec-4.3.0")
#'     filename = file.path(path, "osm_all-parameters.csv")
#'     run_osmose(filename)
#'   }
#' }
#' @export
run_osmose = function(input, parameters = NULL, output = NULL, log = "osmose.log",
                      version = NULL, osmose = NULL, java = "java",
                      options = NULL, verbose = TRUE, clean = TRUE, force = FALSE){
                        
  package_version = packageVersion("osmose")
  
  # Print message with version
  # if version argument is null, Java version is the same as the package version
  if (isTRUE(verbose)) {
    if (!is.null(version)) {
      message(sprintf("This is OSMOSE version %s", version))
    } else {
      message(sprintf("This is OSMOSE version %s", package_version))
    }
  }
  
  # Update to provide by release executables
  if (is.null(osmose)) {
    if (!is.null(version)) {
      osmose_name = sprintf("osmose_%s.jar", version)
      lib = cacheManager("lib")
      osmose = shQuote(cacheManager(osmose_name))
    } else {
       osmose_name = sprintf("osmose_%s-jar-with-dependencies.jar", package_version)
       osmose = shQuote(system.file("java", osmose_name, package = "osmose"))
       version = package_version
    }
  }
  
  # If output path were specified, remove it
  if(isTRUE(clean) & !is.null(output)){
    file.remove(file.path(output, dir(path = output, recursive = TRUE)))
  }
    
  # Define values for options and parameters if they're NULL
  if(is.null(options)) options = ""
  if(is.null(parameters)) parameters = ""
  
  # If specified version is >= 3.4, add the name of the parameter to the 
  # execution expression
  version = .getVersion(version)
  versionRef = .getVersion("3.4")
  
  if(is.null(output)){
    # If output is NULL, file output path is used.
    outDir = ""
  }else{
    # else, overwrites the Osmose output parameter
    if(.compareVersion(version, versionRef) < 0) {
      outDir = output
    } else {
      # changes for version 4 or higher
      outDir = paste0("-Poutput.dir.path=", output)
    }
  }
  
  if(force) { 
    parameters = paste(parameters, "-force")
  }
  
  # If R is running in an interactive mode, use log (if TRUE)
  stdout = ifelse(interactive() & verbose, "", log)
  stderr = ifelse(interactive() & verbose, "", log)
  
  # Generate the execution expression
  args = paste(options, "-jar", osmose, input, outDir, parameters)
  command = paste(c(shQuote(java), args), collapse = " ")
  
  if(isTRUE(verbose)) message(sprintf("Running: %s", command))
  
  system2(java, args = args, stdout = stdout, stderr = stderr, wait = TRUE)
  
  conf = read_osmose(input = input)
  prefix = .getPar(conf, "output.file.prefix")
  if(is.null(prefix)) prefix = "osmose"
  write_osmose.osmose.configuration(conf, file = file.path(output, sprintf("%s-configuration.osm", prefix)))
  
  return(invisible(command))
}


# read_osmose -------------------------------------------------------------
#' @title Read OSMOSE outputs into an R object
#' 
#' @description This function create object of class \code{osmose} with the 
#' outputs from OSMOSE in the \code{path} folder.  
#' 
#' @param path Path to the directory containing OSMOSE outputs. 
#' @param input Path to a main OSMOSE configuration file.
#' @param version OSMOSE version used to run the model. 
#' @param species.names Display names for species, overwrite the species names
#' provided to the OSMOSE model. Used for plots and summaries.
#' @param absolute Whether the path is absolute (\code{TRUE}) or relative 
#' (\code{FALSE}). Only used if input is not NULL.
#' @param ... Additional arguments.
#' 
#' @details \code{read_osmose} will return a list of fields with the information
#' of whether an OSMOSE running or the configuration that is going to be used in 
#' a running. Output class will depend on the read info: If \code{path} is 
#' specified, output class will be \code{osmose}; otherwise, if ONLY \code{input}
#' is given, the class will be \code{osmose.config}. If both are specified, the 
#' \code{osmose} class output will content inside a field (\code{config}) of 
#' class \code{osmose.config}.
#' 
#' Individual elements can be extracted using the \link{get_var} function.
#' 
#' @author Ricardo Oliveros-Ramos, Laure Velez
#' 
#' @export
#' @examples
#' # Read outputs generated by demo (check osmose_demo function)
#' outdir = cacheManager("outputs")
#' read_osmose(path = outdir)
#'   
#' @aliases osmose2R
read_osmose = function(path = NULL, input = NULL, version = "4.3.2", 
                       species.names = NULL, absolute = TRUE, ...){
  
  # If both path and input are NULL, then show an error message
  if(is.null(path) & is.null(input)) stop("No output or configuration path has been provided.")
  
  # If config is not NULL, then read it
  if(is.null(input)) input = file.path(path, dir(path, pattern="-configuration.osm$")) 
  config = if(length(input)==1) suppressWarnings(.readConfiguration(file = input)) else NULL
  class(config) = "osmose.configuration"
  
  # If path is NULL, just return config
  if(is.null(path)) return(config)
  
  # Define the version that will be used for reading
  output_version = "v3r0"
  if(.compareVersion(version, "3.1.0") >= 0) output_version = "v3r1"
  if(.compareVersion(version, "3.2.0") >= 0) output_version = "v3r2"
  if(.compareVersion(version, "4.0.0") >= 0) output_version = "v4r0"
  
  # Check if path exists
  if(!dir.exists(path)) stop("The output directory does not exist.")
  
  # Depending on the version, apply the corresponding method
  output = switch(output_version, 
                  v3r0 = osmose2R.v3r0(path = path, species.names = species.names, ...),
                  v3r1 = osmose2R.v3r1(path = path, species.names = species.names, ...),
                  v3r2 = osmose2R.v3r2(path = path, species.names = species.names, ...),
                  v4r0 = osmose2R.v4r0(path = path, species.names = species.names, conf=config, ...),
                  stop(sprintf("Incorrect osmose version %s", version)))
  
  # Add config info
  output = c(output, config = list(config))
  
  # Define class of output
  class(output) = "osmose"
  
  return(output)
}

#' @title Report method
#' @description This function built a report for each class including on osmose package.
#' @param x Object of class \code{osmose}.
#' @param format The format to export the report.
#' @param output Folder where the report will be saved.
#' @param ... Extra arguments passed to \code{\link{report}} function.
#' @return A report on specific format.
#' @export
report = function(x, format, output, ...) {
  UseMethod("report")
}


#' @title Get variable from an \code{osmose}-like object.
#' @description Function to get a variable from an object of \code{osmose} class. 
#' This function uses the get_var method (see the \code{\link{get_var.osmose}}).
#'
#' @param object Object of \code{osmose} class (see the \code{\link{read_osmose}} function).
#' @param what Variable to extract
#' @param how Output format
#' @param ... Additional arguments of the function.
#'
#' @return An array or a list containing the extracted data.
#' @export
get_var = function(object, what, how, ...){
  UseMethod("get_var")
}


#' @title Write data in osmose format
#'
#' @description Write an array or dataframe in the Osmose format.
#' The separator is ";", there are no quotes and a blank column is
#' added for the row names column.
#'
#' @param x Object to be written (table or data frame)
#' @param file Output file
#' @param sep The field separator string. Values within each row of x are 
#' separated by this string.
#' @param col.names either a logical value indicating whether the column names 
#' of x are to be written along with x, or a character vector of column names to 
#' be written. See the section on ‘CSV files’ for the meaning of 
#' \code{col.names = NA}.
#' @param quote A \code{logical} value (\code{TRUE} or \code{FALSE}) or a 
#' \code{numeric} vector.
#' @param row.names either a logical value indicating whether the row names of x 
#' are to be written along with x, or a character vector of row names to be 
#' written.
#' @param append Append to 'file' or create a new one?
#' @param ... Extra arguments passed to \code{write.table} funtion.
#' 
#' @export
write_osmose = function(x, file, sep=",", ...) {
  UseMethod("write_osmose")
}


#' @title Generates required OSMOSE configuration files to run a demo.
#' 
#' @description Run demo 
#' 
#' @param path Path where to put the Osmose configuration file.
#' @param config Reference configuration to run ("eec_4.3.0"). 
#' @note So far, only one configuration is propose ("eec_4.3.0")
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
#'demo = osmose_demo(path="../", config="eec_4.3.0")
#'
#'# run the osmose model
#'run_osmose(demo$config_file, parameters=NULL, output=NULL, version="4.3.2", 
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
osmose_demo = function(path = NULL, config = c("gog", "eec_4.3.0")){
  
  config = match.arg(config)
  
  # if no path has been provided, create a path from the working dir.
  if(is.null(path)) path = file.path(getwd(), config)
  
  # if the directory does not exist, then create
  # the directory
  if(!dir.exists(path)) {
    dir.create(path)
  }
  
  # Switch for the configuration directory
  input_dir = switch(config, 
                     gog = cacheManager("gog"),
                     eec_4.3.0 = cacheManager("eec_4.3.0"),
                     stop(paste("There is not reference for", config))
  )
  
  # swith for the configuration directory and defines the configuration file
  config_file = switch(config, 
                       gog = "osm_all-parameters.csv",
                       eec_4.3.0 = "eec_all-parameters.csv",
                       stop(paste("There is not reference for", config))
  )
  
  output_dir = switch(config, 
                      gog = "output",
                      eec_4.3.0 = "output-PAPIER-trophic",
                      stop(paste("There is not reference for", config))
  )
  
  file.copy(from = input_dir, to = path, recursive = TRUE, overwrite = FALSE)
  config = basename(path = input_dir)
  
  demo = list()
  config_file = file.path(path, config, config_file)
  demo$config_file = config_file
  demo$output_dir = file.path(dirname(path = config_file), output_dir)
  
  return(demo)
}


# Demo --------------------------------------------------------------------
#' Generates Osmose configuration files to run an Osmose demo.
#' 
#' @param path Path where to put the Osmose configuration file.
#' @note So far, only one configuration is propose ("eec_4.3.0")
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
#'demo = osmose_demo(path="../", config="eec_4.3.0")
#'
#'# run the osmose model
#'run_osmose(demo$config_file, parameters=NULL, output=NULL, version="4.3.2", 
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
osmose_calib_demo = function(path = NULL) {
  
  # if no path has been provided, create a path from the working dir.
  if(is.null(path)) path = getwd()
  
  # if the directory does not exist, then create it
  if(!dir.exists(path)) dir.create(path = path, showWarnings = FALSE, recursive = TRUE)
  
  # copy the calibration data into the path directory
  input_dir = cacheManager("calib_demo")
  file.copy(from = input_dir, to = path, recursive = TRUE, overwrite = TRUE)
  
  # Copy the reference gog configuration in the calibration folder
  input_dir = cacheManager("gog")
  file.copy(from = input_dir, to = file.path(path, "calib_demo"), recursive = TRUE, overwrite = TRUE)
  
  demo = list(path = file.path(path, "calib_demo"))
  demo$file = "calibrate.R"
  
  return(demo)
}
