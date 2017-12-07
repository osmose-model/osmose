# version 0.1.0

**round 3**

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
