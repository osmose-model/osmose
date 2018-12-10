.makeTree = function(xList, value, i=1) {
  n = length(xList) + 1
  if(i==n) return(value)
  out = setNames(list(.makeTree(xList, value, i=i+1)), xList[i])
  return(out)
}

.listTree = function(xList, value) {
  do.call(list.merge, mapply(FUN=.makeTree, xList=xList, value=value, SIMPLIFY = FALSE))
}

.createCalibrationList = function(input1, input2, xString, inv){
  outputList  = grep(xString, input1, invert=inv)
  outputNames =list()
  outputValue =list()
  
  for(i in seq(length(outputList))) {
    outputNames[i]=input1[outputList[i]]
    outputValue[i]=input2[outputList[i]]
  }
  
  outputNames = unlist(outputNames)
  outputNames = gsub(paste(xString,".",sep=""),".",outputNames)
  
  outputNames = lapply(outputNames, function(x) gsub("."," ", x, fixed = TRUE))
  outputNames = str_split(outputNames," ")
  
  calList = .listTree(outputNames, outputValue)
  return(calList)
  
}

# .strsplit = function(string, pattern) strsplit(unlist(string), pattern)

viewDataList = function(input, path=NULL) {
  L0 = .readOsmoseConfiguration(input)
  namelist = names(L0)
  valuelist = unlist(lapply(unname(L0),toString))
  L0 = data.frame(namelist, valuelist)
  return(L0)
}

##Funciones internas

.readOsmoseConfiguration = function(input, path=NULL, absolute=TRUE) {

  if(isTRUE(absolute)) {
    if(is.null(path)) {
      path  = normalizePath(dirname(input))
      input = basename(input)
    }
  } else {
    path = if(is.null(path)) {
      normalizePath(dirname(input))
    } else {
      normalizePath(file.path(path, dirname(input))) 
    }
    input = basename(input)    
  }
  
  Lines = readLines(file.path(path, input))
  Lines_trim = lapply(Lines, stringr::str_trim)
  Lines_trim[grep("^[[:punct:]]", Lines_trim)] = NULL
  Lines_trim = Lines_trim[nchar(Lines_trim)!=0]
  
  KeySeparator  = sapply(Lines_trim, .guessSeparator)
  Key           = mapply(.getKey, Lines_trim, KeySeparator)
  Values        = mapply(.getValues, Lines_trim, KeySeparator)
  
  names(Values) = tolower(Key)
  ValuesDef     = Values
  
  ValuesDef = lapply(ValuesDef, addAttr, which="path", value=path)
  
  if(length(grep("osmose.configuration", Key))>0) {
    for(i in grep("osmose.configuration", Key)) {
      ValuesRec = .readOsmoseConfiguration(input=Values[[i]], path=path, absolute=absolute)
      ValuesDef = c(ValuesDef, ValuesRec)
    }
  }
  
  isDuplicated = duplicated(names(ValuesDef))
  ValuesDef = ValuesDef[!isDuplicated]
  
  return(ValuesDef)
}

.guessSeparator = function(Line) {
  SEPARATORS = c(equal="=", semicolon=";", coma=",", colon=":", tab="\t")
  guess = which.min(nchar(lapply(str_split(Line,SEPARATORS), "[", i=1)))
  separator  = SEPARATORS[guess]
  return(separator)
}

.getKey = function(Line, KeySeparator) {
  Key = str_split(Line, KeySeparator)[[1]][1]
  return(stringr::str_trim(Key))
}

.getValues = function(Line,KeySeparator){
  Values = stringr::str_sub(Line, gregexpr(KeySeparator, Line)[[1]][1]+1,nchar(Line))
  ValueSeparator = .guessSeparator(Values)
  Value = stringr::str_trim(str_split(Values, ValueSeparator)[[1]])
  Value = Value[nchar(Value)!=0]
  return(list(Value))
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
  
  return(L1)
  
}

# getConfig ---------------------------------------------------------------

.getConfig = function(config, ...) {
  UseMethod(".getConfig")
}

.getConfig.character = function(config, ...) {
  return(.readOsmoseConfiguration(input=config, ...))
}

.getConfig.osmose.config = function(config, ...) {
  return(as.list(unlist(config)))
}

.getConfig.list = function(config, ...) {
  return(config)
}


# GetData functions -------------------------------------------------------

getReproductionData = function(x, var = season.file){
  
  # index on the list using the var
  listIndex = paste0("x = x",
                     switch(var,
                            file        = "[['file']]",
                            season.file = "[['season']][['file']]"))
  
  eval(parse(text = listIndex))
  
  speciesNames = names(x)
  reproData    = as.vector(unlist(lapply(x, FUN = "[[", 1)))
  reproPaths   = as.vector(unlist(lapply(x, attr, "path")))
  
  reproData    = paste(reproPaths, reproData, sep = "/")
  
  dataBase = list()
  for(i in seq_along(speciesNames)){
    dataBase[[i]] = read.csv(file = reproData[i], sep = ";")
  }
  names(dataBase) = speciesNames
  
  return(dataBase)
}


