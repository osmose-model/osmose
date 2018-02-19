
#require("RNetCDF")

# Extract the names of the netcdf variable
#
# @param fid Object of class "NetCDF" which points to the NetCDF dataset.
#
# @return A list of strings
.extract_varnames = function(fid)
{
  
  # extracts the number of variables
  inc = file.inq.nc(fid)
  nvars = inc$nvars
  
  # init the output variable
  varnames = c()
  
  # loop over all the variables indexes 
  # (start from 0 and not from 1)
  for(i in 0:(nvars-1))
  {
    # extracts the varname
    temp = var.inq.nc(fid, i)  
    varnames = c(varnames, temp$name)
  }
  
  return(varnames)
  
}

# Extracts a NetCDF variable, which is returned as a vector or an array.
#
# @param fid Object of class "NetCDF" which points to the NetCDF dataset.
# @param var Variable name
#
# @return True if the variable exists.
.extract_var = function(fid, varname)
{
  
  if (!.check_varexists(fid, varname))
  { 
    stop("The ", varname, " variable does not exist")
  }
  
  # if the dim array in netcdf is (ntime, n)
  var = var.get.nc(fid, varname)
  
  return(var)
  
}

#' Checks if a variable exists in a NetCDF
#'
#' @param fid Object of class "NetCDF" which points to the NetCDF dataset.
#' @param var Variable name
#'
#' @return True if the variable exists.
.check_varexists = function(fid, var)
{
  
  varnames = .extract_varnames(fid)
 
  return(var %in% varnames)
  
}


#' Computes the Osmose grid parameters by using the
#' LTL forcing file. The parameters are prompted in the console.
#' The user should copy the console output and paste it into
#' the Osmose configuration file.
#'
#' @param filename NetCDF filename
#' @param lonname  Longitude name (default "longitude")
#' @param latname  Latitude name (default "latitude")
#'
#' @return Nothing
#' @export
#'
#' @usage extract_grid_param("ltlbiomass.nc")
osmose.nc.extract_grid_param = function(filename, lonname="longitude", latname="latitude")
{

  # checks for the file existence
  if(!file.exists(filename))
  {
  stop("The ", filename, " file does not exist", sep="")
  }
  
  # opens the netcdf file
  fid = open.nc(filename)
  
  # recovers the lon and lat values
  lon = .extract_var(fid, lonname)
  lat = .extract_var(fid, latname)

  # close the netcdf
  close.nc(fid)
  
  # If the longitude is 1D, it is converted into 2D
  if(is.vector(lon))
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
  
  cat("grid.java.classname;fr.ird.osmose.grid.OriginalGrid\n")
  cat("grid.ncolumn;", nlon, "\n")
  cat("grid.nline;", nlat, "\n")
  cat("grid.lowright.lat;", lowrightlat, "\n")
  cat("grid.lowright.lon;", lowrightlon, "\n")
  cat("grid.upperleft.lat;", upperleftlat, "\n")
  cat("grid.upperleft.lon;", upperleftlon, "\n")

}

#' Extracts the model grid file from the LTL forcing file.
#'
#' @param filename LTL forcing file name
#' @param varname  Name of the LTL biomass variable (default "ltl_biomass")
#' @param output_file Name of the output file (default "ltl-grid-mask.csv")
#' @param fillVal Fill Value (default NA but could be 0).
#'
#' @export
osmose.nc.extract_grid_mask = function(filename, varname="ltl_biomass", output_file="ltl-grid-mask.csv", fillVal=NA)
{
 
  # checks for the file existence
  if(!file.exists(filename))
  {
    stop("The ", filename, " file does not exist", sep="")
  }
  
  # opens the netcdf file
  fid = open.nc(filename)
  
  # extracts the LTL variable
  var = .extract_var(fid, varname)
 
  # checks that the number of dimensions is 4
  ndims = length(dim(var))
  if(ndims != 4)
  {
    stop("The number of dimensions of the LTL file must be 4. Currently ", ndims, ".")
  }
  
  # extracts one time step and one ltl class
  var = var[,,1,1]
  nlon = dim(var)[1]
  nlat = dim(var)[2]
  output = array(var, c(nlon, nlat))
  
  # if the fillVal is not NA.
  # we set to NA all the values that are equal
  # to NA
  if(!is.na(fillVal))
  {
    output[output == fillVal] = NA
    
  }
  
  # extracts the mask array (lon, lat)
  output[!is.na(var)] = 0
  output[is.na(var)] = - 99.0

  # reverts the array into (lat, lon) with first latitudes at index 1
  output = aperm(output)
  
  # reverts array so that upper latitudes are at index 1
  output = apply(output, 2, rev)
  
  # writes the output in csv file.
  write.table(output, file=output_file, sep=";", row.names=FALSE, col.names=FALSE)
  
}



