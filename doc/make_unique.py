import numpy as np

fin = open('toto.log')
lines = np.array(fin.readlines())
fin.close()

lines = np.unique(lines)
print len(lines)

fin = open('deprecated.log')
dep = np.array(fin.readlines())
fin.close()

fin = open('out.log', 'w')

for l in lines:
    if(l not in dep):
        fin.write(l)

fin.close()
