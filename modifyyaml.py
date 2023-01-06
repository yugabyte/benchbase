#!/usr/bin/python

import json
import sys
import yaml
import re


def main():
    data = json.loads(sys.argv[1])

    with open(sys.argv[2]) as f:

        yaml_text = f.read()

        print(yaml_text)

        pattern_username = r"^username: \{\{username\}\}$"
        match = re.search(pattern_username, yaml_text, flags=re.MULTILINE)
        if match:
            yaml_text = yaml_text.replace(match.group(), "username: " + data['username'])
        pattern_password = r"^password: \{\{password\}\}$"
        match = re.search(pattern_password, yaml_text, flags=re.MULTILINE)
        if match:
            yaml_text = yaml_text.replace(match.group(), "password: " + data['password'])
        pattern_endpoint = r"\{\{endpoint\}\}"
        match = re.search(pattern_endpoint, yaml_text, flags=re.MULTILINE)
        if match:
            yaml_text = yaml_text.replace(match.group(), data['endpoint'])

        print(yaml_text)

        with open(sys.argv[2], 'w') as f1:
            f1.write(yaml_text)

        try:
            with open(sys.argv[2]) as f1:
                doc1 = yaml.load(f1, Loader=yaml.FullLoader)
        except Exception as e:
            print(e)
            return

        doc = yaml.load(f, Loader=yaml.FullLoader)
        for key, value in data.items():
            if key == "warmup":
                doc["works"]["work"]["warmup"] = value
            if key == "time":
                doc["works"]["work"]["time_secs"] = value
            elif key == "url":
                doc[key] = doc[key].replace("localhost", value)
                if "sslmode" in data:
                    doc[key] = doc[key].replace("disable", "require") if data["sslmode"] else doc[key].replace(
                        "require", "disable")
            else:
                doc[key] = value
            print(key, value)

        with open(sys.argv[2], 'w') as fnew:
            yaml.safe_dump(doc, fnew, encoding='utf-8', allow_unicode=True)
    return


main();
