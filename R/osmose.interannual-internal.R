

# Fishing -----------------------------------------------------------------

# Writes fishing files for individual species.
#
# @param sp Species number (sp0, sp1, etc)
# @param fishing Fishing parameters (method, etc.) 
# @param species Species parameters
# @param T Number of years
# @param ndt  Time step
# @param output Output path
.writeFishingFile = function(sp, fishing, species, T, ndt, output) {
  
  if(is.null(fishing$rate$method[[sp]])) return(NULL)
  
  fileCode = file.path(output, "fishing_%s_%s.csv")
  
  f = getFishingMortality(sp, fishing, T, ndt) 
  write.osmose(f, file=sprintf(fileCode, "byDt", species$name[sp]))
  
  selectivity = getSelectivity(sp, fishing, T, ndt)
  if(!is.null(selectivity)) {
    Fs = f %o% selectivity   # outer product. if dim(f)=N and dim(sel)=M, dim(Fs)=NxM
    #write.osmose(f, file=sprintf(fileCode, "byDt", species$name[sp]))
    write.osmose(Fs, file=file.path(output, paste0("F-", sp, ".csv")))
    
  }
  
  # fishing = cbind(fishing, f)    ????
  # write.osmose(Fs, file=file.path(output, paste0("F-", isp, ".csv")))
  
}


# Selectivity functions ---------------------------------------------------


getSelectivity = function(par, sp, n=1, tiny=1e-6) {
  out = calculateSelectivity(par=getSelectivityParameters(par=par, sp=sp), n=n, tiny=tiny)
  return(out)
}

getSelectivityParameters = function(par, sp) {
  
  output = list()
  output = within(output, {
    
    by        = par$selectivity.by[sp]
    type      = par$selectivity.type[sp]
    longevity = par$longevity[sp]
    Linf      = par$Linf[sp]
    L50       = par$L50[sp]
    L75       = par$L75[sp]
    
  })
  
  return(output)
  
}

calculateSelectivity = function(par, n=1, tiny=1e-6) {
  
  x = switch(par$by, 
             size   = pretty(c(0, 1.1*par$Linf), n=60*n),
             age    = pretty(c(0, 1.1*par$longevity), n=50*n),
             stop("Invalid selectivity type: by must be 'age' or 'length' ")
  )
  
  par$L75 = max(1.01*par$L50, par$L75)
  
  out = switch(par$type,
               log  = .selectivity.log(x=x, L50=par$L50, L75=par$L75, tiny=tiny),
               norm = .selectivity.norm(x=x, L50=par$L50, L75=par$L75, tiny=tiny),
               lnorm = .selectivity.lnorm(x=x, L50=par$L50, L75=par$L75, tiny=tiny),
               edge = .selectivity.edge(x=x, L50=par$L50),
               stop("Invalid selectivity 'type': currently implemented 'log' and 'edge'. See help.")
  )
  
  return(out)
  
}

.selectivity.edge = function(x, L50) {
  
  selec = numeric(length(x))
  selec[x >= L50] = 1
  names(selec) = x
  return(selec)
}

.selectivity.log = function(x, L50, L75, tiny=1e-6) {
  
  s1 = (L50*log(3))/(L75-L50)
  s2 = s1/L50
  selec = 1/(1+exp(s1-(s2*x)))
  selec[selec<tiny] = 0
  names(selec) = x
  return(selec)
  
}

.selectivity.norm = function(x, L50, L75, tiny=1e-6) {
  
  sd = (L75-L50)/qnorm(0.75)
  mean = L50
  selec = dnorm(x, mean=mean, sd=sd)
  selec = selec/max(selec, na.rm=TRUE)
  selec[selec<tiny] = 0
  names(selec) = x
  return(selec)
  
}

.selectivity.lnorm = function(x, L50, L75, tiny=1e-6) {
  
  sd = log(L75/L50)/qnorm(0.75)
  mean = log(L50)
  selec = dlnorm(x, meanlog = mean, sdlog = sd)
  selec = selec/max(selec, na.rm=TRUE)
  selec[selec<tiny] = 0
  names(selec) = x
  return(selec)
  
}


.selectivity.equilibrium = function(x, M, tiny=1e-6) {
  selec = exp(-M*x)
  names(selec) = x
  return(selec)
}