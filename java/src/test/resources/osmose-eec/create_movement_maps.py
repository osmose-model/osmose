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
map_index = [k.replace('movement.species.', '') for k in key_species]
map_index

for m in map_index:
    file = params['movement.file.%s' %m][0]
    if(file == 'null'):
        continue
     
    data = pd.read_csv(file, sep=';', header=None)
    data = data.values[::-1]
    data = np.ma.masked_where(data < 0, data)
    data = np.tile(data, (24, 1, 1))

    dsout = xr.Dataset()
    dsout['movements'] = (['time', 'y', 'x'], data)
    
    output_file = file.replace('.csv', '.nc')
    print('Writting ', output_file)
    dsout.to_netcdf(output_file)


