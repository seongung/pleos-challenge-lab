package com.pleosjournal.domain

/** UC-09 판정 규칙. 제한이 "없다"고 확인된 경우에만 입력을 연다. 모르면(null) 닫는다(fail-safe). */
fun isInputAllowed(requiresDistractionOptimization: Boolean?) = requiresDistractionOptimization == false
