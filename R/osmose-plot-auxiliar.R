
# Internal plot functions -------------------------------------------------

osmosePlots2D = function(x, species, start, end, initialYear, ts, type,
                         replicates, nrep, ci = TRUE, freq, horizontal, conf,
                         factor, xlim, ylim, col, alpha, speciesNames, axes,
                         legend, cex, ...) {
  
  # species indexation
  if(!is.null(species)){
    
    if((max(species) + 1) > dim(x)[2]){
      stop("error on species indexation, incorrect value in the parameter called species")
    }
    
    x = x[ , species + 1, , drop = FALSE]
  }
  
  # Check start and end args
  start <- ifelse(is.null(start), 1, start)
  end <- ifelse(is.null(end), dim(x)[1], end)
  
  if(start < 1 | start > end) stop("Incorrect value for 'start' argument")
  if(end < 1 | end < start) stop("Incorrect value for 'end' argument")
  
  x = x[seq(start, end), , ,drop = FALSE]
  
  # xlim 
  initialYear = ifelse(is.null(initialYear), as.numeric(rownames(x)[1]), initialYear)
  times       = seq(from = initialYear + 0.5/freq, by = 1/freq, length.out = nrow(x))
  xlim        = if(is.null(xlim)) range(times) else xlim
  
  if(!(type %in% c(1:4))){
    warning("The type argument selected is not correct. The value by default is used (type = 1)")
    type = 1
  } 
  
  if(isTRUE(ts)){
    
    if(type == 1) { plot2DTsType1(x = x, replicates = replicates, nrep = nrep, ci = ci,
                                times = times, xlim = xlim, ylim = ylim,
                                conf = conf, factor = factor, col = col, alpha = alpha,
                                speciesNames = speciesNames, axes = axes, cex = cex, ...) }
    
    if(type == 2) { plot2DTsType2(x = x, replicates = replicates, nrep = nrep, ci = ci,
                                times = times, xlim = xlim, ylim = ylim,
                                conf = conf, factor = factor, col = col, alpha = alpha,
                                speciesNames = speciesNames, axes = axes,
                                legend = legend, cex = cex, ...) }
    
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

plot2DTsType1 = function(x, replicates, nrep, ci, times, xlim, ylim, conf, factor, 
                         col, alpha, speciesNames, axes, cex, ...) {
  
  # Define name of species
  if(is.null(speciesNames)){
    speciesNames <- toupper(colnames(x))
  } 
  
  # Define multiplot array if there're more than 1 species
  if(ncol(x) > 1){
    par(oma = rep(1, 4), mar = c(3, 3, 1, 1), mfrow = getmfrow(ncol(x)))
  }
  
  # Extract args related with line customization
  col <- rep(x = if(is.null(list(...)$col)) "black" else list(...)$col, length.out = ncol(x))
  lty <- rep(x = if(is.null(list(...)$lty)) "solid" else list(...)$lty, length.out = ncol(x))
  lwd <- rep(x = if(is.null(list(...)$lwd)) 1 else list(...)$lty, length.out = ncol(x))
  border <- rep(x = if(is.null(list(...)$border)) NA else list(...)$border, length.out = ncol(x))
  
  # Define ylim
  if(is.null(ylim)){
    ylim = range(as.numeric(x))*factor
  }  #pending: ylim flexible for the users
  
  # Generate plots by spp
  for(sp in seq_len(ncol(x))){
    # Extract values for spp i
    xsp = factor*x[, sp, ,drop = FALSE]
    
    # Set an empty canvas
    plot.new()
    plot.window(xlim = xlim, ylim = ylim)
    
    # Draw the plot
    plotCI(x = xsp, y = times, replicates = replicates, ci = ci, nrep = nrep, prob = 1 - conf, 
           col = col[sp], alpha = alpha, lty = lty[sp], lwd = lwd[sp], border = border, ...)
    
    # Add spp names
    mtext(text = speciesNames[sp], side = 3, line = -1.5, adj = 0.05, cex = cex)
    
    # Add factor label at topleft
    legendFactor = -log10(factor)
    legendFactor = bquote("x" ~ 10^.(legendFactor) ~ "tonnes")
    mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)
    
    if(isTRUE(axes)){
      axis(side = 1)
      axis(side = 2, las = 1)
      box()
    }
  }
  
  return(invisible())
}

plot2DTsType2 = function(x, replicates, nrep, ci, times, xlim, ylim, conf, factor, 
                         col, alpha, speciesNames, axes, legend, cex, ...) {
  
  # Define name of species
  if(is.null(speciesNames)){
    speciesNames <- toupper(colnames(x))
  }
  
  if(is.null(ylim)){
    ylim = range(as.numeric(x))*factor
  }
  
  # Extract args related with line customization
  col <- if(is.null(list(...)$col)) rainbow(n = ncol(x)) else rep(x = col, length.out = ncol(x))
  lty <- rep(x = if(is.null(list(...)$lty)) "solid" else list(...)$lty, length.out = ncol(x))
  lwd <- rep(x = if(is.null(list(...)$lwd)) 1 else list(...)$lty, length.out = ncol(x))
  border <- rep(x = if(is.null(list(...)$border)) NA else list(...)$border, length.out = ncol(x))
  
  # Set an empty canvas
  plot.new()
  plot.window(xlim = xlim, ylim = ylim)
  
  # Generate plots by spp
  for(sp in seq(ncol(x))) {
    # Extract values for spp i
    xsp = factor*x[, sp, ,drop = FALSE]
    
    # Draw the plot
    plotCI(x = xsp, y = times, replicates = replicates, ci = ci, nrep = nrep, prob = 1 - conf, 
           col = col[sp], alpha = alpha, lty = lty[sp], lwd = lwd[sp], border = border, ...)
  }
  
  if(isTRUE(axes)){
    axis(side = 1)
    axis(side = 2, las = 1)
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

plotCI = function(x, y, replicates, ci, nrep, prob, col, alpha, lty, lwd, border, ...){
  
  if(dim(x)[3] == 1){
    lines(x = y, y = apply(x, 1, mean, na.rm = TRUE), col = col, lty = lty, ...)
    
    return(invisible())
  }
  
  x.50  = apply(x, 1, median)
  
  if(isTRUE(replicates)) {
    x.inf = apply(x, 1, quantile, prob = prob/2)
    x.sup = apply(x, 1, quantile, prob = 1 - prob/2)
    
    x.pol = c(y, rev(y), y[1])
    y.pol = c(x.inf, rev(x.sup), x.inf[1])
    
    polygon(x = x.pol, y = y.pol, col = adjustcolor(col = col, alpha.f = alpha), 
            border = border, ...)
  }
  
  lines(x = y, y = x.50, col = col, lty = lty, lwd = lwd, ...)
  
  return(invisible())
}

plot2DTsType3 = function(x, times, xlim, ylim=NULL, factor=1e-3,
                         col = NULL, speciesNames = NULL, legend = TRUE, axes = TRUE, cex = 0.8, ...) {
  
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
  
  col <- rep(x = if(is.null(col)) rainbow(ncol(dataSpecies)) else col, 
             length.out = ncol(dataSpecies))[orderData]
  
  par(oma = c(1,1,1,1), mar = c(2,2,1,0.5), xaxs = "i", yaxs = "i")
  plot.new()
  plot.window(xlim=xlim, ylim=ylim)
  
  for(sp in seq_len(ncol(x))) {
    #lines(times, dataSpecies[, sp])
    x.pol = c(times, rev(times))
    y.pol = c(dataSpecies[, sp], rep(0, times = nrow(x)))
    polygon(x.pol, y.pol, border=NA, col = col[sp], ...)
  }
  
  if(isTRUE(axes)){
    axis(side = 1)
    axis(side = 2, las = 1)
    box()
  }
  
  
  legendFactor = -(log10(factor))
  legendFactor = bquote("x" ~ 10^.(legendFactor) ~ "tonnes")
  mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)
  
  if(isTRUE(legend)){
    legend("topleft", legend = speciesNames, col = col, bty = "n", cex = cex, lty = 1)
  }
  
  return(invisible())
}

#plot for only one species using bars
plot2DTsType4 = function(x, times, xlim, ylim = NULL,
                         factor = 1e-3, col = NULL, 
                         speciesNames = NULL, lty = NULL, cex = 0.8, legend = TRUE, axes = TRUE, ...) {
  
  if(is.null(speciesNames)) speciesNames = toupper(colnames(x)) else speciesNames = speciesNames
  if(dim(x)[2]>1) stop("Plot ts = TRUE and type = 4 is only for one species")
  
  col <- rep(x = if(is.null(col)) "black" else col, length.out = dim(x)[2])
  lty <- rep(x = if(is.null(lty)) "black" else lty, length.out = dim(x)[2])
 
  x = apply(x, 1, mean, na.rm = TRUE)*factor
  if(is.null(ylim)){ylim =  c(0, 1.25)*range(x)} else {ylim = ylim}
  
  plot(x = times, y = x, col = col, lty = lty, type = "h", axes = FALSE, xlab = "", ylab = "",
       xlim = xlim, ylim = ylim, ...)
  
  if(isTRUE(axes)){
    axis(side = 1)
    axis(side = 2, las = 1)
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

# .recycleArguments = function(x, length) {
#   
#   if(length(x) < length(1:length)) {x = rep(x, length.out = length)}
#   if(length(x) == length(1:length)) {x = x}
#   if(length(x) > length(1:length)) {x = x[c(1:length(1:length))]}
#   
#   return(x)
# }

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


# normalize function. 
# returns percentage instead of raw values
norm_func = function(data) {
  output = 100 * data / (sum(data, na.rm=TRUE) + .Machine$double.xmin)
  dimnames(output) = dimnames(data)
  return(output)
}

# Extract the mortality array providing the 
# mortality type.
# extract_mort = function(mort, mtype){
#   
#   if(mtype == "Mtot")
#   {
#     mort = mort[["Mpred"]] + mort[["Mstar"]] + mort[["Mnat"]] + mort[["F"]] + mort[["Z"]]
#   } else {
#     
#     if(!(mtype %in% names(mort)))
#     {
#       stop('Mortality type should be "Mtot", Mpred", "Mstar", "Mnat", "F" or "Z"')
#     }
#     
#     # extracts the mortality type
#     mort = mort[[mtype]]
#     
#   }
#   
#   return(mort)
# 
# }