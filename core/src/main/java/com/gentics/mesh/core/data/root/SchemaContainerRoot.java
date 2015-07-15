package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.rest.schema.Schema;

public interface SchemaContainerRoot extends RootVertex<SchemaContainer> {

	SchemaContainer create(Schema schema);

	void addSchemaContainer(SchemaContainer schemaContainer);

	void removeSchemaContainer(SchemaContainer schemaContainer);

	boolean contains(SchemaContainer schema);

}