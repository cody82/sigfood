package de.sigfood;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SigfoodApi {
	public ArrayList<MensaEssen> essen = new ArrayList<MensaEssen>();
	public Date speiseplandatum;
	public Date naechstertag;
	public Date vorherigertag;

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
		this(null);
	}
	
	public SigfoodApi(Date date) {

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
			String url = "http://www.sigfood.de/?do=api.gettagesplan";
			if (date != null) {
				url += ("&datum=");
				url += String.format("%tY-%tm-%td", date, date, date);
			}
			doc = dBuilder.parse(url);
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

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		speiseplandatum = new Date();
		naechstertag = null;
		vorherigertag = null;
		try {
			NodeList blubb = doc.getDocumentElement().getElementsByTagName("Tagesmenue");
			speiseplandatum = df.parse(getChildNode(blubb.item(0), "tag").getTextContent());
			vorherigertag = df.parse(getChildNode(blubb.item(0), "vorherigertag").getTextContent());
			naechstertag = df.parse(getChildNode(blubb.item(0), "naechstertag").getTextContent());
		} catch (DOMException e1) {
			// nothing to do, just ignore it
		} catch (ParseException e1) {
			// nothing to do, just ignore it
		} catch (Exception e1) { /* ANY exception at all, we do not care */
			// nothing to do, just ignore it
		}
		
		for (int i = 0; i < list.getLength(); ++i){
			Node n = list.item(i);

			MensaEssen e = new MensaEssen();

			e.linie = getChildNode(n, "linie").getTextContent();
			e.datumskopie = speiseplandatum;

			NodeList l = n.getChildNodes();
			for (int j = 0; j < l.getLength(); ++j) {
				Node n2 = l.item(j);

				if(n2.getNodeName().equals("hauptgericht")){
					e.hauptgericht = new Hauptgericht();

					Node bewertung = getChildNode(n2, "bewertung");
					e.hauptgericht.bewertung.anzahl = Integer.parseInt(getChildNode(bewertung, "anzahl").getTextContent());
					if(e.hauptgericht.bewertung.anzahl > 0) {
						e.hauptgericht.bewertung.schnitt = Float.parseFloat(getChildNode(bewertung, "schnitt").getTextContent());
						e.hauptgericht.bewertung.stddev = Float.parseFloat(getChildNode(bewertung, "stddev").getTextContent());
					}

					if(getChildNode(n2, "preisbed") != null)
						e.hauptgericht.preis_bed = Float.parseFloat(getChildNode(n2, "preisbed").getTextContent()) * 0.01f;
					if(getChildNode(n2, "preisstud") != null)
						e.hauptgericht.preis_stud = Float.parseFloat(getChildNode(n2, "preisstud").getTextContent()) * 0.01f;
					if(getChildNode(n2, "preisgast") != null)
						e.hauptgericht.preis_gast = Float.parseFloat(getChildNode(n2, "preisgast").getTextContent()) * 0.01f;
					
					e.hauptgericht.id = Integer.parseInt(n2.getAttributes().getNamedItem("id").getTextContent());

					NodeList list2 = n2.getChildNodes();
					for(int k=0;k<list2.getLength();++k) {
						Node n3 = list2.item(k);
						if(n3.getNodeName().equals("bild")) {
							String tmp = n3.getAttributes().getNamedItem("id").getTextContent();
							e.hauptgericht.bilder.add(Integer.parseInt(tmp));
						}
						else if(n3.getNodeName().equals("kommentar")) {
							Kommentar kommentar = new Kommentar();
							kommentar.text = getChildNode(n3, "text").getTextContent();
							if(getChildNode(n3, "nick") != null) {
								kommentar.nick = getChildNode(n3, "nick").getTextContent();
							}
							else {
								kommentar.nick = "anon";
							}
							kommentar.datum = getChildNode(n3, "formattedtime").getTextContent();
							e.hauptgericht.kommentare.add(kommentar);
						}
					}
					e.hauptgericht.bezeichnung = getChildNode(n2, "bezeichnung").getTextContent();
				}
				else if(n2.getNodeName().equals("beilage")){
					Hauptgericht beilage = new Hauptgericht();

					beilage.id = Integer.parseInt(n2.getAttributes().getNamedItem("id").getTextContent());

					Node bewertung = getChildNode(n2, "bewertung");
					beilage.bewertung.anzahl = Integer.parseInt(getChildNode(bewertung, "anzahl").getTextContent());
					if(beilage.bewertung.anzahl > 0) {
						beilage.bewertung.schnitt = Float.parseFloat(getChildNode(bewertung, "schnitt").getTextContent());
						beilage.bewertung.stddev = Float.parseFloat(getChildNode(bewertung, "stddev").getTextContent());
					}

					if(getChildNode(n2, "preisbed") != null)
						e.hauptgericht.preis_bed = Float.parseFloat(getChildNode(n2, "preisbed").getTextContent()) * 0.01f;
					if(getChildNode(n2, "preisstud") != null)
						e.hauptgericht.preis_stud = Float.parseFloat(getChildNode(n2, "preisstud").getTextContent()) * 0.01f;
					if(getChildNode(n2, "preisgast") != null)
						e.hauptgericht.preis_gast = Float.parseFloat(getChildNode(n2, "preisgast").getTextContent()) * 0.01f;
					
					beilage.bezeichnung = getChildNode(n2, "bezeichnung").getTextContent();

					NodeList list2 = n2.getChildNodes();
					for(int k=0;k<list2.getLength();++k) {
						Node n3 = list2.item(k);
						if(n3.getNodeName().equals("kommentar")) {
							Kommentar kommentar = new Kommentar();
							kommentar.text = getChildNode(n3, "text").getTextContent();
							if(getChildNode(n3, "nick") != null) {
								kommentar.nick = getChildNode(n3, "nick").getTextContent();
							}
							else {
								kommentar.nick = "anon";
							}
							kommentar.datum = getChildNode(n3, "formattedtime").getTextContent();
							beilage.kommentare.add(kommentar);
						}
					}

					e.beilagen.add(beilage);
				}
			}
			essen.add(e);

		}
	}
}
