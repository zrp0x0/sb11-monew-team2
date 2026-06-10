package com.codeit.monew.infra.externalapi.hankyung.parser;

import com.codeit.monew.infra.externalapi.hankyung.dto.HankyungRssItem;
import java.io.StringReader;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class HankyungRssParser {

    public List<HankyungRssItem> parse(String rssXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(rssXml)));

            NodeList itemNodes = document.getElementsByTagName("item");

            return java.util.stream.IntStream.range(0, itemNodes.getLength())
                    .mapToObj(itemNodes::item)
                    .filter(node -> node instanceof Element)
                    .map(node -> (Element) node)
                    .map(itemElement -> new HankyungRssItem(
                            getTextContent(itemElement, "title"),
                            getTextContent(itemElement, "link"),
                            getTextContent(itemElement, "description"),
                            getTextContent(itemElement, "pubDate")
                    ))
                    .toList();
        } catch (Exception e) {
            throw new IllegalArgumentException("한국경제 RSS 파싱에 실패했습니다.", e);
        }
    }

    private String getTextContent(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);

        if (nodeList.getLength() == 0) {
            return null;
        }

        return nodeList.item(0).getTextContent();
    }
}