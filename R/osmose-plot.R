
# Plot methods ------------------------------------------------------------

#' Plot function for biomass
#' @description This function takes a object of the class \code{osmose.biomass} and 
#' produce useful plots for the analysis of this variable.
#' @param x Object of the class \code{osmose.biomass}. This object have three dimensions: 
#' species, time and size or age.
#' @param species A \code{numeric} vector. The species indexation of the object \code{x} is specified 
#' with this parameter. By defaul \code{species = NULL} and there is not species indexation, using 
#' all the species of \code{x} for the plots.
#' @param start A \code{numeric} value. The first element of the time indexation of the object \code{x} is
#' specified with this parameter. By default \code{start = NULL} and the time indexation start 
#' with the first element of \code{x}.
#' @param end A \code{numeric} value. The last element of the time indexation of the object \code{x} is
#' specified with this parameter. By default \code{end = NULL} and the time indexation finish 
#' with the last element of \code{x}.
#' @param initialYear A \code{numeric} value. It specifies the first element that is going to be used on 
#' the x axis for the plots.
#' @param ts \code{logical} parameter. By default \code{ts = TRUE} and the plots are going to be time series 
#' where the x-axis is the time. For plots where the x-axis is not the time \code{ts = FALSE}.
#' @param type A \code{numeric} value. This parameter specifies the type of the plot to be performed. By 
#' default \code{type = 1}. To see all the plot types available for this variable check the 
#' Osmose user manual.
#' @param replicates \code{logical} parameter. Parameter used for plots types: \code{type = 1} and \code{type = 2}.
#' When \code{replicates = TRUE} the plot show the values obtained in each replicates of object (\code{x}). 
#' code{replicates = FALSE} show the mean value of the variable over the time.
#' @param nrep A \code{numeric} value. Indicate the number of replicates to show on the plots. This parameter is 
#' used only when \code{replicates = TRUE} and for the plot types where \code{replicates} is used.
#' @param ci TODO. remove this parameter on type 1 and 2 (ts = TRUE).
#' @param freq A \code{numeric} value to indicate the frequence on the time series plots. 
#' It is \code{freq = 12} (monthly) by default. This parameter is going to be used to create 
#' a vector of the time for the x-axis.
#' @param horizontal \code{logical} parameter. For plots types that are not time series (barplots or boxplots), 
#' this parameter allows represent verticaly the plot (\code{horizontal = FALSE}). \code{horizontal = TRUE} 
#' present the plots horizontaly.
#' @param conf A number to indicate the confidence interval to plot. By default \code{conf = 0.95}.
#' @param factor A number to indicate the scale of the variable on the y-axis. By default \code{factor = 1e-3}.
#' @param col A vector with the color names for the plots. By default \code{col = NULL} and the colors
#'  are chosen by the function. If the length of this parameter is shorter than the number of species, 
#'  the colors are recycled and repeated for the species that have not a specific color. 
#' @param alpha A number between 0 and 1. Indicate the transparency with which the 
#'  confidence interval is colored. By default \code{alpha = 0.5}.
#' @param speciesNames A vector with the names for each species. By default \code{speciesNames = NULL} and the 
#' names inside the object \code{x} are used.
#' @param ... Extra arguments of the function.
#' @return A graph of a object of the class \code{osmose.biomass}.
#' @author Criscely Lujan Paredes
#' @export
plot.osmose.biomass = function(x, species = NULL, start = NULL, end = NULL, initialYear = NULL,
                               ts = TRUE, type = 1, replicates = TRUE, nrep = 3,
                               ci = TRUE, freq = 12, horizontal = FALSE, 
                               conf = 0.95, factor = 1e-3, xlim = NULL, ylim = NULL,
                               col = NULL, alpha = 0.5, speciesNames = NULL, axes = TRUE, ...) {
  
  osmosePlots2D(x = x, species = species, start = start, end = end, initialYear = initialYear,
                ts = ts, type = type, replicates = replicates, nrep = nrep, ci = ci,
                freq = freq, horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, speciesNames = speciesNames,
                axes = axes, ...)
  
  return(invisible())
}

