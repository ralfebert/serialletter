# serialletter

A Java library for replacing serial letter fields in zipped XML office documents. Currently supported are Apple Pages '09 documents.

Usage example:

	PagesDocument doc = new PagesDocument(this.getClass().getResourceAsStream("test.pages"));
	doc.setValue("Name", "Otto Mustermann");
	doc.setValue("Address", "Example Street 123");
	doc.setDefaultValue("???");
	doc.apply(new FileOutputStream(outFile));
