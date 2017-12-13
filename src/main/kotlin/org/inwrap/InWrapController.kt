package org.inwrap

import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.async.DeferredResult


@RestController
@RequestMapping("diameter")
class ActivityController constructor() {

	@PostMapping("{diameterType}/{code}")
	fun sendCode(@PathVariable diameterType: String, @PathVariable code: Int, @RequestBody request: RequestDo): DeferredResult<ResponseDo> {

		val result = DeferredResult<ResponseDo>()
		result.setResult(ResponseDo(mapOf("Done" to "Yes")))
		return result
	}
}

