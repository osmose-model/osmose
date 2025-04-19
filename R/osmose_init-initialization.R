
# Initialization methods --------------------------------------------------


init_ncdf = function(input, file, parameters = NULL, output = NULL, 
                     log = "osmose.log", version = NULL, osmose = NULL, 
                     java = "java", options = NULL, verbose = TRUE, 
                     clean = TRUE, force = FALSE, run=TRUE, append=FALSE, ...) {
  
  if(isTRUE(run)) {
    run_osmose(input=input, parameters=parameters, output=output, log=log,
               version=version, osmose=osmose, java=java,
               options=options, verbose=verbose, clean=clean, force=force)
  }
  
  rpath = file.path(output, "restart")
  conf = .readConfiguration(input)
  rfiles = dir(path=rpath, pattern = "\\.nc.*")
  nf = length(rfiles)
  
  spnames = unlist(get_par(conf, par="species.name")[get_par(conf, par="species.type")=="focal"])
  spindex = as.numeric(gsub(names(spnames), pattern="species.name.sp", replacement = ""))
  
  spp       = NULL 
  abundance = NULL
  weight    = NULL
  length    = NULL
  tl        = NULL
  age       = NULL
  
  for(rfile in rfiles) {
    
    nc = nc_open(file.path(rpath, rfile))
    
    spp       = c(spp, ncvar_get(nc, "species"))
    abundance = c(abundance, ncvar_get(nc, "abundance"))
    weight    = c(weight, ncvar_get(nc, "weight"))
    length    = c(length, ncvar_get(nc, "length"))
    tl        = c(tl, ncvar_get(nc, "trophiclevel"))
    age       = c(age, ncvar_get(nc, "age"))
    
    nc_close(nc)
    
  }
  
  biomass = 1e-6*abundance*weight
  
  nsp = get_par(conf, "simulation.nspecies")

  SPP       = NULL 
  ABUNDANCE = NULL
  WEIGHT    = NULL
  LENGTH    = NULL
  TL        = NULL
  AGE       = NULL
  
  for(isp in spindex) {
    
    ind = (spp == isp)
    iabundance = abundance[ind]
    ibiomass = biomass[ind]
    ilength = length[ind]
    iweight = weight[ind]
    itl = tl[ind]
    iage = age[ind]
    this = get_par(conf, sp=isp)
    
    xabundance = rowsum(iabundance, group = iage)
    xbiomass   = rowsum(ibiomass, group = iage)
    xlength    = rowsum(ilength*iabundance, group = iage)/xabundance
    xweight    = rowsum(iweight*iabundance, group = iage)/xabundance
    xtl        = rowsum(itl*iabundance, group = iage)/xabundance
    xschool    = ceiling(rowsum(rep(1, length(iage)), group = iage)/nf)
    xage       = as.numeric(rownames(xabundance))
    xsize      = VB(xage, this, method=1)

    bio_ini = sum(xbiomass)
    bio_rel = as.numeric(xbiomass/bio_ini)
    
    tl_sp = as.numeric(xtl)
    
    bio = bio_ini*bio_rel/nf
    abn = 1e6*bio/xweight/xschool
    
    n = sum(xschool)
    
    SPP       = c(SPP, rep(isp, n)) 
    ABUNDANCE = c(ABUNDANCE, rep(abn, times=xschool))
    WEIGHT    = c(WEIGHT, rep(xweight, times=xschool))
    LENGTH    = c(LENGTH, rep(xlength, times=xschool))
    TL        = c(TL, rep(tl_sp, times=xschool))
    AGE       = c(AGE, rep(xage, times=xschool))
    
    
  }
  
  X = rep(-1, length(SPP))
  Y = rep(-1, length(SPP))
 
  dim(SPP) = dim(ABUNDANCE) = dim(WEIGHT) = dim(LENGTH) = length(SPP)
  dim(TL) = dim(AGE) = dim(X) = dim(Y) = length(SPP)
  
  init = list(species      = SPP,
              x            = X,
              y            = Y,
              abundance    = ABUNDANCE,
              age          = AGE,
              length       = LENGTH,
              weight       = WEIGHT,
              trophiclevel = TL)
  
  dim = list(nschool=seq_along(SPP)-1)
  
  globalAtt = list(step=-1, species=paste(spindex, spnames, sep="=", collapse=", "))
  
  bname = sprintf("%s-initial_conditions.nc", get_par(conf, "output.file.prefix"))
  ncfile = file.path(dirname(file), bname)
  units = c("", "scalar", "scalar", "scalar", "year", "cm", "g", "scalar")
  prec  = c("integer", "float", "float", "double", "float", "float", "float", "float")
  write_ncdf(x=init, filename=ncfile, dim=dim, units = units, global=globalAtt, prec=prec,
             compression=NA, force_v4=FALSE)

  # the file is only bname because we are saving in the same folder as file (relative)
  pars = list(bname)
  pars = as.data.frame(pars)
  colnames(pars) = NULL
  rownames(pars) = "population.initialization.file" 
  xoutput = list(par=pars, init=init)
  class(xoutput) = "osmose.initialization"
  
  return(invisible(xoutput))
  
}

