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

  outlist = list()
  for (sp in names(x)) {
    outlist[[sp]] = process.mortalityRate(x, time.mean=TRUE, species=sp)
  }
  
  msg = sprintf("3D plot type %d is not implemented yet.", type)
  switch(type,
         "1" = plotMortRateType1(outlist, norm=norm, speciesNames=speciesNames, ...),
         "2" = plotMortRateType2(outlist, speciesNames=speciesNames, ...),
         stop(msg))
  
  return(invisible())
  
}

plotMortRateType1 = function(x, norm, speciesNames, ...) { 
  
  # To keep the plot params as the beginning
  op = par(no.readonly = TRUE)
  on.exit(par(op))
  
  # Define multiplot array if there're more than 1 species
  if(length(x) > 1){
    mar = rep(4, 4)  # bottom left top right
    mar = c(4, 4, 1, 4)
    oma = rep(1, 4)
    par(mar=mar, oma = oma)
    mfrow = getmfrow(length(x))
  } else {
    mfrow = c(1, 1)
  }
  
  par(mfrow = mfrow)
  
  i = 1
  for (data in x) { 
    
    # if normalize, display mortality rates into percentage instead of absolutes.
    if(norm) {
      # apply the normalize function to all the elements of the list.
      data = lapply(data, norm_func)
    }
    
    # convert the list into a matrix
    data = do.call(cbind.data.frame, data)
    data = as.matrix(data)
    print(data)
    
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
    print(dim(data))
  
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
  
  par(mfrow = c(2, 2))
  
  # recovers the list of arguments 
  args = list(...)
 
  # Sets the colors by using the color function.
  if(is.null(args$col)) {
    colfun = rainbow
  } else { 
    colfun = args$col
    args$col = NULL
  }
  
  i = 1
  for (data in x) { 

    for (class in names(data)) {
      
      temp = data[[class]]
      title = paste(speciesNames[i], class, sep=', ')
      
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
    
    # Add a spurious plot in order to have one page per species
    plot.new()
    
    i = i + 1
  
  }
  
  return(invisible())
  
}
