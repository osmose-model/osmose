import numpy as np
import matplotlib.pyplot as plt
import matplotlib.patches

dirout = 'osmose/mort/_static/'

def compute_period(start):
    smin = start
    smax = start + season_dur

    period = np.zeros(nstep * nyears)
    xpos = []
    flab = []
    findex = 0
    if(start != 0):
        xpos.append(start / 2)
        flab.append('F%d' %findex)
        findex += 1

    cpt = 1
    for i in range(0, nseasons * nyears):
        index = slice(smin, min(smax, nstep * nyears - 1))
        xtemp = np.arange(smin,  min(smax, nstep * nyears - 1)).mean()
        xpos.append(xtemp)
        flab.append('F%d' %findex)
        period[index] = (i + 1) % 2
        smin += season_dur
        smax += season_dur
        findex += 1

    period = period.astype(int)
    return period, xpos, flab

def plot_period(period, xpos, flab):
    
    cpt = 0

    ax = plt.gca()

    ax.axis('equal')
    width, height = 1, 10
    
    for i in range(nstep * nyears):
        xy = i - 0.5, 0
        col = color[period[i]]
        rect = matplotlib.patches.Rectangle(xy, width, height, angle=0.0, color=col)
        plt.xlim(0-0.5, nstep*nyears - 0.5)
        ax.add_patch(rect)
        cpt += 1

    for x, l in zip(xpos, flab):
        plt.text(x, height/2, l, ha='center', va='center', fontsize=13)

    plt.axis('off')


# number of years
nyears = 2

# number of seasons
nstep = 24

# number of steps
steps = np.arange(0, nyears*nstep)

# number of fishing seasons
nseasons = 2

# season duration
season_dur = nstep // nseasons

color = ['steelblue', 'firebrick']

period0, xpos0, lab0 = compute_period(0)
period6, xpos6, lab6 = compute_period(6)

fig = plt.figure()
plt.subplots_adjust(hspace=0)

ax = plt.subplot(211)
plot_period(period0, xpos0, lab0)
plt.title('Start = 0')

ax = plt.subplot(212)
plot_period(period6, xpos6, lab6)
plt.title('Start = 0.25')

plt.savefig('%s/fishing-period.svg' %dirout, bbox_inches='tight')
plt.savefig('%s/fishing-period.pdf' %dirout, bbox_inches='tight')