init_alaia = function(input, file, parameters = NULL, output = NULL, 
                     log = "osmose.log", version = NULL, osmose = NULL, 
                     java = "java", options = NULL, verbose = TRUE, 
                     clean = TRUE, force = FALSE, run=TRUE, append=FALSE, ...) {
  
  if(isTRUE(run)) {
    run_osmose(input=input, parameters=parameters, output=output, log=log,
               version=version, osmose=osmose, java=java,
               options=options, verbose=verbose, clean=clean, force=force)
  }
  
  rpath = file.path(output, "restart")
  conf = .readConfiguration(input)
  rfiles = dir(path=rpath, pattern = "\\.nc.*")
  nf = length(rfiles)
  
  spnames = unlist(get_par(conf, par="species.name")[get_par(conf, par="species.type")=="focal"])
  spindex = as.numeric(gsub(names(spnames), pattern="species.name.sp", replacement = ""))
 
  if(!dir.exists(file.path(dirname(file), "initial_conditions")))
    dir.create(file.path(dirname(file), "initial_conditions"), recursive = TRUE)
   
  for(i in seq_along(rfiles)) {
    ifile = file.path(rpath, rfiles[i])
    icode = tail(unlist(strsplit(ifile, split="\\.")), 1)
    bname = sprintf("%s-initial_conditions.nc.%s", get_par(conf, "output.file.prefix"), icode)
    ncfile = file.path(dirname(file), "initial_conditions", bname)
    file.copy(ifile, ncfile)
    nc = nc_open(ncfile, write=TRUE)
    ncatt_put(nc, varid = 0, attname="step", attval=-1)
    nc_close(nc)
  }
  
  mainfile = file.path("initial_conditions", 
                       sprintf("%s-initial_conditions.nc", get_par(conf, "output.file.prefix")))
  
  # the file is only bname because we are saving in the same folder as file (relative)
  pars = list(mainfile)
  pars = as.data.frame(pars)
  colnames(pars) = NULL
  rownames(pars) = "population.initialization.file" 
  xoutput = list(par=pars)
  class(xoutput) = "osmose.initialization"
  
  return(invisible(xoutput))
  
}


