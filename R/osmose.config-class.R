
configureCalibration = function(L1) {
  
  nameCal  = names(unlist(L1$calibration))
  valueCal = unname(unlist(L1$calibration))
  
  #guess List
  guessList = .createCalibrationList(nameCal,valueCal,"\\.max|\\.min|\\.phase",TRUE)
  
  #max List
  maxList = .createCalibrationList(nameCal,valueCal,"\\.max",FALSE)
  
  #phase List
  phaseList = .createCalibrationList(nameCal,valueCal,"\\.phase",FALSE)
  
  #min List
  minList= .createCalibrationList(nameCal,valueCal,"\\.min",FALSE)
  
  L2 = list(guess=guessList, max=maxList, min=minList, phase=phaseList)
  
  return(L2)
  
}

readOsmoseConfiguration = function(input, config=NULL, path=NULL) {
  
  L0 = .readOsmoseConfiguration(input, path=path)
  
  if(!is.null(config)) {
    config = .getConfig(config)
    L0 = c(config, L0)
    L0 = L0[!duplicated(names(L0))]
  }
  
  nameLines = names(L0)
  nameLines = lapply(nameLines, function(x) gsub("."," ", x, fixed = TRUE))
  nameLines = str_split(nameLines," ")
  lengthlayers = lapply(nameLines, function(x) length(x))
  maxlayers    = max(unlist(lengthlayers))
  valuelist    = unlist(lapply(unname(L0), toString)) #as.character(L0)?
  
  for(i in seq_along(nameLines)){
    nameLines[[i]][length(nameLines[[i]])+1] = valuelist[i]
  }
  
  makeTree = function(x, i, n) {
    if(i==n) {
      nameLines[[x]][i]
    } else {
      spl = split(x, nameLines[[x]][i])
      lapply(spl, function(x) makeTree(x, i+1, n))
    }
  }
  
  wdata = list(res=0) # is there a more elegant way? Wdata=list()?
  for(i in seq(length(nameLines))) {
    wdata = list.merge(wdata, makeTree(i, 1, length(nameLines[[i]])))
  }
  
  wdata[which(names(wdata) %in% c("res"))] = NULL
  L1 = wdata
  
  class(L1) = c("osmose.config", class(L1))
  
  return(L1)
}

createParameterList = readOsmoseConfiguration

# Methods -----------------------------------------------------------------

# print.osmose.config = function(x, ...)

# summary.osmose.config = function(x, ...)

# print.summary.osmose.config = function(x, ...)


