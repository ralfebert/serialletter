package de.ralfebert.docstamper;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

class KeepOpenInputStream extends FilterInputStream {

	protected KeepOpenInputStream(InputStream in) {
		super(in);
	}

	@Override
	public void close() throws IOException {
		// don't
	}

}