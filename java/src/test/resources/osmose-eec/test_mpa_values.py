# ---
# jupyter:
#   jupytext:
#     formats: ipynb,py:light
#     text_representation:
#       extension: .py
#       format_name: light
#       format_version: '1.5'
#       jupytext_version: 1.13.7
#   kernelspec:
#     display_name: Python 3 (ipykernel)
#     language: python
#     name: python3
# ---

# +
import pandas as pd
import xarray as xr
import matplotlib.pyplot as plt
import numpy as np

mask = xr.open_dataset('eec_grid-mask.nc')['mask'].values.astype(np.float32)
cs = plt.pcolormesh(mask)
plt.colorbar(cs)
print(mask.dtype)


# -

def compute_mpa(percentageMPA, rate):
    
    strin = '++++++++++++++++++++++++++++++++++++++++++++++++++++++++ '
    
    # The user specify a map containing the percentage of the cell occpied by a MPA (`percentageMPA`):
    # and a fishing mortality rate.
    import numpy as np
    
    rate = np.ma.masked_where(mask == 0, rate)
    
    print('Mean rate: ', rate.mean())

    # Using this percentage, we specify a factor, by which the mortality rate will be multiplied. 
    # If no MPA (`percentageMPA == 0`), the factor is 1. If full MPA (`percentageMPA == 1`, factor is 0).
    mpafactor = np.ones(percentageMPA.shape, dtype=np.float32)

    # the number of cells considered is the number of cells 
    # where the fishing rate is not 0
    isfished_cell = (rate > 0) & (mask > 0)
    ncells = np.sum(isfished_cell)
    
    # where we have MPA, we change the factor value
    mpafactor[percentageMPA > 0] = 1 - percentageMPA[percentageMPA > 0]
    mpafactor
    
    # the correction factor is forced to 0 
    # where no fishing is possible
    mpafactor[~isfished_cell] = 0
    
    # Compute effort
    effort = rate / np.sum(rate)

    print('Sum rate:', np.sum(rate))
    print('Sum effort:', np.sum(effort))
    print(np.unique(effort))
        
    print('Correction denom ', np.sum(mpafactor * effort))
    correction = 1 / np.sum(mpafactor * effort)
    print(mpafactor.shape, effort.shape, correction.shape)
        
    print('Correction due to fishing effort compensation:')
    print(correction)
    
    # We now multiply the `mpaFactor` by this correction value:
    mpafactor_bis = mpafactor.copy()
    mpafactor_bis[mpafactor_bis > 0] *=  correction

    print('Mean factor: ', mpafactor_bis[isfished_cell].mean())

    #Finally, the rate that will be applied is the initial rate multiplied by the corrected MPA factor.
    rate_bis = rate * mpafactor_bis
    rate_bis

    print('Mean rate: ', rate_bis.mean())

    # We make sure that the mean fishing mortality rate is the same than in the original rate: 
    test = mpafactor_bis.copy()
    test[mask == 0] = -999
    test = np.ravel(test).astype(str)
    strout = 'f,'.join(test)
    strout = '{' + strout + 'f};'
    print(strout)
    
    np.mean(rate_bis)
    return mpafactor_bis


# ## Full MPA case, constant F

mpamaps = mask.copy().astype(np.float32)
mpamaps[:, 10:] = 0
mpamaps = np.ma.masked_where(mask == 0, mpamaps)
cs = plt.pcolormesh(mpamaps)
plt.colorbar(cs)
mpamaps[np.ma.getmaskarray(mpamaps)] = -999
output = pd.DataFrame(mpamaps[::-1])
output.to_csv('mpa/full_mpa.csv', header=False, index=False)

rate = np.full(mask.shape, 1, dtype=np.float32)
rate[mask == 0] = 0
cs = plt.pcolormesh(rate)
plt.colorbar(cs)

new_rate = compute_mpa(mpamaps, rate)
cs = plt.pcolormesh(new_rate)
plt.colorbar(cs)

# ## Partial MPA, constant F

# +
mpamaps = mask.copy().astype(np.float32)
mpamaps[:, :5] = 0.3
mpamaps[:, 5:10] = 0.7
mpamaps = np.ma.masked_where(mask == 0, mpamaps)
cs = plt.pcolormesh(mpamaps)
plt.colorbar(cs)
mpamaps[np.ma.getmaskarray(mpamaps)] = -999
output = pd.DataFrame(mpamaps[::-1])
output.to_csv('mpa/partial_mpa.csv', header=False, index=False)

rate = np.full(mask.shape, 0.07).astype(np.float32)
rate[mask == 0] = 0
cs = plt.pcolormesh(rate)
plt.colorbar(cs)

new_rate = compute_mpa(mpamaps, rate)
cs = plt.pcolormesh(new_rate)
plt.colorbar(cs)