getSpeciesData = function(x) {
  
  # species name in configuration file
  speciesCode = names(x$name)
  
  # growth variables
  names               = lapply(x$name, FUN = "[[", 1)
  linf                = lapply(lapply(x$linf, FUN = "[[", 1), as.numeric, 1)
  k                   = lapply(lapply(x$k, FUN = "[[", 1), as.numeric, 1)
  t0                  = lapply(lapply(x$t0, FUN = "[[", 1), as.numeric, 1)
  thr                 = lapply(lapply(x$vonbertalanffy$threshold$age, FUN = "[[", 1), as.numeric, 1)
  conditionFactor     = lapply(lapply(x$length2weight$condition$factor, FUN = "[[", 1), as.numeric, 1)
  allometricPower     = lapply(lapply(x$length2weight$allometric$power, FUN = "[[", 1), as.numeric, 1)
  relativityFecundity = lapply(lapply(x$relativefecundity, FUN = "[[", 1), as.numeric, 1)
  eggSize             = lapply(lapply(x$egg$size, FUN = "[[", 1), as.numeric, 1)
  eggWeight           = lapply(lapply(x$egg$weight, FUN = "[[", 1), as.numeric, 1)
  sexRatio            = lapply(lapply(x$sexratio, FUN = "[[", 1), as.numeric, 1)
  maturitySize        = lapply(lapply(x$maturity$size, FUN = "[[", 1), as.numeric, 1)
  lifespan            = lapply(lapply(x$lifespan, FUN = "[[", 1), as.numeric, 1)
  
  # Data base 
  dataBase = list(names               = names,
                  linf                = linf,
                  k                   = k,
                  t0                  = t0, 
                  thr                 = thr,
                  conditionFactor     = conditionFactor,
                  allometricPower     = allometricPower,
                  relativityFecundity = relativityFecundity,
                  eggSize             = eggSize,
                  eggWeight           = eggWeight,
                  sexRatio            = sexRatio,
                  maturitySize        = maturitySize,
                  lifespan            = lifespan,
                  speciesCode         = speciesCode)
  
  return(dataBase)
}


getPredationData = function(x, object, extraWhat = FALSE) {
  
  # Accessibility
  fileAccessibility = unlist(lapply(x$accessibility$file, FUN = "[[", 1))
  pathAccessibility = attributes(x$accessibility$file)$path
  accessibility     = list(data = read.csv(file = paste(pathAccessibility,
                                                        fileAccessibility, sep = "/"), sep = ";"),
                           stageStructure = as.vector(x$accessibility$stage$structure),
                           stageThreshold = lapply(x$accessibility$stage$threshold, FUN = "[[", 1))
  
  # Efficiency
  efficiency   = list(critical = lapply(lapply(x$efficiency$critical, FUN = "[[", 1), as.numeric, 1))
  
  # Ingestion
  ingestion    = list(rateMax = lapply(lapply(x$ingestion$rate$max, FUN = "[[", 1), as.numeric, 1))
  
  # PredPrey
  sizeRatioMax   = lapply(lapply(lapply(x$predprey$sizeratio$max, FUN = "[[", 1), FUN = strsplit, ","), FUN = unlist, 1)
  sizeRatioMin   = lapply(lapply(lapply(x$predprey$sizeratio$min, FUN = "[[", 1), FUN = strsplit, ","), FUN = unlist, 1)
  stageThreshold = lapply(lapply(lapply(x$predprey$stage$threshold, FUN = "[[", 1), FUN = strsplit, ","), FUN = unlist, 1)
  predPrey       = list(sizeRatioMax   = lapply(sizeRatioMax, FUN = as.numeric, 1),
                        sizeRatioMin   = lapply(sizeRatioMin, FUN = as.numeric, 1),
                        stageStructure = as.character(x$predprey$stage$structure),
                        stageThreshold = suppressWarnings(lapply(stageThreshold, FUN = as.numeric, 1)))
  
  # Data base
  dataBase = list(accessibility = accessibility,
                  efficiency    = efficiency,
                  ingestion     = ingestion,
                  predPrey      = predPrey)
  
  if(isTRUE(extraWhat)){
    linf = lapply(lapply(object[["species"]][["linf"]], FUN = "[[", 1), as.numeric, 1)
    speciesNames = lapply(hum$species$name, FUN = "[[", 1)
    dataBase$linf = linf
    dataBase$speciesNames = speciesNames
  } 
  
  return(dataBase)
}
