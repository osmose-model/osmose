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
#' See this function for more information : [rappdirs::user_data_dir()] [BIOMASS::cacheManager(nameFile)]
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


#' @importFrom rappdirs user_data_dir
#' @keywords Internal
cachePath <- function(path = NULL) {
  # give the path of the cache
  basePath <- user_data_dir(file.path("R", "osmose"))
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
    "osmose_4.2.1.jar" = "https://github.com/osmose-model/osmose/raw/master/inst/java/osmose_4.2.1.zip",
    "lib" = "https://github.com/osmose-model/osmose/raw/master/inst/java/lib.zip"
  )
  
  # dowload and unzip the files
  downloadZip <- function(zip_url, path) {
    tmp <- tempfile(fileext = ".zip")
    
    DEMzip <- download.file(zip_url, destfile = tmp)
    
    # If URL ends with .zip, unzip the file
    if(endsWith(basename(zip_url), ".zip")) { 
      unzip(tmp, exdir = dirname(path))
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
