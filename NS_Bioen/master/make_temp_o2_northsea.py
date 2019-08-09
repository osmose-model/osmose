import xarray as xr
import numpy as np
import pylab as plt

data = xr.open_dataset("/home/nbarrier/Bureau/Morgane_Alaia/LTL.nc")
data = data.isel(ltl=slice(0, 3))
data = data.rename({'ltl_biomass':'T'})
data = data.rename({'ltl':'zindex'})


mask = (np.isnan(data['T'].values) == False)
data['O2'] = data['T']

data['T'].values = 8.4 * np.ones(data['T'].values.shape) * mask   # C.
data['O2'].values = 350 * np.ones(data['T'].values.shape) * mask  # C.   # mmol/m3

data.to_netcdf("corrected_physical_forcings_northsea.nc", format="NETCDF4_CLASSIC")
