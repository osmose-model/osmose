# @param ... Additional arguments of the function.
# @export
# @return An array or a list containing the data.
process.dietMatrix = function(out, species = NULL, time.mean = TRUE, thres = 1, ...) {
  
  .check_species(out, species)
  
  # extract the given specie
  out = out[[species]]
  
  # Computes the mean over the replicates
  out = apply(out, c(1, 2), mean)
  
  # computes the time average
  data.time.mean = apply(out, 2, mean, na.rm=TRUE)   # adding this to avoid NULL output in summary
  keep = (data.time.mean > thres)  # keep values for which the max is greater than the threshold
  
  if(time.mean) {
    # extracts the mean values above a given threshold
    data.time.mean = data.time.mean[keep]
    Nvalues = length(data.time.mean)
    
    if(thres > 0) {
      # If thresholds is greater than 0, then the negligible species are binned together.
      
      # compute the proportion of negligible species
      other = 100 - sum(data.time.mean)
      
      # add the other to the output array
      data.time.mean = c(data.time.mean, other)
      names(data.time.mean)[Nvalues + 1] = "Other"
    }
    
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
  
  if(thres > 0) {
    # add the concatenation of small ("other") species
    out = cbind(out, other)  
  }
  
  # sort the data in descending order
  # based on the time maximum (nspecies values) 
  temp = apply(out, 2, mean, na.rm=TRUE)
  
  index = sort(temp, decreasing=FALSE, index.return=TRUE)$ix
  # returns the sorted array
  out = out[, index]
  
  class(out) = c("osmose.output.dietMatrix", class(out))
  
  return(out)
}


# @param ... Additional arguments of the function.
# @return An array or a list containing the data.
process.mortalityRate = function(out, species=NULL, time.mean=TRUE, ...) {
  
  .check_species(out, species)
  
  # extract the given specie
  out = out[[species]]
  
  # computes the replicate mean: for each list element
  # computes the mean over the 4th dimension (replicate)
  out = lapply(out, apply, c(1, 2), mean, na.rm=TRUE)
  
  # if time.mean, computes the time average
  if(time.mean) {
    # for each element, compute the time-average of the matrix
    out = lapply(out, apply, 2, mean, na.rm=TRUE)
  }
  
  class(out) = c("osmose.output.mortalityRate", class(out))
  
  return(out)
  
}


.check_species = function(out, species) {
  if(is.null(species)) {
    stop("You must provide a specie name")
  }
  
  # Check if the specie considered exists in the dataset.
  if(!(species %in% names(out))) {
    stop("The specie name ", species, "is not in the diet matrix")
  }
}


.compute_average_matrix = function(out, time.mean, repl.mean) {
  
  species = colnames(out)
  
  # If replicate and time average: average over dims 2 and 3
  if(repl.mean & time.mean) {
    out = apply(out, 1, mean, na.rm=TRUE)
    names(out) = species
  } else if (repl.mean) {
    out = apply(out, c(1, 2), mean, na.rm=TRUE)
  } else if(time.mean) {
    out = apply(out, c(2, 3), mean, na.rm=TRUE)
  }
  
  return(out)
  
}


#' Title
#'
#' @param object an object of class \code{osmose.mortalityRate} for which a summary is desired.
#' @param species Name of the species to get a summary. 
#' @param thres Threshold which is used to keep values of species matrix.
#' @param ... Extra arguments passed to the method.
#'
#' @export
#' @method summary osmose.dietMatrix
summary.osmose.dietMatrix = function(object, species = NULL, thres = 1, ...) {
  
  dietMatrix = process.dietMatrix(object, species = species, time.mean = TRUE, thres = thres, ...)
  dietMatrix = as.data.frame(dietMatrix)
  colnames(dietMatrix) = 'Predation rate (%)'
  
  #class(dietMatrix) = c("summary.osmose.output.dietMatrix", class(temp))
  return(dietMatrix)
}

#' Title
#'
#' @param object an object of class \code{osmose.mortalityRate} for which a summary is desired.
#' @param species Name of the species to get a summary. 
#' @param ... Extra arguments passed to the method.
#
#' @export
#' @method summary osmose.mortalityRate
summary.osmose.mortalityRate = function(object, species = NULL, ...) {
  data = process.mortalityRate(object, species = species, time.mean = TRUE)
  return(as.data.frame(data))  
}

#' @export
#' @method summary osmose.biomass
summary.osmose.biomass = function(object, ...) {
  return(.summary.generic(object))
}

#' @export
#' @method summary osmose.meanTL
summary.osmose.meanTL = function(object, ...) {
  return(.summary.generic(object))
}

#' @export
#' @method summary osmose.meanTLCatch
summary.osmose.meanTLCatch = function(object, ...) {
  return(.summary.generic(object))
}

.summary.generic = function(object) {
  data = apply(object, 2, mean, na.rm=TRUE)
  data = sort(data, decreasing=TRUE)
  data = as.data.frame(data)
  return(data)
}



