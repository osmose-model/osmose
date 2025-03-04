
# Internal ----------------------------------------------------------------

.calculate_size_residuals_byage = function(x, conf, tiny=1e-4) {
  
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
    isize = VB(age=iage, this, method=1) # replace with integrated size
    xii = as.numeric(colMeans(xi, na.rm=TRUE))
    out[[i]] = log((isize + tiny)/(xii + tiny))
    out1[[i]] = array(rep(isize, each=nrow(xi)), dim=dim(xi))
  }
  
  names(out) = names(xx)
  names(out1) = names(xx)
  
  class(out) = c("osmose.residualSizeByAge", "list")
  class(out1) = c("osmose.expectedSizeByAge", "list")
  
  x$residualSizeByAge = out
  x$expectedSizeByAge = out1
  
  return(x)
  
}

.calculate_mort_residuals_byage = function(x, conf, tiny=1e-4) {
  
  L = x$meanSizeByAge
  M = x$mortalityByAge
  
  if(is.null(L)|is.null(M)) return(x)
  if(length(L)==0|length(M)==0) return(x)
  
  spp = names(L)
  ndt = get_par(conf, "simulation.time.ndtPerYear")/get_par(conf, "output.recordfrequency.ndt$")
  
  out  = list()
  out1 = list()
  out2 = list()
  
  for(i in seq_along(spp)) {
    
    isp = spp[i]
    
    xx = M[[isp]]
    y  = L[[isp]]
    
    mnat = xx$Mpred + xx$Mstar + xx$Mnat
    mnat = mnat[, seq_len(ncol(y)), , drop=FALSE]
    
    mp = calculateMortality(conf, sp=get_species(conf, sp=isp))
    # should we use size or age-based mortality?
    mproxy = array(mp$M[cut(as.numeric(y), breaks=mp$size, labels=FALSE)],
                   dim=dim(y))/ndt
    mnati = colMeans(mnat, na.rm=TRUE)
    mproxyi = colMeans(mproxy, na.rm=TRUE)
    mdeviate = log((mnati + tiny)/(mproxyi + tiny))
    # out[[i]] = mdeviate[,-1, , drop=FALSE] # remove larval mortality
    out[[i]] = mdeviate[-1] # remove larval mortality
    out1[[i]] = mproxy
    out2[[i]] = mnat
  }
  
  names(out)  = spp
  names(out1) = spp
  names(out2) = spp
  class(out)  = c("osmose.residualMortalityByAge", "list")
  class(out1) = c("osmose.expectedMortalityByAge", "list")
  class(out2) = c("osmose.naturalMortalityByAge", "list")
  x$residualMortalityByAge = out
  x$expectedMortalityByAge = out1
  x$naturalMortalityByAge = out2
  
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
  start = get_par(conf, "simulation.time.start")
  if(is.null(start)) start = 0
  
  if(is.null(from)) from = default[1]
  if(is.null(to))     to = default[2]
  if(is.null(by))     by = default[3]
  
  out = list()
  
  for(i in seq_along(xx)) {
    
    xi = xx[[i]]
    this = get_par(conf, sp=spp[i])
    
    xndt = get_par(this, "fisheries.distrib.recordfrequency.ndt")
    if(is.null(xndt))
      xndt = get_par(this, "fisheries.recordfrequency.ndt")
    
    if(!is.null(xndt)) {
      indt = xndt/ndt
      rowok = (nrow(xi)%/%indt)*indt
      if(nrow(xi)!=rowok) xi = xi[seq_len(rowok), , ,drop=FALSE]
      ntime = as.numeric(rownames(xi))[c(rep(FALSE, indt-1), TRUE)]
      del = mean(diff(ntime))/2
      ntime = start + ntime - del
      xi = rowsum(xi, group=rep(seq_len(rowok/indt), each=indt), na.rm=TRUE)
      rownames(xi) = ntime
    } else {
      rownames(xi) = as.numeric(rownames(xi)) + start
    }
    
    xfrom = get_par(this, sprintf("fisheries.distrib.by%s.min", class))
    xto   = get_par(this, sprintf("fisheries.distrib.by%s.max", class))
    xby   = get_par(this, sprintf("fisheries.distrib.by%s.incr", class))
    usemarks = get_par(this, sprintf("fisheries.distrib.by%s.usemarks", class))
    plus  = get_par(this, sprintf("fisheries.distrib.by%s.plusgroup", class))
    
    check = !all(is.null(xfrom), is.null(xto), is.null(xby))
    
    if(check) {
      
      if(is.null(xfrom))        xfrom = from
      if(is.null(xto))            xto = to
      if(is.null(xby))            xby = by
      if(is.null(usemarks))  usemarks = FALSE
      if(is.null(plus))  plus = TRUE
      
      check1 = !all(xfrom==from, xto==to, xby==by)
      
      if(check1) {
        breaks0 = seq(from=xfrom, to=xto+xby, by=xby)
        if(plus) breaks0[length(breaks0)] = Inf
        breaks1 = breaks0 - 0.5*xby
        breaks = if(usemarks) breaks1 else breaks0
        marks = as.numeric(colnames(xi))
        gg = cut(marks, breaks = breaks, right=FALSE, labels = FALSE)
        gg0 = cut(marks, breaks = breaks0, right=FALSE, labels = FALSE)
        if(any(is.na(gg))) {
          xi = xi[, !is.na(gg), ,drop=FALSE]
          # marks = marks[!is.na(gg)]
          gg = gg[!is.na(gg)]
          gg0 = gg0[!is.na(gg)] #  keeping track of labels only.
        }
        xi = colsum(xi, group=gg, na.rm=TRUE)
        colnames(xi) = marks[!duplicated(gg0) & !is.na(gg0)] # gg0 has the right labels always.
      }
    }
    
    out[[i]] = xi
  }
  names(out) = names(xx)
  class(out) = class(xx)
  
  x[[xn]] = out
  
  return(x)
  
}  

