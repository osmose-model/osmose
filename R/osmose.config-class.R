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



#' @title Plot method for \code{osmose.config} objects
#' @description This method takes a \code{osmose.config} object and produce 
#' useful plots.
#' 
#' @param x Object of \code{osmose.config} class. See the \code{\link{read_osmose}} 
#' function for more details.
#' @param what Variable name to plot. By default is \code{what = "predation"}. 
#' See Details.
#' @param ... Additional arguments for the plot function.
#' 
#' @export
#' @method plot osmose.config
plot.osmose.config = function(x, what = "predation", ...) {
  
  x = get_var.osmose.config(x, what = what)
  plot(x, ...)
  
  return(invisible())
}


#' get_var method for osmose configuration objects
#' @description Get the configuration files from 
#' @param object Object of \code{osmose.config} class. 
#' See the \code{\link{read_osmose}} and \code{\link{readOsmoseConfiguration}} functions
#'  for more information about this object.
#' @param what Name of the variable to extract from the configuration file.
#' @param ... Extra arguments for plotting method.
#' @return An object of \code{list} class containing all the relevant information about 
#' the variable extracted.
#' @export
#' @method get_var osmose.config
get_var.osmose.config = function(object, what, ...) {
  
  what = getWhats(x = what)
  
  x = object[[what[1]]]
  
  if(is.null(x)){
    message = paste("The", sQuote(what[1]),
                    "variable doesn't exist on the configuration file.", sep = "")
    stop(message)
  }
  
  x = switch(what,
             reproduction  = getReproductionData(x, var = "season.file", ...),
             species       = getSpeciesData(x, ...),
             predation     = getPredationData(x, object = object, ...),
             x)
  
  class(x) = c(paste("osmose.config", what, sep = "."), class(x))
  
  return(x)
}

# Methods -----------------------------------------------------------------

# print.osmose.config = function(x, ...)

# summary.osmose.config = function(x, ...)

# print.summary.osmose.config = function(x, ...)


