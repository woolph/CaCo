/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.collection.labels.box

interface BoxLabel {
  val title: String
  val subtitle: String?
    get() = null
}
