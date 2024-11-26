
.getSelectivity = function(size, this) {
  
  par = list(type    = .getPar(this, "fisheries.selectivity.type"),
             L50     = .getPar(this, "fisheries.selectivity.l50"),
             L75     = .getPar(this, "fisheries.selectivity.l75"),
             L25     = .getPar(this, "fisheries.selectivity.l25"),
             L0      = .getPar(this, "fisheries.selectivity.l0"),
             L1      = .getPar(this, "fisheries.selectivity.l1"),
             plateau = .getPar(this, "fisheries.selectivity.plateau"),
             breaks  = .getPar(this, "fisheries.selectivity.breaks"),
             values  = .getPar(this, "fisheries.selectivity.values"),
             tiny    = .getPar(this, "fisheries.selectivity.tiny"))
  
  if(is.null(par$tiny)) par$tiny = 1e-3
  
  return(.calculateSelectivity(x=size, par=par))
  
}

.calculateSelectivity = function(x, par) {
  
  par$L75 = max(1.01*par$L50, par$L75)
  
  out = switch(as.character(as.integer(par$type)),
               "0"  = .selectivity_edge(x=x, L50=par$L50),
               "1"  = .selectivity_log(x=x, L50=par$L50, L75=par$L75, tiny=par$tiny),
               "2"  = .selectivity_norm(x=x, L50=par$L50, L75=par$L75, tiny=par$tiny),
               "3"  = .selectivity_lnorm(x=x, L50=par$L50, L75=par$L75, tiny=par$tiny),
               "4"  = .selectivity_doublenorm(x=x, L50=par$L50, L75=par$L75, L25=par$L25, 
                                             tiny=par$tiny), # 3-par
               "5"  = .selectivity_doublenorm(x=x, L50=par$L50, L75=par$L75, L25=par$L25, 
                                             plateau=par$plateau, tiny=par$tiny), # 4-par
               "6"  = .selectivity_doublenorm(x=x, L50=par$L50, L75=par$L75, L25=par$L25, 
                                             plateau=par$plateau, L1=par$L1, tiny=par$tiny), # 5-par
               "7"  = .selectivity_doublenorm(x=x, L50=par$L50, L75=par$L75, L25=par$L25, 
                                             plateau=par$plateau, L1=par$L1, L0=par$L0, tiny=par$tiny), # 6-par
               "9" = .selectivity_nonpar(x, values=par$values, breaks=par$breaks),
               stop("Invalid selectivity 'type': currently implemented 'logistic',
                    'gaussian', 'lnorm', and 'knife-edge'. See help.")
  )
  
  return(out)
  
}

# Internal ----------------------------------------------------------------

# 0: knife-edge
.selectivity_edge = function(x, L50) {
  
  selec = numeric(length(x))
  selec[x >= L50] = 1
  names(selec) = x
  return(selec)
}

# 1: sigmoidal/logistic
.selectivity_log = function(x, L50, L75, tiny=1e-6) {
  
  s1 = (L50*log(3))/(L75-L50)
  s2 = s1/L50
  selec = 1/(1+exp(s1-(s2*x)))
  selec[selec<tiny] = 0
  names(selec) = x
  return(selec)
  
}

# 2: gaussian
.selectivity_norm = function(x, L50, L75, tiny=1e-6) {
  
  sd = (L75-L50)/qnorm(0.75)
  mean = L50 # mean and median!
  selec = dnorm(x, mean=mean, sd=sd)/dnorm(L50, mean=mean, sd=sd)
  selec[selec<tiny] = 0
  names(selec) = x
  return(selec)
  
}

.selectivity_probit = function(x, L50, L75, tiny=1e-6) {
  
  sd = (L75-L50)/qnorm(0.75)
  mean = L50
  selec = pnorm(x, mean=mean, sd=sd)
  selec = selec/max(selec, na.rm=TRUE)
  selec[selec<tiny] = 0
  names(selec) = x
  return(selec)
  
}

.selectivity_norm2 = function(x, L50, L75, tiny=1e-6) {
  
  sd = (L75-L50)/qnorm(0.75)
  mean = L50 # mean and median!
  selec = exp(-((x-mean)^2)/(2*sd^2))
  selec[selec<tiny] = 0
  names(selec) = x
  return(selec)
  
}

# 3: lognormal
.selectivity_lnorm = function(x, L50, L75, tiny=1e-6) {
  
  mean = log(L50) # L50 is the median
  sd   = log(L75/L50)/qnorm(0.75)
  mode = exp(mean - sd^2)
  
  selec = dlnorm(x, mean=mean, sd=sd)/dlnorm(mode, mean=mean, sd=sd)
  selec[selec<tiny] = 0
  names(selec) = x
  
  return(selec)
  
}

# 3: lognormal
.selectivity_lnorm2 = function(x, L50, L75, tiny=1e-6) {
  
  mean = log(L50) # L50 is the median
  sd   = log(L75/L50)/qnorm(0.75)
  mode = exp(mean - sd^2)
  
  selec = exp(-((log(x)-mean)^2)/(2*sd^2))*exp(mean-0.5*sd^2)/x
  selec[selec<tiny] = 0
  names(selec) = x
  
  return(selec)
  
}

# 4-7: double normal 
.selectivity_doublenorm = function(x, L50, L75, L25, plateau=0, L0=0, L1=Inf, tiny=1e-6) {
  
  mean1 = L50
  mean2 = mean1 + plateau
  sd1 = (L25-mean1)/qnorm(0.25)
  sd2 = (L75-mean2)/qnorm(0.75)
  
  selec1 = dnorm(x, mean=mean1, sd=sd1)/dnorm(mean1, mean=mean1, sd=sd1)
  selec2 = dnorm(x, mean=mean2, sd=sd2)/dnorm(mean2, mean=mean2, sd=sd2)
 
  selec = numeric(length(x))
  selec[] = 1
  selec[x <= mean1] = selec1[x <= mean1]
  selec[x >= mean2] = selec2[x >= mean2]
  
  if(L0 > 0) {
    # sL0 = 1
    # if(L0 < mean1) sL0 = dnorm(L0, mean=mean1, sd=sd1)/dnorm(mean1, mean=mean1, sd=sd1)
    # if(L0 > mean2) sL0 = dnorm(L0, mean=mean2, sd=sd2)/dnorm(mean2, mean=mean2, sd=sd2)
    selec[x < L0] = 0
  }
  
  if(L1 < Inf) {
    sL1 = 1
    if(L1 < mean1) sL1 = dnorm(L1, mean=mean1, sd=sd1)/dnorm(mean1, mean=mean1, sd=sd1)
    if(L1 > mean2) sL1 = dnorm(L1, mean=mean2, sd=sd2)/dnorm(mean2, mean=mean2, sd=sd2)
    selec[x > L1] = sL1
  }
   
  selec[selec<tiny] = 0
  names(selec) = x
  return(selec)
  
}

# 9: non-parametric selectivity
.selectivity_nonpar = function(x, values=par$values, breaks=par$breaks) {
  
  # complete
  
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

