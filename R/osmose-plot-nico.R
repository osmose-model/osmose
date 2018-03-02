#' @param ... Additional arguments of the function.
#' @return An array or a list containing the data.
#' @export
#' @method plot osmose.output.dietMatrix
plot.osmose.output.dietMatrix = function(x, time.mean=FALSE, species=NULL, colors=NULL, thres=1, ...)
{
    
    x = process.dietMatrix(x, species=species, time.mean=time.mean, thres=thres)

    if(time.mean)
    {
        # If using a time-averaged diet matrix, then 
        # a barplot is drawn.
        temp = as.vector(x)
        names(temp) = names(x)
        osmose.barplot(temp, xlab="", ylab="Predation (%)", main=specName, ...)
        return(invisible())
    } 

    # format the diet matrix to have stacked format for ggplot2
    data = .osmose.format_data_stacked(x)
    
    # Set the column names.
    colnames(data) = c("Prey", "Time", "Predation")

    # If no color is provided, we set default colors
    if(is.null(colors))
    {
        colors = .make_default_ggplot_col(rev(levels(data$Prey)))
    }

    output = ggplot(data, aes(x=Time, y=Predation, fill=Prey)) +  geom_area(...) + colors + ggtitle(species) 
    output = output + theme(plot.title = element_text(color="black", size=18, face="bold", hjust=0.5))
    return(output)

}


#' @param ... Additional arguments of the function.
#' @return An array or a list containing the data.
#' @export
#' @method plot osmose.output.mortalityRate
plot.osmose.output.mortalityRate = function(data, time.mean=FALSE, norm=FALSE, species=NULL, ...)
{
  
  data = process.mortalityRate(data, species=species, time.mean=time.mean, ...)
  
  if(time.mean==FALSE)
  {
    stop("The plotting of time-varying mortality rate is not implemented yet")
  }
  
  # if normalize, display mortality rates into percentage instead of absolutes.
  if(norm)
  {
    # normalize function
    norm_func = function(data)
    {
      output = 100 * data / sum(data)
      return(output)
    }
    
    # apply the normalize function to all the elements of the list.
    data = lapply(data, norm_func)
    
  }
  
  # convert the list into a matrix
  data = do.call(cbind.data.frame, data)
  data = as.matrix(data)
  
  xlabel = "Stage"
  if(norm) {
    ylabel = "Mortality rate (%)"
  } else {
    ylabel = "Mortality rate"
  }
  
  osmose.stackedpcent(data, xlab=xlabel, main=specName, ylab=ylabel, ...)
  
}






#' @param ... Additional arguments of the function.
#' @return An array or a list containing the data.
#' @export
#' @method plot osmose.output.biomassDistribBySize
plot.osmose.output.biomassDistribBySize = function(data, species=NULL, time.mean=FALSE, lwd=2, ...)
{
  
  .check_species(data, species)
  
  y = data[[species]]
  
  # computes the replicate mean
  apply(y, c(1, 2), mean)
  
  if(time.mean == FALSE)
  {

    nlegend = as.integer(ncol(y) / 5)
    time = 1:nrow(y)
    
    # First create an empty plot.
    plot(1, type = 'n', xlim = c(min(time), max(time)), ylim = c(min(y), max(y)),
         xlab = "Time", ylab = "Biomass")
    
    # Create a list of 22 colors to use for the lines.
    cl <- rainbow(ncol(y))
    plotcol = 1:ncol(y)
    
    # Now fill plot with the log transformed coverage data from the
    # files one by one.
    for(i in 1:ncol(y)) {
      lines(y[,i], col=cl[i], lwd=lwd)
      plotcol[i] <- cl[i]
    }
    
    legend("topright", legend=colnames(y), col = plotcol, lwd=lwd, cex=0.7, title='Size (cm)', ncol=nlegend)
    return(invisible())
  }
  
  if(time.mean){
    
    # Computes the time-mean
    y = apply(y, 2, mean)
    temp = as.vector(y)
    names(temp) = names(y)
    osmose.barplot(temp, xlab="Size (cm)", ylab="Biomass", main=species, ...)
  }
  
  
  
}













