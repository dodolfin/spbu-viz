#!/bin/bash

distribution/bin/pf-2021-viz -o figures/1000_random_numbers -d test_csvs/1000_random_numbers.csv -s 1280 720 -p -t histogram --title "1000 random numbers (from Excel)" -m -b 7
distribution/bin/pf-2021-viz -o figures/cars -d test_csvs/cars.csv -s 1280 720 -p -t bar --title "Cars sales number" -m --horizontal --stacked -rc
distribution/bin/pf-2021-viz -o figures/hyperbola -d test_csvs/hyperbola.csv -p -t scatter --title "Hyperbola" -m
distribution/bin/pf-2021-viz -o figures/multiple_series_and_columns -d test_csvs/multiple_series_and_columns.csv -p -t bar --title "Data without column labels" -m -r
distribution/bin/pf-2021-viz -o figures/pie_chart_without_labels -d test_csvs/pie_chart_without_labels.csv -p -t pie --title "Pie chart" -m
