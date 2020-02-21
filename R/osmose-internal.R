
################################################################## Test on diet matrix
# Parsing input files -----------------------------------------------------


.getVersion = function(version){
  
  if(length(version) > 1) stop("Only one 'version' value must be provided.")
  
  # Split string by places where there's dots
  version = unlist(strsplit(x = as.character(version), split = "\\."))
  version = suppressWarnings(as.numeric(version))
  
  # Check if version has a valid form
  if(any(is.na(version)) | length(version) > 3 | any(version < 0, na.rm = TRUE)){
    stop("Version must be of the form X, X.Y or X.Y.Z")
  }
  
  # Complete version with zeros if its length is less than 3
  version = as.integer(c(version, rep(0, 3 - length(version))))
  
  return(version)
}

# Returns +1 if version1 is greater than version2, -1 if version1
# is less than version2, 0 ib both versions are equal.
.compareVersion = function(version1, version2) {
  
  if(is.character(version1)) version1 = .getVersion(version1)
  if(is.character(version2)) version2 = .getVersion(version2)
  
  # Check numbers of version
  output = as.integer(sign(mapply("-", version1, version2)))
  
  # Return -1, 0, +1 if corresponds
  for(i in seq_along(output)) if(output[i] != 0) return(output[i])
  
  return(0)
}

# guess the type of a vector
.guessType = function(x, keep.att = FALSE){
  if(!is.character(x)) return(x)
  
  att = attributes(x)
  x = stringr::str_trim(strsplit(x, split=",")[[1]])  # because strsplit returns a list
  if(identical(tolower(x), "null")) return(NULL) 
  asNum = suppressWarnings(as.numeric(x))
  isNumeric = !all(is.na(asNum))
  x[x=="na"] = NA
  out = if(isNumeric) asNum else x  
  attributes(out) = NULL
  if(keep.att) attributes(out) = att
  return(out) 
}

# get a parameter from a name chain
.getPar = function(x, ..., keep.att=FALSE) {
  # chain = tolower(unlist(list(...)))
  chain = unlist(list(...))
  if(is.list(x)) x=do.call(.getPar, list(x=x[[chain[1]]], chain[-1]))
  return(.guessType(x, keep.att = keep.att))
}

#' Check if a parameter exists
#' @param par Output of the \code{link{read_osmose}} function
#' @param ... String arguments 
#' @param keep.att Whether parameter attributes should be kept
existOsmoseParameter = function(par, ..., keep.att=FALSE) {
  chain = unlist(list(...))
  x = .getPar(par, ..., keep.att=TRUE)
  if(is.null(x)) return(0) else return(1)
}

# Get species names. It matches the spX regular expression.
#
# @param x Named data
.getSpecies = function(x)  {
  x = names(x)
  x = grep(pattern="^sp[0-9]*$", x=x, value=TRUE)
  return(x)
}

.getBoolean = function(x, default=NULL) {
  if(is.null(x)) return(default)
  if(length(x)>1) stop("More than one value provided.")
  if(is.logical(x)) return(x)
  x = tolower(x)
  if(x=="true") return(TRUE)
  if(x=="false") return(FALSE)
  stop(sprintf("Invalid input, %s is not boolean.", x))
}

.readInputCsv = function(file) {
  sep = .guessSeparator(readLines(file, n=1))
  out = read.csv(file, sep=sep, row.names=1)
}

.getFileAsVector = function(file) {
  if(is.null(file)) return(NULL)
  path = attr(file, which="path")
  if(!is.null(path)) file=file.path(path, file)
  if(!file.exists(file)) stop(sprintf("File %s not found", file))
  out = .readInputCsv(file=file)
  out = as.numeric(as.matrix(out))
  return(out)
}


# Parsing OSMOSE outputs --------------------------------------------------

