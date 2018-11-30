

# Reproduction plot functions ---------------------------------------------

plotReproductionType1 = function(x, times, xlim, ylim, speciesNames = NULL,
                                 axes = TRUE, legend = TRUE, ...){
  
  if(is.null(speciesNames)) {speciesNames = colnames(x)[2]} else {speciesNames = speciesNames}
  
  par(oma = c(1,1,1,1), mar = c(2,2.2,1,1.5)) 
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

plotReproductionType2 = function(x, ylim, speciesNames = NULL, axes = TRUE,  legend = TRUE, ...){
  
  if(is.null(speciesNames)) {speciesNames = colnames(x)[2]} else {speciesNames = speciesNames}
  
  par(oma = c(1,1,1,1), mar = c(2,2.2,1,1.5)) 
  
  barplot(height = x[,2], border = NA, axes = FALSE, ylim = c(0, max(x[,2]*1.25)), xaxs = "i", ...)
  
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

plotGrowthType1 = function(x, species, speciesNames = NULL, axes = TRUE, legend = TRUE,
                           n = 100, add = FALSE, maturity = FALSE, point = TRUE, ...) {
  
  par = getGrowthParameters(par = x, sp = species)
  
  # growth estimation
  age = seq(from = 0, to = 1.1*par$lifespan, len = n)
  length = .osmoseGrowth(age = age, par = par)
  
  # xlim and ylim
  xlim = c(0, max(age))
  ylim = c(0, max(length)*1.25)
  
  # plot
  if(!isTRUE(add)) {plot(age, length, type = "l", lwd = 1.5, xlab = "Age (years)", ylab = "Length (cm)", ...)
  } else {lines(age, length, lwd = 1.5, ...)}
  axis(1, ...)
  axis(2, las = 2)
  box()
  
  # line of the maturity size  
  if(isTRUE(maturity)) abline(h = par$maturitySize, col = "red", lty = 2, ...)  
  
  # growth inver. estimation
  ageInv    = .osmoseGrowthInv(length = par$maturitySize, par = par)
  lengthInv = par$maturitySize
  if(isTRUE(point)) points(x = age, y = length, pch = 19, col = "red", ...)
  
  text(x = (ageInv    + 2.5 * (age[2]-age[1])),
       y = (lengthInv - 2.5 * (length[2]-length[1])),
       bquote(paste('L'['m']*' = ', .(length))), bty = "n", cex = 1.2, pos = 4)
  
  
  segments(x0 = 0,      x1 = ageInv, y0 = lengthInv, y1 = lengthInv, col = "red", lty = 2, lwd = 1.5, ...)
  segments(x0 = ageInv, x1 = ageInv, y0 = 0,         y1 = lengthInv, col = "red", lty = 2, lwd = 1.5, ...)
  
  #gray area
  polygon(x = c(0, par$thr, par$thr, 0),
          y = c(0, 0, max(length)*1.25, max(length)*1.25),
          col = rgb(0, 0, 1, 0.1), border = NA, ...)
  
  mtext(speciesNames[sp], side = 3, line = -1.5, cex = 1, adj = 0.98)
  
  #legend
  if(is.null(speciesNames)) speciesNames = par$speciesNames else speciesNames = speciesNames
  if(isTRUE(legend)){
    mtext(toupper(speciesNames), side = 3, line = -1.5, adj = 0.98, ...)
  }
  
  legend("bottomright", legend = bquote(paste('A'['max']*' = ', .(parameters$lifespan[sp]))),
         bty = "n", cex = 1.2)
  
  return(invisible())
}

# Internal growth functions

getGrowthParameters = function(par, sp) {
  
  # species indexation
  if(length(sp)>1) stop("the value of the parameter called species have to be of length equal to 1")
  if(max(sp+1)>length(par$names)) stop("error on species indexation, incorrect value in the parameter called species")
  if(is.null(sp)) {warning("the value of the parameter called species is NULL, we are using the value 1 by default")
    sp = 1}
  
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

.osmoseGrowth = function(age, par) {
  
  linf    = par$linf
  k       = par$k
  t0      = par$t0
  thr     = par$thr
  eggSize = if(!is.null(par$eggSize)) par$eggSize else 0
  
  l    = linf*(1-exp(-k*(age-t0)))
  lthr = linf*(1-exp(-k*(thr-t0)))
  l2   = eggSize + age*(lthr-eggSize)/thr
  
  l[age <= thr] = l2[age <= thr]
  
  return(l)
  
}

.osmoseGrowthInv = function(length, par) {
  
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

