import json
import multiprocessing

from urllib import request
from pathlib import Path

with open("cleaned.json", 'r') as file:
    apk_list = json.load(file)

print("Number of available Apks: {0}".format(len(apk_list)))

url_list = [d['file_url'] for d in apk_list]


def process_url(url):
    filename = url.split('/')[-1]
    print("Url: {0}, FileName: {1}".format(url, filename))

    file = Path('../' + filename)
    if not file.is_file():
        request.urlretrieve(url, "../" + filename)


pool = multiprocessing.Pool(processes=4)
pool.map(process_url, url_list)
