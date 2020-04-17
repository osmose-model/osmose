#' Manage the cache
#'
#' The function creates a folder (only once) and then controls files that need to be downloaded and
#' placed in this folder.
#'
#' @section Localisation:
#' The localisation of the folder is :
#'   - On Linux : `~/.local/share/R/osmose`
#'   - On Mac OS X : `~/Library/Application Support/R/osmose`
#'   - On Windows 7 up to 10 : `C:\\Users\\<username>\\AppData\\Local\\R\\BIOMASS\\R\\osmose`
#'   - On Windows XP : `C:\\Documents and Settings\\<username>\\Data\\R\\BIOMASS\\R\\osmose`
#'
#' See this function for more information : [rappdirs::user_data_dir()][BIOMASS::cacheManager(nameFile)]
#'
#' @param nameFile the name of the file or folder
#'
#' @return the path to the file we need
#'
#' @author Arthur PERE
#' @author Nicolas BARRIER
#' @seealso [rappdirs::user_data_dir()][BIOMASS::cacheManager(nameFile)]
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
    file.remove(dir(nameFile, recursive = T, full.names = T), nameFile)
    message("Your folder '", nameFile, "' has been moved in this folder : ", basePath)
    file_exists <- T
  }
  
  # if the file is empty
  if (file_exists && length(dir(path)) == 0) {
    file.remove(path)
    file_exists <- F
  }
  
  #if (!getOption("BIOMASS.ignore_update", TRUE)) {
  #  checkTime()
  #}
  
  ############# if the folder does not exist anywhere
  if (!file_exists) {
    updateCache(nameFile)
  }
  
  # give the full path expect for the wc2-5 file who have two files we need.
  #path <- switch(nameFile,
  #  "wc2-5" = file.path(path, c("bio4.bil", "bio15.bil")),
  #  file.path(path, paste0(nameFile, ".bil"))
  #)
  
  return(path)
}


#' @importFrom rappdirs user_data_dir
#' @keywords Internal
cachePath <- function(path = NULL) {
  # give the path of the cache
  basePath <- user_data_dir("R/osmose")
  if (!is.null(path)) {
    basePath <- file.path(basePath, path)
  }
  basePath
}


#' #' @keywords Internal
#' checkTime <- function() {
#'   # Check if it's time to update
#'   if (!file.exists(cachePath("last_check.txt"))) {
#'     writeLines(as.character(Sys.Date()), cachePath("last_check.txt"))
#'   } else {
#'     check_interval <- 1830 # this message will appear every 5 years
#'     
#'     last_check <- as.Date(readLines(cachePath("last_check.txt")))
#'     if ((Sys.Date() - last_check) > check_interval) {
#'       message(
#'         "You can verify if the cache is updated by using this function\n",
#'         "\t\tupdateCache()\n",
#'         "Be careful, all the environement variable will be deleted and updated.\n",
#'         "You can ignore this message, and can prevent this message to appear again by using\n",
#'         "\t\toptions(BIOMASS.ignore_update=TRUE)"
#'       )
#'       
#'       # update the flag
#'       writeLines(as.character(Sys.Date()), cachePath("last_check.txt"))
#'     }
#'   }
#'   
#'   return()
#' }


#' @keywords  Internal
flushCache <- function() {
  
  basePath <- cachePath()
  
  if (dir.exists(basePath)) unlink(basePath, recursive = T, force = F)
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
#' @export
#'
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
    "gog" = "https://github.com/osmose-model/osmose/tree/master/inst/extdata/gog",
    "calib_demo" =  "https://github.com/osmose-model/osmose/tree/master/inst/extdatacalib_demo",
    "outputs" = "https://github.com/osmose-model/osmose/tree/master/inst/extdata/outputs.zip",
    "osmose_3.3.1.jar" = "https://github.com/osmose-model/osmose/tree/master/inst/java/osmose_3.3.1.jar",
    "osmose_3.3.2.jar" = "https://github.com/osmose-model/osmose/tree/master/inst/java/osmose_3.3.2.jar",
    "osmose_3.3.3.jar" = "https://github.com/osmose-model/osmose/tree/master/inst/java/osmose_3.3.3.jar",
    "osmose_4.0.0.jar" = "https://github.com/osmose-model/osmose/tree/master/inst/java/osmose_4.0.0.jar",
    "osmose_4.1.0.jar" = "https://github.com/osmose-model/osmose/tree/master/inst/java/osmose_4.1.0.jar",
    "osmose_4.2.1.jar" = "https://github.com/osmose-model/osmose/tree/master/inst/java/osmose_4.2.1.jar",
    "lib" = "https://github.com/osmose-model/osmose/tree/master/inst/java/lib"
  )
  
  # dowload and unzip the files
  downloadZip <- function(zip_url, path) {
    tmp <- tempfile(fileext = ".zip")
    
    DEMzip <- download.file(zip_url, destfile = tmp)
    
    # If URL ends with .zip, unzip the file
    if(endsWith(basename(zip_url), ".zip")) { 
      unzip(tmp, exdir = path)
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
