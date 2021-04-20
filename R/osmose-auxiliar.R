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


#' Read Osmose output file
#'
#' @param path Osmose output path
#' @param type Data type ("biomass", etc)
#' @param bySpecies TRUE if should read one file per species.
#' @param ext The extension of the files shich will be read.
#' @param ... Additional arguments
#'
#' @return Output data frame
readOsmoseFiles = function(path, type, bySpecies=FALSE, ext="csv", ...) {
  
  # Build the class name pasting osmose + type
  xclass = paste("osmose", type, sep = ".")
  
  # If the class has a Distrib label, remove it
  xclass = gsub(x = xclass, pattern = "Distrib", replacement = "")
  
  # Get a vector with all files on the path
  allFiles = dir(path = path, recursive = TRUE, include.dirs = FALSE)
  
  # Get files with the selected extensio: ext
  extFiles = allFiles[grepl(pattern = paste0(".", ext), x = allFiles)]
  
  # Read files 
  if(isTRUE(bySpecies)){
    # Subset list of files
    files  = extFiles[grepl(pattern = paste0(type, "-"), x = extFiles)]
    
    # Split path names by species 
    files  = .bySpecies(files = files)
    
    # Read files
    output = lapply(files, .readFilesList, path = path, type = type, ...)
  }else{
    # Subset list of files
    files  = extFiles[grepl(pattern = paste0(type, "_"), x = extFiles)]
    
    # Read files
    output = .readFilesList(files = files, path = path, type = type, ...)
  }
  
  # Define a class for output
  if(!is.null(output)) class(output) = c(xclass, class(output))
  
  return(output)
}

# Non-exported ------------------------------------------------------------


getmfrow = function(n){
  m1 = floor(sqrt(n))
  m2 = ceiling(n/m1)
  out = rev(sort(c(m1, m2)))
  
  return(out)
}

writeOsmoseParameters = function(conf, file, sep=";", append=FALSE) {
  .writeParameter = function(x) {
    out = paste(names(x),paste(x, collapse=sep), sep=sep)
    return(out)
  }
  out = sapply(conf, .writeParameter)
  vars = names(out)
  ind = sort(vars, index.return=TRUE)$ix
  dim(out) = c(length(out), 1)
  out = out[ind,, drop=FALSE]
  rownames(out) = vars[ind]
  write.table(out, file=file, sep="", quote=FALSE, col.names=FALSE, append=append)
  return(invisible(out))
}

# Takes a string and returns the words splitted by "_"
getWhats = function(x){
  index = grep(pattern = "_", x = x)
  
  if(length(index) > 0){
    x = unlist(strsplit(x = x, split = "_"))
  }
  
  return(x)
}
