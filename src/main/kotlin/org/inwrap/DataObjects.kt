package org.inwrap


data class RequestDo(val serverUrl: String,
                     val commandCode: Int,
                     val avps: List<AvpDo>)

data class ResponseDo(val commandCode: Int,
                      val command: String,
                      val avps: List<AvpDo>)

data class AvpDo(val code: Int,
                 val key: String,
                 val value: String,
                 val flag: Int = 0x40,
                 val avps: List<AvpDo>,
                 val extras: Map<String, String>)