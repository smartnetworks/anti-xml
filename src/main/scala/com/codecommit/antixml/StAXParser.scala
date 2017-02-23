/*
 * Copyright (c) 2011, Daniel Spiewak
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer. 
 * - Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 * - Neither the name of "Anti-XML" nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.codecommit.antixml

import util.VectorCase
import java.io.{InputStream, StringReader, Reader}
import javax.xml.stream._
import javax.xml.stream.XMLStreamConstants
import javax.xml.transform.stream.StreamSource

/*
<a/>
  START_ELEMENT, xmlReader.getNamespaceCount=0

<a xmlns='urn:a'/>
  START_ELEMENT, xmlReader.getNamespaceCount=1
  xmlReader.getNamespaceURI(i)=urn:a
  rawPrefix=null
  prefix=

 */

/**
 * An XML parser build on top of `javax.xml.stream`.  This implements the same
 * API as [[com.codecommit.antixml.SAXParser]], but the runtime performance is
 * on the order of 12% faster.
 */
class StAXParser extends XMLParser {

  val factory = XMLInputFactory.newInstance()

  override def fromInputStream(inputStream: InputStream): Elem =
    fromStreamSource(new StreamSource(inputStream))
  
  override def fromReader(reader: Reader): Elem =
    fromStreamSource(new StreamSource(reader))
  
  override def fromString(xml: String): Elem =
    fromReader(new StringReader(xml))
  
  private case class ElemBuilder(prefix: Option[String], name: String, namespaces: NamespaceBinding, attrs: Attributes)

  private def fromStreamSource(source: StreamSource): Elem = {
    import XMLStreamConstants.{CHARACTERS, END_ELEMENT, START_ELEMENT}

    val xmlReader = factory.createXMLStreamReader(source)
    var elems: List[ElemBuilder] = Nil
    var scopes = NamespaceBinding.empty :: Nil
    var results = VectorCase.newBuilder[Node] :: Nil
    val text = new StringBuilder
    while(xmlReader.hasNext) {
      xmlReader.next match {
        case `CHARACTERS` =>
          text.appendAll(xmlReader.getTextCharacters, xmlReader.getTextStart, xmlReader.getTextLength)
        case `END_ELEMENT` => {
          val elem = elems.head
          val parents = elems.tail
          val children = results.head
          val ancestors = results.tail

          if (text.size > 0) {
            children += Text(text.result())
            text.clear()
          }

          ancestors.head += Elem(elem.prefix, elem.name, elem.attrs, elem.namespaces, Group fromSeq children.result)
          elems = parents
          results = ancestors
          scopes = scopes.tail
        }
        case `START_ELEMENT` => {
          if (text.size > 0) {
            results.head += Text(text.result())
            text.clear()
          }
          val currentScopes = handleNamespaces(xmlReader, scopes.head)

          val prefix = Option(xmlReader.getPrefix).filterNot(_.isEmpty)
          val attrs: Attributes = handleAttributes(xmlReader)
          elems ::= ElemBuilder(prefix, xmlReader.getLocalName, currentScopes, attrs)
          scopes ::= currentScopes
          results ::= VectorCase.newBuilder[Node]
        }
        case _ =>
      }
    }
    results.head.result().head.asInstanceOf[Elem]
  }


  private def handleAttributes(xmlReader: XMLStreamReader): Attributes = {
    var i = 0
    var attrs = Attributes()
    while (i < xmlReader.getAttributeCount) {
      val localName = xmlReader.getAttributeLocalName(i)
      val prefix = Option(xmlReader.getAttributePrefix(i)).filterNot(_.isEmpty)
      attrs = attrs + (QName(prefix, localName) -> xmlReader.getAttributeValue(i))
      i = i + 1
    }
    attrs
  }

  private def handleNamespaces(xmlReader: XMLStreamReader, _scopes: NamespaceBinding): NamespaceBinding = {
    var scopes: NamespaceBinding = _scopes
    var i = 0

    while (i < xmlReader.getNamespaceCount) {
      val ns = xmlReader.getNamespaceURI(i)
      val rawPrefix = xmlReader.getNamespacePrefix(i)
      val prefix = if (rawPrefix != null) rawPrefix else ""
      val uri = if (ns == null) "" else ns
      scopes = XMLParser.selectBinding(prefix, uri, scopes)
      i = i + 1
    }
    scopes
  }

  def isBlank(back: String): Boolean = {
    back == null || back.trim == ""
  }
}