# Groups input files for each species.
# **The name of the configuration must not contain any _ or - characters**
.bySpecies = function(files, sep=c("_", "-")) {
  out = NULL
  if(length(files)>0) {
    sp  = sapply(sapply(files, FUN=.strsplit2v, sep[1],
                        USE.NAMES=FALSE)[2,], FUN=.strsplit2v, sep[2],
                 USE.NAMES=FALSE)[2,]
    out = as.list(tapply(files, INDEX=sp, FUN=identity))
  }
  # change names for all species
  return(out)
}

.strsplit2v = function(...) {
  out = matrix(unlist(strsplit(...)), ncol=1)
  names(out) = NULL
  return(out)
}

.getModelName = function(path) {
  strsplit(dir(path=path, pattern="_biomass_")[1],"_")[[1]][1]
}

#' Read Osmose CSV files
#'
#' @param file File name
#' @param sep Column separator 
#' @param skip Number of line to skip
#' @param row.names Index of the row names column
#' @param na.strings List of NaN strings
#' @param rm  ???? Not used
#' @param ... Extra arguments passed to the method.
#'
#' @return A data frame
.readOsmoseCsv = function(file, sep=",", skip=1, row.names=1, 
                          na.strings=c("NA", "NaN"), rm=1, ...) {
  out = read.csv(file=file, sep=sep, skip=skip, 
                 row.names=row.names, na.strings=na.strings, check.names = FALSE, ...)
  #   mat = as.matrix(out)
  #   out[is.na(mat) & !is.nan(mat)] = Inf
  #   out[is.na(mat) & is.nan(mat)] = NA
  return(out)
}

.readMortalityCsv = function(file, sep=",", skip=1, row.names=1, 
                             na.strings=c("NA", "NaN"), rm=1, ...) {
  
  x = readLines(file)
  .subSep = function(x) gsub(";", ",", x)
  
  x = lapply(x, .subSep)
  legend = x[[1]]
  headers = x[2:3]
  x = x[-c(1:3)]
  x = paste(x, collapse="\n")
  x = read.csv(text=x, header=FALSE, na.strings=na.strings)
  times = x[,1]
  x = as.matrix(x[, -c(1,17)])
  x = array(as.numeric(x), dim=c(nrow(x), 3, 5))
  rownames(x) = round(times, 2)
  dimnames(x)[[3]] = c("pred", "starv", "other", "fishing", "out")
  
  return(x)
}

.errorReadingOutputs = function(e) {
  warning(e)
  return(invisible(NULL))
}

.warningReadingOutputs = function(type) {
  e = sprintf("File type '%s' is not recognized by 'read_osmose'", type)
  warning(e)
  return(invisible(NULL))
}


