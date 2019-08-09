import xarray as xr
import numpy as np
import pylab as plt

data = xr.open_dataset("LTL_essai.nc")
diatoms = data['Diatoms'].values
time = data['time'].values
latitude = data['latitude'].values
longitude = data['longitude'].values

ntime, nlat, nlon = diatoms.shape

diatoms = diatoms[:, np.newaxis, :, :]

T = np.ones((ntime, 3, nlat, nlon)) * 8.4
O2 = np.ones((ntime, 3, nlat, nlon)) * 350.

T = T * (np.isnan(diatoms) == False)
O2 = O2 * (np.isnan(diatoms) == False)

T[T==0] = np.nan
O2[O2==0] = np.nan

#data = data.rename({'ltl_biomass':'T'})
#data = data.rename({'ltl':'zindex'})

print(T.shape)
print(latitude.shape)
print(longitude.shape)

ds = xr.Dataset({'T': (['time', 'zindex', 'y', 'x'], T),
                  'O2': (['time', 'zindex', 'y', 'x'], O2)},
                 coords={'lat': (['y'], latitude),'lon': (['x'], longitude)}
                  )

ds.to_netcdf("physical_forcings_northsea.nc", format="NETCDF4_CLASSIC")