init_firstyear = function(input, file, parameters = NULL, output = NULL, 
                          log = "osmose.log", version = NULL, osmose = NULL, 
                          java = "java", options = NULL, verbose = TRUE, 
                          clean = TRUE, force = FALSE, run=TRUE, append=FALSE, ...) {
  
  if(isTRUE(run)) {
    run_osmose(input=input, parameters=parameters, output=output, log=log,
               version=version, osmose=osmose, java=java,
               options=options, verbose=verbose, clean=clean, force=force)
  }
  
  rpath = file.path(output, "restart")
  conf = .readConfiguration(input)
  rfiles = dir(path=rpath, pattern = "\\.nc.*")
  nf = length(rfiles)

  spnames = unlist(get_par(conf, par="species.name")[get_par(conf, par="species.type")=="focal"])
  spindex = as.numeric(gsub(names(spnames), pattern="species.name.sp", replacement = ""))
  
  spp       = NULL 
  abundance = NULL
  weight    = NULL
  length    = NULL
  tl        = NULL
  age       = NULL
  
  for(rfile in rfiles) {
    
    nc = nc_open(file.path(rpath, rfile))
    
    spp       = c(spp, ncvar_get(nc, "species"))
    abundance = c(abundance, ncvar_get(nc, "abundance"))
    weight    = c(weight, ncvar_get(nc, "weight"))
    length    = c(length, ncvar_get(nc, "length"))
    tl        = c(tl, ncvar_get(nc, "trophiclevel"))
    age       = c(age, ncvar_get(nc, "age"))
    
    nc_close(nc)
    
  }
  
  biomass = 1e-6*abundance*weight
  
  nsp = get_par(conf, "simulation.nspecies")
  
  pars = list()
  for(isp in spindex) {
    
    ind = (spp == isp)
    iabundance = abundance[ind]
    ibiomass = biomass[ind]
    ilength = length[ind]
    itl = tl[ind]
    iage = age[ind]
    this = get_par(conf, sp=isp)
    egg.size = get_par(this, "egg.size")
    
    xabundance = rowsum(iabundance, group = iage)
    xbiomass   = rowsum(ibiomass, group = iage)
    xlength    = rowsum(ilength*iabundance, group = iage)/xabundance
    xtl        = rowsum(itl*iabundance, group = iage)/xabundance
    xage       = as.numeric(rownames(xabundance))
    xsize      = VB(xage, this, method=1)
    xschool    = as.numeric(ceiling(rowsum(rep(1, length(iage)), group = iage)/nf))
    
    lambda = mean(-log(xschool/xschool[1])/(xage-xage[1]), na.rm=TRUE)
    nschool = pmax(ceiling(xschool[1]*exp(-lambda*(xage-xage[1]))), 1)
    
    xlength = as.numeric(.checkMonotonicity(xlength))
    
    xbins = c(egg.size, 0.5*(head(xlength, -1) + tail(xlength, -1)), 
              tail(xlength,1)+0.5*tail(diff(xlength),1))
    
    bio_ini = sum(xbiomass)
    bio_rel = as.numeric(xbiomass/bio_ini)
    
    tl_sp = as.numeric(xtl)
    
    out = c(round(bio_ini/nf, 1), 
            paste(format(bio_rel, scientific = FALSE), collapse=", "), 
            paste(round(xbins, 2), collapse=","),
            paste(xage, collapse=","),
            paste(nschool, collapse=","),
            paste(round(tl_sp, 2), collapse=", "))
    dim(out) = c(length(out), 1)
    
    out = as.data.frame(out)
    rownames(out) = sprintf(c("population.initialization.biomass.sp%d",
                              "population.initialization.relativebiomass.sp%d",
                              "population.initialization.size.sp%d",
                              "population.initialization.age.sp%d",
                              "population.initialization.nschool.sp%d",
                              "population.initialization.tl.sp%d"), isp)
    colnames(out) = NULL
    
    pars[[isp+1]] = out
    
  }
  
  xoutput = list(par=pars, init=NULL)
  class(xoutput) = "osmose.initialization"
  
  return(invisible(xoutput))
  
}


init_sofia = function(input, file=NULL, test=FALSE, sp=NULL, ...) {
  
  # ow = options("warn")
  # options(warn=1)
  # on.exit(options(ow))
  
  conf = .readConfiguration(input)
  
  nsp = get_par(conf, "simulation.nspecies")
  ndt = get_par(conf, "simulation.time.ndtPerYear")
  
  spind = get_par(conf, "species.type") == "focal"
  spind = gsub(names(spind)[which(spind)], pattern="species.type.sp", replacement = "") 
  spnames = get_par(conf, "species.name")[sprintf("species.name.sp%s", spind)]
  spind = sort(as.numeric(spind))
  
  if(!is.null(sp)) {
    spind = intersect(sp, spind)
    if(length(spind)==0) stop("Species codes are not valid.")
  }
  
  out = vector("list", nsp)
  names(out) = spnames
  pars = list()
  
  for(sp in spind) {
    
    this = get_par(conf, sp=sp)
    iSpName = get_par(this, "species.name")
    
    cat(sprintf("\nInitialising species %d (%s)\n", sp, iSpName))
    
    sim = list()
    sim$cal       = read.cal(conf, sp)
    sim$biomass   = read.biomass(conf, sp)
    sim$yield     = read.yield(conf, sp)
    sim$fecundity = read.fecundity(conf, sp)
    sim$bioguess  = get_par(get_par(conf, sp=sp), "observed.biomass.guess")
    isp = sprintf("osmose.initialization.data.sp%d", sp)
    conf[[isp]]   = sim
    
    this = get_par(conf, sp=sp)
    
    sim = .simF_ini(conf, sp, test=test)
    sim$nschool = get_par(this, "simulation.nschool")
    if(is.null(sim$nschool))
      stop(sprintf("Parameter 'simulation.nschool.sp%d' not found.", sp))
    sim$larvalM = ndt*sim$larvalM # transform to annual rate, asummed 'by time-step'
    add_larvae = is.null(get_par(this, "mortality.additional.larva.rate.sp"))
    mode = get_par(this, "species.reproduction.mode.sp")
    if(is.null(mode)) mode = "oviparity"
    sim$is_vivipare = (mode == "viviparity")
    sim$osmose  = .initial_length_dist(sim, sp, add_larvae)
    pars[[iSpName]] = as.matrix(sim$osmose)
    out[[iSpName]] = sim
    
    larv_msg = sprintf("Estimated Larval Mortality: %0.3f", sim$larvalM)
    message(larv_msg)
    
  }
  
  xoutput = list(par=pars, init=out)
  class(xoutput) = "osmose.initialization"
  
  return(invisible(xoutput))
  
}