.readFilesList = function(files, path, type, ...) {
  output = tryCatch(
    switch(type,
           abundance       =  .read_1D(files=files, path=path, ...),
           biomass         =  .read_1D(files=files, path=path, ...),
           yield           =  .read_1D(files=files, path=path, ...),
           yieldN          =  .read_1D(files=files, path=path, ...),
           meanTL          =  .read_1D(files=files, path=path, ...),
           meanTLCatch     =  .read_1D(files=files, path=path, ...),
           meanSize        =  .read_1D(files=files, path=path, ...),
           meanSizeCatch   =  .read_1D(files=files, path=path, ...),
           biomassPredPreyIni =  .read_1D(files=files, path=path, ...),
           predatorPressure          = .read_2D(files=files, path=path, ...),
           dietMatrix                = .read_2D(files=files, path=path, ...),
           AgeSpectrumSpeciesB       = .read_2D(files=files, path=path, ...),
           AgeSpectrumSpeciesN       = .read_2D(files=files, path=path, ...),
           AgeSpectrumSpeciesYield   = .read_2D(files=files, path=path, ...),
           AgeSpectrumSpeciesYieldN  = .read_2D(files=files, path=path, ...),
           SizeSpectrum              = .read_2D(files=files, path=path, ...),
           SizeSpectrumSpeciesB      = .read_2D(files=files, path=path, ...),
           SizeSpectrumSpeciesN      = .read_2D(files=files, path=path, ...),
           SizeSpectrumSpeciesYield  = .read_2D(files=files, path=path, ...),
           SizeSpectrumSpeciesYieldN = .read_2D(files=files, path=path, ...),
           mortalityRate             = .read_MortStage(files=files, path=path, ...),
           
           # fisheries
           # temporal output reading
           fisheriesOutput = .read_osmose_ncdf(files=files, path=path, ...),
           
           #bioen
           AgeMature  = .read_1D(files=files, path=path, ...),
           growthpot  = .read_1D(files=files, path=path, ...),
           ingestion = .read_1D(files=files, path=path, ...),
           maint = .read_1D(files=files, path=path, ...),
           SizeInf = .read_1D(files=files, path=path, ...),
           SizeMature = .read_1D(files=files, path=path, ...),
           
           # osmose 3r1
           #            mortalityRateDistribByAge = .read_MortStagebyAgeorSize(files=files, path=path, ...),
           #            mortalityRateDistribBySize = .read_MortStagebyAgeorSize(files=files, path=path, ...),
           mortalityRateDistribByAge      = .read_2D(files=files, path=path, ...),
           mortalityRateDistribBySize     = .read_2D(files=files, path=path, ...),
           abundanceDistribBySize         = .read_2D(files=files, path=path, ...),
           biomassDistribBySize            = .read_2D(files=files, path=path, ...),
           naturalMortalityDistribBySize  = .read_2D(files=files, path=path, ...),
           naturalMortalityNDistribBySize = .read_2D(files=files, path=path, ...),
           yieldDistribBySize             = .read_2D(files=files, path=path, ...),
           yieldNDistribBySize            = .read_2D(files=files, path=path, ...),
           abundanceDistribByAge          = .read_2D(files=files, path=path, ...),
           biomassDistribByAge             = .read_2D(files=files, path=path, ...),
           meanSizeDistribByAge           = .read_2D(files=files, path=path, ...),
           naturalMortalityDistribByAge   = .read_2D(files=files, path=path, ...),
           naturalMortalityNDistribByAge  = .read_2D(files=files, path=path, ...),
           yieldDistribByAge              = .read_2D(files=files, path=path, ...),
           yieldNDistribByAge             = .read_2D(files=files, path=path, ...),
           biomassDistribByTL              = .read_2D(files=files, path=path, ...),
           #            dietMatrixbyAge                = .read_2D_ByAgeorSize(files=files, path=path, ...),
           #            dietMatrixbySize               = .read_2D_ByAgeorSize(files=files, path=path, ...),
           dietMatrixbyAge                = .read_2D(files=files, path=path, ...),
           dietMatrixbySize               = .read_2D(files=files, path=path, ...),
           meanTLDistribByAge             = .read_2D(files=files, path=path, ...),
           meanTLDistribBySize            = .read_2D(files=files, path=path, ...),
           predatorPressureDistribByAge   = .read_2D(files=files, path=path, ...),
           predatorPressureDistribBySize  = .read_2D(files=files, path=path, ...),
           abundanceDistribByTL            = .read_2D(files=files, path=path, ...),
           .warningReadingOutputs(type)), 
    error = .errorReadingOutputs)
  
  return(output)
}

# Generic function to read 1D output files (abundance, biomass, etc).
# If no file is found, it returns NULL.
#
# @param files List of input files
# @param path  Data directory
# @param ... Additional arguments of the \code{\link{.readOsmoseCsv}} function.
#
# @return A 3D array (time, species, replicates) or NULL if no file is found.
.read_1D = function(files, path, ...) {
  # TO_DO: change for the unified approach! species as list
  if(length(files)!=0) {
    x = .readOsmoseCsv(file.path(path, files[1]), ...)
    species = names(x)
    times   = rownames(x)
    
    output = array(dim=c(dim(x),length(files)))
    output[,,1] = as.matrix(x)
    if(length(files)>1) {
      for(i in seq_along(files[-1])) {
        x = .readOsmoseCsv(file.path(path, files[i+1]), ...)
        output[,,i+1]= as.matrix(x)
      }
    }
    rownames(output) = times
    colnames(output) = species
  } else {
    output = NULL
  }
  
  return(output)
}

