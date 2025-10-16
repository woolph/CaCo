/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli

class PagePositionCalculator(
    val pageMetadata: PageMetadata = PageMetadata.EIGTHTEEN_POCKET_PAGE,
) {
  data class PageMetadata(
      val name: String,
      val rowsPerPage: Int,
      val columnsPerPage: Int,
      val loadableBackside: Boolean,
  ) {
    val pocketsPerPageFace = rowsPerPage * columnsPerPage
    val pocketsPerPage = if (loadableBackside) pocketsPerPageFace * 2 else pocketsPerPageFace

    companion object {
      val NINE_POCKET_PAGE = PageMetadata("9-pocket page", 3, 3, false)
      val EIGTHTEEN_POCKET_PAGE = PageMetadata("18-pocket page", 3, 3, true)
      val EIGHT_POCKET_LANDSCAPE_PAGE = PageMetadata("8-pocket landscape page", 4, 2, true)
    }
  }

  data class PageInformation(
      val pageMetadata: PageMetadata,
      val page: Int,
      val pocket: Int,
      val face: Face,
  ) {
    enum class Face {
      FRONT,
      BACK,
    }
  }

  fun printPageNumberAndPosition(setNumber: Int): PageInformation {
    require(setNumber > 0)
    val it = setNumber - 1
    return PageInformation(
        pageMetadata = pageMetadata,
        page = (it / pageMetadata.pocketsPerPage),
        pocket = it % pageMetadata.pocketsPerPageFace,
        face =
            if (!pageMetadata.loadableBackside || (it / pageMetadata.pocketsPerPageFace) % 2 == 0)
                PageInformation.Face.FRONT
            else PageInformation.Face.BACK,
    )
  }
}
