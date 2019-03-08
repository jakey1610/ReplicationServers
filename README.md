# Distributed Systems Coursework
## Description
Diagram of the Distributed System I've implemented in file "RS.pdf". The RepServer.java has functions to get and submit ratings, that can be used by the FEServer.java, as a proxy for Client.java. The RepServer's communicate by periodically sending their vector timestamp. If after comparison one of the RepServer's realises it is behind the current vector clock it will look through logs of other servers to get up to date. RepServers have a status. This can be modified in the Client manually (by typing desired server; 0, 1 or 2, and then when prompted typing the status in lower case) or set to randomly change statuses over time. From the Client you can also search for and update/add movies. The Client gets all this information from the RepServers via the FEServer. The FEServer checks their availability before choosing the one to use. If the FEServer sees that a RepServer it wanted to use is OFFLINE then it will move to next one. If it is seen to be OVERLOADED then it will move to next one but will come back when ACTIVE again. If it is the case that all servers are busy and n passes have been made checking all of RepServers then the user will be told to "Try again later.".

## Instructions
### Instructions (Linux):
```bash
$ java FEServer
(Ctrl+Z)
$ bg
$ java Client
```

### Instructions (Windows):
```bash
(Open CMD Window)
> java FEServer
(Open new CMD Window)
> java Client
```
For the purpose of simplicity when testing this, I have made it so that the FEServer launches all of the RepServers and the rmiregistry. In reality, there would be a check these haven't already been created/started.
