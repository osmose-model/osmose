# ---
# jupyter:
#   jupytext:
#     formats: ipynb,py:light
#     text_representation:
#       extension: .py
#       format_name: light
#       format_version: '1.5'
#       jupytext_version: 1.10.3
#   kernelspec:
#     display_name: Python 3 (ipykernel)
#     language: python
#     name: python3
# ---

# +
import pandas as pd
import xarray as xr
import numpy as np
import matplotlib.pyplot as plt

species = 'horseMackerel'
nlat = 22
nlon = 45

with open('eec_param-movement.csv', 'r') as fin:
    lines = fin.readlines()

params = {}
for l in lines[:]:
    l = l.strip()
    if(len(l) == 0):
        continue
    test = l.split(';')
    params[test[0]] = np.array(test[1:])
# -

keys = params.keys()
key_species = [k for k in keys if k.startswith('movement.species')]
key_species = [k for k in key_species if params[k] == species]
map_index = [k.replace('movement.species.', '') for k in key_species]
map_index

initialAge = np.array([params['movement.initialAge.%s' %m][0] for m in map_index], dtype=int)
lastAge = np.array([params['movement.lastAge.%s' %m][0] for m in map_index], dtype=int)
initialAge, lastAge

cpt = 0
for i in np.unique(initialAge):
    print('+++++++++++++++++++++ Processing initialAge', i)
    output = np.zeros((24, nlat, nlon))
    for m in map_index:
        if params['movement.initialAge.%s' %m][0] == i:
            file = params['movement.file.%s' %m][0]
            if(file == 'null'):
                continue
            print(m)
            print('Reading ', file)
            data = pd.read_csv(file, sep=';', header=None)
            data = data.values[::-1]
            data = np.ma.masked_where(data < 0, data)
            steps = params['movement.steps.%s' %m]#.astype(int)
            steps = steps[steps != '']
            print(steps)
            steps = steps.astype(int)
            
            output[steps, :, :] = data
    fileOut = 'maps/movements_%s_initialAge_%d_lastAge_%d.nc' %(species, np.unique(initialAge)[cpt], np.unique(lastAge)[cpt])
    
    dsout = xr.Dataset()
    dsout['movements'] = (['time', 'y', 'x'], output)
    print('Writting ', fileOut)
    dsout.to_netcdf(fileOut)
    
    cpt += 1


