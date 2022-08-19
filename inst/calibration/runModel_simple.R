
runModel = function(par, conf, osmose, is_a_test=FALSE, version="4.3.3", ...) {
  
  nspp = get_species(conf, type="focal", code=TRUE)
  nfsh = get_fisheries(conf, code=TRUE)
  ndtperyear = get_par(conf, 'simulation.time.ndtperyear')
  nyear      = get_par(conf, 'simulation.time.nyear')
  ndt = nyear*ndtperyear
  
  larval_deviates = get_par(par, 'osmose.user.larval.deviate')
  
  for(isp in nspp) {
    nn = sprintf('mortality.additional.larva.rate.seasonality.sp%s', isp)
    ldev = get_par(larval_deviates, sp=as.numeric(isp))
    par[[nn]] = exp(calibrar::spline_par(ldev, n=ndt)$x)  
  }
  
  d75 = get_par(par, "selectivity.delta75.fsh") # all of them
  l50 = get_par(par, "selectivity.l50.fsh") # all of them
  L50 = get_par(conf, "selectivity.l50.fsh")

  for(ifsh in nfsh) {
    nn = sprintf('fisheries.selectivity.l75.fsh%s', ifsh)
    this.d75 = get_par(d75, fsh=as.numeric(ifsh))
    if(is.null(this.d75)) next
    this.l50 = get_par(l50, fsh=as.numeric(ifsh))
    if(is.null(this.l50)) this.l50 = get_par(L50, fsh=as.numeric(ifsh))
    par[[nn]] = this.l50 + this.d75    
  }  
  
  # remove all osmose.user parameters and clean-up
  ind = grep(names(par), pattern="^osmose.user")
  par[ind] = NULL
  par = par[sort(names(par))]
  class(par) = "osmose.configuration"
  # write parameters for osmose to understand
  write_osmose(par, file='calibration_parameters.osm')
  # run osmose!
  if(!isTRUE(is_a_test)) {
    run_osmose(input='osmose-calibration.osm', output='output', osmose=osmose, version = version, verbose=FALSE)
  }
  
  output = read_osmose(path='output', version=version)
  
  cal_output = c(biomass = get_var(output, "biomass", how="list"),
                 yield   = get_var(output, "yieldBySpecies", how="list"),
                 catchatlength = get_var(output, "yieldNBySize", how="list"),
                 mortality = get_var(output, "residualSizeByAge", how="list"),
                 growth = get_var(output, "residualMortalityByAge", how="list")
                 )

  return(invisible(cal_output))
  
}

