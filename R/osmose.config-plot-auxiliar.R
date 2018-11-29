

# Reproduction plot functions ---------------------------------------------

plotReproductionType1 = function(x, times, xlim, ylim, speciesNames = NULL,
                                 axes = TRUE, legend = TRUE, ...){
  
  if(is.null(speciesNames)) {speciesNames = colnames(x)[2]} else {speciesNames = speciesNames}
  
  par(oma = c(1,1,1,1), mar = c(2,2.2,1,1.5)) 
  plot(x = times, y = x[,2], type = "l", xlab = "", ylab = "", xaxs = "i", yaxs = "i",
       xlim = xlim, ylim = ylim, axes = FALSE, ...)
  if(isTRUE(axes)){
    axis(1, ...)
    axis(2, las = 2, ...)
    box()
  }
  
  if(isTRUE(legend)){
    mtext(toupper(speciesNames), side = 3, line = -1.5, adj = 1, ...)
  }
  
  return(invisible())
}

plotReproductionType2 = function(x, ylim, speciesNames = NULL, axes = TRUE,  legend = TRUE, ...){
  
  if(is.null(speciesNames)) {speciesNames = colnames(x)[2]} else {speciesNames = speciesNames}
  
  par(oma = c(1,1,1,1), mar = c(2,2.2,1,1.5)) 
  
  barplot(height = x[,2], border = NA, axes = FALSE, ylim = c(0, max(x[,2]*1.25)), xaxs = "i", ...)
  
  if(isTRUE(axes)){
    axis(1, ...)
    axis(2, las = 2, ...)
    box()
  }
  
  if(isTRUE(legend)){
    mtext(toupper(speciesNames), side = 3, line = -1.5, adj = 1, ...)
  }
  
  return(invisible())
}


# Growth plot functions ---------------------------------------------------

plotGrowthType1 = function(x, speciesNames = NULL, axes = TRUE, legend = TRUE, ...){
  
  print("Working ... ")
  
  return(invisible())
}