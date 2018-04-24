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
An overview of the files:
- `0000.smoosh` The actual data including the bitmap indices
- `0_index.zip` The zipped segments stored on the deep-store, pulled by the historical node and unzipped locally
- `20171211T000000.000Z_20171212T000000.000Z` no idea
- `factory.json` no idea
- `meta.smoosh` no idea
- `version.bin` no idea

## Plan of attack

- Use Apache Spark dataframes to get the relevant dimensions and metrics
- Build the [roaring bitmaps](https://roaringbitmap.org/) on the dimension fields, by looking into the existing MapReduce code, and possibly reuse some of the code
- Generate the metadata files
- Zip everything together into one zip that represents one segment
- Update the Druid metastore to let Druid know there is a new segment

### Optional vanity

- Configure roll-ups, use Spark to group by on the dimension fields, and round the time-dimension to 1, 5, 60, etc minutes
- Make the segment size configurable. Using the MapReduce jobs you can tune the segment size to get optimal performance, using Spark we can do something similar: https://github.com/apache/spark/blob/master/core/src/main/scala/org/apache/spark/util/SizeEstimator.scala
- Load the segment configuration from the Druid indexing format (using Jackson to parse the stuff, these classes can be reused from Druid)
