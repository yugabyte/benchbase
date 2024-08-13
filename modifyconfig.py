from logging import root
import xml.etree.ElementTree as ET
import sys
import json
import re
data = json.loads(sys.argv[1])
file_path = 'config/{}/sample_{}_config.xml'.format(sys.argv[3], sys.argv[2])

# Check if loaderThreads is in the data
if 'loaderThreads' in data:

    loader_threads_value = str(data['loaderThreads'])
    # Read the XML file as a string
    with open(file_path, 'r') as file:
        xml_content = file.read()
    # Regex pattern to find <scalefactor> element and insert <loaderThreads> after it
    pattern = r'(<scalefactor>.*?</scalefactor>)'
    replacement = r'\1\n    <loaderThreads>{}</loaderThreads>'.format(loader_threads_value)

    # Insert the <loaderThreads> element after <scalefactor>
    modified_xml_content = re.sub(pattern, replacement, xml_content, flags=re.DOTALL)

    # Write the modified content back to the file
    with open(file_path, 'w') as file:
        file.write(modified_xml_content)

tree = ET.parse('config/{}/sample_{}_config.xml'.format(sys.argv[3], sys.argv[2]))
root = tree.getroot()



for key, value in data.items():
    element = root.find(key)
    value = str(value)
    if key == "warmup":
        element = root.find("works/work/warmup")
        if element == None:
            element = root.find("works/work")
            newarmp = ET.SubElement(element, "warmup")
            newarmp.text = value
        else:
            element.text = value
    elif key == "time":
        element = root.find("works/work/time")
        if element != None:
            element.text = value
    elif element != None:
        if key == "url":
            element.text = element.text.replace("localhost", value)
            if "sslmode" in data:
                element.text = element.text.replace("disable", "require") if data["sslmode"] else element.text.replace(
                    "require", "disable")
        else:
            element.text = value

tree.write('config/{}/sample_{}_config.xml'.format(sys.argv[3], sys.argv[2]))
# python modifyconfig.py '{"url":"jdbc:postgresql://localhost:5433/yugabyte","username":"yugabyte","password":""}' seats