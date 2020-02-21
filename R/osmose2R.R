
#' Computes the average mortality.
#' 
#' It computes the mean mortality, which
#' is multiplied by the frequency.
#'
#' @param x Mortality dataframe
#' @param stage Stage ("adults", etc.)
#' @param freq Time frequency (months?)
#'
#' @return An array
#' @examples{
#' dirin = system.file("extdata", package="osmose")
#' outdir = paste(dirin, "/outputs", sep="")
#' data = read_osmose(outdir)
#' mortality_df = data$mortality
#' mort = getAverageMortality(mortality_df, stage="juveniles", freq=12)
#' }
getAverageMortality = function(x, stage="adults", freq=12) {
  
  .getZ = function(x, stage) {
    x = x[[stage]]
    x = apply(x, 1:2, mean, na.rm=TRUE)
    x = freq*colMeans(x, na.rm=TRUE)
    return(x)
  }
  
  out = sapply(x, .getZ, stage=stage)
  return(out)
}

#' Get fishing base rate.
#' 
#' @details It assumes that in the Osmose configuration, there is a "fishing" entry.
#' It first check for the "method" parameter for the current
#' specie (\emph{mortality.fishing.rate.method.spX}), whose value can be
#' 
#' \itemize{
#' \item{constant: use \emph{rate.spX}}
#' \item{byRegime: use \emph{rate.byRegime.file.spX}}
#' \item{linear  : use \emph{rate.slope.spX}}
#' \item{byYear  : use \emph{rate.byYear.spX}}
#' \item{byDt    : use \emph{rate.byDt.spX}}
#' }
#' @param sp Current specie (sp0, sp1, etc.)
#' @param fishing Fishing parameters
#' @param T Number of years
#' @param ndt  Time step
#' @examples
#' {
#' filename = system.file("extdata", "data_fishing.csv", package="osmose")
#' conf = readOsmoseConfiguration(filename)
#' ndt = getOsmoseParameter(conf, "simulation", "time", "ndtperyear")
#' nyear = getOsmoseParameter(conf, "simulation", "time", "nyear")
#' 
#' fishing = conf$mortality$fishing
#' 
#' fish0 = getFishingBaseRate("sp0", fishing, nyear, ndt)
#' }
getFishingBaseRate = function(sp, fishing, T, ndt) {
  
  method = fishing$rate$method[[sp]]
  useFiles = .getBoolean(fishing$useFiles, FALSE)
  
  base = switch(method,
                "constant" = getFishingBaseRate.constant(sp, fishing, T, ndt), 
                "byregime" = getFishingBaseRate.byRegime(sp, fishing, T, ndt), 
                "linear"   = getFishingBaseRate.linear(sp, fishing, T, ndt), 
                "byyear"   = getFishingBaseRate.byYear(sp, fishing, T, ndt), 
                "bydt"     = getFishingBaseRate.byDt(sp, fishing, T, ndt))
  
  # barrier.n: should not be done here.
  # base = rep(base, length=ndt*T) #???
  
  return(base)
}

#' Get fishing mortality rate.
#' 
#' @details \deqn{F = \frac{B \times \exp^{A+S}}{ndt}}
#' with \eqn{B}=mortality rate, \eqn{A}=deviates by year, \eqn{S}=deviates by seasons
#' 
#' @param sp Species index (sp0, sp1, sp2, etc.)
#' @param fishing List of fishing parameters
#' @param T Number of years
#' @param ndt  Time step
#'
#' @return  Fishing mortality rate.
#' @examples{
#' filename = system.file("extdata", "data_fishing.csv", package="osmose")
#' conf = readOsmoseConfiguration(filename)
#' ndt = getOsmoseParameter(conf, "simulation", "time", "ndtperyear")
#' nyear = getOsmoseParameter(conf, "simulation", "time", "nyear")
#' 
#' fishing = conf$mortality$fishing
#' 
#' fish0 = getFishingMortality("sp0", fishing, nyear, ndt)
#' }
getFishingMortality = function(sp, fishing, T, ndt) {
  
  # validation?
  B = getFishingBaseRate(sp, fishing, T, ndt)
  A = getFishingDeviatesByYear(sp, fishing, T, ndt) 
  S = getFishingDeviatesBySeason(sp, fishing, T, ndt)
  
  cat(B, "\n")
  cat(A, "\n")
  cat(S, "\n")
  
  F = B*exp(A+S)/ndt # rate by dt!
  
  return(F)
}

#' Get the total mortality rate. 
#' 
#' @param x Mortality dataframe
#' @param stage Stage ("adults", etc.)
#' @param type Mortality type ("pred", "starv", "other", "out", "total"). 
#' The latter is computed as the sum of all mortality types
#' @return A mortality array
#' @examples{
#' dirin = system.file("extdata", package="osmose")
#' outdir = paste(dirin, "/outputs", sep="")
#' data = read_osmose(outdir)
#' mortality_df = data$mortality
#' mort = getMortality(mortality_df, stage="juveniles", type="total")
#' }
getMortality = function(x, stage="adults", type="total") {
  .calcMort = function(x) {
    x = as.data.frame(x)
    x$natural = x$pred + x$starv + x$other + x$out
    x$total = x$natural + x$fishing
    return(x)
  }
  .getZ = function(x, stage, type) {
    x = x[[stage]]
    x = apply(x, 1:2, mean, na.rm=TRUE)
    x = .calcMort(x)
    x = x[, type]
    return(x)
  }
  
  out = sapply(x, .getZ, stage=stage, type=type)
  return(out)
}

