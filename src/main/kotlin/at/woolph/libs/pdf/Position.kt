package at.woolph.libs.pdf

data class Position(
    val x: Float,
    val y: Float,
) {
    operator fun plus(p: Position) = Position(x + p.x, y + p.y)
    operator fun times(d: Float) = Position(x * d, y * d)
    operator fun times(p: Position) = Position(x * p.x, y * p.y)
    operator fun div(d: Float) = Position(x / d, y / d)
    operator fun div(p: Position) = Position(x / p.x, y / p.y)
    operator fun minus(p: Position) = Position(x - p.x, y - p.y)
    operator fun unaryPlus() = this
    operator fun unaryMinus() = Position(-x, -y)
}