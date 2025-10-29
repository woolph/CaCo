package at.woolph.caco.labels.binder

interface MapLabelItem {
  val code: String
  val title: String
  val mainIcon: ByteArray?
  val subCode: String?
    get() = null

  val subTitle: String?
    get() = null

  val subIconLeft: ByteArray?
    get() = null

  val subIconRight: ByteArray?
    get() = null

  val subIconLeft2: ByteArray?
    get() = null

  val subIconRight2: ByteArray?
    get() = null
}
