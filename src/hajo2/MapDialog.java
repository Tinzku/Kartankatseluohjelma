 /* Kartankatseluohjelman graafinen käyttöliittymä
 * @author Johan Sjövall & Tino Lehtola
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MapDialog extends JFrame {

    // Käyttöliittymän komponentit
    private JLabel imageLabel = new JLabel();
    private JPanel leftPanel = new JPanel();

    private JButton refreshB = new JButton("Päivitä");
    private JButton leftB = new JButton("<");
    private JButton rightB = new JButton(">");
    private JButton upB = new JButton("^");
    private JButton downB = new JButton("v");
    private JButton zoomInB = new JButton("+");
    private JButton zoomOutB = new JButton("-");

    private final String ServerinOsoite = "http://demo.mapserver.org/cgi-bin/wms?SERVICE=WMS&VERSION=1.1.1";
    private final String SRS = "EPSG:4326";

    // Resoluutio & formaatti 
    private final int Leveys = 960;
    private final int Korkeus = 480;
    private final String KuvaFormaati = "image/png";
    private final boolean Taustaton = true;

    // Kuvan sijainti
    private int x = 0;
    private int y = 0;
    private int z = 90;
    private int o = 20;
    private List<LayerCheckBox> checkboxes = new ArrayList<>();

    public MapDialog() throws Exception {

        // Valmistele ikkuna ja lisää siihen komponentit
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());

        String url = ServerinOsoite
                    + "&REQUEST=GetMap"
                    + String.format("&BBOX=%d,%d,%d,%d", (x - 2 * z), (y - z), (x + 2 * z), (y + z))
                    + "&SRS=" + SRS
                    + "&WIDTH=" + Leveys
                    + "&HEIGHT=" + Korkeus
                    + "&LAYERS=" + "bluemarble,cities"
                    + "&STYLES="
                    + "&FORMAT=" + KuvaFormaati
                    + "&TRANSPARENT=" + Taustaton;
        imageLabel.setIcon(new ImageIcon(new URL(url)));

        add(imageLabel, BorderLayout.EAST);

        ButtonListener bl = new ButtonListener();
        refreshB.addActionListener(bl);
        leftB.addActionListener(bl);
        rightB.addActionListener(bl);
        upB.addActionListener(bl);
        downB.addActionListener(bl);
        zoomInB.addActionListener(bl);
        zoomOutB.addActionListener(bl);

        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        leftPanel.setMaximumSize(new Dimension(100, 600));

        CheckBoxes();

        leftPanel.add(refreshB);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(leftB);
        leftPanel.add(rightB);
        leftPanel.add(upB);
        leftPanel.add(downB);
        leftPanel.add(zoomInB);
        leftPanel.add(zoomOutB);

        add(leftPanel, BorderLayout.WEST);

        pack();
        setVisible(true);

    }// MapDialog

    // Main-metodi luo aina vain uuden MapDialogin
    public static void main(String[] args) throws Exception {
        new MapDialog();
    }// Main

    // Parsetaan kartan antamat layerit dokumentistä nappuloiksi vasempaan paneeliin
    // Vaihtamalla "new LayerCheckBox(l, l, true); toinen l halutuksi napiksi, 
    // näkyy se sen nimisenä nappulana käyttöliittymässä
    private void CheckBoxes() {
        String url = ServerinOsoite + "&REQUEST=GetCapabilities";
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = f.newDocumentBuilder();
            Document d = db.parse(new URL(url).openStream());
            for (String l : getLayers(d)) {
                /*if(l.equals("cities")||l.equals("bluemarble")){
                    LayerCheckBox b = new LayerCheckBox(l, l, true);
                    checkboxes.add(b);
                    leftPanel.add(b);
                }*/
                if(l.equals("bluemarble")){
                  LayerCheckBox b = new LayerCheckBox(l, "Maapallo", true);
                  checkboxes.add(b);
                  leftPanel.add(b);
              }
                if(l.equals("cities")){
                  LayerCheckBox b = new LayerCheckBox(l, "Pääkaupungit", true);
                  checkboxes.add(b);
                  leftPanel.add(b);
              }
                if(l.equals("country_bounds")){
                  LayerCheckBox b = new LayerCheckBox(l, "Maiden rajat", false);
                  checkboxes.add(b);
                  leftPanel.add(b);
              }
                if(l.equals("continents")){
                  LayerCheckBox b = new LayerCheckBox(l, "Maanosat", false);
                  checkboxes.add(b);
                  leftPanel.add(b);
              }
                else{
                    /*LayerCheckBox b = new LayerCheckBox(l, l, false);
                    checkboxes.add(b);
                    leftPanel.add(b); */
                }
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
        }
    } // Checkboxes

    //XML:n parseemisesta saatu tiedosto parsetaan tasojen nimet dokumentiksi.
    private static List<String> getLayers(Document doc) {
        List<String> list = new ArrayList<>();

        XPathFactory xF = XPathFactory.newInstance();
        XPath xP = xF.newXPath();
        String txt = "/WMT_MS_Capabilities/Capability/Layer/Layer/Name/text()";
        try {
            XPathExpression expr = xP.compile(txt);
            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                list.add(nodes.item(i).getNodeValue());
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return list;
    }// getLayers

    // Kontrollinappien kuuntelija
    private class ButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == refreshB) {
            } else if (e.getSource() == leftB) {
                // VASEMMALLE SIIRTYMINEN KARTALLA
                x = x - o;
            } else if (e.getSource() == rightB) {
                // OIKEALLE SIIRTYMINEN KARTALLA
                x = x + o;
            } else if (e.getSource() == upB) {
                // YLÖSPÄIN SIIRTYMINEN KARTALLA
                y = y + o;
            } else if (e.getSource() == downB) {
                // ALASPÄIN SIIRTYMINEN KARTALLA
                y = y - o;
            } else if (e.getSource() == zoomInB) {
                // ZOOM IN -TOIMINTO
                z = new Double(z * 0.75).intValue();
            } else if (e.getSource() == zoomOutB) {
                // ZOOM OUT -TOIMINTO
                z = new Double(z * 1.25).intValue();
            }
            try {
                updateImage();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            System.out.println("x: " + x + "y: " + y + "z: " + z + "o: " + o);
        }
    }// ButtonListener    
    
    //updateImage päivittää kuvan hakemalla säikeen avulla uuden karttakuvan annetuilla parametreillä
    // Parametrit saadaan "checkboxes":sta löytyvistä valintalaatikoiden tiedoista
    public void updateImage() {
        String set = String.join(",", checkboxes.stream().filter(cb -> cb.isSelected()).map(cb -> cb.getName()).collect(Collectors.toList()));
        new MapThread(set).run();
    }// updateImage

    // Valintalaatikko, joka muistaa karttakerroksen nimen
    private class LayerCheckBox extends JCheckBox {

        private String name = "";

        public LayerCheckBox(String name, String title, boolean selected) {
            super(title, null, selected);
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }// LayerCheckBox

    // Säie joka hakee uuden karttakuvan palvelimelta
    private class MapThread extends Thread {

        private String Tasot;

        public MapThread(String s) {
            this.Tasot = s;
        }
        
        //Kuvan resettaus
        public void run() {
            int x1 = x - 2 * z,
                    y1 = y - z,
                    x2 = x + 2 * z,
                    y2 = y + z;

            String url = ServerinOsoite
                    + "&REQUEST=GetMap"
                    + String.format("&BBOX=%d,%d,%d,%d", x1, y1, x2, y2)
                    + "&SRS=" + SRS
                    + "&WIDTH=" + Leveys
                    + "&HEIGHT=" + Korkeus
                    + "&LAYERS=" + Tasot
                    + "&STYLES=" //Jätettiin tässä työssä käyttämättä
                    + "&FORMAT=" + KuvaFormaati
                    + "&TRANSPARENT=" + Taustaton;
            try {
                imageLabel.setIcon(new ImageIcon(new URL(url)));
            } catch (MalformedURLException m) {
            }
        } // run
    } // MapThread
} // MapDialog