.aggregate_catch_bytime = function(x, conf, type) {
  
  ndt = get_par(conf, "output.recordfrequency")
  start = get_par(conf, "simulation.time.start")
  if(is.null(start)) start = 0
  
  y = get_var.osmose(x, type, no.error=TRUE)
  if(is.null(y)) return(x)
  spp = colnames(y)
  xx = list()
  for(i in seq_len(ncol(y))) {
    xi = y[, i, , drop=FALSE]
    isp = get_codes(spp[i], conf)
    if(!is.null(isp)) {
      xndt = unlist(sapply(isp, FUN=.get_rf, conf=conf, type=attr(isp, "xtype")))
      if(!is.null(xndt)) xndt = max(xndt, na.rm=TRUE)
    } else {
      xndt = NULL
    }
    if(!is.null(xndt)) {
      indt = xndt/ndt
      rowok = (nrow(xi)%/%indt)*indt
      if(nrow(xi)!=rowok) xi = xi[seq_len(rowok), , ,drop=FALSE]
      ntime = as.numeric(rownames(xi))[c(rep(FALSE, indt-1), TRUE)]
      del = mean(diff(ntime))/2
      ntime = start + ntime - del
      xi = rowsum(xi, group=rep(seq_len(rowok/indt), each=indt), na.rm=TRUE)
      rownames(xi) = ntime
    } else {
      rownames(xi) = as.numeric(rownames(xi)) + start
    }
    
    xx[[i]] = xi
    
  }
  
  names(xx) = spp
  class(xx) = "osmose.yieldBySpecies"
  # x[["yieldBySpecies"]] = xx
  
  return(xx)
  
}


.aggregate_catch_byyear = function(x, conf, type) {
  
  ndt = get_par(conf, "output.recordfrequency")
  xdt = get_par(conf, "simulation.time.ndtperyear")
  
  y = get_var.osmose(x, type, no.error=TRUE)
  if(is.null(y)) return(x)
  spp = colnames(y)
  xx = list()
  for(i in seq_len(ncol(y))) {
    xi = y[, i, , drop=FALSE]
    indt = xdt/ndt
    rowok = (nrow(xi)%/%indt)*indt
    if(nrow(xi)!=rowok) xi = xi[seq_len(rowok), , ,drop=FALSE]
    ntime = as.numeric(rownames(xi))[c(rep(FALSE, indt-1), TRUE)]
    xi = rowsum(xi, group=rep(seq_len(rowok/indt), each=indt), na.rm=TRUE)
    rownames(xi) = ntime
    
    xx[[i]] = xi
    
  }
  
  rout = array(dim=c(dim(xi)[1], length(xx), dim(xi)[3]))
  
  for(i in seq_len(ncol(y))) {
    rout[, i, ] = xx[[i]]
  }
  rownames(rout) = rownames(xi)
  colnames(rout) = spp
  class(rout) = "osmose.yieldByYear"
  x[["yieldByYear"]] = rout
  
  return(x)
  
}

