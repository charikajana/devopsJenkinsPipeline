import groovy.json.JsonOutput
def filePath="C:\\Chari_GCP\\GCPperformance\\lgs-performance\\Jmeter_scripts\\nghp-shopping\\Release_NGHP_Shopping_Squatter_V4.jmx"
def Acceptance_data=activeSamplerAcceptanceCriteria(filePath)

def activeSamplerAcceptanceCriteria(Object filePath){

    com.sabre.devops.lodging.Utils utils = new com.sabre.devops.lodging.Utils()
    Map<String, String> map = utils.getAllActiveSamplersFromAJmxFile(filePath);
    String SamplerName = map.get("samplerName");
    String[] Samplers = SamplerName.split(",")
    for (int i = 0; i < Samplers.length; i++) {
        def respTime=Samplers[i]+"_respTime"
        def tps=Samplers[i]+"_tps"
        def ErrorRate=Samplers[i]+"_ErrorRate"
        Acceptance_data=[
                samplerNameOne:[
                        "type": "avg",
                        "unit": "ms",
                        "green":[[
                                1,
                                10
                        ]],
                        "yellow": [[
                                10,
                                20
                        ]],
                        "red": [[
                                20,
                                null
                        ]],
                        "macro":[
                                "name": "jmeter_avg_response_time",
                                "parameters":[
                                        "transaction": Samplers[i],
                                        "ld_label": "jmeter-1"
                                ]
                        ]

                ],
                tps:[
                        "type": "avg",
                        "unit": "ms",
                        "green":[[
                                         1,
                                         10
                                 ]],
                        "yellow": [[
                                           10,
                                           20
                                   ]],
                        "red": [[
                                        20,
                                        null
                                ]],
                        "macro":[
                                "name": "jmeter_avg_response_time",
                                "parameters":[
                                        "transaction": Samplers[i],
                                        "ld_label": "jmeter-1"
                                ]
                        ]

                ],
                ErrorRate:[
                        "type": "avg",
                        "unit": "ms",
                        "green":[[
                                         1,
                                         10
                                 ]],
                        "yellow": [[
                                           10,
                                           20
                                   ]],
                        "red": [[
                                        20,
                                        null
                                ]],
                        "macro":[
                                "name": "jmeter_avg_response_time",
                                "parameters":[
                                        "transaction": Samplers[i],
                                        "ld_label": "jmeter-1"
                                ]
                        ]

                ]
        ]
    }
    return Acceptance_data
}

