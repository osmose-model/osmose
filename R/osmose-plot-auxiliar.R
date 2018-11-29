
# Internal plot functions -------------------------------------------------

osmosePlots2D = function(x, species, start, end, initialYear, ts, type,
                         replicates, nrep, ci, freq, horizontal, conf,
                         factor, xlim, ylim, col, alpha, speciesNames, axes, ...) {
  
  # species indexation
  if(!is.null(species)) {
    if(max(species)+1 > dim(x)[2]) stop("error on species indexation, incorrect value in the parameter called species")
    x = x[ , (species + 1) , , drop = FALSE]
    }
  
  # time indexation
  if(is.null(start)) start = 1 else start = start
  if(is.null(end)) end = dim(x)[1] else end = end
  if(!(start > 0) | !(start < end)) stop("error on time indexation, incorrect value the parameter called start")
  if(!(end > 0) | !(end > start)) stop("error on time indexation, incorrect value in the parameter called")
  x = x[c(start:end), , ,drop = FALSE]
  
  # xlim 
  initialYear   = if(is.null(initialYear)) as.numeric(rownames(x)[1]) else initialYear
  times   = seq(from=initialYear + 0.5/freq, by=1/freq, len=nrow(x))
  xlim = if(is.null(xlim)) range(times)
  
  # opar = par(no.readonly = TRUE)
  # on.exit(par(opar))
  
  if(!(type %in% c(1:4))){
    warning("The type argument selected is not correct. The value by default is used (type = 1)")
    type = 1
  } 
  
  if(isTRUE(ts)){
    
    if(type == 1) { plot2DTsType1(x = x, replicates = replicates, nrep = nrep, ci = ci,
                                times = times, xlim = xlim, ylim = ylim,
                                conf = conf, factor = factor, col = col, alpha = alpha,
                                speciesNames = speciesNames, axes = axes, ...) }
    
    if(type == 2) { plot2DTsType2(x = x, replicates = replicates, nrep = nrep, ci = ci,
                                times = times, xlim = xlim, ylim = ylim,
                                conf = conf, factor = factor, col = col, alpha = alpha,
                                speciesNames = speciesNames, axes = axes, ...) }
    
    if(type == 3) { plot2DTsType3(x = x, times = times,
                                xlim = xlim, ylim = ylim, factor = factor, 
                                col = col, speciesNames = speciesNames, axes = axes, ...) }
    
    if(type == 4) { plot2DTsType4(x = x, times = times,
                                xlim = xlim, ylim = ylim, factor = factor,
                                col = col, speciesNames = speciesNames, axes = axes, ...) } 
      
  } else {
    
    if(type == 1) { plot2DType1(x, ci = ci, horizontal = horizontal, col = col,
                              factor = factor, speciesNames = speciesNames, axes = axes, ...) }
    
    if(type == 2) { plot2DType2(x, horizontal = horizontal, col = col, 
                              factor = factor, speciesNames = speciesNames, axes = axes, ...) }
    
    if(type == 3) { plot2DType3(x, horizontal = horizontal, col = col, 
                              factor = factor, speciesNames = speciesNames, axes = axes, ...) }
  }
  
  return(invisible())
}

# Plot types --------------------------------------------------------------

plot2DTsType1 = function(x, replicates = TRUE, nrep = 3, ci = TRUE,
                         times, xlim, ylim = NULL,
                         conf = 0.95, factor = 1e-3, col = NULL, alpha = 0.5,
                         speciesNames = NULL, lty = NULL, cex = 0.8, border = NA, axes = TRUE, ...) {
  
  if(is.null(speciesNames)) speciesNames = toupper(colnames(x)) else speciesNames = speciesNames
  
  if(ncol(x)!=1) {
    par(oma = c(1,1,1,1), mar = c(3,3,1,1))
    par(mfrow = getmfrow(ncol(x)))
  }
  
  if(is.null(col)) col = .recycleArguments("black",dim(x)[2]) else col = .recycleArguments(col,dim(x)[2])
  if(is.null(lty)) lty = .recycleArguments(1, dim(x)[2]) else lty = .recycleArguments(lty, dim(x)[2])
  
  prob = 1 - conf
  
  for(sp in seq_len(ncol(x))) {
    xsp   = factor*x[, sp, ,drop = FALSE]
    if(is.null(ylim)) ylim = c(0.75, 1.25)*range(xsp) #pending: ylim flexible for the users 
    
    plot.new()
    plot.window(xlim=xlim, ylim=ylim)
    plotCI(x = xsp, y = times, replicates = replicates, ci = ci, nrep = nrep,
           prob = prob, col = col[sp], alpha = alpha, lty = lty[sp], border = border, ...)
    
    if(isTRUE(axes)){
      axis(1, ...)
      axis(2, las=2, ...)
      box()
    }
    
    mtext(speciesNames[sp], 3, line = -1.5, adj = 0.05, cex = cex)
    legendFactor = -(log10(factor))
    legendFactor = bquote("x" ~ 10^.(legendFactor) ~ "tonnes")
    mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)
    
    ylim = NULL 
  }
  
  return(invisible())
}

