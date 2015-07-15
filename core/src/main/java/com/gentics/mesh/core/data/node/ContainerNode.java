package com.gentics.mesh.core.data.node;

import java.util.List;

import com.gentics.mesh.core.data.GenericNode;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.common.AbstractRestModel;

public interface ContainerNode<T extends AbstractRestModel> extends GenericNode<T> {

	List<? extends Node> getChildren();

	void setParentNode(ContainerNode<T> parentNode);

	Node create(User creator, SchemaContainer schemaContainer, Project project);

}