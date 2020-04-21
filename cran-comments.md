# version 3.3.3

**round 5**

The cache flushing of the Java executables is forced in the vignettes. This is to
force the flushing of the corrupted JAR file that may have remained on CRAN cache directory.

**round 4** 

Jar files are now downloaded through Zip files. Direct downloading of the `jar` files caused
errors in Windows.

**round 3**

Size has been reduced by removing the heavy data files and Java libraries. These are downloaded into the user cache file, following what is done in the BIOMASS package.

** round 2 ** 

`
  Found the following (possibly) invalid URLs:
    URL: https://cran.rstudio.com/web/packages/osmose/index.html
      From: README.md
      Status: 200
      Message: OK
      CRAN URL not in canonical form

    Canonical: https://CRAN.R-project.org/package=osmose

  Size of tarball: 33864553 bytes

Not more than 5 MB for a CRAN package, please. 
`

The URL has been corrected as suggested.

Regarding the size of the package, as we said in our previous message, we were not able to reduce the size of the package, since its heavy size is mainly due to an external Java library needed to run the package (netcdfAll-4.6.6.jar) and to external data needed to run the demo scripts (NetCDF forcings).

We therefore have no possibility to reduce the size of the package. Since there are already packages on the CRAN (geomapdata for instance is 26 Mo), we therefore ask for the possibility to submit the package with its actual size.


** round 1 ** 

>  Package has FOSS license, installs .class/.jar but has no 'java' directory.
>
>  Found the following (possibly) invalid URLs:
>    URL: http://cran.r-project.org/package=osmose
>      From: README.md
>      CRAN URL not in canonical form
>    URL: http://cran.rstudio.com/web/packages/osmose/index.html
>      From: README.md
>      Status: 200
>      Message: OK
>      CRAN URL not in canonical form
>    Canonical CRAN.R-project.org URLs use https.
>
>  Size of tarball: 32817276 bytes
>
>Not more than 5 MB for a CRAN package, please.
>
>Please fix and resubmit. 

The "java" directory has been added and put to .Rinstignore.

The CRAN URLs have been corrected.

However, we were not able to reduce the size of the package, since its heavy size is mainly due to an external Java library needed to run the package (netcdfAll-4.6.6.jar) and to external data needed to run the demo scripts (NetCDF forcings).

# version 0.1.2

Adding more plot functions
Correction of a bug in the call to Java
Update in the java code (v3u3)

# version 0.1.1

** round 8 **

Removing of the 0.1.0 version tar.gz file which is responsible for a note on the automatic check

**round 7**

Update in version name

# version 0.1.0

**round 6**

Update in Java version name, as suggested by Brian Ripley

**round 5**

> Thanks, please add more small executable examples in your Rd-files (code coverage 30.35%). 

This has been done (sizeSpectrum, getMortality, getAverageMortality, getMortalityDeviation, getFishingMortality, getFishingBaseRate functions)

**round 4**

Sample CSV files have been added to run examples.

**round 3**

> Thanks, please write the title in title case:
> Object-Oriented Simulator of Marine Ecosystems

This has been done

>Please write package names and software names in single quotes (e.g. 'FishBase').
>
>Please add an URL for 'FishBase' in your description text in the form
><http:...> or <https:...>
>with angle brackets for auto-linking and no space after 'http:' and 'https:'.
>
>Please add a reference for the OSMOSE model in the 'Description' field of your DESCRIPTION file in the form
>authors (year) <doi:...>
>authors (year) <arXiv:...>
>authors (year, ISBN:...)
>with no space after 'doi:', 'arXiv:' and angle brackets for auto-linking.

The FishBase website has been added and single quotes have been added around names.
References have been added.

>Your examples are wrapped in \dontrun{}, hence nothing gets tested. Please unwrap the examples if that is feasible and if they can be executed in < 5 sec for each Rd file or create additionally small toy examples. Something like
>\examples{
>       examples for users:
>       executable in < 5 sec
>       for checks
>       \dontshow{
>              examples for checks:
>              executable in < 5 sec together with the examples above
>              not shown to users
>       }
>       donttest{
>              further examples for users (not used for checks)
>       }
>}
>would be desirable.
>
>There is an example for you function buildConfiguration(), but it looks like the function is not exported. Please check.
>
>Please ensure that your function do not write by default or in your examples in the user's home filespace. tempdir() is allowed. 

