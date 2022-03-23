
#' Update OSMOSE maps from csv (v3.x) to ncdf (v4.2+)
#'
#' @param input Path to the input configuration file (v3)
#' @param output Path to the folder to store the ncdf maps
#' @param conf Path to the output configuration file (v4)
#' @param sep Separator used in the csv maps, default to comma.
#' @param na.strings Value used for 'land' in the maps, default to -99.
#'
#' @return
#' @export
#'
#' @examples
update_maps = function(input, output, conf, sep = ",", na.strings = -99, test=FALSE) {

  xconf = .readConfiguration(input)
  ndt = .getPar(xconf, "simulation.time.ndtperyear")
  nrow = .getPar(xconf, "nline")
  ncol = .getPar(xconf, "ncol")

  mcon = .getPar(xconf, "movement.map")
  smap = .getPar(mcon, "species")
  allsp = unique(unlist(smap))

  rpath = R.utils::getRelativePath(output, relativeTo=dirname(conf))

  out = NULL
  nmap = 0
  for(i in seq_along(allsp)) {
    ind = names(smap)[unlist(smap)==allsp[i]]
    mat = data.frame(sp=unlist(mcon[ind]),
                     min=unlist(mcon[gsub(ind, pattern="species", replacement = "age.min")]),
                     max=unlist(mcon[gsub(ind, pattern="species", replacement = "age.max")]))
    maps = split(ind, f=as.list(mat), drop=TRUE, lex.order=TRUE, sep="__")
    yy = lapply(maps, .mergeMaps, conf=mcon, dim=c(ncol, nrow, ndt), sep=sep, 
                na.strings=na.strings, test=test)

    file = file.path(output, sprintf("%s.nc", allsp[i]))
    vars = sprintf("stage%d", seq_along(yy)-1)
    longs = sapply(strsplit(names(yy), split="__"), FUN=.writelabel)
    dim = setNames(lapply(base::dim(yy[[1]]), seq_len), nm=c("x", "y", "time"))
    imap = seq_along(yy) - 1 + nmap
    nmap = nmap + length(yy)

    xout = mapply(FUN = .mapConfig, x=vars, y=strsplit(names(yy), split="__"), imap=imap, 
                  MoreArgs =list(file=file, ndt=ndt, rpath=rpath),  SIMPLIFY = FALSE)

    out = c(out, xout)

    write_ncdf(yy, filename = file, varid = vars, longname = longs, dim=dim, missval = -99, unlim="time")
  }

  names(out) = NULL

  return(invisible(out))

}


# Auxiliar functions ------------------------------------------------------


.mapConfig = function(x, y, imap, file, ndt, rpath) {

  nm = c("movement.species.map%d", "movement.variable.map%d",
         "movement.nsteps.year.map%d", "movement.initialAge.map%d",
         "movement.lastAge.map%d", "movement.file.map%d")
  
  out = setNames(vector(mode="list", length=6), nm=sprintf(nm, imap))
  out[[1]] = y[1]
  out[[2]] = x
  out[[3]] = ndt
  out[[4]] = as.numeric(y[2])
  out[[5]] = as.numeric(y[3])
  out[[6]] = file.path(rpath, basename(file))

  return(out)

}

.writelabel = function(x) sprintf("%s (%s-%s years)", x[1], x[2], x[3])

.mergeMaps = function(x, conf, dim, sep=",", na.strings="-99", test=FALSE) {

  files  = conf[gsub(x, pattern="species", replacement = "file")]
  season = conf[gsub(x, pattern="species", replacement = "season")]

  dat = lapply(files, FUN = .readthis, sep=sep, na.strings=na.strings)

  idim = if(isTRUE(test)) c(1,1,2) else c(1,1,1)
  out = array(dim=dim*idim)
  for(i in seq_along(season)) {
    out[,, season[[i]]+1] = dat[[i]]
    if(isTRUE(test)) out[,, season[[i]]+1+dim[3]] = dat[[i]]
  }
  return(out)
}

.readthis = function(x, sep, na.strings) {
  file = file.path(attr(x, "path"), x)
  out = as.matrix(read.csv(file, sep=sep, header=FALSE, na.strings = na.strings))
  return(.rotate(out))
}


.rotate = function(x, revCol=TRUE) {

  # clockwise
  if(length(dim(x))==2) {
    x = t(x)
    if(revCol) x = x[, ncol(x):1]
  }
  if(length(dim(x))==3) {
    x = aperm(x, c(2,1,3))
    if(revCol) x = x[, ncol(x):1, ]
  }

  return(x)
}