#' Generic function to read 2D output files (dietMatrix, sizeSpectrum, etc).
#' If no file is found, it returns NULL.
#'
#' @param files List of input files
#' @param path  Data directory
#' @param ... Extra arguments passed to the method.
#'
#' @return A 3D array (time, species, replicates) or NULL if no file is found.
.read_2D = function(files, path, ...) {
  
  if(length(files)!=0) {
    
    x = .readOsmoseCsv(file.path(path, files[1]), row.names=NULL, ...)
    
    rows    = unique(x[,1])
    cols    = unique(x[,2])
    slices  = names(x)[-(1:2)]
    
    x = .reshapeOsmoseTable(x)
    
    out = array(dim = c(dim(x), length(files)))
    
    out[, , , 1] = x
    
    if(length(files)>1) {
      for(i in seq_along(files[-1])) {
        x = .readOsmoseCsv(file.path(path, files[i+1]), row.names=NULL, ...)
        x = .reshapeOsmoseTable(x)
        out[, , , i+1]= x
      }
    }
    
    out = aperm(out, c(1,2,4,3))
    
    rownames(out) = rows
    colnames(out) = cols
    
    nsp = dim(out)[4]
    
    output=list()
    
    for(i in seq_len(nsp)) {
      y = out[, , , i, drop=FALSE]
      dnames = dimnames(y)[1:3]
      dim(y) = dim(y)[-length(dim(y))]
      dimnames(y) = dnames
      output[[i]] = drop(y)
    }
    
    names(output) = slices
    
  } else {
    output = NULL
  }
  
  return(output)
}

.read_MortStage = function(files, path, ...) {
  
  if(length(files)!=0) {
    
    x = .readMortalityCsv(file.path(path, files[1]), row.names=NULL, ...)
    
    rows = row.names(x)
    cols = c("pred", "starv", "other", "fishing", "out")
    
    out = array(dim = c(dim(x), length(files)))
    
    out[, , , 1] = x
    
    if(length(files)>1) {
      for(i in seq_along(files[-1])) {
        x = .readMortalityCsv(file.path(path, files[i+1]), row.names=NULL, ...)
        out[, , , i+1]= x
      }
    }
    
    rownames(out) = rows
    dimnames(out)[[3]] = cols
    
    output=list()
    
    # remove the dropping of dimensions
    # warning, the dimension will be ntime, 1, 5, replicate.
    output$eggs      = out[, 1, , ]
    output$juveniles = out[, 2, , ]
    output$adults    = out[, 3, , ]
    
  } else {
    output = NULL
  }
  
  return(output)
}


#' Function to read osmose netcdf files
#'
#' @param files String of name of the file that will be read.
#' @param path String of path of the file that will be read
#' @param ... Extra arguments 
#'
.read_osmose_ncdf = function(files, path, ...) {
  
  if(length(files)!=0) {
    
    nc = nc_open(file.path(path, files[1]))
    x = ncvar_get(nc) # assumes only one variable in the file
    nc_close(nc)
    
    output = array(dim = c(dim(x), length(files)))
    
    output[, , , 1] = x
    
    if(length(files)>1) {
      for(i in seq_along(files[-1])) {
        nc = nc_open(file.path(path, files[i+1]))
        x = ncvar_get(nc) # assumes only one variable in the file
        nc_close(nc)
        output[, , , i+1]= x
      }
    }
    
  } else {
    output = NULL
  }
  
  return(output)
}


.reshapeOsmoseTable = function(x) {
  
  rows    = unique(x[,1])
  cols    = unique(x[,2])
  slices  = names(x)[-(1:2)]
  
  x       = as.matrix(x[,-(1:2)])
  dim(x)  = c(length(cols), length(rows), length(slices))
  x       = aperm(x, c(2,1,3))
  
  dimnames(x) = list(rows, cols, slices)
  
  return(x)
}

