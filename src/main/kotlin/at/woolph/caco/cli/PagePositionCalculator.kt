//package at.woolph.caco.cli
//
//import org.springframework.shell.standard.ShellComponent
//import org.springframework.shell.standard.ShellMethod
//
//class PagePositionCalculator(
//    val inputReader: InputReader,
//    val output: Output,
//) {
//    fun page(pocketsPerPageSide: Int = 9) {
//        do {
//            val result = inputReader.prompt("Set number:")?.toIntOrNull()?.apply {
//                printPageNumberAndPosition(pocketsPerPageSide, this)
//            }
//        } while(result != null)
//    }
//
////    @ShellMethod("Calculates Page Number and Page Position from Setnumber.")
////    fun page(pocketsPerPageSide: Int, setNumber: Int) {
////        printPageNumberAndPosition(pocketsPerPageSide, setNumber)
////    }
//
//    fun printPageNumberAndPosition(pocketsPerPageSide: Int, setNumber: Int) {
//        val it = setNumber - 1
//        val page = (it / pocketsPerPageSide) / 2
//        val pocket = it % pocketsPerPageSide
//        val pageSide = if ((it / pocketsPerPageSide) % 2 == 0) "front" else "back"
//
//        output.println("p${page + 1} $pageSide")
//
//        for (x in 0..2) {
//            for (y in 0..2) {
//                output.print(if (x * 3 + y == pocket) "\u25AE" else "\u25AF")
//            }
//            output.println()
//        }
//    }
//}
