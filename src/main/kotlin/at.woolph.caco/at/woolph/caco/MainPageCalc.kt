package at.woolph.caco

/**
 *
 */
fun main() {
	var line = readLine()

	val pocketsPerPageSide = 9

	while (!line.isNullOrBlank()) {
		line.toIntOrNull()?.let { setNumber ->
			val it = setNumber - 1
			val page = (it / pocketsPerPageSide) / 2

			val pocket = it % pocketsPerPageSide

			val pageSide = if ((it / pocketsPerPageSide) % 2 == 0) "front" else "back"

			println("p${page+1} $pageSide")

			for (x in 0..2) {
				for (y in 0..2) {
					print(if(x*3+y == pocket) "\u25AE" else "\u25AF")
				}
				println()
			}
		}
		line = readLine()
	}
}
