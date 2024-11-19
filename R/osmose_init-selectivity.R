
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

.nonNullPoints = function(y, thr, span) {
  # copy y
  
  if(length(span)==1) span = c(span, span)
  if(any(span<0)) stop("span must be positive.")
  
  y[is.na(y)] = 0
  yx = cumsum(y)/sum(y)
  
  ind0 = which.min(yx<thr/2)
  ind1 = which.max(yx>=(1-thr/2))
  
  imin = which.max(y>0)
  imax = length(y) - which.max(rev(y)>0) + 1
  
  ind = c(ind0, ind1)
  ind = seq(from=ind[1]-span[1], to=ind[2]+span[2], by=1)
  ind = pmin(pmax(ind, imin), imax)
  ind = sort(unique(ind))
  return(ind)
}

