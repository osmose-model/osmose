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

# All the methods for osmose class ----------------------------------------

#' @title Plot method for osmose objects
#' @description This method takes a \code{osmose} object and produce useful 
#' plots
#' @param x Object of \code{osmose} class. See the \code{\link{read_osmose}} 
#' function for more details.
#' @param what Variable name to plot. By default is \code{what = "biomass"}. 
#' See Details
#' @param ... Additional arguments for the plot function.
#' 
#' @details \code{what} argument can take next values:
#' \itemize{
#'  \item "biomass" to plot the species biomass (see \link{plot.osmose.biomass})
#'  \item "abundance" to plot the species abundance (see \link{plot.osmose.abundance})
#'  \item "yield" to plot the species yield (see \link{plot.osmose.yield})
#'  \item "yieldN" to plot the species yield (in numbers) (see \link{plot.osmose.yieldN})
#' }
#' 
#' @return A graph of an \code{osmose} object.
#' @export
#' @method plot osmose
plot.osmose = function(x, what = "biomass", ...) {
  
  x = get_var(x, what = what, expected = FALSE)
  plot(x, ...)
  
  return(invisible())
}

#' get_var method for osmose outputs objects
#' @description Get a variable from an \code{osmose} object. 
#'
#' @param object Object of \code{osmose} class (see the \code{\link{read_osmose}} 
#' function).
#' @param what Name of variable to extract. See Details.
#' @param how How to return the object. Current options are "matrix" and "list".
#' @param expected A logical parameter. If \code{TRUE}, the average over the 
#' last dimensions will be performed (only if the output is an array).
#' @param ... Additional arguments of the function.
#' 
#' @details \code{what} can be any available variable contained on \code{object}
#' (e.g. biomass, abundance, yield, yieldN, etc).
#'
#' @return An matrix or a list containing the data.
#' @export
#' @method get_var osmose
get_var.osmose = function(object, what, how = c("matrix", "list"), 
                          expected = FALSE, ...){
  
  # Argument verification of 'how' using partial matching
  how = match.arg(how)
  
  # Change the value of expected if how is a list
  if(how == "list") expected = TRUE
  
  # Extract variable from object
  out = object[[what]]
  
  # If it's NULL, then show an error message
  if(is.null(out)) {
    message = paste("The", sQuote(what), "variable is NULL.", sep="")
    stop(message) 
  }
  
  if(inherits(out, "array") & isTRUE(expected)){
    out = apply(out, c(1, 2), mean, na.rm = TRUE)
  }
  
  if(how == "matrix") return(out)
  
  if(how == "list") return(as.list(as.data.frame(out, check.names = FALSE)))
  
  return(out)
}


#' Print information for an \code{osmose} object
#'
#' @param x \code{osmose} class object (see the \code{\link{read_osmose}} 
#' function).
#' @param ... Additional arguments for \code{print}.
#' 
#' @method print osmose
#' @export
print.osmose = function(x, ...) {
  cat(paste0("OSMOSE v.", x$model$version, "\n"))
  cat("Model", sQuote(x$model$model),"\n\n")
  cat(sprintf("%s species modeled (%s simulations):", x$model$nsp, x$model$simus))
  cat(sprintf("\n\t[sp%s] %s", seq(0, x$model$nsp - 1), x$species), "\n")
  
  # Get dimension (no vector classes) or length (vector classes) for each level
  infoLevels = sapply(x, function(x) if(is.array(x)) dim(x) else length(x))
  
  # Check which levels are empty (dim or length equal to zero)
  infoLevels = sapply(infoLevels, function(x) isTRUE(all.equal(x, 0)))
  
  # Add a mark (*) for those empty level's' names
  infoLevels = paste0(names(infoLevels), ifelse(infoLevels, " (*)", ""))
  
  # If length of level (of names) vector is odd, add an empty value
  infoLevels = c(infoLevels, 
                 if(length(infoLevels) %% 2 != 0) "---------" else NULL)
  
  # Sort vector (of names) as a 2 columns matrix
  infoLevels = matrix(data = infoLevels, ncol = 2)
  dimnames(infoLevels) = list(rep("", nrow(infoLevels)), rep("", ncol(infoLevels)))
  
  # Show available variables
  cat("\nAvailable fields:\n")
  print(infoLevels)
  cat("\n(*) Empty fields.\n")
}

