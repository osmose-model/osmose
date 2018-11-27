
osmoseConfigPlot2D = function(x, species, start, end, initialYear, freq, ts, type, 
                              xlim, ylim, speciesNames, axes, legend, ...) {
  
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
  times = seq(from = initialYear, by = freq, to = x[dim(x)[1],1])
  xlim = if(is.null(xlim)) range(times)
  ylim = if(is.null(ylim)) c(0, range(x[2])[2]*1.2)
  if(!(type %in% c(1,2))){
    warning("The type argument selected is not correct. The value by default is used (type = 1)")
  }
  
  # switch for plotType
  if(isTRUE(ts)){
    if(type == 1) {configPlot2DType1(x = x, times = times, xlim = xlim, ylim = ylim,
                                     speciesNames = speciesNames, axes = axes, legend = legend, ...)}
    if(type == 2) {print("working on the other plot types ... patience please :) ")}
  }
  
  return(invisible())
}


configPlot2DType1 = function(x, times, xlim, ylim, speciesNames = NULL,
                             axes = TRUE, legend = TRUE, ...){
  
  # speciesNames
  if(is.null(speciesNames)) {speciesNames = colnames(x)[2]} else {speciesNames = speciesNames}
  
  par(oma = c(1,1,1,1), mar = c(2,2.2,1,1.5)) 
  lines(x = times, y = x[,2], type = "l", xlab = "", ylab = "", xaxs = "i", yaxs = "i",
        xlim = xlim, ylim = ylim, ...)
  if(isTRUE(axes)){
    axis(1, ...)
    axis(2, las = 2, ...)
  }
  
  if(isTRUE(legend)){
    legend(toupper(speciesNames), side = 3, line = -1.5, adj = 1, ...)
  }
  
  return(invisible())
}
