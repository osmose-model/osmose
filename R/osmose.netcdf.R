#' Returns the index of a species within the configuration.
#' 
#'
#' @param spec Name of the species
#' @param param Osmose configuration (osmose.config object)
#'
#' @return An integer containing the species index
find_species_index = function(spec, param){
  
  species = param$species$name
  index = sapply(names(species), gsub, pattern = "sp", replacement = "")
  out = index[species == spec]
  if(length(out) == 0) {
    stop(paste0("No index was found for species ", spec, "\n"))
  }
  
  return(out)
  
}

#' Computes the Osmose grid parameters by using the
#' LTL forcing file. The parameters are prompted in the console.
#' The user should copy the console output and paste it into
#' the Osmose configuration file.
#'
#' @param filename NetCDF filename
#' @param lonname  Longitude name (default "longitude")
#' @param latname  Latitude name (default "latitude")
#' @param outfile Output CSV file (if NULL, no file is written). The directory is created recursively
#' if it does not exist
extract_grid_param = function(filename, lonname = "longitude", latname = "latitude", 
                              outfile = NULL){
  
  #' Warning: This function must be checked in order to be sure that it's not just
  #' being created for an specific version of osmose-java (the return has names of
  #' parameters that are only valid for an specific version). Besides, it'd be 
  #' better to create a more global function that extract parameters, mask, etc 
  #' from LTL netCDF files ('extract_grid_mask' function).
  
  # opens the netcdf file
  fid = nc_open(filename = filename)
  
  # Get lon/lat dimensions
  lon = ncvar_get(nc = fid, varid = lonname)
  lat = ncvar_get(nc = fid, varid = latname)
  
  # close the netcdf
  nc_close(fid)
  
  # If the longitude is 1D, it is converted into 2D
  if(length(dim(lon)) == 1){
    message("Longitude and Latitudes are 1D.\nThey are be converted into 2D arrays.\n")
    
    # Get lengths of lon/lat
    nlon = length(lon)
    nlat = length(lat)
    
    # reconstructs a 2D longitude array
    temp = rep(lon, nlat)
    lon = array(data=temp, dim=c(nlon, nlat))
    
    # reconstructs a 2D latitude array
    temp = rep(lat, each=nlon)
    lat = array(data=temp, dim=c(nlon, nlat))
  }
  
  nlat = dim(lon)[2]
  nlon = dim(lon)[1]
  
  # computation of the mean dlat
  dlat = lat[, 2:nlat] - lat[, 1:nlat-1]
  dlat = mean(dlat)
  
  # computation of the mean dlon
  dlon = lon[2:nlon, ] - lon[1:nlon-1, ]
  dlon = mean(dlon)
  
  # computes the osmose related variables
  lowrightlon = max(lon) + dlon * 0.5
  lowrightlat = min(lat) - dlat * 0.5
  upperleftlon = min(lon) - dlon * 0.5
  upperleftlat = max(lat) + dlat * 0.5
  
  output = data.frame(value="fr.ird.osmose.grid.OriginalGrid", row.names="grid.java.classname", stringsAsFactors = F, check.rows=TRUE, check.names=TRUE)
  output = rbind(output, data.frame(row.names="grid.nlon", value=nlon))
  output = rbind(output, data.frame(row.names="grid.nlat", value=nlat))
  output = rbind(output, data.frame(row.names="grid.lowright.lat", value=lowrightlat))
  output = rbind(output, data.frame(row.names="grid.lowright.lon", value=lowrightlon))
  output = rbind(output, data.frame(row.names="grid.upleft.lat", value=upperleftlat))
  output = rbind(output, data.frame(row.names="grid.upleft.lon", value=upperleftlon))
  
  # Handling of the saving into CSV
  if(!is.null(outfile)) 
  {
    # creates the directory recursively
    if(!dir.exists(dirname(outfile))) {
      dir.create(dirname(outfile), recursive=TRUE)
    }
    
    write.table(x = output, file = outfile, sep = ";", col.names = FALSE, row.names = TRUE, quote = FALSE)
  }
  
  return(output)
  
}

