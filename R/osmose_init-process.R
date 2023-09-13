
# Growth ------------------------------------------------------------------

VB = function(age, this, method=3) {
  
  k = .getPar(this, "species.k") 
  Linf = .getPar(this, "species.linf")
  t0 = .getPar(this, "species.t0")
  t_thr = .getPar(this, "species.vonbertalanffy.threshold.age")
  L_egg = .getPar(this, "species.egg.size")
  L_thr = Linf*(1 - exp(-k*(t_thr - t0)))
  
  if(method==0) {
    # original VB
    l = ifelse(age < 0, NA, Linf*(1 - exp(-k*(age - t0))))
    return(l)
  }
  if(method==1) {
    # linear approximation (current OSMOSE)
    a = (L_thr - L_egg)/t_thr
    b = L_egg
    l = ifelse(age < t_thr, a*age + b, Linf*(1 - exp(-k*(age - t0))))
    return(l)
  }
  if(method==2) {
    # cuadratic (match up to first derivative)
    a = k*(Linf - L_thr)/t_thr - L_thr/(t_thr)^2
    b = -k*(Linf - L_thr) + 2*L_thr/t_thr
    c = L_egg
    l = ifelse(age < t_thr, a*age^2 + b*age + c, Linf*(1 - exp(-k*(age - t0))))
    return(l)
  }
  if(method==3) {
    # cubic (match up to second derivative)
    a = (L_thr - L_egg)/t_thr^3 - k*(Linf - L_thr)/t_thr^2 - (k^2/2)*(Linf - L_thr)/t_thr
    b = (k^2 + 3*k/t_thr)*(Linf - L_thr) - 3*(L_thr - L_egg)/t_thr^2  
    c = -2*k*(Linf - L_thr) - (k^2/2)*(Linf - L_thr)*t_thr + 3*(L_thr - L_egg)/t_thr
    d = L_egg
    l = ifelse(age < t_thr, a*age^3 + b*age^2 + c*age + d, Linf*(1 - exp(-k*(age - t0))))
    return(l)
  }
  stop("Incorrect 'method' specification.")
}

VB_inv = function(size, this, method=3) {
  
  k = .getPar(this, "species.k") 
  Linf = .getPar(this, "species.linf")
  t0 = .getPar(this, "species.t0")
  t_thr = .getPar(this, "species.vonbertalanffy.threshold.age")
  L_egg = .getPar(this, "species.egg.size")
  L_thr = Linf*(1 - exp(-k*(t_thr - t0)))
  A = .getPar(this, "species.lifespan")
  
  .invVB = function(size, k, Linf, t0, A) {
    out = suppressWarnings((-1/k)*log(1-size/Linf) + t0)
    out[out>A | is.na(out)] = A
    return(out)
  }
  
  if(method==0) {
    # original VB
    age = ifelse(size < 0, NA, .invVB(size, k, Linf, t0, A))
    return(age)
  }
  if(method==1) {
    # linear approximation (current OSMOSE)
    a = (L_thr - L_egg)/t_thr
    b = L_egg
    age = ifelse(size < L_thr, (size-b)/a, .invVB(size, k, Linf, t0, A))
    return(age)
  }
  if(method==2) {
    # cuadratic (match up to first derivative)
    a = k*(Linf - L_thr)/t_thr - L_thr/(t_thr)^2
    b = -k*(Linf - L_thr) + 2*L_thr/t_thr
    c = L_egg - size
    athr = suppressWarnings(cbind((-b + sqrt(b^2 - 4*a*c))/(2*a),
                                  (-b - sqrt(b^2 - 4*a*c))/(2*a)))
    athr[athr>t_thr | is.nan(athr)] = NA
    athr = suppressWarnings(apply(athr, 1, min, na.rm=TRUE))
    age = ifelse(size < L_thr, athr, .invVB(size, k, Linf, t0, A))
    return(age)
  }
  if(method==3) {
    # cubic (match up to second derivative)
    a = (L_thr - L_egg)/t_thr^3 - k*(Linf - L_thr)/t_thr^2 - (k^2/2)*(Linf - L_thr)/t_thr
    b = (k^2 + 3*k/t_thr)*(Linf - L_thr) - 3*(L_thr - L_egg)/t_thr^2  
    c = -2*k*(Linf - L_thr) - (k^2/2)*(Linf - L_thr)*t_thr + 3*(L_thr - L_egg)/t_thr
    d = L_egg - size
    # solution to the cubic, guessing is the real one!
    D0 = b^2 - 3*a*c
    D1 = 2*b^3 - 9*a*b*c + 27*a^2*d
    C = ((D1 + sqrt(D1^2 - 4*D0^3))/2)^(1/3)
    athr = -(b + C + D0/C)/(3*a)
    age = ifelse(size < L_thr, athr, .invVB(size, k, Linf, t0, A))
    return(age)
  }
  stop("Incorrect 'method' specification.")
}


