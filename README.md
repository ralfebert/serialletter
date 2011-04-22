# serialletter

A Java library for replacing serial letter fields in zipped XML office documents. Currently supported are Apple Pages '09 documents.

## Usage example

	PagesDocument doc = new PagesDocument(this.getClass().getResourceAsStream("test.pages"));
	doc.setValue("Name", "Otto Mustermann");
	doc.setValue("Address", "Example Street 123");
	doc.setDefaultValue("???");
	doc.apply(new FileOutputStream(outFile));

## Maven Dependency

	<dependency>
		<groupId>de.ralfebert</groupId>
		<artifactId>serialletter</artifactId>
		<version>1.0.0</version>
	</dependency>

## License

You can use serialletter under the conditions of one of the following licenses:

* The Apache Software License, Version 2.0
* Eclipse Public License - v 1.0