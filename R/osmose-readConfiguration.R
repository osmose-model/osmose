

# Main --------------------------------------------------------------------

.readConfiguration = function(file, recursive=TRUE, ...) {
  .read_configuration(file=file, recursive=recursive, keep.names=FALSE, conf.key="osmose.configuration", ...)
}


# Internal ----------------------------------------------------------------

#' Get a parameter from a configuration file.
#'
#' @param conf The 'osmose.configuration' object.
#' @param par The name of the parameter, partial matching is allowed.
#' @param sp The number of the species (starting at 0).
#' @param fsh The number of the fishery (starting at 0).
#' @param invert Revert the selection (all but 'par')
#' @param as.is Boolean, TRUE if the results be returned as an osmose.configuration object, FALSE for a list or vector. 
#' The default is FALSE, but only when one exact match is found.
#' @param unlist Boolean, TRUE if you want the result to be returned as a vector instead of as a list.
#' @param linear Boolean, TRUE to transform 'log' and 'logit' parameters into linear scale. 
#'
#' @return A list with all the matched parameters.
#' @export
get_par = function(conf, par=NULL, sp=NULL, fsh=NULL, sr=NULL, invert=FALSE, as.is=FALSE, unlist=FALSE, linear=FALSE) {
  xind = 0 + (!is.null(sp)) + (!is.null(fsh)) + (!is.null(sr))
  if(xind > 1) {
    warning("You can only specify one of the following arguments: sp, fsh, sr. Returning NULL.")
    return(NULL)
  }
  if(xind > 1 & !is.null(par))
    warning("Ignoring 'par' argument as sp, fsh or sr are in use.")
  
  if(!is.null(sp)) par = sprintf(".sp%d$", sp)
  if(!is.null(fsh)) par = sprintf(".fsh%d$", fsh)
  if(!is.null(sr)) par = sprintf(".sr%d$", sr)
  
  if(!is.null(par)) {
    par = tolower(par)
    out = conf[grep(names(conf), pattern=par, invert=invert)]
  } else {
    out = conf
  }
  
  if(length(out)==0) return(NULL)

  if(isTRUE(linear)) {
    # log10 to linear
    ind = grep(x=names(out), pattern="\\.log10\\.")
    out[ind] = lapply(out[ind], FUN=function(x) 10^x)
    names(out)[ind] = gsub(x=names(out)[ind], pattern="\\.log10\\.", replacement = ".")
    # log to linear
    ind = grep(x=names(out), pattern="\\.log\\.")
    out[ind] = lapply(out[ind], FUN=exp)
    names(out)[ind] = gsub(x=names(out)[ind], pattern="\\.log\\.", replacement = ".")
    # logit to linear
    ind = grep(x=names(out), pattern="\\.logit\\.")
    out[ind] = lapply(out[ind], FUN=ilogit)
    names(out)[ind] = gsub(x=names(out)[ind], pattern="\\.logit\\.", replacement = ".")
  }

  if(isTRUE(as.is)) {
    class(out) = "osmose.configuration"
    return(out)
  }
  
  if(length(out)==1) out = out[[1]]
  
  if(isTRUE(unlist)) {
    # out = out[order(names(out))]
    return(unlist(out))
  }
    
  return(out)
  
}

# to_do: find all instances of .getPar and replace by get_par
.getPar = get_par

#' @param type type of species to get the names for.
#' @param code Boolean, return the numerical code of the species or fishery?
#' @rdname get_par
#' @export
get_species = function(x, type=NULL, code=FALSE, sp=NULL, nm=NULL) {
  
  type = match.arg(type, choices = c("all", "focal", "background", "resource"))
  
  if(!is.null(sp) & !is.null(nm)) stop("Only 'sp' or 'nm' must be provided.")
  
  if(!is.null(nm)) {
    
    sp = gsub(regmatches(nm, regexec("sp[0-9]*$", nm)), pattern="sp", replacement = "")
    sp = suppressWarnings(as.numeric(sp))
    out = get_species(x)[match(x=sp, get_species(x, code=TRUE))]
    return(out)
    
  }
  
  if(!is.null(sp)) {
    isp = as.numeric(get_species(x, type=type, code = TRUE)[match(x=sp, get_species(x, type=type))])
    if(any(is.na(isp))) {
      xsp = paste(sp[is.na(isp)], collapse=", ")
      if(type=="all") stop(sprintf("These are not species in the model: %s.", xsp))
      stop(sprintf("These are not %s species in the model: %s.", type, xsp))
    }
    return(isp)
  }
  
  nm = unlist(get_par(x, "species.name"))
  xtype = unlist(get_par(x, "species.type"))
  this = if(type=="all") nm else nm[xtype==type]  
  xcode = gsub(names(this), pattern="species.name.sp", replacement = "")
  names(this) = NULL
  if(isTRUE(code)) return(xcode)
  return(this)
}

