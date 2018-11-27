
plot.osmose.config.reproduction = function(x, species = NULL, start = NULL, end = NULL, initialYear = NULL, freq = 12,
                                           ts = TRUE, type = 1, xlim =NULL, ylim = NULL,
                                           speciesNames = NULL, axes = TRUE, legend = TRUE, ...){
  
  
  osmoseConfigPlot2D(x = x, species = species, start = start, end = end,
                     initialYear = initialYear, freq = freq,
                     ts = ts, type = type, xlim = xlim, ylim = ylim,
                     speciesNames = speciesNames, axes = axes, legend = legend, ...)
  
  return(invisible())
}