.rewriteOutputs = function(path) {
  dir.create(file.path(path, "osmose2R"))
  # not finished
}

.countOnes = function(files, ...) {
  
  out = numeric(length(files))
  
  for(i in seq_along(files)) {
    
    x = read.csv(files[i], header=FALSE, ...)
    out[i] = sum(x>0, na.rm=TRUE)
    
  }
  
  out = c(min=min(out), mean=mean(out), 
          median=median(out), max=max(out))
  return(out)
  
}


.removeZeros = function(object) {
  remove = apply(object, 2, function(x) all(x==0))
  object = object[ , !remove, ]
  return(object)
}


.niceSeq = function(x, zero=FALSE) {
  ncode = floor(log10(x)) + 1 
  out = sprintf(paste0("%0", ncode, "d"), seq_len(x) - zero)
  return(out)
}

# read_osmose old ---------------------------------------------------------

# Read Osmose (version 4 release 0) outputs
#
# @param path Osmose output path
# @param species.names Array of species names. If
# NULL, it is extracted from file.
#
# @return A list of list containing the output functions
#
osmose2R.v4r0 = function (path=NULL, species.names=NULL) {
  
  # Output data
  outputData = list(biomass = readOsmoseFiles(path = path, type = "biomass"),  
                    abundance = readOsmoseFiles(path = path, type = "abundance"),  
                    mortality = readOsmoseFiles(path = path, type = "mortalityRate", bySpecies = TRUE), 
                    meanTL = readOsmoseFiles(path = path, type = "meanTL"),  
                    meanTLCatch = readOsmoseFiles(path = path, type = "meanTLCatch"), 
                    biomassByTL = readOsmoseFiles(path = path, type = "biomassDistribByTL"),  
                    predatorPressure = readOsmoseFiles(path = path, type = "predatorPressure"),  
                    predPreyIni = readOsmoseFiles(path = path, type = "biomassPredPreyIni"),
                    dietMatrix = readOsmoseFiles(path = path, type = "dietMatrix"),  
                    meanSize = readOsmoseFiles(path = path, type = "meanSize"),  
                    meanSizeCatch = readOsmoseFiles(path = path, type = "meanSizeCatch"),  
                    SizeSpectrum  = readOsmoseFiles(path=path, type="SizeSpectrum")$Abundance,
                    abundanceBySize = readOsmoseFiles(path = path, type = "abundanceDistribBySize"), 
                    biomassBySize = readOsmoseFiles(path = path, type = "biomassDistribBySize"),   
                    meanTLBySize = readOsmoseFiles(path = path, type = "meanTLDistribBySize"),  
                    mortalityBySize = readOsmoseFiles(path = path, type = "mortalityRateDistribBySize", bySpecies = TRUE),  
                    dietMatrixBySize = readOsmoseFiles(path = path, type = "dietMatrixbySize", bySpecies = TRUE),  
                    predatorPressureBySize = readOsmoseFiles(path = path, type = "predatorPressureDistribBySize", bySpecies = TRUE),
                    abundanceByAge = readOsmoseFiles(path = path, type = "abundanceDistribByAge"),  
                    biomassByAge = readOsmoseFiles(path = path, type = "biomassDistribByAge"),  
                    meanSizeByAge = readOsmoseFiles(path = path, type = "meanSizeDistribByAge"), 
                    meanTLByAge = readOsmoseFiles(path = path, type = "meanTLDistribByAge"),  
                    mortalityByAge = readOsmoseFiles(path = path, type = "mortalityRateDistribByAge", bySpecies = TRUE),
                    dietMatrixByAge = readOsmoseFiles(path = path, type = "dietMatrixbyAge", bySpecies = TRUE),  
                    predatorPressureByAge = readOsmoseFiles(path = path, type = "predatorPressureDistribByAge", bySpecies = TRUE), 
                    abundanceByTL = readOsmoseFiles(path = path, type = "abundanceDistribByTL"),  
                    
                    # Fisheries outputs
                    
                    yieldByFishery = readOsmoseFiles(path = path, type = "fisheriesOutput", ext="nc"),
                    yield = readOsmoseFiles(path = path, type = "yield"), 
                    yieldN = readOsmoseFiles(path = path, type = "yieldN"), 
                    yieldBySize = readOsmoseFiles(path = path, type = "yieldDistribBySize"),  
                    yieldNBySize = readOsmoseFiles(path = path, type = "yieldNDistribBySize"),  
                    yieldByAge = readOsmoseFiles(path = path, type = "yieldDistribByAge"),  
                    yieldNByAge = readOsmoseFiles(path = path, type = "yieldNDistribByAge"),  
                    
                    
                    # bioen variables
                    ageMature = readOsmoseFiles(path = path, type = "AgeMature"),
                    growthPotential = readOsmoseFiles(path = path, type = "growthpot"),
                    ingestion = readOsmoseFiles(path = path, type = "ingestion"),
                    maintenance = readOsmoseFiles(path = path, type = "maint"),
                    sizeInf = readOsmoseFiles(path = path, type = "SizeInf"),
                    sizeMature = readOsmoseFiles(path = path, type = "SizeMature")
                    
  )
  
  if(!is.null(outputData$yieldByFishery)) {
    # temporal
    outputData$yield = aperm(apply(outputData$yieldByFishery, 2:4, sum, na.rm=TRUE), 
                             perm = c(2,1,3))
    rownames(outputData$yield) = seq_len(nrow(outputData$yield)) - 1
    colnames(outputData$yield) = colnames(outputData$biomass)
    
    class(outputData$yield) = "osmose.yield"
    # end of temporal
  }
  
  model = list(version = "4",
               model = .getModelName(path = path),
               simus = dim(outputData$biomass)[3],
               times = as.numeric(row.names(outputData$biomass)),
               T = nrow(outputData$biomass),
               start = as.numeric(row.names(outputData$biomass))[1],
               nsp = ncol(outputData$biomass),
               lspecies = if (!is.null(species.names)) species.names else colnames(outputData$biomass))
  
  output = c(model = list(model), species = list(colnames(outputData$biomass)),
             outputData)
  
  return(output)
}

