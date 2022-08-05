Running the test configuration
--------------------------------

To run the test configuration, launch :samp:`R` and type the following:

.. literalinclude:: _static/run_testconf.R
    :language: R

.. ipython:: python
    :suppress:

    import os
    import subprocess
    cwd = os.getcwd()

    fpath = "astart/_static/run_testconf.R"
    subprocess.call(["Rscript", fpath], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

You should obtain the following figure:

.. figure:: _static/osmose_ref_conf.png
    :width: 600 px
    :align: center

    Outputs of the reference configuration
