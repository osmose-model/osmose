import pandas as pd
import pylab as plt
import xarray as xr
import numpy as np

data = xr.open_dataset("corrected_ns_ltlbiomassTons_test.nc", decode_times=False)
time = data['time'].values
print(time)
print(np.nonzero(time == 220))

data = data['Diatoms'].values


mask = pd.read_table("maps/grid-mask.csv", header=None, sep=";")
mask = mask.values
mask = mask[::-1, :]

ilat, ilon = np.nonzero(mask >= 0)

print(data.shape)
print(mask.shape)

plt.figure()
ax = plt.subplot(121)
plt.imshow(data[0])
ax.set_facecolor('DarkGray')
plt.scatter(ilon, ilat, marker='.', c='k', s=20)

ax = plt.subplot(122)
plt.imshow(data[-1])
plt.scatter(ilon, ilat, marker='.', c='k', s=20)
ax.set_facecolor('DarkGray')
plt.savefig('mask.png', bbox_inches='tight')

