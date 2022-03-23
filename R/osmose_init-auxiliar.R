
#' create OSMOSE ncdf grid (in development)
#'
#' @param conf OSMOSE configuration
#' @param ... 
#'
#' @return
#' @export
#'
osmose_grid = function(conf, center=TRUE, ...) {
  
  lat = c(.getPar(conf, "grid.lowright.lat"),
          .getPar(conf, "grid.upleft.lat"))
  lon = c(.getPar(conf, "grid.upleft.lon"),
          .getPar(conf, "grid.lowright.lon"))
  dx = diff(lon)/.getPar(conf, "ncolumn")
  dy = diff(lat)/.getPar(conf, "nline")
  
  # Create a rectangular grid given lat, lon and dxy.
  # No correction by Earth curvature
  if(dx <= 0 || dy <= 0) stop("dx and dy must be positive.")
  
  if(isTRUE(center)) {
    lat[which.min(lat)] = lat[which.min(lat)] + 0.5*dy
    lat[which.max(lat)] = lat[which.max(lat)] - 0.5*dy
    lon[which.min(lon)] = lon[which.min(lon)] + 0.5*dx
    lon[which.max(lon)] = lon[which.max(lon)] - 0.5*dx
  }
  
  nx = 1 + round(diff(lon)/dx, 0)
  ny = 1 + round(diff(lat)/dy, 0)
  
  lats.rho = seq(from=min(lat),to=max(lat), length=ny)
  lons.rho = seq(from=min(lon),to=max(lon), length=nx)
  
  lats.psi = seq(from=min(lat) - 0.5*dy, to=max(lat) + 0.5*dy, length=ny+1)
  lons.psi = seq(from=min(lon) - 0.5*dx, to=max(lon) + 0.5*dx, length=nx+1)
  
  rho = list(lat=lats.rho, lon=lons.rho)
  psi = list(lat=lats.psi, lon=lons.psi)
  
  nlat = length(rho$lat)
  nlon = length(rho$lon)
  
  LAT = matrix(rho$lat, ncol=nlat, nrow=nlon, byrow=TRUE)
  LON = matrix(rho$lon, ncol=nlat, nrow=nlon)
  
  area = (111*dy)*(111*cos(LAT*pi/180)*dx)
  
  output = list(lon=lons.rho, lat=lats.rho, rho=rho, psi=psi, 
                LON=LON, LAT=LAT, area=area)
  
  return(output)
  
}