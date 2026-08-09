@echo off
start javaw -XX:+IgnoreUnrecognizedVMOptions -Xmx2g -XX:G1PeriodicGCInterval=60000 -XX:+G1PeriodicGCInvokesConcurrent -jar .\vortexscape-launcher.jar live https://github.com/staticvoid07/Client-Java/releases/download/live/manifest.json 10 0 highmem members 32 play.vortexidle.com
exit
