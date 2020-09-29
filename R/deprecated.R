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


# to keep back compatibility for a while
#' @rdname read_osmose
#' @export
osmose2R = function(path = NULL, version = "v3r2", species.names = NULL, ...) {
  
  .Deprecated("read_osmose")
  read_osmose(path=path, version=version, species.names=species.names, ...)
  
}

#' @rdname get_var
#' @export
getVar = function(object, what, how, ...) {
  
  .Deprecated("get_var")
  get_var(object=object, what=what, how=how, ...)
  
}

#' @rdname run_osmose
#' @export
runOsmose = function(input, parameters=NULL, output="output", log="osmose.log",
                     version="4.1.0", osmose=NULL, java="java", 
                     options=NULL, verbose=TRUE, clean=TRUE) {
  
  .Deprecated("run_osmose")
  
  run_osmose(input = input, parameters = parameters, output = output,
             log = log, version = version, osmose = osmose, java = java, 
             options = options, verbose = verbose, clean = clean) 
}

#' @rdname write_osmose
#' @export
write.osmose = function(x, file)   {
  .Deprecated("write_osmose")
  write.table(x=x, file=file, sep=",", col.names=NA, quote=FALSE)
}

