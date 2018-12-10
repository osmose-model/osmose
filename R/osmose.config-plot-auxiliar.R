

# Reproduction plot functions ---------------------------------------------

plotReproductionType1 = function(x, times, xlim, ylim, speciesNames = NULL,
                                 axes = TRUE, legend = TRUE, cex = 1.2, cex.axis = 1.2, ...){
  
  if(is.null(speciesNames)) {speciesNames = colnames(x)[2]} else {speciesNames = speciesNames}
  
  #par(oma = c(1,1,1,1), mar = c(2,2.2,1,1.5)) 
  plot(x = times, y = x[,2], type = "l", xlab = "", ylab = "", xaxs = "i", yaxs = "i",
       xlim = xlim, ylim = ylim, axes = FALSE, ...)
  if(isTRUE(axes)){
    axis(1, cex.axis = cex.axis, ...)
    axis(2, las = 2, cex.axis = cex.axis, ...)
    box()
  }
  
  if(isTRUE(legend)){
    mtext(toupper(speciesNames), side = 3, line = -1.5, adj = 1, cex = cex)
  }
  
  return(invisible())
}

plotReproductionType2 = function(x, ylim, speciesNames = NULL, axes = TRUE,  legend = TRUE,
                                 names.arg = NULL, border = NA, cex = 1.2, cex.axis = 1.2, ...){
  
  if(is.null(speciesNames)) {speciesNames = colnames(x)[2]} else {speciesNames = speciesNames}
  
  #par(oma = c(1,1,1,1), mar = c(2,2.2,1,1.5)) 
  
  barplot(height = x[,2], border = border, axes = FALSE, ylim = c(0, max(x[,2]*1.25)), xaxs = "i",
          names.arg = names.arg, ...)
  
  if(isTRUE(axes)){
    axis(1, cex.axis = cex.axis, ...)
    axis(2, las = 2, cex.axis = cex.axis, ...)
    box()
  }
  
  if(isTRUE(legend)){
    mtext(toupper(speciesNames), side = 3, line = -1.5, adj = 1, cex = cex)
  }
  
  return(invisible())
}


# Growth plot functions ---------------------------------------------------

plotGrowthType1 = function(x, species, n = 100, xlim = NULL, ylim = NULL, 
                           addSegment = TRUE, addPoint = TRUE, addPolygon = TRUE, addText = TRUE,
                           legend = TRUE, speciesNames = NULL, cex.axis = 1.2, xlab = NULL, ylab = NULL, 
                           border = NA, col = "black", cex = 1.2, lty = 1, lwd = 1, ...) {
  
  # get parameter to use on the von bertalanffy growth equation
  par = getGrowthParameters(par = x, sp = species)
  
  # growth estimation
  age = seq(from = 0, to = 1.1*par$lifespan, len = n)
  length = osmoseGrowth(age = age, par = par)
  
  # growth inver. estimation
  ageInv    = osmoseGrowthInv(length = par$maturitySize, par = par)
  lengthInv = par$maturitySize
  
  xlim = if(is.null(xlim)) c(0, max(age))
  ylim = if(is.null(ylim)) c(0, max(length)*1.25)
  
  # plot
  # par(mar = c(4,4,2,1.5), oma = c(1,1,0.5,0.5))
  plot(age, length, type = "l", xlab = "", ylab = "", axes = FALSE,
       xlim = xlim, ylim = ylim, xaxs = "i", yaxs = "i", col = col, lty = lty, lwd = lwd, ...)
  axis(1, cex.axis = cex.axis, ...)
  axis(2, las = 2, cex.axis = cex.axis, ...)
  box()
  
  if(is.null(xlab)) xlab = "Age (years)" else xlab = xlab
  mtext(text = xlab, side = 1, line = 2.5, cex = cex, ...)
  
  if(is.null(ylab)) ylab = "Length (cm)" else ylab = ylab
  mtext(text = ylab, side = 2, line = 2.8, cex = cex, ...)
  
  if(isTRUE(addSegment)){
    segments(x0 = 0,      x1 = ageInv, y0 = lengthInv, y1 = lengthInv, col = "red", lty = 2, lwd = 1.5, ...)
    segments(x0 = ageInv, x1 = ageInv, y0 = 0,         y1 = lengthInv, col = "red", lty = 2, lwd = 1.5, ...)
  }
  
  if(isTRUE(addPoint)) points(x = ageInv, y = lengthInv, pch = 19, col = "red", ...)
  
  if(isTRUE(addPolygon)){
    #gray area
    polygon(x = c(0, par$thr, par$thr, 0),
            y = c(0, 0, max(length)*1.25, max(length)*1.25),
            col = rgb(0, 0, 1, 0.1), border = border, ...)
  } 
  
  if(isTRUE(addText)){
    text(x = (ageInv    + 2.5 * (age[2]-age[1])),
         y = (lengthInv - 2.5 * (length[2]-length[1])),
         bquote(paste('L'['m']*' = ', .(lengthInv))), bty = "n", cex = cex, pos = 4, ...)
    
    legend("bottomright", legend = bquote(paste('A'['max']*' = ', .(par$lifespan))),
           bty = "n", cex = cex, ...)
  }
  
  #legend
  if(is.null(speciesNames)) speciesNames = par$speciesNames else speciesNames = speciesNames
  if(isTRUE(legend)){mtext(toupper(speciesNames), side = 3, line = -1.5, adj = 1, cex = cex, ...)}
  
  return(invisible())
}

