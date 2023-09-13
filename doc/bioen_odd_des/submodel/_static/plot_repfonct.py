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
import matplotlib.pyplot as plt
import numpy as np
import os

plt.rcParams['text.usetex'] = True
plt.rcParams['font.size'] = 15

filename = 'repfonct_bioen'
dirout = '.'
dirout = os.path.join('bioen_odd_des', 'submodel', '_static')
fileout = os.path.join(dirout, filename)
fileout
# -

imax = 60
a = 0.6
b = 20

x = np.linspace(0, 100, 1000)
x.shape

y = a * x + b
ymax = a * imax + b
y[x >= imax] = ymax

plt.figure()
ax = plt.gca()
plt.title('Functional response')
plt.plot(x, y, color='k', linewidth=3)
plt.ylabel('Ingestion $f_p(P(w))$ $(g)$')
plt.xlabel('Available prey $P(w)$ $(g)$')
plt.axvline(imax, color='red')
ax.set_xticks([imax])
plt.setp(ax.get_yticklabels(), visible=False)
ax.set_xticklabels(['$I_{max}$'], color='red')
plt.ylim(0, 1.25 * ymax)
plt.annotate('$f_p = P(w)$', [0.45 * imax, 0.75 * imax], rotation=30, va='center', ha='center')
plt.annotate(r'$f_p = I_{max}\ w^{\alpha}$', [0.5 * (imax + x.max()), 1.1 * ymax], va='center', ha='center')

plt.savefig(fileout + '.svg', bbox_inches='tight')
plt.savefig(fileout + '.pdf', bbox_inches='tight')


