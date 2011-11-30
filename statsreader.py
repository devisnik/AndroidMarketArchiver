#! /usr/bin/python

import sys

key = "DOWNLOADS"
try:
    key = sys.argv[1]
except IndexError:
    pass
for line in sys.stdin:
    appData = {}
    for pair in line.strip('\n').split(',')[2:]:
        data = pair.split('=')
        appData[data[0]]=data[1]
    try:
        print appData[key]
    except KeyError:
        pass 


