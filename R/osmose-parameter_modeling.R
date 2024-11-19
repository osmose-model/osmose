


#' Generate a migration input file using gaussian dispersal.
#'
#' @param par A list with parameters, see detail.
#' @param conf The OSMOSE configuration file
#' @param sp The species code.
#'
#' @return A data.frame to be written with write_osmose()
#' @export
gaussian_dispersal = function(par, conf, sp) {
  par = get_par(par, sp=sp, linear=TRUE, as.is=TRUE)
  spp = get_par(conf, sp=sp)
  
  ndt      = get_par(conf, "simulation.time.ndtPerYear")
  T        = get_par(conf, "simulation.time.nyear")
  lifespan = get_par(spp, "lifespan")
  
  scale = get_par(par, "migration.totalbiomass")
  mean  = c(ndt*get_par(par, "migration.time.peak"), 
            get_par(par, "migration.age.mean"))
  sd    = c(ndt*get_par(par, "migration.time.dispersal")/4, 
            get_par(par, "migration.age.sd"))
  r     = get_par(par, "migration.correlation")
  tiny  = get_par(par, "migration.tiny")
  
  if(is.null(tiny)) tiny = 1e-3
  
  cov = r*prod(sd)
  sigma = matrix(c(sd[1]^2, cov, cov, sd[2]^2), nrow=2)
  par = list(mean=mean, sigma=sigma)
  
  lower = c(1, 0)
  upper = c(T*ndt, lifespan)
  n = c(T*ndt, lifespan*ndt)
  out = calibrar::gaussian_kernel(par, lower, upper, n=n)
  z = out$z
  rownames(z) = out$x
  colnames(z) = out$y
  z[z<tiny] = 0
  z = scale*z/sum(z)
  z = round(z, 2)
  nonzero = colSums(z)!=0
  nz = rle(nonzero)
  nz$lengths = pmax(nz$lengths + -1*!nz$values, 0) 
  nz$lengths[nz$values] = length(nonzero) - sum(nz$lengths[!nz$values])
  nonzero = inverse.rle(nz)
  z = z[, nonzero]
  z = as.data.frame(cbind(time=out$x, z))
  return(z)
}