
run_model = function(par, conf, osmose, is_a_test=FALSE, version="4.3.3", options="-Xmx3g -Xms1g", ...) {
  
  nspp = get_species(conf, type="focal", code=TRUE)
  nfsh = get_fisheries(conf, code=TRUE)
  ndtperyear = get_par(conf, 'simulation.time.ndtperyear')
  nyear      = get_par(conf, 'simulation.time.nyear')
  ndt = nyear*ndtperyear
  
  larval_deviates  = get_par(par, 'osmose.user.larval.deviate.log')
  fishing_deviates = get_par(par, 'fisheries.rate.byperiod.log')
  
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
  
  ### BEGIN USER ADDED
  
  ### END USER ADDED
  
  # remove all osmose.user parameters and clean-up
  par = get_par(par, "osmose.user", invert = TRUE, as.is=TRUE)
  par = get_par(par, linear=TRUE, as.is = TRUE) # write parameters in linear scale.

  write_osmose(par, file='calibration_parameters.osm')
  # run osmose!
  if(!isTRUE(is_a_test)) {
    run_osmose(input='osmose-calibration.osm', output='output', osmose=osmose, 
               version = version, options=options, verbose=FALSE)
  }
  
  output = read_osmose(path='output', version=version, null.on.error=TRUE)
  
  names(larval_deviates)  = paste("larval", get_species(conf, nm=names(larval_deviates)), sep="_")
  names(fishing_deviates) = paste("F", get_fisheries(conf, nm=names(fishing_deviates)), sep="_")
  
  # random = list(larval  = sapply(larval_deviates,  FUN=function(x) sum(x^2)),
  #               fishing = sapply(fishing_deviates, FUN=function(x) sum(x^2)))
  
  cal_output = c(biomass = get_var(output, "biomass", how="list", no.error = TRUE),
                 yield   = get_var(output, "yieldBySpecies", how="list", no.error = TRUE),
                 catchatlength = get_var(output, "yieldNBySize", how="list", no.error = TRUE),
                 mortality = get_var(output, "residualSizeByAge", how="list", no.error = TRUE),
                 growth = get_var(output, "residualMortalityByAge", how="list", no.error = TRUE)
                 )
  
  if(is.null(cal_output)) {
    # sometimes, you get a 'no-write' error. Restarting the calibration usually fix the problem.
    # several things can be the issue. Most times, you won't see the error again. 
    # Those are caused by the ghosts in the machine (DATARMOR). To please them, we will run the 
    # model again, and see.
    if(!isTRUE(is_a_test)) {
      run_osmose(input='osmose-calibration.osm', output='output', osmose=osmose, 
                 version = version, options=options, verbose=FALSE)
    }
    
    output = read_osmose(path='output', version=version, null.on.error=TRUE)
    
    cal_output = c(biomass = get_var(output, "biomass", how="list", no.error = TRUE),
                   yield   = get_var(output, "yieldBySpecies", how="list", no.error = TRUE),
                   catchatlength = get_var(output, "yieldNBySize", how="list", no.error = TRUE),
                   mortality = get_var(output, "residualSizeByAge", how="list", no.error = TRUE),
                   growth = get_var(output, "residualMortalityByAge", how="list", no.error = TRUE)
                   )
    
  }
  
  # if is still NULL, we will let calibrar to deal with it.
  if(is.null(cal_output)) {
    cal_output = NULL
    message("Something wrong happened while running 'run_model'. Returning NULL. Check the '.calibrar_dump' folder.")
  }

  cal_output = c(cal_output, random=larval_deviates, random=fishing_deviates)
  
  return(invisible(cal_output))
  
}

