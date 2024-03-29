# OSMOSE (Object-oriented Simulator of Marine Ecosystems)
# http://www.osmose-model.org
#
# Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
#
# Osmose is a computer program whose purpose is to simulate fish
# populations and their interactions with their biotic and abiotic environment.
# OSMOSE is a spatial, multispecies and individual-based model which assumes
# size-based opportunistic predation based on spatio-temporal co-occurrence
# and size adequacy between a predator and its prey. It represents fish
# individuals grouped into schools, which are characterized by their size,
# weight, age, taxonomy and geographical location, and which undergo major
# processes of fish life cycle (growth, explicit predation, additional and
# starvation mortalities, reproduction and migration) and fishing mortalities
# (Shin and Cury 2001, 2004).
#
# Contributor(s):
# Yunne SHIN (yunne.shin@ird.fr),
# Morgane TRAVERS (morgane.travers@ifremer.fr)
# Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
# Philippe VERLEY (philippe.verley@ird.fr)
# Laure VELEZ (laure.velez@ird.fr)
# Nicolas Barrier (nicolas.barrier@ird.fr)
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation (version 3 of the License). Full description
# is provided on the LICENSE file.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.



# @param ... Additional arguments of the function.
# @export
# @return An array or a list containing the data.
process.dietMatrix = function(out, species = NULL, time.mean = TRUE, thres = 1, ...) {
  
  # If a list, extracts species names
  if(is.list(out)) { 
    .check_species(out, species)
    # extract the given specie
    out = out[[species]]
  }
  
  # Computes the mean over the replicates
  out = apply(out, c(1, 2), mean)
  
  # computes the time average
  data.time.mean = apply(out, 2, mean, na.rm=TRUE)   # adding this to avoid NULL output in summary
  keep = (data.time.mean >= thres)  # keep values for which the max is greater than the threshold
  
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
  
  # If list, extract value for one species
  if(!is.null(species)) { 
    .check_species(out, species)
    
    # extract the given specie
    out = out[[species]]
  }
  
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

.extract_species_from_list = function(x, species, ...) {
  
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
      
      species = match(species, names(x)) - 1
    }
    
    species = names(x)[species + 1]
    
    x = x[species, drop = FALSE]
  }
  
  return(x)
}