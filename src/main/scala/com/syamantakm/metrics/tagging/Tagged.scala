package com.syamantakm.metrics.tagging

/**
 * A trait which allows individual events to be tagged
 *
 * @author syamantak.
 */
trait Tagged {
  def tags: Seq[String]
}
