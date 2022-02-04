# ---
# jupyter:
#   jupytext:
#     formats: ipynb,py:light
#     text_representation:
#       extension: .py
#       format_name: light
#       format_version: '1.5'
#       jupytext_version: 1.13.0
#   kernelspec:
#     display_name: Python 3 (ipykernel)
#     language: python
#     name: python3
# ---

# +
import pandas as pd
import numpy as np
import sys

data = pd.read_csv('predation-accessibility.csv', index_col = 0, sep=';')
data.head()
# -

data.tail()


def sort(names):
    prednames = []
    predclasses = []
    for temp in names:
        test = temp.split('<')
        if len(test) == 1:
            predclasses.append(sys.float_info.max)
        else:
            predclasses.append(float(test[1]))
        prednames.append(test[0].strip().lower())
    
    preddf = pd.DataFrame(index=names, data={'names': prednames, 'classes':predclasses})
    return preddf


prey = sort(data.index)
prey.tail()

pred = sort(data.columns)
pred.tail()

sortpred = pred.sort_values(by=['names', 'classes'])
sortpred.head()

sortprey = prey.sort_values(by=['names', 'classes'])
sortprey.head()

output = data.loc[sortprey.index, sortpred.index]
output

output.to_csv('sorted-predation-accessibility.csv')