#' Extracts the model grid file from the LTL forcing file.
#'
#' @param filename LTL forcing file name
#' @param varname  Name of the LTL biomass variable (default "ltl_biomass")
#' @param output_file Name of the output file (default "ltl-grid-mask.csv")
#' @param fillVal Fill Value (default NA but could be 0).
#' @param ... Additional arguments of the \code{ncvar_get} function. See Details. 
#' 
#' @details \code{...} can be used to allow more flexibility on the choice of 
#' the mask variable via the start and count arguments in order to get a final
#' 2D matrix.
extract_grid_mask = function(filename, varname = "ltl_biomass", 
                             output_file = "ltl-grid-mask.csv", fillVal = NA, ...){
  # Open nc file
  fid = nc_open(filename = filename)
  
  # Extracts the LTL variable
  var = drop(ncvar_get(nc = fid, varid = varname, ...))
  
  # Close nc file
  nc_close(fid)
  
  # Check dimensions of nc var
  if(length(dim(var)) < 2) stop("The number of dimensions of the input file must be greater than 2.")
  
  # Extract a matrix with the first two dimensions of var
  var <- matrix(data = as.numeric(var), nrow = nrow(var), ncol = ncol(var))
  
  # Define NA
  var[var == fillVal] = NA
  
  # extracts the mask array (lon, lat)
  var[!is.na(var)] = 0
  var[is.na(var)] = - 99.0
  
  # reverts the array from (lon, lat) into (lat, lon) with first latitudes at index 1
  var = aperm(var)
  
  # reverts array so that upper latitudes are at index 1, used for the saving of grid file
  var = apply(var, 2, rev)
  
  # writes the output in csv file.
  write.table(var, file=output_file, sep=";", row.names=FALSE, col.names=FALSE, quote=FALSE)
  
  return(var)
  
}

