# Code structure

We have now, [rq.clj](../src/rq.clj) responsible for stablishing the connection to the redis server. If you do not have a redis running on your machine, you can either download redis and run it locally or use our [dockerfile or docker-compose.yml](../docker/docker-compose.yml) to run it. 
Also, our [rq folder](../src/rq/) contains all the code we need. You can see how we handle redis on clj or you can test them by yourself.


## Running some tests

To run the tests we've set, you can use one of the following commands:

### Using Clojure CLI 

This command will run all the test cases on our deps.edn file.

```sh 

clj -M:tests

```

### Using Leiningen
if you want to use lein, you will have to set the environmet before. But this will be the command to run the tests.

```sh

lein test com.moclojer.rq.queue-test

```

This commands will run all the test cases defined in the our tests namespaces and provide feedback on their status.
By running these tests, you can verify the correctness and reliability of the queue operations, such as seeing some important info output on debug mode.
