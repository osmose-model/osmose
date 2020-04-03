
#' Read Osmose output file
#'
#' @param path Osmose output path
#' @param type Data type ("biomass", etc)
#' @param bySpecies TRUE if should read one file per species.
#' @param ext The extension of the files shich will be read.
#' @param ... Additional arguments
#'
#' @return Output data frame
readOsmoseFiles = function(path, type, bySpecies=FALSE, ext="csv", ...) {
  
  # Build the class name pasting osmose + type
  xclass = paste("osmose", type, sep = ".")
  
  # If the class has a Distrib label, remove it
  xclass = gsub(x = xclass, pattern = "Distrib", replacement = "")
  
  # Get a vector with all files on the path
  allFiles = dir(path = path, recursive = TRUE, include.dirs = FALSE)
  
  # Get files with the selected extensio: ext
  extFiles = allFiles[grepl(pattern = paste0(".", ext), x = allFiles)]
  
  # Read files 
  if(isTRUE(bySpecies)){
    # Subset list of files
    files  = extFiles[grepl(pattern = paste0(type, "-"), x = extFiles)]
    
    # Split path names by species 
    files  = .bySpecies(files = files)
    
    # Read files
    output = lapply(files, .readFilesList, path = path, type = type, ...)
  }else{
    # Subset list of files
    files  = extFiles[grepl(pattern = paste0(type, "_"), x = extFiles)]
    
    # Read files
    output = .readFilesList(files = files, path = path, type = type, ...)
  }
  
  # Define a class for output
  if(!is.null(output)) class(output) = c(xclass, class(output))
  
  return(output)
}

# Non-exported ------------------------------------------------------------


getmfrow = function(n){
  m1 = floor(sqrt(n))
  m2 = ceiling(n/m1)
  out = rev(sort(c(m1, m2)))
  
  return(out)
}

writeOsmoseParameters = function(conf, file, sep=";") {
  .writeParameter = function(x) {
    out = paste(names(x),paste(x, collapse=sep), sep=sep)
    return(out)
  }
  out = sapply(conf, .writeParameter)
  vars = names(out)
  ind = sort(vars, index.return=TRUE)$ix
  dim(out) = c(length(out), 1)
  out = out[ind,, drop=FALSE]
  rownames(out) = vars[ind]
  write.table(out, file=file, sep="", quote=FALSE, col.names=FALSE)
  return(invisible(out))
}

# Takes a string and returns the words splitted by "_"
getWhats = function(x){
  index = grep(pattern = "_", x = x)
  
  if(length(index) > 0){
    x = unlist(strsplit(x = x, split = "_"))
  }
  
  return(x)
}