#' Computes the mortality deviation. The "proxy", which is removed,
#' can be provided by the user in the "pars" argument.
#'
#' @param x Mortality dataframe
#' @param stage Stage ("adults", etc.)
#' @param type Mortality type
#' @param pars A list or data frame containing  
#' \emph{dt.save}, \emph{M.proxy}, \emph{dt} entries.
#' If NULL, then \code{proxy = colMeans(x)}
#'
#' @return An array
#' @examples{
#' dirin = system.file("extdata", package="osmose")
#' outdir = paste(dirin, "/outputs", sep="")
#' data = read_osmose(outdir)
#' mortality_df = data$mortality
#' mortdev = getMortalityDeviation(mortality_df, stage="juveniles", type="total")
#' }
getMortalityDeviation = function(x, stage, type, pars=NULL) {
  x     = getMortality(x=x, stage=stage, type=type)
  if(!is.null(pars)) {
    proxy = pars$dt.save*pars$M.proxy/pars$dt    
  } else {
    proxy = colMeans(x)
  }
  out   = t(apply(x, 1, "-", proxy))
  return(out)
}

#' Get a parameter from a name chain (error if not found).
#' @param par Output of the \code{link{readOsmoseConfiguration}} function
#' @param ... String arguments 
#' @param keep.att Whether parameter attributes should be kept
#' @examples{
#'    filename = system.file("extdata", "gog/osm_all-parameters.csv", package="osmose")
#'    par = readOsmoseConfiguration(filename)
#'    getOsmoseParameter(par, "population", "seeding", "year", "max", keep.att=FALSE)
#' }
getOsmoseParameter = function(par, ..., keep.att=FALSE) {
  chain = unlist(list(...))
  x = .getPar(par, ..., keep.att=TRUE)
  if(!isTRUE(keep.att)) attributes(x) = NULL
  if(is.null(x)) stop(sprintf("Parameter '%s' not found.", paste(chain, collapse=".")))
  return(x)
}

#' Get size spectrum
#'
#' @param file File to read
#' @param sep File separator
#' @param ... Additional arguments of the \code{read.csv} function
#'
#' @return A 3D array (time, length, species)
#' @examples{
#' dirin = system.file("extdata", package="osmose")
#' file = paste(dirin, "/outputs/SizeIndicators/gogosm_yieldDistribBySize_Simu0.csv", sep="")
#' size = getSizeSpectrum(file)
#' }
getSizeSpectrum = function(file, sep=",", ...) {
  
  # sizeSpectrum = read.table(file, sep=sep, dec=".", skip=1,
  #                          header=TRUE)
  sizeSpectrum = .readOsmoseCsv(file=file, sep=sep, header=TRUE, row.names=NULL, ...)
  
  nsp = ncol(sizeSpectrum) - 2
  times = unique(sizeSpectrum$Time)
  lengths = unique(sizeSpectrum$Size)
  
  out = array(dim = c(length(times), length(lengths), nsp))
  
  for(t in seq_along(times)) {
    out[t,,]  = as.matrix(sizeSpectrum[sizeSpectrum$Time==times[t],-(1:2)])
  }
  colnames(out) = lengths
  rownames(out) = round(times,3)
  dimnames(out)[[3]] = paste0("sp.", seq(nsp)-1)
  return(out)
}

#' Write fishing files from osmose configuration list.
#'
#' @param L1 Osmose configuration (see \code{\link{readOsmoseConfiguration}})
#' @param outputPath Output path
#' 
writeFishingFiles = function(L1, outputPath) {
  
  nsp = getOsmoseParameter(L1, "simulation", "nspecies")
  T   = getOsmoseParameter(L1, "simulation", "time", "nyear")
  ndt = getOsmoseParameter(L1, "simulation", "time", "ndtPerYear")
  
  spp = .getSpecies(L1$fishing$rate$method)
  
  if(length(spp)==0) return(NULL)
  
  ofiles = unlist(sapply(spp, FUN=.writeFishingFile, 
                         fishing=L1$mortality$fishing,
                         species=L1$species, T=T, ndt=ndt,
                         output=outputPath))
  
  return(ofiles)  
}

# TO-DO: warnings for recycling time series, generic for extension of rate methods
# check deviates are deviates, update the -9999 or equivalent

# Base rate ---------------------------------------------------------------
# mortality.fishing.rate.method.sp0;"constant","byRegime","linear", "byYear", "byDt"
# # missing/default: nothing, by default hierarchy? error?
# # constant: use rate.sp0
# # byRegime: use rate.byRegime.file.sp0
# # linear  : use rate.slope.sp0
# # byYear  : use rate.byYear.sp0;
# # byDt    : use rate.byDt.sp;
# # mean
# mortality.fishing.rate.sp0;0.5
# mortality.fishing.rate.slope.sp0;0.03 # 3% per year
# mortality.fishing.rate.byRegime.file.sp0;path/to/file
# mortality.fishing.rate.byRegime.sp0;numeric_vector
# mortality.fishing.rate.byRegime.shift.sp0;240
# mortality.fishing.rate.byYear.file.sp0;path/to/file
# mortality.fishing.rate.byYear.sp0;numeric_vector
# mortality.fishing.rate.byDt.file.sp0;path/to/file

#' Get fishing base rate using constant rate
#' 
#' @details It assumes that in the Osmose configuration, there is a "fishing" entry. It reads
#' the \emph{rate.spX} parameter, which is repeated \eqn{T \times ndt} times.
#'
#' @param sp Current specie (sp0, sp1, etc.)
#' @param fishing Fishing parameters
#' @param T Number of years
#' @param ndt  Time step
getFishingBaseRate.constant = function(sp, fishing, T, ndt) {
  rate = getOsmoseParameter(fishing, "rate", sp)
  if(is.null(rate)) stop(sprintf("No fishing rate provided for %s", sp))
  if(length(rate)>1) stop(sprintf("More than one fishing rate for %s provided.", sp))
  return(rep(rate, T*ndt))
}

