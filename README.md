<img align="left" src="app/src/main/res/drawable/appicon.png" height="80" width="80">

# TCL Watcher


Simple app for [TCL](http://tcl.fr/), Lyon's subways and bus network.

Features :
* autocomplete stations names
* bookmark routes(from → to)
* leave at / arrive at
* map view of route steps
* route warnings (deviated bus stop...)


**Download the app : [here](https://github.com/eviallet/TCLWatcher/releases)**

<img src="screenshots/homepage.png" height="350" width="200"> <img src="screenshots/route.png" height="350" width="200"> <img src="screenshots/conflict.png" height="350" width="200"> <img src="screenshots/map.png" height="350" width="200"> 


# How it works

A list of the stations and their positions is downloaded from [here](https://download.data.grandlyon.com/wfs/rdata?SERVICE=WFS&VERSION=2.0.0&outputformat=GEOJSON&maxfeatures=100000&request=GetFeature&typename=tcl_sytral.tclarret) and stored in a local db. Their position is used to show the user the final route with [osmdroid](https://github.com/osmdroid/osmdroid) and their name to autocomplete the queries.
