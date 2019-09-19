
#' Finds the F_msy value for a given species
#'
#' @param sp Index of the species from which F_msy is analysed
#' @param input.file Path of the configuration file
#' @param restart True if Fmsy should start from restart
#' @param Fmin Minimum value of the fishing mortality
#' @param Fmax Maximum value of the fishing mortality
#' @param StepF Fishing mortality step between Fmin and Fmax
#' @param Sub_StepF Fishing mortality step between Fmin and StepF
#' @param ...  Additionnal arguments of the run_osmose function
#'
#' @export
F_msy <- function(sp, input.file, restart=FALSE, 
                  Fmin=0, Fmax=2, StepF=0.1, Sub_StepF=0.01, ...) {
  
  # Initial values
  input.folder = normalizePath(dirname(input.file))
  
  Param = readOsmoseConfiguration(input.file)
  
  if(is.numeric(sp)){
    index = paste0("sp", sp)   # recovers "spX" string
    species = getOsmoseParameter(Param, "species", "name", index)   # recovers species name 
  } else {
    species = sp   # recovers species name as argument
    all_species_names = Param$species$name   # recovers all the species names
    test = which(all_species_names == species)   # find the index that corresponds to the species name
    if(length(test) == 0) {
      stop("error on species indexation, invalid species name provided")
    }
    
    pattern = "sp([0-9]+)"
    sp = as.numeric(sub(x=names(test), pattern=pattern, replacement="\\1"))
    
  }
  
  index = paste0("sp", sp)   # recovers "spX" string
  
  IsSeasonal = FALSE
  
  if(existOsmoseParameter(Param, "mortality", "fishing", "rate", "byDt", "byAge", "file", index)){
    selectivity.by = "age"
    IsSeasonal = TRUE
  }
  
  if(existOsmoseParameter(Param, "mortality", "fishing", "rate", "byDt", "bySize", "file", index)){
    selectivity.by = "size"
    IsSeasonal = TRUE
  }
  
  if(IsSeasonal){
    fishing.file = getOsmoseParameter(Param, "mortality", "fishing", "rate", "byDt", paste0("by", selectivity.by), "file", index)
    F.rate = read.csv(fishing.file, sep=";")
    fishing.folder = normalizePath(dirname(fishing.file))
  } else {
    F.rate = getOsmoseParameter(Param, "mortality", "fishing", "rate", index)
  }
  
  # Creation of a new parameters files in order to be modified
  # contains the path of the two new configuration files. 
  # The FMSY file, which will have precedence on the main configuration file
  MsyFile = list()
  MsyFile[["osmose.configuration.Fmsy"]] = file.path(input.folder, paste0("Fmsy-parameters_sp", sp, ".csv"))
  MsyFile[["osmose.configuration.main"]] = file.path(input.folder, basename(input.file))
  
  writeOsmoseParameters(MsyFile, file.path(input.folder, paste0("configFmsy_sp", sp, ".csv")))
  
  # Temporary file
  # creates the FMSY configuration file if does not exist
  if (file.exists(paste0("FmsyTemp_sp", sp, ".csv"))){
    FmsyTemp = read.csv(paste0("FmsyTemp_sp", sp, ".csv"), header=T, sep=",", dec=".", row.names=1)
  } else {
    FmsyTemp = data.frame(iSteps=1, FcollapseUpper=NA, FmsyUpper=NA)
    write.csv(FmsyTemp, file=paste0("FmsyTemp_sp", sp, ".csv"))
  }
  
  iSteps = FmsyTemp$iSteps
  FcollapseUpper = FmsyTemp$FcollapseUpper
  FmsyUpper = FmsyTemp$FmsyUpper
  
  # Creation of a vector of F that will contain Fishing mortality rates at each Step
  Fval = c(seq(Fmin, Fmin + StepF - Sub_StepF, Sub_StepF), seq(Fmin+StepF, Fmax, StepF))
  N_Fval = length(Fval)   # number of fishing values
  cat("@@@@@@@@@@@@@@@@@@@@@ Fval: ", N_Fval, "\n")
  print(Fval)
  
  N_Fcollapse = length(seq(Fmin, Fmin+StepF, Sub_StepF))   # number of time steps from Fmin to StepF
  cat("@@@@@@@@@@@@@@@@@@@@@ Collapse: ", N_Fcollapse, "\n")
  print(seq(Fmin, Fmin+StepF, Sub_StepF))
  
  N_Fmsy = length(seq(Fmin, Fmin+(2*StepF), Sub_StepF))
  cat("@@@@@@@@@@@@@@@@@@@@@ F_msy: ", N_Fmsy, "\n")
  print(seq(Fmin, Fmin+(2*StepF), Sub_StepF))  
  
  # To create R output folder if is not
  if (!file.exists(file.path("output"))) {
    dir.create(file.path("output"))
  }
  if (!file.exists(file.path("output", "Fmsy_R"))) {
    dir.create(file.path("output", "Fmsy_R"))
  }
  
  # Creation of the Res file
  Res = list(B0=vector(), vect_F=vector(), Y_Fvect=list(), B_Fvect=list(),
             B_CI=list(), Y_CI=list(), MSY=vector(), Fcollapse=vector())
  
  # Do you want to restart?
  if(restart == TRUE){
    load(file.path("output", "Fmsy_R", paste0("Res", sp)))
  } else {
    iSteps = 1
    FcollapseUpper = NA
    FmsyUpper = NA
    FmsyTemp$iSteps = iSteps
    FmsyTemp$FcollapseUpper = FcollapseUpper
    FmsyTemp$FmsyUpper = FmsyUpper
    write.csv(FmsyTemp, file=paste0("FmsyTemp_sp", sp, ".csv"))
  }
  
  update_upper = TRUE
  update_collapse = TRUE
  
  cpt = iSteps
  while(!is.na(Fval[cpt])) {
    
    i = cpt
    
    # To complete matrix F once FcollapseUpper is found
    #if (!is.na(FcollapseUpper)&is.na(F[(length(Fval)+1),NumEsp])){
    if (!is.na(FcollapseUpper) & update_collapse){
      if((FcollapseUpper >= Fmin) & (FcollapseUpper < (Fmin + StepF))) {
        FcollapseUpper = Fmin + StepF
      }
      Fval  = c(Fval, seq(FcollapseUpper-StepF, FcollapseUpper, Sub_StepF))
      print("New Fval Collapse")
      print(Fval)
      update_collapse = FALSE
    }
    
    # To complete matrix F once FmsyUpper is found
    if (!is.na(FmsyUpper) & update_upper){
      if((FmsyUpper >= Fmin) & (FmsyUpper < (Fmin + StepF))) {
        FmsyUpper = Fmin + StepF
      }
      Fval = c(Fval,rev(seq(FmsyUpper-StepF,FmsyUpper+StepF,Sub_StepF)))
      print("New Fval Collapse")
      print(Fval)
      update_upper = FALSE
    }
    
    ### To modify the fishing parameter
    ParamFmsy = list()
    ParamFmsy["output.file.prefix"] = paste("Sp", sp, "F", Fval[i], sep="")
    #WriteOsmoseParameters(ParamFmsy,paste0("Fmsy-parameters_sp",sp,".csv"))
    #writing F fishing.byYear.sp0 = F
    if(IsSeasonal){
      
      Time = F.rate[ , ]
      F_file = F.rate[ , -1]
      for (l in 1:(length(b) - 1)){
        
        F_file[(b[l]+ 1 ):b[l + 1], ] = F_file[(b[l]+1):b[l+1],]/rep(apply(F_file[(b[l]+1):b[l+1],],2,sum), 1, each=dtperYear)
        
      }
      
      F_file[is.na(F_file)] = 0
      F_file = as.matrix(F_file)
      
      F_file = F_file*Fval[i]
      F_file = cbind(Time,F_file)
      colnames(F_file) = c("",substr(Names,2,length(Names)))
      write.table(F_file,file.path(fishing.folder,paste0("F-",species,"_msy.csv")),row.names=FALSE,quote=FALSE,sep=";")
      ParamFmsy[paste0("mortality.fishing.rate.byDt.by",selectivity.by,".file.sp",sp)] = file.path(fishing.folder,paste0("F-",species,"_msy.csv"))
      writeOsmoseParameters(ParamFmsy,paste0("Fmsy-parameters_sp",sp,".csv"))
      
    } else {
      
      F_rate = Fval[i]
      ParamFmsy[paste0("mortality.fishing.rate.sp", sp)] = F_rate
      writeOsmoseParameters(ParamFmsy, file.path(input.folder, paste0("Fmsy-parameters_sp", sp, ".csv")))
      
    }
    
    ### To run Osmose with R
    run_osmose(file.path(input.folder, paste0("configFmsy_sp", sp, ".csv")), output=file.path("output", paste("Sp", sp, "F", Fval[i], sep="")), ...)
    
    #####################################################
    
    ### Load output files (Biomass and Yield)
    
    out = read_osmose(file.path("output", paste("Sp", sp, "F", Fval[i], sep="")))
    biomass = getVar(out, "biomass")
    yield = getVar(out, "yield")
 
    if (Fval[i]==0) {
      
      # B0 = average biomass over all time steps and replicates
      Res$B0 = mean(biomass[, species, ])   #  biomass when no fishing is applied on the current species
      cat("B0=", Res$B0, "\n")
      
    }
    
    # biomass = (time, species, replicates)
    # biomass[, species,] = (time, replicates)
    # here, computation of time average (dim 2 is kept save)
    Res$B_Fvect[[i]] = apply(biomass[,species,], 2, mean)   # dimension N replicates
    Res$Y_Fvect[[i]] = apply(yield[,species,], 2, mean)   # dimension N replicates
    #Res$B_CI[[i]] = FilesBiomassCI[NumEsp,]
    #Res$Y_CI[[i]] = FilesYieldCI[NumEsp,]
    
    # To check if FcollapseUpper is reached
    # FcollapseUpper is the F value for which biomass is less than 10% of biomass without fishing
    if((sum((mean(biomass[, species, ])) <= (10 * Res$B0 / 100))> 0) & is.na(FcollapseUpper)){
      FcollapseUpper = Fval[i]
      FmsyTemp$FcollapseUpper = FcollapseUpper
    }	# end of if
    
    # To check if FmsyUpper is reached. 
    # FmsyUpper assumed to be the value  where the next two values of yields are smaller 
    if (i > 2){
      if(sum(((mean(Res$Y_Fvect[[i]])) < (mean(Res$Y_Fvect[[i-1]]))) & ((mean(Res$Y_Fvect[[i]])) < (mean(Res$Y_Fvect[[i-2]]))) & is.na(FmsyUpper)) > 0) {  # end of sunn
        FmsyUpper = Fval[i-2]
        FmsyTemp$FmsyUpper = FmsyUpper
      }	# end of if
    }
    
    ### To save values for restart
    Res$vect_F[i] = Fval[i]
    save(Res, file=file.path("output", "Fmsy_R", paste0("Res", sp)))
    FmsyTemp$iSteps[1] = i + 1
    Restart = TRUE
    write.csv(FmsyTemp, file=paste0("FmsyTemp_sp",sp,".csv"))
    
    cpt = cpt + 1  
    
  }
  
  if(length(which(is.na(Res$vect_F)))>0){
    
    Res$B_Fvect[which(is.na(Res$vect_F))] = NULL
    Res$Y_Fvect[which(is.na(Res$vect_F))] = NULL
    Res$vect_F = Res$vect_F[-which(is.na(Res$vect_F))]
  }
  
  # sort the biomass, yields values by increasing fishing mortality
  Res$B_Fvect = Res$B_Fvect[sort.list(Res$vect_F)]
  Res$Y_Fvect = Res$Y_Fvect[sort.list(Res$vect_F)]
  Res$vect_F = Res$vect_F[order(Res$vect_F)]
  
  save(Res, file=file.path("output", "Fmsy_R", paste0("Res", sp)))
  
  ###################################################
  #PLOT
  
  ### Yield data
  
  #Ymean = vector()
  
  #for (k in 1:length(Res$Y_Fvect)){
  
  # Ymean[k] = mean(Res$Y_Fvect[[k]])
  
  #}
  
  Ymean = lapply(Res$Y_Fvect, mean)
  
  Yvect = Res$Y_Fvect
  
  replicat = lapply(Yvect,length)
  vectF=list()
  for(i in 1:length(replicat)){
    vectF[[i]] = rep(Res$vect_F[i], each = unlist(replicat)[i])
  }
  
  datY = cbind(unlist(vectF), unlist(Yvect))
  
  datY = as.data.frame(datY)
  names(datY) = c("F","Y")
  
  # create F vector for prediction
  Fs = seq(0, max(datY$F), by=0.01)
  
  # calculate weight for the fitting
  
  jY = jitter(datY$Y)
  
  dat.sdY = tapply(jY, INDEX=datY$F, sd)
  we0Y = rep(1/dat.sdY, table(datY[,1]))
  
  # fit the "gam" with cr spline
  
  xY = gam(Y~s(F, bs="cr"), data=datY, weigths=we0)
  
  # predict the model
  Ys = predict(xY, newdata=data.frame(F=Fs), type="response", se.fit=TRUE)
  
  Res$Fmsy = Fs[which.max(Ys$fit)]
  Res$MSY = max(Ys$fit,na.rm=TRUE)
  
  ### Biomass Data
  Bmean = lapply(Res$B_Fvect,mean)
  
  Bvect = Res$B_Fvect
  
  datB = cbind(unlist(vectF),unlist(Bvect))
  
  datB = as.data.frame(datB)
  names(datB) = c("F","B")
  
  # create F vector for prediction
  Fs = seq(0,max(datB$F),by=0.01)
  
  # calculate weight for the fitting
  
  jB = jitter(datB$B)
  
  dat.sdB = tapply(jB,INDEX=datB$F,sd)
  we0B = rep(1/dat.sdB,table(datB[,1]))
  
  # fit the "gam" with cr spline
  
  xB = gam(B~s(F,bs="cr"),data=datB,weigths=we0)
  
  # predict the model
  Bs = predict(xB,newdata=data.frame(F=Fs),type="response",se.fit=TRUE)
  
  Res$Fcollapse = Fs[which(Bs$fit<=((10*Res$B0)/100))[1]]
  
  save(Res, file=file.path("output", "Fmsy_R", paste0("Res",sp)))
  # To plot results
  
  #pdf(paste(pathInputOsmose,"output/Fmsy_R/Fmsy",paste0('_',sp,sep='',collapse=''),"1.pdf",sep=""))
  
  #	plotAreaCI(Res$vect_F,do.call(rbind,Res$Y_CI))
  #	segments(Res$Fcollapse,-10000,Res$Fcollapse,Ys$fit[which(Fs==Res$Fcollapse)],col="red",lty=2)
  #	segments(Res$Fmsy,-10000,Res$Fmsy,Res$MSY,col="blue",lty=2)
  #	mtext(paste("Fcollapse",Res$Fcollapse,sep="\n"),side=1,at=Res$Fcollapse,col="red")
  #	mtext(paste("Fmsy",Res$Fmsy,sep="\n"),side=1,at=Res$Fmsy,col="blue")
  #dev.off()
  
  pdf(file.path("output", "Fmsy_R", paste0("Fmsy_sp", sp, ".pdf")))
  
  plot(datY$F, datY$Y, pch=19, col="gray", cex=0.5, main=paste("Sp", sp, sep=""))
  lines(Fs, Ys$fit, col="red", lwd=2)
  points(Res$vect_F, Ymean, pch=19, col="blue", cex=0.7)
  if(!is.na(Res$Fcollapse)){
    segments(Res$Fcollapse, -10000, Res$Fcollapse, Ys$fit[which(Fs==Res$Fcollapse)], col="red", lty=2)
    mtext(paste("Fcollapse", Res$Fcollapse,sep="\n"), side=1, at=Res$Fcollapse, col="red")
  }
  if(!is.na(Res$Fmsy)){
    segments(Res$Fmsy, -10000, Res$Fmsy, Res$MSY, col="blue", lty=2)
    mtext(paste("Fmsy", Res$Fmsy, sep="\n"), side=1, at=Res$Fmsy, col="blue")
  }
  
  dev.off()
  
  ##To remove temporary files
  file.remove(file.path(input.folder, paste0("configFmsy_sp", sp,".csv")))
  file.remove(paste0("FmsyTemp_sp",sp,".csv"))
  file.remove(file.path(input.folder, paste0("Fmsy-parameters_sp",sp,".csv")))
  
  if(IsSeasonal) {
    file.remove(file.path(fishing.folder, paste0("F-", species, "_msy.csv")))
  }
  
  return(Res)
  
} # end of Fmsy
