package de.sigfood;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SigfoodApi {
	public ArrayList<MensaEssen> essen = new ArrayList<MensaEssen>();
	
	Node getChildNode(Node n, String name) {
		NodeList list = n.getChildNodes();
		for(int i=0;i<list.getLength();++i){
			Node n2=list.item(i);
			if(n2.getNodeName().equals(name)){
				return n2;
			}
		}
		return null;
	}
	
	public SigfoodApi() {
        
		
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		Document doc;
		try {
			doc = dBuilder.parse("http://www.sigfood.de/?do=api.gettagesplan");
			//doc = dBuilder.parse("/home/cody/sigfood.xml");
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		doc.getDocumentElement().normalize();

		
		NodeList list = doc.getDocumentElement().getElementsByTagName("Mensaessen");
		for(int i=0;i<list.getLength();++i){
			Node n=list.item(i);

			MensaEssen e = new MensaEssen();
			
			e.linie = Integer.parseInt(getChildNode(n, "linie").getTextContent());
			e.tag = getChildNode(n.getParentNode(), "tag").getTextContent();
			
			NodeList l = n.getChildNodes();
			for(int j=0;j<l.getLength();++j) {
				Node n2 = l.item(j);
				
				if(n2.getNodeName().equals("hauptgericht")){
					e.hauptgericht = new Hauptgericht();

					Node bewertung = getChildNode(n2, "bewertung");
					e.hauptgericht.bewertung.anzahl = Integer.parseInt(getChildNode(bewertung, "anzahl").getTextContent());
					if(e.hauptgericht.bewertung.anzahl > 0)
						e.hauptgericht.bewertung.schnitt = Float.parseFloat(getChildNode(bewertung, "schnitt").getTextContent());
					
					e.hauptgericht.id = Integer.parseInt(n2.getAttributes().getNamedItem("id").getTextContent());
					
					NodeList list2 = n2.getChildNodes();
					for(int k=0;k<list2.getLength();++k) {
						Node n3 = list2.item(k);
						if(n3.getNodeName().equals("bild")) {
							String tmp = n3.getAttributes().getNamedItem("id").getTextContent();
							e.hauptgericht.bilder.add(Integer.parseInt(tmp));
						}
						else if(n3.getNodeName().equals("kommentar")) {
							e.hauptgericht.kommentare.add(getChildNode(n3, "text").getTextContent());
						}
					}
					e.hauptgericht.bezeichnung = getChildNode(n2, "bezeichnung").getTextContent();
				}
				else if(n2.getNodeName().equals("beilage")){
					Hauptgericht beilage = new Hauptgericht();
					e.beilagen.add(beilage);
				}
			}
			essen.add(e);
			
		}
	}
}
