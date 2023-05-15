
# Auxiliar ----------------------------------------------------------------

#' Set values for a parameter list
#'
#' @param x The osmose.configuration list.
#' @param value The parameter value to replace.
#' @param ... Additional arguments
#'
#' @return The configuration list with parameter values changed.
#' @export
#'
set_par = function(x, value=NULL, scale=1, lower=NULL, upper=NULL, delta=0, digits=3, ...) {
  if(!is.null(lower) & !is.null(upper)) stop("Both 'lower' and 'upper' cannot be set.")
  xround = ifelse(scale<=1, -1, +1)
  if(!is.null(lower)) xround = -1
  if(!is.null(upper)) xround = +1
  x = as.relistable(x)
  val = unlist(x)
  if(!is.null(value)) {
    val[] = value
  } else {
    val = val - (1-scale)*abs(val)
    val = val + delta
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

transform_par = function(x, FUN, ...) {
  nmfun = deparse(substitute(FUN))
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