#' @rdname get_par
#' @export
get_fisheries = function(x, code=FALSE, fsh=NULL, nm=NULL) {
  
  if(!is.null(fsh) & !is.null(nm)) stop("Only 'fsh' or 'nm' must be provided.")
  
  if(!is.null(nm)) {
    
    fsh = gsub(regmatches(nm, regexec("fsh[0-9]*$", nm)), pattern="fsh", replacement = "")
    fsh = suppressWarnings(as.numeric(fsh))
    out = get_fisheries(x)[match(x=fsh, get_fisheries(x, code=TRUE))]
    return(out)
    
  }
  
  if(!is.null(fsh)) {
    isp = as.numeric(get_fisheries(x, code=TRUE)[match(x=fsh, get_fisheries(x))])
    if(any(is.na(isp))) {
      xsp = paste(fsh[is.na(isp)], collapse=", ")
      stop(sprintf("These are not fisheries in the model: %s.", xsp))
    }
    return(isp)
  }
  
  this = unlist(get_par(x, "fisheries.name"))
  xcode = gsub(names(this), pattern="fisheries.name.fsh", replacement = "")
  names(this) = NULL
  if(isTRUE(code)) return(xcode)
  return(this)
}

#' @rdname get_par
#' @export
get_surveys = function(x, code=FALSE, sr=NULL, nm=NULL) {
  
  if(!is.null(sr) & !is.null(nm)) stop("Only 'sr' or 'nm' must be provided.")
  
  if(!is.null(nm)) {
    
    sr = gsub(regmatches(nm, regexec("sr[0-9]*$", nm)), pattern="sr", replacement = "")
    sr = suppressWarnings(as.numeric(sr))
    out = get_surveys(x)[match(x=sr, get_surveys(x, code=TRUE))]
    return(out)
    
  }
  
  if(!is.null(sr)) {
    isp = as.numeric(get_surveys(x, code=TRUE)[match(x=sr, get_surveys(x))])
    if(any(is.na(isp))) {
      xsp = paste(sr[is.na(isp)], collapse=", ")
      stop(sprintf("These are not surveys in the model: %s.", xsp))
    }
    return(isp)
  }
  
  this = unlist(get_par(x, "surveys.name", as.is=TRUE))
  xcode = gsub(names(this), pattern="surveys.name.sr", replacement = "")
  names(this) = NULL
  if(isTRUE(code)) return(xcode)
  return(this)
}
  


.time.conv = function(ndt_in, ndt, Tref, T) {
  caltime = seq(from=0, by=1/ndt_in, length.out=Tref+1)
  simtime = seq(from=0.5/ndt, by=1/ndt, length.out=T)
  ind = cut(simtime, breaks = caltime, labels = FALSE)
  const = rle(ind)
  const$values = 1/const$lengths
  w = inverse.rle(const)
  return(list(ind=ind, w=w))
}

.bioguess = function(x, ndt, ts=FALSE) {
  bio = x$biomass
  bguess = x$bioguess
  msg = sprintf("More than one '%s' has been provided.", unique(names(bguess)))
  if(length(bguess)>1) stop(msg)
  if(!is.null(bguess)) {
    out = bguess
    out_ts = rep(NA, ndt)
    out_ts[1] = out
    if(isTRUE(ts)) return(out_ts) else return(out)
  }
  if(all(is.na(bio))) stop("No biomass information is provided.") # check
  out_ts = bio[1:ndt]
  out = mean(out_ts, na.rm=TRUE)
  if(is.na(out)) {
    message("No biomass information for the first year, using average of time series.")
    out = mean(bio, na.rm=TRUE)
    out_ts = rep(NA, ndt)
    out_ts[1] = out
  }
  if(isTRUE(ts)) return(out_ts) else return(out)
} 

