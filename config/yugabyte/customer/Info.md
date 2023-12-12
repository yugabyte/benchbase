# Information about the input yamls

1. Multithreaded workloads:
   - benchbase does not allow running multi-threaded(terminal) runs exactly once. Instead, it allows you to run it time bound. For example, if time_secs: 60 means it will repeat the transactions for 60 seconds before aborting the threads.
   - The results uses the transactions happening during the runtime to calculate the latency and throughput
2. Single threaded workloads:
   - Benchbase allows running single threaded runs both once only(using serial: true in yaml) and time bound(time_secs:60 and serial needs to be removed). In the single threaded yamls, we have used serial: true, which means execution will happen only once.

## How to run
1. install jdk17+, maven
2. clone the repository for the mentioned branch and build the code
    ```shell
    git clone https://github.com/yugabyte/benchbase.git -b demo1
    cd benchbase
    ./mvnw clean package -P yugabyte -DskipTests
    cd target
    tar -xzf benchbase-yugabyte.tgz
    cd benchbase-yugabyte
    ```

3. run the benchmark by providing using:
    ```shell
    java -jar benchbase.jar -b featurebench -c config/yugabyte/customer/regular/single_threaded_insert.yaml --create=true --load=true --execute=true
    ```

4. results are generated in ```results/``` directory.


## Notes:
1. you may need to change the endpoint, username, password and sslmode depending on your cluster setup
2. you can choose to run only specific workloads from the list of executeRules by providing a comma separated list to the run command.
    ```shell
    --workloads=insert_orders_query1
    ```
3. colocated yamls uses following to create a colocated db named ```yb_colocated``` and use it by replacing it in ```url``` at runtime.
    ```shell
    createdb: drop database if exists yb_colocated; create database yb_colocated with colocation=true
    ```
4. You can modify the pre-loaded number of rows by changing it in the ```loadRules -> rows``` .Accordingly, you also need to change the utility function's ```max``` limit (wherever applicable)
## For additional details on Featurebench framework refer to the [document](https://github.com/yugabyte/benchbase/tree/main/src/main/java/com/oltpbenchmark/benchmarks/featurebench)