# Read Osmose (version 3 release 2) outputs
#
# @param path Osmose output path
# @param species.names Array of species names. If
# NULL, it is extracted from file.
#
# @return A list of list containing the output functions
#
osmose2R.v3r2 = function (path=NULL, species.names=NULL) {
  
  # Output data
  outputData = list(biomass = readOsmoseFiles(path = path, type = "biomass"),  
                    abundance = readOsmoseFiles(path = path, type = "abundance"),  
                    yield = readOsmoseFiles(path = path, type = "yield"), 
                    yieldN = readOsmoseFiles(path = path, type = "yieldN"), 
                    mortality = readOsmoseFiles(path = path, type = "mortalityRate", bySpecies = TRUE), 
                    meanTL = readOsmoseFiles(path = path, type = "meanTL"),  
                    meanTLCatch = readOsmoseFiles(path = path, type = "meanTLCatch"), 
                    biomassByTL = readOsmoseFiles(path = path, type = "biomassDistribByTL"),  
                    predatorPressure = readOsmoseFiles(path = path, type = "predatorPressure"),  
                    predPreyIni = readOsmoseFiles(path = path, type = "biomassPredPreyIni"),
                    dietMatrix = readOsmoseFiles(path = path, type = "dietMatrix"),  
                    meanSize = readOsmoseFiles(path = path, type = "meanSize"),  
                    meanSizeCatch = readOsmoseFiles(path = path, type = "meanSizeCatch"),  
                    SizeSpectrum  = readOsmoseFiles(path=path, type="SizeSpectrum")$Abundance,
                    abundanceBySize = readOsmoseFiles(path = path, type = "abundanceDistribBySize"), 
                    biomassBySize = readOsmoseFiles(path = path, type = "biomassDistribBySize"),   
                    yieldBySize = readOsmoseFiles(path = path, type = "yieldDistribBySize"),  
                    yieldNBySize = readOsmoseFiles(path = path, type = "yieldNDistribBySize"),  
                    meanTLBySize = readOsmoseFiles(path = path, type = "meanTLDistribBySize"),  
                    mortalityBySize = readOsmoseFiles(path = path, type = "mortalityRateDistribBySize", bySpecies = TRUE),  
                    dietMatrixBySize = readOsmoseFiles(path = path, type = "dietMatrixbySize", bySpecies = TRUE),  
                    predatorPressureBySize = readOsmoseFiles(path = path, type = "predatorPressureDistribBySize", bySpecies = TRUE),
                    abundanceByAge = readOsmoseFiles(path = path, type = "abundanceDistribByAge"),  
                    biomassByAge = readOsmoseFiles(path = path, type = "biomassDistribByAge"),  
                    yieldByAge = readOsmoseFiles(path = path, type = "yieldDistribByAge"),  
                    yieldNByAge = readOsmoseFiles(path = path, type = "yieldNDistribByAge"),  
                    meanSizeByAge = readOsmoseFiles(path = path, type = "meanSizeDistribByAge"), 
                    meanTLByAge = readOsmoseFiles(path = path, type = "meanTLDistribByAge"),  
                    mortalityByAge = readOsmoseFiles(path = path, type = "mortalityRateDistribByAge", bySpecies = TRUE),
                    dietMatrixByAge = readOsmoseFiles(path = path, type = "dietMatrixbyAge", bySpecies = TRUE),  
                    predatorPressureByAge = readOsmoseFiles(path = path, type = "predatorPressureDistribByAge", bySpecies = TRUE), 
                    abundanceByTL = readOsmoseFiles(path = path, type = "abundanceDistribByTL"),  
                    
                    # bioen variables
                    ageMature = readOsmoseFiles(path = path, type = "AgeMature"),
                    growthPotential = readOsmoseFiles(path = path, type = "growthpot"),
                    ingestion = readOsmoseFiles(path = path, type = "ingestion"),
                    maintenance = readOsmoseFiles(path = path, type = "maint"),
                    sizeInf = readOsmoseFiles(path = path, type = "SizeInf"),
                    sizeMature = readOsmoseFiles(path = path, type = "SizeMature")
                    
  )
  
  model = list(version = "3u2",
               model = .getModelName(path = path),
               simus = dim(outputData$biomass)[3],
               times = as.numeric(row.names(outputData$biomass)),
               T = nrow(outputData$biomass),
               start = as.numeric(row.names(outputData$biomass))[1],
               nsp = ncol(outputData$biomass),
               lspecies = if (!is.null(species.names)) species.names else colnames(outputData$biomass))
  
  output = c(model = list(model), species = list(colnames(outputData$biomass)),
             outputData)
  
  # remove NULL elements fom the list
  cond = sapply(output, is.null)
  output = output[!cond]
  
  return(output)
}