plotCI = function(x, y, replicates, ci, nrep, prob, col, alpha = 0.1, border, lty, ...) {
  
  if(dim(x)[3] == 1){
    lines(x = y, y = apply(x, 1, mean, na.rm = TRUE), col = col, lty = lty, ...)
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
    # matplot(y, x[,,seq_len(nrep)], add=TRUE, type="l", lty = lty, 
    #         col=makeTransparent(col=col, alpha=(alpha + 2)/3))
  }
  lines(y, x.50, col = col, lty = lty, ...)
  
  return(invisible())
}

plot2DTsType2 = function(x, replicates = TRUE, nrep = 3, ci = TRUE,
                         times, xlim, ylim=NULL, 
                         conf=0.95, factor=1e-3, col = NULL, alpha = 0.5, 
                         speciesNames = NULL, lty = NULL, cex = 0.8, legend = TRUE, border = NA, axes = TRUE, ...) {
  
  if(is.null(speciesNames)) speciesNames = toupper(colnames(x)) else speciesNames = speciesNames
  if(is.null(ylim)){
    ylim    = c(0.75, 1.25)*c(min(apply(x, 2, min))*factor, max(apply(x, 2, max))*factor)
  } else {
    ylim = ylim
  }
  
  # par(oma = c(1,1,1,1), mar = c(2,2,1,0.5))
  if(is.null(col)) col = .recycleArguments(rainbow(dim(x)[2]),dim(x)[2]) else col = .recycleArguments(col,dim(x)[2])
  if(is.null(lty)) lty = .recycleArguments(1, dim(x)[2]) else lty = .recycleArguments(lty, dim(x)[2])
  
  prob = 1 - conf
  plot.new()
  plot.window(xlim=xlim, ylim=ylim)
  
  for(sp in seq_len(ncol(x))) {
    xsp   = factor*x[, sp, ,drop = FALSE]
    
    plotCI(x = xsp, y = times, replicates = replicates, ci = ci, nrep = nrep,
           prob = prob, col = col[sp], alpha = alpha, lty = lty[sp], border = border, ...)
  }
  
  if(isTRUE(axes)){
    axis(1, ...)
    axis(2, las=2, ...)
    box()
  }
  
  legendFactor = -(log10(factor))
  legendFactor = bquote("x" ~ 10^.(legendFactor) ~ "tonnes")
  mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)
  
  if(isTRUE(legend)){
    legend("topleft", legend = speciesNames, col = col, bty = "n", cex = cex, lty = lty)
  }
  
  return(invisible())
}

