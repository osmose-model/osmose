

#' Plots biomass.
#'
#' @param x osmose.output.object
#' @param start First time-step to plot
#' @param conf  Confidence interval
#' @param factor Multiplication factor
#' @param replicates Draws the biomass for each replicate
#' @param freq Output frequency (used to convert from time-step to time)
#' @param alpha Transparency for confidence interval
#' @param col  Line color
#' @param xlim  Xaxis limit
#' @param ylim  Yaxis limit
#' @param nrep  Maximum number of replicate to draw. If NULL, all the replicates
#' are drawn. Only used if replicates=TRUE
#' @param time.mean If FALSE, the mean biomass is drawn for each specie as a barplot.
#' @param nmax Maximum number of species to draw (only used if time.mean=TRUE)
#' @param species Name of the species to draw (only if time.mean=FALSE)
#' @param ... 
#'
#' @export
#' @method plot osmose.output.biomass
plot.osmose.output.biomass = function(x, start=NULL, conf=0.95, factor=1e-6, replicates=FALSE,
                               freq=12, alpha=0.5, col="black", xlim=NULL, ylim=NULL, nrep=NULL,
                               time.mean=FALSE, nmax=NULL, species=NULL, ...) {

  opar = par(no.readonly = TRUE)
  on.exit(par(opar))
  
  if(isTRUE(time.mean)) {
    .plotAverageBiomass(x, col=col, nmax=nmax, factor=factor, ylab="Biomass", title="Mean biomass", ...)
    return(invisible())
  }
  
  if(!species %in% colnames(x) | is.null(species))
  {
    stop("You should proper a species name")
  }
  
  start   = if(is.null(start)) as.numeric(rownames(x)[1]) else start

  .plotBiomass(x=x, sp=species, start=start, conf=conf, factor=factor, 
               replicates=replicates, nrep=nrep, freq=freq, col=col, alpha=alpha, 
               xlim=xlim, ylim=xlim) 
  
  title(xlab="Time (years)", ylab="Biomass", main=species)
  
  return(invisible())
}

#' @export
#' @method plot osmose.output.abundance
plot.osmose.output.abundance = function(x, start=NULL, conf=0.95, factor=1e-6, replicates=FALSE,
                               freq=12, alpha=0.5, col="black", xlim=NULL, ylim=NULL, nrep=NULL,
                               time.mean=FALSE, species=NULL, nmax=NULL, ...) {
  
  opar = par(no.readonly = TRUE)
  on.exit(par(opar))
  
  if(isTRUE(time.mean)) {
    .plotAverageBiomass(x, col=col, nmax=nmax, factor=factor, ylab="Abundance", title="Mean abundance", ...)
    return(invisible())
  }
  
  if(!species %in% colnames(x) | is.null(species))
  {
    stop("You should proper a species name")
  }
  
  start   = if(is.null(start)) as.numeric(rownames(x)[1]) else start

  .plotBiomass(x=x, sp=species, start=start, conf=conf, factor=factor, 
               replicates=replicates, nrep=nrep, freq=freq, col=col, alpha=alpha, 
               xlim=xlim, ylim=xlim) 
  
  title(xlab="Time (years)", ylab="Abundance", main=species)
  
  return(invisible())
}

