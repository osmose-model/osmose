plot.osmose.abundance = function(x, start=NULL, conf=0.95, factor=1e-6, replicates=FALSE,
                               freq=12, alpha=0.5, col="black", xlim=NULL, ylim=NULL, nrep=3,
                               aggregate=FALSE, ...) {
  
  opar = par(no.readonly = TRUE)
  on.exit(par(opar))
  
  par(oma=c(1,1,1,1), mar=c(3,4,1,1))
  par(mfrow=getmfrow(ncol(x)))
  
  if(isTRUE(aggregate)) {
    .plotAverageBiomass(x, col=col, ...)
    return(invisible())
  }
  
  species = colnames(x)
  start   = if(is.null(start)) as.numeric(rownames(x)[1]) else start
  
  for(sp in species) {
    .plotBiomass(x=x, sp=sp, start=start, conf=conf, factor=factor, 
                 replicates=replicates, nrep=nrep, freq=freq, col=col, alpha=alpha, 
                 xlim=xlim, ylim=xlim) 
    
  }
  
  return(invisible())
}



plot.osmose.yieldN = function(x, start=NULL, conf=0.95, factor=1e-6, replicates=FALSE, nrep=3,
                             freq=12, alpha=0.5, col="black", xlim=NULL, ylim=NULL, 
                             aggregate=FALSE, zeros=TRUE, ...) {
  
  
  if(!isTRUE(zeros)) x = .removeZeros(x)
  
  opar = par(no.readonly = TRUE)
  on.exit(par(opar))
  
  if(isTRUE(aggregate)) {
    .plotAverageYield(x, col=col, ...)
    return(invisible())
  }
  
  par(oma=c(1,1,1,1), mar=c(3,4,1,1))
  par(mfrow=getmfrow(ncol(x)))
  
  species = colnames(x)
  start   = if(is.null(start)) as.numeric(rownames(x)[1]) else start
  
  for(sp in species) {
    .plotBiomass(x=x, sp=sp, start=start, conf=conf, factor=factor, freq=freq, nrep=nrep,
                 col=col, alpha=alpha, xlim=xlim, ylim=xlim, replicates=replicates) 
    
  }
  
  return(invisible())
}


plot.osmose.meanTL = function(x, start=NULL, conf=0.95, factor=1e-6, replicates=FALSE,
                                 freq=12, alpha=0.5, col="black", xlim=NULL, ylim=NULL, nrep=3,
                                 aggregate=FALSE, ...) {
  
  opar = par(no.readonly = TRUE)
  on.exit(par(opar))
  
  par(oma=c(1,1,1,1), mar=c(3,4,1,1))
  par(mfrow=getmfrow(ncol(x)))
  
  if(isTRUE(aggregate)) {
    .plotAverageBiomass(x, col=col, ...)
    return(invisible())
  }
  
  species = colnames(x)
  start   = if(is.null(start)) as.numeric(rownames(x)[1]) else start
  
  for(sp in species) {
    .plotBiomass(x=x, sp=sp, start=start, conf=conf, factor=factor, 
                 replicates=replicates, nrep=nrep, freq=freq, col=col, alpha=alpha, 
                 xlim=xlim, ylim=xlim) 
    
  }
  
  return(invisible())
}

plot.osmose.meanTLCatch = function(x, start=NULL, conf=0.95, factor=1e-6, replicates=FALSE,
                              freq=12, alpha=0.5, col="black", xlim=NULL, ylim=NULL, nrep=3,
                              aggregate=FALSE, ...) {
  
  opar = par(no.readonly = TRUE)
  on.exit(par(opar))
  
  par(oma=c(1,1,1,1), mar=c(3,4,1,1))
  par(mfrow=getmfrow(ncol(x)))
  
  if(isTRUE(aggregate)) {
    .plotAverageBiomass(x, col=col, ...)
    return(invisible())
  }
  
  species = colnames(x)
  start   = if(is.null(start)) as.numeric(rownames(x)[1]) else start
  
  for(sp in species) {
    .plotBiomass(x=x, sp=sp, start=start, conf=conf, factor=factor, 
                 replicates=replicates, nrep=nrep, freq=freq, col=col, alpha=alpha, 
                 xlim=xlim, ylim=xlim) 
    
  }
  
  return(invisible())
}

plot.osmose.meanSize = function(x, start=NULL, conf=0.95, factor=1e-6, replicates=FALSE,
                              freq=12, alpha=0.5, col="black", xlim=NULL, ylim=NULL, nrep=3,
                              aggregate=FALSE, ...) {
  
  opar = par(no.readonly = TRUE)
  on.exit(par(opar))
  
  par(oma=c(1,1,1,1), mar=c(3,4,1,1))
  par(mfrow=getmfrow(ncol(x)))
  
  if(isTRUE(aggregate)) {
    .plotAverageBiomass(x, col=col, ...)
    return(invisible())
  }
  
  species = colnames(x)
  start   = if(is.null(start)) as.numeric(rownames(x)[1]) else start
  
  for(sp in species) {
    .plotBiomass(x=x, sp=sp, start=start, conf=conf, factor=factor, 
                 replicates=replicates, nrep=nrep, freq=freq, col=col, alpha=alpha, 
                 xlim=xlim, ylim=xlim) 
    
  }
  
  return(invisible())
}

