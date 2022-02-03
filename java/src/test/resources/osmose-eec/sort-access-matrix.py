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

data.tail()

rownames = data.index.values.copy()
sortedrows = sorted(rownames, key=str.lower)

colnames = data.columns.values.copy()
sortedcols = sorted(colnames, key=str.lower)
sortedcols

output = data.loc[sortedrows, sortedcols]
output

output.to_csv('sorted-predation-accessibility.csv')