#' Plot function for abundance
#' @description This function takes a object of the class \code{osmose.abundance} and 
#' produce useful plots for the analysis of this variable.
#' @param x Object of the class \code{osmose.abundance}. This object have three dimensions: 
#' species, time and size or age.
#' @param species A \code{numeric} vector. The species indexation of the object \code{x} is specified 
#' with this parameter. By defaul \code{species = NULL} and there is not species indexation, using 
#' all the species of \code{x} for the plots.
#' @param start A \code{numeric} value. The first element of the time indexation of the object \code{x} is
#' specified with this parameter. By default \code{start = NULL} and the time indexation start 
#' with the first element of \code{x}.
#' @param end A \code{numeric} value. The last element of the time indexation of the object \code{x} is
#' specified with this parameter. By default \code{end = NULL} and the time indexation finish 
#' with the last element of \code{x}.
#' @param initialYear A \code{numeric} value. It specifies the first element that is going to be used on 
#' the x axis for the plots.
#' @param ts \code{logical} parameter. By default \code{ts = TRUE} and the plots are going to be time series 
#' where the x-axis is the time. For plots where the x-axis is not the time \code{ts = FALSE}.
#' @param type A \code{numeric} value. This parameter specifies the type of the plot to be performed. By 
#' default \code{type = 1}. To see all the plot types available for this variable check the 
#' Osmose user manual.
#' @param replicates \code{logical} parameter. Parameter used for plots types: \code{type = 1} and \code{type = 2}.
#' When \code{replicates = TRUE} the plot show the values obtained in each replicates of object (\code{x}). 
#' code{replicates = FALSE} show the mean value of the variable over the time.
#' @param nrep A \code{numeric} value. Indicate the number of replicates to show on the plots. This parameter is 
#' used only when \code{replicates = TRUE} and for the plot types where \code{replicates} is used.
#' @param ci TODO. remove this parameter on type 1 and 2 (ts = TRUE).
#' @param freq A \code{numeric} value to indicate the frequence on the time series plots. 
#' It is \code{freq = 12} (monthly) by default. This parameter is going to be used to create 
#' a vector of the time for the x-axis.
#' @param horizontal \code{logical} parameter. For plots types that are not time series (barplots or boxplots), 
#' this parameter allows represent verticaly the plot (\code{horizontal = FALSE}). \code{horizontal = TRUE} 
#' present the plots horizontaly.
#' @param conf A number to indicate the confidence interval to plot. By default \code{conf = 0.95}.
#' @param factor A number to indicate the scale of the variable on the y-axis. By default \code{factor = 1e-3}.
#' @param col A vector with the color names for the plots. By default \code{col = NULL} and the colors
#'  are chosen by the function. If the length of this parameter is shorter than the number of species, 
#'  the colors are recycled and repeated for the species that have not a specific color. 
#' @param alpha A number between 0 and 1. Indicate the transparency with which the 
#'  confidence interval is colored. By default \code{alpha = 0.5}.
#' @param speciesNames A vector with the names for each species. By default \code{speciesNames = NULL} and the 
#' names inside the object \code{x} are used.
#' @param ... Extra arguments of the function.
#' @return A graph of a object of the class \code{osmose.abundance}.
#' @author Criscely Lujan Paredes
#' @export
plot.osmose.abundance = function(x, species = NULL, start = NULL, end = NULL, initialYear = NULL,
                                 ts = TRUE, type = 1, replicates = TRUE, nrep = 3,
                                 ci = TRUE, freq = 12, horizontal = FALSE, 
                                 conf = 0.95, factor = 1e-3, xlim = NULL, ylim = NULL,
                                 col = NULL, alpha = 0.5, speciesNames = NULL, axes = TRUE, ...) {
  
  osmosePlots2D(x = x, species = species, start = start, end = end, initialYear = initialYear,
                ts = ts, type = type, replicates = replicates, nrep = nrep, ci = ci,
                freq = freq, horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, speciesNames = speciesNames,
                axes = axes, ...)
  
  return(invisible())
}

