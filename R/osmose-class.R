# All the methods for osmose class ----------------------------------------

plot.osmose = function(x, type="biomass", ...) {
  
  x = getVar(x, var=type)
  plot(x)
  
  return(invisible())
}

getVar = function(object, var, ...) {
  UseMethod("getVar")
}

getVar.osmose = function(object, var, type="global", expected=TRUE, ...) {
  out = object[[type]][[var]]
  
  xclass = "list" %in% class(out)
  if(isTRUE(!xclass) & isTRUE(expected))
    out = apply(out, c(1,2), mean, na.rm=TRUE)
  
  return(out)
}


print.osmose = function(x, ...) {
    cat(paste0("OSMOSE v.", x$model$version,"\n"))
    cat("Model", sQuote(x$model$model),"\n")
    cat(x$model$sp, " species modeled (",x$model$simus,
        " simulations):\n", sep="")
    cat(paste(x$species, collapse=", "),".\n", sep="")
  }


summary.osmose = function(object, ...) {
    
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

print.summary.osmose = function(x, ...) {
  cat(paste0("OSMOSE v.", x$version,"\n"))
  cat("Model", sQuote(x$model),"\n")
  cat(x$sp, "species modeled:\n")
  cat(paste(x$species, collapse=", "),".\n", sep="")
  cat("Main indicators:\n")
  print(x$resumen)
}
