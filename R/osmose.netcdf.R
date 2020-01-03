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
  
  output = data.frame(value="fr.ird.osmose.grid.OriginalGrid", row.names="grid.java.classname", stringsAsFactors = F, check.rows=TRUE, check.names=TRUE)
  output = rbind(output, data.frame(row.names="grid.nlon", value=nlon))
  output = rbind(output, data.frame(row.names="grid.nlat", value=nlat))
  output = rbind(output, data.frame(row.names="grid.lowright.lat", value=lowrightlat))
  output = rbind(output, data.frame(row.names="grid.lowright.lon", value=lowrightlon))
  output = rbind(output, data.frame(row.names="grid.upleft.lat", value=upperleftlat))
  output = rbind(output, data.frame(row.names="grid.upleft.lon", value=upperleftlon))
  
  # Handling of the saving into CSV
  if(!is.null(csv)) 
  {
    # creates the directory recursively
    if(!dir.exists(dirname(csv))) {
      dir.create(dirname(csv), recursive=TRUE)
    }
    write.table(output, csv, sep=";", col.names = FALSE, row.names = TRUE, quote=FALSE)
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
  write.table(var, file=output_file, sep=";", row.names=FALSE, col.names=FALSE, quote=FALSE)
  
  return(var)
  
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
#' @param varlon Name of the longitude variable
#' @param varlat Name of the latitude variable
#' @param vartime Name of the time variable
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

convert_access_to_netcdf = function(config_file, output_file="access.nc") {
  
  params = readOsmoseConfiguration(config_file)
  
  #predationstage = getOsmoseParameter(params, "predation", "accessibility", "stage", keep.att=TRUE)
  # reading the predation accessibility file
  #predationfile = getOsmoseParameter(params, "predation", "accessibility", "file", keep.att=TRUE)
  #path = attributes(predationfile)$path
  #access = read.csv(file.path(path, predationfile))
  
  access = getVar(params, what="predation", sep=',')$accessibility
  data = access$data
  
  nprey = dim(data)[1]
  npred = dim(data)[2]
  
  pattern = "([A-Za-z_]+) < ([0-9]+)"
  pattern1 = "([A-Za-z_]+).*"
  preynames = rownames(data)
  
  upper_bounds_prey = 1:nprey
  names_prey = 1:nprey
  upper_bounds_pred = 1:npred
  names_pred = 1:npred
  
  cpt = 1
  for (v in preynames) {
    if(grepl(v, pattern=pattern)) { 
      # if class row contains a < string, recover the class value
      name = sub(x=v, pattern=pattern, replacement="\\1")
      class = sub(x=v, pattern=pattern, replacement="\\2")
      upper_bounds_prey[cpt] = class
    }
    
    else {
      # if not, assumes that upper bounds is infinity
      name = sub(x=v, pattern=pattern1, replacement="\\1")
      upper_bounds_prey[cpt] = .Machine$double.xmax
    }
    
    # updates the names of preys.
    names_prey[cpt] = name
    cpt = cpt + 1
    
  }
  
  # data is of format prey(line)/predator(column)
  # but R will transpose at writting.
  # hence transposing is forced here
  data = as.matrix(t(data)) # predator/prey
  
  nchars = .get_max_nchars(names_prey)
  
  # create dimensions
  preyDim = ncdim_def("prey", "", 1:nprey, create_dimvar=FALSE)
  predDim = ncdim_def("pred", "", 1:npred, create_dimvar=FALSE)
  timeDim = ncdim_def("time", "", 1, unlim=TRUE)
  charDim = ncdim_def("nchar", "", 1:nchars, create_dimvar=FALSE)
  
  # create variables
  var_access = ncvar_def("accessibility", "%", list(predDim, preyDim, timeDim), -99.99, "accessibility", prec="double")
  
  # defines the string variables (nchars, nprey/npred)
  var_names_prey = ncvar_def("preySpeciesName", "", list(charDim, preyDim), prec="char")
  var_names_pred = ncvar_def("predSpeciesName", "", list(charDim, predDim), prec="char")
  # creates the class variables 
  var_class_prey = ncvar_def("preyClass", "", list(preyDim), prec="double")
  var_class_pred = ncvar_def("predClass", "", list(predDim), prec="double")
  
  varlist = list(var_access, var_names_prey, var_names_pred, var_class_prey, var_class_pred)
  
  # create the file
  nc = nc_create(output_file, varlist, force_v4=TRUE, verbose=FALSE)
  
  # add the class type attribute
  ncatt_put(nc, var_access, "class_type", access$stageStructure)
  ncvar_put(nc, var_access, data)
  
  # writes variables for prey
  ncvar_put(nc, var_class_prey, upper_bounds_prey, verbose=FALSE)
  ncvar_put(nc, var_names_prey, names_prey, verbose=TRUE)
  
  # writes variables for predators (subspan of preys)
  ncvar_put(nc, var_names_pred, names_prey[1:npred], verbose=FALSE)
  ncvar_put(nc, var_class_pred, upper_bounds_prey[1:npred], verbose=FALSE)
  
  nc_close(nc)
  
}

.get_max_nchars = function(names_prey) {
  
  # Recovers the maximum length of names
  nchars = max(sapply(names_prey, nchar))
  
  # pad prey names + convert into string
  #names_prey = sapply(names_prey, str_pad, nchars, side="right", pad=" ")
  #names_prey = sapply(names_prey, str_split, "")
  # convert inyo array
  #names_prey_array = as.data.frame(names_prey)  # nchars, npreys
  #names_prey_array = as.matrix(names_prey_array)
  
  return(nchars)
  
}

.convert_predPrey_netcdf = function(filename, output_file='predPrey.nc') {

  config = readOsmoseConfiguration(filename)
  
  # Extract the predation params
  predation = getVar(config, what="predation")
  predPrey = predation$predPrey
  
  # recover the species params
  species = getVar(config, what="species")
  nspecies = length(species)
  species_index = c()
  for(i in 0:(nspecies-1)) {
    species_index = c(species_index, paste0("sp", i))
  }
  
  # loop over all the species to recover the thresholds 
  final_species = c()
  final_thres = c()
  final_min = c()
  final_max = c()
  for (sp in species_index) {
    thres = predPrey$stageThreshold[[sp]]
    if(any(is.na(thres))) {
      final_species = c(final_species, species$names[[sp]])
      final_thres = c(final_thres, .Machine$double.xmax)
      final_min = c(final_max, predPrey$sizeRatioMin[[sp]])
      final_max = c(final_max, predPrey$sizeRatioMax[[sp]])
    } else {
      thres = c(thres, .Machine$double.xmax)
      nclass = length(thres)
      final_species = c(final_species, rep(c(species$names[[sp]]), times=nclass))
      final_thres = c(final_thres, thres)
      final_min = c(final_max, predPrey$sizeRatioMin[[sp]])
      final_max = c(final_max, predPrey$sizeRatioMax[[sp]])
    }
  }
  
  npred = length(final_species)
  
  nchars = .get_max_nchars(final_species)
  
  # # create dimensions
  predDim = ncdim_def("species", "", 1:npred, create_dimvar=TRUE)
  timeDim = ncdim_def("time", "", 1, unlim=TRUE)
  charDim = ncdim_def("nchar", "", 1:nchars, create_dimvar=FALSE)
  
  # create variables
  var_ratiomin = ncvar_def("sizeRatioMin", "", list(predDim, timeDim), -99.99, "sizeRatioMin", prec="double")
  var_ratiomax = ncvar_def("sizeRatioMax", "", list(predDim, timeDim), -99.99, "sizeRatioMax", prec="double")
  
  # defines the string variables (nchars, nprey/npred)
  var_names_species= ncvar_def("speciesname", "", list(charDim, predDim), prec="char")
  
  # creates the class variables
  var_class_species = ncvar_def("SpeciesClass", "", list(predDim), prec="double")
  
  # concatenates the list of variables
  varlist = list(var_names_species, var_ratiomin, var_ratiomax, var_class_species)
  
  # # create the file
  nc = nc_create(output_file, varlist)
  
  # add the class type attribute
  ncatt_put(nc, var_ratiomax, "class_type", predPrey$stageStructure)
  ncatt_put(nc, var_ratiomin, "class_type", predPrey$stageStructure)
  
  # adds the ratio variables
  ncvar_put(nc, var_ratiomin, final_min)
  ncvar_put(nc, var_ratiomax, final_max)
  
  # writes variables for prey
  ncvar_put(nc, var_class_species, final_thres)
  ncvar_put(nc, var_names_species, final_species)
  
  nc_close(nc)
  
}



#' Creates a NetCDF file containing the land-sea mask extracted
#' from the resource file
#'
#' @param filename Name of the NetCDF resource file.
#' @param varname  Name of the variable used to extract the mask
#' @param lonname Name of the longitude variable
#' @param latname  Name of the latitude variable.
#' @param output_file  Name of the output file.
#' @param ...  Additional arguments
#'
#' @return None
#' @export
create_netcdf_mask = function(filename, varname, lonname, latname, output_file="grid-mask.nc", ...) {
  
  # opens the netcdf file
  fid = nc_open(filename)
  
  # extracts the LTL variable
  var = drop(ncvar_get(fid, varname, ...))
  
  # extracts the grid coordinates
  lon = drop(ncvar_get(fid, lonname, ...))
  lat = drop(ncvar_get(fid, latname, ...))

  nc_close(fid)
  
  # counts the number of dimensions
  ndims = length(dim(var))
  
  if(ndims == 4) {
    # sum arrays along the time and ltl dimensions
    var = apply(var, c(1, 2), sum)
  } else if(ndims == 3) {
    # sum arrays along the time dimension
    var = apply(var, c(1, 2), sum)
  } else if (ndims == 2) {
    var = var
  } else {
    stop("The number of dimensions of the input file must be 2, 3 or 4")
  }
  
  nlon = dim(var)[1]
  nlat = dim(var)[2]
  
  # if the lon/lat arrays are 1D, convert them into 2d
  if(length(dim(lon)) == 1) {
    lon = array(rep(lon, times=nlat), dim=c(nlon, nlat))
    lat = array(rep(lat, each=nlon), dim=c(nlon, nlat))
  }
  
  # converts NA and 0 to land (0)
  # else, to water (1)
  var[is.na(var)] = 0
  var[var == 0] = 0
  var[var != 0] = 1

  # create dimensions
  lonDim = ncdim_def("x", "", 1:nlon, create_dimvar=FALSE)
  latDim = ncdim_def("y", "", 1:nlat, create_dimvar=FALSE)
  
  # create variables
  var_lon = ncvar_def("lon", "", list(lonDim, latDim), -99.99, "lon", prec="float")
  var_lat = ncvar_def("lat", "", list(lonDim, latDim), -99.99, "lat", prec="float")
  var_mask = ncvar_def("mask", "", list(lonDim, latDim), -99.99, "mask", prec="byte")
  
  # concatenates the list of variables
  varlist = list(var_lon, var_lat, var_mask)
  
  # create the file
  nc = nc_create(output_file, varlist)
  
  # writes variables for prey
  ncvar_put(nc, var_lon, lon)
  ncvar_put(nc, var_lat, lat)
  ncvar_put(nc, var_mask, var)
  
  nc_close(nc)

}