#' Plot function for yield
#' @description This function takes a object of the class \code{osmose.yield} and 
#' produce useful plots for the analysis of this variable.
#' @param x Object of the class \code{osmose.yield}. This object have three dimensions: 
#' species, time and size or age.
#' @param species A \code{numeric} vector. The species indexation of the object \code{x} is specified 
#' with this parameter. By defaul \code{species = NULL} and there is not species indexation, using 
#' all the species of \code{x} for the plots.
#' @param start A \code{numeric} value. The first element of the time indexation of the object \code{x} is
#' specified with this parameter. By default \code{start = NULL} and the time indexation start 
#' with the first element of \code{x}.
#' @param end A \code{numeric} value. The last element of the time indexation of the object \code{x} is
#' specified with this parameter. By default \code{end = NULL} and the time indexation finish 
#' with the last element of \code{x}.
#' @param initialYear A \code{numeric} value. It specifies the first element that is going to be used on 
#' the x axis for the plots.
#' @param ts \code{logical} parameter. By default \code{ts = TRUE} and the plots are going to be time series 
#' where the x-axis is the time. For plots where the x-axis is not the time \code{ts = FALSE}.
#' @param type A \code{numeric} value. This parameter specifies the type of the plot to be performed. By 
#' default \code{type = 1}. To see all the plot types available for this variable check the 
#' Osmose user manual.
#' @param replicates \code{logical} parameter. Parameter used for plots types: \code{type = 1} and \code{type = 2}.
#' When \code{replicates = TRUE} the plot show the values obtained in each replicates of object (\code{x}). 
#' code{replicates = FALSE} show the mean value of the variable over the time.
#' @param nrep A \code{numeric} value. Indicate the number of replicates to show on the plots. This parameter is 
#' used only when \code{replicates = TRUE} and for the plot types where \code{replicates} is used.
#' @param ci TODO. remove this parameter on type 1 and 2 (ts = TRUE).
#' @param freq A \code{numeric} value to indicate the frequence on the time series plots. 
#' It is \code{freq = 12} (monthly) by default. This parameter is going to be used to create 
#' a vector of the time for the x-axis.
#' @param horizontal \code{logical} parameter. For plots types that are not time series (barplots or boxplots), 
#' this parameter allows represent verticaly the plot (\code{horizontal = FALSE}). \code{horizontal = TRUE} 
#' present the plots horizontaly.
#' @param conf A number to indicate the confidence interval to plot. By default \code{conf = 0.95}.
#' @param factor A number to indicate the scale of the variable on the y-axis. By default \code{factor = 1e-3}.
#' @param col A vector with the color names for the plots. By default \code{col = NULL} and the colors
#'  are chosen by the function. If the length of this parameter is shorter than the number of species, 
#'  the colors are recycled and repeated for the species that have not a specific color. 
#' @param alpha A number between 0 and 1. Indicate the transparency with which the 
#'  confidence interval is colored. By default \code{alpha = 0.5}.
#' @param speciesNames A vector with the names for each species. By default \code{speciesNames = NULL} and the 
#' names inside the object \code{x} are used.
#' @param ... Extra arguments of the function.
#' @return A graph of a object of the class \code{osmose.yield}.
#' @author Criscely Lujan Paredes
#' @export
plot.osmose.yield = function(x, species = NULL, start = NULL, end = NULL, initialYear = NULL,
                             ts = TRUE, type = 1, replicates = TRUE, nrep = 3,
                             ci = TRUE, freq = 12, horizontal = FALSE, 
                             conf = 0.95, factor = 1e-3, xlim = NULL, ylim = NULL,
                             col = NULL, alpha = 0.5, speciesNames = NULL, axes = TRUE, ...) {
  
  osmosePlots2D(x = x, species = species, start = start, end = end, initialYear = initialYear,
                ts = ts, type = type, replicates = replicates, nrep = nrep, ci = ci,
                freq = freq, horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, speciesNames = speciesNames,
                axes = axes, ...)
  
  return(invisible())
}

