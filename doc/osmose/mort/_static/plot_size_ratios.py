import numpy as np
import pylab as plt

dirout = 'osmose/mort/_static/'

plt.rcParams['lines.linewidth'] = 1.
plt.rcParams['text.usetex'] = True

sizemax = 3
sizemin = 50

length = np.linspace(0, 10, 1000)

plt.figure()
valmax = length / sizemax
valmin = length / sizemin
plt.plot(length, valmin, label='Min. prey length', color='SteelBlue')
plt.plot(length, valmax, label='Max. prey length', color='FireBrick')
plt.fill_between(length, valmin, valmax, where=(valmin <= valmin), interpolate=True, color='lightgray', label='Predation range')
plt.legend()
plt.xlabel('Predator size (cm)')
plt.ylabel('Prey size (cm)')
plt.xlim(length.min(), length.max())
plt.title(r'Predation range: $\displaystyle 3 \le \frac{L_{pred}}{L_{prey}} \le 50$')
plt.savefig('%s/size_ratio.svg' %dirout, bbox_inches='tight')
plt.savefig('%s/size_ratio.pdf' %dirout, bbox_inches='tight')
