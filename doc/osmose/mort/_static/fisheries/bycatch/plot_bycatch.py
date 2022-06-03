import pandas as pd
import pylab as plt
import numpy as np

data = pd.read_csv("osmose/mort/_static/fisheries/bycatch/fishingMatrix.csv", index_col=0)

values = data.values
nfisheries, nspecies = values.shape

fishlab = data.index.values
splab = data.columns.values

values = np.ma.masked_equal(values, 0)

plt.figure()
ax = plt.gca()
cs = plt.imshow(values, origin='lower', interpolation='none', cmap=plt.cm.get_cmap('jet'))
cb = plt.colorbar(cs)
cb.set_label('target (\%)')
ax.set_xticks(np.arange(nspecies))
ax.set_xticks(np.arange(nspecies))
ax.set_xticklabels(splab, rotation=90)
ax.set_yticks(np.arange(nfisheries))
ax.set_xlabel('species')
ax.set_ylabel('fisheries indexes')
plt.savefig('osmose/mort/_static/fisheries/bycatch/plot_bycatch.png', bbox_inches='tight')
