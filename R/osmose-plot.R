
# Plot methods ------------------------------------------------------------

plot.osmose.biomass = function(x, species = NULL, start = NULL, end = NULL, initialYear = NULL,
                               ts = TRUE, type = 1, replicates = TRUE, nrep = 3,
                               ci = TRUE, freq = 12, horizontal = FALSE, 
                               conf = 0.95, factor = 1e-3, xlim = NULL, ylim = NULL,
                               col = NULL, alpha = 0.5, speciesNames = NULL, ...) {
  
  osmosePlots2D(x = x, species = species, start = start, end = end, initialYear = initialYear,
                ts = ts, type = type, replicates = replicates, nrep = nrep, ci = ci,
                freq = freq, horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, speciesNames = speciesNames, ...)
  
  return(invisible())
}

plot.osmose.abundance = function(x, species = NULL, start = NULL, end = NULL, initialYear = NULL,
                                 ts = TRUE, type = 1, replicates = TRUE, nrep = 3,
                                 ci = TRUE, freq = 12, horizontal = FALSE, 
                                 conf = 0.95, factor = 1e-3, xlim = NULL, ylim = NULL,
                                 col = NULL, alpha = 0.5, speciesNames = NULL, ...) {
  
  osmosePlots2D(x = x, species = species, start = start, end = end, initialYear = initialYear,
                ts = ts, type = type, replicates = replicates, nrep = nrep, ci = ci,
                freq = freq, horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, speciesNames = speciesNames, ...)
  
  return(invisible())
}

plot.osmose.yield = function(x, species = NULL, start = NULL, end = NULL, initialYear = NULL,
                             ts = TRUE, type = 1, replicates = TRUE, nrep = 3,
                             ci = TRUE, freq = 12, horizontal = FALSE, 
                             conf = 0.95, factor = 1e-3, xlim = NULL, ylim = NULL,
                             col = NULL, alpha = 0.5, speciesNames = NULL, ...) {
  
  osmosePlots2D(x = x, species = species, start = start, end = end, initialYear = initialYear,
                ts = ts, type = type, replicates = replicates, nrep = nrep, ci = ci,
                freq = freq, horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, speciesNames = speciesNames, ...)
  
  return(invisible())
}

plot.osmose.yieldN = function(x, species = NULL, start = NULL, end = NULL, initialYear = NULL,
                              ts = TRUE, type = 1, replicates = TRUE, nrep = 3,
                              ci = TRUE, freq = 12, horizontal = FALSE, 
                              conf = 0.95, factor = 1e-3, xlim = NULL, ylim = NULL,
                              col = NULL, alpha = 0.5, speciesNames = NULL, ...) {
  
  osmosePlots2D(x = x, species = species, start = start, end = end, initialYear = initialYear,
                ts = ts, type = type, replicates = replicates, nrep = nrep, ci = ci,
                freq = freq, horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, speciesNames = speciesNames, ...)
  
  return(invisible())
}
