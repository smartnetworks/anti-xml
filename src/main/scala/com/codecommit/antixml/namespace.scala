package com.codecommit.antixml

import annotation.tailrec

/*
 * The URI is required and cannot be an empty namespace name. See "2.2 Use of URIs as Namespace Names", "The empty
 * string, though it is a legal URI reference, cannot be used as a namespace name.".
 *
 */

object NamespaceEntry {
  def apply(uri: String): NamespaceEntry = UnprefixedNamespaceBinding(uri)

  def apply(prefix: String, uri: String): NamespaceEntry = PrefixedNamespaceBinding(prefix, uri)
}

sealed trait NamespaceEntry {
  def uri: Option[String]
  def prefix: Option[String]
  def fqname(local: String) = FQName(uri.getOrElse(""), local, prefix)
}

case class NamespaceBinding(bindings: List[NamespaceEntry] = List.empty) extends Iterable[NamespaceEntry] {
 
  def append(nse: NamespaceEntry) = copy(bindings = nse :: bindings)

  def append(uri: String): NamespaceBinding = append(UnprefixedNamespaceBinding(uri))

  def append(prefix: String, uri: String): NamespaceBinding = append(PrefixedNamespaceBinding(prefix, uri))

  def iterator = {
    var checked = Set.empty[String]
    @tailrec
    def prepareNext(ns: List[NamespaceEntry]): List[NamespaceEntry] = ns match {
      case Nil => Nil
      case l @ nb :: parent =>
        val prefix = nb.prefix.getOrElse("")
        if (checked.contains(prefix)) prepareNext(parent) else {
          checked = checked + prefix
          l
        }
    }
    var ns = prepareNext(bindings)
    new Iterator[NamespaceEntry] {
      def hasNext = !ns.isEmpty
      def next = { val nx = ns.head; ns = prepareNext(ns.tail); nx }
    }
  }

  final def findByPrefix(prefix: String): Option[NamespaceEntry] = find(_.prefix.getOrElse("") == prefix)

  // If prefix is supplied, then an UnprefixedNamespaceBinding will never be returned
  final def findByUri(uri: String, prefix: String = ""): Option[NamespaceEntry] = {
    val muri = Option(uri)
    val mpfx = if (prefix.isEmpty) None else Some(prefix)
    find(nb => nb.uri == muri && (mpfx.isEmpty || mpfx == nb.prefix))
  }

  final def findPrefixes(uri: String): List[String] = {
    val muri = Option(uri)
    collect { case nb if (nb.uri == muri) => nb.prefix.getOrElse("") }.toList
  }
}

object NamespaceBinding {
  
  val empty = new NamespaceBinding()
  
  def apply(t: (String, String)):NamespaceBinding = apply(t._1, t._2)
  
  def apply(nse: NamespaceEntry) = new NamespaceBinding(nse::Nil)

  def apply(prefix: String, uri: String):NamespaceBinding  = apply(PrefixedNamespaceBinding(prefix, uri))

  def apply(uri: String):NamespaceBinding = apply(UnprefixedNamespaceBinding(uri))
  
  def apply(prefix: String, uri: String, ns: NamespaceBinding) = ns.append(prefix, uri)

  def apply(uri: String, ns: NamespaceBinding) = ns.append(uri)
  
  def unapply(nb: NamespaceEntry) = Some((nb.prefix, nb.uri))
}

object NamespaceUri {
  def unapply(namespaceBinding: NamespaceEntry) = namespaceBinding.uri
}

sealed class NSRepr(val uri: String)

case class FQName(uri: String, local: String, defaultPrefix: Option[String] = None)

object NSRepr {
  def apply(uri: String): NSRepr = new NSRepr(uri)
  def apply(nb: NamespaceEntry): NSRepr = new NSRepr(nb.uri.getOrElse(throw new IllegalArgumentException("A namespace binding has to have an URI")))
}

case class PrefixedNamespaceBinding(_prefix: String, _uri: String) extends NamespaceEntry {
  if (!Elem.isValidName(_prefix)) {
    throw new IllegalArgumentException("Illegal namespace prefix, '" + prefix + "'")
  }
  if (!Elem.isValidNamespaceUri(_uri)) {
    throw new IllegalArgumentException("Illegal namespace uri, '" + _uri + "'")
  }

  def prefix = Some(_prefix)
  def uri = Some(_uri)
}

case class UnprefixedNamespaceBinding(_uri: String) extends NamespaceEntry {
  if (!Elem.isValidNamespaceUri(_uri)) {
    throw new IllegalArgumentException("Illegal namespace uri, '" + _uri + "'")
  }

  def uri = Some(_uri)
  def prefix = None
}

object ElemNamespaceUri {
  def unapply(elem: Elem) = elem.namespaces.findByPrefix(elem.prefix.getOrElse("")).flatMap(_.uri)
}