.get_rf = function(sp, conf, type) {
  type = match.arg(type, c("species", "fisheries"))
  if(type=="species") this = get_par(conf, sp=sp)
  if(type=="fisheries") this = get_par(conf, fsh=sp)
  xndt = get_par(this, "fisheries.recordfrequency.ndt")
  return(xndt)
}

get_codes = function(sp, conf) {
  out = .get_codes(sp=sp, conf=conf, type="species")
  if(!is.null(out)) attr(out, "xtype") = "species"
  if(is.null(out)) {
    out = .get_codes(sp=sp, conf=conf, type="fisheries")
    if(!is.null(out)) attr(out, "xtype") = "fisheries"
  }
  return(out)
}

.get_codes = function(sp, conf, type="species") {
  code = .get_group_code(conf, sp, type=type)
  if(is.null(code)) {
    fg = .get_functional_groups(conf, type=type)
    if(is.null(fg[[sp]])) return(NULL)
    code = .get_group_code(conf, sp=fg[[sp]], type=type) 
  }
  return(code)
}

.get_group_code = function(conf, sp, type="species") {
  type = match.arg(type, c("species", "fisheries"))
  if(type=="species") code = get_species(conf, sp=sp, null.on.error=TRUE)
  if(type=="fisheries") code = get_fisheries(conf, fsh=sp, null.on.error=TRUE)
  return(code)
}

.write_yield_files = function(x, conf, path) {
  
  model = get_par(conf, "output.file.prefix")
  start = get_par(conf, "simulation.time.start")
  if(is.null(start)) {
    warning("Parameter 'simulation.time.start' is missing. Osmose will assume is 1900.")
    start = 1900
  }
  
  tsize = sapply(x$yieldBySpecies, nrow)
  tt = table(tsize)
  it = as.numeric(names(tt))
  j = 1
  
  files = character(length(tsize))
  names(files) = names(tsize)
  
  for(i in seq_along(it)) {
    
    spnames = names(tsize)[tsize==it[i]]
    year = start + .getYear(x$yieldBySpecies[[spnames[1]]])
    period = .getTimeIndex(x$yieldBySpecies[[spnames[1]]])
    
    freq = it[i]/length(unique(year))
    pnames = c(year=1, semester=2, quarter=4, month=12)
    pname = names(pnames[pnames==freq])
    
    if(length(pname)==0) {
      pname = sprintf("period%02d", j)
      j = j + 1
    }
    
    dfnames = unique(c("year", pname, spnames))
    
    out = matrix(NA, nrow=it[i], ncol=length(dfnames))
    out = as.data.frame(out)
    names(out) = dfnames
    
    out[, 1] = year
    if(pname!="year") out[, 2] = period
    
    fname = sprintf("%s_yield_%s.csv", model, pname)  
    files[spnames] = fname
    
    write_osmose(out, file=file.path(path, fname), row.names = FALSE, col.names = TRUE)
    
  }
  
  names(files) = paste("yield", names(files), sep=".")
  
  return(invisible(files))
  
}


.write_biomass_files = function(x, conf, path, what="biomass") {
  
  model = get_par(conf, "output.file.prefix")
  start = get_par(conf, "simulation.time.start")
  if(is.null(start)) {
    warning("Parameter 'simulation.time.start' is missing. Osmose will assume is 1900.")
    start = 1900
  }
  
  bio = get_var(x, what, expected=TRUE, drop=FALSE)
  
  files = character(ncol(bio))
  names(files) = paste(what, colnames(bio), sep=".")
  
  year = start + .getYear(bio)
  period = .getTimeIndex(bio)
  freq = nrow(bio)/length(unique(year))
  
  pnames = c(year=1, semester=2, quarter=4, month=12)
  pname = names(pnames[pnames==freq])
  
  if(length(pname)==0) pname = "period"
  
  dfnames = unique(c("year", pname))
  
  out = cbind(year)
  if(pname!="year") out = cbind(out, period)
  colnames(out) = dfnames
  
  out = cbind(out, NA*bio)
  
  xwhat = sapply(strsplit(what, split="\\."), FUN=tail, n=1)
  fname = sprintf("%s_%s-index_%s.csv", model, what, pname)  
  
  files[] = fname
  
  write_osmose(out, file=file.path(path, fname), row.names = FALSE, col.names = TRUE)
 
  return(invisible(files))
  
}

