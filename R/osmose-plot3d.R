################################################################ Plots3D

# Internal plot functions -------------------------------------------------

osmosePlots3D = function(x, species, speciesNames, start, end, initialYear, ts, 
                         type, replicates, freq, horizontal, conf, factor, 
                         xlim, ylim, col, alpha, border, lty, lwd, axes, legend, 
                         units, ci = TRUE, ...) {

  print('#####################')
  # CHECK ARGUMENTS
  if(!is.null(species)){
    # Check species I
    message1 = "'species' must be whether a numeric or character vector without NA or duplicated values."
    if(!is.vector(species) || # isn't it a vector?
       all(!is.element(c("character", "numeric"), mode(species))) || # is it character or numeric?
       length(species) < 1 || # its length is greater than 1?
       sum(is.na(species)) > 0 || # is there any NA?
       any(duplicated(species))){ # is there any duplicated value?
       stop(message1)
    }
    
    # Check species II
    if(is.numeric(species)){
      if(any(species > ncol(x))){
        stop("'species' must be between 1 and ", ncol(x))  
      }
    }else if(is.character(species)){
      if(is.null(dimnames(x))){
        stop("Is not possible to define species as character due to 'x' has not species names defined.")
      }
      
      if(any(!is.element(species, colnames(x)))){
        stop("Some values of 'species' does not exist.")
      }
      
      species = match(species, colnames(x))
    }
    
    print('allalalalalal')
    print(species)
    print(names(x))
    #x = x[ , species + 1, , drop = FALSE]
    
  }
  
  print(names(x))
  
  # if(!is.null(speciesNames) && length(speciesNames) != ncol(x)){
  #   stop("'speciesNames' has an incorrect length.")
  # }
  # 
  # # Check start and end args
  # if(is.null(start)) start = 1
  # if(is.null(end)) end = dim(x)[1]
  # 
  # if(start < 1 | start > end) stop("Incorrect value for 'start' argument")
  # if(end > dim(x)[1] | end < start) stop("Incorrect value for 'end' argument")
  # 
  # x = x[seq(start, end), , ,drop = FALSE]
  # 
  # # xlim 
  # if(is.null(initialYear)) initialYear = 0
  # 
  # times = seq(from = initialYear + start/freq, by = 1/freq, 
  #             length.out = nrow(x))
  # 
  # msg = sprintf("Not defined method for ts = %s and type = %s", isTRUE(ts), type)
  # if(isTRUE(ts)){
  #   
  #   switch(type,
  #          "1" = plot2DTsType1(x = x, replicates = replicates, 
  #                              ci = ci, times = times, xlim = xlim, ylim = ylim,
  #                              conf = conf, factor = factor, col = col, 
  #                              alpha = alpha, speciesNames = speciesNames, 
  #                              lty = lty, lwd = lwd, axes = axes, units = units,
  #                              border = border, ...),
  #          "2" = plot2DTsType2(x = x, replicates = replicates, 
  #                              ci = ci, times = times, xlim = xlim, ylim = ylim,
  #                              conf = conf, factor = factor, col = col, 
  #                              alpha = alpha, speciesNames = speciesNames, 
  #                              lty = lty, lwd = lwd, axes = axes, 
  #                              legend = legend, units = units, border = border, 
  #                              ...),
  #          "3" = plot2DTsType3(x = x, times = times, xlim = xlim, ylim = ylim, 
  #                              factor = factor, col = col, alpha = alpha, 
  #                              legend = legend, speciesNames = speciesNames, 
  #                              axes = axes, units = units, border = border, ...),
  #          "4" = plot2DTsType4(x = x, times = times, xlim = xlim, ylim = ylim, 
  #                              factor = factor, lty = lty, lwd = lwd, col = col, 
  #                              alpha = alpha, legend = legend, 
  #                              speciesNames = speciesNames, axes = axes, 
  #                              units = units, ...),
  #          stop(msg))
  # }else{
  #   
  #   switch(type,
  #          "1" = plot2DType1(x, ci = ci, horizontal = horizontal, col = col,
  #                            factor = factor, speciesNames = speciesNames, 
  #                            xlim = xlim, ylim = ylim, axes = axes, units = units,
  #                            border = border, conf = conf, ...),
  #          "2" = plot2DType2(x, horizontal = horizontal, col = col, 
  #                            factor = factor, speciesNames = speciesNames, 
  #                            xlim = xlim, ylim = ylim, axes = axes, units = units, 
  #                            border = border, ...),
  #          "3" = plot2DType3(x, horizontal = horizontal, col = col, 
  #                            factor = factor, speciesNames = speciesNames, 
  #                            xlim = xlim, ylim = ylim, axes = axes, units = units, 
  #                            border = border, ...),
  #          stop(msg))
  # }
  # 
  
  return(invisible())
  
}