.readCSV = function(file, ...) {
  
  .guessSeparator = function(Line) {
    SEPARATORS = c(equal = "=", semicolon = ";",
                   coma = ",", colon = ":", tab = "\t")
    guess = which.min(nchar(lapply(str_split(Line,SEPARATORS), "[", i = 1)))
    separator = SEPARATORS[guess]
    
    return(separator)
  }
  
  if(length(file)!=1) stop("You can only read one file at the time.")
  file = if(!is.null(attr(file, "path"))) file.path(attr(file, "path"), file) else file
  
  config = readLines(file) # read lines
  sep = names(which.max(table(sapply(config, .guessSeparator))))
  
  out = if(sep==",") read.csv(file=file, ...) else read.csv2(file=file, sep=sep, ...)
  return(out)
  
}

# Data Parsing ------------------------------------------------------------

.setupInitialization = function(conf) {
  
  nsp = .getPar(conf, "simulation.nspecies")
  
  spind = .getPar(conf, "species.type") == "focal"
  spind = gsub(names(spind)[which(spind)], pattern="species.type.sp", replacement = "") 
  spnames = .getPar(conf, "species.name")[sprintf("species.name.sp%s", spind)]
  spind = as.numeric(spind)
  
  for(sp in spind) {
    
    sim = list()
    sim$cal       = read.cal(conf, sp)
    sim$biomass   = read.biomass(conf, sp)
    sim$yield     = read.yield(conf, sp)
    sim$fecundity = read.fecundity(conf, sp)
    sim$bioguess  = .getPar(.getPar(conf, sp=sp), "observed.biomass.guess")
    isp = sprintf("osmose.initialization.data.sp%d", sp)
    conf[[isp]]   = sim
    
  }
  
  return(conf)
  
}