.write_cal_files = function(x, conf, path) {
  
  model = get_par(conf, "output.file.prefix")
  start = get_par(conf, "simulation.time.start")
  if(is.null(start)) {
    warning("Parameter 'simulation.time.start' is missing. Osmose will assume is 1900.")
    start = 1900
  }
  
  cal = get_var(x, "yieldNBySize", expected=TRUE)
  files = character(length(cal))
  names(files) = names(cal)

  if(!dir.exists(file.path(path, "catch_at_length")))
    dir.create(file.path(path, "catch_at_length"), recursive=TRUE)
            
  for(i in seq_along(cal)) {
    
    bio = cal[[i]]
    year = start + .getYear(bio)
    period = .getTimeIndex(bio)
    freq = nrow(bio)/length(unique(year))
    
    marks = as.numeric(colnames(bio))
    dl = diff(marks)/2
    colnames(bio) = round(marks + dl[1], 3)
    
    pnames = c(period=1, semester=2, quarter=4, month=12)
    pname = names(pnames[pnames==freq])
    
    if(length(pname)==0) pname = "period"
    
    dfnames = unique(c("year", pname))
    
    out = cbind(year)
    if(pname!="year") out = cbind(out, period)
    colnames(out) = dfnames
    
    out = cbind(out, NA*bio)
    
    if(freq==1) pname = "year"
    fname = sprintf("%s_catchatlength-%s_%s.csv", model, names(cal)[i], pname)  
    files[names(cal)[i]] = fname
    
    write_osmose(out, file=file.path(path, "catch_at_length", fname), row.names = FALSE, col.names = TRUE)
    
  }

  nm = paste("catchatlength", names(files), sep=".")
  files = file.path("catch_at_length", files)
  names(files) = nm
  return(invisible(files))
  
}



.add_to_configuration = function(conf) {
  
  # add total fecudity by time step for mortality calculation
  spp = get_species(conf, type="focal", code=TRUE)
  fec = suppressWarnings(lapply(as.numeric(spp), FUN=function(sp, conf) read.fecundity(conf, sp), conf=conf))
  names(fec) = sprintf("reproduction.fecundity.sp%s", spp)
  conf = c(conf, fec)
  
  return(conf)
  
}


# Calibration settings ----------------------------------------------------

.create_calibration_settings = function(output, files, type) {
  
  out = switch(type, 
         "simple" = .create_calibration_settings_simple(output, files),
         "survey" = .create_calibration_settings_survey(output, files),
         stop("Unrecognized 'type' for calibration.")
         )
  
  return(out)

  }

.create_calibration_settings_simple = function(output, files) {
  
  cal_type = c(biomass="lnorm3", yield="lnorm2", catchatlength="multinom", 
               mortality="penalty", growth="penalty", random="re")
  
  cal_cv = c(biomass=0.25, yield=0.05, catchatlength=1, 
             mortality=1/sqrt(2), growth=1/sqrt(2), random=0.5)
  
  cal_useData = c(biomass=TRUE, yield=TRUE, catchatlength=TRUE, 
                  mortality=FALSE, growth=FALSE, penalty=FALSE, random=FALSE)
  
  cal_novarid = c("catchatlength", "growth", "mortality", "random")
  
  cal_settings = data.frame(variable=names(output))
  cal_settings$itype = sapply(strsplit(names(output), split = "\\."), FUN="[", i=1)
  cal_settings$spp   = sapply(strsplit(names(output), split = "\\."), FUN="[", i=2)
  cal_settings$type  = cal_type[cal_settings$itype]
  cal_settings$calibrate = TRUE
  cal_settings$cv = cal_cv[cal_settings$itype]
  cal_settings$use_data = cal_useData[cal_settings$itype]
  cal_settings$file = files[cal_settings$variable]
  cal_settings$varid = cal_settings$spp
  cal_settings$varid[cal_settings$itype %in% cal_novarid] = NA
  
  cal_settings$nrows = NA
    
  cal_settings$itype = NULL
  cal_settings$spp = NULL
  
  return(cal_settings)
  
}

