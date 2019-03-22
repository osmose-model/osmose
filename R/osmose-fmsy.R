
##' Title
##'
##' @param sp 
##' @param input.file 
##' @param restart 
##' @param Fmin 
##' @param Fmax 
##' @param StepF 
##' @param Sub_StepF 
##' @param java 
##' @param osmose.exec 
##' @param ... 
##'
##' @return
##' @export
#F_msy <- function(sp, input.file, restart=FALSE, 
#                  Fmin=0, Fmax=2, StepF=0.1, Sub_StepF=0.01, java="java", osmose.exec=NULL, ...) {
#
#  Param = readOsmoseConfiguration(input.file)
#  if(is.numeric(sp)){
#    index = paste0("sp", sp)   # recovers "spX" string
#    species = getOsmoseParameter(Param, "species", "name", index)   # recovers species name 
#  }else{
#    species = sp   # recovers species name as argument
#    all_species_names = Param$species$name   # recovers all the species names
#    test = which(all_species_names == species)   # find the index that corresponds to the species name
#    if(length(test) == 0) {
#      stop("error on species indexation, invalid species name provided")
#    }
#    
#    pattern = "sp([0-9]+)"
#    sp = as.numeric(sub(x=names(test), pattern=pattern, replacement="\\1"))
#    
#  }
#  
#  index = paste0("sp", sp)   # recovers "spX" string
#  
#  # par = list()
#  #par$species = species
#  #par$F.base = getParameters(Param,paste0("mortality.fishing.rate.sp",sp),what="numeric")
#  #par$T = 1# may be 1, looping
#  #par$dt = getParameters(Param,"simulation.time.ndtperyear",what="numeric")
#  #par$longevity = getParameters(Param,paste0("species.lifespan.sp",sp),what="numeric")
#  #par$Linf = getParameters(Param,paste0("species.linf.sp",sp),what="numeric")
#  #par$fishery = TRUE
#  IsSeasonal = FALSE
#  
#  
#  if(existOsmoseParameter(Param, "mortality", "fishing", "rate", "byDt", "byAge", "file", index)){
#      selectivity.by = "age"
#      IsSeasonal = TRUE
#    }
#
#  if(existOsmoseParameter(Param, "mortality", "fishing", "rate", "byDt", "bySize", "file", index)){
#      selectivity.by = "size"
#      IsSeasonal = TRUE
#  }
#  
#  #par$selectivity.type = selectivity.type
#  #if(is.null(L50)){
#  # par$L50 = getParameters(Param,paste0("mortality.fishing.recruitment.",par$selectivity.by,".sp",sp),what="numeric")  	# recruitment age or length (read from osmose)
#  #}else{
#  #par$L50 = L50
#  #}
#  #par$L75 = L75
#  #par$F.month = read.csv(getParameters(Param,paste0("mortality.fishing.season.distrib.file.sp",sp)),sep=";") # read the seasonality file
#
#
#  if(IsSeasonal){
#    fishing.file = getOsmoseParameter(Param, "mortality", "fishing", "rate", "byDt", paste0("by", selectivity.by), "file", index))
#    F.rate = read.csv(fishing.file, sep=";")
#    fishing.folder = normalizePath(dirname(fishing.file))
#  }else{
#    F.rate = getOsmoseParameters(Param, "mortality", "fishing", "rate", index))
#  }
#  
#  # # Creation of a new parameters files in order to be modified
#  # MsyFile = list()
#  # 
#  # MsyFile[["osmose.configuration.Fmsy"]] = paste0("Fmsy-parameters_sp",sp,".csv")
#  # MsyFile[["osmose.configuration.main"]] = input.file
#  # 
#  # writeOsmoseParameters(MsyFile,file.path(input.folder,paste0("configFmsy_sp",sp,".csv")))
#  # 
#  # # Temporary file
#  # 
#  # if (file.exists(paste0("FmsyTemp_sp",sp,".csv"))){
#  #   FmsyTemp = read.csv(paste0("FmsyTemp_sp",sp,".csv"),header=T,sep=",",dec=".",row.names=1)
#  # }else{
#  #   FmsyTemp = data.frame(iSteps=1,FcollapseUpper=NA,FmsyUpper=NA)
#  #   write.csv(FmsyTemp,file=paste0("FmsyTemp_sp",sp,".csv"))
#  # }
#  # 
#  # iSteps = FmsyTemp$iSteps
#  # FcollapseUpper = FmsyTemp$FcollapseUpper
#  # FmsyUpper = FmsyTemp$FmsyUpper
#  # 
#  # # Creation of a vector of F that will contain Fishing mortality rates at each Step	
#  # Fval = c(seq(Fmin,Fmin+StepF-Sub_StepF,Sub_StepF),seq(Fmin+StepF,Fmax,StepF))
#  # N_Fval = length(Fval)
#  # N_Fcollapse = length(seq(Fmin,Fmin+StepF,Sub_StepF))
#  # N_Fmsy = length(seq(Fmin,Fmin+(2*StepF),Sub_StepF))
#  # 
#  # # To create R output folder if is not
#  # if (!file.exists(file.path(input.folder,"output"))) {
#  #   dir.create(file.path(input.folder,"output"))		
#  # }
#  # if (!file.exists(file.path(input.folder,"output/Fmsy_R"))) {
#  #   dir.create(file.path(input.folder,"output/Fmsy_R"))
#  # }		
#  # 
#  # # Creation of the Res file
#  # 
#  # Res = list(B0=vector(),vect_F=vector(),Y_Fvect=list(),B_Fvect=list(),B_CI = list(), Y_CI = list(),MSY=vector(),Fcollapse=vector())
#  # 
#  # # Do you want to restart?
#  # if(Restart==TRUE){
#  #   load(file.path(input.folder,paste0("output/Fmsy_R/Res",sp)))
#  # }else{
#  #   iSteps=1
#  #   FcollapseUpper = NA
#  #   FmsyUpper = NA
#  #   
#  #   FmsyTemp$iSteps = iSteps
#  #   FmsyTemp$FcollapseUpper =FcollapseUpper
#  #   FmsyTemp$FmsyUpper = FmsyUpper
#  #   write.csv(FmsyTemp,file=paste0("FmsyTemp_sp",sp,".csv"))
#  # }
#  # 
#  # ### Start of the run	
#  # 
#  # for(i in iSteps:(N_Fval+N_Fcollapse+N_Fmsy)){
#  #   cat("i",i,"\n")
#  #   
#  #   # To complete matrix F once FcollapseUpper is found
#  #   #if (!is.na(FcollapseUpper)&is.na(F[(length(Fval)+1),NumEsp])){
#  #   if (!is.na(FcollapseUpper)&i==(N_Fval+1)){
#  #     if(FcollapseUpper>=Fmin&FcollapseUpper<(Fmin+StepF)){
#  #       FcollapseUpper=Fmin+StepF
#  #     }	
#  #     Fval  = c(Fval,seq(FcollapseUpper-StepF,FcollapseUpper,Sub_StepF))
#  #   }
#  #   
#  #   # To complete matrix F once FmsyUpper is found		
#  #   if (!is.na(FmsyUpper)&i==(N_Fval+N_Fcollapse+1)){
#  #     if(FmsyUpper>=Fmin&FmsyUpper<(Fmin+StepF)){
#  #       FmsyUpper=Fmin+StepF
#  #     }		
#  #     Fval = c(Fval,rev(seq(FmsyUpper-StepF,FmsyUpper+StepF,Sub_StepF)))
#  #   }
#  #   
#  #   if(i>N_Fval&length(Fval)==N_Fval){
#  #     next
#  #   }else{
#  #     
#  #     if(sum(as.character(Fval[i])==as.character(Res$vect_F),na.rm=T)>0){
#  #       
#  #       Res$vect_F[i] = Res$vect_F[which(as.character(Fval[i])==as.character(Res$vect_F))][1]
#  #       Res$B_Fvect[[i]] = Res$B_Fvect[[which(as.character(Fval[i])==as.character(Res$vect_F))[1]]]
#  #       Res$Y_Fvect[[i]] = Res$Y_Fvect[[which(as.character(Fval[i])==as.character(Res$vect_F))[1]]]
#  #       #Res$B_CI[[i]] = Res$B_CI[[which(as.character(Fval[i])==as.character(Res$vect_F))[1]]]
#  #       #Res$Y_CI[[i]] = Res$Y_CI[[which(as.character(Fval[i])==as.character(Res$vect_F))[1]]]
#  #       
#  #       save(Res,file=file.path(input.folder,paste0("output/Fmsy_R/Res",sp)))
#  #       FmsyTemp$iSteps= i+1
#  #       write.csv(FmsyTemp,file=paste0("FmsyTemp_sp",sp,".csv"))
#  #       
#  #       next
#  #       
#  #     }else{
#  #       
#  #       
#  #       
#  #       ### To modify the fishing parameter
#  #       ParamFmsy = list()
#  #       ParamFmsy["output.file.prefix"] = paste("Sp",sp,"F",Fval[i],sep="")
#  #       #WriteOsmoseParameters(ParamFmsy,paste0("Fmsy-parameters_sp",sp,".csv"))
#  #       #writing F fishing.byYear.sp0 = F
#  #       if(IsSeasonal){
#  #         
#  #         Time = F.rate[,1]
#  #         F_file = F.rate[,-1]
#  #         Names = names(F_file)
#  #         
#  #         dtperYear = getParameters(Param,"simulation.time.ndtperyear",what="numeric")
#  #         Year = getParameters(Param,"simulation.time.nyear",what="numeric") - getParameters(Param,"output.start.year",what="numeric")
#  #         
#  #         if(dtperYear*Year!=dim(F.rate)[1]) error("F-file do not have the rigth dimension")
#  #         
#  #         b = seq(0,Year*dtperYear,dtperYear)
#  #         
#  #         for (l in 1:(length(b)-1)){
#  #           
#  #           F_file[(b[l]+1):b[l+1],] = F_file[(b[l]+1):b[l+1],]/rep(apply(F_file[(b[l]+1):b[l+1],],2,sum),1,each=dtperYear)
#  #           
#  #         }
#  #         
#  #         F_file[is.na(F_file)] = 0
#  #         F_file = as.matrix(F_file)
#  #         
#  #         F_file = F_file*Fval[i]
#  #         F_file = cbind(Time,F_file)
#  #         colnames(F_file) = c("",substr(Names,2,length(Names)))
#  #         write.table(F_file,file.path(fishing.folder,paste0("F-",species,"_msy.csv")),row.names=FALSE,quote=FALSE,sep=";")
#  #         ParamFmsy[paste0("mortality.fishing.rate.byDt.by",selectivity.by,".file.sp",sp)] = file.path(fishing.folder,paste0("F-",species,"_msy.csv"))
#  #         writeOsmoseParameters(ParamFmsy,paste0("Fmsy-parameters_sp",sp,".csv"))
#  #         
#  #       } else {
#  #         
#  #         F_rate = Fval[i]
#  #         ParamFmsy[paste0("mortality.fishing.rate.sp",sp)] = F_rate
#  #         writeOsmoseParameters(ParamFmsy,paste0("Fmsy-parameters_sp",sp,".csv"))
#  #         
#  #       }
#  #       ### To run Osmose with R
#  #       
#  #       runOsmose(osmose.exec,java,paste0("configFmsy_sp",sp,".csv"),paste("output/Sp",sp,"F",Fval[i],sep=""))
#  #       
#  #       #####################################################
#  #       
#  #       ### Load output files (Biomass and Yield)
#  #       
#  #       out = osmose2R(paste("output/Sp",sp,"F",Fval[i],sep=""))
#  #       biomass = out$global$biomass
#  #       yield = out$global$yield
#  #       #yield = getVar(out,"yield")
#  #       #FilesBiomass = readOsmoseFiles(path=paste0("output/Sp",sp,"F",Fval[i]),type="biomass")
#  #       #FilesYield = readOsmoseFiles(path=paste0("output/Sp",sp,"F",Fval[i]),type="yield")
#  #       
#  #       #FilesBiomassSelectTime = SelectYear(FilesBiomass,SimuPars1,SimuPars2,OutputPars1,OutputPars2,Year=Year)
#  #       #FilesBiomassMeanTime = MeanYear(FilesBiomassSelectTime,SimuPars1,SimuPars2,OutputPars1,OutputPars2,type="biomass")
#  #       #FilesBiomassCI = calculateCI(FilesBiomassMeanTime,1,alpha=0.05,prob=NULL,useMean=TRUE)
#  #       
#  #       
#  #       #FilesYieldSelectTime = SelectYear(FilesYield,SimuPars1,SimuPars2,OutputPars1,OutputPars2,Year=Year)
#  #       #FilesYieldMeanTime = MeanYear(FilesYieldSelectTime,SimuPars1,SimuPars2,OutputPars1,OutputPars2,type="yield")
#  #       #FilesYieldCI = calculateCI(FilesYieldMeanTime,1,alpha=0.05,prob=NULL,useMean=TRUE)			
#  #       
#  #       
#  #       if (Fval[i]==0) {	
#  #         
#  #         Res$B0=mean(biomass[,species,])
#  #         cat("B0=",Res$B0,"\n")
#  #         
#  #       }
#  #       
#  #       Res$B_Fvect[[i]]=apply(biomass[,species,],2,mean)
#  #       Res$Y_Fvect[[i]]=apply(yield[,species,],2,mean)
#  #       #Res$B_CI[[i]] = FilesBiomassCI[NumEsp,]
#  #       #Res$Y_CI[[i]] = FilesYieldCI[NumEsp,]
#  #       
#  #       # To check if FcollapseUpper is reached				
#  #       if(sum((mean(biomass[,species,]))<=(10*Res$B0/100))>0&is.na(FcollapseUpper)){
#  #         
#  #         FcollapseUpper = Fval[i]
#  #         FmsyTemp$FcollapseUpper = FcollapseUpper			
#  #       }	# end of if
#  #       
#  #       # To check if FmsyUpper is reached	
#  #       
#  #       if (i >2){
#  #         #if(sum((Res$Y_CI[[i]][2])<(Res$Y_CI[[i-1]][2])&is.na(FmsyUpper))>0){
#  #         if(sum((mean(Res$Y_Fvect[[i]]))<(mean(Res$Y_Fvect[[i-1]]))&(mean(Res$Y_Fvect[[i]]))<(mean(Res$Y_Fvect[[i-2]]))&is.na(FmsyUpper))>0){				
#  #           FmsyUpper=Fval[i-2]
#  #           FmsyTemp$FmsyUpper = FmsyUpper			
#  #         }	# end of if	
#  #       }
#  #       
#  #       ### To save values
#  #       
#  #       Res$vect_F[i] = Fval[i]
#  #       save(Res,file=file.path(input.folder,paste0("output/Fmsy_R/Res",sp)))
#  #       FmsyTemp$iSteps[1] = i+1
#  #       Restart=TRUE
#  #       write.csv(FmsyTemp,file=paste0("FmsyTemp_sp",sp,".csv"))
#  #     }
#  #   }	
#  # } # end of i
#  # 
#  # 
#  # 
#  # if(length(which(is.na(Res$vect_F)))>0){
#  #   
#  #   Res$B_Fvect[which(is.na(Res$vect_F))] = NULL
#  #   Res$Y_Fvect[which(is.na(Res$vect_F))] = NULL
#  #   #Res$B_CI[which(is.na(Res$vect_F))] = NULL
#  #   #Res$Y_CI[which(is.na(Res$vect_F))] = NULL
#  #   Res$vect_F = Res$vect_F[-which(is.na(Res$vect_F))]
#  # }
#  # 
#  # 
#  # Res$B_Fvect = Res$B_Fvect[sort.list(Res$vect_F)]
#  # Res$Y_Fvect = Res$Y_Fvect[sort.list(Res$vect_F)]
#  # #Res$B_CI = Res$B_CI[sort.list(Res$vect_F)]
#  # #Res$Y_CI = Res$Y_CI[sort.list(Res$vect_F)]
#  # Res$vect_F = Res$vect_F[order(Res$vect_F)]
#  # 
#  # save(Res,file=file.path(input.folder,paste0("output/Fmsy_R/Res",sp)))
#  # 
#  # 
#  # ###################################################
#  # #PLOT
#  # 
#  # ### Yield data
#  # 
#  # #Ymean = vector()
#  # 
#  # #for (k in 1:length(Res$Y_Fvect)){
#  # 
#  # # Ymean[k] = mean(Res$Y_Fvect[[k]])
#  # 
#  # #}
#  # 
#  # Ymean = lapply(Res$Y_Fvect,mean)
#  # 
#  # Yvect = Res$Y_Fvect
#  # 
#  # replicat = lapply(Yvect,length)
#  # vectF=list()
#  # for(i in 1:length(replicat)){
#  #   vectF[[i]] = rep(Res$vect_F[i], each = unlist(replicat)[i])
#  # }
#  # 
#  # datY = cbind(unlist(vectF),unlist(Yvect))
#  # 
#  # datY = as.data.frame(datY)
#  # names(datY) = c("F","Y")
#  # 
#  # # create F vector for prediction
#  # Fs = seq(0,max(datY$F),by=0.01)
#  # 
#  # # calculate weight for the fitting
#  # 
#  # jY = jitter(datY$Y)
#  # 
#  # dat.sdY = tapply(jY,INDEX=datY$F,sd)
#  # we0Y = rep(1/dat.sdY,table(datY[,1]))
#  # 
#  # # fit the "gam" with cr spline
#  # 
#  # xY = gam(Y~s(F,bs="cr"),data=datY,weigths=we0)
#  # 
#  # # predict the model
#  # Ys = predict(xY,newdata=data.frame(F=Fs),type="response",se.fit=TRUE)
#  # 
#  # Res$Fmsy = Fs[which.max(Ys$fit)] 
#  # Res$MSY = max(Ys$fit,na.rm=TRUE)
#  # 
#  # 
#  # ### Biomass Data
#  # Bmean = lapply(Res$B_Fvect,mean)
#  # 
#  # Bvect = Res$B_Fvect
#  # 
#  # datB = cbind(unlist(vectF),unlist(Bvect))
#  # 
#  # datB = as.data.frame(datB)
#  # names(datB) = c("F","B")
#  # 
#  # # create F vector for prediction
#  # Fs = seq(0,max(datB$F),by=0.01)
#  # 
#  # # calculate weight for the fitting
#  # 
#  # jB = jitter(datB$B)
#  # 
#  # dat.sdB = tapply(jB,INDEX=datB$F,sd)
#  # we0B = rep(1/dat.sdB,table(datB[,1]))
#  # 
#  # # fit the "gam" with cr spline
#  # 
#  # xB = gam(B~s(F,bs="cr"),data=datB,weigths=we0)
#  # 
#  # # predict the model
#  # Bs = predict(xB,newdata=data.frame(F=Fs),type="response",se.fit=TRUE)
#  # 
#  # Res$Fcollapse = Fs[which(Bs$fit<=((10*Res$B0)/100))[1]] 
#  # 
#  # save(Res,file=file.path(input.folder,paste0("output/Fmsy_R/Res",sp)))
#  # # To plot results
#  # 
#  # 
#  # 
#  # 
#  # #pdf(paste(pathInputOsmose,"output/Fmsy_R/Fmsy",paste0('_',sp,sep='',collapse=''),"1.pdf",sep=""))
#  # 
#  # #	plotAreaCI(Res$vect_F,do.call(rbind,Res$Y_CI))
#  # #	segments(Res$Fcollapse,-10000,Res$Fcollapse,Ys$fit[which(Fs==Res$Fcollapse)],col="red",lty=2)
#  # #	segments(Res$Fmsy,-10000,Res$Fmsy,Res$MSY,col="blue",lty=2)
#  # #	mtext(paste("Fcollapse",Res$Fcollapse,sep="\n"),side=1,at=Res$Fcollapse,col="red")
#  # #	mtext(paste("Fmsy",Res$Fmsy,sep="\n"),side=1,at=Res$Fmsy,col="blue")
#  # #dev.off()
#  # 
#  # pdf(paste0(input.folder,"/output/Fmsy_R/Fmsy_sp",sp,".pdf"))
#  # 
#  # plot(datY$F, datY$Y, pch=19, col="gray", cex=0.5,main=paste("Sp",sp,sep=""))
#  # lines(Fs,Ys$fit, col="red", lwd=2)
#  # points(Res$vect_F, Ymean, pch=19, col="blue", cex=0.7)
#  # if(!is.na(Res$Fcollapse)){
#  #   segments(Res$Fcollapse,-10000,Res$Fcollapse,Ys$fit[which(Fs==Res$Fcollapse)],col="red",lty=2)
#  #   mtext(paste("Fcollapse",Res$Fcollapse,sep="\n"),side=1,at=Res$Fcollapse,col="red")
#  # }
#  # if(!is.na(Res$Fmsy)){
#  #   segments(Res$Fmsy,-10000,Res$Fmsy,Res$MSY,col="blue",lty=2)
#  #   mtext(paste("Fmsy",Res$Fmsy,sep="\n"),side=1,at=Res$Fmsy,col="blue")
#  # }
#  # 
#  # dev.off()
#  # 
#  # ##To remove temporary files
#  # file.remove(paste0("configFmsy_sp",sp,".csv"))
#  # file.remove(paste0("FmsyTemp_sp",sp,".csv"))
#  # file.remove(paste0("Fmsy-parameters_sp",sp,".csv"))
#  # file.remove(file.path(fishing.folder,paste0("F-",species,"_msy.csv")))
#  # 
#  # return(Res)	
#  
#} # end of Fmsy
