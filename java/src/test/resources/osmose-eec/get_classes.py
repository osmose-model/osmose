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
import numpy as np

data = pd.read_csv('predation-accessibility.csv', index_col = 0, sep=';')
data.head()
# -

rownames = data.index.values.copy()
rownames

colnames = data.columns.values.copy()
colnames


def classify(names):
    outnames = []
    outclass = []
    for temp in names:

        test = temp.split('<')

        if(len(test) == 1):
            outclass.append(np.nan)
        else:
            outclass.append(float(test[1]))
        outnames.append(test[0].strip())
    return outnames, outclass


preynames, preyclass = classify(rownames)

prednames, predclass = classify(colnames)

for i in range(len(prednames)):
    print(i, prednames[i])

for i in range(len(preynames)):
    print(i, preynames[i])

import re
pattern = ' +'
regex = re.compile(pattern)
toshow = str(data.values)
toshow = re.sub(pattern, ', ', toshow)
toshow = toshow.replace('[', '{')
toshow = toshow.replace(']', '}')
toshow = toshow.replace(', }', '}')
print(toshow)


