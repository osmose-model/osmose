
# Auxiliar ----------------------------------------------------------------

#' Set values for a parameter list
#'
#' @param x The osmose.configuration list.
#' @param value The parameter value to replace.
#' @param scale All values in x are multiplied by this 'scale' units.
#' @param delta All values in x are shifted by this delta' units.
#' @param lower Minimum possible value after transformations (value and scale)
#' @param upper Maximum possible value after transformations (value and scale)
#' @param auto If auto=TRUE, scale is NOT applied to deviates and delta is ONLY applied to deviates.
#' @param digits Maximum number of digits in the result
#' @param ... Additional arguments
#'
#' @return The configuration list with parameter values changed.
#' @export
#'
set_par = function(x, value=NULL, scale=1, delta=0, lower=NULL, upper=NULL, digits=3, auto=FALSE, ...) {
  if(!is.null(lower) & !is.null(upper)) stop("Both 'lower' and 'upper' cannot be set.")
  xround = ifelse(scale<=1, -1, +1)
  if(!is.null(lower)) xround = -1
  if(!is.null(upper)) xround = +1
  if(digits==0) xround = 0
  x = as.relistable(x)
  val = unlist(x)
  xauto1 = rep(1, length(val))
  xauto2 = rep(1, length(val))
  if(isTRUE(auto)) {
    xauto2 = rep(0, length(val))
    xind = grep(names(val), pattern="deviate")
    xauto1[xind] = 0
    xauto2[xind] = 1
  }
  if(!is.null(value)) {
    if(length(value)==0) stop("'value' must be of length 1 or 2")
    if(length(value)>2) stop("'value' must be of length 1 or 2")
    if(length(value)==1) value = c(value, value + sign(value))
    val[] = ifelse(xauto1==1, value[1], value[2])
  } else {
    val = val - (1-scale)*abs(val)*xauto1 # is auto=TRUE, scale is NOT applied to deviates
    val = val + delta*xauto2 # is auto=TRUE, delta is ONLY applied to deviates
    fac = 10^ceiling(digits)
    val = floor(fac*val) + xround
    val = val/fac
    if(!is.null(lower)) val = pmin(val, lower, na.rm=TRUE)
    if(!is.null(upper)) val = pmax(val, upper, na.rm=TRUE)
  }
  val = relist(val)
  attr(val, "skeleton") = NULL
  class(val) = "osmose.configuration"
  return(val)
}

transform_par = function(x, FUN, nmfun=NULL, ...) {
  if(is.null(nmfun)) nmfun = deparse(substitute(FUN))
  FUN = match.fun(FUN)
  x = as.relistable(x)
  val = FUN(unlist(x))
  if(any(!is.finite(val))) {
    val[which(val == Inf)] = 999
    val[which(val == -Inf)] = -999
  }
  val = relist(val)
  nm = names(val)
  nm = gsub(nm, pattern="\\.sp", replacement=sprintf(".%s.sp", nmfun))
  nm = gsub(nm, pattern="\\.fsh", replacement=sprintf(".%s.fsh", nmfun))
  names(val) = nm
  class(val) = "osmose.configuration"
  return(val)
}

replicate_par = function(x, n) {
  .extend = function(x, n) {
    if(is.null(x)) return(x)
    if(length(x)==0) return(x)
    if(length(x)>1) return(x)
    return(rep(x, times=n))
  }
  out = lapply(x, FUN=.extend, n=n)
  class(out) = "osmose.configuration"
  return(out)
}

get_par2 = function(conf, par) {
  bg = get_par(conf, sprintf("calibration.%s.bygroup", par))
  if(isTRUE(bg)) {
    usedev = get_par(conf, sprintf("calibration.%s.usedeviates", par))
    if(is.null(usedev)) usedev = TRUE
    out = get_calibration_parameters(conf, par, usedev=usedev)
    if(is.null(out)) out = get_par(conf, sprintf("%s.sp", par), as.is=TRUE)
  } else {
    out = get_par(conf, sprintf("%s.sp", par), as.is=TRUE)
  }
  return(out)
}

