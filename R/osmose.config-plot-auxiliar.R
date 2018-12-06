

# Reproduction plot functions ---------------------------------------------

plotReproductionType1 = function(x, times, xlim, ylim, speciesNames = NULL, axes = TRUE, legend = TRUE, ...){
  
  if(is.null(speciesNames)) {speciesNames = colnames(x)[2]} else {speciesNames = speciesNames}
  
  #par(oma = c(1,1,1,1), mar = c(2,2.2,1,1.5)) 
  plot(x = times, y = x[,2], type = "l", xlab = "", ylab = "", xaxs = "i", yaxs = "i",
       xlim = xlim, ylim = ylim, axes = FALSE, ...)
  if(isTRUE(axes)){
    axis(1, ...)
    axis(2, las = 2, ...)
    box()
  }
  
  if(isTRUE(legend)){
    mtext(toupper(speciesNames), side = 3, line = -1.5, adj = 1, ...)
  }
  
  return(invisible())
}

plotReproductionType2 = function(x, ylim, speciesNames = NULL, axes = TRUE,  legend = TRUE,
                                 names.arg = NULL, border = NA, ...){
  
  if(is.null(speciesNames)) {speciesNames = colnames(x)[2]} else {speciesNames = speciesNames}
  
  #par(oma = c(1,1,1,1), mar = c(2,2.2,1,1.5)) 
  
  barplot(height = x[,2], border = border, axes = axes, ylim = c(0, max(x[,2]*1.25)), xaxs = "i",
          names.arg = names.arg, ...)
  
  if(isTRUE(axes)){
    axis(1, ...)
    axis(2, las = 2, ...)
    box()
  }
  
  if(isTRUE(legend)){
    mtext(toupper(speciesNames), side = 3, line = -1.5, adj = 1, ...)
  }
  
  return(invisible())
}


# Growth plot functions ---------------------------------------------------

plotGrowthType1 = function(x, species, n = 100, xlim = NULL, ylim = NULL, 
                           addSegment = TRUE, addPoint = TRUE, addPolygon = TRUE, addText = TRUE,
                           legend = TRUE, speciesNames = NULL, ...) {
  
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
  plot(age, length, type = "l", lwd = 1.5, xlab = "Age (years)", ylab = "Length (cm)",
       axes = FALSE, xlim = xlim, ylim = ylim, xaxs = "i", yaxs = "i", ...)
  axis(1, ...)
  axis(2, las = 2, ...)
  box()
  
  if(isTRUE(addSegment)){
    segments(x0 = 0,      x1 = ageInv, y0 = lengthInv, y1 = lengthInv, col = "red", lty = 2, lwd = 1.5, ...)
    segments(x0 = ageInv, x1 = ageInv, y0 = 0,         y1 = lengthInv, col = "red", lty = 2, lwd = 1.5, ...)
  }
  
  if(isTRUE(addPoint)) points(x = ageInv, y = lengthInv, pch = 19, col = "red", ...)
  
  if(isTRUE(addPolygon)){
    #gray area
    polygon(x = c(0, par$thr, par$thr, 0),
            y = c(0, 0, max(length)*1.25, max(length)*1.25),
            col = rgb(0, 0, 1, 0.1), border = NA, ...)
  } 
  
  if(isTRUE(addText)){
    text(x = (ageInv    + 2.5 * (age[2]-age[1])),
         y = (lengthInv - 2.5 * (length[2]-length[1])),
         bquote(paste('L'['m']*' = ', .(lengthInv))), bty = "n", cex = 1.2, pos = 4, ...)
    
    legend("bottomright", legend = bquote(paste('A'['max']*' = ', .(par$lifespan))),
           bty = "n", cex = 1.2, ...)
  }
  
  #legend
  if(is.null(speciesNames)) speciesNames = par$speciesNames else speciesNames = speciesNames
  if(isTRUE(legend)){mtext(toupper(par$speciesNames), side = 3, line = -1.5, adj = 1, ...)}
  
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

plotPredationType1 = function(x, species){
  
  print("in process")
  
  return(invisible())
}