# Simulation --------------------------------------------------------------


.simCatch_ini = function(conf, sp) {
  
  this = get_par(conf, sp=sp)
  dat  = get_par(this, par="osmose.initialization.data")
  ndt = conf$simulation.time.ndtperyear
  A = get_par(this, "species.lifespan")
  a = get_par(this, "species.length2weight.condition.factor")
  b = get_par(this, "species.length2weight.allometric.power")
  
  age_bins = seq(from=0, to=A, by=1/ndt)
  age = 0.5*head(age_bins, -1) + 0.5*tail(age_bins, -1)
  size = VB(age, this, method=3)
  size_bins = VB(age_bins, this, method=3)
  
  T = 5*length(age)
  C = length(age)
  
  CAL = dat$cal
  if(is.null(CAL)) return(NULL)
  harvested = CAL$harvested
  BIO = dat$biomass
  fecundity = dat$fecundity
  
  rF = fecundity[1:ndt]/sum(fecundity[1:ndt])
  bioguess = .bioguess(x=dat, ndt=ndt)

  if(bioguess==0) {
    # species not starting in the simulation
    output = list(pop=rep(0, C), catch=rep(0, C), R=0, 
                  biomass=rep(0, ndt), yield=rep(0, ndt), F=rep(0, ndt),
                  dist = rep(0, C), distB = rep(0, C), 
                  selectivity=rep(0, C), age=age, size=size,
                  Fguess=0, observed=list(biomass=0, yield=rep(0, ndt)),
                  bins=list(age=age_bins, size=size_bins), harvested=FALSE, larvalM=0)
    
    class(output) = c("osmose.init", class(output))
    
    return(output)
  }
  
  trans = rebinning(CAL$bins, VB(age_bins, this, method=3))
  cal = CAL$mat %*% trans
  weight = a*size^b
  yield_obs   = rep((1e-6*rowSums(t(t(cal)*weight)))[1:ndt], length=T)
  
  M = calculateMortality(conf, sp)
  Ma = M$M[cut(age, breaks = M$age, labels = FALSE)]
  
  cutoff = get_par(this, "observed.biomass.cutoff.size")
  if(is.null(cutoff)) 
    stop(sprintf("Parameter 'observed.biomass.cutoff.size.sp%d' has not been provided.", sp), call. = FALSE)
  xn = which.max(size >= cutoff) - 1
  ixn = seq_len(C)
  ixn = if(xn>0) tail(ixn, -xn) else ixn # ixn:= is observed for biomass computation
  
  surv = exp(-cumsum(c(0,Ma[-length(Ma)]/ndt))) # improve with recruitment seasonality
  irF = rep(rF, length=length(surv)) # relative fecundity over lifespan
  inibio = 1e-6*rev(irF)*surv*rep(weight, length=length(surv)) # tonnes
  R = (bioguess/sum(inibio[ixn]))
  inir = rev(irF)*surv # R:= annual recruitment
  
  for(ix in 1:5) {
    
    pop = matrix(0, nrow=T, ncol=C)
    catch = matrix(NA, nrow=T, ncol=C)
    pop[1, ] = R*inir
    pop[ , 1] = R*rep(rF, length=T)
    
    for(t in 1:T) {
      it = ((t-1) %% ndt) + 1
      MID = pop[t, ]*exp(-0.5*Ma/ndt) # first half
      MID.C = pmax(MID - cal[it, ], 0.01*MID) # remove as much catch as possible, CHECK
      catch[t, ] = MID - MID.C
      END = MID.C*exp(-0.5*Ma/ndt) # second half
      if(t!=T) pop[t+1, 2:C] = head(END, -1)   
    }
    
    biopop = 1e-6*t(t(pop)*weight) # biomass in tonnes
    biomass = rowSums(biopop[, ixn]) # exclude stages below cutoff
    bioref = mean(tail(biomass, ndt))
    yield   = 1e-6*rowSums(t(t(catch)*weight)) # in tonnes
    
    Fguess = tail(yield_obs, ndt)/tail(biomass, ndt)
    Fseason = Fguess/sum(Fguess)
    if(all(is.na(Fseason))) Fseason = rep(1, ndt)/ndt
    
    Rupb = bioguess/max(bioref, 1, na.rm=TRUE) # assume a minimum of 1 tonne (biomass)
    Rupc = sum(tail(yield_obs, ndt))/max(sum(tail(yield,ndt)), 0.1, na.rm=TRUE) # assume a minimum of 100 kg (catch)
    
    inc = weighted.mean(c(Rupb, Rupc), w = c(1, 2), na.rm=TRUE)
    R = R*inc
    
    ini = inc*pop[T - ndt + 1 ,]
    
  }
  
  xind = T - ndt + 1
  pind = xind + seq_len(ndt) - 1
  
  es = colSums(catch[pind, ])/colMeans(pop[pind, ])
  es = es/max(es, na.rm=TRUE)
  
  Fguess = sum(yield[pind]/biomass[pind])
  
  harvested = !all(yield[pind]< 1e-3) 
  
  output = list(pop=pop[pind, ], catch=catch[pind, ], R=R, 
                biomass=biomass[pind], yield=yield[pind], F=Fseason,
                dist = pop[xind ,], distB = 1e-6*pop[xind ,]*weight, 
                selectivity=es, age=age, size=size,
                Fguess=Fguess, observed=list(biomass=bioguess, yield=yield_obs[1:ndt]),
                bins=list(age=age_bins, size=size_bins), harvested=harvested)
  
  matsize = .get_matsize(this)
  isMature = size >= matsize
  
  mode = get_par(this, "species.reproduction.mode")
  if(is.null(mode)) mode = "oviparity"
  if(mode=="viviparity") weight = 1e-6
  
  eggs   = sum(fecundity[1:ndt]*t(t(output$pop)*isMature*(1e6*weight)), na.rm=TRUE)
  larvae = output$R
  Mlarval = -log(larvae/eggs)
  
  output$larvalM = max(Mlarval, 1, na.rm=TRUE)
  
  class(output) = c("osmose.init",class(output))
  return(output)
  
}