# Internal growth functions
getGrowthParameters = function(par, sp) {
  
  # species indexation
  if(length(sp)>1) stop("the value of the parameter called species have to be of length equal to 1")
  if(max(sp)>length(par$names)) stop("error on species indexation, incorrect value in the parameter called species")
  if(is.null(sp)) {warning("the value of the parameter called species is NULL, we are using the value 1 by default")
    sp = 0}
  sp = paste0("sp", sp)
  
  output = list()
  output = within(output, {
    
    lifespan      = as.numeric(par$lifespan[sp])
    linf          = as.numeric(par$linf[sp])
    k             = as.numeric(par$k[sp])
    t0            = as.numeric(par$t0[sp])
    thr           = as.numeric(par$thr[sp])
    eggSize       = as.numeric(par$eggSize[sp])
    maturitySize  = as.numeric(par$maturitySize[sp]) # check, only work with sizes! TODO
    speciesNames  = as.character(par$names[sp])
    
  })
  
  return(output)
}

osmoseGrowth = function(age, par) { 
  
  linf    = par$linf
  k       = par$k
  t0      = par$t0
  thr     = par$thr
  eggSize = if(!is.null(par$eggSize)) par$eggSize else 0
  
  length  = linf*(1-exp(-k*(age-t0)))
  lthr    = linf*(1-exp(-k*(thr-t0)))
  length2 = eggSize + age*(lthr-eggSize)/thr
  
  length[age <= thr] = length2[age <= thr]
  
  return(length)
}

osmoseGrowthInv = function(length, par) {
  
  linf     = par$linf
  k        = par$k
  t0       = par$t0
  thr      = par$thr
  eggSize  = if(!is.null(par$eggSize)) par$eggSize else 0
  lthr     = linf*(1-exp(-k*(thr-t0)))
  
  age     = t0 - (1/k)*log(1-length/linf)
  age2    = thr*(length-eggSize)/(lthr-eggSize)
  
  age[length<=lthr] = age2[length<=lthr]
  
  return(age)
}


# Predation plot functions ------------------------------------------------