#' Plot function for yield 
#' @description This function takes a object of the class \code{osmose.yieldN}
#'  (yield in Number) and produce useful plots for the analysis of this variable.
#' @param x Object of the class \code{osmose.yieldN}. This object have three dimensions: 
#' species, time and size or age.
#' @param species A \code{numeric} vector. The species indexation of the object \code{x} is specified 
#' with this parameter. By defaul \code{species = NULL} and there is not species indexation, using 
#' all the species of \code{x} for the plots.
#' @param start A \code{numeric} value. The first element of the time indexation of the object \code{x} is
#' specified with this parameter. By default \code{start = NULL} and the time indexation start 
#' with the first element of \code{x}.
#' @param end A \code{numeric} value. The last element of the time indexation of the object \code{x} is
#' specified with this parameter. By default \code{end = NULL} and the time indexation finish 
#' with the last element of \code{x}.
#' @param initialYear A \code{numeric} value. It specifies the first element that is going to be used on 
#' the x axis for the plots.
#' @param ts \code{logical} parameter. By default \code{ts = TRUE} and the plots are going to be time series 
#' where the x-axis is the time. For plots where the x-axis is not the time \code{ts = FALSE}.
#' @param type A \code{numeric} value. This parameter specifies the type of the plot to be performed. By 
#' default \code{type = 1}. To see all the plot types available for this variable check the 
#' Osmose user manual.
#' @param replicates \code{logical} parameter. Parameter used for plots types: \code{type = 1} and \code{type = 2}.
#' When \code{replicates = TRUE} the plot show the values obtained in each replicates of object (\code{x}). 
#' code{replicates = FALSE} show the mean value of the variable over the time.
#' @param nrep A \code{numeric} value. Indicate the number of replicates to show on the plots. This parameter is 
#' used only when \code{replicates = TRUE} and for the plot types where \code{replicates} is used.
#' @param ci TODO. remove this parameter on type 1 and 2 (ts = TRUE).
#' @param freq A \code{numeric} value to indicate the frequence on the time series plots. 
#' It is \code{freq = 12} (monthly) by default. This parameter is going to be used to create 
#' a vector of the time for the x-axis.
#' @param horizontal \code{logical} parameter. For plots types that are not time series (barplots or boxplots), 
#' this parameter allows represent verticaly the plot (\code{horizontal = FALSE}). \code{horizontal = TRUE} 
#' present the plots horizontaly.
#' @param conf A number to indicate the confidence interval to plot. By default \code{conf = 0.95}.
#' @param factor A number to indicate the scale of the variable on the y-axis. By default \code{factor = 1e-3}.
#' @param col A vector with the color names for the plots. By default \code{col = NULL} and the colors
#'  are chosen by the function. If the length of this parameter is shorter than the number of species, 
#'  the colors are recycled and repeated for the species that have not a specific color. 
#' @param alpha A number between 0 and 1. Indicate the transparency with which the 
#'  confidence interval is colored. By default \code{alpha = 0.5}.
#' @param speciesNames A vector with the names for each species. By default \code{speciesNames = NULL} and the 
#' names inside the object \code{x} are used.
#' @param ... Extra arguments of the function.
#' @return A graph of a object of the class \code{osmose.yieldN}.
#' @author Criscely Lujan Paredes
#' @export
plot.osmose.yieldN = function(x, species = NULL, start = NULL, end = NULL, initialYear = NULL,
                              ts = TRUE, type = 1, replicates = TRUE, nrep = 3,
                              ci = TRUE, freq = 12, horizontal = FALSE, 
                              conf = 0.95, factor = 1e-3, xlim = NULL, ylim = NULL,
                              col = NULL, alpha = 0.5, speciesNames = NULL, axes = TRUE, ...) {
  
  osmosePlots2D(x = x, species = species, start = start, end = end, initialYear = initialYear,
                ts = ts, type = type, replicates = replicates, nrep = nrep, ci = ci,
                freq = freq, horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, speciesNames = speciesNames,
                axes = axes, ...)
  
  return(invisible())
}
