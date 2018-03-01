# @param ... Additional arguments of the function.
# @return An array or a list containing the data.
process.dietMatrix = function(out, species=NULL, time.mean=FALSE, thres=1, ...) {
  
  .check_species(out, species)
  
  # extract the given specie
  out = out[[species]]
  
  # Computes the mean over the replicates
  out = apply(out, c(1, 2), mean)
  
  # computes the time average
  data.time.mean = apply(out, 2, mean)
  keep = (data.time.mean > thres)  # keep values for which the max is greater than the threshold
  
  if(time.mean)
  {
    # extracts the mean values above a given threshold
    data.time.mean = data.time.mean[keep]
    Nvalues = length(data.time.mean)
    
    # compute the proportion of negligible species
    other = 100 - sum(data.time.mean)
    
    # add the other to the output array
    data.time.mean = c(data.time.mean, other)
    names(data.time.mean)[Nvalues+1] = "other"
    
    # sort the time average in increasing order
    data.time.mean = sort(data.time.mean, decreasing=TRUE)
    
    class(data.time.mean) = c("osmose.output.dietMatrix", class(data.time.mean))
    return(data.time.mean)
    
  } 
  
  # extract the data that do not match the threshold requirements
  # and sum over the specie dimension.
  other = out[, keep==FALSE]
  other = apply(other, 1, sum)
  
  # extracts the data that match the requirement
  out = out[, keep==TRUE]
  
  # add the concatenation of small ("other") species
  out = cbind(out, other)
  
  # sort the data in descending order
  # based on the time maximum (nspecies values) 
  temp = apply(out, 2, mean)
  index = sort(temp, decreasing=FALSE, index.return=TRUE)$ix
  # returns the sorted array
  out = out[, index]
  
  class(out) = c("osmose.output.dietMatrix", class(out))
  
  return(out)
}


# @param ... Additional arguments of the function.
# @return An array or a list containing the data.
process.mortalityRate = function(out, species=NULL, time.mean=FALSE, ...)
{
  
  .check_species(out, species)
  
  # extract the given specie
  out = out[[species]]
  
  # computes the replicate mean: for each list element
  # computes the mean over the 4th dimension (replicate)
  out = lapply(out, apply, c(1, 2), mean)
  
  # if time.mean, computes the time average
  if(time.mean)
  {
    # for each element, compute the time-average of the matrix
    out = lapply(out, apply, 2, mean)
  }
  
  
  class(out) = c("osmose.output.mortalityRate", class(out))
  
  return(out)
  
}


.check_species = function(out, species)
{
  if(is.null(species))
  {
    stop("You must provide a specie name")
  }
  
  # Check if the specie considered exists in the dataset.
  if(!(species %in% names(out)))
  {
    stop("The specie name ", specName, "is not in the diet matrix")
  }
}


.compute_average_matrix = function(out, time.mean, repl.mean)
{
  
  species = colnames(out)
  
  # If replicate and time average: average over dims 2 and 3
  if(repl.mean & time.mean)
  {
    out = apply(out, 1, mean, na.rm=TRUE)
    names(out) = species
  } else if (repl.mean) {
    out = apply(out, c(1, 2), mean, na.rm=TRUE)
  } else if(time.mean) {
    out = apply(out, c(2, 3), mean, na.rm=TRUE)
  }
  
  return(out)
  
}