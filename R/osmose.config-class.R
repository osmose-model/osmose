
createParameterList = function(input, path=NULL) {
  
  L0 = .readOsmoseConfiguration(input)
  
  nameLines = names(L0)
  nameLines = lapply(nameLines, function(x) gsub("."," ", x, fixed = TRUE))
  nameLines = str_split(nameLines," ")
  lengthlayers = lapply(nameLines, function(x) length(x))
  maxlayers    = max(unlist(lengthlayers))
  valuelist    = unlist(lapply(unname(L0), toString)) #as.character(L0)?
  
  for(i in seq_along(nameLines)){
    nameLines[[i]][length(nameLines[[i]])+1] = valuelist[i]
  }
  
  makeTree=function(x, i, n) {
    if(i==n) {
      nameLines[[x]][i]
    } else {
      spl = split(x, nameLines[[x]][i])
      lapply(spl, function(x) makeTree(x, i+1, n))
    }
  }
  
  Wdata=list(res=0) # is there a more elegant way? Wdata=list()?
  for(i in seq(length(nameLines))) {
    Wdata = list.merge(Wdata, makeTree(i, 1, length(nameLines[[i]])))
  }
  
  Wdata[which(names(Wdata) %in% c("res"))] = NULL
  L1 = Wdata
  
  class(L1) = c("osmose.config", class(L1))
  
  return(L1)
}


# Methods -----------------------------------------------------------------

# print.osmose.config = function(x, ...)

# summary.osmose.config = function(x, ...)

# print.summary.osmose.config = function(x, ...)


