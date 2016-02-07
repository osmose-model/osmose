
# Auxiliar ----------------------------------------------------------------

.getFishingFrequency = function(sp, fishing, ndt) {
  periods = fishing$periodsperyear[[sp]]
  if(is.null(periods)) periods = 1L
  if(periods%%1!=0) stop(sprintf("periodsPerYear.%s must be an integer.", sp))
  freq = ndt/periods
  if(freq%%1!=0) stop(sprintf("simulation.time.ndtPerYear must be a multiple of periodsPerYear.%s.", sp))
  return(freq)
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
