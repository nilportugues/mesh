package com.gentics.mesh.search;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.verticle.role.RoleVerticle;

public class RoleSearchVerticleTest extends AbstractSearchVerticleTest {

	@Autowired
	private RoleVerticle roleVerticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(roleVerticle);
		return list;
	}

	@Test
	@Override
	public void testDocumentCreation() throws InterruptedException, JSONException {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		// TODO Auto-generated method stub

	}
}