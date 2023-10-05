(runcal)=

# Running a calibration

In this section, the methodology of {{ os }} calibration using the sequential approach described in {cite}`Oliveros2017` is provided. This calibration uses the {samp}`calibrar` and {samp}`osmose` R libraries (see {numref}`rlibs` for installation instructions). For a detailed description of the {samp}`calibrar` package, the user is referred to {cite}`Oliveros2017bis`.

The philosophy of the method is to assess unknown or poorly described parameters (for instance larval mortality or plankton accessibility) by fitting model outputs to observed datasets (biomass, etc.).

The calibration of the reference calibration, which is explained in details in the following, can be tested by typing in R:

```{literalinclude} calib/_static/democalib.R
:language: R
```

```{eval-rst}
.. ipython:: python

    import os
    import subprocess
    cwd = os.getcwd()

    fpath = "calib/_static/democalib.R"
    subprocess.call(["Rscript", fpath], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
```

```{toctree}
:caption: 'Contents:'
:maxdepth: 1

calib/dirarc.md
calib/dataprep.md
calib/calset.md
calib/varcal.md
calib/runcal.md
calib/runmod.md
calib/run.md
```
