@echo off
start javaw -XX:+IgnoreUnrecognizedVMOptions -Xmx2g -XX:G1PeriodicGCInterval=60000 -XX:+G1PeriodicGCInvokesConcurrent -jar .\vortexscape-launcher.jar staging https://github.com/staticvoid07/Client-Java/releases/download/staging/manifest.json 10 1 highmem members 32 staging.vortexidle.com "VortexScape (STAGING)"
exit
