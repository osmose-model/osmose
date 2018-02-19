.osmose.extract_mort = function(mortMatrix, specName, repl=NULL)
{   
  
  data = mortMatrix[[specName]]   # time, npreys, nreplicates
  
  data = lapply(data, .osmose.extract_repl, repl=repl)
  
  return(data)
  
}

#' Extracts the time averaged mortality rate for a given specie.
#' It also eventually normalizes the mortality rates into percent. 
#'
#' @param mortMatrix Mortality matrix 
#' @param specName  Name of the specie to process
#' @param repl Replicate index. If NULL, the replicate mean is extracted.
#' @param norm If TRUE, normalize the mortality rates (return percentage)
#'
#' @export
osmose.extract_mort_tmean = function(mortMatrix, specName, repl=NULL, norm=TRUE)
{   
  
  # extracts the replicate mean for each stade
  # output is a list (one per stade) of (ntime, nmort) matrixes
  data = .osmose.extract_mort(mortMatrix, specName, repl)
  
  # computes the time-average mortality rate
  data = lapply(data, apply, 2, mean, na.rm=TRUE)
  
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
  output = do.call(cbind.data.frame, data)
  output = as.matrix(output)
  
  # add some information attributes.
  attr(output, "specie") = specName
  attr(output, "norm") = norm
  
  return(output)
  
}

#' Plots the time-mean mortality rates as
#' a barplot
#'
#' @param data Time-average mortality rate 
#' @param ... Additional arguments to the barplot functions
#' @export
osmose.plot.mortMean = function(data, ...)
{
  
  xlabel = "Stade"
  specName = attr(output, "specie")
  norm = attr(output, "norm")
  if(norm) {
    ylabel = "Mortality rate (%)"
  } else {
    ylabel = "Mortality rate"
  }
  
  osmose.stackedpcent(data, xlab=xlabel, main=specName, ylab=ylabel, ...)

}