get_par_phase = function(conf, par, default=-1) {
  phase = get_par(conf, sprintf("calibration.%s.phase", par))
  if(is.null(phase)) phase = default
  return(phase)
}

get_calibration_parameters = function(conf, par, usedev=TRUE) {
  
  gg = get_par(conf, sprintf("%s.group.sp", par), unlist=TRUE)
  if(is.null(gg)) return(NULL)
  ll = get_par(conf, sprintf("%s.sp", par), unlist=TRUE)
  
  dat0 = data.frame(code=sprintf("sp%s", get_species(conf, type="focal", code=TRUE)))
  dat = data.frame(code=gsub(names(ll), pattern=paste0(par, "."), replacement=""),
                   par=ll)
  datg = data.frame(code=gsub(names(gg), pattern=paste0(par, ".group."), replacement=""),
                    group=gg)
  dat0 = merge(dat0, dat, all.x=TRUE)
  dat0 = dat0[complete.cases(dat0), ] # all species with the parameter
  dat0 = merge(dat0, datg, all.x=TRUE) # groups added
  miss = sort(dat0$code[!complete.cases(dat0)])
  dat0 = dat0[complete.cases(dat0), ]
  means = tapply(dat0$par, INDEX=dat0$group, FUN=mean)
  dat0$base = means[dat0$group]
  dat0$base[is.na(dat0$base)] = dat0$par[is.na(dat0$base)] 
  dat0$deviate = dat0$par/dat0$base
  dat0$ind = as.numeric(gsub(dat0$code, pattern="sp", replacement=""))
  dat0 = dat0[order(dat0$ind), ]
  pmiss = setNames(dat$par, nm=dat$code)[miss]
  if(!isTRUE(usedev)) dat0$deviate = 1
  
  out = c(as.list(setNames(dat0$deviate, nm=sprintf("%s.deviate.%s", par, dat0$code))),
          as.list(setNames(means, nm=sprintf("%s.base.sp%s", par, names(means)))),
          as.list(setNames(pmiss, nm=sprintf("%s.%s", par, names(pmiss)))))
  class(out) = c("osmose.configuration", class(out))
  return(out)
}

get_osmose_parameter = function(par, conf, nm) {
  
  Lall = get_par(par, nm, as.is=TRUE, linear=TRUE)
  Lbase = get_par(Lall, ".base.", unlist=TRUE)
  if(is.null(Lbase)) return(par)
  par = get_par(par, nm, invert=TRUE, as.is=TRUE)
  Lall = get_par(Lall, ".base.", invert = TRUE, as.is=TRUE)
  Ldev = get_par(Lall, ".deviate.", unlist=TRUE)
  Lall = get_par(Lall, ".deviate", invert = TRUE, as.is=TRUE)
  
  gg = get_par(conf, sprintf("%s.group.sp", nm), unlist=TRUE)
  names(Lbase) = gsub(names(Lbase), pattern=sprintf("%s.base.sp", nm), replacement="")
  lgg = setNames(Lbase[gg], nm=names(gg))
  
  dat0 = data.frame(code=sprintf("sp%s", get_species(conf, type="focal", code=TRUE)))
  datg = data.frame(code=gsub(names(gg), pattern=paste0(nm, ".group."), replacement=""),
                    base=lgg, group=gg)
  datl = data.frame(code=gsub(names(Ldev), pattern=paste0(nm, ".deviate."), replacement=""),
                    deviate=Ldev)
  dat0 = merge(dat0, datg, all.x=TRUE)
  dat0 = merge(dat0, datl, all.x=TRUE)
  dat0$value = dat0$base*dat0$deviate
  dat0 = dat0[complete.cases(dat0), ] # all species with the parameter
  
  nn = sprintf("%s.%s", "random", nm)
  penalty = list()
  penalty[[nn]] = sqrt(sapply(split(log(dat0$deviate), f = dat0$group), FUN=function(x) sum(x^2)))
  
  out = as.list(setNames(dat0$value, nm=sprintf("%s.%s", nm, dat0$code)))
  class(out) = c("osmose.configuration", class(out))
  out = c(par, out, Lall, penalty)
  return(out)
  
}