#' @export
#' @method plot osmose.output.yield
plot.osmose.output.yield = function(x, start=NULL, conf=0.95, factor=1e-6, replicates=FALSE, nrep=3,
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

#' @export
#' @method plot osmose.output.yieldN
plot.osmose.output.yieldN = function(x, start=NULL, conf=0.95, factor=1e-6, replicates=FALSE, nrep=3,
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

#' @export
#' @method plot osmose.output.meanTL
plot.osmose.output.meanTL = function(x, start=NULL, conf=0.95, factor=1e-6, replicates=FALSE,
                                 freq=12, alpha=0.5, col="black", xlim=NULL, ylim=NULL, nrep=3,
                                 time.mean=FALSE, nmax=NULL, species=NULL, ...) {
  
  opar = par(no.readonly = TRUE)
  on.exit(par(opar))
  
  if(isTRUE(time.mean)) {
    .plotAverageBiomass(x, col=col, factor=factor, nmax=nmax, ylab="TL", title="Mean trophic level", ...)
    return(invisible())
  }
  
  if(!species %in% colnames(x) | is.null(species))
  {
    stop("You should proper a species name")
  }
  
  start   = if(is.null(start)) as.numeric(rownames(x)[1]) else start
  
  .plotBiomass(x=x, sp=species, start=start, conf=conf, factor=factor, 
               replicates=replicates, nrep=nrep, freq=freq, col=col, alpha=alpha, 
               xlim=xlim, ylim=xlim) 
    
  title(xlab="Time (years)", ylab="Trophic level", main=species)
  
  return(invisible())
}

#' @export
#' @method plot osmose.output.meanTLCatch
plot.osmose.output.meanTLCatch = function(x, start=NULL, conf=0.95, factor=1e-6, replicates=FALSE,
                              freq=12, alpha=0.5, col="black", xlim=NULL, ylim=NULL, nrep=3,
                              time.mean=FALSE, nmax=NULL, species=NULL, ...) {
  
  opar = par(no.readonly = TRUE)
  on.exit(par(opar))
  
  if(isTRUE(time.mean)) {
    .plotAverageBiomass(x, col=col, factor=factor, nmax=nmax, ylab="meanTLCatch", title="meanTLCatch", ...)
    return(invisible())
  }
  
  if(!species %in% colnames(x) | is.null(species))
  {
    stop("You should proper a species name")
  }
  
  start   = if(is.null(start)) as.numeric(rownames(x)[1]) else start
  
  .plotBiomass(x=x, sp=species, start=start, conf=conf, factor=factor, 
               replicates=replicates, nrep=nrep, freq=freq, col=col, alpha=alpha, 
               xlim=xlim, ylim=xlim) 
  
  title(xlab="Time (years)", ylab="Mean Trophic Level Catch", main=species)
  
  return(invisible())
}

#' @export
#' @method plot osmose.output.meanSize
plot.osmose.output.meanSize = function(x, start=NULL, conf=0.95, factor=1e-6, replicates=FALSE,
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

#' @export
#' @method plot osmose.output.meanSizeCatch
plot.osmose.output.meanSizeCatch = function(x, start=NULL, conf=0.95, factor=1e-6, replicates=FALSE,
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



# Auxiliar plot functions -------------------------------------------------

.plotBiomass = function(x, sp, start, conf=0.95, factor=1e-6, freq=12, replicates=FALSE, nrep=3,
                        col="black", alpha=0.5, xlim=NULL, ylim=NULL) {
  
  prob = 1 - conf
  
  x = factor*x[, sp, , drop=FALSE]
  times = seq(from=start + 0.5/freq, by=1/freq, len=nrow(x))
  xlim = if(is.null(xlim)) range(times)
  #ylim = if(is.null(ylim)) c(0.75, 1.25)*range(x)
  ylim = if(is.null(ylim)) c(1, 1)*range(x)
  
  plot.new()
  plot.window(xlim=xlim, ylim=ylim)

  .plotCI(x=x, y=times, prob=prob, col=col, alpha=alpha, replicates=replicates, nrep=nrep)

  #mtext(toupper(sp), 3, line=-1.5, adj=0.05, cex=0.75)
  axis(1)
  axis(2, las=2)
  box()
  
  return(invisible())
}

.plotAverageBiomass = function(x, col="grey", factor=1e-6, border=NA, nmax=NULL, ylab=NULL, title=NULL, ...) {
  
  if(factor != 1) {
    ylab = paste(factor, " ", ylab, sep="")
  }
  
  # Computes the mean over time (dimension 1) and eventually replicates
  x = factor*apply(x, 2, mean, na.rm=TRUE)
  x = sort(x, decreasing=TRUE)
  if(!is.null(nmax)) x = x[1:nmax]
  osmose.barplot(x, ylab=ylab, main=title, ...)
  return(invisible())
}

.plotAverageYield = function(x, col="grey", factor=1e-6, ...) {
  
  x[x==0] = NA
  x = as.data.frame(factor*apply(x, 1:2, mean, na.rm=TRUE))
  boxplot(x, col=col, ...)
  return(invisible())
}

.plotCI = function(x, y, prob, col, replicates=FALSE, nrep=NULL, lwd=2.5, alpha=0.1) {
  

  if(dim(x)[3]==1) {
    # If there is no replicates, x has dims (ntime, 1, 1)
    # so the average is identical to data extraction
    lines(x=y, y=apply(x, 1, mean, na.rm=TRUE), col=col)
    return(invisible(NULL))
  }

  if(is.null(nrep)) nrep = dim(x)[3]==1
  
  x.inf = apply(x, 1, quantile, prob=prob/2)
  x.sup = apply(x, 1, quantile, prob=1-prob/2)
  x.50  = apply(x, 1, median)
  x.pol = c(y, rev(y), y[1])
  y.pol = c(x.inf, rev(x.sup), x.inf[1])
  polygon(x.pol, y.pol, col=makeTransparent(col=col, alpha=alpha), border=NA)
  if(isTRUE(replicates)) {
    nrep = max(min(nrep, dim(x)[3]), 2)
    matplot(y, x[,,seq_len(nrep)], add=TRUE, type="l", lty=1, 
            col=makeTransparent(col=col, alpha=(alpha + 2)/3))
  }
  
  lines(y, x.50, col=col, lwd=lwd)
  return(invisible(NULL))
  
}