plot.osmose.meanSizeCatch = function(x, start=NULL, conf=0.95, factor=1e-6, replicates=FALSE,
                              freq=12, alpha=0.5, col="black", xlim=NULL, ylim=NULL, nrep=3,
                              aggregate=FALSE, ...) {
  
  opar = par(no.readonly = TRUE)
  on.exit(par(opar))
  
  par(oma=c(1,1,1,1), mar=c(3,4,1,1))
  par(mfrow=getmfrow(ncol(x)))
  
  if(isTRUE(aggregate)) {
    .plotAverageBiomass(x, col=col, ...)
    return(invisible())
  }
  
  species = colnames(x)
  start   = if(is.null(start)) as.numeric(rownames(x)[1]) else start
  
  for(sp in species) {
    .plotBiomass(x=x, sp=sp, start=start, conf=conf, factor=factor, 
                 replicates=replicates, nrep=nrep, freq=freq, col=col, alpha=alpha, 
                 xlim=xlim, ylim=xlim) 
    
  }
  
  return(invisible())
}


# Other plots -------------------------------------------------------------


.plotBiomass = function(x, sp, start, conf=0.95, factor=1e-6, freq=12, replicates=FALSE, nrep=3,
                        col="black", alpha=0.5, xlim=NULL, ylim=NULL) {
  
  prob = 1 - conf
  
  x = factor*x[, sp, , drop=FALSE]
  times = seq(from=start + 0.5/freq, by=1/freq, len=nrow(x))
  xlim = if(is.null(xlim)) range(times)
  ylim = if(is.null(ylim)) c(0.75, 1.25)*range(x)
  
  plot.new()
  plot.window(xlim=xlim, ylim=ylim)
  
  .plotCI(x=x, y=times, prob=prob, col=col, alpha=alpha, replicates=replicates, nrep=nrep)
  mtext(toupper(sp), 3, line=-1.5, adj=0.05, cex=0.75)
  axis(1)
  axis(2, las=2)
  box()
  
  return(invisible())
}

.plotAverageBiomass = function(x, col="grey", factor=1e-6, border=NA, ...) {
  
  x = factor*apply(x, 2, mean, na.rm=TRUE)
  barplot(x, border=border, col=col, ...)
  return(invisible())
  i
}

.plotAverageYield = function(x, col="grey", factor=1e-6, ...) {
  
  x[x==0] = NA
  x = as.data.frame(factor*apply(x, 1:2, mean, na.rm=TRUE))
  boxplot(x, col=col, ...)
  return(invisible())
}


plot.osmose.yield = function(object, start=NULL, conf=0.95, factor=1e-6, replicates=FALSE, nrep=3,
                             freq=12, alpha=0.5, col="black", xlim=NULL, ylim=NULL, 
                             aggregate=FALSE, zeros=TRUE, ...) {
  
  
  if(!isTRUE(zeros)) object = .removeZeros(object)
  
  opar = par(no.readonly = TRUE)
  on.exit(par(opar))
  
  if(isTRUE(aggregate)) {
    .plotAverageYield(object, col=col, ...)
    return(invisible())
  }
  
  par(oma=c(1,1,1,1), mar=c(3,4,1,1))
  par(mfrow=getmfrow(ncol(object)))
  
  species = colnames(object)
  start   = if(is.null(start)) as.numeric(rownames(object)[1]) else start
  
  for(sp in species) {
    .plotBiomass(x=object, sp=sp, start=start, conf=conf, factor=factor, freq=freq, nrep=nrep,
                 col=col, alpha=alpha, xlim=xlim, ylim=xlim, replicates=replicates) 
    
  }
  
  return(invisible())
}

plot.osmose.biomass = function(object, start=NULL, conf=0.95, factor=1e-6, replicates=FALSE,
                               freq=12, alpha=0.5, col="black", xlim=NULL, ylim=NULL, nrep=3,
                               aggregate=FALSE, ...) {
  
  opar = par(no.readonly = TRUE)
  on.exit(par(opar))
  
  par(oma=c(1,1,1,1), mar=c(3,4,1,1))
  par(mfrow=getmfrow(ncol(object)))
  
  if(isTRUE(aggregate)) {
    .plotAverageBiomass(object, col=col, ...)
    return(invisible())
  }
  
  species = colnames(object)
  start   = if(is.null(start)) as.numeric(rownames(object)[1]) else start
  
  for(sp in species) {
    .plotBiomass(x=object, sp=sp, start=start, conf=conf, factor=factor, 
                 replicates=replicates, nrep=nrep, freq=freq, col=col, alpha=alpha, 
                 xlim=xlim, ylim=xlim) 
    
  }
  
  return(invisible())
}

.plotCI = function(x, y, prob, col, replicates=FALSE, nrep=3, lwd=2.5, alpha=0.1) {
  
  if(dim(x)[3]==1) {
    lines(x=y, y=apply(x, 1, mean, na.rm=TRUE), col=col)
    return(invisible(NULL))
  }
  
  x.inf = apply(x, 1, quantile, prob=prob/2)
  x.sup = apply(x, 1, quantile, prob=1-prob/2)
  x.50  = apply(x, 1, median)
  x.pol = c(y, rev(y), y[1])
  y.pol = c(x.inf, rev(x.sup), x.inf[1])
  polygon(x.pol, y.pol, col=makeTransparent(col=col, alpha=alpha), border=NA)
  if(isTRUE(replicates)) {
    nrep = max(min(nrep, dim(x)[3]),2)
    matplot(y, x[,,seq_len(nrep)], add=TRUE, type="l", lty=1, 
            col=makeTransparent(col=col, alpha=(alpha + 2)/3))
  }
  lines(y, x.50, col=col, lwd=lwd)
  return(invisible(NULL))
  
}