read.cal = function(conf, sp) {
  
  MSG = NULL
  
  this = .getPar(conf, sp=sp)
  ndt = conf$simulation.time.ndtperyear
  T   = conf$simulation.time.nyear*ndt
  
  spname = .getPar(this, "species.name")
  landings = read.yield(conf, sp)
  
  start = .getPar(conf, par="simulation.time.start")
  if(is.null(start)) start = attr(landings, "start")
  
  time = start + seq(from=0.5/ndt, by=1/ndt, length=T)
  
  harvested = !all(landings==0)
  
  if(!isTRUE(harvested)) {
    
    Linf = .getPar(this, "species.linf")
    bins = pretty(c(0, 0.9*Linf), n=15)
    dbin = unique(diff(bins))
    length_classes = 0.5*head(bins, -1) + 0.5*tail(bins, -1)
    
    newmat = matrix(0, nrow=T, ncol=length(length_classes))
    
    output = list(cal=NULL, marks=length_classes, dbin=dbin, mat=newmat, bins=bins,
                  harvested=FALSE, time=time)
    return(output)
    
  }
  
  a = .getPar(this, "species.length2weight.condition.factor")
  b = .getPar(this, "species.length2weight.allometric.power")
  
  file = .getPar(this, "fisheries.catchatlength.file")
  msg = sprintf("Only one catch-at-length file must be provided for %s.", spname)
  if(length(file)>1) stop(msg)
  if(is.null(file)) return(NULL)
  
  if(!is.null(attr(file, "path"))) file = file.path(attr(file, "path"), file)
  if(!file.exists(file)) stop(sprintf("File '%s' not found.", file))
  periods = c("year", "quarter", "month", "week","period")
  out = read.csv(file, check.names = FALSE)
  must = names(out)[names(out) %in% periods]
  msg = sprintf("Missing time information in %s's catch-at-length file.", spname)
  if(length(must)<1) stop(msg)
  check = !(must %in% names(out))
  if(any(check)) stop(msg)
  length_classes = as.numeric(setdiff(colnames(out), must))
  bad = paste(setdiff(colnames(out), must)[is.na(length_classes)], collapse=", ")
  if(any(is.na(length_classes)))
    stop(sprintf("Size class marks should be numeric, check: %s in %s's file.", bad, spname))
  check = !identical(length_classes, sort(length_classes))
  msg = sprintf("Catch-at-length size classes must be in increasing order, check %s's file.", spname)
  if(check) stop(msg)
  dbin = unique(round(diff(length_classes), 2))
  msg = sprintf("Catch-at-length size classes must be provided in regular size bins, check %s's file.", spname)
  if(length(dbin)!=1) stop(msg)
  
  mat = as.matrix(out[, as.character(length_classes)])

  ndtcal = .getPar(this, "fisheries.catchatlength.ndtPerYear")
  msg = sprintf("Parameter 'fisheries.catchatlength.ndtPerYear.sp%d' is missing.", sp)
  if(is.null(ndtcal)) stop(msg)  
  nT = conf$simulation.time.nyear*ndtcal 
  
  msg = sprintf("Catch-at-length data for %s is incomplete, %d rows found and at least %d rows are expected (%d x %d).",
                spname, nrow(mat), nT, conf$simulation.time.nyear, ndtcal)
  if(nrow(mat)<nT) stop(msg)
  msg = sprintf("Only first %d rows of %s's catch-at-length data are used for %d years of simulation.", nT, spname, T/ndt)
  if(nrow(mat)>nT) MSG = c(MSG, msg)
  mat = mat[seq_len(nT), ]
  
  if(all(is.na(mat))) {
    
    msg = sprintf("No catch-at-length data in %s's file is provided, all NAs.", spname)
    message(msg)
    
    Linf = .getPar(this, "species.linf")
    bins = pretty(c(0, 0.9*Linf), n=15)
    dbin = unique(diff(bins))
    length_classes = 0.5*head(bins, -1) + 0.5*tail(bins, -1)
    # check for the right filling
    newmat = matrix(NA, nrow=T, ncol=length(length_classes))
    output = list(cal=NULL, marks=length_classes, dbin=dbin, mat=newmat, bins=NULL,
                  harvested=TRUE, time=time)
    
  }
  
  ix = .time.conv(ndtcal, ndt, nrow(mat), T)
  bins = c(length_classes, length_classes[length(length_classes)] + dbin)
  bins = pmax(0, bins)
  
  isize = pmax(bins, .getPar(this, "egg.size")) 
  
  L1 = head(isize, -1)
  L2 = tail(isize, -1)
  W2 = (a/(b+1))*(L2^(b+1) - L1^(b+1))
  
  newmat = ix$w*mat[ix$ind, ]
  
  wmat = t(t(newmat)*W2)
  ilandings = 1e-6*rowSums(wmat)
  ilandings[ilandings==0] = 1
  units = landings/ilandings
  if(all(is.na(units))) {
    units = 1 # assume CAL is unbiased
    msg = sprintf("No landing data for %s, assuming catch-at-length is unbiased and representing the full landings. 
            If TRUE, please manually calculate the landings from catch-at-length and run the initialization again.", spname)
    MSG = c(MSG, msg)
  }
  
  units[is.na(units)] = 1 # assume unbiased when landing data is not available.
  
  newmat = newmat*units
  
  # check for incomplete rows
  
  allna = apply(newmat, 1, FUN = function(x) all(is.na(x)))
  
  if(any(allna)) {
    
    calmean = colMeans(newmat, na.rm=TRUE)
    newmat[allna, ] = calmean

    wmat = t(t(newmat)*W2)
    ilandings = 1e-6*rowSums(wmat)
    ilandings[ilandings==0] = 1
    units = landings/ilandings
    if(all(is.na(units))) {
      units = 1 # assume CAL is unbiased
      msg = sprintf("No landing data for %s, assuming catch-at-length is unbiased and representing the full landings. 
            If TRUE, please manually calculate the landings from catch-at-length and run the initialization again.", spname)
      MSG = c(MSG, msg)
    }
    
    units[is.na(units)] = 1 # assume unbiased when landing data is not available.
    
    newmat = newmat*units
    
  }
  
  # check for maximum size
  
  Lmax = ncol(newmat) - which.max(rev(diff(cumsum(colSums(newmat, na.rm=TRUE))))>0)
  Lmax = min(ncol(newmat), Lmax + 3)
  
  imax = which.max(cumsum(colSums(wmat, na.rm=TRUE))/sum(wmat, na.rm=TRUE) > 0.999)
  lmax_cal = length_classes[imax]
  
  Amax = .getPar(this, "species.lifespan")
  Linf = .getPar(this, "species.Linf")
  
  marks = length_classes[seq_len(Lmax)]
  bins = c(marks, marks[length(marks)] + dbin)
  bins = pmax(0, bins)
  
  newmat = newmat[, seq_len(Lmax)]
  lmax_pop = VB(Amax, this)
  
  ratio = VB(Amax+0.5, this)/lmax_cal
  
  iinf = which.min((marks - lmax_pop)^2)
  icat = cumsum(colSums(wmat, na.rm=TRUE))[c(iinf, Lmax)]
  irat = icat[1]/icat[2]
  
  # validation
  msg1 = sprintf("Maximum length for %s in the model (%0.1f cm at %d years) is lower than maximum reported size in landings (%0.2fcm), check catch-at-length data.",
                 .getPar(this, "species.name"), lmax_pop, Amax, lmax_cal)
  msg2 = sprintf("Maximum length for %s in the model (%0.1f cm at %d years) is %0.1f%% of Linf (%0.1fcm), check growth parameters.",
                 .getPar(this, "species.name"), lmax_pop, Amax, 100*lmax_pop/Linf, Linf)
  msg3 = sprintf("Only %0.1f%% of landings are used! Check catch-at-length data and growth parameters.",
                 100*irat)
  
  msg = NULL
  if(ratio<1) msg = c(msg, msg1)
  if(lmax_pop/Linf<0.9) msg = c(msg, msg2)
  if(irat<0.99) msg = c(msg, msg3)
  
  MSG = c(MSG, msg)
  
  if(!is.null(MSG)) message(paste(MSG, collapse="\n"))
  
  output = list(cal=out, marks=marks, dbin=dbin, mat=newmat, bins=bins, harvested=TRUE, time=time)
  
  return(output)
}

read.biomass = function(conf, sp) {
  
  this = .getPar(conf, sp=sp)
  ndt = conf$simulation.time.ndtperyear 
  T = ndt*conf$simulation.time.nyear
  biofile = .getPar(this, "observed.biomass.file")
  if(is.null(biofile)) {
    message(sprintf("Observed biomass has not been provided for species %d, using 'observed.biomass.guess' instead.", sp))
    return(NULL)
  }
    
  q = .getPar(this, "observed.biomass.q")
  if(is.null(q)) q = 1
  bioref = .readCSV(biofile)
  ivar= .getPar(this, "species.name")
  ndtbio = .getPar(this, "observed.biomass.ndtPerYear")
  if(is.null(ndtbio)) stop("Parameter 'observed.biomass.ndtPerYear' is missing.")
  ix = .time.conv(ndtbio, ndt, nrow(bioref), T)
  biomass = bioref[ix$ind, ivar]/q
  return(biomass)
  
}

read.yield = function(conf, sp) {
  
  this = .getPar(conf, sp=sp)
  ndt = conf$simulation.time.ndtperyear 
  T = ndt*conf$simulation.time.nyear
  biofile = .getPar(this, "fisheries.yield.file")
  if(is.null(biofile)) stop("Landings have not been provided.")
  bioref = .readCSV(biofile)
  ivar= .getPar(this, "species.name")
  ndtbio = .getPar(this, "fisheries.yield.ndtPerYear")
  if(is.null(ndtbio)) stop("Parameter 'fisheries.yield.ndtPerYear' is missing.")
  ix = .time.conv(ndtbio, ndt, nrow(bioref), T)
  biomass = ix$w*bioref[ix$ind, ivar]
  
  iyear = 0
  if(!is.null(bioref$year)) iyear = min(bioref$year, na.rm=TRUE)
  
  attr(biomass, "start") = iyear
  
  return(biomass)
  
}

read.fecundity = function(conf, sp) {
  
  this = .getPar(conf, sp=sp)
  opar = .getPar(this, "reproduction.fecundity.sp")
  if(!is.null(opar)) return(opar)
  repfile = .getPar(this, "reproduction.season.file")
  fecundity = as.numeric(unlist(.readCSV(repfile, row.names = 1)))
  sfec = sum(fecundity) 
  if(abs(sfec-1)< 1e-2) {
    warning("Using relative fecundities.")
    relfec = .getPar(this, "species.relativefecundity")
    fecundity = relfec*fecundity
  }
  return(fecundity)
  
}