osmose2R.v3r1 = function(path=NULL, species.names=NULL, ...) {
  
  # Output data
  outputData = list(biomass    = readOsmoseFiles(path=path, type="biomass"),
                    abundance  = readOsmoseFiles(path=path, type="abundance"),
                    yield      = readOsmoseFiles(path=path, type="yield"),
                    catch      = readOsmoseFiles(path=path, type="yieldN"),
                    mortality  = readOsmoseFiles(path=path, type="mortalityRate", bySpecies=TRUE),
                    dietMatrix  = readOsmoseFiles(path=path, type="dietMatrix"),
                    meanTL      = readOsmoseFiles(path=path, type="meanTL"),
                    meanTLCatch = readOsmoseFiles(path=path, type="meanTLCatch"),
                    predatorPressure = readOsmoseFiles(path=path, type="predatorPressure"),
                    predPreyIni = readOsmoseFiles(path=path, type="biomassPredPreyIni"),
                    TLDistrib   = readOsmoseFiles(path=path, type="TLDistrib"),
                    meanSize      = readOsmoseFiles(path=path, type="meanSize"),
                    meanSizeCatch = readOsmoseFiles(path=path, type="meanSizeCatch"),
                    SizeSpectrum  = readOsmoseFiles(path=path, type="SizeSpectrum")$Abundance,
                    SizeSpectrumN = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesN"),
                    SizeSpectrumB = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesB"),
                    SizeSpectrumC = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesYield"),
                    SizeSpectrumY = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesYieldN"),
                    AgeSpectrumN = readOsmoseFiles(path=path, type="AgeSpectrumSpeciesN"),
                    AgeSpectrumB = readOsmoseFiles(path=path, type="AgeSpectrumSpeciesB"),
                    AgeSpectrumC = readOsmoseFiles(path=path, type="AgeSpectrumSpeciesYield"),
                    AgeSpectrumY = readOsmoseFiles(path=path, type="AgeSpectrumSpeciesYieldN"))
  
  model = list(version  = "3u1",
               model    = .getModelName(path=path),
               simus    = dim(outputData$biomass)[3],
               times    = as.numeric(row.names(outputData$biomass)),
               T        = nrow(outputData$biomass),
               start    = as.numeric(row.names(outputData$biomass))[1],
               nsp      = ncol(outputData$biomass),
               lspecies = if(!is.null(species.names)) species.names else colnames(outputData$biomass))
  
  output = c(model = list(model), species = list(colnames(outputData$biomass)), 
             outputData)
  
  return(output)
  
}