.create_calibration_settings_survey = function(output, files) {
  
  cal_type = c(biomass="lnorm4", yield="lnorm2", catchatlength="multinom", 
               mortality="penalty", growth="penalty", random="re")
  
  cal_cv = c(biomass=0.25, yield=0.05, catchatlength=1, 
             mortality=1/sqrt(2), growth=1/sqrt(2), random=0.5)
  
  cal_useData = c(biomass=TRUE, yield=TRUE, catchatlength=TRUE, 
                  mortality=FALSE, growth=FALSE, penalty=FALSE, random=FALSE)
  
  cal_novarid = c("catchatlength", "growth", "mortality", "random")
  
  cal_settings = data.frame(variable=names(output))
  cal_settings$itype = sapply(strsplit(names(output), split = "\\."), FUN="[", i=1)
  cal_settings$sname = sapply(strsplit(names(output), split = "\\."), FUN="[", i=2)
  cal_settings$spp   = sapply(strsplit(names(output), split = "\\."), FUN=tail, n=1)
  cal_settings$type  = cal_type[cal_settings$itype]
  cal_settings$calibrate = TRUE
  cal_settings$cv = cal_cv[cal_settings$itype]
  cal_settings$use_data = cal_useData[cal_settings$itype]
  cal_settings$file = files[cal_settings$variable]
  cal_settings$varid = cal_settings$spp
  cal_settings$varid[cal_settings$itype %in% cal_novarid] = NA
  
  cal_settings$nrows = NA
  
  cal_settings$itype = NULL
  cal_settings$sname = NULL
  cal_settings$spp = NULL
  
  return(cal_settings)
  
}


.getTimeIndex = function(x) {
  y = as.numeric(rownames(x)) 
  y = y %% 1
  y1 = min(y[y!=0])
  if(y[1]==0) y[1] = 1e-6
  y[y==0] = 1
  y = y/y1
  y = round(y, 0)
  return(y)
}

.getYear = function(x) {
  y = as.numeric(rownames(x))
  y = y - y[1]
  y = as.integer(y)
  return(y)
}

.dim_or_length = function(x) {
  xdim = dim(x)
  if(!is.null(xdim)) return(xdim)
  return(length(x))
}

.getDataFolder = function(script, data_folder="data_path") {
  txt = readLines(script)
  ind = grep(txt, pattern=data_folder)[1]
  if(is.na(ind)) stop("Could not find the 'data_path'.")
  path = read.table(file=script, sep="=", skip=ind-1, nrows=1, row.names=1)
  path = str_trim(as.character(path))
}



# TESTS -------------------------------------------------------------------


.calibration_test_1 = function(simulated, observed, setup) {

  test1 = identical(sort(names(observed)), sort(names(simulated)))
  
  useData = as.logical(setup$use_data)
  isActive = as.logical(setup$calibrate)
  readData = useData & isActive
  
  if(!test1) {
    message("Pre-calibration test: FAILED. Simulated and observed have not the same variable names. Check your calibration settings.\n")
  }
  
  obs_check = lapply(observed, FUN=.dim_or_length)
  sim_check = lapply(simulated, FUN=.dim_or_length)
  sim_check[!readData] = 1L

  nm = sort(names(observed))
  obs_check = obs_check[nm]
  sim_check = sim_check[nm]
  
  test2 = identical(obs_check, sim_check)
  
  msg = "Error in variable '%s': observed [%s] and simulated [%s] dimensions do not match."
  msg2 = "Check your data and configuration."
  
  if(!test2) {
    for(i in seq_along(obs_check)) {
      .test2 = identical(obs_check[[i]], sim_check[[i]])
      if(.test2) next
      dim1 = paste(obs_check[[i]], collapse=",")
      dim2 = paste(sim_check[[i]], collapse=",")
      message(sprintf(msg, nm[i], dim1, dim2))
    }
    message("Pre-calibration test: FAILED. Simulated and observed data have not the same dimensions. Check your data and configuration.\n")
  }
  
  if(test1 & test2) {
    message("Pre-calibration test: PASSED. Simulated and observed data are compatible.\n")
  } else {
    message("Pre-calibration test: FAILED. Simulated and observed data are NOT compatible.\n")
    stop("Test failed.")
  }
  return(invisible(NULL))
}

.set_reltol = function(x, reltol) {
  x = unlist(x)
  nphase = max(x, na.rm=TRUE)
  out = table(x)
  nm = as.numeric(names(out))
  ind = nm > 0
  out = cumsum(out[ind])
  if(length(out)!=nphase) stop("Incompatible phases configuration.")
  npar = max(out)
  mult = sapply(out, FUN=function(x) max(pretty(npar/x)))
  return(pmin(reltol*mult, 1e-1))
}
