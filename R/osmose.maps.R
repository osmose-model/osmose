
# createOsmoseMaps - main -------------------------------------------------


#' Create osmose maps
#'
#' @param ... Several object containing maps for species distribution
#' @param outdir 
#' @param confdir 
#' @param write Boolean, write maps to csv files for osmose?
#' @param confile File to save osmose configuration for choosen maps.
#'
#' @return
#' @export
#'
#' @examples
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
  DateStamp(nmap, "maps written.")
  
  return(invisible())
}

#' Set maps of species distribution for OSMOSE
#'
#' @param object 
#' @param type 
#' @param interannual 
#' @param start 
#' @param end 
#' @param sp 
#' @param lifespan 
#' @param ages 
#' @param frequency 
#' @param toPA 
#' @param prob 
#' @param criteria 
#' @param normalize 
#' @param lat 
#' @param lon 
#'
#' @return
#' @export
#'
#' @examples
setOsmoseMaps = function(object, type, sp, lifespan, frequency=24, normalize=TRUE, ...) {
  UseMethod("setOsmoseMaps")
}

#' @export
setOsmoseMaps.prediction.niche.models = 
  function(object, type, interannual=FALSE, start=NULL, end=NULL, sp, lifespan, 
           ages=NULL, frequency=24, toPA=TRUE,
           prob=TRUE, criteria="MinROCdist",
           normalize=TRUE, lat=NULL, lon=NULL) {
  
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

#' Write maps for osmose species distribution
#'
#' @param object 
#' @param files 
#' @param outdir 
#'
#' @return
#' @export
#'
#' @examples
writeMaps = function(object, files=NULL, outdir="maps") {
  if(is.null(files)) files = object$files
  if(!file.exists(outdir)) dir.create(outdir, recursive = TRUE)
  if(!file.exists(file.path(outdir, "_images"))) 
    dir.create(file.path(outdir, "_images"))
  maps = kali::rotate(object$map, direction="anticlockwise")
  nmap = if(is.matrix(maps)) 1 else dim(maps)[3]
  for(i in seq_len(nmap)) {
    file = file.path(outdir, paste0(files[i], ".csv"))
    png(filename=file.path(outdir, "_images", paste0(files[i], ".png")))
    image.plot(kali:::.rotate(maps[,,i]))
    dev.off()
    write.table(maps[,,i], file=file, sep=";", quote=FALSE,
                row.names=FALSE, col.names=FALSE, na="-99")
  }
  return(invisible())
}

