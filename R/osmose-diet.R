

#' Extracts the diet matrix of a given specie and eventually averages
#' it over the replicates
#'
#' @param dietMatrix Diet Matrix
#' @param specName Specie name
#' @param repl Index of the replicate to extract. If NULL, averages over the replicates
.osmose.extract_diet = function(dietMatrix, specName, repl)
{
  
  # Check if the specie considered exists in the dataset.
  test = specName %in% names(dietMatrix)
  if(!test)
  {
    stop("The specie name ", specName, "is not in the diet matrix")
  }
  
  data = dietMatrix[[specName]]   # time, npreys, nreplicates
  
  # if the data has more than three dimensions.
  # if so, replicates exist. 
  if(length(dim(data)) == 3)
  {
    # if no replicate number is provided, a mean
    # over the replicates is computed.
    if(is.null(repl))
    {
      data = apply(data, c(1, 2), mean)
    }
    
    else {
      
      # checks that the replicate index is ok
      nreplicates = dim(data)[3]
      if((repl < 1) | (repl > nreplicates))
      {
        error("The number of replicates must be between 1 and ", nreplicates)
      }
      
      # extracts the given replicate
      data = data[, , repl]
    }
  }
  
  return(data)
}


#' Extracts a time averaged diet matrix.
#' It also merges together all the prey species
#' for which the predation rate is less equal a given threshold.
#'
#' @param dietMatrix Diet matrix (data$trophic$dietMatrix)
#' @param specName Name of the specie.
#' @param repl Replicate number to extract. If NULL, the replicate averaged is extracted.
#' @param thres Percentage below which the predation rates will be merged
#'
#' @export
  osmose.extract_tmean_diet = function(dietMatrix, specName, repl=NULL, thres=1) {
  
  # extracts the diet matrix for the given specie
  # and eventually averages over the replicates
  data = .osmose.extract_diet(dietMatrix, specName, repl)
  
  # computes the time average
  data.time.mean = apply(data, 2, mean)
  
  # extracts the mean values above a given threshold
  data.time.mean = data.time.mean[data.time.mean > thres]
  Nvalues = length(data.time.mean)
  
  # compute the proportion of negligible species
  other = 100 - sum(data.time.mean)
  
  # add the other to the output array
  data.time.mean = c(data.time.mean, other)
  names(data.time.mean)[Nvalues+1] = "other"
  
  # sort the time average in increasing order
  data.time.mean = sort(data.time.mean, decreasing=TRUE)
  
  attr(data.time.mean, "specie") = specName

  return(data.time.mean)
  
  }



#' Extracts the time series of the diet matrix for a given 
#' specie. The output is also formatted into the ggplor2
#' stacked plot format. The predation rates below a given threshold
#' are merged together.
#'
#' @param dietMatrix Diet matrix 
#' @param specName  Specie name
#' @param repl  Replicates.
#' @param thres Threshold below which predation rates
#' are merged.
#'
#' @return A dataframe
#' @export
osmose.extract_ts_diet = function(dietMatrix, specName, repl=NULL, thres=1)
{
  # extracts the diet matrix for the given specie
  # and eventually averages over the replicates
  data = .osmose.extract_diet(dietMatrix, specName, repl)
  
  data.time.mean = apply(data, 2, mean)
  keep = (data.time.mean > thres)  # keep values for which the max is greater than the threshold
  
  # extract the data that do not match the threshold requirements
  # and sum over the specie dimension.
  other = data[, keep==FALSE]
  other = apply(other, 1, sum)
  
  # extracts the data that match the requirement
  data = data[, keep==TRUE]
  
  # add the concatenation of small ("other") species
  data = cbind(data, other)
  
  # sort the data in descending order
  # based on the time maximum (nspecies values) 
  temp = apply(data, 2, mean)
  index = sort(temp, decreasing=FALSE, index.return=TRUE)$ix
  # returns the sorted array
  data = data[, index]
  
  output = .osmose.format_data_stacked(data)
  
  attr(output, "specie") = specName

  return(output)
  
  
}

# Reformat the data into the ggplot2 stacked plot format.
# It returns a dataframe with specie, time and predation rate
# in the columns.
.osmose.format_data_stacked = function(data)
{
  
  ntime = dim(data)[1]
  npecie = dim(data)[2]
  time = 1:ntime  # extracts the time array
  
  # Initialize the output array
  output = data.frame()
  
  # loop over all the specie and update the output array.
  for (spec in colnames(data)) {
    temp = data.frame(specie=rep(spec, ntime), time=time, value=data[, spec])
    output = rbind(output, temp)
  }
  
  # converts specie into a factor
  output$specie = factor(output$specie)
  
  return(output)
  
}



#' Plots a time averaged diet matrix as a barplot.
#' It also merges together all the prey species
#' for which the predation rate is below a given threshold.
#'
#' @param dietMatrix osmose.dietMean object
#' @param ... Additional arguments to the osmose.barplot function
#'
#' @export
osmose.plot.dietMean = function(x, ...)
{
  specName = attr(x, "specie")
  temp = as.vector(x)
  names(temp) = names(x)
  osmose.barplot(temp, xlab="", ylab="Predation (%)", main=specName, ...)
  
}

#' Plot time series of osmose diet as a stacked plot
#'
#' @param data Diet ts data
#' @param colors  Ggplot2 color table. If NULL, a default value (scale_fill_brewer) is used.
#' @param ...  Additional arguments to the geom_area ggplot2 function
#'
#' @export
osmose.plot.dietTs = function(data, colors=NULL, ...)
{
  
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




