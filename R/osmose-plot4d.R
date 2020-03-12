#' Plot mortality rates
#'
#' @param x Object of class osmose.mortalityRate
#' @param time.mean True if time-average plot should be ok 
#' @param species List of species to plot. If NULL, all species are drawn.
#' @param norm True if percentage is returned instead of raw mort. rates
#' @param ... Additional parameters to the barplot/lines functions
#' @method plot osmose.mortalityRate
#' @export 
plot.osmose.mortalityRate = function(x, species=NULL, speciesNames=NULL, norm=TRUE, type=1, ...) {
  
  # extract the values for a given list of species
  x = .extract_species_from_list(x, species)
  
  if(!is.null(speciesNames) && length(speciesNames) != length(x)){
    stop("'speciesNames' has an incorrect length.")
  }
  
  if(is.null(speciesNames)){
    speciesNames = toupper(names(x))
  }
  
  # computes the time and replicate mean for all the mortalities type class (eggs, juveniles, ...)
  outlist = list()
  for (sp in names(x)) {
    outlist[[sp]] = process.mortalityRate(x, time.mean=TRUE, species=sp)
  }
  
  # draws plots
  msg = sprintf("3D plot type %d is not implemented yet.", type)
  switch(type,
         "1" = plotMortRateType1(outlist, norm=norm, speciesNames=speciesNames, ...),
         "2" = plotMortRateType2(outlist, speciesNames=speciesNames, ...),
         stop(msg))
  
  return(invisible())
  
}

# Plots the mortality rate as a stacked plot
plotMortRateType1 = function(x, norm, speciesNames, ...) { 
  
  # To keep the plot params as the beginning
  op = par(no.readonly = TRUE)
  on.exit(par(op))
  
  i = 1
  # Loop over all the species
  for (data in x) {  
    
    data = .norm_final(data, norm=norm)

    xlabel = "Stage"
    if(norm) {
      ylabel = "Mortality rate (%)"
    } else {
      ylabel = "Mortality rate"
    }
    
    N = nrow(data)
    
    # recovers the list of arguments 
    args = list(...)
    args$height = data
    
    # Sets the colors by using the color function.
    if(is.null(args$col)) {
      args$col = rainbow(N)
    } else { 
      args$col = args$col(N)
    }
    
    args$xaxt ="n"

    xpos = do.call(barplot, args)
    axis(1, at=xpos, labels=colnames(data), las=2, cex.axis=1)  # below
    
    # to do: check that data has colnames and rownames#
    legend("right", "top", 
           legend = rownames(data), 
           ncol=1, xpd=TRUE, inset=c(0.2, 0.),
           fill=args$col, bg="white")
    
    cex = list(...)[["cex"]]
    cex = ifelse(is.null(cex), 0.8, cex)
    
    mtext(text = speciesNames[i], side = 3, line = -1.5, adj = 0.05, cex = cex)
    i = i + 1
    
  }
  
  return(invisible())
  
}

plotMortRateType2 = function(x, speciesNames, ...) { 
  
  # To keep the plot params as the beginning
  op = par(no.readonly = TRUE)
  on.exit(par(op))
  
  # recovers the list of arguments 
  args = list(...)
  
  # Sets the colors by using the color function.
  if(is.null(args$col)) {
    colfun = rainbow
  } else { 
    colfun = args$col
    args$col = NULL
  }
  
  i = 1  # species counter
  
  # loop over the species to extract 
  for (data in x) { 
    
    # normalise mortality and convert it into a common feature
    data = .norm_final(data, norm=FALSE)  
    rnames = rownames(data)
    cnames = colnames(data)
    
    # loop over the class (eggs, juveniles, adults)
    for (class in colnames(data)) {
      
      temp = data[, class] # extracts the data for the given species
      title = paste(speciesNames[i], class, sep=', ')
      names(temp) = rnames
      
      args = list(...)
      args$x = temp
      args$labels = NA
      args$main = title
      args$col = colfun(length(temp))
      
      do.call(pie, args)
      
      legend("topright", 
             legend = names(temp), 
             ncol=1, xpd=TRUE, inset=c(-0.1, 0.),
             fill=args$col, bg="white")
      
    }
  
    i = i + 1
    
  }
  
  return(invisible())
  
}








#' Plots mortality rates by size class.
#'
#' @param x Mortality object
#' @param species Species name
#' @param time.mean If time-average should be plotted. If TRUE, mortality rates by
#' for each age class and for each mortality types are plotted. Else, the time series for
#' a specific type of mortality is plotted. 
#' @param norm Wheter mortality should be normalized (only if time.mean=TRUE)
#' @param mtype Mortality type ("Mtot", Mpred", "Mstar", "Mnat", "F" or "Z")
#' @param ... Additional arguments of the function.
#' 
plot.mortalityRateDistrib = function(x, species=NULL, norm=TRUE, ...) {
  
  # extract the values for a given list of species
  x = .extract_species_from_list(x, species)
  x = lapply(x, .process_mort_rate_dis, norm)
  
  msg = sprintf("3D plot type %d is not implemented yet.", type)
  switch(type,
         "1" = plotMortRateType1(outlist, norm=norm, speciesNames=speciesNames, ...),
         "2" = plotMortRateType2(outlist, speciesNames=speciesNames, ...),
         stop(msg))
  
  return(invisible())

}


.process_mort_rate_dis = function(x, norm=TRUE) {
  
  x = lapply(x, apply, mean, MARGIN=2, na.rm=TRUE) # computes the mean over replicates and time
  mort = do.call(cbind.data.frame, x)
  mort = as.matrix(mort)
  
  # Counts the total mortality rate for each size class
  tot = apply(mort, 1, sum, na.rm=TRUE)
  
  # Extracts the size class with mortality > 0
  mort = mort[tot>0, ]
  tot = tot[tot>0]
  
  # If data should be normalized, then it is divided by the total
  # mortality
  if(norm) mort = (mort / tot) * 100
  
  # Transpose the dataframe
  mort = as.data.frame(t(mort))
  
  
}

# Final normalization. For all the mortality class (either string or values)
# normalize if needed (extracts percentage).

.norm_final = function(data, norm) {
  
  isdf = is.data.frame(data)  # check if data is data frame
  
  # if data frame, recover the dimension names
  dnames = NULL 
  if(isdf) dnames = dimnames(data)
  
  # if normalize, display mortality rates into percentage instead of absolutes.
  if(norm) {
    # apply the normalize   function to all the elements of the list.
    data = lapply(data, norm_func)
  }
  
  # convert the list into a matrix
  data = do.call(cbind.data.frame, data)
  data = as.matrix(data)
  
  if(isdf) dimnames(data) = dnames
  
  return(data)
  
}
  