.simF_ini = function(conf, sp, tiny=1e-3, cv=c(0.25, 0.1), test=FALSE) {
  
  sim = .simCatch_ini(conf, sp)
  if(!isTRUE(sim$harvested) & !is.null(sim)) return(sim)
  if(isTRUE(test)) return(sim)
  
  this = get_par(conf, sp=sp)
  dat  = get_par(this, par="osmose.initialization.data")
  ndt = conf$simulation.time.ndtperyear
  A = get_par(this, "species.lifespan")
  a = get_par(this, "species.length2weight.condition.factor")
  b = get_par(this, "species.length2weight.allometric.power")
  
  age_bins = seq(from=0, to=A, by=1/ndt)
  age = 0.5*head(age_bins, -1) + 0.5*tail(age_bins, -1)
  size = VB(age, this, method=3)
  size_bins = VB(age_bins, this, method=3)
  
  T = 5*length(age)
  C = length(age)
  
  CAL = dat$cal
  BIO = dat$biomass
  fecundity = dat$fecundity
  rF = fecundity[1:ndt]/sum(fecundity[1:ndt])
  biofit   = .bioguess(x=dat, ndt=ndt, ts=TRUE)
  bioguess = .bioguess(x=dat, ndt=ndt)
  
  if(all(is.na(dat$yield))) stop("No catch data has been provided (all NAs).")
  if(any(is.na(dat$yield))) warning("Missing catch data for some years, complete it if possible.", call. = FALSE)
  
  yield_obs   = dat$yield
  yield_obs   = rep(yield_obs[1:ndt], length=T)
  if(all(is.na(yield_obs))) {
    warning("No catch data has been provided for the initial year, using the average of the time series.", call. = FALSE)
    yield_obs = rep(rowSums(matrix(dat$yield, nrow=ndt), na.rm=TRUE), length=T)
  }
  
  weight = a*size^b
  M = calculateMortality(conf, sp)
  Ma = M$M[cut(age, breaks = M$age, labels = FALSE)]
  
  cutoff = get_par(this, "observed.biomass.cutoff.size")
  if(is.null(cutoff)) stop(sprintf("Parameter 'observed.biomass.cutoff.size.sp%d' has not been provided.", sp), call. = FALSE)
  xn = which.max(size >= cutoff) - 1
  ixn = seq_len(C)
  ixn = if(xn>0) tail(ixn, -xn) else ixn # ixn:= is observed for biomass computation
  
  if(is.null(sim)) {
    
    sel = .getSelectivity(size, this) # creado con parametros
    Fseason = yield_obs[1:ndt]/sum(yield_obs[1:ndt])
    if(all(is.nan(Fseason))) Fseason = rep(1, length(Fseason))/length(Fseason)
    
    # ini = exp(-cumsum(c(0,Ma[-length(Ma)]/ndt)))
    # inibio = 1e-6*sum(ini*weight)
    # R = as.numeric(ndt)*(bioguess/inibio)
    # xdist = R*ini/sum(ini)
    
    surv = exp(-cumsum(c(0,Ma[-length(Ma)]/ndt))) # improve with recruitment seasonality
    irF = rep(rF, length=length(surv)) # relative fecundity over lifespan
    inibio = 1e-6*rev(irF)*surv*rep(weight, length=length(surv)) # tonnes
    R = (bioguess/sum(inibio[ixn]))
    inir = rev(irF)*surv # R:= annual recruitment
    xdist = R*inir
    
    sim = list(R=R, Fguess=sum(yield_obs[1:ndt]/bioguess))
    
  } else {
    
    es = empirical_selectivity(matrix(sim$selectivity, nrow=1), fleet = "sim",
                               years = 1, bins = sim$size)
    xind = .nonNullPoints(y=sim$selectivity, thr=1e-3, span=5)
    k = min(length(xind)-1, 5)
    ss_emp = try(suppressMessages(fit_selectivity(es, pattern=27, k=k)), silent = TRUE)
    if(inherits(ss_emp, "try-error")) {
      sel = sim$selectivity
    } else {
      sel = ss_emp$selectivity    
    }
  
    Fseason = sim$F
    xdist = sim$dist
    inir = xdist/sim$R
    
  }
  
  sel[sel<tiny] = 0
  
  .simF = function(par, value=FALSE) {
    
    R = exp(par[1])
    F = exp(par[2])
    if(is.na(F)) F=0
    
    pop = matrix(0, nrow=T, ncol=C)
    catch = matrix(NA, nrow=T, ncol=C)
    pop[1, ] = R*inir
    pop[ , 1] = R*rep(rF, length=T)
    
    for(t in 1:T) {
      it = ((t-1) %% ndt) + 1
      Ft = F*Fseason[it]*as.numeric(sel)
      Zt = Ma/ndt + Ft
      tmp = pop[t, ]*exp(-Zt) 
      catch[t, ] = pop[t, ]*(Ft/Zt)*(1-exp(-Zt)) 
      if(t!=T) pop[t+1, 2:C] = head(tmp, -1)   
    }
    
    biopop = 1e-6*t(t(pop)*weight)[, ixn]
    biomass = rowSums(biopop)
    yield   = 1e-6*rowSums(t(t(catch)*weight))
    
    ll_biomass  = lnorm2(biofit, tail(biomass, ndt))
    ll_yield    = lnorm2(yield_obs[1:ndt] , tail(yield, ndt))
    
    ll = ll_biomass*llw(cv[1]) + ll_yield*llw(cv[2])
    
    if(isFALSE(value)) return(ll)
    
    xind = T - ndt + 1
    pind = xind + seq_len(ndt) - 1
    
    es = colSums(catch[pind, ])/colMeans(pop[pind, ])
    
    output = list(pop=pop[pind, ], catch=catch[pind, ], R=R, 
                  biomass=biomass[pind], yield=yield[pind], F=Fseason, Fguess=F,
                  dist=pop[xind ,], distB = 1e-6*pop[xind, ]*weight, 
                  selectivity=sel, age=age, size=size,
                  observed=list(biomass=biofit, yield=yield_obs[1:ndt]),
                  bins=list(age=age_bins, size=size_bins))
    
    return(output)
    
  }
  
  logR = log(sim$R)
  logF = log(sim$Fguess)
  
  if(sum(yield_obs)==0) {
    opt = optim2(par=logR, fn = .simF, method = "Brent", lower=0, upper=logR+6)  
  } else {
    opt = optim2(par=c(logR, logF), fn = .simF, method = "Rvmmin")
  }
  
  # opt = calibrate(par=log(c(sim$R, sim$Fguess)), fn = .simF, method = "Rvmmin",
  #                 lower=c(0, -20), upper=c(log(),2))
  
  output = c(.simF(opt$par, value=TRUE),  opt=list(opt))
  
  matsize = .get_matsize(this)
  isMature = size >= matsize
  
  mode = get_par(this, "species.reproduction.mode")
  if(is.null(mode)) mode = "oviparity"
  if(mode=="viviparity") weight = 1e-6
  
  eggs   = sum(fecundity[1:ndt]*t(t(output$pop)*isMature*(1e6*weight)), na.rm=TRUE)
  larvae = output$R
  Mlarval = -log(larvae/eggs)
  
  msgL = sprintf("Estimated larval mortality (%0.2f) for %s is too low, check fecundity.",
                 ndt*Mlarval, get_par(this, par="species.name"))
  
  if((Mlarval < 5) && (mode=="oviparity")) message(msgL)
  
  output$larvalM = max(Mlarval, 1, na.rm=TRUE)
  
  mB = mean(output$biomass/output$observed$biomass, na.rm=TRUE)
  mY = mean(output$yield/output$observed$yield, na.rm=TRUE)
  
  msgB = sprintf("It was not possible to fit the suggested biomass (%0.2f). Please check all the parameters provided for this species, including growth and length-weight.", mB)
  msgY = sprintf("It was not possible to fit the suggested yield (%0.2f). Please check all the parameters provided for this species, including growth and length-weight.", mY)
  
  if(mB > 100) stop(msgB, call. = FALSE)
  if(mY > 10) stop(msgY, call. = FALSE)
  
  class(output) = c("osmose.init", class(output))
  return(output)
  
}