#' Converts the movement CSV settings into a set of NetCDF files 
#' (one per species)
#'
#' @param filename Name of the parameter file containing the movement parameters.
#'
#' make_movement_netcdf = function(filename){
#'   
#'   #' Warning: Optimize the code of this function
#'   
#'   param = readOsmoseConfiguration(filename)
#'   
#'   # Extract the list of species names
#'   # list of type ("sp0=names1, sp)
#'   species_name = param$species$name
#'   
#'   # Extract the maps names by considering that at least
#'   # species is defined for any of them.
#'   # list of type (map0=spMap0, map1=spMap1)
#'   tempspecies = param$movement$species
#'   
#'   ntime = get_var(param, "simulation", "time", "ndtperyear")
#'   nlat =  getOsmoseParameter(param, "grid", "nline")
#'   nlon =  getOsmoseParameter(param, "grid", "ncolumn")
#'   
#'   for (spec in species_name) {
#'     
#'     cat("+++++++++++++++++++++++ Processing ", spec, "\n")
#'     
#'     # Checks if the movement distribution for the current species is maps.
#'     # if not, skip the foloowing
#'     if(getOsmoseParameter(param, "movement", "distribution", "method", paste0("sp", find_species_index(spec, param))) != "maps") {
#'       cat("Movement for species ", spec, "is not maps.\n")
#'       cat("Nothing will be done\n")
#'       next;
#'     }
#'     
#'     N = 0 # count the number of maps associated with the current specie
#'     namesOk = c()
#'     # here, loop over all the names, i.e. map0, map1, etc.
#'     for (name in names(tempspecies))
#'     {
#'       mapspecies = getOsmoseParameter(param, "movement", "species", name)
#'       # process only the maps that correspond to the propoer species
#'       if(mapspecies == spec) {
#'         N = N + 1
#'         namesOk = c(namesOk, name)
#'       }
#'     }
#'     
#'     if(N == 0) {
#'       cat("There is no map extracted for species ", spec, "\n")
#'       cat("Nothing will be done\n")
#'       next
#'     }
#'     
#'     # init arrays with the good dimensions (to get rid off abind)
#'     # note that to save data in NC, transposed dimensions must be used.
#'     season = array(0, dim=c(ntime, N))
#'     ymin = array(0, dim=c(N))
#'     ymax = array(0, dim=c(N))
#'     agemin = array(0, dim=c(N))
#'     agemax = array(0, dim=c(N))
#'     map = array(0, dim=c(nlon, nlat, N))
#'     
#'     i = 1
#'     # here, loop over all the map names, i.e. map0, map1, etc,
#'     # that are linked with the specific spacies
#'     for (name in namesOk) {
#'       
#'       mapspecies = getOsmoseParameter(param, "movement", "species", name)
#'       
#'       cat("Processing map ", name, "\n")
#'       
#'       if(existOsmoseParameter(param, "movement", "year", "min", name)) {
#'         ymin[i] = getOsmoseParameter(param, "movement", "year", "min", name)
#'       } else {
#'         ymin[i] = -1
#'       }
#'       
#'       if(existOsmoseParameter(param, "movement", "year", "max", name)) {
#'         ymax[i] = getOsmoseParameter(param, "movement", "year", "max", name)
#'       } else {
#'         ymax[i] = -1
#'       }
#'       
#'       if(existOsmoseParameter(param, "movement", "age", "max", name)) {
#'         agemax[i] = getOsmoseParameter(param, "movement", "age", "max", name)
#'       } else {
#'         agemax[i] = -1
#'       }
#'       
#'       if(existOsmoseParameter(param, "movement", "age", "min", name)) {
#'         agemin[i] = getOsmoseParameter(param, "movement", "age", "min", name)
#'       } else {
#'         agemin[i] = -1
#'       }
#'       
#'       if(existOsmoseParameter(param, "movement", "season", name)) {
#'         tempseas = getOsmoseParameter(param, "movement", "season", name) + 1
#'         season[tempseas, i] = 1
#'       }
#'       
#'       # If the map parameter exists, it is read and converted into matrix.
#'       # if null, set to 0 everywhere
#'       if(existOsmoseParameter(param, "movement", "file", name)) {
#'         mapFile = file.path(dirname(filename), getOsmoseParameter(param, "movement", "file", name))
#'         temp = t(data.matrix(read.table(mapFile, sep=";")))  # (lat, lon) converted into (lon, lat)
#'         # reverts array so that upper latitudes are at index 1, used for the saving of grid file
#'         temp = temp[, ncol(temp):1]
#'         map[,,i] = temp
#'         
#'       } else {
#'         map[,,i] = 0
#'       }
#'       
#'       i = i + 1
#'       
#'     }
#'     
#'     x = ncdim_def("x", "", vals=1:nlon)
#'     y = ncdim_def("y", "",  vals=1:nlat)
#'     m = ncdim_def("m", "",  vals=1:N)
#'     t = ncdim_def("t", "",  vals=1:ntime)
#'     
#'     varAgemin = ncvar_def("agemin", units="",  dim=m, prec="integer")
#'     varAgemax = ncvar_def( "agemax",  units="", prec="integer", dim=m)
#'     varYearmin = ncvar_def("yearmin",  units="", prec="integer", dim=m)
#'     varYearmax = ncvar_def("yearmax",  units="", prec="integer", dim=m)
#'     varSeason = ncvar_def("season",  units="", prec="byte", dim=list(t, m))
#'     varMap = ncvar_def("map",  units="", prec="float", dim=list(x, y, m))
#'     
#'     map[map<0] = NA
#'     
#'     varlist = list(varAgemin, varAgemax, varYearmin, varYearmax, varSeason, varMap)
#'     
#'     ncfilename = paste0("movement_", spec, ".nc")
#'     ncid = nc_create(ncfilename, vars=varlist)
#'     
#'     ncvar_put(ncid, "agemin", agemin)
#'     ncvar_put(ncid, "agemax", agemax)
#'     ncvar_put(ncid, "yearmax", ymax)
#'     ncvar_put(ncid, "yearmin", ymin)
#'     ncvar_put(ncid, "map", map)
#'     ncvar_put(ncid, "season", season)
#'     
#'     nc_close(ncid)
#'     
#'   }
#' }
