

# Principal plot functions ------------------------------------------------

plot.osmose.biomass = function(x, ts = TRUE, type = 1, species = NULL, replicates = FALSE, nrep = 3,
                               ci = TRUE, start = NULL, freq = 12, horizontal = FALSE, 
                               conf = 0.95, factor = 1e-6, xlim = NULL, ylim = NULL,
                               col = "black", alpha = 0.1, lwd = 2.5, speciesNames = NULL, unitNames = NULL, ...) {
  
  opar = par(no.readonly = TRUE)
  on.exit(par(opar))
  
  if(isTRUE(ts)){
    if(type == 1){plotTsType1(x = x, species = species, replicates = replicates, nrep = nrep, ci = ci, start = start,
                              freq = freq, conf = conf, factor = factor, xlim = xlim, ylim = ylim, col = col, 
                              alpha = alpha, lwd = lwd, speciesNames = speciesNames, unitNames = unitNames, ...)}
  }
  
   
  return(invisible())
}


# Internal plot functions -------------------------------------------------

plotTsType1 = function(x, species = NULL, replicates = FALSE, nrep = 3, ci = TRUE,
                       start = NULL, freq = 12, conf=0.95, factor=1e-6,
                       xlim=NULL, ylim=NULL, col = "black", alpha = 0.1, lwd = 2.5,
                       speciesNames = NULL, unitNames = NULL, ...) {
  
  if(!is.null(species)){x = x[ , (species + 1) , , drop = FALSE]}
  start   = if(is.null(start)) as.numeric(rownames(x)[1]) else start
  times   = seq(from=start + 0.5/freq, by=1/freq, len=nrow(x))
  xlim    = if(is.null(xlim)) range(times)
  if(is.null(speciesNames)) speciesNames = toupper(colnames(x)) else speciesNames = speciesNames
  
  par(oma = c(1,1,1,1), mar = c(3,3,1,1))
  par(mfrow = getmfrow(ncol(x)))
  
  prob = 1 - conf
  
  for(sp in seq_len(ncol(x))) {
    xsp   = factor*x[, sp, ,drop = FALSE]
    ylim = if(is.null(ylim)) c(0.75, 1.25)*range(xsp)
    
    plot.new()
    plot.window(xlim=xlim, ylim=ylim)
    plotCI(x = xsp, y = times, replicates = replicates, ci = ci, nrep = nrep,
           prob = prob, col = col, alpha = alpha, lwd = lwd, ...)
    axis(1)
    axis(2, las=2)
    box()
    
    mtext(speciesNames[sp], 3, line = -1.5, adj = 0.05, cex = 0.80)
    if(is.null(unitNames)) unitNames = expression(paste("x", 10^{6}, "tonnes")) else unitNames = unitNames
    mtext(text = unitNames, side = 3, line = 0, adj = 0, cex = 0.75)
    
    ylim = NULL 
  }
  
  return(invisible())
}

plotCI = function(x, y, replicates, ci, nrep, prob, col, alpha, lwd, ...) {
  
  if(dim(x)[3] == 1){
    lines(x = y, y = apply(x, 1, mean, na.rm = TRUE), col = col)
    return(invisible())
  }
  
  x.inf = apply(x, 1, quantile, prob=prob/2)
  x.sup = apply(x, 1, quantile, prob=1-prob/2)
  x.50  = apply(x, 1, median)
  x.pol = c(y, rev(y), y[1])
  y.pol = c(x.inf, rev(x.sup), x.inf[1])
  
  if(isTRUE(replicates)) {
    polygon(x.pol, y.pol, col=makeTransparent(col=col, alpha=alpha), border=NA)
    nrep = max(min(nrep, dim(x)[3]),2)
    matplot(y, x[,,seq_len(nrep)], add=TRUE, type="l", lty = 1, 
            col=makeTransparent(col=col, alpha=(alpha + 2)/3))
  }
  lines(y, x.50, col = col, lwd = lwd, ...)
  
  return(invisible())
}

