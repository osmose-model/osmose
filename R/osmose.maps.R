
# createOsmoseMaps - main -------------------------------------------------


#' Create osmose maps
#'
#' @param ... Several object containing maps for species distribution
#' @param outdir Directory to write maps, only used if \code{write} is TRUE.
#' @param confdir Directory to save the configuration file.
#' @param write Boolean, write maps to csv files for osmose?
#' @param confile File to save osmose configuration for choosen maps.
#' @param prefixPath Path where the maps are located, used to write the full
#' path in the configuration files.
#'
#' @return \code{NULL}, it writes the maps to the disk.
#' @export
#'
createOsmoseMaps = function(..., outdir=NULL, confdir=NULL, write=TRUE, 
                            confile = "maps-parameters.csv", prefixPath="maps") {
  
  .par = function(par, i=NULL, type=NULL) {
    if(!is.null(type)) par = paste(par, type, sep=".")
    sprintf(paste("movement", "map%1d", par, sep="."), i)
  }
  
  maps = list(...)
  if(length(maps)==1 & is.list(maps[[1]])) maps = maps[[1]]
  
  nmap = 0
  conf = list()
  for(i in seq_along(maps)) {
    obj = maps[[i]]
    if(write) writeMaps(obj, outdir=outdir)
    n = obj$info$nmap
    for(j in seq_len(n)) {
      conf[[.par("age.max", nmap)]] = max(obj$info$ages)
      conf[[.par("age.min", nmap)]] = min(obj$info$ages)
      conf[[.par("file", nmap)]]    = file.path(prefixPath, paste0(obj$files[j], ".csv"))
      conf[[.par("season", nmap)]]  = obj$info$steps[j,]
      conf[[.par("species", nmap)]] = obj$info$sp
      conf[[.par("year.min", nmap)]] = obj$info$year.min[j]
      conf[[.par("year.max", nmap)]] = obj$info$year.max[j]
      nmap = nmap + 1
    }
  }
  writeOsmoseParameters(conf=conf, file=file.path(confdir, confile))
  message(paste(nmap, "maps written.", sep=" "))
  
  return(invisible())
}

#' Set maps of species distribution for OSMOSE
#'
#' @param object An object with species distribution maps. Currently only work
#' with the \code{predict.niche.models} class from package \code{kali}.
#' @param type Types of maps used: climatology (average for each time step),
#' seasonal (average for each season: summer, winter, spring, fall) and annual 
#' (average for all year) are available.
#' @param sp Species name as used in the osmose model.
#' @param lifespan Life span os the species.
#' @param frequency Number of simulation time steps per year.
#' @param ages set of ages corresponding to the maps. Default it \code{NULL} which
#' means for all ages (from 0 to \code{lifespan})
#' @param normalize boolean, make all probabilities add up to 1?
#' @param interannual boolean, uses the specified type for each year 
#' independently or for all the period.
#' @param ... Additional arguments for specific methods. 
#'
#' @return A list with the information to write osmose maps to the disk.
#'
setOsmoseMaps = function(object, type, sp, lifespan, frequency=24, 
                         ages=NULL, normalize=TRUE, interannual=FALSE, ...) {
  UseMethod("setOsmoseMaps")
}

# @param start argument to extract a window from the time series: c(year, month)
# @param end argument to extract a window from the time series: c(year, month)
# @param toPA boolean, transform map information in presence/absence (0/1)?
# @param prob boolean, keep probabilities inside the presence area?
# @param criteria criteria used to calculate the threshold for toPA.
# @param lat 
# @param lon

#' @export
setOsmoseMaps.prediction.niche.models = 
  function(object, type, sp, lifespan, frequency=24, ages=NULL, normalize=TRUE, 
           interannual=FALSE, start=NULL, end=NULL,  
           toPA=TRUE, prob=TRUE, criteria="MinROCdist", lat=NULL, lon=NULL, ...) {
  
    if(!requireNamespace("kali", quietly = TRUE)) 
      stop("You need to install the 'kali' package.")
    
  if(is.null(ages)) ages = seq_len(ceiling(lifespan) + 1) - 1
  
  object = kali::window.prediction.niche.models(object, start=start, end=end) 
  
  nstep = switch(type,
                 climatology = frequency/12,
                 seasonal    = frequency/4,
                 annual      = frequency,
                 frequency)
  
  # take care of different ages
  if(interannual) {
    
    map = switch(type,
                 climatology = object$prediction,
                 seasonal    = kali::getSeasonalMap(object),
                 annual      = kali::climatology(object$prediction, 
                                           object$info$time$year),
                 object$prediction) # create arrayts package 
    
    index = switch(type,
                   climatology = "month",
                   seasonal    = "season",
                   annual      = "year",
                   "month")
    
    year.min = kali::getYearMin(object, index)
    year.max = kali::getYearMax(object, index)
    
    nmap = if(is.matrix(map)) 1 else dim(map)[3]
    
    
  } else {
    map = switch(type,
                 climatology = object$climatology,
                 seasonal    = object$season,
                 annual      = object$mean,
                 object$mean)
    
    year.min = 0
    year.max = max(object$info$time$year) - min(object$info$time$year) 
    
    nmap = if(is.matrix(map)) 1 else dim(map)[3]
    
    year.min = rep(year.min, nmap)
    year.max = rep(year.max, nmap)
    
  }
  
  map = kali::removeSector(map, coords=object$info$coords, lat=lat, lon=lon)
  
  thr = kali::getThreshold(x=object, criteria=criteria)
  if(toPA) map = kali::toPA.default(map, thr=thr, prob=prob)
  if(normalize) map = kali::normalize(map)
  
  
  mapNames = paste(sp, .niceSeq(nmap), sep="-")
  # generalize 'steps'
  steps = matrix(seq_len(frequency)-1, ncol=nstep, nrow=nmap, byrow=TRUE)
  
  
  info = list(nmap = nmap, sp=sp, lifespan=lifespan, ages=ages,
              frequency=frequency, steps=steps, year.min=year.min, 
              year.max=year.max)
  
  output = list(map = map, files=mapNames, info=info)
  return(output)
}

# createOsmoseMaps - auxiliar functions -----------------------------------

# Write maps for osmose species distribution
#
# @param object 
# @param files 
# @param outdir 
#
# @return
#
writeMaps = function(object, files=NULL, outdir="maps") {
  if(!requireNamespace("kali", quietly = TRUE)) 
    stop("You need to install the 'kali' package.")
  if(is.null(files)) files = object$files
  if(!file.exists(outdir)) dir.create(outdir, recursive = TRUE)
  if(!file.exists(file.path(outdir, "_images"))) 
    dir.create(file.path(outdir, "_images"))
  maps = kali::rotate(object$map, direction="anticlockwise")
  nmap = if(is.matrix(maps)) 1 else dim(maps)[3]
  for(i in seq_len(nmap)) {
    file = file.path(outdir, paste0(files[i], ".csv"))
    png(filename=file.path(outdir, "_images", paste0(files[i], ".png")))
    fields::image.plot(kali::rotate(maps[,,i]))
    dev.off()
    write.table(maps[,,i], file=file, sep=";", quote=FALSE,
                row.names=FALSE, col.names=FALSE, na="-99")
  }
  return(invisible())
}