getFishingBaseRate.byRegime = function(sp, fishing, T, ndt) {
  
  shifts   = getOsmoseParameter(fishing, "rate", "byregime", "shift", sp)
  nRegimes = length(shifts) + 1
  shifts   = shifts[shifts<T*ndt]
  
  useFiles = .getBoolean(fishing$useFiles, FALSE)
  
  if(useFiles) {
    rates = .getFileAsVector(fishing$rate$byRegime$file[[sp]])
    if(is.null(rates)) rates = getOsmoseParameter(fishing, "rate", "byregime", "sp")    
  } else {
    rates = getOsmoseParameter(fishing, "rate", "byregime", sp)           
    if(is.null(rates)) rates = .getFileAsVector(fishing$rate$byRegime$file[[sp]])
  }
  
  if(is.null(rates)) stop(sprintf("No fishing rates provided for %s", sp))
  if(length(rates)!=nRegimes) stop(sprintf("You must provide %d fishing rates.", nRegimes))
  
  nRegimes = length(shifts) + 1
  rates  = rates[1:nRegimes]
  
  le = diff(c(0, shifts, T*ndt))
  
  rates = rep.int(rates, le)
  
  return(rates)
}

#' Get fishing base rate using a linear rate
#' 
#' @details It assumes that in the Osmose configuration, there is a "fishing" entry. It reads
#' the \emph{rate.spX} and \emph{rate.slope.spX} parameters. 
#' The slope is computed any time the fishing frequency changes (fishingperiod parameter)
#' For instance, if ndt=20, period=5, there are 4 time steps between two fishing time step. Therefore, 
#' the value will be (i0, i0, i0, i0, i1, i1, i1, i1, ...)
#' @param sp Current specie (sp0, sp1, etc.)
#' @param fishing Fishing parameters
#' @param T Number of years
#' @param ndt  Time step
getFishingBaseRate.linear = function(sp, fishing, T, ndt) {
  
  # mortality.fishing.rate.slope.sp0;0.03 # 3% per year
  rate  = getOsmoseParameter(fishing, "rate", sp)
  slope  = getOsmoseParameter(fishing, "rate", "slope", sp)
  #rate  = fishing$rate[[sp]]
  #slope = fishing$rate$slope[[sp]]
  
  freq = .getFishingFrequency(sp, fishing, ndt)
  
  if(is.null(rate)) stop(sprintf("No fishing rate provided for %s", sp))
  if(length(rate)>1) stop(sprintf("More than one fishing rate for %s provided.", sp))
  if(is.null(slope)) stop(sprintf("No fishing slope provided for %s", sp))
  if(length(slope)>1) stop(sprintf("More than one slope for %s provided.", sp))
  
  # time is a vector that goes from 0 to T by a step of period, all in time step unit.
  # ndt/freq is the fishing period period.
  time = seq(from=0, by=freq/ndt, length=T*ndt/freq)
  rates = rate + slope*rate*time
  rates = rep(rates, each=freq)    # repeats x0 x1 to x0 x0 x0 ... x1 x1 x1 with N repetitions, N=fishing freq.
  
  return(rates)
  
}

#' Get fishing base rate using annual rates.
#' 
#' @description It reads the annual values either from an external file or from the configuration file.
#' It must contain a number of elements which is a multiple of the fishing period.
#' 
#' Warning: The number of rate elements must be a multiple of the fishing 
#' period since \strong{recycling} is performed!)
#' 
#' @param sp Current specie (sp0, sp1, etc.)
#' @param fishing Fishing parameters
#' @param T Number of years
#' @param ndt  Time step
getFishingBaseRate.byYear = function(sp, fishing, T, ndt) {
  
  useFiles = .getBoolean(fishing$useFiles, FALSE)
  
  if(useFiles) {
    rates = .getFileAsVector(fishing$rate$byyear$file[[sp]])
    if(is.null(rates)) rates = getOsmoseParameter(fishing, "rate", "byyear", sp)
  } else {
    rates = getOsmoseParameter(fishing, "rate", "byyear", sp)
    if(is.null(rates)) rates = .getFileAsVector(fishing$rate$byyear$file[[sp]])
  }
  
  # rates contains T values
  if(is.null(rates)) stop(sprintf("No fishing rates provided for %s", sp))
  
  freq = .getFishingFrequency(sp, fishing, ndt)
  nPeriods = ndt/freq   # number of fishing periods
  
  if((length(rates)%%nPeriods)!=0) 
    stop(sprintf("You must provide a multiple of %d rates for %s.", nPeriods, sp))
  
  rates = rep(rates, each=freq, length=T*ndt)
  return(rates)
  
}

#' Get fishing base rate using time-step rates.
#' 
#' @description It reads the time-step values either from an external file or from the configuration file.
#' It must contain a number of elements which is a multiple of the fishing period.
#' 
#' Warning: The number of rate elements must be a multiple of the fishing 
#' period since \strong{recycling} is performed!)
#' 
#' @param sp Current specie (sp0, sp1, etc.)
#' @param fishing Fishing parameters
#' @param T Number of years
#' @param ndt  Time step
getFishingBaseRate.byDt = function(sp, fishing, T, ndt) {
  
  useFiles = .getBoolean(fishing$useFiles, FALSE)
  
  if(useFiles) {
    rates = .getFileAsVector(fishing$rate$bydt$file[[sp]])
    if(is.null(rates)) rates = getOsmoseParameter(fishing, "rate", "bydt", sp)
  } else {
    rates = getOsmoseParameter(fishing, "rate", "bydt", sp)
    if(is.null(rates)) rates = .getFileAsVector(fishing$rate$bydt$file[[sp]])
  }
  
  if(is.null(rates)) stop(sprintf("No fishing rates provided for %s", sp))
  
  rates = rep(rates, length=T*ndt)
  return(rates)
  
}