#' @title \code{osmose} object summaries
#'
#' @param object \code{osmose} class object (see the \code{\link{read_osmose}} 
#' function).
#' @param ... Additional arguments for \code{summary}.
#' @param digits \code{integer}, used for number formatting (by default, 
#' \code{1L}). Check \link{summary.default}.
#' 
#' @method summary osmose
#' @export
summary.osmose = function(object, ..., digits = 1L) {
  
  # Catch model and species info from object
  output = list(model = object$model,
                species = object$species)
  
  # Get dimension (no vector classes) or length (vector classes) for each level
  infoLevels = sapply(object, function(x) if(is.array(x)) dim(x) else length(x))
  
  # Check which levels are empty (dim or length equal to zero)
  infoLevels = sapply(infoLevels, function(x) isTRUE(all.equal(x, 0)))
  
  # Add vector indicating if level is empty or not
  output$is_empty = matrix(data = infoLevels, ncol = 1, 
                           dimnames = list(names(infoLevels), "is_empty"))
  
  # Define main outputs to show info
  resumenVars = c("biomass", "abundance", "yield", "yieldN")
  
  # Get an index for those variables which are not NULL
  resumenVars = intersect(resumenVars, names(infoLevels)[!infoLevels])
  
  # Get summary info about selected variables
  if(length(resumenVars) > 0){
    # Get summery values by spp
    values = sapply(object[resumenVars], 
                    function(x) apply(x, 2, mean, na.rm = TRUE))
    
    # Convert values to more readable number
    output$resumen = as.data.frame(apply(values, 2, function(x) format(round(x, digits = digits))))
  }
  
  # Generate output
  class(output) = "summary.osmose"
  
  return(output)
}

#' Print the summary informations about Osmose outputs
#'
#' @param x Osmose outputs (see the \code{\link{read_osmose}} function)
#' @param ... Additional arguments for \code{print}.
#' @method print summary.osmose
#'
#' @export
print.summary.osmose = function(x, ...) {
  
  # Replicate print(osmose)
  cat(paste0("OSMOSE v.", x$model$version, "\n"))
  cat("Model", sQuote(x$model$model),"\n\n")
  cat(sprintf("%s species modeled (%s simulations):", x$model$nsp, x$model$simus))
  cat(sprintf("\n\t[sp%s] %s", seq(0, x$model$nsp - 1), x$species), "\n")
  
  # Show extra info
  if(!is.null(x$resumen)){
    cat("\nMain indicators:\n")
    print(x$resumen)  
  }
}

#' @title Report method for \code{osmose} objects
#' @description Build and export a report of \code{osmose} objects using
#' R markdown.
#'
#' @param x Object of \code{osmose} class.
#' @param format The R Markdown output format to convert to (check 
#' \link[rmarkdown]{render}).
#' @param output The output directory for the rendered the output file.
#' @param tangle Boolean; whether to tangle the R code from the input file 
#' (check \link[rmarkdown]{render}).
#' @param open Do you want to open the output file at the end? (only useful for 
#' MS Windows environment).
#' @param ... Extra arguments pased to \link[rmarkdown]{render}.
#'
#' @method report osmose
#' 
#' @export
report.osmose = function(x, format = "pdf_document", output = NULL, 
                         tangle = FALSE, open = TRUE, ...) {
  
  if(is.null(output)) output = getwd()
  
  outputName = deparse(substitute(x))
  
  skeleton = system.file("reports", "osmose-report.Rmd", package = "osmose")
  
  if(isTRUE(tangle)) {
    knit(skeleton, tangle = TRUE, encoding = "latin1")
    f1 = gsub(pattern = ".Rmd", replacement = "\\.R", skeleton)
    file.rename(from=basename(f1), to=paste0(outputName, ".R"))
  }
  
  outputFile = paste0(outputName, "_output.pdf")
  render(input = skeleton, output_format = format, output_file = outputFile, 
         output_dir = output, encoding = "latin1", )
  
  if(Sys.info()['sysname'] == "Windows" && isTRUE(open)){
    shell.exec(file = file.path(output, outputFile, fsep = "\\"))
  }
  
  return(invisible(file.path(output, outputFile)))
}
