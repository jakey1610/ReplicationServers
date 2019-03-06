# Distributed Systems Coursework
Diagram of the Distributed System I've implemented in file "RS.pdf". The RepServer.java has functions to get and submit ratings, that can be used by the FEServer.java, as a proxy for Client.java. The RepServer's communicate by periodically sending their vector timestamp. If after comparison one of the RepServer's realises it is behind the current vector clock it will look through logs of other servers to get up to date. RepServers have a status. This can be modified in the Client manually (by typing desired status) or set to randomly change statuses over time. From the Client you can also search for and update/add movies. The Client gets all this information from the RepServers via the FEServers, which checks their availability before choosing the one to use. If the FEServer sees that a RepServer it wanted to use is OFFLINE then it will move to next one. If it is seen to be OVERLOADED then it will move to next one but will come back when ACTIVE again. For the purpose of simplicity when testing this I have made it so that the FEServer launches all of the RepServers and the rmiregistry. In reality, there would be a check these haven't already been created/started.
```bash
$ java FEServer
(Ctrl+Z)
$ bg
$ java Client
```
