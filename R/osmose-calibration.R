
#' Set values for a parameter list
#'
#' @param x The osmose.configuration list.
#' @param value The parameter value to replace.
#' @param ... Additional arguments
#'
#' @return The configuration list with parameter values changed.
#' @export
#'
set_par = function(x, value, ...) {
  x = as.relistable(x)
  val = unlist(x)
  val[] = value
  val = relist(val)
  class(val) = "osmose.configuration"
  return(val)
}

#' @exportS3Method relist osmose.configuration
relist.osmose.configuration = function(flesh, skeleton = attr(flesh, "skeleton")) {
  ind <- 1L
  result <- skeleton
  for (i in seq_along(skeleton)) {
    size <- length(unlist(result[[i]]))
    result[[i]] <- relist(flesh[seq.int(ind, length.out = size)], 
                          result[[i]])
    ind <- ind + size
  }
  result
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


# Internal ----------------------------------------------------------------

.calculate_residuals_byage = function(x, conf, tiny=1e-4) {
  
  xx = x$meanSizeByAge
  if(is.null(xx)) return(x)
  
  isp = get_species(conf, sp=names(xx))
  
  out = list()
  out1 = list()
  for(i in seq_along(xx)) {
    xi = xx[[i]]
    this = get_par(conf, sp=isp[i])
    iage = as.numeric(colnames(xi))
    dage = mean(diff(iage))
    iage = iage + 0.5*dage
    isize = VB(age=iage, this, method=1)
    isize = array(rep(isize, each=nrow(xi)), dim=dim(xi))
    out[[i]] = log((isize + tiny)/(xi + tiny))
    out1[[i]] = isize
  }
  
  names(out) = names(xx)
  names(out1) = names(xx)
  
  class(out) = c("osmose.residualSizeByAge", "list")
  class(out1) = c("osmose.expectedSizeByAge", "list")
  
  x$residualSizeByAge = out
  x$expectedSizeByAge = out1
  
  return(x)
  
}

.aggregate_catch_byclass = function(x, conf, class, type) {
  
  class = tolower(class)
  type  = tolower(type)
  
  if(!(class %in% c("size", "age"))) stop("'class' must be 'size' or 'age'")
  if(!(type %in% c("biomass", "abundance"))) stop("'type' must be 'biomass' or 'abundance'")
  
  if(class=="size") default = c(0, 200, 1)
  if(class=="age") default = c(0, max(unlist(get_par(conf, "lifespan"))), 1)
  
  if(class=="size" & type=="abundance") xn = "yieldNBySize"
  if(class=="age" & type=="abundance")  xn = "yieldNByAge" 
  if(class=="size" & type=="biomass")   xn = "yieldBySize"
  if(class=="age" & type=="biomass")    xn = "yieldByAge" 
  
  xx = x[[xn]]
  
  if(is.null(xx) | length(xx)==0) return(x)
  
  spp = get_species(conf, sp=names(xx))
  ndt = get_par(conf, "output.recordfrequency")
  from = get_par(conf, sprintf("output.distrib.by%s.min", class))
  to   = get_par(conf, sprintf("output.distrib.by%s.max", class))
  by = get_par(conf, sprintf("output.distrib.by%s.incr", class))
  
  if(is.null(from)) from = default[1]
  if(is.null(to))     to = default[2]
  if(is.null(by))     by = default[3]
  
  out = list()
  
  for(i in seq_along(xx)) {
    
    xi = xx[[i]]
    this = get_par(conf, sp=spp[i])
    
    xndt = get_par(this, "fisheries.recordfrequency.ndt")
    if(!is.null(xndt)) {
      indt = xndt/ndt
      rowok = (nrow(xi)%/%indt)*indt
      if(nrow(xi)!=rowok) xi = xi[seq_len(rowok), , ,drop=FALSE]
      ntime = as.numeric(rownames(xi))[c(rep(FALSE, indt-1), TRUE)]
      xi = rowsum(xi, group=rep(seq_len(rowok/indt), each=indt), na.rm=TRUE)
      rownames(xi) = ntime
    }
    
    xfrom = get_par(this, sprintf("fisheries.distrib.by%s.min", class))
    xto   = get_par(this, sprintf("fisheries.distrib.by%s.max", class))
    xby   = get_par(this, sprintf("fisheries.distrib.by%s.incr", class))
    
    check = !all(is.null(xfrom), is.null(xto), is.null(xby))
    
    if(check) {
      
      if(is.null(xfrom)) xfrom = from
      if(is.null(xto))     xto = to
      if(is.null(xby))     xby = by
      
      check1 = !all(xfrom==from, xto==to, xby==by)
      
      if(check1) {
        breaks = seq(from=xfrom, to=xto, by=xby)
        marks = as.numeric(colnames(xi))
        gg = cut(marks, breaks = breaks, right=FALSE, labels = FALSE)
        if(any(is.na(gg))) {
          xi = xi[, !is.na(gg), ,drop=FALSE]
          marks = marks[!is.na(gg)]
          gg = gg[!is.na(gg)]
        }
        xi = colsum(xi, group=gg, na.rm=TRUE)
        colnames(xi) = marks[!duplicated(gg)]
      }
    }
    
    out[[i]] = xi
  }
  names(out) = names(xx)
  class(out) = class(xx)
  
  x[[xn]] = out
  
  return(x)
  
}  
