# All the methods for osmose class ----------------------------------------


#' Plot method for osmose objects
#' @description This method takes a \code{osmose} object to make useful plot 
#' for each variables.
#' @param x Object of \code{osmose} class (see the \code{\link{read_osmose}} function).
#' @param type Name of the variable to plot. By default is \code{type = "biomass"} but 
#' it could be:
#' \itemize{
#'  \item "biomass" to plot the species biomass.
#'  \item "abundance" to plot the species abundance.
#'  \item "yield" to plot the species yield.
#'  \item "yieldN" to plot the species yield (in numbers).
#' }
#' @param ... Extra arguments of the function. These could be:
#' \itemize{
#'  \item aggregate A \code{logical} parameter. By default \code{aggregate = FALSE} 
#'  show the evolution of the variable over the time. \code{aggregate = TRUE} show 
#'  the aggregation of the variable for each species. 
#'  \item start A number to indicate the first x-axis element to plot when 
#'  \code{aggregate = FALSE}. By default \code{start = NULL} and start with the 
#'  first element of the osmose object over the time.
#'  \item conf A number to indicate the confidence interval to plot. By default 
#'  \code{conf = 0.95}.
#'  \item factor A number to indicate the variable scale on the y axis. By default 
#'  \code{factor = 1e-6}.
#'  \item replicates A \code{logical} parameter. \code{replicates = FALSE} show the 
#'  mean value of the variable over the time but \code{replicates = TRUE} show the 
#'  values obtained in each replicates of the osmose object.
#'  \item nrep A number to indicate the number of replicates to show. This 
#'  parameter is used only when \code{replicates = TRUE}. By default \code{nrep = 3}.
#'  \item col The color of the lines (\code{agregate = FALSE}), 
#'  barplot (for variables biomass or abundance using \code{aggregate = TRUE}) or 
#'  boxplot (for variables yield or yieldN using \code{aggregate = TRUE}). 
#'  By default \code{col = "black"}.
#'  \item alpha A number between 0 and 1. Indicate the transparency with which the 
#'  confidence interval is colored. By default \code{alpha = 0.5}.
#' }
#' @details The parameters: \code{start}, \code{conf}, \code{factor},
#'  \code{replicates}, \code{nrep}, \code{alpha} are used only when \code{aggregate = FALSE}.
#' 
#' When \code{aggregate = TRUE} a \code{barplot} is generated for the 
#' variables "biomass" and "abundance". Each bar of the barplot represents the 
#' average value of the variable over the time for each species. For the variables 
#' "yield" and "yieldN" a \code{boxplot} is generated. Each box of the boxplot 
#' represents one species.
#' 
#' The plots with the evolution of the variable over the time (\code{aggregate = FALSE}) 
#' show the variable of each species included in a osmose object, where the x axis 
#' represents the time and the y axis the variable. The line (by default a black line) 
#' is the mean value over the time and the light grey part represent the confidence 
#' interval (by default \code{conf = 0.95}).
#' @return A graph of a osmose object.
#' @author Ricardo Oliveros-Ramos
#' @export
#' @method plot osmose
plot.osmose = function(x, what = "biomass", ...) {
  
  x = getVar(x, var = what, expected = FALSE)
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
getVar = function(object, var, ...) {
  UseMethod("getVar")
}

#' GetVar method for osmose outputs objects
#' @description Get a variable from an \code{osmose} object. 
#' @param object Object of \code{osmose} class (see the \code{\link{read_osmose}} function).
#' @param var Name of variable to extract. It could be: "biomass","abundance",
#'  "yield", "yieldN".
#' @param type Type of the variable to extract. By default is \code{type = "global"}.
#' @param expected A logical parameter. \code{TRUE} if the average over the 
#' last dimensions should be performed if the output is an array. By default is 
#' \code{expected = FALSE}.
#' @param ... Additional arguments of the function.
#' @return An array or a list containing the data.
#' @export
#' @method getVar osmose
getVar.osmose = function(object, var, expected=FALSE, ...) {
  
  out = object[[var]]
  if(is.null(out))
  {
      message = paste("The ", var, " variable is NULL", sep="")
      stop(message) 
  }

  xclass = "array" %in% class(out)
  if(isTRUE(!xclass) & isTRUE(expected))
    out = apply(out, c(1, 2), mean, na.rm=TRUE)
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


