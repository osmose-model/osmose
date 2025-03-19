
.set_param_user = function(allfiles, conf) {
  
  guess_file = allfiles["guess"] 
  min_file = allfiles["min"]
  max_file = allfiles["max"]
  phase_file = allfiles["phase"]
  
  mcat("\n#---- User parameters\n", file=allfiles, append = TRUE)
  
  pars = get_par(conf, "osmose.user", as.is=TRUE)
  pars = get_par(pars, "osmose.user.larval.deviate.log", invert=TRUE, as.is = TRUE)
  pars = get_par(pars, "osmose.user.selectivity.delta75.fsh", invert=TRUE, as.is = TRUE)
  
  if(is.null(pars)) return(invisible(NULL))
  
  write_osmose(pars, file=guess_file, append = TRUE)
  write_osmose(set_par(pars, scale=0.8), file=min_file, append = TRUE)
  write_osmose(set_par(pars, scale=1.2), file=max_file, append = TRUE)
  write_osmose(set_par(pars, -1), file=phase_file, append = TRUE) # user must activate manually this parameters
  return(invisible(NULL))
}

.set_param_resources = function(allfiles, conf) {
  guess_file = allfiles["guess"] 
  min_file = allfiles["min"]
  max_file = allfiles["max"]
  phase_file = allfiles["phase"]
  
  mcat("\n#---- Resources species\n", file=allfiles, append = TRUE)
  
  pars = get_par(conf, "species.accessibility2fish.sp", as.is=TRUE)
  pars = transform_par(pars, FUN=logit)
  phase = get_par_phase(conf, "species.accessibility2fish", default=1)
  
  write_osmose(pars, file=guess_file, append = TRUE)
  write_osmose(set_par(pars, scale=0.8, lower=-20), file=min_file, append = TRUE)
  write_osmose(set_par(pars, scale=1.2, upper=+20), file=max_file, append = TRUE)
  write_osmose(set_par(pars, phase), file=phase_file, append = TRUE)
  return(invisible(NULL))
}


.set_param_background = function(allfiles, conf) {
  guess_file = allfiles["guess"] 
  min_file = allfiles["min"]
  max_file = allfiles["max"]
  phase_file = allfiles["phase"]
  nbkg = get_par(conf, "nbackground")
  if(!is.null(nbkg)) {
    mcat("\n#---- Background species\n", file=allfiles, append = TRUE)
    pars = get_par(conf, "species.multiplier.sp", as.is=TRUE)
    pars = transform_par(pars, FUN=log10)
    phase = get_par_phase(conf, "species.multiplier", default=1)
    
    write_osmose(pars, file=guess_file, append = TRUE)
    write_osmose(set_par(pars, scale=0.8, lower=0), file=min_file, append = TRUE)
    write_osmose(set_par(pars, scale=1.2, upper=7), file=max_file, append = TRUE)
    write_osmose(set_par(pars, phase), file=phase_file, append = TRUE)
  }
  return(invisible(NULL))
}


.set_param_focal = function(allfiles, conf) {
  
  guess_file = allfiles["guess"] 
  min_file = allfiles["min"]
  max_file = allfiles["max"]
  phase_file = allfiles["phase"]
  
  nyear = get_par(conf, "simulation.time.nyear")
  
  mcat("\n#---- Focal species\n", file=allfiles, append = TRUE)
  
   write_osmose_parameter(conf, par="population.initialization.biomass", FUN=log10, 
                         files=allfiles, scale=c(0.8, 1.2), lower=0, upper=+7, delta=1, default=-1)
  
   write_osmose_parameter(conf, par="mortality.additional.rate", FUN=log10, 
                          files=allfiles, scale=c(0.8, 1.2), lower=-4, upper=+1, delta=1, default=2)
  
   write_osmose_parameter(conf, par="mortality.additional.larva.rate", FUN=log10, 
                          files=allfiles, scale=c(0.8, 1.2), lower=-1, upper=+3, delta=1, default=1)
   
  knots = get_par(conf, "mortality.additional.larva.knots", as.is=TRUE)
  viv   = get_par(conf, "species.reproducion.mode", as.is=TRUE)
  
  pars = list()
  for(isp in get_species(conf, type="focal", code=TRUE)) {
    irmod = get_par(viv, sp=as.numeric(isp))
    if(is.null(irmod)) irmod = "oviparity"
    if(irmod=="viviparity") next
    iknots = get_par(knots, sp=as.numeric(isp))
    if(is.null(iknots)) iknots = nyear + 1
    nn = sprintf("osmose.user.larval.deviate.log.sp%s", isp)
    pars[[nn]] = rep(0, iknots)
  }
  class(pars) = "osmose.configuration"
  phase = get_par_phase(conf, "mortality.additional.larva.deviate", default=3)
  
  write_osmose(pars, file=guess_file, append = TRUE)
  write_osmose(set_par(pars, scale=0.8, lower=-4), file=min_file, append = TRUE)
  write_osmose(set_par(pars, scale=1.2, upper=+4), file=max_file, append = TRUE)
  write_osmose(set_par(pars, phase), file=phase_file, append = TRUE)
  
  write_osmose_parameter(conf, par="predation.ingestion.rate.max", FUN=log10, 
                         files=allfiles, scale=c(0.8, 1.2), lower=0, upper=+2, delta=1, default=-1)

  
  return(invisible(TRUE))
}

