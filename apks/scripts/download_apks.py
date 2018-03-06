import json

with open("cleaned.json", 'r') as file:
    apk_list = json.load(file)

print("Number of available Apks: {0}".format(len(apk_list)))

print(apk_list)

for url in [d['file_url'] for d in apk_list]:
    filename = url.split('/')[-1]
    print("Url: {0}, FileName: {1}".format(url, filename))
