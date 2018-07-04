

# Principal plot functions ------------------------------------------------

plot.osmose.biomass = function(x, species = NULL, start = NULL, end = NULL, initialYear = NULL,
                               ts = TRUE, type = 1, replicates = TRUE, nrep = 3,
                               ci = TRUE, freq = 12, horizontal = FALSE, 
                               conf = 0.95, factor = 1e-3, xlim = NULL, ylim = NULL,
                               col = "black", alpha = 0.5, speciesNames = NULL, ...) {
  
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
  
  opar = par(no.readonly = TRUE)
  on.exit(par(opar))
  
  if(isTRUE(ts)){
    
    if(type == 1){plotTsType1(x = x, replicates = replicates, nrep = nrep, ci = ci, initialYear = initialYear,
                              freq = freq, conf = conf, factor = factor, xlim = xlim, ylim = ylim, col = col, 
                              alpha = alpha, speciesNames = speciesNames, ...)}
    
    if(type == 2){plotTsType2(x = x, replicates = replicates, nrep = nrep, ci = ci, initialYear = initialYear,
                              freq = freq, conf = conf, factor = factor, xlim = xlim, ylim = ylim, col = NULL, 
                              alpha = alpha, speciesNames = speciesNames, ...)}
    
    if(type == 3){plotTsType3(x = x, initialYear = initialYear, freq = freq, factor = factor, 
                              xlim = xlim, ylim = ylim, col = col, speciesNames = speciesNames, ...)}  
  }
  
  if(isFALSE(ts)){
    if(type == 1){plotBarplot(x, ci = ci, horizontal = horizontal, col = col,
                              factor = factor, speciesNames = speciesNames, ...)}
    
    if(type == 2){plotBoxplot(x, horizontal = horizontal, col = col, 
                              factor = factor, speciesNames = speciesNames, ...)}
  }
  
   
  return(invisible())
}


# Internal plot functions -------------------------------------------------

plotTsType1 = function(x, replicates = TRUE, nrep = 3, ci = TRUE,
                       initialYear = NULL, freq = 12, conf=0.95, factor=1e-3,
                       xlim=NULL, ylim=NULL, col = "black", alpha = 0.5,
                       speciesNames = NULL, ...) {
  
  initialYear   = if(is.null(initialYear)) as.numeric(rownames(x)[1]) else initialYear
  times   = seq(from=initialYear + 0.5/freq, by=1/freq, len=nrow(x))
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
           prob = prob, col = col, alpha = alpha, ...)
    axis(1)
    axis(2, las=2)
    box()
    
    mtext(speciesNames[sp], 3, line = -1.5, adj = 0.05, cex = 0.80)
    mtext(text = expression(paste("x", 10^{3}, "tonnes")), side = 3, line = 0, adj = 0, cex = 0.75)
    
    ylim = NULL 
  }
  
  return(invisible())
}

plotCI = function(x, y, replicates, ci, nrep, prob, col, alpha = 0.1, border = NA, ...) {
  
  if(dim(x)[3] == 1){
    lines(x = y, y = apply(x, 1, mean, na.rm = TRUE), col = col, ...)
    return(invisible())
  }
  
  x.inf = apply(x, 1, quantile, prob=prob/2)
  x.sup = apply(x, 1, quantile, prob=1-prob/2)
  x.50  = apply(x, 1, median)
  x.pol = c(y, rev(y), y[1])
  y.pol = c(x.inf, rev(x.sup), x.inf[1])
  
  if(isTRUE(replicates)) {
    polygon(x.pol, y.pol, col=makeTransparent(col=col, alpha=alpha), border = border, ...)
    nrep = max(min(nrep, dim(x)[3]),2)
    matplot(y, x[,,seq_len(nrep)], add=TRUE, type="l", lty = 1, 
            col=makeTransparent(col=col, alpha=(alpha + 2)/3))
  }
  lines(y, x.50, col = col, ...)
  
  return(invisible())
}

plotTsType2 = function(x, replicates = TRUE, nrep = 3, ci = TRUE,
                       initialYear = NULL, freq = 12, conf=0.95, factor=1e-3,
                       xlim=NULL, ylim=NULL, col = NULL, alpha = 0.5, 
                       speciesNames = NULL, ...) {
  
  initialYear   = if(is.null(initialYear)) as.numeric(rownames(x)[1]) else initialYear
  times   = seq(from=initialYear + 0.5/freq, by=1/freq, len=nrow(x))
  xlim    = if(is.null(xlim)) range(times)
  ylim    = if(is.null(ylim)) c(0.75, 1.25)*c(min(apply(x, 2, min)), max(apply(x, 2, max)))
  if(is.null(speciesNames)) speciesNames = toupper(colnames(x)) else speciesNames = speciesNames
  if(is.null(col)) col = rainbow(n = ncol(x)) else col = col
  par(oma = c(1,1,1,1), mar = c(2,2,1,0.5))
  
  prob = 1 - conf
  plot.new()
  plot.window(xlim=xlim, ylim=ylim*factor)
  
  for(sp in seq_len(ncol(x))) {
    xsp   = factor*x[, sp, ,drop = FALSE]
    
    plotCI(x = xsp, y = times, replicates = replicates, ci = ci, nrep = nrep,
           prob = prob, col = col[sp], alpha = alpha, ...)
  }
  axis(1)
  axis(2, las=2)
  box()
  
  mtext(text = expression(paste("x", 10^{3}, "tonnes")), side = 3, line = 0, adj = 0, cex = 0.9)
  legend("topleft", legend = speciesNames, col = col, bty = "n", cex = 0.7, lty = 1)
  
  return(invisible())
}

