curl -H 'Content-Type: application/json' -d '
{
"sspId":"0", "symId":"sym910",
"internalIdResource":"InternalIDSensor001",
  "semanticDescription":{
    "@c":".Sensor",
    "observesProperty":null,
    "id":"",
    "name":"SensorDevice001",
    "description":null,
    "interworkingServiceURL":null,
    "locatedAt":null,
    "services":[
      {
        "@c":".Service",
        "id":null,
        "name":null,
        "description":null,
        "interworkingServiceURL":null,
        "resultType":null,
        "parameters":[
          {
            "name":"param1",
            "mandatory":false,
            "restrictions":null,
            "datatype":null
          }
        ]
      }
    ]
  }
}
' -X POST -D - http://localhost:8080/innkeeper/join
