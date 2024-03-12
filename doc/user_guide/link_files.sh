#!/bin/bash
mkdir -p _static
cp -r ../_static/* _static
cp -f ../astart/down_java.qmd .
cp -f ../astart/download_osm.qmd .
cp -f ../astart/datarmor.qmd .
cp -f ../astart/running.qmd .
cp -f ../odd_des/input.qmd .

quarto render
