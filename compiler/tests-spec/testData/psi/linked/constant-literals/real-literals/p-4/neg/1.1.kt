/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACES: constant-literals, real-literals -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Real literals with a not allowed underscore at the beginning.
 */

val value = _.0_0f
val value = _.0__0___0e1
val value = _.0_0__0_0f

val value = _.0
val value = _.0______0
val value = _______________00.0_0_0F

val value = _001.0_0e10f
val value = _4.33__3.00__0E-10
val value = __4_44____4.00______00E0
val value = _________555_________5.0E-0
val value = _666_666.0_____________________________________________________________________________________________________________________0e-0000000f
val value = ____.0_0_0F
val value = _8888888_8.0000
val value = _______9______9_____9____9___9__9_9.0f

val value = _0_0_0_0_0_0_0_0_0.1234567890
val value = _9e-10
val value = ___234_5_678.345______________678E+000001000000____
val value = ___3_456_7.4567E
val value = _456.5_6F
val value = _5.6_5
val value = ___6_54.7654e-0
val value = _7_6543.87654_3E+
val value = _____________________876543_____________2.9_____________8765432E-1100F
val value = _0_9_____________87654321.098765432_____________1ef

val value = _000000000000000000000000000000000000000000000000000000000000000000000000000000000000000___0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000_0F
val value = _______________0_000000000000000000000000000000000000000000000000000000000000000000000000000000000000000.0___000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e-10
val value = ___9999999999999999999999999999999999999999999_______________999999999999999999999999999999999999999999999.33333333333333333333333333333333333333333333333_33333333333333333333333333333333333333333E0
