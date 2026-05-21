
# Growth ------------------------------------------------------------------

VB = function(age, this, method=3) {
  
  k = get_par(this, "species.k") 
  Linf = get_par(this, "species.linf")
  t0 = get_par(this, "species.t0")
  t_thr = get_par(this, "species.vonbertalanffy.threshold.age")
  L_egg = get_par(this, "species.egg.size")
  L_thr = Linf*(1 - exp(-k*(t_thr - t0)))
  
  spname = get_par(this, "species.name")
  
  msg = "Value not found for growth parameter '%s' (%s)."
  if(is.null(k)) stop(sprintf(msg, "k", spname))
  if(is.null(Linf)) stop(sprintf(msg, "Linf", spname))
  if(is.null(t0)) stop(sprintf(msg, "t0", spname))
  if(is.null(t_thr)) stop(sprintf(msg, "vonbertalanffy.threshold.age", spname))
  if(is.null(L_egg)) stop(sprintf(msg, "egg.size", spname))
  
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
  
  k = get_par(this, "species.k") 
  Linf = get_par(this, "species.linf")
  t0 = get_par(this, "species.t0")
  t_thr = get_par(this, "species.vonbertalanffy.threshold.age")
  L_egg = get_par(this, "species.egg.size")
  L_thr = Linf*(1 - exp(-k*(t_thr - t0)))
  A = get_par(this, "species.lifespan")
  
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
  this = get_par(conf, sp=sp)
  
  a = get_par(this, "species.length2weight.condition.factor")
  b = get_par(this, "species.length2weight.allometric.power")
  tn = get_par(this, "species.lifespan")
  
  age = seq(from=0+0.5/ndt, to=tn, by=1/ndt)
  size = VB(age, this, method=3)
  weight = a*size^b
  
  msg = sprintf("Failure in Mean Lifetime Fecundity calculation, check growth parameters for %s.", get_par(this, "species.name"))
  
  if(any(is.na(weight))) stop(msg, call. = FALSE)
  
  repfile = get_par(this, "reproduction.season.file")
  mode = get_par(this, "species.reproduction.mode")
  if(is.null(mode)) mode = "oviparity"
  
  fecundity = read.fecundity(conf = conf, sp=sp)
  
  matsize = .get_matsize(this)
  isMature = size >= matsize
  
  if(all(!isMature)) {
    msg0 = sprintf("Failure in Mean Lifetime Fecundity calculation, check maturity and growth parameters for %s.", 
                  get_par(this, "species.name"))
    msg1 = sprintf("No mature individuals are produced, maximum length in the model (%0.2f cm) is lower than maturity size (%0.2f cm).", 
                   max(size), matsize)
    stop(paste(msg0, msg1, collapse="/n"), call. = FALSE)
  }
  
  if(mode=="viviparity") weight=1 # MLF use absolute fecundity
  MLF = .calculateMLF(fecundity, isMature, weight)
  
  return(MLF)
  
}

.get_matsize = function(this, method=3) {
  matsize = get_par(this, "species.maturity.size")
  if(is.null(matsize)) matsize = get_par(this, "species.maturity.l50")
  if(is.null(matsize)) {
    matsize = VB(get_par(this, "species.maturity.age"), this, method=method)
  }
  msg0 = sprintf("No maturity parameters for %s.", get_par(this, "species.name"))
  if(is.null(matsize)) stop(msg0)
  return(matsize)
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
  
  tsMLF = calculateMLF(conf, sp=sp)
  MLF = mean(tsMLF) 
  
  this = get_par(conf, sp=sp)
  
  rmode = get_par(this, "species.reproduction.mode") # is viviparous?
  d1 = get_par(this, "species.egg.stage.duration") # days
  if(is.null(d1)) d1 = 2
  d1 = d1/365 # transformed to years
  
  tn    = get_par(this, "species.lifespan")
  ratio = get_par(this, "species.sexratio")
  ndt   = get_par(conf, "time.ndtPerYear")
  
  ind = sapply(2:20, .getM, ratio=ratio, MLF=MLF, tn=tn, d1=d1, value=FALSE)
  n = which.min(ind)
  
  tmp = .getM(n=n, ratio=ratio, MLF=MLF, tn=tn, d1=d1)
  Mi = tmp$Mi
  di = tmp$di
  
  ti = c(0, cumsum(di))
  
  size = VB(ti, this, method=3)
  size[length(size)] = Inf
  
  xndt = 15*ndt
  xage = seq(from=0+0.5/xndt, to=tn, by=1/xndt)

  # average mortality by age in time steps of the model
  m = colMeans(matrix(Mi[cut(xage, breaks=ti, labels = FALSE)], nrow=15))
  
  out = list(age=ti, size=size, M=Mi, Mage=m)
  
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





