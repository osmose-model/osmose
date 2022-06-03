.. OSMOSE documentation master file, created by
   sphinx-quickstart on Tue Aug  8 09:44:06 2017.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

.. image:: _static/logo-osmose.*
   :width: 250 px
   :align: center

|

=========================================================================
OSMOSE (Object-oriented Simulator of Marine ecOSystEms)
=========================================================================

.. About this User guide
.. #########################

.. The present document contains all the necessary information and guidelines to help understand the principles and assumptions of the OSMOSE model, apply it to a specific case study and run simulations for addressing specific issues. The user guideline will describe how to install, configure and run an executable, so no pre-requisite in programming is necessary. After a brief presentation of the history of development of the OSMOSE model and its purpose, the model structure, processes and concepts are described in detail. We then get more hands-on and explain how to install the software, how to parameterize the model to a specific case study, how to calibrate the model and run simulations.

The present document contains all the necessary information and guidelines to help understand the principles and assumptions of the OSMOSE model,
apply it to a specific case study and run simulations for addressing specific issues. The OSMOSE model aims at exploring fish community dynamics and the
ecosystem effects of fishing and climate change. It is an Individual-based model (IBM) which focuses on fish species and their trophic interactions (:cite:`Shin2001,Shin2004`).

.. The model description follows the ODD ("Overview", "Design concept" and "Details") protocol for describing individual- or agent-based models (Grimm et al. 2006, Grimm et al. 2010).

The Osmose model assumes opportunistic predation based on spatial co-occurrence and size adequacy between a predator and its prey (size-based opportunistic predation). It represents fish individuals grouped in schools, which are characterized by their size, weight, age, taxonomy and geographical location (2D model), and which undergo different processes of fish life cycle (growth, explicit predation, natural and starvation mortalities, reproduction and migration) and a fishing mortality distinct for each species and structured by age/size, space and season. The model needs basic biological parameters for growth and reproduction processes, that are often available for a wide range of species, and which can be found in FishBase for instance. It also needs to be forced by spatial distribution maps for each species, by age/size/stage and by season depending on data availability. In output, a variety of size-based and species-based ecological indicators can be produced and compared to in situ data (surveys and catch data) at different levels of aggregation: at the species level (e.g. mean size, mean size-at-age, maximum size, mean trophic level, within-species distribution of TL), and at the community level (e.g. slope and intercept of size spectrum, Shannon diversity index, mean TL of catch). The model can be fitted to observed biomass and catch data, using a dedicated evolutionary algorithm. Recent developments have focused on the coupling of OSMOSE to various hydrodynamic and biogeochemical models, allowing to build end-to-end models of marine ecosystems that explicit combined effects of climate and fishing on fish dynamics.

.. The present document contains is a companion website of the `Osmose model <http://www.osmose-model.org/>`_ webpage. 

.. It contains a full documentation of the |os| model and of the R packages dedicated to the model calibration and pre/post-processing.

.. toctree::
   :maxdepth: 1
   :caption: Contents:
   :numbered:

   astart
   osmose
   osmose-econ
   rosmose
   calib
   biblio
   misc
   genindex

.. :ref:`genindex`

..   setup_newconfig
..   params
..   rosmose
..   calib
..   biblio

   
.. .. todo::
..     Who, date, IRD, projects? Major changes between versions? Osmose users and developers community, how to join? Website? Contact persons?
.. .. caution::
..     Caution
.. .. seealso::
..     Lalala
.. .. tip::
..     toto
.. .. attention::
..     "attention"
.. .. caution::
..     "caution"
.. .. danger:: 
..     "danger"
.. .. error::  
..     "error"
.. .. hint:: 
..     "hint"
.. .. important:: 
..     "important"
.. .. note:: 
..     "note"
.. .. tip:: 
..     "tip"
.. .. warning::
..     "warning"
.. .. admonition:: test
..     "admonition"
