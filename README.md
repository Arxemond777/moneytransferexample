# Description  
The RESTful API example for money transfers between accounts.  

## Run
### You can change your server port in src/main/conf/my-application-conf.json
```bash
./build_and_run.sh
```
### or
```bash
mvn clean package; java -jar target/mondeytransfer-1.0-SNAPSHOT-fat.jar -conf src/main/conf/my-application-conf.json
```

## Curls
### Add an user. NOTE you can`t recreate users
```bash
curl -X POST  -H "Content-Type: application/json" -d '{"id": 22, "balance": "1"}' -i localhost:8083/addUser
```
### Get all
```bash
curl -i localhost:8083/getAll
```
### Get by id
```bash
curl -i localhost:8083/getById?id=1
```
### Send a transaction. NOTE you CAN`T send negative or zero sentSum!!! It will be incorrect data
```bash
curl -X POST -H "Content-Type: application/json" -d '{"fromId": "1", "sentSum": "100.1", "toId": "2"}' -i localhost:8083/sendTransaction
```
if this response was success, then it return UUID (to save it on a client). By this UUUID a push-service notification can
send a notification to the user about his transaction 
### Send transaction statuses for some a push-service for users notification about transactions statuses
```bash
curl -i localhost:8083/getStatuses
```