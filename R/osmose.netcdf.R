#' Returns the index of a species within the configuration.
#' 
#'
#' @param spec Name of the species
#' @param param Osmose configuration (osmose.config object)
#'
#' @return An integer containing the species index
#' @export
#'
find_species_index = function(spec, param)
{
  
  species = param$species$name
  .subSep = function(x) as.integer(gsub("sp", "", x))
  index = sapply(names(species), .subSep, USE.NAMES=FALSE)
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
#' @param csv Output CSV file (if NULL, no file is written). The directory is created recursively
#' if it does not exist
#'
#' @return The grid format as a data frame.
#' @export
#'
#' @usage extract_grid_param("ltlbiomass.nc")
extract_grid_param = function(filename, lonname="longitude", latname="latitude", csv=NULL)
{
  
  # checks for the file existence
  if(!file.exists(filename))
  {
    stop("The ", filename, " file does not exist", sep="")
  }
  
  # opens the netcdf file
  fid = nc_open(filename)
  
  lon = ncvar_get(fid, lonname)
  lat = ncvar_get(fid, latname)
  
  # close the netcdf
  nc_close(fid)
  
  # If the longitude is 1D, it is converted into 2D
  if(length(dim(lon)) == 1)
  {
    cat("Longitude and Latitudes are 1D.\n")
    cat("They are be converted into 2D arrays.\n")
    
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
  
  output = data.frame(par="grid.java.classname", value="fr.ird.osmose.grid.OriginalGrid", stringsAsFactors = F)
  output = rbind(output, c("grid.nlon", nlon))
  output = rbind(output, c("grid.nlat", nlat))
  output = rbind(output, c("grid.lowright.lat", lowrightlat))
  output = rbind(output, c("grid.lowright.lon", lowrightlon))
  output = rbind(output, c("grid.upperleft.lat", upperleftlat))
  output = rbind(output, c("grid.upperleft.lon", upperleftlon))
  
  # Handling of the saving into CSV
  if(!is.null(csv)) 
  {
    # creates the directory recursively
    if(!dir.exists(dirname(csv))) {
      dir.create(dirname(csv), recursive=TRUE)
    }
    write.table(output, csv, sep=";", col.names = F, row.names = F)
  }
  
  return(output)
  
}

#' Extracts the model grid file from the LTL forcing file.
#'
#' @param filename LTL forcing file name
#' @param varname  Name of the LTL biomass variable (default "ltl_biomass")
#' @param output_file Name of the output file (default "ltl-grid-mask.csv")
#' @param fillVal Fill Value (default NA but could be 0).
#' @param ... Additional arguments of the ncvar_get function. Used to allow more flexibility on the choice of the
#' mask variable via the start and count arguments. If your mask variable is already 2D, nothing to do. If it is 3D, you
#' need to set the start/count arrays so that the final variable is of dimensions (lon, lat).
#' @export
extract_grid_mask = function(filename, varname="ltl_biomass", output_file="ltl-grid-mask.csv", fillVal=NA, ...)
{
  
  # opens the netcdf file
  fid = nc_open(filename)
  
  # extracts the LTL variable
  var = drop(ncvar_get(fid, varname, ...))
  
  nc_close(fid)

  ndims = length(dim(var))
  
  if(ndims == 4) {
      var = var[, , 1, 1]
  } else if(ndims == 3) {
    var = var[, , 1]
  } else if (ndims == 2) {
    var = var
  } else {
      stop("The number of dimensions of the input file must be 2, 3 or 4")
  }

  nlon = dim(var)[1]
  nlat = dim(var)[2]
  
  # if the fillVal is not NA.
  # we set to NA all the values that are equal
  # to NA
  if(!is.na(fillVal))
  {
    var[var == fillVal] = NA
  }
  
  # extracts the mask array (lon, lat)
  var[!is.na(var)] = 0
  var[is.na(var)] = - 99.0
  
  # reverts the array from (lon, lat) into (lat, lon) with first latitudes at index 1
  var = aperm(var)
  
  # reverts array so that upper latitudes are at index 1, used for the saving of grid file
  var = apply(var, 2, rev)
  
  # writes the output in csv file.
  write.table(var, file=output_file, sep=";", row.names=FALSE, col.names=FALSE)
  
}

#' Converts the movement CSV settings into a set of NetCDF files 
#' (one per species)
#'
#' @param filename Name of the parameter file containing the movement parameters.
#'
#' @export
make_movement_netcdf = function(filename) {
  
  param = readOsmoseConfiguration(filename)
  
  # Extract the list of species names
  # list of type ("sp0=names1, sp)
  species_name = param$species$name
  
  # Extract the maps names by considering that at least
  # species is defined for any of them.
  # list of type (map0=spMap0, map1=spMap1)
  tempspecies = param$movement$species
  
  ntime = getOsmoseParameter(param, "simulation", "time", "ndtperyear")
  nlat =  getOsmoseParameter(param, "grid", "nline")
  nlon =  getOsmoseParameter(param, "grid", "ncolumn")
  
  for (spec in species_name) {
    
    cat("+++++++++++++++++++++++ Processing ", spec, "\n")
    
    # Checks if the movement distribution for the current species is maps.
    # if not, skip the foloowing
    if(getOsmoseParameter(param, "movement", "distribution", "method", paste0("sp", find_species_index(spec, param))) != "maps") {
      cat("Movement for species ", spec, "is not maps.\n")
      cat("Nothing will be done\n")
      next;
    }
    
    N = 0 # count the number of maps associated with the current specie
    namesOk = c()
    # here, loop over all the names, i.e. map0, map1, etc.
    for (name in names(tempspecies))
    {
      mapspecies = getOsmoseParameter(param, "movement", "species", name)
      # process only the maps that correspond to the propoer species
      if(mapspecies == spec) {
        N = N + 1
        namesOk = c(namesOk, name)
      }
    }
    
    if(N == 0) {
      cat("There is no map extracted for species ", spec, "\n")
      cat("Nothing will be done\n")
      next
    }
    
    # init arrays with the good dimensions (to get rid off abind)
    # note that to save data in NC, transposed dimensions must be used.
    season = array(0, dim=c(ntime, N))
    ymin = array(0, dim=c(N))
    ymax = array(0, dim=c(N))
    agemin = array(0, dim=c(N))
    agemax = array(0, dim=c(N))
    map = array(0, dim=c(nlon, nlat, N))
    
    i = 1
    # here, loop over all the map names, i.e. map0, map1, etc,
    # that are linked with the specific spacies
    for (name in namesOk) {
      
      mapspecies = getOsmoseParameter(param, "movement", "species", name)
      
      cat("Processing map ", name, "\n")
      
      if(existOsmoseParameter(param, "movement", "year", "min", name)) {
        ymin[i] = getOsmoseParameter(param, "movement", "year", "min", name)
      } else {
        ymin[i] = -1
      }
      
      if(existOsmoseParameter(param, "movement", "year", "max", name)) {
        ymax[i] = getOsmoseParameter(param, "movement", "year", "max", name)
      } else {
        ymax[i] = -1
      }
      
      if(existOsmoseParameter(param, "movement", "age", "max", name)) {
        agemax[i] = getOsmoseParameter(param, "movement", "age", "max", name)
      } else {
        agemax[i] = -1
      }
      
      if(existOsmoseParameter(param, "movement", "age", "min", name)) {
        agemin[i] = getOsmoseParameter(param, "movement", "age", "min", name)
      } else {
        agemin[i] = -1
      }
      
      if(existOsmoseParameter(param, "movement", "season", name)) {
        tempseas = getOsmoseParameter(param, "movement", "season", name) + 1
        season[tempseas, i] = 1
      }
      
      # If the map parameter exists, it is read and converted into matrix.
      # if null, set to 0 everywhere
      if(existOsmoseParameter(param, "movement", "file", name)) {
        mapFile = file.path(dirname(filename), getOsmoseParameter(param, "movement", "file", name))
        temp = t(data.matrix(read.table(mapFile, sep=";")))  # (lat, lon) converted into (lon, lat)
        # reverts array so that upper latitudes are at index 1, used for the saving of grid file
        temp = temp[, ncol(temp):1]
        map[,,i] = temp
        
      } else {
        map[,,i] = 0
      }
      
      i = i + 1
      
    }
    
    x = ncdim_def("x", "", vals=1:nlon)
    y = ncdim_def("y", "",  vals=1:nlat)
    m = ncdim_def("m", "",  vals=1:N)
    t = ncdim_def("t", "",  vals=1:ntime)
    
    varAgemin = ncvar_def("agemin", units="",  dim=m, prec="integer")
    varAgemax = ncvar_def( "agemax",  units="", prec="integer", dim=m)
    varYearmin = ncvar_def("yearmin",  units="", prec="integer", dim=m)
    varYearmax = ncvar_def("yearmax",  units="", prec="integer", dim=m)
    varSeason = ncvar_def("season",  units="", prec="byte", dim=list(t, m))
    varMap = ncvar_def("map",  units="", prec="float", dim=list(x, y, m))
    
    map[map<0] = NA
    
    varlist = list(varAgemin, varAgemax, varYearmin, varYearmax, varSeason, varMap)
    
    ncfilename = paste0("movement_", spec, ".nc")
    ncid = nc_create(ncfilename, vars=varlist)
    
    ncvar_put(ncid, "agemin", agemin)
    ncvar_put(ncid, "agemax", agemax)
    ncvar_put(ncid, "yearmax", ymax)
    ncvar_put(ncid, "yearmin", ymin)
    ncvar_put(ncid, "map", map)
    ncvar_put(ncid, "season", season)
    
    nc_close(ncid)
    
  }
}

#' Creates a NetCDF file containing the new format for LTL
#' concentrations. The "ltl_biomass" of dims (ntime, nltl, nlat, nlon)
#' is splitted into nltl variables of dims (ntime, nlat, nlon)
#'
#' @param ltl_filename Name of the NetCDF LTL file
#' @param osmose_config Name of the main configuration file
#' @param absolute Whether the path is absolute (TRUE) or relative (FALSE)
#'
#' @export
correct_ltl_file = function(ltl_filename, osmose_config, varlon="longitude", varlat="latitude", vartime='time', absolute=TRUE)
{
  
  #absolute=FALSE
  #dirname = "/home/nbarrier/Modeles/osmose/test_configuration_bgspecies/"
  #ltl_filename = file.path(dirname, "eec_ltlbiomassTons.nc")
  #osmose_config = file.path(dirname, "eec_all-parameters.csv")
  
  # Reads the CSV parameter files
  param = readOsmoseConfiguration(osmose_config, absolute=absolute)
  
  # recovers the index for plk as 
  pltindex = names(param$plankton$name)
  
  # Opens the old netcdf file
  ncin = nc_open(ltl_filename)
  
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
    print(attr_time)
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
  output_file_name = file.path(dirname(ltl_filename), paste0("corrected_", basename(ltl_filename)))
  output_file_name = basename(output_file_name)
  ncout = nc_create(output_file_name, list_vars)
  
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
  
}