.set_param_growth = function(allfiles, conf, bioen) {
  guess_file = allfiles["guess"] 
  min_file = allfiles["min"]
  max_file = allfiles["max"]
  phase_file = allfiles["phase"]
  
  if(!isTRUE(bioen)) {
    # estimating delta.lmax for classical OSMOSE growth
    pars = get_par(conf, "species.delta.lmax.factor.sp", as.is=TRUE)
    if(is.null(pars)) pars = list()
    for(isp in get_species(conf, type="focal", code=TRUE)) {
      nn = sprintf("species.delta.lmax.factor.sp%s", isp)
      if(is.null(pars[[nn]])) pars[[nn]] = 2
    }
    class(pars) = "osmose.configuration"
    phase = get_par_phase(conf, "species.delta.lmax.factor", default=2)
    
    write_osmose(pars, file=guess_file, append = TRUE)
    write_osmose(set_par(pars, lower=1), file=min_file, append = TRUE)
    write_osmose(set_par(pars, upper=3), file=max_file, append = TRUE)
    write_osmose(set_par(pars, phase), file=phase_file, append = TRUE)
    
  } else {
    # bioen parameters
  }
  return(invisible(NULL))
}

.set_param_bioen = function(allfiles, conf) {
  guess_file = allfiles["guess"] 
  min_file = allfiles["min"]
  max_file = allfiles["max"]
  phase_file = allfiles["phase"]
  
  mcat("\n#---- Bioenergetics\n", file=allfiles, append = TRUE)
  
  return(invisible(NULL))
}


.set_param_fisheries = function(allfiles, conf) {
  
  guess_file = allfiles["guess"] 
  min_file = allfiles["min"]
  max_file = allfiles["max"]
  phase_file = allfiles["phase"]
  
  mcat("\n#---- Fisheries\n", file=allfiles, append = TRUE)
  
  nmf = get_fisheries(conf)
  nmc = get_fisheries(conf, code=TRUE)
  T = get_par(conf, "simulation.time.nyear")
  
  for(i in seq_along(nmc)) {
    
    ifsh = nmc[i]
    msg = sprintf("\n-- Fishery %s: %s\n", nmc[i], nmf[i])
    mcat(msg, file=allfiles, append = TRUE)
    this = get_par(conf, sprintf("fsh%s$", ifsh))
    pars = get_par(get_par(this, "fisheries.rate.base.fsh", as.is=TRUE), "shift", invert=TRUE, as.is=TRUE)
    pars = transform_par(pars, FUN=log)
    phase = get_par_phase(conf, "fisheries.rate.base", default=2)
    
    write_osmose(pars, file=guess_file, append = TRUE)
    write_osmose(set_par(pars, delta=-3), file=min_file, append = TRUE)
    write_osmose(set_par(pars, delta=+3), file=max_file, append = TRUE)
    write_osmose(set_par(pars, value=phase), file=phase_file, append = TRUE)
    
    pars = get_par(this, "fisheries.rate.byperiod.fsh", as.is=TRUE)
    n = get_par(this, "fisheries.period.number")*T + (get_par(this, "fisheries.period.start")>0)
    idefault = if(all(unlist(pars)==1)) -4 else 4
    pars = replicate_par(pars, n=n)
    pars = transform_par(pars, FUN=log)
    phase = get_par_phase(conf, "fisheries.rate.byperiod", default=idefault)
    
    write_osmose(pars, file=guess_file, append = TRUE)
    write_osmose(set_par(pars, delta=-3), file=min_file, append = TRUE)
    write_osmose(set_par(pars, delta=+3), file=max_file, append = TRUE)
    write_osmose(set_par(pars, value=phase), file=phase_file, append = TRUE)
    
    # make difference for each one: l50, l75
    pars = get_par(this, "fisheries.selectivity.l50", as.is=TRUE)
    msg = sprintf("Parameter 'fisheries.selectivity.l50' not found for fishery '%s'. Age selectivity is NOT supported with multispecies fisheries.", nmf[i])
    if(is.null(pars)) stop(msg)
    phase = get_par_phase(conf, "fisheries.selectivity.l50", default=5)
    L50 = as.numeric(unlist(pars))
    write_osmose(pars, file=guess_file, append = TRUE)
    write_osmose(set_par(pars, floor(0.5*L50)), file=min_file, append = TRUE)
    write_osmose(set_par(pars, ceiling(2*L50)), file=max_file, append = TRUE)
    write_osmose(set_par(pars, phase), file=phase_file, append = TRUE)
    
    pars75 = get_par(this, "fisheries.selectivity.l75", as.is=TRUE)
    
    if(!is.null(pars75)) {
      
      L75 = as.numeric(unlist(pars75))
      pars = list()
      nn = sprintf("osmose.user.selectivity.delta75.fsh%s", ifsh)
      pars[[nn]] = L75 - L50
      
      class(pars) = "osmose.configuration"
      phase = get_par_phase(conf, "fisheries.selectivity.l75", default=5)
      
      write_osmose(pars, file=guess_file, append = TRUE)
      write_osmose(set_par(pars, 0), file=min_file, append = TRUE)
      write_osmose(set_par(pars, ceiling(1*L50)), file=max_file, append = TRUE)
      write_osmose(set_par(pars, phase), file=phase_file, append = TRUE)
      
    }
    
  } # end of fisheries parameters
}


