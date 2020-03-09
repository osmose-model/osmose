################################################################ Plots3D
osmosePlots3D = function(x, species, speciesNames, start, end, initialYear, ts, 
                         type, replicates, freq, horizontal, conf, factor, 
                         xlim, ylim, col, alpha, border, lty, lwd, axes, legend, 
                         units, ci = TRUE, ...) {

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
      if(any(species > length(x))){
        stop("'species' must be between 1 and ", ncol(x))  
      }
    }else if(is.character(species)){
      if(is.null(names(x))){
        stop("Is not possible to define species as character due to 'x' has not species names defined.")
      }
      
      if(any(!is.element(species, names(x)))){
        stop("Some values of 'species' does not exist.")
      }
      
      species = match(species, names(x))
    }

    species = names(x)[species]

    x = x[species]
    
  }

  if(!is.null(speciesNames) && length(speciesNames) != length(x)){
   stop("'speciesNames' has an incorrect length.")
  }
  
  msg = sprintf("ts=TRUE for 3D fields is not implemented yet.")
  if(isTRUE(ts)){
    stop(msg) 
  } 
  
  msg = sprintf("replicates=FALSE for 3D fields is not implemented yet.")
  if(!isTRUE(replicates)) {
    stop(msg) 
  }
  
  # getting time array (not used so far)
  index = .get_start_end(x[[1]], start, end)
  start = index[1]
  end = index[2]
  
  if(is.null(initialYear)) initialYear = 0
  times = seq(from = initialYear + start/freq, by = 1/freq, 
              length.out = nrow(x[[1]]))
  
  # apply processing on the time-series (so far, replicate and time-mean)
  x = lapply(x, .process_3d_fields, start, end, ts, replicates, ...) 
  
  msg = sprintf("3D plot type %d is not implemented yet.", type)
  switch(type,
         "1" = plot3DType1(x, lty=lty, lwd=lwd, alpha=alpha, ci = ci, horizontal = horizontal, col = col,
                           factor = factor, speciesNames = speciesNames,
                           xlim = xlim, ylim = ylim, axes = axes, units = units,
                           border = border, conf = conf, ...),
         "2" = plot3DType2(x, lty=lty, lwd=lwd, alpha=alpha, ci = ci, horizontal = horizontal, col = col,
                           factor = factor, speciesNames = speciesNames,
                           xlim = xlim, ylim = ylim, axes = axes, units = units,
                           border = border, conf = conf, ...),
         stop(msg))

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

.process_3d_fields = function(x, start, end, ts, replicates, ...) {
  
  index = .get_start_end(x, start, end)
  start = index[1]
  end = index[2]

  x = x[seq(start, end), , , drop = FALSE]  # time, class, replicates

  if(replicates & ts) {  # replicates mean + time series
    x = apply(x, c(1, 2), mean, na.rm = TRUE)
  } else if (replicates & !ts) {  # replicates nean + time_mean
    x = apply(x, 2, mean, na.rm = TRUE)
  } else {
    stop('not implemented yet')
  }

}

