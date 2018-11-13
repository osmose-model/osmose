 
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
#' 
#' @export
write_osmose = function(x, file)   {
  write.table(x=x, file=file, sep=";", col.names=NA, quote=FALSE)
}

#' @export
write.osmose = function(x, file)   {
  .Deprecated("write_osmose")
  write.table(x=x, file=file, sep=";", col.names=NA, quote=FALSE)
}

#' Read Osmose output file
#'
#' @param path Osmose output path
#' @param type Data type ("biomass", etc)
#' @param bySpecies TRUE if should read one file per species.
#' @param ... Additional arguments
#'
#' @return Output data frame
readOsmoseFiles = function(path, type, bySpecies=FALSE, ...) {
  
  xclass = paste("osmose", type, sep=".")
  
  allFiles = dir(path=path, recursive=TRUE, include.dirs=FALSE)
  csvFiles = allFiles[grepl(".csv", allFiles)]
  
  if(!isTRUE(bySpecies)) {
    
    type_  = paste0(type, "_")
    files  = csvFiles[grepl(type_, csvFiles)]
    output = .readFilesList(files=files, path=path, type=type, ...)
    
  } else {
    
    type_  = paste0(type, "-")
    files  = csvFiles[grepl(type_, csvFiles)]
    files  = .bySpecies(files=files)
    output = lapply(files, FUN=.readFilesList, path=path, type=type, ...)
    
  }
  
  if(!is.null(output)) class(output) = c(xclass, class(output))
  
  return(output)
  
}

#' Get size spectrum
#'
#' @param file File to read
#' @param sep File separator
#' @param ... Additional arguments of the \code{read.csv} function
#'
#' @return A 3D array (time, length, species)
#' @examples{
#' dirin = system.file("extdata", package="osmose")
#' file = paste(dirin, "/outputs/SizeIndicators/gogosm_yieldDistribBySize_Simu0.csv", sep="")
#' size = getSizeSpectrum(file)
#' }
#' @export
getSizeSpectrum = function(file, sep=",", ...) {
  
  # sizeSpectrum = read.table(file, sep=sep, dec=".", skip=1,
  #                          header=TRUE)
  sizeSpectrum = .readOsmoseCsv(file=file, sep=sep, header=TRUE, row.names=NULL, ...)

  nsp = ncol(sizeSpectrum) - 2
  times = unique(sizeSpectrum$Time)
  lengths = unique(sizeSpectrum$Size)
  
  out = array(dim = c(length(times), length(lengths), nsp))
  
  for(t in seq_along(times)) {
    out[t,,]  = as.matrix(sizeSpectrum[sizeSpectrum$Time==times[t],-(1:2)])
  }
  colnames(out) = lengths
  rownames(out) = round(times,3)
  dimnames(out)[[3]] = paste0("sp.", seq(nsp)-1)
  return(out)
}


#' Get the total mortality rate. 
#' 
#' @param x Mortality dataframe
#' @param stage Stage ("adults", etc.)
#' @param type Mortality type ("pred", "starv", "other", "out", "total"). 
#' The latter is computed as the sum of all mortality types
#' @return A mortality array
#' @examples{
#' dirin = system.file("extdata", package="osmose")
#' outdir = paste(dirin, "/outputs", sep="")
#' data = read_osmose(outdir)
#' mortality_df = data$mortality
#' mort = getMortality(mortality_df, stage="juveniles", type="total")
#' }
#' @export
getMortality = function(x, stage="adults", type="total") {
  .calcMort = function(x) {
    x = as.data.frame(x)
    x$natural = x$pred + x$starv + x$other + x$out
    x$total = x$natural + x$fishing
    return(x)
  }
  .getZ = function(x, stage, type) {
    x = x[[stage]]
    x = apply(x, 1:2, mean, na.rm=TRUE)
    x = .calcMort(x)
    x = x[, type]
    return(x)
  }
  
  out = sapply(x, .getZ, stage=stage, type=type)
  return(out)
}

#' Computes the average mortality.
#' 
#' It computes the mean mortality, which
#' is multiplied by the frequency.
#'
#' @param x Mortality dataframe
#' @param stage Stage ("adults", etc.)
#' @param freq Time frequency (months?)
#'
#' @return An array
#' @examples{
#' dirin = system.file("extdata", package="osmose")
#' outdir = paste(dirin, "/outputs", sep="")
#' data = read_osmose(outdir)
#' mortality_df = data$mortality
#' mort = getAverageMortality(mortality_df, stage="juveniles", freq=12)
#' }
#' @export
getAverageMortality = function(x, stage="adults", freq=12) {
  
  .getZ = function(x, stage) {
    x = x[[stage]]
    x = apply(x, 1:2, mean, na.rm=TRUE)
    x = freq*colMeans(x, na.rm=TRUE)
    return(x)
  }
  
  out = sapply(x, .getZ, stage=stage)
  return(out)
}

#' Computes the mortality deviation. The "proxy", which is removed,
#' can be provided by the user in the "pars" argument.
#'
#' @param x Mortality dataframe
#' @param stage Stage ("adults", etc.)
#' @param type Mortality type
#' @param pars A list or data frame containing  
#' \emph{dt.save}, \emph{M.proxy}, \emph{dt} entries.
#' If NULL, then \code{proxy = colMeans(x)}
#'
#' @return An array
#' @examples{
#' dirin = system.file("extdata", package="osmose")
#' outdir = paste(dirin, "/outputs", sep="")
#' data = read_osmose(outdir)
#' mortality_df = data$mortality
#' mortdev = getMortalityDeviation(mortality_df, stage="juveniles", type="total")
#' }
#' @export
getMortalityDeviation = function(x, stage, type, pars=NULL) {
  x     = getMortality(x=x, stage=stage, type=type)
  if(!is.null(pars)) {
    proxy = pars$dt.save*pars$M.proxy/pars$dt    
  } else {
    proxy = colMeans(x)
  }
  out   = t(apply(x, 1, "-", proxy))
  return(out)
}




# Non-exported ------------------------------------------------------------


getmfrow = function(n) .getmfrow(n=n)

makeTransparent = function(..., alpha=0.5) {
  
  if(alpha<0 | alpha>1) stop("alpha must be between 0 and 1")
  
  alpha = floor(255*alpha)  
  newColor = col2rgb(col=unlist(list(...)), alpha=FALSE)
  
  .makeTransparent = function(col, alpha) {
    rgb(red=col[1], green=col[2], blue=col[3], alpha=alpha, maxColorValue=255)
  }
  
  newColor = apply(newColor, 2, .makeTransparent, alpha=alpha)
  
  return(newColor)
  
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
