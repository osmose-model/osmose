# All the methods for osmose class ----------------------------------------


#' @title Plot method for osmose objects
#' @description This method takes a \code{osmose} object and produce useful 
#' plots
#' @param x Object of \code{osmose} class. See the \code{\link{read_osmose}} 
#' function for more details.
#' @param what Variable name to plot. By default is \code{what = "biomass"}. 
#' See Details
#' @param ... Additional arguments for the plot function.
#' 
#' @details \code{what} argument can take next values:
#' \itemize{
#'  \item "biomass" to plot the species biomass (see \link{plot.osmose.biomass})
#'  \item "abundance" to plot the species abundance (see \link{plot.osmose.abundance})
#'  \item "yield" to plot the species yield (see \link{plot.osmose.yield})
#'  \item "yieldN" to plot the species yield (in numbers) (see \link{plot.osmose.yieldN})
#' }
#' 
#' @return A graph of an \code{osmose} object.
#' @export
#' @method plot osmose
plot.osmose = function(x, what = "biomass", ...) {
  
  x = get_var(x, what = what, expected = FALSE)
  plot(x, ...)
  
  return(invisible())
}

#' get_var method for osmose outputs objects
#' @description Get a variable from an \code{osmose} object. 
#'
#' @param object Object of \code{osmose} class (see the \code{\link{read_osmose}} 
#' function).
#' @param what Name of variable to extract. See Details.
#' @param how How to return the object. Current options are "matrix" and "list".
#' @param expected A logical parameter. If \code{TRUE}, the average over the 
#' last dimensions will be performed (only if the output is an array).
#' @param ... Additional arguments of the function.
#' 
#' @details \code{what} can be any available variable contained on \code{object}
#' (e.g. biomass, abundance, yield, yieldN, etc).
#'
#' @return An matrix or a list containing the data.
#' @export
#' @method get_var osmose
get_var.osmose = function(object, what, how = c("matrix", "list"), 
                          expected = FALSE, ...){
  
  # Argument verification of 'how' using partial matching
  how = match.arg(how)
  
  # Change the value of expected if how is a list
  if(how == "list") expected = TRUE
  
  # Extract variable from object
  out = object[[what]]
  
  # If it's NULL, then show an error message
  if(is.null(out)) {
    message = paste("The", sQuote(what), "variable is NULL.", sep="")
    stop(message) 
  }
  
  if(inherits(out, "array") & isTRUE(expected)){
    out = apply(out, c(1, 2), mean, na.rm = TRUE)
  }
  
  if(how == "matrix") return(out)
  
  if(how == "list") return(as.list(as.data.frame(out, check.names = FALSE)))
  
  return(out)
}


#' Print informations about Osmose outputs
#'
#' @param x Osmose outputs (see the \code{\link{read_osmose}} function)
#' @param ... Additional arguments for \code{print}.
#' @method print osmose
#'
#' @export
print.osmose = function(x, ...) {
  cat(paste0("OSMOSE v.", x$model$version,"\n"))
  cat("Model", sQuote(x$model$model),"\n")
  cat(x$model$sp, " species modeled (",x$model$simus,
      " simulations):\n", sep="")
  cat(paste(x$species, collapse=", "),".\n", sep="")
}

#' Summarizes informations about Osmose outputs
#'
#' @param object Osmose outputs (see the \code{\link{read_osmose}} function)
#' @param ... Additional arguments for \code{summary}.
#' 
#' @method summary osmose
#' @export
summary.osmose = function(object, ...) {
  
  output = object$model
  output$species = object$species
  biomass = apply(object$biomass, 2, mean, na.rm=TRUE)
  yieldN = apply(object$yieldN, 2, mean, na.rm=TRUE)
  resumen = data.frame(biomass=biomass,
                       yield = yieldN)
  rownames(resumen) = object$species
  output$resumen = resumen
  
  class(output) = "summary.osmose"
  return(output)
  
}

#' Print the summary informations about Osmose outputs
#'
#' @param x Osmose outputs (see the \code{\link{read_osmose}} function)
#' @param ... Additional arguments for \code{print}.
#' @method print summary.osmose
#'
#' @export
print.summary.osmose = function(x, ...) {
  cat(paste0("OSMOSE v.", x$version,"\n"))
  cat("Model", sQuote(x$model),"\n")
  cat("Species modeled:\n")
  cat(paste(x$species, collapse=", "),".\n", sep="")
  cat("Main indicators:\n")
  print(x$resumen)
}

#' @title Report method for osmose objects
#' @description Export a report of osmose objects.
#'
#' @param x Object of \code{osmose} class.
#' @param format The R Markdown output format to convert to (check \link[rmarkdown]{render}).
#' @param output The output directory for the rendered the output file.
#' @param tangle Boolean; whether to tangle the R code from the input file (check \link[rmarkdown]{render}).
#' @param open Do you want to open the output file at the end? (Only for Windows envir).
#' @param ... Extra arguments pased to \link[rmarkdown]{render}.
#'
#' @method report osmose
#' 
#' @export
report.osmose = function(x, format = "pdf_document", output = NULL, tangle = FALSE, open = TRUE, ...) {
  
  if(is.null(output)) output = getwd()
  
  outputName = deparse(substitute(x))
  
  skeleton = system.file("reports", "osmose-report.Rmd", package = "osmose")
  
  if(isTRUE(tangle)) {
    knit(skeleton, tangle = TRUE, encoding = "latin1")
    f1 = gsub(pattern = ".Rmd", replacement = "\\.R", skeleton)
    file.rename(from=basename(f1), to=paste0(outputName, ".R"))
  }
  
  outputFile = paste0(outputName, "_output.pdf")
  render(input = skeleton, output_format = format, output_file = outputFile, output_dir = output, encoding = "latin1", )
  
  if(Sys.info()['sysname'] == "Windows" && isTRUE(open)){
    shell.exec(file = file.path(output, outputFile, fsep = "\\"))
  }
  
  return(invisible(file.path(output, outputFile)))
}
