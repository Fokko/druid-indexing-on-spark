# druid-indexing-on-spark

Using Apache Spark to build indices for Apache Druid (Incubating)

Right now Druid uses a Mapreduce job to build indexes for Druid. MapReduce is old and slow, and we would like to replace it with Apache Spark.

Metamarkets has an old version of the [Spark batch indexer](https://github.com/metamx/druid-spark-batch/issues), but is not maintained anymore. Also this code is hard to patch since some of the classes used by this indexer have changed their signature from public to protected/private, we can't use them anymore.

It would make me very happy if we could build something to generate Druid segments using Spark:

- First separate it from the Druid distribution to make development a bit easier
- First only implement the roaring bitmaps, since they offer [better compression and performance](https://dl.acm.org/citation.cfm?id=2938515) (in most cases)

An overview of the content of a segment.
```
root@hadoopedge01:/tmp/druid#hdfs dfs -get /druid/data/datasources/Bids/20171211.../0_index.zip /tmp/druid
root@hadoopedge01:/tmp/druid# unzip 0_index.zip
root@hadoopedge01:/tmp/druid# ls -lah
total 712M
drwxr-xr-x  3 root root 4.0K Jan 22 11:00 .
drwxrwxrwt 31 root root 4.0K Jan 22 11:00 ..
-rw-r--r--  1 root root 452M Jan 22 12:17 00000.smoosh
-rw-r--r--  1 root root 261M Jan 22 11:00 0_index.zip
drwxr-xr-x  3 root root 4.0K Jan 22 10:58 20171211T000000.000Z_20171212T000000.000Z
-rw-r--r--  1 root root   29 Jan 22 12:17 factory.json
-rw-r--r--  1 root root 2.3K Jan 22 12:17 meta.smoosh
-rw-r--r--  1 root root    4 Jan 22 12:17 version.bin
```
An [overview of the files](https://github.com/druid-io/druid/blob/master/docs/content/design/segments.md):
- `0000.smoosh` The actual data including the bitmap indices
- `0_index.zip` The zipped segments stored on the deep-store, pulled by the historical node and unzipped locally
- `20171211T000000.000Z_20171212T000000.000Z` no idea
- `factory.json` no idea
- `meta.smoosh` A file with metadata (filenames and offsets) about the contents of the other `smoosh` files
- `version.bin` 4 bytes representing the current segment version as an integer. E.g., for v9 segments, the version is 0x0, 0x0, 0x0, 0x9



## Plan of attack

- Use Apache Spark dataframes to get the relevant dimensions and metrics
- Build the [roaring bitmaps](https://roaringbitmap.org/) on the dimension fields, by looking into the [existing MapReduce code](https://github.com/druid-io/druid/tree/master/indexing-hadoop/src/main/java/io/druid/indexer), and possibly reuse some of the code
- Generate the metadata files
- Zip everything together into one zip that represents one segment
- Update the Druid metastore to let Druid know there is a new segment

### Optional vanity

- Configure roll-ups, use Spark to group by on the dimension fields, and round the time-dimension to 1, 5, 60, etc minutes
- Make the segment size configurable. Using the MapReduce jobs you can tune the segment size to get optimal performance, using Spark we can do something similar: https://github.com/apache/spark/blob/master/core/src/main/scala/org/apache/spark/util/SizeEstimator.scala or count to get [partitions from ~5m rows](https://github.com/druid-io/druid/blob/master/docs/content/design/segments.md#segments)
- Load the segment configuration from the Druid indexing format (using Jackson to parse the stuff, these classes can be reused from Druid)
- Look into storing the segments on the deepstore using gzip instead of zip

## TODO

- Generate a segment using MapReduce of the Wikiticker data (so we can compare it with the Spark output)
- Make local instance of Druid to test the segments
- Build some queries to compare the end results

## Local Druid instance

Using Docker it is easy to spin up a local Druid instance:
```sh
git clone https://github.com/Fokko/docker-druid
cd docker-druid
docker build -t druid .
echo "Grab some coffee, this will take a while..."
docker run --rm -i -p 8082:8082 -p 8081:8081 -p 5432:5432 druid
```
The Druid console should come up:
Druid Console: http://localhost:8081/
Druid Indexing: http://localhost:8081/console.html

You can connect to the Druid `psql`:
```
Fokkos-MacBook-Pro:druid-indexing-on-spark fokkodriesprong$ psql -h 127.0.0.1 -p 5432 -U druid
Password for user druid: diurd
psql (10.3, server 9.5.12)
SSL connection (protocol: TLSv1.2, cipher: ECDHE-RSA-AES256-GCM-SHA384, bits: 256, compression: off)
Type "help" for help.

druid=# \dt
               List of relations
 Schema |         Name          | Type  | Owner
--------+-----------------------+-------+-------
 public | druid_audit           | table | druid
 public | druid_config          | table | druid
 public | druid_datasource      | table | druid
 public | druid_pendingsegments | table | druid
 public | druid_rules           | table | druid
 public | druid_segments        | table | druid
 public | druid_supervisors     | table | druid
 public | druid_tasklocks       | table | druid
 public | druid_tasklogs        | table | druid
 public | druid_tasks           | table | druid
(10 rows)
```

Inside of the container, the data is stored under `/tmp/druid/`:
```
root@6ad4fee19af0:/var/lib/druid# find /tmp/druid
/tmp/druid
/tmp/druid/localStorage
/tmp/druid/localStorage/wikiticker
/tmp/druid/localStorage/wikiticker/2015-09-12T00:00:00.000Z_2015-09-13T00:00:00.000Z
/tmp/druid/localStorage/wikiticker/2015-09-12T00:00:00.000Z_2015-09-13T00:00:00.000Z/2018-05-06T21:06:57.094Z
/tmp/druid/localStorage/wikiticker/2015-09-12T00:00:00.000Z_2015-09-13T00:00:00.000Z/2018-05-06T21:06:57.094Z/0
/tmp/druid/localStorage/wikiticker/2015-09-12T00:00:00.000Z_2015-09-13T00:00:00.000Z/2018-05-06T21:06:57.094Z/0/index.zip
/tmp/druid/localStorage/wikiticker/2015-09-12T00:00:00.000Z_2015-09-13T00:00:00.000Z/2018-05-06T21:06:57.094Z/0/descriptor.json
/tmp/druid/localStorage/wikiticker/2015-09-12T00:00:00.000Z_2015-09-13T00:00:00.000Z/2018-05-06T21:06:57.094Z/0/.index.zip.crc
/tmp/druid/localStorage/wikiticker/2015-09-12T00:00:00.000Z_2015-09-13T00:00:00.000Z/2018-05-06T21:06:57.094Z/0/.descriptor.json.crc
```

## Futher reference

Real time analytics: Divolte + Kafka + Druid + Superset: https://blog.godatadriven.com/divolte-kafka-druid-superset
