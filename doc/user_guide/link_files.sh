#!/bin/bash
cp -r ../_static .
cp -f ../astart/down_java.qmd .
cp -f ../astart/download_osm.qmd .
cp -f ../astart/datarmor.qmd .
cp -f ../astart/running.qmd .

quarto render
