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
