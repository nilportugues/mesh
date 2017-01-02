package com.gentics.mesh.search.index.microschema;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractIndexHandler;

/**
 * Handler for the elastic search microschema index.
 */
@Singleton
public class MicroschemaContainerIndexHandler extends AbstractIndexHandler<MicroschemaContainer> {

	@Inject
	MicroschemaTransformator transformator;

	@Inject
	public MicroschemaContainerIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		super(searchProvider, db, boot, searchQueue);
	}

	public MicroschemaTransformator getTransformator() {
		return transformator;
	}

	@Override
	protected String getIndex(SearchQueueEntry entry) {
		return MicroschemaContainer.TYPE;
	}

	@Override
	protected String getDocumentType(SearchQueueEntry entry) {
		return MicroschemaContainer.TYPE;
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return Collections.singleton(MicroschemaContainer.TYPE);
	}

	@Override
	public String getKey() {
		return MicroschemaContainer.TYPE;
	}

	@Override
	protected RootVertex<MicroschemaContainer> getRootVertex() {
		return boot.meshRoot().getMicroschemaContainerRoot();
	}

}
