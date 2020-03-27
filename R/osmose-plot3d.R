################################################################ Plots3D
osmosePlots3D = function(x, type, by, species, speciesNames, start, end, 
                         initialYear, freq, horizontal, factor,
                         xlim, ylim, col, border, lty, lwd, axes, legend, 
                         units, ci, log, ...) {

  x = .extract_species_from_list(x, species)
  
  if(!is.null(speciesNames) && length(speciesNames) != length(x)){
   stop("'speciesNames' has an incorrect length.")
  }
  
  if(is.null(speciesNames)){
    speciesNames = toupper(names(x))
  }
  
  # getting time array (not used so far)
  index = .get_start_end(x[[1]], start, end)
  start = index[1]
  end = index[2]
  
  if(is.null(initialYear)) initialYear = 0
  times = seq(from = initialYear + start/freq, by = 1/freq, 
              length.out = nrow(x[[1]]))
  
  # apply processing on the time-series (so far, replicate and time-mean)
  x = sapply(x, .process_3d_fields, start, end, ts, replicates, ...) 
  
  switch(type,
         "1" = plot3DType1(x, by = by, horizontal = horizontal, col = col,
                           factor = factor, speciesNames = speciesNames,
                           ylim = ylim, axes = axes, units = units,
                           border = border, ...),
         "2" = plot3DType2(x, by = by, col = col, factor = factor, 
                           speciesNames = speciesNames,
                           axes = axes, units = units, ...),
         stop(sprintf("3D plot type %d is not implemented yet.", type)))

  return(invisible())
  
}

.get_start_end = function(x, start, end) {
  
  # Check start and end args
  if(is.null(start)) start = 1
  if(is.null(end)) end = dim(x)[1]
  
  if(start < 1 | start > end) stop("Incorrect value for 'start' argument")
  if(end > dim(x)[1] | end < start) stop("Incorrect value for 'end' argument")
  
  return(c(start, end))
}

# Process 3D fields (fields by class). Computes replicates
# or time means. Called through lapply (to loop over all the species)
.process_3d_fields = function(x, start, end, ts, replicates, ...) {
  
  index = .get_start_end(x, start, end)
  start = index[1]
  end = index[2]
  
  if(length(dim(x)) == 3) { 
    x = x[seq(start, end), , , drop = FALSE]  # time, class, replicates
  } else {
    x = x[seq(start, end), , drop = FALSE]  # time, class, replicates  
  }
  x = apply(x, 2, mean, na.rm = TRUE)
  
}

# Displays a histogram showing one plot per species.
plot3DType1 = function(x, by, horizontal, col, factor, 
                       speciesNames, axes, ylim, units, border, ...){
  

  # To keep the plot params as the beginning
  op = par(no.readonly = TRUE)
  on.exit(par(op))

  # Define multiplot array if there're more than 1 species
  if(ncol(x) > 1){
    # Modify oma & mar
    mar = rep(0, 4)
    oma = c(3, 4, 2, 4)
    par(mar = mar, oma = oma)
    
    # Set pane distribution
    mfrow = getmfrow(ncol(x))
  }else{
    mfrow = c(1, 1)
  }
  
  par(mfrow = mfrow)
  
  # Extract args related with line customization
  col = rep(x = if(is.null(col)) "black" else col, length.out = ncol(x))
  
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 0.8, cex)
  
  # Define default value for border
  if(is.null(border)) border = NA

  # recovering the size-class
  byNames = rownames(x)
  
  # Define xlim & ylim if NULL
  if(is.null(ylim)) { 
    ymin = Reduce(min, lapply(x, min)) * factor
    ymax = Reduce(max, lapply(x, max)) * factor
    ylim = c(ymin, ymax*1.1)
  }
  
  # Generate plots by spp
  for(i in seq(ncol(x))){
    
    # Extract values for spp i
    xsp = factor*x[,i]
    
    # Draw barplot
    bp <- barplot(xsp, horiz = horizontal, names.arg = rep(NA, nrow(x)), 
            col = col, ylim = ylim, border = border, 
            xlab = NA, ylab = NA, axes = FALSE, ...)
    
    bp <- as.numeric(bp)
    
    # Add label of factor
    if(i == 1){
      legendFactor = -log10(factor)
      legendFactor = bquote("x" ~ 10^.(legendFactor) ~ .(units$y))
      mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)  
    }
    
    # Add name of species
    mtext(text = speciesNames[i], side = 3, line = -1.5, adj = 1, cex = cex)
    
    # Add axis
    if(isTRUE(axes)){
      las = list(...)$las
      las = ifelse(is.null(las), 1, las)
      
      line = list(...)$line
      line = ifelse(is.null(line), NA, line)
      
      cex.axis = list(...)[["cex.axis"]]
      cex.axis = ifelse(is.null(cex.axis), 1, cex.axis)
      
      if(is.element(i %% (mfrow[2]*2), c(0, 1))){
        axis(side = ifelse(i %% 2 == 1, 2, 4), las = las, line = line, 
             cex.axis = cex.axis)  
      }
      
      if(mfrow[2] > 1 && is.element(i, seq(2, mfrow[2], 2))){
        axis(side = 3, at = bp, labels = byNames, las = las, line = line, 
             cex.axis = cex.axis)
      }
      
      index = c(seq(from = ncol(x) - mfrow[2] + 1, by = 2, 
                    to = prod(mfrow) - mfrow[2] + 1),
                seq(from = prod(mfrow), by = -2, length.out = mfrow[2] - 1))
      if(is.element(i, index)){
        axis(side = 1, at = bp, labels = byNames, las = las, line = line, 
             cex.axis = cex.axis)
      }
      
      box()
      
      if(i == 1){
        mtext(text = paste0(by, ' (', units$x, ')'), side = 1, line = 2, 
              outer = TRUE, cex = cex)
      }
    }
  }
  
  return(invisible())
}

# Showing biomass as a function of class as a raster file.
plot3DType2 = function(x, col, factor, speciesNames, axes, units, by, ...){
  
  # List of the form x, y & z
  x <- list(x = seq(nrow(x)),
            y = seq(ncol(x)),
            z = x*factor)
  
  # Modify margins to give space for species names
  par(mar = c(3, 10, 1, 1))
  
  # Define default color palette
  if(is.null(col)) col <- tim.colors(1e3)
  
  # Draw image plot
  image.plot(x, axes = FALSE, legend.lab = units$y, col = col, ...)
  
  # Add axis
  if(isTRUE(axes)){
    axis(side = 1, at = x$x, labels = rownames(x$z))
    axis(side = 2, at = x$y, labels = colnames(x$z), las = 1)
    
    box()
    
    mtext(text = paste0(by, ' (', units$x, ')'), side = 1, line = 2)
  }
  
  return(invisible())
}