@examples statements have been replaced by @usage statements. The buildconfiguration function was not exported since not yet finished. It has thus been commented out.



> License: CeCILL
> Imports: graphics, grDevices, rlist, stats, stringr, utils
>
>
> The maintainer confirms that he or she
> has read and agrees to the CRAN policies.
>
> Submitter's comment: The title has been changed as suggested
>
> =================================================
>
> Original content of DESCRIPTION file:
>
> Package: osmose
> Type: Package
> Title: Object-oriented Simulator of Marine Ecosystems
> Version: 0.1.0
> Date: 2017-12-06
> Authors@R: c(
>    person("Yunne-Jai", "Shin", role="aut"),
>    person("Travers", "Morgane", role="aut"),
>    person("Verley", "Philippe", role="aut"),
>    person("Ricardo", "Oliveros-Ramos", role = "aut"),
>    person("Laure", "Velez", role = "aut"),
>    person("Nicolas", "Barrier", email="nicolas.barrier@ird.fr", role="cre"),
>    person("Criscely", "Lujan", role="ctb"),
>    person("Michael", "Hurtado", role = "ctb")
>    )
> Description: The multispecies and Individual-based model (IBM) OSMOSE focuses on fish species.
>    This model assumes opportunistic predation based on spatial co-occurrence and size
>    adequacy between a predator and its prey (size-based opportunistic predation). It
>    represents fish individuals grouped into schools, which are characterized by their
>    size, weight, age, taxonomy and geographical location (2D model), and which undergo
>    major processes of fish life cycle (growth, explicit predation, natural and starvation
>    mortalities, reproduction and migration) and fishing exploitation. The model needs
>    basic biological parameters that are often available for a wide range of species, and
>    which can be found in FishBase for instance, and fish spatial distribution data. This
>    package provides tools to build a model and run simulations using the OSMOSE model. See
>    <http://www.osmose-model.org/> for more details.
> License: CeCILL
> Encoding: UTF-8
> Depends: R (>= 2.15)
> Imports: graphics, grDevices, rlist, stats, stringr, utils
> URL: http://www.osmose-model.org/
> LazyData: FALSE
> BugReports: https://github.com/osmose-model/osmose/issues
> SystemRequirements: Java JDK 1.7 or higher
> RoxygenNote: 6.0.1
> NeedsCompilation: no
> Packaged: 2017-12-08 12:12:37 UTC; nbarrier
> Author: Yunne-Jai Shin [aut],
>    Travers Morgane [aut],
>    Verley Philippe [aut],
>    Ricardo Oliveros-Ramos [aut],
>    Laure Velez [aut],
>    Nicolas Barrier [cre],
>    Criscely Lujan [ctb],
>    Michael Hurtado [ctb]
> Maintainer: Nicolas Barrier <nicolas.barrier@ird.fr>
>







**round 2**

> Dear maintainer,
 
> package osmose_0.1.0.tar.gz does not pass the incoming checks automatically, please see the pre-test at:
> <https://win-builder.r-project.org/incoming_pretest/171207_074550_osmose_010/00check.log>
> Status: 3 WARNINGs, 3 NOTEs

> Best regards,
> CRAN teams' auto-check service

Upss.

**round 1**

> Thanks, we see:

> Non-FOSS package license (file LICENSE)

The license has be updated to CeCILL

> Suggests or Enhances not in mainstream repositories:
  kali

Removed from Suggests.

> Found the following (possibly) invalid URLs:
>  URL: http://cran.r-project.org/package=osmose
>    From: README.md
>    CRAN URL not in canonical form
>  URL: http://cran.rstudio.com/web/packages/osmose/index.html (moved to 
> https://cran.rstudio.com/web/packages/osmose/index.html)
>    From: README.md
>    Status: 404
>    Message: Not Found
>    CRAN URL not in canonical form
>  Canonical CRAN.R-project.org URLs use https.

README.md added to .Rbuildignore

> The Title field starts with the package name.

> The Description field should not start with the package name,
>  'This package' or similar.

DESCRIPTION file has been edited.

> The Date field is over a month old.

Updated.

> * checking package dependencies ... NOTE
> Package suggested but not available for checking: 'kali'

Kali imports has been removed.

> Please fix and resubmit.
> Best,
> Uwe Ligges

Thanks for the comments.
