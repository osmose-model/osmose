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

mask = xr.open_dataset('eec_grid-mask.nc')['mask'].values
cs = plt.pcolormesh(mask)
plt.colorbar(cs)


# +
def compute_mpa(percentageMPA, rate):
    
    strin = '++++++++++++++++++++++++++++++++++++++++++++++++++++++++ '
    
    # The user specify a map containing the percentage of the cell occpied by a MPA (`percentageMPA`):
    # and a fishing mortality rate.
    import numpy as np
    
    rate = np.ma.masked_where(mask == 0, rate)
    
#     print(strin, 'Percentage occupied by the MPA:')
#     print(percentageMPA)
    
#     fig = plt.figure()
#     ax = plt.subplot(I, J, cpt)
#     cs = plt.imshow(percentageMPA)
#     plt.title('MPA percentage')
#     plt.colorbar(cs)
    
#     print(strin, 'Input mortality rate ')
#     print(rate)
    print('Mean rate: ', rate.mean())

    # Using this percentage, we specify a factor, by which the mortality rate will be multiplied. 
    # If no MPA (`percentageMPA == 0`), the factor is 1. If full MPA (`percentageMPA == 1`, factor is 0).
    mpafactor = np.ones(percentageMPA.shape)

    # the number of cells considered is the number of cells 
    # where the fishing rate is not 0
    isfished_cell = (rate > 0) & (mask > 0)
    ncells = np.sum(isfished_cell)
    
    # the correction factor is forced to 0 
    # where no fishing is possible
    mpafactor[~isfished_cell] = 0
    
    # where we have MPA, we change the factor value
    mpafactor[percentageMPA > 0] = 1 - percentageMPA[percentageMPA > 0]
    mpafactor

#     print(strin, 'Original MPA correction factor:')
#     print(mpafactor)

    # Compute effort
    effort = rate / np.sum(rate)
    
#     print(strin, 'Effort :')
#     print(effort)
    print('Sum effort:', np.sum(effort))
    
    # We compute a correction factor, which mimics the increase of the fishing effort 
    # where there are no MPA and where there is fishing effort
    #correction = ncells / (np.sum(mpafactor))
    #correction
      
    correction = 1 / np.sum(mpafactor * effort)
    print(mpafactor.shape, effort.shape, correction.shape)
        
    print('Correction due to fishing effort compensation:')
    print(correction)
    
    # We now multiply the `mpaFactor` by this correction value:
    mpafactor_bis = mpafactor.copy()
    mpafactor_bis[mpafactor_bis > 0] *=  correction
#     print(strin, 'Corrected MPA correction factor:')
#     print(mpafactor_bis)
    print('Mean factor: ', mpafactor_bis[isfished_cell].mean())

    #Finally, the rate that will be applied is the initial rate multiplied by the corrected MPA factor.
    rate_bis = rate * mpafactor_bis
    rate_bis
#     print(strin, 'New fishing mortality rate:')
#     print(rate_bis)
    print('Mean rate: ', rate_bis.mean())

    # We make sure that the mean fishing mortality rate is the same than in the original rate: 

    np.mean(rate_bis)
    return rate_bis


# -

# ## Full MPA case, constant F

mpamaps = mask.copy()
mpamaps[:, 10:] = 0
mpamaps = np.ma.masked_where(mask == 0, mpamaps)
cs = plt.pcolormesh(mpamaps)
plt.colorbar(cs)
output = pd.DataFrame(mpamaps[::-1])
output.to_csv('mpa/full_mpa.csv')

rate = np.full(mask.shape, 0.07)
rate[mask == 0] = 0
cs = plt.pcolormesh(rate)
plt.colorbar(cs)

new_rate = compute_mpa(mpamaps, rate)
cs = plt.pcolormesh(new_rate)
plt.colorbar(cs)

# ## Partial MPA, constant F

mpamaps = mask.copy()
mpamaps[:, :5] = 0.3
mpamaps[:, 5:10] = 0.7
mpamaps = np.ma.masked_where(mask == 0, mpamaps)
cs = plt.pcolormesh(mpamaps)
plt.colorbar(cs)
output = pd.DataFrame(mpamaps[::-1])
output.to_csv('mpa/partial_mpa.csv')

rate = np.full(mask.shape, 0.07)
rate[mask == 0] = 0
cs = plt.pcolormesh(rate)
plt.colorbar(cs)

new_rate = compute_mpa(mpamaps, rate)
cs = plt.pcolormesh(new_rate)
plt.colorbar(cs)
