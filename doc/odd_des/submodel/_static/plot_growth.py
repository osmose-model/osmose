import numpy as np
import matplotlib.pyplot as plt

dirout = 'odd_des/submodel/_static'

lifespan = 10
eggsize = 9.5
linf = 87.42
t0 = -1.09
thres = 0.5
K = 0.118
x = np.linspace(0, lifespan, 10000)

Ke = 0.3
lstart = 15
agom = 3

def vonber(x):

    fun = lambda x : linf * (1 - np.exp(-K * (x - t0)))
    lthres = min(eggsize, fun(thres))
    lthres = fun(thres)

    if(x == 0):
        output = eggsize
    elif ((x > 0) & (x < thres)):
        output = eggsize + (lthres - eggsize) * x / thres
    else:
        output = fun(x)

    return output

vb = np.array([vonber(a) for a in x])
plt.figure()
plt.plot(x, vb)
plt.title('von Bertalanffy')
plt.xlabel('Age (years)')
plt.ylabel('Size (cm)')
plt.axvline(thres, color='red', linestyle='--')
plt.savefig('%s/vb.svg' %dirout, bbox_inches='tight')
plt.savefig('%s/vb.pdf' %dirout, bbox_inches='tight')

def grom(x):

    fun = lambda x : linf * np.exp(-np.exp(-K * (x - t0)))
    funexp = lambda x : lstart * np.exp(Ke * x)
    lthres = min(eggsize, fun(thres))
    lthres = fun(thres)

    if(x == 0):
        output = eggsize
    elif ((x > 0) & (x < thres)):
        output = funexp(x)
    elif (x > thres) & (x < agom):
        lexp = funexp(thres)
        lgom = fun(agom)
        output =  lexp + (lgom - lexp) * (x-thres) / (agom - thres)
    else:
        output = fun(x)

    return output

gom = np.array([grom(a) for a in x])
plt.figure()
plt.plot(x, vb)
plt.title('Gompertz')
plt.xlabel('Age (years)')
plt.ylabel('Size (cm)')
plt.axvline(thres, color='red', linestyle='--')
plt.axvline(agom, color='blue', linestyle='--')
plt.savefig('%s/gom.svg' %dirout, bbox_inches='tight')
plt.savefig('%s/gom.pdf' %dirout, bbox_inches='tight')
