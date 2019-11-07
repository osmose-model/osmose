

#' Update LTL ncdf files to version 4 format
#'
#' @param filename Filename of the NetCDF LTL file
#' @param input Filename of the main configuration file
#' @param absolute Whether the input file uses absolute (TRUE) or relative (FALSE) paths.
#'
#' @export
update_ltl = function(filename, input, absolute=TRUE) {
  
  # check in right format
  # get path from config
  
  # Reads the CSV parameter files
  param = readOsmoseConfiguration(input, absolute=absolute)
  
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


