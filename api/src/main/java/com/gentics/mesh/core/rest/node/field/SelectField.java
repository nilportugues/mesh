package com.gentics.mesh.core.rest.node.field;

import java.util.List;

public interface SelectField extends ListableField, MicroschemaListableField {

	void setSelections(List<String> selections);

	List<String> getSelections();

}