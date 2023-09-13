.. _osm_inst:

Installing Osmose
-------------------------

The Osmose model is provided as a combination of a Java numerical core (referred to as Osmose-Java), associated with an Osmose R package (referred to as Osmose-R). The code is stored in two GitHub directories, a public one (`https://github.com/osmose-model/osmose <https://github.com/osmose-model/osmose>`_) and a private one (for developers only, `https://github.com/osmose-model/osmose-private <https://github.com/osmose-model/osmose-private>`_). There are several ways to install the Osmose model.

CRAN
+++++++++++++++++++++++++++++++

The Osmose version on the :samp:`master` branch of the public repository is consistent with the most recent version of the package that is submitted to the CRAN. To install this version, open a R session and type :samp:`install.packages("osmose")`

Manual install
+++++++++++++++++++++++++++++++++++++

In order to use the development version of the Osmose model, it must be installed manually

Install from source files
########################################

The first way is to clone or download the source code and to install the code manually. To clone the directory, type in a Terminal:

To clone the Osmose repository:

.. code-block:: bash

    # using HTTPS:
    git clone https://github.com/osmose-model/osmose-private.git

    # using SSH
    git clone git@github.com:osmose-model/osmose-private.git

When a new version of the code is released, it can be updated as follows:

.. code-block:: bash

    git pull

.. note:: 

    When using the SSH, it is necessary to generate a RSA key that will connect the computer and the remote repository 
    (see `Github Help <https://help.github.com/en/github/authenticating-to-github/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent>`_ for details)

When the code has been downloaded, it must be installed as follows: 

.. code-block:: bash

    R CMD INSTALL osmose

.. warning::

    The code must be reinstalled after each upgrade

Using devtools or RStudio
################################

The Osmose package can also be by using the `devtools` R package or RStudio.

devtools
##########################################

Open a R session and type the following lines:

.. code-block:: R

    library("devtools")
    install_github("osmose-model/osmose")

RStudio
###################################

To install Osmose using RStudio, click on the :guilabel:`File --> New Project` menu and
open the :guilabel:`Version Control --> Git` menu. 
Set the package URL (`https://github.com/osmose-model/osmose-private.git <https://github.com/osmose-model/osmose>`_). When the project is opened, click on the
:guilabel:`Build & Reload` button to install the package.