plot2DTsType3 = function(x, times, xlim, ylim=NULL, factor=1e-3,
                         col = NULL, speciesNames = NULL, legend = TRUE, axes = TRUE, ...) {
  
  if(length(dim(x)) == 3){x = apply(x, c(1,2), mean, na.rm = TRUE)}
  
  x = factor*x
  orderData = order(apply(x, 2, sum, na.rm = TRUE), decreasing = FALSE)
  x = x[, orderData]
  
  dataSpecies = NULL
  for (sp in seq_len(ncol(x))) {
    xsp  = apply(as.data.frame(x[, c(sp:(dim(x))[2])]), 1, sum, na.rm = TRUE)
    dataSpecies = cbind(dataSpecies, xsp)
  }
  colnames(dataSpecies) = colnames(x)
  
  if(is.null(ylim)){ylim = c(0.75, 1.25)*range(dataSpecies[, 1])} else {ylim = ylim}
  if(is.null(speciesNames)) speciesNames = toupper(colnames(dataSpecies)) else speciesNames = speciesNames[orderData]
  if(is.null(col)) {
    col = .recycleArguments(rainbow(ncol(dataSpecies)), ncol(dataSpecies))
  } else {
    col = .recycleArguments(col, ncol(dataSpecies))
    col = col[orderData]
  } 
  #if(is.null(col)) col = rainbow(n = ncol(dataSpecies)) else col = col
  
  par(oma = c(1,1,1,1), mar = c(2,2,1,0.5), xaxs = "i", yaxs = "i")
  plot.new()
  plot.window(xlim=xlim, ylim=ylim)
  
  for(sp in seq_len(ncol(x))) {
    #lines(times, dataSpecies[, sp])
    x.pol = c(times, rev(times))
    y.pol = c(dataSpecies[, sp], rep(0, times = nrow(x)))
    polygon(x.pol, y.pol, border=NA, col = col[sp], ...)
  }
  
  if(isTRUE(axes)) {
    axis(1, ...)
    axis(2, las=2, ...)
    box()
  }
  
  
  legendFactor = -(log10(factor))
  legendFactor = bquote("x" ~ 10^.(legendFactor) ~ "tonnes")
  mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = 0.9)
  
  if(isTRUE(legend)){
    legend("topleft", legend = speciesNames, col = col, bty = "n", cex = 0.7, lty = 1)
  }
  
  return(invisible())
}

#plot for only one species using bars
plot2DTsType4 = function(x, times, xlim, ylim = NULL,
                         factor = 1e-3, col = NULL, 
                         speciesNames = NULL, lty = NULL, cex = 0.8, legend = TRUE, axes = TRUE, ...) {
  
  if(is.null(speciesNames)) speciesNames = toupper(colnames(x)) else speciesNames = speciesNames
  if(dim(x)[2]>1) stop("Plot ts = TRUE and type = 4 is only for one species")
  if(is.null(col)) col = .recycleArguments("black",dim(x)[2]) else col = .recycleArguments(col,dim(x)[2])
  if(is.null(lty)) lty = .recycleArguments(1, dim(x)[2]) else lty = .recycleArguments(lty, dim(x)[2])
 
  x = apply(x, 1, mean, na.rm = TRUE)*factor
  if(is.null(ylim)){ylim =  c(0, 1.25)*range(x)} else {ylim = ylim}
  
  plot(x = times, y = x, col = col, lty = lty, type = "h", axes = FALSE, xlab = "", ylab = "",
       xlim = xlim, ylim = ylim, ...)
  
  if(isTRUE(axes)){
    axis(1, ...)
    axis(2, las=2, ...)
    box()
  }
  
  legendFactor = -(log10(factor))
  legendFactor = bquote("x" ~ 10^.(legendFactor) ~ "tonnes")
  mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)
  
  if(isTRUE(legend)){
    legend("topleft", legend = speciesNames, col = col, bty = "n", cex = cex, lty = lty)
  }
  
  return(invisible())
}


plot2DType1 = function(x, ci = TRUE, horizontal = FALSE, col = NULL,
                       factor = 1e-3, speciesNames = NULL, border = NA,
                       cex.names = 0.8, cex = 0.9, axes = TRUE, ...) {
  
  if(is.null(speciesNames)) speciesNames = toupper(colnames(x)) else speciesNames = speciesNames
  if(is.null(col)) col="gray"
  
  if(!isTRUE(ci)){
    x = apply(x, 2, mean, na.rm = TRUE) #mean over the replicates
    x = x * factor
    if(!isTRUE(horizontal)){
      mar = c(2.5,2,1,0.3)
      ylim = c(0, 1.2*max(x))
      xlim = NULL
    } else {
      mar = c(2,5.5,1,0.3)
      xlim = c(0, 1.2*max(x))
      ylim = NULL
    } 
    
    par(oma = c(1,1,1,1), mar = mar, las = 1)
    barplot(x, horiz = horizontal, names.arg = speciesNames, col = col,
            ylim = ylim, xlim = xlim, cex.names = cex.names, border = border, axes = axes, ...)
    
  } else {
    barplotCI(x, horizontal = horizontal, speciesNames = speciesNames, col = col,
              factor = factor, border = border, cex.names = cex.names, axes = axes, ...)
  }
  
  box()
  legendFactor = -(log10(factor))
  legendFactor = bquote("x" ~ 10^.(legendFactor) ~ "tonnes")
  mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)
  
  return(invisible())
  
}  