# Reproduction ------------------------------------------------------------

calculateMLF = function(conf, sp) {
  
  .calculateMLF = function(fecundity, isMature, weight) {
    
    .mlf = function(delay, fecundity, isMature, weight) {
      MLF = sum(isMature*fecundity[delay + seq_along(isMature)]*weight)
      return(MLF)
    }
    
    nfec  = length(fecundity)
    nlife = length(isMature)
    if(nfec <= nlife) {
      fecundity = rep(fecundity, length=nlife+nfec)
      niter = nfec
    } else {
      niter = nfec - nlife
    }
    nfec = length(fecundity)
    
    out = sapply(0:niter, .mlf, fecundity=fecundity, isMature=isMature, 
                 weight=weight)
    return(out)
  }
  
  ndt = conf$simulation.time.ndtperyear
  this = .getPar(conf, sp=sp)
  
  a = .getPar(this, "species.length2weight.condition.factor")
  b = .getPar(this, "species.length2weight.allometric.power")
  tn = .getPar(this, "species.lifespan")
  
  age = seq(from=0+0.5/ndt, to=tn, by=1/ndt)
  size = VB(age, this, method=3)
  weight = a*size^b
  
  repfile = .getPar(this, "reproduction.season.file")
  
  fecundity = read.fecundity(conf = conf, sp=sp)
  
  isMature = size >= .getPar(this, "species.maturity.size")
  
  MLF = .calculateMLF(fecundity, isMature, weight)
  
  return(MLF)
  
}


# Mortality ---------------------------------------------------------------


calculateMortality = function(conf, sp) {
  
  .getM = function(n, ratio, MLF, tn, d1, value=TRUE) {
    G = -log((1/ratio)/MLF)/n
    alpha = (tn/d1)^(1/(n-1)) - 1
    di = c(d1, alpha*(1+alpha)^(2:n - 2)*d1)
    Mi = G/di
    if(isTRUE(value)) return(list(Mi=Mi, di=di))
    return(all(diff(Mi) < 0))
  }
  
  this = .getPar(conf, sp=sp)
  d1 = .getPar(this, "species.egg.stage.duration") # days
  if(is.null(d1)) d1 = 2
  d1 = d1/365 # transformed to years
  
  tsMLF = calculateMLF(conf, sp=sp)
  MLF = mean(tsMLF) 
  
  this = .getPar(conf, sp=sp)
  
  tn = .getPar(this, "species.lifespan")
  ratio = .getPar(this, "species.sexratio")
  
  ind = sapply(2:20, .getM, ratio=ratio, MLF=MLF, tn=tn, d1=d1, value=FALSE)
  n = which.min(ind)
  
  tmp = .getM(n=n, ratio=ratio, MLF=MLF, tn=tn, d1=d1)
  Mi = tmp$Mi
  di = tmp$di
  
  ti = cumsum(di)
  
  size = VB(c(0, ti), this, method=3)
  size[length(size)] = Inf
  out = list(age = c(0, ti), size=size, M=Mi)
  return(out)
  
}


# Internal ----------------------------------------------------------------

rebinning = function(x, y) {
  
  if(is.list(y)) stop("'y' must be a single vector.")
  if(any(diff(y)==0)) stop("All length bins must be different, check growth parameters.")
  
  .rebinning = function(x, y, k=10000) {
    .mini = function(x, k=100) head(approx(x=x, n=k*length(x)-(k-1))$y, -1)
    .mytable = function(x, levels) table(factor(x, levels=levels))
    xm = cbind(head(x, -1), tail(x, -1))
    mini = apply(xm, 1, .mini, k=k)
    out = cut(mini, breaks=y, right = FALSE, include.lowest=TRUE)
    levels = levels(out)
    out = matrix(out, ncol=k, byrow = TRUE)
    xout = t(apply(out, 1, .mytable, levels=levels))/k
    rownames(xout) = levels(cut(x, breaks = x, right = FALSE, include.lowest=TRUE))
    return(xout)
  }
  
  if(!is.list(x)) return(.rebinning(x, y))
  
  return(lapply(x, .rebinning, y=y))
  
}





