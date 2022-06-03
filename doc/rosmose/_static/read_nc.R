rm(list=ls())

library("ncdf4")

filename = file.path("rosmose", "_static",
                     "gogosm_mortalityRateDistribByAge-OctopusVulgaris_Simu0.nc")

print("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ fid")
fid = nc_open(filename)
print(fid)

print("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ vars")
var = fid$var
print(names(var))
mort = ncvar_get(fid, "mortality")

print("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ dims")
dims = fid$dim
print(names(dims))

print("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ mortality")
mcause = ncvar_get(fid, "mortality_cause")
attrs = ncatt_get(fid, "mortality_cause")
print(attrs)

print("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ age")
age = ncvar_get(fid, "Age")
print(age)
