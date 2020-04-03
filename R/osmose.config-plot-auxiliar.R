

# Reproduction plot functions ---------------------------------------------

plotReproductionType1 = function(x, times, xlim, ylim, speciesNames, axes, 
                                 legend, col, ...){
  
  if(is.null(speciesNames)) speciesNames = colnames(x)[2]
  
  plot(x = times, y = x[,2], type = "l", xlab = NA, ylab = NA,
       xlim = xlim, ylim = ylim, axes = FALSE, col = col, ...)
  
  if(isTRUE(axes)){
    las = list(...)$las
    las = ifelse(is.null(las), 1, las)
    
    line = list(...)$line
    line = ifelse(is.null(line), NA, line)
    
    cex.axis = list(...)[["cex.axis"]]
    cex.axis = ifelse(is.null(cex.axis), 1, cex.axis)
    
    axis(side = 1, las = las, line = line, cex.axis = cex.axis)
    axis(side = 2, las = las, line = line, cex.axis = cex.axis)
    box()
  }
  
  if(isTRUE(legend)){
    adj = list(...)[["adj"]]
    adj = ifelse(is.null(adj), 0.99, adj)
    
    cex = list(...)[["cex"]]
    cex = ifelse(is.null(cex), 1, cex)
    
    mtext(text = toupper(speciesNames), side = 3, line = -1.5, adj = adj, 
          cex = cex, col = "black")
  }
  
  return(invisible())
}

plotReproductionType2 = function(x, ylim, speciesNames, axes, border, legend, 
                                 col, ...){
  
  if(is.null(speciesNames)) speciesNames = colnames(x)[2]
  
  xValues = structure(x[,2], .Names = rownames(x))
  xValues = as.numeric(barplot(height = xValues, axes = FALSE, ylim = ylim, 
                               col = col, border = border, ...))
  
  if(isTRUE(axes)){
    las = list(...)$las
    las = ifelse(is.null(las), 1, las)
    
    line = list(...)$line
    line = ifelse(is.null(line), NA, line)
    
    cex.axis = list(...)[["cex.axis"]]
    cex.axis = ifelse(is.null(cex.axis), 1, cex.axis)
    
    axis(side = 1, at = xValues, labels = NA, las = las, line = line, 
         cex.axis = cex.axis)
    axis(side = 2, las = las, line = line, cex.axis = cex.axis)
    box()
  }
  
  if(isTRUE(legend)){
    adj = list(...)[["adj"]]
    adj = ifelse(is.null(adj), 0.99, adj)
    
    cex.lab = list(...)[["cex.lab"]]
    cex.lab = ifelse(is.null(cex.lab), 1, cex.lab)
    
    mtext(text = toupper(speciesNames), side = 3, line = -1.5, adj = adj, 
          cex = cex.lab, col = "black")
  }
  
  return(invisible())
}


# Growth plot functions ---------------------------------------------------

