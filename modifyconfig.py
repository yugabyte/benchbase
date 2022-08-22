from logging import root
import xml.etree.ElementTree as ET
import sys
import json


tree = ET.parse('config/postgres/sample_seats_config.xml')
root = tree.getroot()

data=json.loads(sys.argv[1])
for key,value in data.items():
    element=root.find(key)
    if element!=None :
     element.text=value
    print(key,value)


tree.write('config/postgres/sample_seats_config.xml')


# python modifyconfig.py '{"url":"jdbc:postgresql://localhost:5433/yugabyte","username":"yugabyte","password":""}'