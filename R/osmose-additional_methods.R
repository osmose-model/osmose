
# tail --------------------------------------------------------------------

.tail = function(x, n, ...) {
  out = NextMethod()
  class(out) = class(x)
  return(out)
}

#' @export
tail.osmose.biomass   = .tail
#' @export
tail.osmose.abundance = .tail
#' @export
tail.osmose.yield     = .tail
#' @export
tail.osmose.yieldN    = .tail

# head --------------------------------------------------------------------

.head = function(x, n, ...) {
  out = NextMethod()
  class(out) = class(x)
  return(out)
}

#' @export
head.osmose.biomass   = .head
#' @export
head.osmose.abundance = .head
#' @export
head.osmose.yield     = .head
#' @export
head.osmose.yieldN    = .head

