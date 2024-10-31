
#' Create initialisation file for an OSMOSE configuration
#'
#' @param file File to write the initialization configuration
#' @param type Initialisation type: currently 'interannual', 'climatological' and 'ncdf'
#' @param run Do we have to run the model to produce the outputs or are already there?
#' @param append To append or not the results to \code{file}
#' @param sp Species to initialise. NULL by default, meaning all species.
#' @param ... Additional arguments, currently unused.
#' @inheritParams run_osmose
#'
#' @return The side effect is to create a configuration file with the initialization parameters. 
#' @export
#'
initialize_osmose = function(input, file, type="internannual", parameters = NULL, output = NULL, 
                             log = "osmose.log", version = NULL, osmose = NULL, 
                             java = "java", options = NULL, verbose = TRUE, 
                             clean = TRUE, force = FALSE, run=TRUE, append=FALSE, sp=NULL,
                             ...) {

  if(is.null(version)) version = packageVersion("osmose")
  
  restart_par = if(version >= "4.4.0") "-Psimulation.restart.enabled=TRUE" else "-Poutput.restart.enabled=TRUE"
  
  parameters = paste(restart_par, parameters)
  
  input = suppressWarnings(normalizePath(input, mustWork=TRUE))
  
  if(missing(file)) {
    conf = read_osmose(input=input)
    file = get_par(conf, "osmose.configuration.initialization")
    if(is.null(file)) {
      file = file.path(dirname(input), "input", "initial_conditions.osm")
      if(!is.null(attr(file, "path"))) file = file.path(attr(file, "path"), file)
      file  = suppressWarnings(normalizePath(file))
    }
      
  }
    
  if(!is.null(attr(file, "path"))) file = file.path(attr(file, "path"), file)
  file  = suppressWarnings(normalizePath(file))
  
  if(identical(input, file)) {
    bck = paste(input, ".backup", format(Sys.time(), format="%y%m%d%H%M%S"), sep="")
    file.copy(from=input, to=bck)
  }
  
  out = switch(type,
               "ncdf"  = init_ncdf(input=input, file=file, parameters=parameters, output=output, 
                                        log=log, version=version, osmose=osmose, 
                                        java=java, options=options, verbose=verbose, 
                                        clean=clean, force=force, run=run, append=append, ...),
               "alaia"  = init_alaia(input=input, file=file, parameters=parameters, output=output, 
                                   log=log, version=version, osmose=osmose, 
                                   java=java, options=options, verbose=verbose, 
                                   clean=clean, force=force, run=run, append=append, ...),
               "climatology"  = init_firstyear(input=input, file=file, parameters=parameters, output=output, 
                                               log=log, version=version, osmose=osmose, 
                                               java=java, options=options, verbose=verbose, 
                                               clean=clean, force=force, run=run, append=append, ...),
               "internannual" = init_sofia(input=input, file=file, test=!run, sp=sp, ...),
               stop(sprintf("Type='%s' is not supported.", type))
  )  

  # write the output
  msg = sprintf("# OSMOSE initialisation configuration (created %s)\n", date())
  dn = dirname(file)
  if(!dir.exists(dn)) dir.create(dn, recursive=TRUE)
  cat(msg, file=file, append=append)
  cat("# Do not edit by hand.\n", file=file, append=TRUE)
  suppressWarnings(write_osmose(out, file=file, append=TRUE))
  
  if(FALSE) {
    
    out = "the plankton accessibilities"
    suppressWarnings(write_osmose(out, file=file, append=TRUE))
  }
  
  message(sprintf("Initialisation configuration file written in '%s'.", file))
  
  return(invisible(out))
    
}

