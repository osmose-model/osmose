
# Initialization methods --------------------------------------------------


init_ncdf = function(input, file, parameters = NULL, output = NULL, 
                     log = "osmose.log", version = NULL, osmose = NULL, 
                     java = "java", options = NULL, verbose = TRUE, 
                     clean = TRUE, force = FALSE, run=TRUE, append=FALSE, ...) {
  
  parameters = paste("-Poutput.restart.enabled=TRUE", parameters)
  
  if(isTRUE(run)) {
    run_osmose(input=input, parameters=parameters, output=output, log=log,
               version=version, osmose=osmose, java=java,
               options=options, verbose=verbose, clean=clean, force=force)
  }
  
  rpath = file.path(output, "restart")
  conf = .readConfiguration(input)
  rfiles = dir(path=rpath, pattern = "\\.nc.*")
  nf = length(rfiles)
  
  spnames = unlist(.getPar(conf, par="species.name")[.getPar(conf, par="species.type")=="focal"])
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
  
  nsp = .getPar(conf, "simulation.nspecies")

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
    this = .getPar(conf, sp=isp)
    
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
  
  bname = sprintf("%s-initial_conditions.nc", .getPar(conf, "output.file.prefix"))
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


init_firstyear = function(input, file, parameters = NULL, output = NULL, 
                          log = "osmose.log", version = NULL, osmose = NULL, 
                          java = "java", options = NULL, verbose = TRUE, 
                          clean = TRUE, force = FALSE, run=TRUE, append=FALSE, ...) {
  
  parameters = paste("-Poutput.restart.enabled=TRUE", parameters)
  
  if(isTRUE(run)) {
    run_osmose(input=input, parameters=parameters, output=output, log=log,
               version=version, osmose=osmose, java=java,
               options=options, verbose=verbose, clean=clean, force=force)
  }
  
  rpath = file.path(output, "restart")
  conf = .readConfiguration(input)
  rfiles = dir(path=rpath, pattern = "\\.nc.*")
  nf = length(rfiles)

  spnames = unlist(.getPar(conf, par="species.name")[.getPar(conf, par="species.type")=="focal"])
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
  
  nsp = .getPar(conf, "simulation.nspecies")
  
  pars = list()
  for(isp in spindex) {
    
    ind = (spp == isp)
    iabundance = abundance[ind]
    ibiomass = biomass[ind]
    ilength = length[ind]
    itl = tl[ind]
    iage = age[ind]
    this = .getPar(conf, sp=isp)
    egg.size = .getPar(this, "egg.size")
    
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


init_sofia = function(input, file=NULL, test=FALSE, ...) {
  
  ow = options("warn")
  options(warn=1)
  on.exit(options(ow))
  
  conf = .readConfiguration(input)
  
  nsp = .getPar(conf, "simulation.nspecies")
  
  spind = .getPar(conf, "species.type") == "focal"
  spind = gsub(names(spind)[which(spind)], pattern="species.type.sp", replacement = "") 
  spnames = .getPar(conf, "species.name")[sprintf("species.name.sp%s", spind)]
  spind = sort(as.numeric(spind))
  
  out = vector("list", nsp)
  names(out) = spnames
  pars = list()
  
  for(sp in spind) {
    
    this = .getPar(conf, sp=sp)
    iSpName = .getPar(this, "species.name")
    
    cat(sprintf("\nInitializing species %d (%s)\n", sp, iSpName))
    
    sim = list()
    sim$cal       = read.cal(conf, sp)
    sim$biomass   = read.biomass(conf, sp)
    sim$yield     = read.yield(conf, sp)
    sim$fecundity = read.fecundity(conf, sp)
    sim$bioguess  = .getPar(.getPar(conf, sp=sp), "observed.biomass.guess")
    isp = sprintf("osmose.initialization.data.sp%d", sp)
    conf[[isp]]   = sim
    
    this = .getPar(conf, sp=sp)
    
    sim = .simF_ini(conf, sp, test=test)
    sim$nschool = .getPar(this, "simulation.nschool")
    if(is.null(sim$nschool))
      stop(sprintf("Parameter 'simulation.nschool.sp%d' not found.", sp))
    sim$osmose = .initial_length_dist(sim, sp)
    pars[[iSpName]] = as.matrix(sim$osmose)
    out[[iSpName]] = sim
    
  }
  
  # pars = as.data.frame(pars)
  # colnames(pars) = NULL
  # pars = pars[order(rownames(pars)), ]
  
  xoutput = list(par=pars, init=out)
  class(xoutput) = "osmose.initialization"
  
  return(invisible(xoutput))
  
}


# Simulation --------------------------------------------------------------


.simCatch_ini = function(conf, sp) {
  
  this = .getPar(conf, sp=sp)
  dat  = .getPar(this, par="osmose.initialization.data")
  ndt = conf$simulation.time.ndtperyear
  A = .getPar(this, "species.lifespan")
  a = .getPar(this, "species.length2weight.condition.factor")
  b = .getPar(this, "species.length2weight.allometric.power")
  
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

  trans = rebinning(CAL$bins, VB(age_bins, this, method=3))
  cal = CAL$mat %*% trans
  weight = a*size^b
  yield_obs   = rep((1e-6*rowSums(t(t(cal)*weight)))[1:ndt], length=T)
  
  M = calculateMortality(conf, sp)
  Ma = M$M[cut(age, breaks = M$age, labels = FALSE)]
  
  
  xn = which.max(size >= .getPar(this, "observed.biomass.cutoff.size")) - 1
  
  ini = exp(-cumsum(c(0,Ma[-length(Ma)]/ndt)))
  inibio = 1e-6*sum(ini*weight)
  R = ndt*bioguess/inibio
  ini = R*ini/sum(ini)
  
  if(bioguess==0) {
    # species not starting in the simulation
    output = list(pop=rep(0, C), catch=rep(0, C), R=0, 
                  biomass=rep(0, ndt), yield=rep(0, ndt), F=rep(0, ndt),
                  dist = rep(0, C), distB = rep(0, C), 
                  selectivity=rep(0, C), age=age, size=size,
                  Fguess=0, observed=list(biomass=bioguess, yield=yield_obs[1:ndt]),
                  bins=list(age=age_bins, size=size_bins), harvested=FALSE, larvalM=0)
    
    class(output) = c("osmose.init", class(output))
    
    return(output)
    
  }
  
  for(ix in 1:5) {
    
    pop = matrix(0, nrow=T, ncol=C)
    catch = matrix(NA, nrow=T, ncol=C)
    pop[1, ] = ini
    pop[ , 1] = R*rep(rF, length=T)
    
    for(t in 1:T) {
      it = ((t-1) %% ndt) + 1
      MID = pop[t, ]*exp(-0.5*Ma/ndt) # first half
      MID.C = pmax(MID - cal[it, ], 0.01*MID) # remove as much catch as possible, CHECK
      catch[t, ] = MID - MID.C
      END = MID.C*exp(-0.5*Ma/ndt) # second half
      if(t!=T) pop[t+1, 2:C] = head(END, -1)   
    }
    
    ixn = seq_len(ncol(pop))
    ixn = if(xn>0) tail(ixn, -xn) else ixn 
    biopop = 1e-6*t(t(pop)*weight)[, ixn]
    biomass = rowSums(biopop)
    bioref = mean(tail(biomass, ndt))
    yield   = 1e-6*rowSums(t(t(catch)*weight))
    
    Fguess = tail(yield_obs, ndt)/tail(biomass, ndt)
    Fseason = Fguess/sum(Fguess)
    if(all(is.na(Fseason))) Fseason = rep(1, ndt)/ndt
    
    Rupb = bioguess/bioref
    Rupc = mean(tail(yield_obs/yield, ndt))
    
    inc = weighted.mean(c(Rupb, Rupc), w = c(1, 2), na.rm=TRUE)
    R = R*inc
    
    ini = inc*pop[T - ndt + 1 ,]
    
  }
  
  xind = T - ndt + 1
  pind = xind + seq_len(ndt) - 1
  
  es = colSums(catch[pind, ])/colMeans(pop[pind, ])
  
  Fguess = sum(yield[pind]/biomass[pind])
  
  harvested = !all(yield[pind]< 1e-3) 
  
  output = list(pop=pop[pind, ], catch=catch[pind, ], R=R, 
                biomass=biomass[pind], yield=yield[pind], F=Fseason,
                dist = pop[xind ,], distB = 1e-6*pop[xind ,]*weight, 
                selectivity=es, age=age, size=size,
                Fguess=Fguess, observed=list(biomass=bioguess, yield=yield_obs[1:ndt]),
                bins=list(age=age_bins, size=size_bins), harvested=harvested)
  
  isMature = size >= .getPar(this, "species.maturity.size")
  eggs   = rowSums(1e6*fecundity[1:ndt]*t(t(output$pop)*isMature))
  larvae = output$R*rF
  Mlarval = mean(-log(larvae/eggs), na.rm=TRUE)
  
  output$larvalM = Mlarval
  
  class(output) = c("osmose.init",class(output))
  return(output)
  
}

.simF_ini = function(conf, sp, tiny=1e-3, cv=c(0.1, 0.1), test=FALSE) {
  
  sim = .simCatch_ini(conf, sp)
  if(!isTRUE(sim$harvested) & !is.null(sim)) return(sim)
  if(isTRUE(test)) return(sim)
  
  this = .getPar(conf, sp=sp)
  dat  = .getPar(this, par="osmose.initialization.data")
  ndt = conf$simulation.time.ndtperyear
  A = .getPar(this, "species.lifespan")
  a = .getPar(this, "species.length2weight.condition.factor")
  b = .getPar(this, "species.length2weight.allometric.power")
  
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
  
  yield_obs   = dat$yield
  yield_obs   = rep(yield_obs[1:ndt], length=T)
  
  weight = a*size^b
  M = calculateMortality(conf, sp)
  Ma = M$M[cut(age, breaks = M$age, labels = FALSE)]
  
  xn = which.max(size >= .getPar(this, "observed.biomass.cutoff.size")) - 1
  
  if(is.null(sim)) {
    
    sel = .getSelectivity(size, this) # creado con parametros
    Fseason = yield_obs[1:ndt]/sum(yield_obs[1:ndt])
    ini = exp(-cumsum(c(0,Ma[-length(Ma)]/ndt)))
    inibio = 1e-6*sum(ini*weight)
    R = ndt*bioguess/inibio
    xdist = R*ini/sum(ini)
    
    sim = list(R=R, Fguess=sum(yield_obs[1:ndt]/bioguess))
    
  } else {
    
    es = empirical_selectivity(matrix(sim$selectivity, nrow=1), fleet = "sim",
                               years = 1, bins = sim$size)
    ss_emp = suppressMessages(fit_selectivity(es, pattern=27, k=5))
    sel = ss_emp$selectivity
    Fseason = sim$F
    xdist = sim$dist
    
  }
  
  sel[sel<tiny] = 0
  
  .simF = function(par, value=FALSE) {
    
    R = exp(par[1])
    F = exp(par[2])
    
    pop = matrix(0, nrow=T, ncol=C)
    catch = matrix(NA, nrow=T, ncol=C)
    pop[1, ] = xdist
    pop[ , 1] = R*rep(rF, length=T)
    
    for(t in 1:T) {
      it = ((t-1) %% ndt) + 1
      Ft = F*Fseason[it]*as.numeric(sel)
      Zt = Ma/ndt + Ft
      tmp = pop[t, ]*exp(-Zt) 
      catch[t, ] = pop[t, ]*(Ft/Zt)*(1-exp(-Zt)) 
      if(t!=T) pop[t+1, 2:C] = head(tmp, -1)   
    }
    
    ixn = seq_len(ncol(pop))
    ixn = if(xn>0) tail(ixn, -xn) else ixn 
    biopop = 1e-6*t(t(pop)*weight)[, ixn]
    biomass = rowSums(biopop)
    yield   = 1e-6*rowSums(t(t(catch)*weight))
    
    ll_biomass  = lnorm2(biofit, tail(biomass, ndt))
    ll_yield    = lnorm2(yield_obs[1:ndt] , tail(yield, ndt))
    
    ll = ll_biomass*llw(cv[1]) + ll_yield*llw(cv[2])
    
    if(!isTRUE(value)) return(ll)
    
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
  
  opt = calibrate(par=log(c(sim$R, sim$Fguess)), fn = .simF, method = "L-BFGS-B")
  
  output = c(.simF(opt$par, value=TRUE),  opt=list(opt))
  
  isMature = size >= .getPar(this, "species.maturity.size")
  eggs   = rowSums(1e6*fecundity[1:ndt]*t(t(output$pop)*isMature))
  larvae = output$R*rF
  Mlarval = mean(-log(larvae/eggs), na.rm=TRUE)
  
  output$larvalM = Mlarval
    
  class(output) = c("osmose.init", class(output))
  return(output)
  
}


# Internal ----------------------------------------------------------------


lnorm2 = function(obs, sim, tiny=1e-2, ...) {
  if(all(!is.finite(sim))) return(Inf)
  obs = log(obs + tiny)
  sim = log(sim + tiny)
  nlogLike = sum((obs-sim)^2, na.rm=TRUE)
  return(nlogLike)
}

llw = function(cv) 1/(2*cv^2)

.initial_length_dist = function(sim, sp) {
  
  dist = sim$distB
  dist[dist==0] = 1e-3 # 1kg instead of nothing
  bio_ini = sum(dist)
  bio_rel = if(bio_ini==0) dist else dist/bio_ini
  tl_sp = rep(2, length(dist))
  xage  = sim$age
 
  # begin test 
  
  # end test
   
  rel_dist = sim$dist/max(sim$dist, na.rm=TRUE)
  
  nschool = pmax(ceiling(sim$nschool*rel_dist), 1)
  
  out = c(round(bio_ini, 1), 
          paste(format(bio_rel, scientific = FALSE), collapse=", "), 
          paste(round(sim$bins$size,3), collapse=","),
          paste(xage, collapse=","),
          paste(round(tl_sp, 2), collapse=", "),
          paste(nschool, collapse = ", "),
          round(sim$larvalM, 3))
  dim(out) = c(length(out), 1)
  
  out = as.data.frame(out)
  rownames(out) = sprintf(c("population.initialization.biomass.sp%d",
                            "population.initialization.relativebiomass.sp%d",
                            "population.initialization.size.sp%d",
                            "population.initialization.age.sp%d",
                            "population.initialization.tl.sp%d",
                            "population.initialization.nschool.sp%d",
                            "mortality.additional.larva.rate.sp%d"), sp)
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
