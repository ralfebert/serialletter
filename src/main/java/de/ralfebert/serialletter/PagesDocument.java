package de.ralfebert.serialletter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.io.IOUtils;

/**
 * PagesDocument replaces serial letter fields in Apple Pages '09 .pages
 * document files. Usage example:
 * 
 * <pre>
 * PagesDocument doc = new PagesDocument(this.getClass().getResourceAsStream(&quot;test.pages&quot;));
 * doc.setValue(&quot;Name&quot;, &quot;Otto Müstermanß&quot;);
 * doc.setValue(&quot;Address&quot;, &quot;Example Street 123&quot;);
 * doc.setDefaultValue(&quot;???&quot;);
 * doc.apply(doc.apply(new FileOutputStream(outFile)););
 * </pre>
 * 
 * @author Ralf Ebert <info@ralfebert.de>
 * @see http://www.ralfebert.de/blog/java/serialletter/
 */
public class PagesDocument {

	private static final String NS_SF = "http://developer.apple.com/namespaces/sf";
	private static final String NS_SFA = "http://developer.apple.com/namespaces/sfa";

	private final InputStream src;
	private final Map<String, String> values = new HashMap<String, String>();
	private String defaultValue = "";

	/**
	 * Creates a new PagesDocument instance with the contents of the src
	 * document.
	 */
	public PagesDocument(InputStream src) {
		this.src = src;
	}

	/**
	 * Sets values to replace by a map fieldName => value.
	 */
	public void setValues(Map<String, String> values) {
		this.values.putAll(values);
	}

	/**
	 * Sets a single field to replace.
	 */
	public void setValue(String fieldName, String value) {
		this.values.put(fieldName, value);
	}

	/**
	 * Sets the default value to use if no value was specified for a field. By
	 * default, "" is used as value.
	 * 
	 * @param defaultValue
	 */
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Overwrite this method to provide values for fields on-demand.
	 */
	protected String getFieldValue(String fieldName) {
		String value = values.get(fieldName);
		if (value == null)
			value = defaultValue;
		return value;
	}

	/**
	 * Replace the fields in the src documents by the values and write the
	 * resulting file to dest.
	 */
	public void replaceFields(OutputStream dest) {
		ZipInputStream zipsrc = new ZipInputStream(src);
		ZipOutputStream zipdest = new ZipOutputStream(dest);
		try {
			ZipEntry entry;
			while ((entry = zipsrc.getNextEntry()) != null) {
				String name = entry.getName();
				zipdest.putNextEntry(new ZipEntry(name));
				if (name.equals("index.xml")) {
					processIndexXml(zipsrc, zipdest);
				} else {
					IOUtils.copy(zipsrc, zipdest);
				}
				zipsrc.closeEntry();
				zipdest.closeEntry();
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} finally {
			IOUtils.closeQuietly(zipsrc);
			IOUtils.closeQuietly(zipdest);
			IOUtils.closeQuietly(src);
			IOUtils.closeQuietly(dest);
		}
	}

	private void processIndexXml(InputStream src, OutputStream dest) throws XMLStreamException,
			FactoryConfigurationError {

		XMLEventFactory eventFactory = XMLEventFactory.newInstance();
		XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(new KeepOpenInputStream(src));
		XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(dest);

		String lastFieldId = null;
		Map<String, String> fieldMapping = new HashMap<String, String>();
		boolean skip = false;

		while (reader.hasNext()) {
			XMLEvent event = (XMLEvent) reader.next();
			if (event.isStartElement()) {
				StartElement e = event.asStartElement();
				QName name = e.getName();

				// Handle declaration of fields:
				// <sf:merge-field sf:category="to"
				// sfa:ID="SFWPMergeField-0"><sf:table-field sfa:string="Name"/>
				// by building the fieldMapping map ID => Name
				if (name.getNamespaceURI().equals(NS_SF) && name.getLocalPart().equals("merge-field")) {
					lastFieldId = e.getAttributeByName(new QName(NS_SFA, "ID")).getValue();
				}

				if (name.getNamespaceURI().equals(NS_SF) && name.getLocalPart().equals("table-field")) {
					String fieldName = e.getAttributeByName(new QName(NS_SFA, "string")).getValue();
					fieldMapping.put(lastFieldId, fieldName);
					lastFieldId = null;
				}

				// Freeze the map when a page starts, just being paranoid

				if (name.getNamespaceURI().equals(NS_SF) && name.getLocalPart().equals("page-start")) {
					fieldMapping = Collections.unmodifiableMap(fieldMapping);
				}

				// Replace field references <sf:merge-field-ref
				// sfa:IDREF="SFWPMergeField-0">value</sf:merge-field-ref> by
				// the actual value and skip everything until the
				// merge-field-ref element ends

				if (name.getNamespaceURI().equals(NS_SF) && name.getLocalPart().equals("merge-field-ref")) {
					skip = true;
					String fieldIdRef = e.getAttributeByName(new QName(NS_SFA, "IDREF")).getValue();
					String fieldName = fieldMapping.get(fieldIdRef);
					writer.add(eventFactory.createCharacters(getFieldValue(fieldName)));
				}
			}

			if (!skip)
				writer.add(event);

			if (event.isEndElement()) {
				EndElement e = event.asEndElement();
				QName name = e.getName();
				if (name.getNamespaceURI().equals(NS_SF) && name.getLocalPart().equals("merge-field-ref")) {
					skip = false;
				}
			}
		}
		writer.flush();
	}

}
