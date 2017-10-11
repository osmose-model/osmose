# All the methods for osmose class ----------------------------------------


#' Plot method for osmose objects
#' @description This method takes a \code{osmose} object to make useful plot 
#' for each variables.
#' @param x Object of \code{osmose} class (see the \code{\link{osmose2R}} function).
#' @param type Name of variable to plot. It could be: "biomass","abundance",
#'  "yield" and "yieldN".
#' @param ... Additional arguments of the function.
#' @return invisible()
#' @export
plot.osmose = function(x, type = "biomass", ...) {
  
  x = getVar(x, var = type, expected = FALSE)
  plot(x, ...)
  
  return(invisible())
}


#' Get variable
#' @description Function to get a variable from an object of \code{osmose} 
#' class. This function uses the getVar method (see the \code{\link{getVar.osmose}}).
#' @param object Object of \code{osmose} class (see the \code{\link{osmose2R}} function).
#' @param var Name of variable to extract. It could be: "biomass","abundance",
#'  "yield", "yieldN".
#' @param ... Additional arguments of the function.
#' @return An array or a list containing the extracted data.
#' @export
getVar = function(object, var, ...) {
  UseMethod("getVar")
}

#' GetVar method for osmose objects
#' @description 
#' @param object Object of \code{osmose} class (see the \code{\link{osmose2R}} function).
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
getVar.osmose = function(object, var, type = "global", expected = FALSE, ...) {
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
