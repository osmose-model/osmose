

.check_density = function(conf) {
  
  weight = get_par(conf, "egg.weight", unlist=TRUE)
  radius = get_par(conf, "egg.size", unlist=TRUE)/2
  volume = (4/3)*pi*radius^3
  
  density = weight/volume
  
  water = which(density < 1.02)
  abn = which(density < 1 | density > 1.15)
  bad = which(density < 0.9 | density > 1.5)
  
  nm_water = gsub(names(density[water]), pattern="species.egg.weight.sp", replacement="")
  nm_abn = gsub(names(density[abn]), pattern="species.egg.weight.sp", replacement="")
  nm_bad = gsub(names(density[bad]), pattern="species.egg.weight.sp", replacement="")
  
  msg1 = sprintf("# Egg density lower than seawater (~1.025 g/cm3) found for %d species:\n%s.\nPlease check.", 
                 length(water), paste(nm_water, collapse=", "))
  
  msg2 = sprintf("# Abnormal egg density (lower than 1.0 g/cm3 or greater than 1.15 g/cm3) found for %d species:\n%s.\nPlease check.", 
                 length(abn), paste(nm_abn, collapse=", "))
  
  msg3 = sprintf("# Wrong egg density (lower than 0.9 g/cm3 or greater than 1.5 g/cm3) found for %d species:\n%s.\nPlease check.", 
                 length(bad), paste(nm_bad, collapse=", "))
  
  if(length(water)>0) warning(msg1)
  if(length(abn)>0)   warning(msg2)
  if(length(bad)>0)   warning(msg3)
  
  return(invisible(NULL))
  
}

