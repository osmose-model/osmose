# All the methods for osmose class ----------------------------------------


#' Plot Osmose outputs.
#'
#' @param x Osmose outputs (see the \code{\link{osmose2R}} function)
#' @param type Data type to draw ("biomass", etc)
#' @param ... Additional arguments of the plot function
#'
#' @return invisible()
#' @export
plot.osmose = function(x, type="biomass", ...) {
  
  x = getVar(x, var=type, expected=FALSE)
  plot(x, ...)
  
  return(invisible())
}


#' Get a specific Osmose output.
#'
#' @param object Osmose outputs (see the \code{\link{osmose2R}} function)
#' @param var Variable to extract
#'
#' @return An array or a list containing the data.
#' @export
getVar = function(object, var) {
  UseMethod("getVar")
}

#' Get a specific Osmose output.
#' @param object Osmose outputs (see the \code{\link{osmose2R}} function)
#' @param var Variale to extract
#' @param type Type of the variable to extract
#' @param expected TRUE if the average over the 
#' last dimensions should be performed if the output is an array.
#'
#' @return An array or a list containing the data.
#' @export
#' @method getVar osmose
getVar.osmose = function(object, var, type="global", expected=FALSE) {
  out = object[[type]][[var]]
  xclass = "list" %in% class(out)
  if(isTRUE(!xclass) & isTRUE(expected))
    out = apply(out, c(1,2), mean, na.rm=TRUE)
  return(out)
}


#' Print informations about Osmose outputs
#'
#' @param x Osmose outputs (see the \code{\link{osmose2R}} function)
#' @method print osmose
#'
#' @export
print.osmose = function(x) {
  cat(paste0("OSMOSE v.", x$model$version,"\n"))
  cat("Model", sQuote(x$model$model),"\n")
  cat(x$model$sp, " species modeled (",x$model$simus,
      " simulations):\n", sep="")
  cat(paste(x$species, collapse=", "),".\n", sep="")
}

#' Summarizes informations about Osmose outputs
#'
#' @param x Osmose outputs (see the \code{\link{osmose2R}} function)
#' 
#' @method summary osmose
#' @export
summary.osmose = function(object) {
  
  output = object$model
  output$species = object$species
  biomass = apply(object$global$biomass, 2, mean, na.rm=TRUE)
  yield = apply(object$global$yield, 2, mean, na.rm=TRUE)
  resumen = data.frame(biomass=biomass,
                       yield = yield)
  rownames(resumen) = object$species
  output$resumen = resumen
  
  class(output) = "summary.osmose"
  return(output)
  
}

#' Print the summary informations about Osmose outputs
#'
#' @param x Osmose outputs (see the \code{\link{osmose2R}} function)
#' @method print summary.osmose
#'
#' @export
print.summary.osmose = function(x, ...) {
  cat(paste0("OSMOSE v.", x$version,"\n"))
  cat("Model", sQuote(x$model),"\n")
  cat(x$sp, "species modeled:\n")
  cat(paste(x$species, collapse=", "),".\n", sep="")
  cat("Main indicators:\n")
  print(x$resumen)
}
