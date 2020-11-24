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



#' Update OSMOSE configuration
#'
#' @param input path to the main configuration file
#' @param log File to save OSMOSE execution messages.
#' @param version target OSMOSE version for the update.
#' @param osmose Path to a OSMOSE .jar executable. By default (NULL), uses the stable 
#' jar for the current version.
#' @param java Path to the java executable. The default assumes 'java' is 
#' on the search path. 
#' @param verbose Show messages? (output in the log file if FALSE).
#' @param absolute Whether the input file uses absolute (\code{TRUE}, default) or relative (\code{FALSE}) paths.
#'
#' @export
update_osmose = function(input, log = "osmose.log",
                         version = "4.3.2", osmose = NULL, java = "java",
                         verbose = TRUE, absolute = TRUE) {
  
  if(isTRUE(verbose)) message(sprintf("This is OSMOSE version %s", version))
  
  # update to provide by release executables
  if(is.null(osmose)){
    osmose_name = sprintf("osmose_%s.jar", version)
    lib = cacheManager("lib")
    osmose = shQuote(cacheManager(osmose_name))
  }
  
  args = paste("-jar", osmose, "-update", input)
  
  stdout = ifelse(interactive() & verbose, "", log)
  stderr = ifelse(interactive() & verbose, "", log)
  
  command = paste(c(shQuote(java), args), collapse = " ")
  
  if(isTRUE(verbose)) message(sprintf("Running: %s", command))
 
  # update LTL ncdf if ltl.netcdf.file is found in input file (if version 4)
  if(.compareVersion(version, "4") >= 0) {
    update_ltl(input = input, absolute=absolute)
  }
  # update configuration file
  system2(java, args=args, stdout=stdout, stderr=stderr, wait=TRUE)
  
  return(invisible(command))
  
}


#' Update LTL ncdf files to version 4 format
#'
#' @param filename Filename of the NetCDF LTL file
#' @param input Filename of the main configuration file
#' @param absolute Whether the input file uses absolute (TRUE) or relative (FALSE) paths.
#'
update_ltl = function(input, filename=NULL, absolute=TRUE) {
  
  # get path from config
  
  # Reads the CSV parameter files
  param = readOsmoseConfiguration(input, absolute=absolute)
  if(is.null(filename)) {
    filename = param$ltl$netcdf$file
    if(is.null(filename)) {
      warning("LTL filename not found in configuration file, nothing to do.")
      return(invisible())
    }
  }
  
  # recovers the index for plk as 
  pltindex = names(param$plankton$name)
  
  # create back up file
  isOK = FALSE
  bck = sprintf("%s.backup%s", filename, 
                suppressWarnings(format(Sys.time(), "%Y%m%d%H%M", tz="")))
  tmp = sprintf("%s.temp", filename)
  file.copy(from=filename, to=bck)
  on.exit(if(!isOK) file.remove(bck))
  
  # Opens the old netcdf file
  ncin = nc_open(filename)
  if(length(ncin$var)!=1) {
    warning("More than one variable in ncdf file, skipping LTL update.")
    return(invisible())
  }
  
  if(ncin$ndims!=4) {
    warning("Wrong number of dimensions in ncdf file, skipping LTL update.")
    return(invisible())
  }
  
  varlon  = names(ncin$dim)[1]
  varlat  = names(ncin$dim)[2]
  vartime = names(ncin$dim)[4]
  
  # Recover spatial coordinates
  if(length(ncin$var[[varlon]]$varsize) == 2) {
    # if coordinates are 2D, then extract data as 1D
    lon = ncvar_get(ncin, varid=varlon)[,1]
    lat = ncvar_get(ncin, varid=varlat)[1,]
  } else {
    # If longitudes are 1D, nothing to do.
    lon = ncvar_get(ncin, varid=varlon)
    lat = ncvar_get(ncin, varid=varlat)
  }
  
  # Recovers the original time coordinates and attributes (for units)
  if(is.null(ncin$var[[vartime]])) {
    attr_time = ncatt_get(ncin, vartime)
    time = ncin$dim[[vartime]]$vals
  } else {
    attr_time = ncatt_get(ncin, vartime)
    time = ncvar_get(ncin, varid=vartime)
  }
  
  # recovers the number of plankton within the file
  n_ltl_file = ncin$dim$ltl$len
  
  # biomass is of size (lon, lat, ltl, time)
  biomass_units = ncatt_get(ncin, "ltl_biomass")$units
  biomass = ncvar_get(ncin, varid='ltl_biomass')
  
  # Creates the output dimensions (lon, lat, time)
  dim_time = ncdim_def("time", attr_time$units, time)
  dim_lon = ncdim_def("longitude", "", lon)
  dim_lat = ncdim_def("latitude", "", lat)
  dims = list(dim_lon=dim_lon, dim_lat=dim_lat, dim_time=dim_time)
  
  # Loop over all the plankton classes to initialise variables in the NetCDF
  list_vars = c()
  for(i in 1:n_ltl_file) {
    ltl_var = param$plankton$name[[pltindex[i]]]
    var_nc = ncvar_def(ltl_var, biomass_units, dims, longname=ltl_var)
    list_vars[[i]] = var_nc
  }
  
  # Opens the output NetCDF file
  ncout = nc_create(tmp, list_vars)
  
  # loops over all the LTL classes and write data into 
  # the file
  for(i in 1:n_ltl_file) {
    ltl_var = param$plankton$name[[pltindex[i]]]
    ncvar_put(ncout, ltl_var, biomass[, , i, ])
  }
  
  # Add the calendar attribute to the time variable
  if(!is.null(attr_time$calendar)) {
    ncatt_put(ncout, "time", "calendar", attr_time$calendar)
  }
  
  nc_close(ncout)
  nc_close(ncin)
  
  on.exit(if(file.exists(tmp)) file.remove(tmp), add = TRUE)
  
  isOK = file.remove(filename)
  if(!isOK) stop("Updated file could not be written, permission denied.")
  isOK = file.rename(tmp, filename)
  if(!isOK) {
    file.copy(from=bck, to=filename)
    stop("Updated file could not be written, permission denied.")
  }
  
  msg = sprintf("LTL file successfully updated, original file saved as %s.", bck)
  message(msg) 
  
  return(invisible())
  
}
