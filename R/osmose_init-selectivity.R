
.getSelectivity = function(size, this) {
  
  par = list(type = .getPar(this, "fisheries.selectivity.type"),
             L50  = .getPar(this, "fisheries.selectivity.l50"),
             L75  = .getPar(this, "fisheries.selectivity.l75"),
             tiny = .getPar(this, "fisheries.selectivity.tiny"))
  
  if(is.null(par$tiny)) par$tiny = 1e-3
  
  return(.calculateSelectivity(x=size, par=par))
  
}

.calculateSelectivity = function(x, par) {
  
  par$L75 = max(1.01*par$L50, par$L75)
  
  out = switch(par$type,
               logistic   = .selectivity_log(x=x, L50=par$L50, L75=par$L75, tiny=par$tiny),
               gaussian   = .selectivity_norm(x=x, L50=par$L50, L75=par$L75, tiny=par$tiny),
               lnorm      = .selectivity_lnorm(x=x, L50=par$L50, L75=par$L75, tiny=par$tiny),
               knife_edge = .selectivity_edge(x=x, L50=par$L50),
               stop("Invalid selectivity 'type': currently implemented 'logistic',
                    'gaussian', 'lnorm', and 'knife-edge'. See help.")
  )
  
  return(out)
  
}

# Internal ----------------------------------------------------------------


.selectivity_edge = function(x, L50) {
  
  selec = numeric(length(x))
  selec[x >= L50] = 1
  names(selec) = x
  return(selec)
}

.selectivity_log = function(x, L50, L75, tiny=1e-6) {
  
  s1 = (L50*log(3))/(L75-L50)
  s2 = s1/L50
  selec = 1/(1+exp(s1-(s2*x)))
  selec[selec<tiny] = 0
  names(selec) = x
  return(selec)
  
}

.selectivity_norm = function(x, L50, L75, tiny=1e-6) {
  
  sd = (L75-L50)/qnorm(0.75)
  mean = L50
  selec = dnorm(x, mean=mean, sd=sd)
  selec = selec/max(selec, na.rm=TRUE)
  selec[selec<tiny] = 0
  names(selec) = x
  return(selec)
  
}

.selectivity_lnorm = function(x, L50, L75, tiny=1e-6) {
  
  sd = log(L75/L50)/qnorm(0.75)
  mean = log(L50)
  selec = dlnorm(x, mean=mean, sd=sd)
  selec = selec/max(selec, na.rm=TRUE)
  selec[selec<tiny] = 0
  names(selec) = x
  return(selec)
  
}

