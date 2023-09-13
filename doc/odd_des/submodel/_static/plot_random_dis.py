import re
import numpy as np
import matplotlib.pyplot as plt
import matplotlib as mpl
import os

np.random.seed(1)

dirout = 'odd_des/submodel/_static'

def getNeighbourCells(j, i, r):

    im1 = np.max([i - r, 0])
    ip1 = np.min([i + r, nx - 1])
    jm1 = np.max([j - r, 0])
    jp1 = np.min([j + r, ny - 1])

    neighbours = []

    print(im1, ip1)

    for i in range(im1, ip1 + 1):
        for j in range(jm1, jp1 + 1):
            neighbours.append([j, i])

    return neighbours

nx = 15
ny = 10

nCells = 40

mask = np.full((ny, nx), 0, dtype=int)
mask[3:7, 3:5] = 1

alreadyChoosen = np.full((ny, nx), False, dtype=bool)
randomMap = []

i = int(np.round(np.random.rand(1) * (nx - 1)))
j = int(np.round(np.random.rand(1) * (ny - 1)))
while(mask[j, i]):
    i = int(np.round(np.random.rand(1) * (nx - 1)))
    j = int(np.round(np.random.rand(1) * (ny - 1)))


cpt = 0

randomMap.append([j, i, cpt])
alreadyChoosen[j, i] = True

cpt += 1

index = iFirstSorted = iLastSorted = 0

while (index < (nCells - 1)):
    for iCell in range(iFirstSorted, iLastSorted + 1):
        jjj, iii, ppp = randomMap[iCell]
        neighbors = getNeighbourCells(jjj, iii, 1)
        kkk = 0
        while ((index < (nCells - 1)) & (kkk < len(neighbors))):

            jtemp, itemp = neighbors[kkk]
            if (~mask[jtemp][itemp] & ~alreadyChoosen[jtemp][itemp]):
                index += 1
                alreadyChoosen[jtemp][itemp] = True
                randomMap.append([jtemp, itemp, cpt])

            kkk += 1

        cpt += 1

    iFirstSorted = iLastSorted + 1
    iLastSorted = index


jout = [v[0] for v in randomMap]
iout = [v[1] for v in randomMap]
colors = np.array([v[2] for v in randomMap])

ncolors = len(np.unique(colors)) - 1

cmap = plt.cm.get_cmap('Spectral')

fig = plt.figure()
ax = plt.gca()

plt.imshow(mask, interpolation='none', cmap=plt.cm.get_cmap('binary'))

for j, i, c in zip(jout, iout, colors):

    col = cmap(c/ncolors)

    rect = mpl.patches.Rectangle([i - 0.5, j - 0.5], 1, 1, hatch='//', color=col)
    ax.add_artist(rect)
    plt.plot([i], [j], color=col, linestyle='none', marker='o')
    plt.text(i, j, c, ha='center', va='center')

ax.set_xlim(0 - 0.5, nx + 0.5)
ax.set_ylim(0 - 0.5, ny + 0.5)

ax.set_aspect('equal')
for i in range(nx + 1):
    ax.axvline(i - 0.5, linewidth=0.5)

for j in range(ny + 1):
    ax.axhline(j - 0.5, linewidth=0.5)


plt.savefig(os.path.join(dirout, 'random_drift.svg'), bbox_inches='tight')
plt.savefig(os.path.join(dirout, 'random_drift.pdf'), bbox_inches='tight')