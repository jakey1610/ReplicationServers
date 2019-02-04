# ReplicationServers
This is a Java Program which makes use of Java RMI to create a distributed system via replication servers being made to process requests by a front-end server. This front-end server is directly connected to the client and processes their requests.
Run the following commands to start the distributed server:
```bash
$ rmiregistry 37029
(Ctrl+Z)
$ bg
$ javac FEServer.java RepServer.java Client.java Status.java
$ java FEServer
(Ctrl+Z)
$ bg
$ java Client
```
