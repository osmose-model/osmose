import xarray as xr
import nbtools.map as nbmap
import pylab as plt
from mpl_toolkits.basemap import Basemap
import numpy as np
import pandas as pd
import random

data = xr.open_dataset("eec_lon_lat.nc")

imin = 5
imax = 8
jmin = 2
jmax = 15

data = data.isel(nx=slice(imin, imax))
data = data.isel(ny=slice(jmin, jmax))

lon = data['longitude']
lat = data['latitude']
tp = data['ltl_biomass'][0, 0, :, :]

tp1 = np.ma.masked_where(np.isnan(tp), tp)
tp1[tp1.mask == 0] = 1

rt = 6371 * 1e3
dlat = np.mean(lat[1:, :] - lat[:-1, :])
dlon = np.mean(lon[:, 1:] - lon[:, :-1])

surf = rt * np.deg2rad(dlat) * rt * np.deg2rad(dlon) * np.cos(np.deg2rad(lat))
surf = np.ma.masked_where(tp1.mask, surf)
surf

def wmean(tp):

    out = np.sum(tp * surf) / np.sum(surf)
    #return out.values
    return out

wmean1 = wmean(tp1)
out1 = tp1 / wmean1
tp1
out1

tp2 = tp1.copy()
tp2[3] = 5
tp2[4] = 5
tp2 = np.ma.masked_where(tp1.mask, tp2)
wmean2 = wmean(tp2)
out2 = tp2 / wmean2

tp3 = tp1.copy()
tp3[3] = 0
tp3[4] = 0
tp3[5] = 2
tp3[6] = 2
tp3 = np.ma.masked_where(tp1.mask, tp3)
wmean3 = wmean(tp3)
out3 = tp3 / wmean3

print tp1.shape
print tp1

iok = np.nonzero(tp1.mask==0)
fac = tp1[iok]
print tp1[iok]


#random.seed(1)
npercell = np.empty(len(fac))
ncell = len(npercell)
for p in xrange(0, ncell):
    npercell[p] = random.randint(5, 30)

npercell[:] = 20

ntot = np.sum(npercell)

f = 0.01

ndead = np.sum(npercell * (1 - np.exp(-f * fac)))
test = ntot * (1 - np.exp(-f))

fac2 = fac.copy()
fac2[:5] = 0
fac2[:5] = 5
fac2 /= np.mean(fac2)

ndead = np.sum(npercell * (1 - np.exp(-f * fac2)))
test = ntot * (1 - np.exp(-f))

print test
print ndead












"""
output = (np.ma.getmaskarray(tp)==False).astype(int)
output[output==0] = 0
output = output[::-1, :]   # to make write the upper latitudes first
output = pd.DataFrame(output)
#output = pd.DataFrame(data['latitude'].values)
output.to_csv("template_grid.csv", sep=',', header=False, index=False)
"""

'''
plt.figure()
m = nbmap.make_bmap(lon, lat, resolution='h')
#m = Basemap(llcrnrlon=lon.min()-10, llcrnrlat=lat.min() - 10,
#            urcrnrlon=lon.max()+10, urcrnrlat=lat.max() + 10,)
m.drawcoastlines()
#m.pcolormesh(lon, lat, tp)
m.imshow(tp, interpolation='none')
plt.savefig('toto.png')
'''
