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


#' Manage the cache
#'
#' The function creates a folder (only once) and then controls files that need to be downloaded and
#' placed in this folder.
#'
#' @section Localisation:
#' The localisation of the folder is defined in the `.Renviron` file, by setting the `OSMOSE_DIR`
#' environment variable:
#'   - On Linux/Mac Os X : `OSMOSE_DIR=/Users/Nicolas/Desktop/OSMOSE_TEST/R`
#'   - On Windows: `OSMOSE_DIR=C:\\Users\\Nicolas\\Desktop\\OSMOSE_TEST\\R`
#'
#' If this variable is not set, the files will be downloaded into a temporary directory.
#'
#' @param nameFile the name of the file or folder
#'
#' @return the path to the file we need
#'
#' @author Arthur PERE
#' @author Nicolas BARRIER
#'
#' @keywords Internal
#' @export
cacheManager <- function(nameFile) {

  # gets the mainPath for the cache directory
  basePath <- cachePath()

  # creates the cache directory if does not exist
  if (!dir.exists(basePath)) dir.create(basePath, recursive = T, showWarnings = F)

  path <- cachePath(nameFile)

  ############# if the folder exists in the designed cache folder
  file_exists <- file.exists(path)

  ############# if the folder exists in the working directory, move it to the cache directory
  if (file.exists(nameFile) && !file_exists) {
    file.copy(nameFile, basePath, recursive = T)
    unlink(nameFile, recursive = TRUE, force = FALSE)
    message("Your folder '", nameFile, "' has been moved in this folder : ", basePath)
    file_exists <- T
  }

  # if the file is empty
  #if (file_exists && length(dir(path)) == 0) {
  #  file.remove(path)
  #  file_exists <- F
  #}

  ############# if the folder does not exist anywhere
  if (!file_exists) {
    updateCache(nameFile)
  }

  return(path)
}


#' @keywords Internal
cachePath <- function(path = NULL) {

  # give the path of OSMOSE_DIR
  # If variable is not set, then points to a temporary directory.
  if(Sys.getenv("OSMOSE_DIR") == "") {
    tdir = tempdir()
    Sys.setenv(OSMOSE_DIR=tdir)
    message("The OSMOSE_DIR Renviron variable was set to ", tdir)
  }

  # Recover the paths to the OSMOSE_DIR and creates
  # it if needed
  basePath = Sys.getenv("OSMOSE_DIR")

  if (!is.null(path)) {
    basePath <- file.path(basePath, path)
  }

  basePath

}


#' @keywords  Internal
flushCache <- function(filename=NULL) {

  basePath = ifelse(is.null(filename), cachePath(), cachePath(filename))
  if (dir.exists(basePath) || file.exists(basePath)) {
    message(basePath, " has been cleaned.")
    unlink(basePath, recursive = T, force = F)
  }
}



#' Update the cache for the different function
#'
#' This function update the cache for the environmental variables:
#'    - wc2-5
#'    - CWD
#'    - E
#'
#' @param nameFile The name of the file you want to update. If it's `NULL` the function will update all the files.
#'
#' @return NULL
#' @author Arthur PERE
#'
#' @importFrom utils download.file unzip
#' @examples
#' \dontrun{
#' updateCache()
#' }
updateCache <- function(nameFile) {

  # the url of the differents zip to download
  zip_urls <- list(
    "gog" = "https://github.com/osmose-model/osmose/raw/master/data-raw/gog.zip",
    "calib_demo" =  "https://github.com/osmose-model/osmose/raw/master/data-raw/calib_demo.zip",
    "outputs" = "https://github.com/osmose-model/osmose/raw/master/data-raw/outputs.zip",
    "osmose_3.3.1.jar" = "https://github.com/osmose-model/osmose/raw/master/inst/java/osmose_3.3.1.zip",
    "osmose_3.3.2.jar" = "https://github.com/osmose-model/osmose/raw/master/inst/java/osmose_3.3.2.zip",
    "osmose_3.3.3.jar" = "https://github.com/osmose-model/osmose/raw/master/inst/java/osmose_3.3.3.zip",
    "osmose_4.0.0.jar" = "https://github.com/osmose-model/osmose/raw/master/inst/java/osmose_4.0.0.zip",
    "osmose_4.1.0.jar" = "https://github.com/osmose-model/osmose/raw/master/inst/java/osmose_4.1.0.zip",
    "osmose_4.2.0.jar" = "https://github.com/osmose-model/osmose/raw/master/inst/java/osmose_4.2.0.zip",
    "osmose_4.3.0.jar" = "https://sourcesup.renater.fr/www/osmose/osmose_4.3.0.zip",
    "osmose_4.3.1.jar" = "https://sourcesup.renater.fr/www/osmose/osmose_4.3.1.zip",
    "osmose_4.3.2.jar" = "https://sourcesup.renater.fr/www/osmose/osmose_4.3.2.zip",
    "eec_4.3.0" =  "https://github.com/osmose-model/osmose-eec/archive/refs/heads/develop.zip",
    "lib" = "https://github.com/osmose-model/osmose/raw/master/inst/java/lib.zip"
  )

  folder_names <- list(
    "eec_4.3.0" =  "osmose-eec-develop"
  )

  # dowload and unzip the files
  downloadZip <- function(zip_url, path) {
    tmp <- tempfile(fileext = ".zip")

    DEMzip <- download.file(zip_url, destfile = tmp)

    # If URL ends with .zip, unzip the file
    if(endsWith(basename(zip_url), ".zip")) {

      if(is.null(folder_names[[nameFile]])) {
        # If the folder name entry is null, then assume that the zip contains only the files.
        unzip(tmp, exdir = dirname(path))
      } else {
        # If the folder name entry is not null, then assume that the zip contain a folder.
        # in this case, we unzip into the tmp folder, and we move the
        # folder in the new location
        unzip(tmp, exdir = dirname(tmp))
        input_path = file.path(dirname(tmp), folder_names[[nameFile]])
        file.rename(input_path, path)
      }
    } else {
      file.copy(from=tmp, to=path)
    }

    # update the flag
    #writeLines(as.character(Sys.Date()), cachePath("last_check.txt"))

    message("Your file ", nameFile, " has been download and/or deziped in this folder : ", path)
    return()
  }

  #nameFileZip <- paste0(nameFile, ".zip")
  path <- cachePath(nameFile)  # gets the cache path for the given file

  zip_url <- zip_urls[[nameFile]]
  if (is.null(zip_url)) {
    stop("URL is not found")
  }

  downloadZip(zip_url, path)
  return()

}
