# Ellipsoid Surface Projection #

This is a [Fiji](http://www.fiji.sc/Downloads) plugin for fitting an ellipsoid surface to bright spots in a 3D image.
A stack of ellipsoid slices can then be extracted by projection to a sphere or cylinder.
If the 3D image is part of a time series, the projection can be repeated for each timepoint, yielding a 4D stack.
This plugin is based on [BigDataViewer](http://imagej.net/BigDataViewer).

## Installation instructions ##

To install this plugin to your Fiji installation, activate the Update site 

    http://sites.imagej.net/Egg_Chamber_Dynamics
   
in your Fiji installation. You can get more information on update sites [here](https://imagej.net/Following_an_update_site).

## Developer instructions ##

If you you want to build this plugin from source, clone this repository and use
maven to build and install it:

    mvn install

The entry-point for the plugin is the [OvariesProjectionPlugin](src/main/java/de/mpicbg/ovaries/OvariesProjectionPlugin.java) class.