write_osmose_parameter = function(conf, par, FUN=NULL, files, scale, lower, upper, delta, default) {
  
  if(!is.null(FUN)) nmfun = deparse(substitute(FUN))
  FUN = match.fun(FUN)
  guess_file = files["guess"] 
  min_file   = files["min"]
  max_file   = files["max"]
  phase_file = files["phase"]
  
  pars = get_par2(conf, par)
  if(!is.null(FUN)) pars = transform_par(pars, FUN=FUN, nmfun=nmfun)
  phase = get_par_phase(conf, par, default=default)
  
  write_osmose(pars, file=guess_file, append = TRUE)
  write_osmose(set_par(pars, scale=min(scale), lower=lower, delta=-delta, auto=TRUE), file=min_file, append = TRUE)
  write_osmose(set_par(pars, scale=max(scale), upper=upper, delta=+delta, auto=TRUE), file=max_file, append = TRUE)
  write_osmose(set_par(pars, value=phase, auto=TRUE), file=phase_file, append = TRUE)
  
  return(invisible(TRUE))
}

#' @exportS3Method relist osmose.configuration
relist.osmose.configuration = function(flesh, skeleton = attr(flesh, "skeleton")) {
  ind = 1L
  result = skeleton
  for (i in seq_along(skeleton)) {
    size = length(unlist(result[[i]]))
    result[[i]] = relist(flesh[seq.int(ind, length.out = size)], 
                          result[[i]])
    ind = ind + size
  }
  return(result)
}

mcat = function(..., file = "", sep = " ", fill = FALSE, labels = NULL,
                append = FALSE) {
  for(ifile in file) {
    cat(... , file = ifile, sep = sep, fill = fill, labels = labels,
        append = append)
  }
  return(invisible(NULL))
}

logit = function(x) log(x/(1-x))
ilogit = function(x) 1/(1 + exp(-x))

#' Give Row or Colums Sums of a Matrix or Array, Based on a Grouping Variable 
#' @param group a vector or factor giving the grouping, with one element per column of x. Missing values will be treated as another group and a warning will be given.
#' @inheritParams base::rowsum
#' @export
colsum = function (x, group, reorder = TRUE, ...) {
  UseMethod("colsum")
} 


#' @rdname colsum
#' @export
colsum.default = function(x, group, reorder=TRUE, na.rm=FALSE, ...) {
  x = t(rowsum(t(x), group=group, reorder=reorder, na.rm=na.rm, ...))
  return(x)
}

#' @rdname colsum
#' @export
colsum.matrix = colsum.default

#' @rdname colsum
#' @export
colsum.array = function(x, group, reorder=TRUE, na.rm=FALSE, ...) {
  perm = seq_along(dim(x))
  perm[1:2] = 2:1
  x = rowsum(aperm(x, perm=perm), group=group, reorder=reorder, na.rm=na.rm, ...)
  x = aperm(x, perm=perm)
  return(x)
}


#' @export
rowsum.matrix = rowsum.default

#' @export
rowsum.array = function(x, group, reorder=TRUE, na.rm=FALSE, ...) {
  if(!is.numeric(x)) 
    stop("'x' must be numeric")
  if(length(group) != NROW(x)) 
    stop("incorrect length for 'group'")
  if(anyNA(group)) 
    warning("missing values for 'group'")
  ugroup = unique(group)
  xo = apply(x, seq_along(dim(x))[-c(1:2)], FUN=rowsum.default, group=group, reorder=reorder, na.rm=na.rm, ...)
  dim(xo) = c(length(ugroup), dim(x)[-1])
  dimnames(xo) = c(list(ugroup), dimnames(x)[-1])
  return(xo)
}

