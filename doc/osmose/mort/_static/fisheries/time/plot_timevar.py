import numpy as np
import pylab as plt

nyear = 2  # number of years
ndt = 12   # time step per years
ntot = ndt * nyear   # total number of time steps
peryear = 4  # number of fishing periods per year
freq = ndt / peryear

######################################### Test regime shift
shift = np.array([3, 5, 15])
rates = np.array([1, 5, 3, 7])

output_shift = np.zeros(nyear*ndt) - 999

ishift = 0
irates = 0
sh = shift[ishift]
for i in range(0, nyear*ndt):
    if(i>=sh):
        ishift = ishift + 1
        irates = irates + 1
        if(ishift<len(shift)):
            sh = shift[ishift]
        else:
            sh = nyear * ndt

    output_shift[i] = rates[irates]

######################################### Test linear
output_lin = np.zeros(nyear*ndt) - 999
slope = 2
rate = 0.4

freq = int(ndt / peryear)

x = 0
time = 0
for i in range(0, nyear*ndt):

    x = int(i / freq)
    time = x * freq/float(ndt)
    output_lin[i] = rate * (1 + slope*time)


######################################### Test by_year

output_byyear = np.zeros(nyear*ndt) - 999
value = [1, 1.7, 1.8, 2.4]

cpt = 0
for i in range(0, nyear):
    for j in range(0, ndt):
        output_byyear[cpt] = value[int(j/freq)]
        cpt += 1

############################ test bydt

output_bydt = np.zeros(ndt * nyear) - 999
rates = [1, 8, 4, 9, 3]

for i in range(0, ndt * nyear):
    k = i % len(rates)
    output_bydt[i] = rates[k]

################################################ figure
plt.rcParams['lines.marker'] = 'None'
plt.rcParams['lines.linewidth'] = 2
lw = 1
ls = '--'

def plot_axvline():

    cpt = 0
    for p in range(0, 2*peryear):
        cpt += freq
        plt.axvline(cpt, color='firebrick', marker="None", linewidth=lw)

plt.figure(figsize = (8,8))
plt.subplots_adjust(top=0.97, right=0.97, bottom=0.07, left=0.1)
ax0 = plt.subplot(3, 2, 1)
ax0.plot(np.zeros(ntot) + 3) 
ax0.set_title('Constant')

ax1 = plt.subplot(3, 2, 2, sharex=ax0)
ax1.plot(output_byyear) 
ax1.set_title('By Year')
plt.axvline(ndt, color='gold', linewidth=2, zorder=100)
plot_axvline()

ax2 = plt.subplot(3, 2, 3, sharex=ax0)
ax2.plot(output_bydt) 
cpt = 0
N = int(ntot / len(rates))
for s in range(0, N + 1):
    ax2.axvline(cpt, color='skyblue', marker='None', linewidth=lw)
    cpt += len(rates)
ax2.set_title('By Dt')

ax3 = plt.subplot(3, 2, 4, sharex=ax0)
ax3.plot(output_lin) 
ax3.set_title('Linear')
plot_axvline()

ax4 = plt.subplot(3, 2, 5, sharex=ax0)
ax4.plot(output_shift) 
for s in shift:
    ax4.axvline(s, color='plum', marker='None', linewidth=lw)
ax4.set_title('Shift')

plt.xlim(0, ntot-1)

for ax in [ax0, ax1, ax2]:
    plt.setp(ax.get_xticklabels(), visible=False)

for ax in [ax3, ax4]:
    ax.set_xlabel('Time step')

for ax in [ax0, ax2, ax4]:
    ax.set_ylabel('Fishing rate')

plt.savefig('osmose/mort/_static/fisheries/time/plot_timevar', bbox_inches='tight')
