package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.syncleus.ferma.AbstractEdgeFrame;

public class Translated extends AbstractEdgeFrame {

	public static final String LANGUAGE_TAG_KEY = "languageTag";

	public String getLanguageTag() {
		return getProperty(LANGUAGE_TAG_KEY);
	}

	public void setLanguageTag(String languageTag) {
		setProperty(LANGUAGE_TAG_KEY, languageTag);
	}

	public GenericNode getStartNode() {
		return inV().next(GenericNode.class);
	}

	public I18NProperties getI18NProperties() {
		return outV().next(I18NProperties.class);
	}
}