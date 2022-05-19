
#' Create initialization file for an OSMOSE configuration
#'
#' @param input Filename of the main OSMOSE configuration file
#' @param file File to write the initialization configuration
#' @param type 
#' @param parameters 
#' @param output 
#' @param log 
#' @param version 
#' @param osmose 
#' @param java 
#' @param options 
#' @param verbose 
#' @param clean 
#' @param force 
#' @param run 
#' @param append 
#' @param ... 
#'
#' @return
#' @export
#'
#' @examples
initialize_osmose = function(input, file, type="internannual", parameters = NULL, output = NULL, 
                             log = "osmose.log", version = NULL, osmose = NULL, 
                             java = "java", options = NULL, verbose = TRUE, 
                             clean = TRUE, force = FALSE, run=TRUE, append=FALSE, 
                             ...) {

  input = suppressWarnings(normalizePath(input, mustWork=TRUE))
  file  = suppressWarnings(normalizePath(file))
  
  if(identical(input, file)) {
    bck = paste(input, ".bkg", format(Sys.time(), format="%y%m%d%H%M%S"), sep="")
    file.copy(from=input, to=bck)
  }
  
  out = switch(type,
               "ncdf"  = init_ncdf(input=input, file=file, parameters=parameters, output=output, 
                                        log=log, version=version, osmose=osmose, 
                                        java=java, options=options, verbose=verbose, 
                                        clean=clean, force=force, run=run, append=append, ...),
               "climatology"  = init_firstyear(input=input, file=file, parameters=parameters, output=output, 
                                               log=log, version=version, osmose=osmose, 
                                               java=java, options=options, verbose=verbose, 
                                               clean=clean, force=force, run=run, append=append, ...),
               "internannual" = init_sofia(input=input, file=file, test=run, ...),
               stop(sprintf("Type='%s' is not supported.", type))
  )  

  # write the output
  msg = sprintf("# OSMOSE initialization configuration (created %s)\n", date())
  cat(msg, file=file, append=append)
  cat("# Do not edit by hand.\n", file=file, append=TRUE)
  suppressWarnings(write_osmose(out, file=file, append=TRUE))
  
  return(invisible(out))
    
}

