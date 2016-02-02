
configureCalibration = function(L1) {
  
  nameCal  = names(unlist(L1$calibration))
  valueCal = unname(unlist(L1$calibration))
  
  #guess List
  guessList = .createCalibrationList(nameCal,valueCal,"\\.max|\\.min|\\.phase",TRUE)
  
  #max List
  maxList   = .createCalibrationList(nameCal,valueCal,"\\.max",FALSE)
  
  #phase List
  phaseList = .createCalibrationList(nameCal,valueCal,"\\.phase",FALSE)
  
  #min List
  minList   = .createCalibrationList(nameCal,valueCal,"\\.min",FALSE)
  
  L2 = list(guess=guessList, max=maxList, min=minList, phase=phaseList)
  
  return(L2)
  
}

addAttr = function(x, which, value) { 
  attr(x=x, which=which)=value
  return(x)
}
                                      
.createParameterList = function(L0) {

  nameLines    = names(L0)
  nameLines    = str_split(nameLines, "\\.")
  valueList    = lapply(unname(L0), toString)
  paths        = unname(unlist(lapply(L0, attr, which="path")))
  valueList    = mapply(addAttr, x=valueList, value=paths, which="path", SIMPLIFY = FALSE)

  L1 = .listTree(nameLines, valueList)  
  class(L1) = c("osmose.config", class(L1))
  
  return(L1)
  
}

readOsmoseConfiguration = function(input, config=NULL, absolute=TRUE) {
  
  L0 = .readOsmoseConfiguration(input=input, absolute=absolute)
  
  if(!is.null(config)) {
    config = .getConfig(config)
    L0 = c(config, L0)
    L0 = L0[!duplicated(names(L0))]
  }
  
  L1 = .createParameterList(L0)
  
  return(L1)
}



# Methods -----------------------------------------------------------------

# print.osmose.config = function(x, ...)

# summary.osmose.config = function(x, ...)

# print.summary.osmose.config = function(x, ...)


