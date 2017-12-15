package org.inwrap

import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.async.DeferredResult

@RestController
@RequestMapping("diameter")
class ActivityController constructor() {

	@PostMapping("{diameterType}")
	fun sendCode(@PathVariable diameterType: String, @RequestBody request: RequestDo): DeferredResult<ResponseDo> {
		if (diameterType != "ccs")
			throw IllegalArgumentException("Invalid Diameter Type")

		val ec = Client().configure(request)

		val result = DeferredResult<ResponseDo>()
		result.setResult(ResponseDo(300, "cmd", listOf(AvpDo(0, "testKey", "testVal", 64, listOf(), mapOf(Pair("a", "b"))))))
		return result
	}
}

