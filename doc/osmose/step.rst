Osmose time-stepping
-------------------------

Processes called in one Osmose time-step are show in :numref:`osm_step`.

.. ipython:: python
    :suppress:

    import os
    import subprocess
    cwd = os.getcwd()

    fpath = "osmose/_static/compile.sh"
    subprocess.call(["bash", fpath], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

.. _osm_step:
.. figure:: _static/plot_step_chart.*
    :align: center
    :width: 30%

    Processes called during one Osmose time-step.

A detailed description of the processes and their associated parameters is given below.

.. toctree::

    process/influx.rst
    process/school-init.rst
    process/movement.rst
    process/growth.rst
    process/reprod.rst