osmose2R.v3r0 = function(path=NULL, species.names=NULL, ...) {
  
  # Output data
  outputData = list(biomass    = readOsmoseFiles(path=path, type="biomass"),
                    abundance  = readOsmoseFiles(path=path, type="abundance"),
                    yield      = readOsmoseFiles(path=path, type="yield"),
                    catch      = readOsmoseFiles(path=path, type="yieldN"),
                    mortality  = readOsmoseFiles(path=path, type="mortalityRate", bySpecies=TRUE),
                    meanTL      = readOsmoseFiles(path=path, type="meanTL"),
                    meanTLCatch = readOsmoseFiles(path=path, type="meanTLCatch"),
                    predatorPressure = readOsmoseFiles(path=path, type="predatorPressure"),
                    predPreyIni = readOsmoseFiles(path=path, type="biomassPredPreyIni"),
                    meanSize      = readOsmoseFiles(path=path, type="meanSize"),
                    meanSizeCatch = readOsmoseFiles(path=path, type="meanSizeCatch"),
                    SizeSpectrum  = readOsmoseFiles(path=path, type="SizeSpectrum")$Abundance,
                    SizeSpectrumN = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesN"),
                    SizeSpectrumB = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesB"),
                    SizeSpectrumC = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesYield"),
                    SizeSpectrumY = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesYieldN"),
                    AgeSpectrumN = readOsmoseFiles(path=path, type="AgeSpectrumSpeciesN"),
                    AgeSpectrumB = readOsmoseFiles(path=path, type="AgeSpectrumSpeciesB"),
                    AgeSpectrumC = readOsmoseFiles(path=path, type="AgeSpectrumSpeciesYield"),
                    AgeSpectrumY = readOsmoseFiles(path=path, type="AgeSpectrumSpeciesYieldN"))
  
  model = list(version  = "3.0b",
               model    = .getModelName(path=path),
               simus    = dim(outputData$biomass)[3],
               times    = as.numeric(row.names(outputData$biomass)),
               T        = nrow(outputData$biomass),
               start    = as.numeric(row.names(outputData$biomass))[1],
               nsp      = ncol(outputData$biomass),
               lspecies = if(!is.null(species.names)) species.names else colnames(outputData$biomass))
  
  output = c(model = list(model), species = list(colnames(outputData$biomass)),
             outputData)
  
  return(output)
  
}