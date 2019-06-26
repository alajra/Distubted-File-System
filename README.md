# Distubted-File-System
This is a very simple distributed file system (DFS)
in that a DFS client retrieves a file from a central DFS server and caches it in its /tmp
directory for better performance. Our implementation uses Java RMI. Through this
project, we will understand that (1) the design of both client and server is based on a
state-transition diagram; (2) the server needs to maintain a directory of clients sharing the
same file so as to implement delayed write and server-initiated invalidation; and (3) both
server and client need to call each other’s RMI functions.
