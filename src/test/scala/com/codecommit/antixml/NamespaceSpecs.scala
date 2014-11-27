package com.codecommit.antixml

import org.specs2.mutable._

class NamespaceSpecs extends Specification {
  val urnA = NamespaceEntry("urn:a")
  val urnB = NamespaceEntry("urn:b")

  "findByUri" should {
    "find None on empty" in {
      NamespaceBinding.empty.findByUri("urn:a") must beNone
    }

    "find Some('urn:a') from 'urn:a'" in {
      NamespaceBinding(urnA).findByUri("urn:a") must beSome(urnA)
    }

    // Should be hidden if a more recent ns with same prefix is found
    "find None from 'urn:a' :: 'urn:b'" in {
      NamespaceBinding("urn:b", NamespaceBinding(urnA)).findByUri("urn:a") must beNone
    }

    "find None from NamespaceBinding('urn:a')" in {
      NamespaceBinding(urnA).findByUri("urn:b") must beNone
    }
  }
}
