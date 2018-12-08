
plot.osmose.config.reproduction = function(x, species = NULL, start = NULL, end = NULL, initialYear = NULL,
                                           freq = 12, type = 1, xlim =NULL, ylim = NULL,
                                           speciesNames = NULL, axes = TRUE, legend = TRUE,
                                           names.arg = NULL, ...){
  
  # species indexation
  if(is.null(species)){
    x = x[[1]] 
  } else {
    if(length(species)>1) stop("the value of the parameter called species have to be of length equal to 1")
    if(max(species+1)>length(x)) stop("error on species indexation, incorrect value in the parameter called species")
    x = x[[species+1]]
  }
  
  # time indexation
  if(is.null(start)) start = 1 else start = start
  if(is.null(end)) end = dim(x)[1] else end = end
  if(!(start > 0) | !(start < end)) stop("error on time indexation, incorrect value the parameter called start")
  if(!(end > 0) | !(end > start)) stop("error on time indexation, incorrect value in the parameter called")
  x = x[c(start:end), ]
  
  # xlim and ylim
  initialYear = if(is.null(initialYear)) as.numeric(x[1,1]) else initialYear
  times = seq(from = initialYear + 0.5/freq, by = 1/freq, len = nrow(x))
  xlim = if(is.null(xlim)) range(times)
  ylim = if(is.null(ylim)) c(0, range(x[2])[2]*1.2)
  if(!(type %in% c(1,2))){
    warning("The type argument selected is not correct. The value by default is used (type = 1)")
    type = 1
  }
  
  if(type == 1) {plotReproductionType1(x = x, times = times, xlim = xlim, ylim = ylim,
                                       speciesNames = speciesNames, axes = axes, legend = legend, ...)}
  
  if(type == 2) {plotReproductionType2(x = x, ylim = ylim, speciesNames = speciesNames,
                                       axes = axes, legend = legend, names.arg = names.arg, ...)}
  
  return(invisible())
}


plot.osmose.config.species = function(x, species = NULL, type = 1, xlim = NULL, ylim = NULL, 
                                      addSegment = TRUE, addPoint = TRUE, addPolygon = TRUE, addText = TRUE,
                                      legend = TRUE, speciesNames = NULL, ...){
  
  if(!(type %in% 1)){
    warning("The type argument selected is not correct. The value by default is used (type = 1)")
    type = 1
  }
  
  if(type == 1) {plotGrowthType1(x = x, species = species, xlim = xlim, ylim = ylim,
                                 addSegment = addSegment, addPoint = addPoint, addPolygon = addPolygon, addText = addText,
                                 legend = legend, speciesNames = speciesNames, ...)}
  
  return(invisible())
}


plot.osmose.config.predation = function(x, species = NULL, type = 1, xlim = NULL, ylim = NULL,
                                        border = NA, col = rgb(0,0,1,0.1), cex.axis = 1.2,
                                        addSegment = TRUE, addPoint = TRUE, addText = TRUE,
                                        legend = TRUE, speciesNames = NULL, cex = 1,
                                        xlab = NULL, ylab = NULL, lwd = 2, lty = 2, pch = 19, ...){
  
  if(!(type %in% 1)){
    warning("The type argument selected is not correct. The value by default is used (type = 1)")
    type = 1
  }
  
  if(type == 1) {plotPredationType1(x = x, species = species, xlim = xlim, ylim = ylim, 
                                    border = border, col = col, cex.axis = cex.axis,
                                    addSegment = addSegment, addPoint = addPoint, addText = addText,
                                    legend = legend, speciesNames = speciesNames, cex = cex,
                                    xlab = xlab, ylab = ylab, lwd = lwd, lty = lty, pch = pch, ...)}
  
  return(invisible())
}