# Annual deviates ---------------------------------------------------------
# mortality.fishing.deviate.byYear.enabled.sp0;true
# # default: false
# # true: add interannual deviates to the mean rate 
# # false: don't add interannual deviates to the mean rate
# mortality.fishing.deviate.byYear.method.sp0;"multiplicative","random.walk"
# mortality.fishing.deviate.byYear.random.sp0;false
# # default: false
# # false: use interannual deviates as time series
# # true: add interannual deviates to the mean rate from distribution
# mortality.fishing.deviate.byYear.random.method.sp0;"distribution","resampling"
# if parameters of the distribution (and distribution) are not specified, used ts
# resampling just reshuffle.
# mortality.fishing.periodsPerYear.sp0;2
# mortality.fishing.deviate.byYear.file.sp0;path/to/file
# mortality.fishing.deviate.byYear.sp0;numeric_vector # length freq*T or freq*T-1 (first is zero)

getFishingDeviatesByYear = function(sp, fishing, T, ndt) {
  
  # rate method
  rateMethod = fishing$rate$method[[sp]]
  if(rateMethod %in% c("byyear", "bydt")) return(rep(0, T*ndt))
  
  isInterannual = .getBoolean(fishing$deviate$byyear$enabled[[sp]], FALSE)
  if(!isInterannual) return(rep(0, T*ndt))
  
  # deviate method
  deviateMethod = fishing$deviate$byyear$method[[sp]]
  if(is.null(deviateMethod)) deviateMethod = "multiplicative" 
  # frequency
  freq = .getFishingFrequency(sp, fishing, ndt)
  ndev = switch(deviateMethod, 
                multiplicative = T*ndt/freq,
                random.walk    = T*ndt/freq - 1)  
  
  useFiles = .getBoolean(fishing$useFiles, FALSE)
  
  if(useFiles) {
    deviates = .getFileAsVector(fishing$deviate$byyear$file[[sp]])
    if(is.null(deviates)) deviates = fishing$deviate$byyear[[sp]]    
  } else {
    deviates = fishing$deviate$byyear[[sp]]        
    if(is.null(deviates)) deviates = .getFileAsVector(fishing$deviate$byyear$file[[sp]])
  }
  
  if(length(deviates)<ndev) 
    stop(sprintf("Not enough annual deviates provided (%d), %d needed.", 
                 length(deviates), ndev))
  
  if(deviateMethod=="random.walk") {
    deviates = cumsum(c(0, deviates))
  }
  
  deviates = rep(deviates, each=freq, length=T*ndt)
  
  return(deviates)
}

# Seasonal deviates -------------------------------------------------------

# mortality.fishing.season.method.sp0;"default", "periodic", "non-periodic"
# # missing: default.
# # default: look for non-periodic then periodic, then uniform
# # periodic: look for periodic, if not create a climatology from non-periodic, then error
# # non-periodic: look for non-periodic, then error
# mortality.fishing.deviate.season.file.sp0;path/to/file
#   mortality.fishing.deviate.season.sp0;numeric_vector # length ndt or >ndt*T
#   mortality.fishing.season.byDt.file.sp0;path/to/file # length ndt or >ndt*T 
#   mortality.fishing.season.distrib.file.sp0;

# getFishingDeviatesBySeason ----------------------------------------------

getFishingDeviatesBySeason = function(sp, fishing, T, ndt) {
  
  rateMethod = fishing$rate$method[[sp]]
  if(rateMethod == "bydt") return(rep(0, T*ndt))
  
  method = fishing$season$method[[sp]]
  if(is.null(method)) method = "default"
  
  useFiles = .getBoolean(fishing$useFiles, FALSE)
  deviates = switch(method,
                    "default"      = getFishingDeviatesBySeason.default(sp, fishing, T, ndt, useFiles),
                    "periodic"     = getFishingDeviatesBySeason.periodic(sp, fishing, T, ndt, useFiles),
                    "non-periodic" = getFishingDeviatesBySeason.nonPeriodic(sp, fishing, T, ndt, useFiles))
  deviates = rep(deviates, length=ndt*T)
  return(deviates)
}

# getFishingDeviatesBySeason.default
getFishingDeviatesBySeason.default = function(sp, fishing, T, ndt, useFiles=FALSE) {
  
  freq = .getFishingFrequency(sp, fishing, ndt)
  
  if(useFiles) {
    deviates = .getFileAsVector(fishing$deviate$season$file[[sp]])
    if(is.null(deviates)) deviates = fishing$deviate$season[[sp]]    
  } else {
    deviates = fishing$deviate$season[[sp]]        
    if(is.null(deviates)) deviates = .getFileAsVector(fishing$deviate$season$file[[sp]])
  }
  
  if(is.null(deviates)) {
    season = .getFileAsVector(fishing$season$byDt$file[[sp]])
    if(is.null(season)) {
      season = .getFileAsVector(fishing$season$distrib$file[[sp]])
    }
    if(is.null(season)) season = rep(1, ndt)
    
    stopifnot(length(season)%%ndt==0)
    
    deviates = .calculateDeviates(x=season, freq, ndt)  
  }
  
  stopifnot(length(deviates)%%ndt==0)
  
  return(as.numeric(deviates))  
  
}


# getFishingDeviatesBySeason.periodic
getFishingDeviatesBySeason.periodic = function(sp, fishing, T, ndt, useFiles=FALSE) {
  
  freq = .getFishingFrequency(sp, fishing, ndt)
  
  if(useFiles) {
    deviates = .getFileAsVector(fishing$deviate$season$file[[sp]])
    if(is.null(deviates)) deviates = fishing$deviate$season[[sp]]    
  } else {
    deviates = fishing$deviate$season[[sp]]        
    if(is.null(deviates)) deviates = .getFileAsVector(fishing$deviate$season$file[[sp]])
  }
  
  if(is.null(deviates)) {
    season = .getFileAsVector(fishing$season$distrib$file[[sp]])
    if(is.null(season)) {
      season = .getFileAsVector(fishing$season$byDt$file[[sp]]) 
      if(is.null(season)) stop(sprintf("No seasonality information for %s", sp))
    }
    stopifnot(length(season)%%ndt==0)
    deviates = .calculateDeviates(x=season, freq, ndt)  
  }
  
  stopifnot(length(deviates)%%ndt==0)
  deviates = calculateSeasonalPattern(x=deviates, ndt=ndt)
  
  return(as.numeric(deviates))
  
}


