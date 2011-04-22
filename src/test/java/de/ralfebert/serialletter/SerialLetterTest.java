package de.ralfebert.serialletter;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import de.ralfebert.serialletter.PagesDocument;

public class SerialLetterTest {

	@Test
	public void testApplePagesDocument() throws Exception {
		Map<String, String> fields = new HashMap<String, String>();
		fields.put("Name", "Otto Müstermanß");

		for (int i = 0; i < 3; i++) {
			long l = System.currentTimeMillis();

			PagesDocument doc = new PagesDocument(this.getClass().getResourceAsStream("test.pages"));
			doc.setValues(fields);
			doc.setValue("Address", "Example Street 123");
			doc.setDefaultValue("???");
			File tmpFile = File.createTempFile("test", ".pages");
			doc.replaceFields(new FileOutputStream(tmpFile));

			System.out.println("Took " + (System.currentTimeMillis() - l) + "ms to apply to " + tmpFile);

			assertEquals(extractIndexXml(new FileInputStream(tmpFile)),
					IOUtils.toString(this.getClass().getResourceAsStream("result.xml")));
		}

	}

	private String extractIndexXml(InputStream src) throws IOException, FileNotFoundException {
		ZipInputStream in = new ZipInputStream(src);
		ZipEntry entry;
		while ((entry = in.getNextEntry()) != null) {
			if (entry.getName().equals("index.xml"))
				return IOUtils.toString(in);
		}
		throw new IllegalStateException("No index.xml in document!");
	}

}
