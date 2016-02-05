# guess the type of a vector
.guessType = function(x, keep.att=FALSE) {
  if(!is.character(x)) return(x)
  att = attributes(x)
  x = str_trim(strsplit(tolower(x), split=",")[[1]])
  if(identical(x,"null")) return(NULL) 
  asNum = suppressWarnings(as.numeric(x))
  isNumeric = !all(is.na(asNum))
  x[x=="na"] = NA
  out = if(isNumeric) asNum else x  
  attributes(out) = NULL
  if(keep.att) attributes(out) = att
  return(out) 
}

# get a parameter from a name chain
.getPar = function(x, ..., keep.att=FALSE) {
  chain = tolower(unlist(list(...)))
  if(is.list(x)) x=do.call(.getPar, list(x=x[[chain[1]]], chain[-1]))
  return(.guessType(x, keep.att = keep.att))
}

# get a parameter from a name chain (error if not found)
#' @export
getOsmoseParameter = function(x, ..., keep.att=FALSE) {
  chain = unlist(list(...))
  x = .getPar(x, ..., keep.att=TRUE)
  if(!isTRUE(keep.att)) attributes(x) = NULL
  if(is.null(x)) stop(sprintf("Parameter '%s' not found.", paste(chain, collapse=".")))
  return(x)
}


.getSpecies = function(x)  {
  x = names(x)
  x = grep(pattern = "^sp[0-9]*$", x = x, value=TRUE)
  return(x)
}

.getBoolean = function(x, default=NULL) {
  if(is.null(x)) return(default)
  if(length(x)>1) stop("More than one value provided.")
  if(is.logical(x)) return(x)
  x = tolower(x)
  if(x=="true") return(TRUE)
  if(x=="false") return(FALSE)
  stop(sprintf("Invalid input, %s is not boolean.", x))
}

.readInputCsv = function(file) {
  sep = .guessSeparator(readLines(file, n=1))
  out = read.csv(file, sep=sep, row.names=1)
}

.getFileAsVector = function(file) {
  if(is.null(file)) return(NULL)
  path = attr(file, which="path")
  if(!is.null(path)) file=file.path(path, file)
  if(!file.exists(file)) stop(sprintf("File %s not found", file))
  out = .readInputCsv(file=file)
  out = as.numeric(as.matrix(out))
  return(out)
}