# getFishingDeviatesBySeason.nonPeriodic
getFishingDeviatesBySeason.nonPeriodic = function(sp, fishing, T, ndt, useFiles=FALSE) {
  
  freq = .getFishingFrequency(sp, fishing, ndt)
  
  if(useFiles) {
    deviates = .getFileAsVector(fishing$deviate$season$file[[sp]])
    if(is.null(deviates)) deviates = fishing$deviate$season[[sp]]    
  } else {
    deviates = fishing$deviate$season[[sp]]        
    if(is.null(deviates)) deviates = .getFileAsVector(fishing$deviate$season$file[[sp]])
  }
  
  if(is.null(deviates)) {
    season = .getFileAsVector(fishing$season$byDt$file[[sp]]) 
    if(is.null(season)) stop(sprintf("No seasonality information for %s", sp))
    stopifnot(length(season)%%ndt==0)
    deviates = .calculateDeviates(x=season, freq, ndt)  
  }
  
  if(length(deviates)<(ndt*T)) stop("Seasonal information is not appropiate.")
  
  return(as.numeric(deviates))   
  
}


.getFishingFrequency = function(sp, fishing, ndt) {
  periods = getOsmoseParameter(fishing, "periodsperyear", sp)
  if(is.null(periods)) periods = 1L 
  if(periods%%1!=0) stop(sprintf("periodsPerYear.%s must be an integer.", sp))
  freq = ndt/periods
  if(freq%%1!=0) stop(sprintf("simulation.time.ndtPerYear must be a multiple of periodsPerYear.%s.", sp))
  return(freq)
}

# Writes fishing files for individual species.
#
# @param sp Species number (sp0, sp1, etc)
# @param fishing Fishing parameters (method, etc.) 
# @param species Species parameters
# @param T Number of years
# @param ndt  Time step
# @param output Output path
.writeFishingFile = function(sp, fishing, species, T, ndt, output) {
  
  if(is.null(fishing$rate$method[[sp]])) return(NULL)
  
  fileCode = file.path(output, "fishing_%s_%s.csv")
  
  f = getFishingMortality(sp, fishing, T, ndt) 
  write.osmose(f, file=sprintf(fileCode, "byDt", species$name[sp]))
  
  selectivity = getSelectivity(sp, fishing, T, ndt)
  if(!is.null(selectivity)) {
    Fs = f %o% selectivity   # outer product. if dim(f)=N and dim(sel)=M, dim(Fs)=NxM
    #write.osmose(f, file=sprintf(fileCode, "byDt", species$name[sp]))
    write.osmose(Fs, file=file.path(output, paste0("F-", sp, ".csv")))
    
  }
  
  # fishing = cbind(fishing, f)    ????
  # write.osmose(Fs, file=file.path(output, paste0("F-", isp, ".csv")))
  
}


.normalizeByFreq = function(x, freq) {
  if(any(is.na(x))) stop("x must not contain NA.")
  if(any(x<0)) stop("x must not contain negative values")
  .norm = function(x) if(sum(x)==0) return(x) else return(x/sum(x))
  ind = rep(seq_along(x), each=freq, length=length(x))
  xNorm = tapply(x, INDEX = ind, FUN=.norm)
  xNorm = setNames(unlist(xNorm), nm = names(x))
  return(xNorm)
}

.calculateDeviates = function(x, freq, ndt) {
  x = .normalizeByFreq(x=x, freq=freq)
  deviates = log(x) + log(freq)
  return(deviates)
}

calculateSeasonalPattern = function(x, ndt) {
  rowMeans(matrix(x, nrow=ndt))
}

# Selectivity functions ---------------------------------------------------


getSelectivity = function(par, sp, n=1, tiny=1e-6) {
  out = calculateSelectivity(par=getSelectivityParameters(par=par, sp=sp), n=n, tiny=tiny)
  return(out)
}

getSelectivityParameters = function(par, sp) {
  
  output = list()
  output = within(output, {
    
    by        = par$selectivity.by[sp]
    type      = par$selectivity.type[sp]
    longevity = par$longevity[sp]
    Linf      = par$Linf[sp]
    L50       = par$L50[sp]
    L75       = par$L75[sp]
    
  })
  
  return(output)
  
}

calculateSelectivity = function(par, n=1, tiny=1e-6) {
  
  x = switch(par$by, 
             size   = pretty(c(0, 1.1*par$Linf), n=60*n),
             age    = pretty(c(0, 1.1*par$longevity), n=50*n),
             stop("Invalid selectivity type: by must be 'age' or 'length' ")
  )
  o
  par$L75 = max(1.01*par$L50, par$L75)
  
  out = switch(par$type,
               log  = .selectivity.log(x=x, L50=par$L50, L75=par$L75, tiny=tiny),
               norm = .selectivity.norm(x=x, L50=par$L50, L75=par$L75, tiny=tiny),
               lnorm = .selectivity.lnorm(x=x, L50=par$L50, L75=par$L75, tiny=tiny),
               edge = .selectivity.edge(x=x, L50=par$L50),
               stop("Invalid selectivity 'type': currently implemented 'log' and 'edge'. See help.")
  )
  
  return(out)
  
}

.selectivity.edge = function(x, L50) {
  
  selec = numeric(length(x))
  selec[x >= L50] = 1
  names(selec) = x
  return(selec)
}

.selectivity.log = function(x, L50, L75, tiny=1e-6) {
  
  s1 = (L50*log(3))/(L75-L50)
  s2 = s1/L50
  selec = 1/(1+exp(s1-(s2*x)))
  selec[selec<tiny] = 0
  names(selec) = x
  return(selec)
  
}

