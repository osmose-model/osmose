#' @param ... Additional arguments of the function.
#' @return An array or a list containing the data.
#' @export
#' @method plot osmose.output.dietMatrix
plot.osmose.output.dietMatrix = function(x, colors=NULL, ...)
{
    time.mean = attr(x, "time.mean")
    specName = attr(x, "specie")

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

    specName = attr(data, "specie")
    output = ggplot(data, aes(x=Time, y=Predation, fill=Prey)) +  geom_area(...) + colors + ggtitle(specName) 
    output = output + theme(plot.title = element_text(color="black", size=18, face="bold", hjust=0.5))
    return(output)

}


#' @param ... Additional arguments of the function.
#' @return An array or a list containing the data.
#' @export
#' @method plot osmose.output.mortalityRate
plot.osmose.output.mortalityRate = function(data, norm=FALSE, ...)
{
  
  time.mean = attr(data, "time.mean")
  specName = attr(data, "specie")
  
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