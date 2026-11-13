package com.pleosjournal.platform

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.util.Log
import com.pleosjournal.domain.isInputAllowed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 주행 제한 게이트(UC-09). 플랫폼의 CarUxRestrictions만 근거로 쓴다.
 * 트립 판정(TripDetector) 결과는 여기에 절대 쓰지 않는다(CLAUDE.md 제약 5, 6).
 *
 * 클래스, 메서드 출처:
 * https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide
 * (Android 표준 android.car API. CarUxRestrictionsManager.registerListener, isRequiresDistractionOptimization)
 */
class DrivingGate(context: Context) {
    private val _inputAllowed = MutableStateFlow(isInputAllowed(null)) // 시작값은 닫힘
    /** true일 때만 입력, 편집, 회고 UI를 연다. */
    val inputAllowed: StateFlow<Boolean> = _inputAllowed

    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    init {
        try {
            val car = Car.createCar(context).also { car = it }
            val m = (car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager).also { manager = it }
            update(m.currentCarUxRestrictions)
            m.registerListener(::update)
        } catch (t: Throwable) {
            // 차량이 아닌 기기(android.car 없음)나 서비스 오류. 상태를 모르므로 닫힌 채로 둔다.
            Log.w("DrivingGate", "CarUxRestrictions unavailable, input stays closed", t)
        }
    }

    private fun update(r: CarUxRestrictions?) {
        _inputAllowed.value = isInputAllowed(r?.isRequiresDistractionOptimization)
    }

    fun close() {
        try {
            manager?.unregisterListener()
            car?.disconnect()
        } catch (t: Throwable) {
            Log.w("DrivingGate", "close failed", t)
        }
    }
}
