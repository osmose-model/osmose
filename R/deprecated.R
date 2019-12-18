
# to keep back compatibility for a while
#' @export
osmose2R = function(path=NULL, version="v3r2", species.names=NULL, ...) {
  
  .Deprecated("read_osmose")
  read_osmose(path=path, version=version, species.names=species.names, ...)
  
}

#' @export
getVar = function(object, what, how, ...) {
  
  .Deprecated("get_var")
  get_var(object=object, what=what, how=how, ...)
  
}

#' @export
runOsmose = function(input, parameters=NULL, output="output", log="osmose.log",
                     version="4.1.0", osmose=NULL, java="java", 
                     options=NULL, verbose=TRUE, clean=TRUE) {
  
  message("runOsmose will be deprecated, use run_osmose instead.")
  
  run_osmose(input = input, parameters = parameters, output = output,
             log = log, version = version, osmose = osmose, java = java, 
             options = options, verbose = verbose, clean = clean) 
}

#' @export
write.osmose = function(x, file)   {
  .Deprecated("write_osmose")
  write.table(x=x, file=file, sep=",", col.names=NA, quote=FALSE)
}

