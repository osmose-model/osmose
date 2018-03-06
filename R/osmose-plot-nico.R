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
plot.osmose.output.mortalityRate = function(data, stade=NULL, time.mean=FALSE, norm=FALSE, species=NULL, colors=NULL, ...)
{
  
  data = process.mortalityRate(data, species=species, time.mean=time.mean, ...)
  
  
  if(time.mean==FALSE)
  {
    message = "You must provide a life stage among 'eggs', 'juveniles' or 'adults'"
    if(is.null(stade) | !(stade %in% names(data))) {
      stop(message)
    }
    
    data = data[[stade]]
    
    data = osmose:::.osmose.format_data_stacked(data)
    
    colnames(data) = c("Type", "Time", "Rate")
    
    if(is.null(colors))
    {
    colors = osmose:::.make_default_ggplot_col(rev(levels(data$Type)))
    }
    
    species = specName
    title = paste(species, " (", stade, ")", sep='')
    
    output = ggplot(data, aes(x=Time, y=Rate, fill=Type)) +  geom_area(...) + ggtitle(title)
    output = output + theme(plot.title = element_text(color="black", size=18, face="bold", hjust=0.5))
    return(output)
    
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
  y = apply(y, c(1, 2), mean)
  
  if(time.mean == FALSE)
  {
    .osmose.plot_ts(y, xlab='Time', ylab='Biomass', title=species, lwd=lwd, ...)
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

#' @return An array or a list containing the data.
#' @export
#' @method plot osmose.output.biomassDistribByTL
plot.osmose.output.biomassDistribByTL = function(data, species=NULL, time.mean=TRUE, lwd=2, thres=1, colors=NULL, ...) {
  
  .check_species(data, species)
  
  data = data[[species]]

  # computes the replicate mean
  data = apply(data, c(1, 2), mean)
  
  if(time.mean)
  {
    # computes temporal mean
    data = apply(data, 2, mean)
    
    # if threshold, removes all the time means below 
    # threashold
    if(!is.null(thres)) data = data[data>thres]
  
    # keeps only two significative digits to the TL label
    names(data) = signif(as.numeric(names(data)), 2)
    osmose:::osmose.barplot(data, ylab="Biomass", xlab="Trophic Level", main=species)
    return(invisible())
  }
  
  # removes the TL class for which biomass is 0
  temp = (data != 0)
  temp = apply(temp, 2, sum)
  data = data[, temp>0]
  
  colnames(data) = signif(as.numeric(colnames(data)), 2)
  data = osmose:::.osmose.format_data_stacked(data)
  
  colnames(data) = c("TL", "Time", "Biomass")
    
  if(is.null(colors))
  {
    colors = osmose:::.make_default_ggplot_col(rev(levels(data$Type)))
  }
  
  output = ggplot(data, aes(x=Time, y=Biomass, fill=TL)) +  geom_area(...) + ggtitle(species)
  output = output + theme(plot.title = element_text(color="black", size=18, face="bold", hjust=0.5))
  return(output)
  
}








#' @param ... Additional arguments of the function.
#' @return An array or a list containing the data.
#' @export
#' @method plot osmose.output.predatorPressure
plot.osmose.output.predatorPressure = function(data, time.mean=FALSE, species=NULL, colors=NULL, nmax=25, ...)
{

  .check_species(data, species)
  data = data[[specName]]
  
  # computes replicate mean
  data = apply(data, c(1, 2), mean, na.rm=TRUE)
  
  if(time.mean)
  {
    # If using a time-averaged diet matrix, then 
    # a barplot is draw.
    
    # time.mean
    data = apply(data, 2, mean, na.rm=TRUE)
    data = sort(data, decreasing=TRUE)
    if(!is.null(nmax)) data = data[1:nmax]
    
    temp = as.vector(data)
    names(temp) = names(data)
    osmose.barplot(temp, xlab="", ylab="Predation pressure", main=specName, ...)
    return(invisible())
  } 
  
  # computes the temporal mean and extracts the indexes from higher to lower
  temp = apply(data, 2, mean, na.rm=TRUE)
  temp = sort(temp, decreasing=TRUE, index.return=TRUE)
  index = temp$ix
  data = data[, index]
  if(!is.null(nmax))
  {
    data = data[, 1:nmax]
  }
  
  # format the diet matrix to have stacked format for ggplot2
  data = .osmose.format_data_stacked(data)
  
  # Set the column names.
  colnames(data) = c("Prey", "Time", "Predation.pressure")
  
  # If no color is provided, we set default colors
  if(is.null(colors))
  {
    colors = .make_default_ggplot_col(rev(levels(data$Prey)))
  }
  
  output = ggplot(data, aes(x=Time, y=Predation.pressure, fill=Prey)) +  geom_area(...) + colors + ggtitle(species) 
  output = output + theme(plot.title = element_text(color="black", size=18, face="bold", hjust=0.5))
  return(output)
  
}


