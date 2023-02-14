import numpy as np
import pylab as plt

plt.rcParams['lines.linewidth'] = 2

x = np.linspace(0, 200, 1000)
x50 = 100

y1 = (x>=x50).astype(int)

b = 0.1
a = 1
y2 = 1 / (1 + a * np.exp(-b * (x - x50)))

b = 0.0005
a = 1
y3 = np.exp(-b * (x-x50)**2)

plt.figure()
plt.plot(x, y1, label='step')
plt.plot(x, y2, label='sigmo')
plt.plot(x, y3, label='gauss')
plt.xlim(x.min(), x.max())
plt.ylim(0, 1.05)
plt.legend(loc=0)
plt.xlabel('Size or Age')
plt.ylabel('Selectivity')
plt.savefig('odd_des/submodel/mort/_static/fisheries/select/plot_select.png', bbox_inches='tight')
