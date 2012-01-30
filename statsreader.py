#! /usr/bin/python

import sys

keys = ["DOWNLOADS"]
try:
    keys = sys.argv[1:]
except IndexError:
    pass
for line in sys.stdin:
    appData = {}
    for pair in line.strip('\n').split('|'):
        data = pair.split('=')
        appData[data[0]]=data[1]
    try:
        for key in keys:
            print '{0:20s}'.format(appData[key]),
        print
    except KeyError:
        pass 


