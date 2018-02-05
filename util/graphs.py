import jaydebeapi
import os
import matplotlib.pyplot as plt
import numpy as np

if __name__ == '__main__':

    folder_path = os.path.dirname(os.path.abspath(__file__))
    output_path = os.path.abspath(os.path.join(os.path.dirname( __file__ ), os.pardir, 'output'))
    db_path = output_path + "/jabref"

    print("Connecting to database at ", db_path)

    conn = jaydebeapi.connect("org.hsqldb.jdbcDriver",
                              "jdbc:hsqldb:file:" + db_path,
                              ["SA", ""],
                              folder_path + "/hsqldb-2.4.0/lib/hsqldb.jar",)
    curs = conn.cursor()

    #curs.execute('SELECT score, COUNT(score) FROM strangeness GROUP BY score ORDER BY score')
    #strangeness_data = curs.fetchall()

    curs.execute('SELECT ')

    curs.close()
    conn.close()

    print(strangeness_data)

    x, y = zip(*strangeness_data)

    x = list(x)
    y = list(map(lambda x: x.longValue(), y))

    print(x)
    print(y)