.selectivity.norm = function(x, L50, L75, tiny=1e-6) {
  
  sd = (L75-L50)/qnorm(0.75)
  mean = L50
  selec = dnorm(x, mean=mean, sd=sd)
  selec = selec/max(selec, na.rm=TRUE)
  selec[selec<tiny] = 0
  names(selec) = x
  return(selec)
  
}

.selectivity.lnorm = function(x, L50, L75, tiny=1e-6) {
  
  sd = log(L75/L50)/qnorm(0.75)
  mean = log(L50)
  selec = dlnorm(x, meanlog = mean, sdlog = sd)
  selec = selec/max(selec, na.rm=TRUE)
  selec[selec<tiny] = 0
  names(selec) = x
  return(selec)
  
}


.selectivity.equilibrium = function(x, M, tiny=1e-6) {
  selec = exp(-M*x)
  names(selec) = x
  return(selec)
}


#' Finds the F_msy value for a given species
#'
#' @param sp Index of the species from which F_msy is analysed
#' @param input.file Path of the configuration file
#' @param restart True if Fmsy should start from restart
#' @param Fmin Minimum value of the fishing mortality
#' @param Fmax Maximum value of the fishing mortality
#' @param StepF Fishing mortality step between Fmin and Fmax
#' @param Sub_StepF Fishing mortality step between Fmin and StepF
#' @param ...  Additionnal arguments of the run_osmose function

