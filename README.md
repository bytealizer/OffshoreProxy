Build the docker image
```
docker build --no-cache  -t offshoreproxy:1.0 .
```

Run the docker image

```
docker run -p 8081:8081 offshoreproxy:1.0 --port 8081
```
A client for this proxy server is avilable at https://github.com/bytealizer/ShipProxy