def data = [
        "project"            : [
                "name"                   : "nghp-shopping",
                "version"                : "plaas-validation",
                "plaas_trigger"          : "None",
                "requester"              : "None",
                "app_env"                : "None",
                "default_metrics_project": "sab-dev-nghp-shopping-2479"
        ],
        "loaddrivers"        : [
                [
                        "type"      : "jmeter",
                        "label"     : "jmeter-1",
                        "parameters": [

                                "plan"      :
                                        "git::https://proxy.git.sabre-gcp.com/projects/TNTLGS/repos/lgs-performance/browse/Jmeter_scripts/nghp-shopping/Release_NGHP_Shopping_Squatter_V4.jmx?at=lgs-components"
                                ,
                                "test_data" :
                                        "git::ssh://git@proxy.git.sabre.com/tntlgs/lgs-performance.git?ref=lgs-components&path=supporting_resources"
                                ,
                                "parameters": [
                                        "search_paths": "\${TEST_DATA}/supporting_resources/custom_ext_lib",
                                        "threadCount" : "10",
                                        "duration"    : "120",
                                        "loopCount"   : "20",
                                        "rampUpPeriod": "0",
                                        "throughput"  : "6000",
                                        "workspace"   : "\${TEST_DATA}/supporting_resources/nghp-shopping",
                                        "token"       :
                                                "U2hhcmVkL0lETDpJY2VTZXNzXC9TZXNzTWdyOjFcLjAuSURML0NvbW1vbi8hSUNFU01TXC9TVFNCIUlDRVNNU0xCXC9TVFMuTEIhMTY1MDg5NzE1NDc4MSExODM2ITUyMQ=="

                                ]

                        ]
                ]
        ],
        "datasources"        : [],
        "phases"             : [
                "phase1": [
                        "start"              : [
                                "reference": "test",
                                "state"    : "started"
                        ],
                        "end"                : [
                                "reference": "test",
                                "state"    : "finished",
                                "delta"    : "0"
                        ],
                        "acceptance_criteria": [[
                                                        "criteria"
                                                ]]
                ]
        ],
        "acceptance_criteria": [
                "criteria": [

                        "avg_CPU_utilization_Total" : [
                                "type"    : "avg",
                                "unit"    : "%",
                                "query"   : "from(bucket:\"plaas\") |> range(\$range) |> filter(fn: (r) => r._measurement == \"compute.googleapis.com/instance/cpu\" and r._field =~ /utilization/ and r.instance_id == \"6648633339000958902\") |> group() |> mean() |> map(fn: (r) => ({_value: r._value * 100.0}))|> yield()",
                                "criteria": [[
                                                     "assessment": "green",
                                                     "range"     : [
                                                             0,
                                                             70
                                                     ]
                                             ],
                                             [
                                                     "assessment": "yellow",
                                                     "range"     : [
                                                             70,
                                                             70
                                                     ]
                                             ],
                                             [

                                                     "assessment": "red",
                                                     "range"     : [
                                                             0,
                                                             70
                                                     ]
                                             ]]

                        ],
                        "avg_CPU_utilization_User"  : [
                                "type"    : "avg",
                                "unit"    : "%",
                                "query"   : "from(bucket:\"plaas\") |> range(\$range) |> filter(fn: (r) => r._measurement == \"agent.googleapis.com/cpu\" and r._field =~ /utilization/ and r.cpu_state == \"user\" and r.instance_id == \"6648633339000958902\") |> group() |> mean() |> yield()",
                                "criteria": [[
                                                     "assessment": "green",
                                                     "range"     : [
                                                             0,
                                                             70
                                                     ]
                                             ],
                                             [
                                                     "assessment": "yellow",
                                                     "range"     : [
                                                             70,
                                                             70
                                                     ]
                                             ],
                                             [

                                                     "assessment": "red",
                                                     "range"     : [
                                                             0,
                                                             70
                                                     ]
                                             ]]

                        ],
                        "avg_CPU_utilization_System": [
                                "type"    : "avg",
                                "unit"    : "%",
                                "query"   : "from(bucket:\"plaas\") |> range(\$range) |> filter(fn: (r) => r._measurement == \"agent.googleapis.com/cpu\" and r._field =~ /utilization/ and r.cpu_state == \"system\" and r.instance_id == \"6648633339000958902\") |> group() |> mean() |> yield()",
                                "criteria": [[
                                                     "assessment": "green",
                                                     "range"     : [
                                                             0,
                                                             70
                                                     ]
                                             ],
                                             [
                                                     "assessment": "yellow",
                                                     "range"     : [
                                                             70,
                                                             70
                                                     ]
                                             ],
                                             [

                                                     "assessment": "red",
                                                     "range"     : [
                                                             0,
                                                             70
                                                     ]
                                             ]]

                        ],
                        "avg_Mem_utilization(%)": [
                                "type"    : "avg",
                                "unit"    : "%",
                                "query": "from(bucket:\"plaas\") |> range(\$range) |> filter(fn: (r) => r._measurement == \"agent.googleapis.com/memory\" and r._field =~ /percent_used/ and r.state == \"used\" and r.instance_id == \"6648633339000958902\") |> group() |> mean() |> yield()",
                                "criteria": [[
                                                     "assessment": "green",
                                                     "range"     : [
                                                             0,
                                                             70
                                                     ]
                                             ],
                                             [
                                                     "assessment": "yellow",
                                                     "range"     : [
                                                             70,
                                                             70
                                                     ]
                                             ],
                                             [

                                                     "assessment": "red",
                                                     "range"     : [
                                                             0,
                                                             70
                                                     ]
                                             ]]

                        ],
                        "avg_Mem_utilization_(GBytes)": [
                                "type"    : "avg",
                                "unit"    : "GB",
                                "query": "from(bucket:\"plaas\") |> range(\$range) |> filter(fn: (r) => r._measurement == \"agent.googleapis.com/memory\" and r._field =~ /bytes_used/ and r.instance_id == \"6648633339000958902\" and r.state == \"used\") |> group() |> mean() |> map(fn: (r) => ({_value: r._value / 1024.0 / 1024.0 / 1024.0})) |> yield()",
                                "criteria": [[
                                                     "assessment": "green",
                                                     "range"     : [
                                                             0,
                                                             70
                                                     ]
                                             ],
                                             [
                                                     "assessment": "yellow",
                                                     "range"     : [
                                                             70,
                                                             70
                                                     ]
                                             ],
                                             [

                                                     "assessment": "red",
                                                     "range"     : [
                                                             0,
                                                             70
                                                     ]
                                             ]]

                        ]



                ]
        ],
        "notifications":[[
                                 "type": "email",
                                 "on_events": [
                                         "report",
                                         "finished"
                                 ],
                                 "parameters":[
                                         "recipients":[
                                                 "Chari.veeraBrahmachari@sabre.com",
                                                 "Sanjana.SS@sabre.com"
                                         ]
                                 ]
                         ]

        ]


]
def json_str = JsonOutput.toJson(data)
println(json_str)
def json_beauty = JsonOutput.prettyPrint(json_str)
println(json_beauty)

File file = new File("Normal.json")
file.write(json_beauty)