plotTsType3 = function(x, initialYear = NULL, freq = 12, factor=1e-3,
                       xlim=NULL, ylim=NULL, col = NULL, 
                       speciesNames = NULL, ...) {
  
  if(length(dim(x)) == 3){x = apply(x, c(1,2), mean, na.rm = TRUE)}
  initialYear   = if(is.null(initialYear)) as.numeric(rownames(x)[1]) else initialYear
  
  x = factor*x
  x = x[, order(apply(x, 2, sum, na.rm = TRUE), decreasing = TRUE)]
  
  xsp0 = rep(0, times = nrow(x))
  dataSpecies = NULL
  for (sp in seq_len(ncol(x))) {
    xsp  = xsp0 + x[, sp]
    dataSpecies = cbind(dataSpecies, xsp)
    xsp0 = xsp
  }
  colnames(dataSpecies) = colnames(x)
  
  times   = seq(from=initialYear + 0.5/freq, by=1/freq, len=nrow(x))
  xlim    = if(is.null(xlim)) range(times)
  ylim    = if(is.null(ylim)) c(0.75, 1.25)*range(dataSpecies[, dim(dataSpecies)[2]])
  
  par(oma = c(1,1,1,1), mar = c(2,2,1,0.5))
  plot.new()
  plot.window(xlim=xlim, ylim=ylim)
  
  dataSpecies = dataSpecies[, order(apply(dataSpecies, 2, sum, na.rm = TRUE), decreasing = TRUE)]
  if(is.null(speciesNames)) speciesNames = toupper(colnames(dataSpecies)) else speciesNames = speciesNames
  if(is.null(col)) col = rainbow(n = ncol(dataSpecies)) else col = col
  
  for(sp in seq_len(ncol(x))) {
    
    #lines(times, dataSpecies[, sp])
    
    x.pol = c(times, rev(times))
    y.pol = c(dataSpecies[, sp], rep(0, times = nrow(x)))
    polygon(x.pol, y.pol, border=NA, col = col[sp])
    
  }
  
  axis(1)
  axis(2, las=2)
  box()
  mtext(text = expression(paste("x", 10^{3}, "tonnes")), side = 3, line = 0, adj = 0, cex = 0.9)
  legend("topleft", legend = speciesNames, col = col, bty = "n", cex = 0.7, lty = 1)
  
  return(invisible())
}

plotBarplot = function(x, ci = FALSE, horizontal = FALSE, col = "gray", 
                       factor = 1e-3, speciesNames = NULL, border = NA, ...) {
  
  if(is.null(speciesNames)) speciesNames = toupper(colnames(x)) else speciesNames = speciesNames
  
  if(isFALSE(ci)){
    x = apply(x, 2, mean, na.rm = TRUE) #mean over the replicates
    x = x * factor
    if(isFALSE(horizontal)){
      par(oma = c(1,1,1,1), mar = c(2.5,2,1,0.3), las = 1)
      ylim = c(0, 1.2*max(x)) 
      xlim = NULL
    } else {
      par(oma = c(1,1,1,1), mar = c(2,5.5,1,0.3), las = 1)
      xlim = c(0, 1.2*max(x)) 
      ylim = NULL
    } 
    
    barplot(x, horiz = horizontal, names.arg = speciesNames, col = col,
            ylim = ylim, xlim = xlim, cex.names = 0.8, border = border, ...)
    
  } else {
    barplotCI(x, horizontal = horizontal, col = col, factor = factor,
              speciesNames = speciesNames, border = border, ...)
  }
  
  box()
  mtext(text = expression(paste("x", 10^{3}, "tonnes")), side = 3, line = 0, adj = 0, cex = 0.9)
  
  return(invisible())
}  

barplotCI = function(x, horizontal, col, factor, speciesNames, border, ...) {
  
  y.mean = apply(x*factor, 2, mean, na.rm = TRUE)
  y.sd   = apply(x*factor, 2, sd, na.rm = TRUE)
  if(isFALSE(horizontal)){
    ylim = c(0, 1.2*max(y.mean)) 
    xlim = NULL
    par(oma = c(1,1,1,1), mar = c(2.5,2,1,0.3), las = 1)
  } else {
    xlim = c(0, 1.2*max(y.mean)) 
    ylim = NULL
    par(oma = c(1,1,1,1), mar = c(2,5.5,1,0.3), las = 1)
  } 
  barx = barplot(y.mean, horiz = horizontal, names.arg = speciesNames, col = col, ylim = ylim, xlim = xlim, cex.names = 0.8, border = border, ...)
  
  if(isFALSE(horizontal)){
    arrows(barx, y.mean + 1.96*y.sd/10, barx, y.mean - 1.96*y.sd/10, angle = 90, code = 3, length = 0.10, ...)
  } else {
    arrows(y.mean - 1.96*y.sd/10, barx, y.mean + 1.96*y.sd/10, barx, angle = 90, code = 3, length = 0.10, ...)
  }
  
  
}

plotBoxplot = function(x, horizontal = FALSE, col = FALSE, 
                       factor = 1e-3, speciesNames = NULL, ...) {
  
  if(is.null(speciesNames)) speciesNames = toupper(colnames(x)) else speciesNames = speciesNames
  
  if(isFALSE(horizontal)){par(oma = c(1,1,1,1), mar = c(2.5,2,1,0.3), las = 1)
  } else {par(oma = c(1,1,1,1), mar = c(2,5.5,1,0.3), las = 1)}
  
  x = apply(x*factor, c(1,2), mean, na.rm = TRUE) #mean over the replicates
  
  boxplot(x, horizontal = horizontal, names = speciesNames, col = col, ...)
  mtext(text = expression(paste("x", 10^{3}, "tonnes")), side = 3, line = 0, adj = 0, cex = 0.9)
  
  return(invisible())
}
