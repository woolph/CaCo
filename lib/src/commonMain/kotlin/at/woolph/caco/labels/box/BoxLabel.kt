package at.woolph.caco.labels.box

interface BoxLabel {
  val title: String
  val subtitle: String?
    get() = null
}
