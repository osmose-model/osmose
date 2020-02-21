 
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


#' Write data in osmose format
#'
#' Write an array or dataframe in the Osmose format.
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
#' @param ... Extra arguments passed to \code{write.table} funtion.
#' 
#' @export
write_osmose = function(x, file, sep = ",", col.names = NA, quote = FALSE, 
                        row.names = TRUE, ...){
  write.table(x = x, file = file, sep = sep, col.names = col.names, quote = quote,
              row.names = row.names, ...)
}


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

# adjustcolor?
# makeTransparent = function(..., alpha=0.5) {
#   
#   if(alpha<0 | alpha>1) stop("alpha must be between 0 and 1")
#   
#   alpha = floor(255*alpha)  
#   newColor = col2rgb(col=unlist(list(...)), alpha=FALSE)
#   
#   .makeTransparent = function(col, alpha) {
#     rgb(red=col[1], green=col[2], blue=col[3], alpha=alpha, maxColorValue=255)
#   }
#   
#   newColor = apply(newColor, 2, .makeTransparent, alpha=alpha)
#   
#   return(newColor)
#   
# }

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