plotPredationType1 = function(x, species, xlim = NULL, ylim = NULL, border = NA, col = rgb(0,0,1,0.1),
                              cex.axis = 1.2, addSegment = TRUE, addPoint = TRUE, addText = TRUE,
                              legend = TRUE, speciesNames = NULL, cex = 1, xlab = NULL, ylab = NULL,
                              lwd = 2, lty = 2, pch = 19, ...){
  
  #get the predation parameters to plot
  par = getPredationParameter(par = x, sp = species)
  
  #xlim and ylim
  if(is.null(xlim)) xlim = c(0, tail(par$threshold, n = 1)) else xlim = xlim
  if(is.null(ylim)) ylim = c(0, max((1/par$sizeRatioMax)*(par$threshold[-1])))*1.3 else ylim = ylim
  
  #plot
  plot.new()
  plot.window(xlim = xlim, ylim = ylim, xaxs = "i", yaxs = "i", ...)
  
  stages = seq_along(par$threshold)
  for (stage in stages) {
    polygon(x = c(par$threshold[stage], par$threshold[stage+1], par$threshold[stage+1], par$threshold[stage]),
            y = c(par$threshold[stage]*1/par$sizeRatioMin[stage], par$threshold[stage+1]*1/par$sizeRatioMin[stage],
                  par$threshold[stage+1]*1/par$sizeRatioMax[stage], par$threshold[stage]*1/par$sizeRatioMax[stage]),
            border = border, col = col, ...)
    
    if(stage %in% stages[-c(1, length(stages))]){
      if(isTRUE(addSegment)){
        segments(x0 = par$threshold[stage], x1 = par$threshold[stage],
                 y0 = par$threshold[stage]*1/par$sizeRatioMin[stage],
                 y1 = max((1/par$sizeRatioMax)*(par$threshold[-1]))*0.975,
                 col = "black", lwd = lwd, lty = lty, ...)}
      
      if(isTRUE(addPoint)){
        points(x = par$threshold[stage],
               y = max((1/par$sizeRatioMax)*(par$threshold[-1]))*0.975,
               pch = pch, ...)} 
      
      if(isTRUE(addText)){
        text(x = par$threshold[stage],
             y = max((1/par$sizeRatioMax)*(par$threshold[-1]))*0.975,
             bquote(paste('S'['thr']*' = ', .(par$threshold[stage]))),
             bty = "n", pos = 3, cex = cex, ...)}
    }
  }
  
  if(is.null(xlab)) xlab = "Predator size" else xlab = xlab
  mtext(text = xlab, side = 1, line = 2.5, cex = cex, ...)
  
  if(is.null(ylab)) ylab = "Prey size" else ylab = ylab
  mtext(text = ylab, side = 2, line = 2.8, cex = cex, ...)
  
  box()
  axis(1, cex.axis = cex.axis, ...)
  axis(2, las = 2, cex.axis = cex.axis, ...)
  
  if(is.null(speciesNames)) {speciesNames = par$speciesNames} else {speciesNames = speciesNames}
  if(isTRUE(legend)) {mtext(toupper(speciesNames), side = 3, line = -1.5, adj = 1, cex = cex, ...)}
  
  return(invisible())
}

getPredationParameter = function(par, sp){
  
  # species indexation
  if(is.null(sp)) {
    warning("the value of the parameter called species is NULL, we are using the value 1 by default")
    sp = 0}
  if(length(sp) > 1) stop("the value of the parameter called species have to be of length equal to 1")
  if(sp >= length(par$speciesNames)) stop("error on species indexation, incorrect value in the parameter called species")
  
  sp = paste0("sp", sp)
  
  linf           = as.vector(unlist(par$linf[sp]))
  threshold      = c(0, as.vector(unlist(par$predPrey$stageThreshold[sp])), linf)
  
  output = list()
  output = within(output, {
    # Vars
    threshold      = threshold[!is.na(threshold)]
    sizeRatioMax   = as.vector(unlist(par$predPrey$sizeRatioMax[sp]))
    sizeRatioMin   = as.vector(unlist(par$predPrey$sizeRatioMin[sp]))
    speciesNames   = as.vector(unlist(par$speciesNames[sp]))
  })
  
  return(output)
}

