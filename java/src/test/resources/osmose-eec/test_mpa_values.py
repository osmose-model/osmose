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
import matplotlib.pyplot as plt
import numpy as np
plt.rcParams['text.usetex'] = False

mask = xr.open_dataset('eec_grid-mask.nc')['mask'].values.astype(np.float32)
cs = plt.pcolormesh(mask)
plt.colorbar(cs)
print(mask.dtype, mask.shape)


# -

def compute_mpa(percentageMPA, rate, display=False):
    
    strin = '++++++++++++++++++++++++++++++++++++++++++++++++++++++++ '
    
    # The user specify a map containing the percentage of the cell occpied by a MPA (`percentageMPA`):
    # and a fishing mortality rate.
    import numpy as np
    
    rate = np.ma.masked_where(mask == 0, rate)
    print('Average over non zero cells ', np.mean(rate[rate != 0]))
    rate /= np.mean(rate[rate != 0])
    
    print('Mean rate: ', rate.mean())

    # Using this percentage, we specify a factor, by which the mortality rate will be multiplied. 
    # If no MPA (`percentageMPA == 0`), the factor is 1. If full MPA (`percentageMPA == 1`, factor is 0).
    mpafactor = np.ones(percentageMPA.shape, dtype=np.float32)

    # the number of cells considered is the number of cells 
    # where the fishing rate is not 0
    isfished_cell = (rate > 0) & (mask > 0)
    
    # where we have MPA, we change the factor value
    mpafactor[percentageMPA > 0] = 1 - percentageMPA[percentageMPA > 0]
    mpafactor
    
    # the correction factor is forced to 0 
    # where no fishing is possible
    mpafactor[~isfished_cell] = 0
    mpafactor = np.ma.masked_where(mask == 0, mpafactor)
    
    plt.figure()
    cs = plt.pcolormesh(mpafactor)
    plt.colorbar(cs)
    plt.title('mpafactor')
    
    print(np.sum(rate))
    
    # Compute effort
    effort = rate / np.sum(rate)
    
    plt.figure()
    cs = plt.pcolormesh(effort)
    plt.colorbar(cs)
    plt.title('effort')

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
    
    plt.figure()
    cs = plt.pcolormesh(mpafactor_bis)
    plt.colorbar(cs)
    plt.title('mpafactor_bis')

    print('Mean factor: ', mpafactor_bis[isfished_cell].mean())

    #Finally, the rate that will be applied is the initial rate multiplied by the corrected MPA factor.
    rate_bis = rate * mpafactor_bis
    rate_bis

    print('Mean rate: ', rate_bis.mean())

    # We make sure that the mean fishing mortality rate is the same than in the original rate:
    if(display):
        test = rate_bis.copy()
        test[mask == 0] = -999
        test = np.ravel(test).astype(str)
        strout = 'f,'.join(test)
        strout = '{' + strout + 'f};'
        print(strout)
    
    np.mean(rate_bis)
    return rate_bis


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

# ## Partial MPA, constant F

# +
mpamaps = np.zeros(mask.shape).astype(np.float32)
mpamaps[:, :5] = 0.7
mpamaps[:, 5:10] = 0.3
mpamaps = np.ma.masked_where(mask == 0, mpamaps)

plt.figure()
cs = plt.pcolormesh(mpamaps)
plt.colorbar(cs)
mpamaps[np.ma.getmaskarray(mpamaps)] = -999
output = pd.DataFrame(mpamaps[::-1])
output.to_csv('mpa/partial_mpa.csv', header=False, index=False)

rate = np.full(mask.shape, 1).astype(np.float32)
rate[mask == 0] = 0

plt.figure()
cs = plt.pcolormesh(rate)
plt.colorbar(cs)

new_rate = compute_mpa(mpamaps, rate)
# -

# ## Partial map with variable fishing effort

# +
mpamaps = np.zeros(mask.shape).astype(np.float32)
mpamaps[:, :5] = 0.7
mpamaps[:, 5:10] = 0.3
mpamaps = np.ma.masked_where(mask == 0, mpamaps)

rate = np.ones(mask.shape).astype(np.float32)
rate[:, 20:30] = 0.5
rate[:, 30:] = 0.7
rate = np.ma.masked_where(mask == 0, rate)

# plt.figure()
# cs = plt.pcolormesh(rate)
# plt.colorbar(cs)
# plt.title('rate')
rate[np.ma.getmaskarray(rate)] = -999
output = pd.DataFrame(rate[::-1])
output.to_csv('mpa/rate_map1.csv', header=False, index=False)

plt.figure()
cs = plt.pcolormesh(mpamaps)
plt.colorbar(cs)
plt.title('mpa')
mpamaps[np.ma.getmaskarray(mpamaps)] = -999
output = pd.DataFrame(mpamaps[::-1])
output.to_csv('mpa/partial_mpa.csv', header=False, index=False)

new_rate = compute_mpa(mpamaps, rate)
print(np.unique(new_rate))
# -

# ## Partial MPAs, variable F with 0s.

# +
mpamaps = np.zeros(mask.shape).astype(np.float32)
mpamaps[:, :5] = 0.7
mpamaps[:, 5:10] = 0.3
mpamaps = np.ma.masked_where(mask == 0, mpamaps)

rate = np.ones(mask.shape).astype(np.float32)
rate[:, 20:30] = 0.
rate[:, 30:] = 0.7
rate = np.ma.masked_where(mask == 0, rate)

plt.figure()
cs = plt.pcolormesh(rate)
plt.colorbar(cs)
plt.title('rate')
rate[np.ma.getmaskarray(rate)] = -999
output = pd.DataFrame(rate[::-1])
output.to_csv('mpa/rate_map2.csv', header=False, index=False)

# plt.figure()
# cs = plt.pcolormesh(mpamaps)
# plt.colorbar(cs)
# plt.title('mpa')
mpamaps[np.ma.getmaskarray(mpamaps)] = -999
output = pd.DataFrame(mpamaps[::-1])
output.to_csv('mpa/partial_mpa.csv', header=False, index=False)

new_rate = compute_mpa(mpamaps, rate)
print(np.unique(new_rate))
# -
# ## New Ricardo's test

# +
mpamaps = np.zeros(mask.shape).astype(np.float32)
mpamaps[:, :5] = 1
mpamaps[:, 5:10] = 0.8
mpamaps[:, 10:15] = 0.2
mpamaps = np.ma.masked_where(mask == 0, mpamaps)

rate = np.ones(mask.shape).astype(np.float32)
rate[:, :10] = 1.5
rate[:, 10:20] = 0.1
rate[:, 20:30] = 0.2
rate[:, 30:] = 0.01
rate = np.ma.masked_where(mask == 0, rate)

plt.figure()
cs = plt.pcolormesh(rate)
plt.colorbar(cs)
plt.title('rate')
rate[np.ma.getmaskarray(rate)] = -999
output = pd.DataFrame(rate[::-1])
output.to_csv('mpa/rate_map3.csv', header=False, index=False)

plt.figure()
cs = plt.pcolormesh(mpamaps)
plt.colorbar(cs)
plt.title('mpa')
mpamaps[np.ma.getmaskarray(mpamaps)] = -999
output = pd.DataFrame(mpamaps[::-1])
output.to_csv('mpa/mpa_map3.csv', header=False, index=False)

new_rate = compute_mpa(mpamaps, rate, display=True)
print(np.unique(new_rate))
# -