# Internal ----------------------------------------------------------------

llw = function(cv) 1/(2*cv^2)

.initial_length_dist = function(sim, sp, add_larvae) {
  
  dist = sim$distB
  biotest = sum(dist)==0
  dist[dist==0] = 1e-3 # 1kg instead of nothing
  bio_ini = sum(dist)
  if(isTRUE(biotest)) bio_ini = 0
  bio_rel = if(bio_ini==0) dist else dist/bio_ini
  tl_sp = rep(2, length(dist))
  xage  = sim$age
 
  # begin test 
  
  # end test

  min_school = pmax(ceiling(sim$nschool*0.025), 1L)
  max_school = sim$nschool - min_school
  rel_dist = sim$distB
  rel_dist = rel_dist - min(rel_dist, na.rm=TRUE)
  rel_dist = rel_dist/max(rel_dist, na.rm=TRUE)
  
  nschool = pmax(ceiling(max_school*rel_dist), 1) + min_school
  nschool[is.na(nschool)] = 1
  nschool = pmin(nschool, sim$nschool)
  
  out = c(round(bio_ini, 3), 
          paste(format(bio_rel, scientific = FALSE), collapse=", "), 
          paste(round(sim$bins$size,3), collapse=","),
          paste(xage, collapse=","),
          paste(round(tl_sp, 2), collapse=", "),
          paste(nschool, collapse = ", "),
          round(sim$larvalM, 5))
  dim(out) = c(length(out), 1)
  
  check = add_larvae & (!sim$is_vivipare)
  lpatt = if(check) "mortality.additional.larva.rate.sp%d" else "initialization.additional.larva.rate.sp%d"
  out = as.data.frame(out)
  rownames(out) = sprintf(c("population.initialization.biomass.sp%d",
                            "population.initialization.relativebiomass.sp%d",
                            "population.initialization.size.sp%d",
                            "population.initialization.age.sp%d",
                            "population.initialization.tl.sp%d",
                            "population.initialization.nschool.sp%d",
                            lpatt), sp)
  colnames(out) = NULL
  
  return(out)
  
}


# Internal functions ------------------------------------------------------

.checkMonotonicity = function(x) {
  ind = which(diff(x)<0)
  if(length(ind)==0) return(x)
  x[ind+1] = NA
  fun = splinefun(x=seq_along(x), y=x)
  x[ind+1] = fun(x=ind+1)
  return(x)  
}
