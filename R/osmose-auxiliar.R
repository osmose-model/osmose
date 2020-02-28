 
#' Returns the list of Osmose Java versions
#'
#' @return List of Osmose Java versions
#'
list_osmose_versions = function() {
  dirin = system.file(package="osmose", "java")
  output = list.files(path=dirin, pattern=".jar")
  return(output)
}

# 
# isfalse = function(x) {
#   output = !isTRUE(x)  
#   return(output)
# }


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
  
  xclass = paste("osmose", type, sep=".")
  
  allFiles = dir(path=path, recursive=TRUE, include.dirs=FALSE)
  extFiles = allFiles[grepl(paste0(".", ext), allFiles)]
  
  if(!isTRUE(bySpecies)) {
    
    type_  = paste0(type, "_")
    files  = extFiles[grepl(type_, extFiles)]
    output = .readFilesList(files=files, path=path, type=type, ...)
    
  } else {
    
    type_  = paste0(type, "-")
    files  = extFiles[grepl(type_, extFiles)]
    files  = .bySpecies(files=files)
    output = lapply(files, FUN=.readFilesList, path=path, type=type, ...)
    
  }
  
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

# Reads a csv file using COMA and SEMICOLON and select that what honor the 
# ncol_condition
readCSVGuessing <- function(file, ncol_condition = "> 1", ...){
  x1 <- read.table(file = file, sep = ",", ...)
  x2 <- read.table(file = file, sep = ";", ...)
  
  evalCondition <- sapply(paste0(c(ncol(x1), ncol(x2)), ncol_condition), 
                          function(x) eval(parse(text = x)))
  
  if(all(!evalCondition)){
    stop("The file doesn't match with the condition neither for ',' and ';'.")
  }else if(all(evalCondition)){
    stop("The file do match with the condition using both ',' and ';'. Improve the condition.")
  }else{
    return(get(paste0("x", which(evalCondition))))
  }
}