plotGrowthType1 = function(x, n, species, speciesNames, addElements, xlim, ylim, 
                           legend, axes, border, col, ...) {
  
  # Get parameters to display Von Bertalanffy growth equation
  params = getGrowthParameters(x = x, species = species)
  
  # growth estimation
  age = seq(from = 0, to = params$lifespan, length.out = n)
  length = osmoseGrowth(age = age, params = params)
  
  # growth inverted estimation
  ageInv    = osmoseGrowthInv(length = params$maturitySize, params = params)
  lengthInv = params$maturitySize
  
  if(is.null(xlim)) xlim = c(0, max(age))
  if(is.null(ylim)) ylim = c(0, max(length, na.rm = TRUE))
  
  # Set default values for some graphical parameters
  xlab = list(...)[["xlab"]]
  xlab = ifelse(is.null(xlab), "Age (years)", xlab)
  
  ylab = list(...)[["ylab"]]
  ylab = ifelse(is.null(ylab), "Length (cm)", ylab)
  
  # Draw an empty canvas
  plot(1, 1, type = "n", axes = FALSE, xlab = xlab, ylab = ylab, 
       xlim = xlim, ylim = ylim, xaxs = "i", yaxs = "i")
  
  lines(x = age, y = length, col = col, ...)
  
  # Add axes
  if(isTRUE(axes)){
    las = list(...)$las
    las = ifelse(is.null(las), 1, las)
    
    line = list(...)$line
    line = ifelse(is.null(line), NA, line)
    
    cex.axis = list(...)[["cex.axis"]]
    cex.axis = ifelse(is.null(cex.axis), 1, cex.axis)
    
    axis(side = 1, las = las, line = line, cex.axis = cex.axis)
    axis(side = 2, las = las, line = line, cex.axis = cex.axis)
    box()
  }
  
  usr = par("usr")
  
  # Add segments
  if(is.element("segments", tolower(addElements))){
    segments(x0 = usr[1], x1 = ageInv, 
             y0 = lengthInv, y1 = lengthInv, 
             col = "red", lty = "dashed")
    segments(x0 = ageInv, x1 = ageInv, 
             y0 = usr[3], y1 = lengthInv, 
             col = "red", lty = "dashed")
  }
  
  # Add points
  if(is.element("points", tolower(addElements))){
    points(x = ageInv, y = lengthInv, pch = 19, col = "red", ...)
  } 
  
  # Add polygons
  if(is.element("polygon", tolower(addElements))){
    polygon(x = c(usr[1], rep(params$thr, 2), usr[1]),
            y = rep(usr[3:4], each = 2),
            col = adjustcolor(col = col, alpha.f = 0.3), border = border)
  } 
  
  # Add text
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 1, cex)
  
  if(is.element("text", tolower(addElements))){
    
    text(x = ageInv, y = lengthInv,
         labels = bquote(paste('L'['m']*' = ', .(lengthInv))), 
         bty = "n", cex = cex, pos = 3)
    
    legend("bottomright", legend = bquote(paste('A'['max']*' = ', .(params$lifespan))),
           bty = "n", cex = cex)
  }
  
  # Add legend
  if(isTRUE(legend)){
    if(is.null(speciesNames)) speciesNames = params$speciesNames
    
    mtext(text = toupper(speciesNames), side = 3, line = -1.5, adj = 1, cex = cex)
  }
  
  return(invisible())
}

getGrowthParameters = function(x, species){
  
  # Check species argument
  if(is.null(species) || is.na(species)) stop("'species' argument must be specified.")
  if(length(species) > 1) stop("The value of 'species' must be of length 1.")
  if(min(species) < 0 || max(species) > (length(x) - 1)) stop("Incorrect value for 'species'.")
  
  species = paste0("sp", species)
  
  output = list(lifespan      = as.numeric(x$lifespan[species]),
                linf          = as.numeric(x$linf[species]),
                k             = as.numeric(x$k[species]),
                t0            = as.numeric(x$t0[species]),
                thr           = as.numeric(x$thr[species]),
                eggSize       = as.numeric(x$eggSize[species]),
                maturitySize  = as.numeric(x$maturitySize[species]), # check, only work with sizes! TODO
                speciesNames  = as.character(x$names[species]))
  
  return(output)
}


osmoseGrowth = function(age, params) { 
  
  linf    = params$linf
  k       = params$k
  t0      = params$t0
  thr     = params$thr
  eggSize = if(is.null(params$eggSize)) 0 else params$eggSize
  
  length  = linf*(1 - exp(-k*(age - t0)))
  lthr    = linf*(1 - exp(-k*(thr - t0)))
  length2 = eggSize + age*(lthr - eggSize)/thr
  
  length[age <= thr] = length2[age <= thr]
  
  return(length)
}

osmoseGrowthInv = function(length, params) {
  
  linf     = params$linf
  k        = params$k
  t0       = params$t0
  thr      = params$thr
  eggSize  = if(is.null(params$eggSize)) 0 else params$eggSize
  lthr     = linf*(1 - exp(-k*(thr - t0)))
  
  age     = t0 - (1/k)*log(1 - length/linf)
  age2    = thr*(length - eggSize)/(lthr - eggSize)
  
  age[length <= lthr] = age2[length <= lthr]
  
  return(age)
}

