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
# Nicolas BARRIER (nicolas.barrier@ird.fr)
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


# write_osmose methods ----------------------------------------------------


#' @author Ricardo Oliveros-Ramos
#' 
#' @export
#' @method write_osmose default
write_osmose.default = function(x, file, sep = ",", col.names = NA, quote = FALSE, 
                        row.names = TRUE, append=FALSE, ...) {
  
  if(isTRUE(append)) cat("\n", file=file, append=TRUE)
  write.table(x = x, file = file, sep = sep, col.names = col.names, quote = quote,
              row.names = row.names, append=append, ...)

}

#' @author Ricardo Oliveros-Ramos
#' 
#' @export
#' @method write_osmose osmose.configuration
write_osmose.osmose.configuration = function(x, file, sep = " = ", append=FALSE, 
                                            par.sep=",", ...) {

  .format_par = function(x, sep=",") {
    if(length(x)<2) return(x)
    out = paste(x, collapse=sep)
    return(out)
  }
  
  x = as.matrix(lapply(x, FUN=.format_par, sep=par.sep))
  
  write_osmose.default(x=x, file=file, sep=sep, col.names=FALSE, quote = FALSE, 
                                  row.names = TRUE, append=append, ...)
  
}

#' @author Ricardo Oliveros-Ramos
#' 
#' @export
#' @method write_osmose list 
write_osmose.list = function(x, file, sep = ",", col.names = NA, quote = FALSE, 
                            row.names = TRUE, append=FALSE, ...) {

    for(i in seq_along(x)) {
      append = append | !(i==1)
      write_osmose(x=x[[i]], file=file, sep=sep, col.names=col.names, quote=quote, 
                                  row.names=row.names, append=append, ...)
    }

}

#' @author Ricardo Oliveros-Ramos
#' 
#' @export
#' @method write_osmose matrix 
write_osmose.matrix = write_osmose.default

#' @author Ricardo Oliveros-Ramos
#' 
#' @export
#' @method write_osmose osmose.initialization
write_osmose.osmose.initialization = function(x, file, sep=" = ", append=FALSE, ...) {
  
  write_osmose(x=x$par, file=file, sep=sep, col.names = FALSE, quote = FALSE, 
               row.names = TRUE, append=append)
  
}


