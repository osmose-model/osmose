# All the methods for osmose class ----------------------------------------


#' Plot method for osmose objects
#' @description This method takes a \code{osmose} object and produce useful plots
#' @param x Object of \code{osmose} class. See the \code{\link{read_osmose}} function for more details.
#' @param what Variable name to plot. By default is \code{what = "biomass"}.
#' @param ... Additional arguments for the plot function.
#' @return A graph of a osmose object
#' @export
#' @method plot osmose
plot.osmose = function(x, what = "biomass", ...) {
  
  x = getVar(x, what = what, expected = FALSE)
  plot(x, ...)
  
  return(invisible())
}


#' Get variable
#' @description Function to get a variable from an object of \code{osmose} 
#' class. This function uses the getVar method (see the \code{\link{getVar.osmose}}).
#' @param object Object of \code{osmose} class (see the \code{\link{read_osmose}} function).
#' @param var Name of variable to extract. It could be: "biomass","abundance",
#'  "yield", "yieldN".
#' @param ... Additional arguments of the function.
#' @return An array or a list containing the extracted data.
#' @export
getVar = function(object, what, how, ...) {
  UseMethod("getVar")
}

#' GetVar method for osmose outputs objects
#' @description Get a variable from an \code{osmose} object. 
#'
#' @param object Object of \code{osmose} class (see the \code{\link{read_osmose}} function).
#' @param what Name of variable to extract. It could be: "biomass","abundance",
#'  "yield", "yieldN".
#' @param how How to return the object. Current options are "matrix" and "list".
#' @param expected A logical parameter. \code{TRUE} if the average over the 
#' last dimensions should be performed if the output is an array. By default is 
#' \code{expected = FALSE}.
#' @param ... Additional arguments of the function.
#'
#' @return An array or a list containing the data.
#' @export
#' @method getVar osmose
getVar.osmose = function(object, what, how=c("matrix", "data.frame", "list"), 
                         expected=FALSE, ...) {
  
  how = match.arg(how)
  
  if(how=="list") expected = TRUE
  
  out = object[[what]]
  
  if(is.null(out)) {
      message = paste("The", sQuote(what), "variable is NULL.", sep="")
      stop(message) 
  }

  if(inherits(out, "array") & isTRUE(expected))
    out = apply(out, c(1, 2), mean, na.rm=TRUE)
  
  if(how=="matrix") return(out)
  
  if(how=="list") return(as.list(as.data.frame(out)))
  
  warning("No output defined for data.frame yet.")
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
  yield = apply(object$yield, 2, mean, na.rm=TRUE)
  resumen = data.frame(biomass=biomass,
                       yield = yield)
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
#' @param x Object of \code{osmose} class.
#' @method report osmose
#' 
#' @export
report.osmose = function(x, format = "latex", tangle=FALSE, output = NULL, open = TRUE) {
  
  if(is.null(output)) output = getwd()
  
  outputName = deparse(substitute(x))
  
  skeleton = system.file("reports", "osmose-report.Rmd", package = "osmose")
  
  if(isTRUE(tangle)) {
    knit(skeleton, tangle=TRUE, encoding = "latin1")
    f1 = gsub(pattern = ".Rmd", replacement = "\\.R", skeleton)
    file.rename(from=basename(f1), to=paste0(outputName, ".R"))
  }
  
  outputFile = paste0(outputName, "_output.pdf")
  render(skeleton, c("pdf_document"), output_file=outputFile, output_dir=output, encoding = "latin1")
  
  if(isTRUE(open)) shell.exec(outputFile)
  
  return(invisible(file.path(output, outputFile)))
  
}


