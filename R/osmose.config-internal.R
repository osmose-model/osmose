.makeTree = function(xList, x, i, n) {
  if(i==n){
    xList[[x]][i]
  } else {
    spl=split(x, xList[[x]][i])
    lapply(spl, function(x) .makeTree(xList,x,i+1,n))
  }
}

.listTree = function(xList) {
  Wdata = list(xfnres=0)
  
  for(i in seq_along(xList)) {
    Wdata=list.merge(Wdata, .makeTree(xList,i,1,length(xList[[i]])))
  }
  
  Wdata[which(names(Wdata) %in% "xfnres")] = NULL
  
  return(Wdata)
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
  
  outputNames = lapply(outputNames,function(x) gsub("."," ",x,fixed = TRUE))
  outputNames = str_split(outputNames," ")
  
  for(i in seq_along(outputNames)){
    outputNames[[i]][length(outputNames[[i]])+1] = outputValue[[i]]
  }
  
  calList = .listTree(outputNames)
  return(calList)
  
}

viewDataList = function(input, path=NULL) {
  L0 = .readOsmoseConfiguration(input)
  namelist = names(L0)
  valuelist = unlist(lapply(unname(L0),toString))
  L0 = data.frame(namelist,valuelist)
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
  Lines_trim = lapply(Lines, str_trim)
  Lines_trim[grep("^[[:punct:]]", Lines_trim)] = NULL
  Lines_trim = Lines_trim[nchar(Lines_trim)!=0]
  
  KeySeparator  = sapply(Lines_trim, .guessSeparator)
  Key           = mapply(.getKey, Lines_trim, KeySeparator)
  Values        = mapply(.getValues, Lines_trim, KeySeparator)
  
  names(Values) = tolower(Key)
  ValuesDef     = Values
  
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
  return(str_trim(Key))
}

.getValues = function(Line,KeySeparator){
  Values = str_sub(Line, gregexpr(KeySeparator, Line)[[1]][1]+1,nchar(Line))
  ValueSeparator = .guessSeparator(Values)
  Value = str_trim(str_split(Values, ValueSeparator)[[1]])
  Value = Value[nchar(Value)!=0]
  return(list(Value))
}


# getConfig ---------------------------------------------------------------

.getConfig = function(config) {
  UseMethod(".getConfig")
}

.getConfig.character = function(config) {
  return(.readOsmoseConfiguration(file=config))
}

.getConfig.osmose.config = function(config) {
  return(as.list(unlist(config)))
}

.getConfig.list = function(config) {
  return(config)
}