F_msy = function(sp, input.file, restart=FALSE,
                  Fmin=0, Fmax=2, StepF=0.1, Sub_StepF=0.01, ...) {
  
  # Initial values
  input.folder = normalizePath(dirname(input.file))
  
  Param = readOsmoseConfiguration(input.file)
  
  if(is.numeric(sp)){
    index = paste0("sp", sp)   # recovers "spX" string
    species = getOsmoseParameter(Param, "species", "name", index)   # recovers species name
  } else {
    species = sp   # recovers species name as argument
    all_species_names = Param$species$name   # recovers all the species names
    test = which(all_species_names == species)   # find the index that corresponds to the species name
    if(length(test) == 0) {
      stop("error on species indexation, invalid species name provided")
    }
    
    pattern = "sp([0-9]+)"
    sp = as.numeric(sub(x=names(test), pattern=pattern, replacement="\\1"))
    
  }
  
  index = paste0("sp", sp)   # recovers "spX" string
  
  IsSeasonal = FALSE
  
  if(existOsmoseParameter(Param, "mortality", "fishing", "rate", "byDt", "byAge", "file", index)){
    selectivity.by = "age"
    IsSeasonal = TRUE
  }
  
  if(existOsmoseParameter(Param, "mortality", "fishing", "rate", "byDt", "bySize", "file", index)){
    selectivity.by = "size"
    IsSeasonal = TRUE
  }
  
  if(IsSeasonal){
    fishing.file = getOsmoseParameter(Param, "mortality", "fishing", "rate", "byDt", paste0("by", selectivity.by), "file", index)
    F.rate = read.csv(fishing.file, sep=";")
    fishing.folder = normalizePath(dirname(fishing.file))
  } else {
    F.rate = getOsmoseParameter(Param, "mortality", "fishing", "rate", index)
  }
  
  # Creation of a new parameters files in order to be modified
  # contains the path of the two new configuration files.
  # The FMSY file, which will have precedence on the main configuration file
  MsyFile = list()
  MsyFile[["osmose.configuration.Fmsy"]] = file.path(input.folder, paste0("Fmsy-parameters_sp", sp, ".csv"))
  MsyFile[["osmose.configuration.main"]] = file.path(input.folder, basename(input.file))
  
  writeOsmoseParameters(MsyFile, file.path(input.folder, paste0("configFmsy_sp", sp, ".csv")))
  
  # Temporary file
  # creates the FMSY configuration file if does not exist
  if (file.exists(paste0("FmsyTemp_sp", sp, ".csv"))){
    FmsyTemp = read.csv(paste0("FmsyTemp_sp", sp, ".csv"), header=T, sep=",", dec=".", row.names=1)
  } else {
    FmsyTemp = data.frame(iSteps=1, FcollapseUpper=NA, FmsyUpper=NA)
    write.csv(FmsyTemp, file=paste0("FmsyTemp_sp", sp, ".csv"))
  }
  
  iSteps = FmsyTemp$iSteps
  FcollapseUpper = FmsyTemp$FcollapseUpper
  FmsyUpper = FmsyTemp$FmsyUpper
  
  # Creation of a vector of F that will contain Fishing mortality rates at each Step
  Fval = c(seq(Fmin, Fmin + StepF - Sub_StepF, Sub_StepF), seq(Fmin+StepF, Fmax, StepF))
  N_Fval = length(Fval)   # number of fishing values
  cat("@@@@@@@@@@@@@@@@@@@@@ Fval: ", N_Fval, "\n")
  print(Fval)
  
  N_Fcollapse = length(seq(Fmin, Fmin+StepF, Sub_StepF))   # number of time steps from Fmin to StepF
  cat("@@@@@@@@@@@@@@@@@@@@@ Collapse: ", N_Fcollapse, "\n")
  print(seq(Fmin, Fmin+StepF, Sub_StepF))
  
  N_Fmsy = length(seq(Fmin, Fmin+(2*StepF), Sub_StepF))
  cat("@@@@@@@@@@@@@@@@@@@@@ F_msy: ", N_Fmsy, "\n")
  print(seq(Fmin, Fmin+(2*StepF), Sub_StepF))
  
  # To create R output folder if is not
  if (!file.exists(file.path("output"))) {
    dir.create(file.path("output"))
  }
  if (!file.exists(file.path("output", "Fmsy_R"))) {
    dir.create(file.path("output", "Fmsy_R"))
  }
  
  # Creation of the Res file
  Res = list(B0=vector(), vect_F=vector(), Y_Fvect=list(), B_Fvect=list(),
             B_CI=list(), Y_CI=list(), MSY=vector(), Fcollapse=vector())
  
  # Do you want to restart?
  if(restart == TRUE){
    load(file.path("output", "Fmsy_R", paste0("Res", sp)))
  } else {
    iSteps = 1
    FcollapseUpper = NA
    FmsyUpper = NA
    FmsyTemp$iSteps = iSteps
    FmsyTemp$FcollapseUpper = FcollapseUpper
    FmsyTemp$FmsyUpper = FmsyUpper
    write.csv(FmsyTemp, file=paste0("FmsyTemp_sp", sp, ".csv"))
  }
  
  update_upper = TRUE
  update_collapse = TRUE
  
  cpt = iSteps
  while(!is.na(Fval[cpt])) {
    
    i = cpt
    
    # To complete matrix F once FcollapseUpper is found
    #if (!is.na(FcollapseUpper)&is.na(F[(length(Fval)+1),NumEsp])){
    if (!is.na(FcollapseUpper) & update_collapse){
      if((FcollapseUpper >= Fmin) & (FcollapseUpper < (Fmin + StepF))) {
        FcollapseUpper = Fmin + StepF
      }
      Fval  = c(Fval, seq(FcollapseUpper-StepF, FcollapseUpper, Sub_StepF))
      print("New Fval Collapse")
      print(Fval)
      update_collapse = FALSE
    }
    
    # To complete matrix F once FmsyUpper is found
    if (!is.na(FmsyUpper) & update_upper){
      if((FmsyUpper >= Fmin) & (FmsyUpper < (Fmin + StepF))) {
        FmsyUpper = Fmin + StepF
      }
      Fval = c(Fval,rev(seq(FmsyUpper-StepF,FmsyUpper+StepF,Sub_StepF)))
      print("New Fval Collapse")
      print(Fval)
      update_upper = FALSE
    }
    
    ### To modify the fishing parameter
    ParamFmsy = list()
    ParamFmsy["output.file.prefix"] = paste("Sp", sp, "F", Fval[i], sep="")
    #WriteOsmoseParameters(ParamFmsy,paste0("Fmsy-parameters_sp",sp,".csv"))
    #writing F fishing.byYear.sp0 = F
    if(IsSeasonal){
      
      Time = F.rate[ , ]
      F_file = F.rate[ , -1]
      for (l in 1:(length(b) - 1)){
        
        F_file[(b[l]+ 1 ):b[l + 1], ] = F_file[(b[l]+1):b[l+1],]/rep(apply(F_file[(b[l]+1):b[l+1],],2,sum), 1, each=dtperYear)
        
      }
      
      F_file[is.na(F_file)] = 0
      F_file = as.matrix(F_file)
      
      F_file = F_file*Fval[i]
      F_file = cbind(Time,F_file)
      colnames(F_file) = c("",substr(Names,2,length(Names)))
      write.table(F_file,file.path(fishing.folder,paste0("F-",species,"_msy.csv")),row.names=FALSE,quote=FALSE,sep=";")
      ParamFmsy[paste0("mortality.fishing.rate.byDt.by",selectivity.by,".file.sp",sp)] = file.path(fishing.folder,paste0("F-",species,"_msy.csv"))
      writeOsmoseParameters(ParamFmsy,paste0("Fmsy-parameters_sp",sp,".csv"))
      
    } else {
      
      F_rate = Fval[i]
      ParamFmsy[paste0("mortality.fishing.rate.sp", sp)] = F_rate
      writeOsmoseParameters(ParamFmsy, file.path(input.folder, paste0("Fmsy-parameters_sp", sp, ".csv")))
      
    }
    
    ### To run Osmose with R
    run_osmose(file.path(input.folder, paste0("configFmsy_sp", sp, ".csv")), output=file.path("output", paste("Sp", sp, "F", Fval[i], sep="")), ...)
    
    #####################################################
    
    ### Load output files (Biomass and Yield)
    
    out = read_osmose(file.path("output", paste("Sp", sp, "F", Fval[i], sep="")))
    biomass = get_var(out, "biomass")
    yield = get_var(out, "yield")
    
    if (Fval[i]==0) {
      
      # B0 = average biomass over all time steps and replicates
      Res$B0 = mean(biomass[, species, ])   #  biomass when no fishing is applied on the current species
      cat("B0=", Res$B0, "\n")
      
    }
    
    # biomass = (time, species, replicates)
    # biomass[, species,] = (time, replicates)
    # here, computation of time average (dim 2 is kept save)
    Res$B_Fvect[[i]] = apply(biomass[,species,], 2, mean)   # dimension N replicates
    Res$Y_Fvect[[i]] = apply(yield[,species,], 2, mean)   # dimension N replicates
    #Res$B_CI[[i]] = FilesBiomassCI[NumEsp,]
    #Res$Y_CI[[i]] = FilesYieldCI[NumEsp,]
    
    # To check if FcollapseUpper is reached
    # FcollapseUpper is the F value for which biomass is less than 10% of biomass without fishing
    if((sum((mean(biomass[, species, ])) <= (10 * Res$B0 / 100))> 0) & is.na(FcollapseUpper)){
      FcollapseUpper = Fval[i]
      FmsyTemp$FcollapseUpper = FcollapseUpper
    }	# end of if
    
    # To check if FmsyUpper is reached.
    # FmsyUpper assumed to be the value  where the next two values of yields are smaller
    if (i > 2){
      if(sum(((mean(Res$Y_Fvect[[i]])) < (mean(Res$Y_Fvect[[i-1]]))) & ((mean(Res$Y_Fvect[[i]])) < (mean(Res$Y_Fvect[[i-2]]))) & is.na(FmsyUpper)) > 0) {  # end of sunn
        FmsyUpper = Fval[i-2]
        FmsyTemp$FmsyUpper = FmsyUpper
      }	# end of if
    }
    
    ### To save values for restart
    Res$vect_F[i] = Fval[i]
    save(Res, file=file.path("output", "Fmsy_R", paste0("Res", sp)))
    FmsyTemp$iSteps[1] = i + 1
    Restart = TRUE
    write.csv(FmsyTemp, file=paste0("FmsyTemp_sp",sp,".csv"))
    
    cpt = cpt + 1
    
  }
  
  if(length(which(is.na(Res$vect_F)))>0){
    
    Res$B_Fvect[which(is.na(Res$vect_F))] = NULL
    Res$Y_Fvect[which(is.na(Res$vect_F))] = NULL
    Res$vect_F = Res$vect_F[-which(is.na(Res$vect_F))]
  }
  
  # sort the biomass, yields values by increasing fishing mortality
  Res$B_Fvect = Res$B_Fvect[sort.list(Res$vect_F)]
  Res$Y_Fvect = Res$Y_Fvect[sort.list(Res$vect_F)]
  Res$vect_F = Res$vect_F[order(Res$vect_F)]
  
  save(Res, file=file.path("output", "Fmsy_R", paste0("Res", sp)))
  
  ###################################################
  #PLOT
  
  ### Yield data
  
  #Ymean = vector()
  
  #for (k in 1:length(Res$Y_Fvect)){
  
  # Ymean[k] = mean(Res$Y_Fvect[[k]])
  
  #}
  
  Ymean = lapply(Res$Y_Fvect, mean)
  
  Yvect = Res$Y_Fvect
  
  replicat = lapply(Yvect,length)
  vectF=list()
  for(i in 1:length(replicat)){
    vectF[[i]] = rep(Res$vect_F[i], each = unlist(replicat)[i])
  }
  
  datY = cbind(unlist(vectF), unlist(Yvect))
  
  datY = as.data.frame(datY)
  names(datY) = c("F","Y")
  
  # create F vector for prediction
  Fs = seq(0, max(datY$F), by=0.01)
  
  # calculate weight for the fitting
  
  jY = jitter(datY$Y)
  
  dat.sdY = tapply(jY, INDEX=datY$F, sd)
  we0Y = rep(1/dat.sdY, table(datY[,1]))
  
  # fit the "gam" with cr spline
  
  xY = gam(Y~s(F, bs="cr"), data=datY, weigths=we0)
  
  # predict the model
  Ys = predict(xY, newdata=data.frame(F=Fs), type="response", se.fit=TRUE)
  
  Res$Fmsy = Fs[which.max(Ys$fit)]
  Res$MSY = max(Ys$fit,na.rm=TRUE)
  
  ### Biomass Data
  Bmean = lapply(Res$B_Fvect,mean)
  
  Bvect = Res$B_Fvect
  
  datB = cbind(unlist(vectF),unlist(Bvect))
  
  datB = as.data.frame(datB)
  names(datB) = c("F","B")
  
  # create F vector for prediction
  Fs = seq(0,max(datB$F),by=0.01)
  
  # calculate weight for the fitting
  
  jB = jitter(datB$B)
  
  dat.sdB = tapply(jB,INDEX=datB$F,sd)
  we0B = rep(1/dat.sdB,table(datB[,1]))
  
  # fit the "gam" with cr spline
  
  xB = gam(B~s(F,bs="cr"),data=datB,weigths=we0)
  
  # predict the model
  Bs = predict(xB,newdata=data.frame(F=Fs),type="response",se.fit=TRUE)
  
  Res$Fcollapse = Fs[which(Bs$fit<=((10*Res$B0)/100))[1]]
  
  save(Res, file=file.path("output", "Fmsy_R", paste0("Res",sp)))
  # To plot results
  
  #pdf(paste(pathInputOsmose,"output/Fmsy_R/Fmsy",paste0('_',sp,sep='',collapse=''),"1.pdf",sep=""))
  
  #	plotAreaCI(Res$vect_F,do.call(rbind,Res$Y_CI))
  #	segments(Res$Fcollapse,-10000,Res$Fcollapse,Ys$fit[which(Fs==Res$Fcollapse)],col="red",lty=2)
  #	segments(Res$Fmsy,-10000,Res$Fmsy,Res$MSY,col="blue",lty=2)
  #	mtext(paste("Fcollapse",Res$Fcollapse,sep="\n"),side=1,at=Res$Fcollapse,col="red")
  #	mtext(paste("Fmsy",Res$Fmsy,sep="\n"),side=1,at=Res$Fmsy,col="blue")
  #dev.off()
  
  pdf(file.path("output", "Fmsy_R", paste0("Fmsy_sp", sp, ".pdf")))
  
  plot(datY$F, datY$Y, pch=19, col="gray", cex=0.5, main=paste("Sp", sp, sep=""))
  lines(Fs, Ys$fit, col="red", lwd=2)
  points(Res$vect_F, Ymean, pch=19, col="blue", cex=0.7)
  if(!is.na(Res$Fcollapse)){
    segments(Res$Fcollapse, -10000, Res$Fcollapse, Ys$fit[which(Fs==Res$Fcollapse)], col="red", lty=2)
    mtext(paste("Fcollapse", Res$Fcollapse,sep="\n"), side=1, at=Res$Fcollapse, col="red")
  }
  if(!is.na(Res$Fmsy)){
    segments(Res$Fmsy, -10000, Res$Fmsy, Res$MSY, col="blue", lty=2)
    mtext(paste("Fmsy", Res$Fmsy, sep="\n"), side=1, at=Res$Fmsy, col="blue")
  }
  
  dev.off()
  
  ##To remove temporary files
  file.remove(file.path(input.folder, paste0("configFmsy_sp", sp,".csv")))
  file.remove(paste0("FmsyTemp_sp",sp,".csv"))
  file.remove(file.path(input.folder, paste0("Fmsy-parameters_sp",sp,".csv")))
  
  if(IsSeasonal) {
    file.remove(file.path(fishing.folder, paste0("F-", species, "_msy.csv")))
  }
  
  return(Res)
  
} # end of Fmsy
