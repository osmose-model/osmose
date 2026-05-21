
run_model = function(par, conf, osmose, is_a_test=FALSE, version="4.3.3", options="-Xmx3g -Xms1g", ...) {
  
  .t0 = Sys.time() # timing
  nspp = get_species(conf, type="focal", code=TRUE)
  nfsh = get_fisheries(conf, code=TRUE)
  ndtperyear = get_par(conf, 'simulation.time.ndtperyear')
  nyear      = get_par(conf, 'simulation.time.nyear')
  ndt = nyear*ndtperyear
  
  # get deviates to compute random effects
  larval_deviates  = get_par(par, 'osmose.user.larval.deviate.log')
  fishing_deviates = get_par(par, 'fisheries.rate.byperiod.log')
  
  # recover parameters modelled by groups
  random = NULL
  nms = get_par(conf, "calibration.*.bygroup$", unlist=TRUE)
  if(!is.null(nms)) {
    nms = names(nms)[which(nms)]
    if(length(nms)>0) {
      nms = gsub(nms, pattern="^calibration.", replacement="")
      nms = gsub(nms, pattern=".bygroup$", replacement="")
      for(nm in nms) {
        par = osmose:::get_osmose_parameter(par=par, conf=conf, nm=nm)
        irandom = paste0("random.", nm)
        random = c(random, get_par(par, irandom, as.is=TRUE))
        par = get_par(par, irandom, invert = TRUE, as.is=TRUE)
      }
    }
  }
  
  # create interannual variability in larval mortality
  for(isp in nspp) {
    nn = sprintf('mortality.additional.larva.rate.seasonality.sp%s', isp)
    ldev = get_par(larval_deviates, sp=as.numeric(isp))
    if(is.null(ldev)) next
    par[[nn]] = exp(calibrar::spline_par(ldev, n=ndt)$x)  
  }
  
  # fishing selectivity
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

  names(larval_deviates)  = paste("M.larva", get_species(conf, nm=names(larval_deviates)), sep=".")
  names(fishing_deviates) = paste("F.byperiod", get_fisheries(conf, nm=names(fishing_deviates)), sep=".")

  # run osmose!
  t0 = t1 = 0 # init times
  if(!isTRUE(is_a_test)) {
    t0 = run_osmose(input='osmose-calibration.osm', output='output', osmose=osmose, 
               version = version, options=options, verbose=FALSE)$elapsed
  }
  
  output = read_osmose(path='output', version=version, null.on.error=TRUE)
  cal_output = osmose_calibration_outputs(output)
  tr = output$elapsed
  
  if(is.null(cal_output)) {
    # sometimes, you get a 'no-write' error. Restarting the calibration usually fix the problem.
    # several things can be the issue. Most times, you won't see the error again. 
    # Those are caused by the ghosts in the machine (DATARMOR). To please them, we will run the 
    # model again, and see.
    if(!isTRUE(is_a_test)) {
      t1 = run_osmose(input='osmose-calibration.osm', output='output', osmose=osmose,
                 version = version, options=options, verbose=FALSE)$elapsed
    }
    
    output = read_osmose(path='output', version=version, null.on.error=TRUE)
    cal_output = osmose_calibration_outputs(output)
    tr = tr + output$elapsed
  }
  
  # if is still NULL, we will let calibrar to deal with it.
  if(is.null(cal_output)) {
    message("Something wrong happened while running 'run_model'. Returning NULL. Check the '.calibrar_dump' folder.")
    return(invisible(cal_output))
  }

  # create final output list, including random effects
  cal_output = c(cal_output, random, random=larval_deviates, random=fishing_deviates)
  
  # save timing information.
  osmose:::.timing(.t0=.t0, t0=t0, t1=t1, tr=tr, file="../timing.log")
  
  return(invisible(cal_output))
  
}

