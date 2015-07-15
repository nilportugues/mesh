package com.gentics.mesh.core.data.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import com.gentics.mesh.core.data.GenericNode;
import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.generic.AbstractGenericNode;
import com.gentics.mesh.core.rest.schema.MicroschemaResponse;

public class MicroschemaContainerImpl extends AbstractGenericNode<MicroschemaResponse> implements MicroschemaContainer {

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public GenericNode<MicroschemaResponse> transformToRest(RoutingContext rc, Handler<AsyncResult<MicroschemaResponse>> handler) {
		MicroschemaResponse response = new MicroschemaResponse();
		//fillRest(response, rc);
		handler.handle(Future.succeededFuture(response));
		return this;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub

	}

}