plot3DType1 = function(x, lwd, lty, alpha, ci, horizontal, col, factor, speciesNames, axes, 
                       xlim, ylim, units, border, conf, ...){
  
  if(is.null(speciesNames)){
    speciesNames = toupper(names(x))
  }
  
  print(speciesNames)
  
  # To keep the plot params as the beginning
  op = par(no.readonly = TRUE)
  on.exit(par(op))

  # Define multiplot array if there're more than 1 species
  if(length(x) > 1){
    mar = rep(2.5, 4)  # bottom left top right
    oma = rep(1, 4)
    par(mar=mar, oma = oma)
    mfrow = getmfrow(length(x))
  } else {
    mfrow = c(1, 1)
  }
  
  par(mfrow = mfrow)
  
  # Extract args related with line customization
  col = rep(x = if(is.null(col)) "black" else col, length.out = length(x))
  lty = rep(x = if(is.null(lty)) "solid" else lty, length.out = length(x))
  lwd = rep(x = if(is.null(lwd)) 1 else lwd, length.out = length(x))
  
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 0.8, cex)
  
  # Define default value for alpha
  if(is.null(alpha)) alpha = 0.3
  
  # Define default value for border
  if(is.null(border)) border = NA

  # recovering the size-class
  colnames = as.numeric(names(x[[1]]))
  
  # Define xlim & ylim if NULL
  if(is.null(xlim)) xlim = range(colnames)
  if(is.null(ylim)) { 
    ymin = Reduce(min, lapply(x, min)) * factor
    ymax = Reduce(max, lapply(x, max)) * factor
    ylim = c(ymin, ymax)
  }
  
  # Generate plots by spp
  i = 0
  for(n in names(x)) {
    
    i = i + 1
    
    # Extract values for spp i
    xsp = factor*x[[n]]
    
    # Set an empty canvas
    # two lines below are commented out because it returns bad layout
    #plot.new()  
    #plot.window(xlim = xlim, ylim = ylim)
    barplot(xsp, horiz = horizontal, names.arg = colnames, col = col,
            ylim = ylim, xlim = xlim, axes = axes, border = border, ...)
    mtext(text = speciesNames[i], side = 3, line = -1.5, adj = 0.05, cex = cex)

  }
  
  return(invisible())
  
}

plot3DType2 = function(x, lwd, lty, alpha, ci, horizontal, col, factor, speciesNames, axes, 
                       xlim, ylim, units, border, conf, ...){
  
  if(is.null(speciesNames)){
    speciesNames = toupper(names(x))
  }
  
  print(speciesNames)
  
  # To keep the plot params as the beginning
  op = par(no.readonly = TRUE)
  on.exit(par(op))
  
  par(oma=rep(4, 4))

  nval = length(x)
  nel = length(x[[1]])

  disclass = as.numeric(names(x[[1]]))

  z = matrix(as.numeric(unlist(x)), nrow = nel, ncol = nval) # class / species
  print(dim(z))  # 25 x 11 = class x species
  z[z<= 0] = NA
  z = log10(z)
  z = apply(z, 2, rev)  # rev to put imshow in the proper place (equivalent to Python's origin=lower)
  rownames(z) = disclass
  colnames(z) = speciesNames
  
  x = 1:nel  
  y = 1:nval
  xlim = c(min(x), max(x))
  ylim = c(min(y), max(y))
  
  ras = raster(nrow=nel, ncol=nval, 
               xmn=min(x) - 0.5, xmx=max(x) - 0.5, ymn=min(y) - 0.5, ymx=max(y) + 0.5)
  ras[] = z
  plot(ras, xlim=xlim, ylim=ylim, interpolate=FALSE, asp=NA)
  axis(2, at=y, labels=colnames(z), las=2)
  axis(1, at=x, labels=rownames(z), las=2)
  
  return(invisible())
  
}

# require(raster)
# # ## set up the plot region:
# op <- par(bg = "thistle")
# x = matrix(1:15, ncol = 5, nrow = 3)
# x = apply(x, 2, rev)
# xlim = c(-40, 10)
# ylim = c(10, 20)
# image <- raster(ncol=5, nrow=3, xmn=min(xlim), xmx=max(xlim), ymn=min(ylim), ymx=max(ylim))
# print(class(image))
# image[] = x
# class(image)
# print(image)
# plot(image, asp=NA)

# 
# # Copy configuration files into the proper directory
# # run the osmose model
# #run_osmose(demo$config_file, parameters=NULL, output=NULL, version="3.3.3",
# #          options=NULL, verbose=TRUE, clean=TRUE)
# 
# # reads output data
# data = read_osmose('/Users/Nicolas/Dropbox/test_osmose_plots/gog/output')
# 
# biom = get_var(data, what='abundance')
# print(dim(biom))  # 120 = time-steps, 25=species, replicates=3
# #plot(biom, axes=TRUE)
# 
# #species = get_var(data, what='species')
# #print(species)
# #print(length(species))
# 
# #names(data)
# 
# data = get_var(data, "biomassByAge")
# print(colnames(data))
# plot(data, type=2)
# 