barplotCI = function(x, horizontal, speciesNames, col, factor, border, cex.names = 0.8,
                     angle = 90, code = 3, length = 0.10, axes, ...) {
  
  y.mean = apply(x*factor, 2, mean, na.rm = TRUE)
  y.sd   = apply(x*factor, 2, sd, na.rm = TRUE)
  
  if(!isTRUE(horizontal)){
    mar = c(2.5,2,1,0.3)
    ylim = c(0, 1.2*(max(y.mean)+1.96*max(y.sd)/10))
    xlim = NULL
  } else {
    mar = c(2,5.5,1,0.3)
    xlim = c(0, 1.2*(max(y.mean)+1.96*max(y.sd)/10))
    ylim = NULL
  }
  
  par(oma = c(1,1,1,1), mar = mar, las = 1)
  barx = barplot(y.mean, horiz = horizontal, names.arg = speciesNames, col = col,
                 ylim = ylim, xlim = xlim, cex.names = cex.names, border = border, axes = axes, ...)
  
  if(!isTRUE(horizontal)){
    arrows(barx, y.mean + 1.96*y.sd/10, barx, y.mean - 1.96*y.sd/10, angle = angle, code = code, length = length, ...)
  } else {
    arrows(y.mean - 1.96*y.sd/10, barx, y.mean + 1.96*y.sd/10, barx, angle = angle, code = code, length = length, ...)
  }
  
  
}

#boxplot with mean over the replicates
plot2DType2 = function(x, horizontal = FALSE, col = NULL, 
                       factor = 1e-3, speciesNames = NULL, cex = 0.8, axes = TRUE, ...) {
  
  if(is.null(speciesNames)) speciesNames = toupper(colnames(x)) else speciesNames = speciesNames
  if(is.null(col)) col="gray"
  
  if(!isTRUE(horizontal)){par(oma = c(1,1,1,1), mar = c(2.5,2,1,0.3), las = 1, ...)
  } else {par(oma = c(1,1,1,1), mar = c(2,5.5,1,0.3), las = 1, ...)}
  
  x = apply(x*factor, c(1,2), mean, na.rm = TRUE) #mean over the replicates
  
  boxplot(x, horizontal = horizontal, names = speciesNames, col = col, axes = axes, ...)
  
  legendFactor = -(log10(factor))
  legendFactor = bquote("x" ~ 10^.(legendFactor) ~ "tonnes")
  mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)
  
  return(invisible())
}

.recycleArguments = function(x, length) {
  
  if(length(x) < length(1:length)) {x = rep(x, length.out = length)}
  if(length(x) == length(1:length)) {x = x}
  if(length(x) > length(1:length)) {x = x[c(1:length(1:length))]}
  
  return(x)
}

# boxplot with mean over the time
plot2DType3 = function(x, horizontal = FALSE, col = NULL, 
                       factor = 1e-3, speciesNames = NULL, cex = 0.8, axes = TRUE, ...) {
  
  if(is.null(speciesNames)) speciesNames = toupper(colnames(x)) else speciesNames = speciesNames
  if(is.null(col)) col="gray"
  
  if(!isTRUE(horizontal)){par(oma = c(1,1,1,1), mar = c(2.5,2,1,0.3), las = 1, ...)
  } else {par(oma = c(1,1,1,1), mar = c(2,5.5,1,0.3), las = 1, ...)}
  
  x = apply(x*factor, c(3,2), mean, na.rm = TRUE) #mean over the time
  
  boxplot(x, horizontal = horizontal, names = speciesNames, col = col, axes = axes, ...)
  
  legendFactor = -(log10(factor))
  legendFactor = bquote("x" ~ 10^.(legendFactor) ~ "tonnes")
  mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)
  
  return(invisible())
}