# Predation plot functions ------------------------------------------------

plotPredationType1 = function(x, species, speciesNames, addElements, axes,
                              border, xlim, ylim, col, legend, ...){
  
  if(is.null(species)) stop("'species' argument must not be NULL.")
  
  #get the predation parameters to plot
  params = getPredationParameter(x = x, species = species)
  
  #xlim and ylim
  if(is.null(xlim)) xlim = c(0, tail(x = params$threshold, n = 1))
  if(is.null(ylim)) ylim = c(0, max(params$threshold)/min(params$sizeRatioMax)*1.1)
  
  # Draw an empty canvas
  xlab = list(...)[["xlab"]]
  xlab = ifelse(is.null(xlab), "Predator size", xlab)
  
  ylab = list(...)[["ylab"]]
  ylab = ifelse(is.null(ylab), "Prey size", ylab)
  
  # Define default value for cex
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 1, cex)
  
  usr = par(no.readonly = TRUE)$usr
  
  # Draw an empty canvas
  plot(1, 1, type = "n", axes = FALSE, xlab = xlab, ylab = ylab, 
       xlim = xlim, ylim = ylim, xaxs = "i", yaxs = "i")
  
  for(i in seq_along(params$threshold)){
    xValues = params$threshold[c(i, rep(i + 1, 2), i)]
    yValues = xValues/rep(x = c(params$sizeRatioMin[i], params$sizeRatioMax[i]), 
                          each = 2)
    
    # Draw predation size ratio polygon
    polygon(x = xValues, yValues, col = col, border = border, ...)
    
    if(!is.element(i, c(1, length(params$threshold)))){
      
      # Add segments
      if(is.element("segments", tolower(addElements))){
        segments(x0 = params$threshold[i], 
                 x1 = params$threshold[i],
                 y0 = usr[3],
                 y1 = max(params$threshold[-1]/params$sizeRatioMax),
                 col = "black", lty = "dashed", )
      }
      
      # Add points
      if(is.element("points", tolower(addElements))){
        
        pch = list(...)[["pch"]]
        if(is.null(pch)) pch = 16
        
        points(x = params$threshold[i],
               y = max(params$threshold[-1]/params$sizeRatioMax),
               pch = pch)
      } 
      
      # Add text
      cex = list(...)[["cex"]]
      cex = ifelse(is.null(cex), 1, cex)
      
      if(is.element("text", tolower(addElements))){
        
        text(x = params$threshold[i],
             y = max(params$threshold[-1]/params$sizeRatioMax),
             labels = bquote(paste('S'['thr']*' = ', .(params$threshold[i]))),
             pos = 4, cex = cex)
      }
    }
  }
  
  # Add axes
  if(isTRUE(axes)){
    las = list(...)$las
    las = ifelse(is.null(las), 1, las)
    
    line = list(...)$line
    line = ifelse(is.null(line), NA, line)
    
    cex.axis = list(...)[["cex.axis"]]
    cex.axis = ifelse(is.null(cex.axis), 1, cex.axis)
    
    axis(side = 1, las = las, line = line, cex.axis = cex.axis)
    axis(side = 2, las = las, line = line, cex.axis = cex.axis)
    box()
  }
  
  # Add legend
  if(isTRUE(legend)){
    if(is.null(speciesNames)) speciesNames = params$speciesNames
    
    mtext(text = toupper(speciesNames), side = 3, line = -1.5, adj = 1, cex = cex)
  }
  
  return(invisible())
}

getPredationParameter = function(x, species){
  
  species = species + 1
  
  linf      = as.numeric(x$linf[species])
  threshold = c(0, x$predPrey$stageThreshold[[species]], linf)
  
  output = list(threshold    = threshold[!is.na(threshold)],
                sizeRatioMax = x$predPrey$sizeRatioMax[[species]],
                sizeRatioMin = x$predPrey$sizeRatioMin[[species]],
                speciesNames = x$speciesNames[species])
  
  return(output)
}

