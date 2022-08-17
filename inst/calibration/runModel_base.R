
runModel = function(par, conf, osmose, ...) {
  
  nspp = get_species(conf, code=TRUE)
  ndtperyear = get_par(conf, 'simulation.time.ndtperyear')
  nyear      = get_par(conf, 'simulation.time.nyear')
  ndt = nyear*ndtperyear
  
  larval_deviates = get_par(par, 'osmose.user.larval.deviate')
  
  for(isp in nspp) {
    nn = sprintf('mortality.additional.larva.rate.seasonality.sp%s', isp)
    ldev = get_par(larval_deviates, sp=as.numeric(isp))
    par[[nn]] = exp(spline_par(ldev, n=ndt)$x)  
  }
  
  write_osmose(par, file='calibration_parameters.R')
  
  run_osmose(input='osmose-calibration.osm', output='output', osmose=osmose, 
             version = '4.3.3')
  
  output = read_osmose(path='output', version='4.3.3')
  
  return(invisible(NULL))
  
}

