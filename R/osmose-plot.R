
# Plot methods ------------------------------------------------------------

plot.osmose.biomass = function(x, species = NULL, start = NULL, end = NULL, initialYear = NULL,
                               ts = TRUE, type = 1, replicates = TRUE, nrep = 3,
                               ci = TRUE, freq = 12, horizontal = FALSE, 
                               conf = 0.95, factor = 1e-3, xlim = NULL, ylim = NULL,
                               col = NULL, alpha = 0.5, speciesNames = NULL, ...) {
  
  # species indexation
  if(!is.null(species)){
    if(max(species)+1 > dim(x)[2]) stop("error on species indexation, incorrect value in species parameter")
    x = x[ , (species + 1) , , drop = FALSE]}
  
  # time indexation
  if(is.null(start)) start = 1 else start = start
  if(is.null(end)) end = dim(x)[1] else end = end
  if(!start > 0 | !start < end) stop("error on time indexation, incorrect value in start parameter")
  if(!end > 0 | !end > start) stop("error on time indexation, incorrect value in end parameter")
  x = x[c(start:end), , ,drop = FALSE]
  
  # xlim 
  initialYear   = if(is.null(initialYear)) as.numeric(rownames(x)[1]) else initialYear
  times   = seq(from=initialYear + 0.5/freq, by=1/freq, len=nrow(x))
  xlim = if(is.null(xlim)) range(times)
  
  opar = par(no.readonly = TRUE)
  on.exit(par(opar))
  
  if(isTRUE(ts)){
    
    if(type == 1){plotTsType1(x = x, replicates = replicates, nrep = nrep, ci = ci,
                              initialYear = initialYear, times = times, xlim = xlim, ylim = ylim,
                              conf = conf, factor = factor, col = col, alpha = alpha,
                              speciesNames = speciesNames, ...)}
    
    if(type == 2){plotTsType2(x = x, replicates = replicates, nrep = nrep, ci = ci,
                              initialYear = initialYear, times = times, xlim = xlim, ylim = ylim,
                              conf = conf, factor = factor, col = col, alpha = alpha,
                              speciesNames = speciesNames, ...)}
    
    if(type == 3){plotTsType3(x = x, initialYear = initialYear, times = times,
                              xlim = xlim, ylim = ylim, factor = factor, 
                              col = col, speciesNames = speciesNames, ...)}  
  }
  
  if(isFALSE(ts)){
    if(type == 1){plotBarplot(x, ci = ci, horizontal = horizontal, col = col,
                              factor = factor, speciesNames = speciesNames, ...)}
    
    if(type == 2){plotBoxplot(x, horizontal = horizontal, col = col, 
                              factor = factor, speciesNames = speciesNames, ...)}
  }
  
   
  return(invisible())
}


