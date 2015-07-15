package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.BaseNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.RoutingContextHelper;

public class NodeTest extends AbstractBasicObjectTest {

	/**
	 * Test linking two contents
	 */
	@Test
	public void testPageLinks() {
		Node folder = data().getFolder("2015");
		Node node = folder.create(getUser(), getSchemaContainer(), getProject());
		Node node2 = folder.create(getUser(), getSchemaContainer(), getProject());

		NodeFieldContainer englishContainer = node2.getOrCreateFieldContainer(data().getEnglish());
		englishContainer.createString("content").setString("english content");
		englishContainer.createString("name").setString("english.html");

		NodeFieldContainer englishContainer2 = node.getOrCreateFieldContainer(data().getGerman());
		englishContainer2.createString("content").setString("english2 content");
		englishContainer2.createString("name").setString("english2.html");
		node.createLink(node2);

		// TODO verify that link relation has been created
		// TODO render content and resolve links
	}

	@Test
	public void testMeshNodeStructure() {
		Node newsNode = data().getContent("news overview");
		assertNotNull(newsNode);
		Node newSubNode;
		newSubNode = newsNode.create(getUser(), getSchemaContainer(), getProject());

		assertEquals(1, newsNode.getChildren().size());
		Node firstChild = newsNode.getChildren().iterator().next();
		assertEquals(newSubNode.getUuid(), firstChild.getUuid());
	}

	@Test
	public void testTaggingOfMeshNode() {
		Node newsNode = data().getContent("news overview");
		assertNotNull(newsNode);

		Tag carTag = data().getTag("car");
		assertNotNull(carTag);

		newsNode.addTag(carTag);

		assertEquals(1, newsNode.getTags().size());
		Tag firstTag = newsNode.getTags().iterator().next();
		assertEquals(carTag.getUuid(), firstTag.getUuid());
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {

		List<String> languageTags = new ArrayList<>();
		languageTags.add("de");

		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = RoutingContextHelper.getUser(rc);
		Page<? extends Node> page = boot.nodeRoot().findAll(requestUser, languageTags, new PagingInfo(1, 10));

		// There are nodes that are only available in english
		assertEquals(data().getNodeCount(), page.getTotalElements());
		assertEquals(10, page.getSize());

		languageTags.add("en");
		page = boot.nodeRoot().findAll(requestUser, languageTags, new PagingInfo(1, 15));
		assertEquals(data().getNodeCount(), page.getTotalElements());
		assertEquals(15, page.getSize());

	}

	@Test
	public void testMeshNodeFields() {
		Node newsNode = data().getContent("news overview");
		Language german = data().getGerman();
		NodeFieldContainer germanFields = newsNode.getOrCreateFieldContainer(german);

		// TODO add some fields

	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		List<String> languageTags = new ArrayList<>();
		languageTags.add("de");
		languageTags.add("en");
		Page<? extends Node> page = boot.nodeRoot().findAll(getRequestUser(), languageTags, new PagingInfo(1, 25));
		assertNotNull(page);

	}

	@Test
	@Override
	public void testRootNode() {
		Project project = data().getProject();
		BaseNode root = project.getBaseNode();
		assertNotNull(root);
	}

	@Test
	@Override
	@Ignore("nodes can not be located using the name")
	public void testFindByName() {

	}

	@Test
	@Override
	public void testFindByUUID() {
		Node newsNode = data().getContent("news overview");
		boot.nodeRoot().findByUuid(newsNode.getUuid(), rh -> {
			Node node = rh.result();
			assertNotNull(node);
			assertEquals(newsNode.getUuid(), node.getUuid());
		});
	}

	@Test
	@Override
	public void testTransformation() {
		RoutingContext rc = getMockedRoutingContext("lang=en");
		Node newsNode = data().getContent("porsche 911");
		newsNode.transformToRest(rc, rh -> {
			NodeResponse response = rh.result();
			assertNotNull(response);
			System.out.println(JsonUtil.toJson(response));
			//TODO assert for english fields
		});
	}

	@Test
	@Override
	public void testCreateDelete() {
		Node folder = getFolder();
		Node subNode = folder.create(getUser(), getSchemaContainer(), getProject());
		assertNotNull(subNode.getUuid());
		subNode.delete();
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		Node node = getFolder().create(getUser(), getSchemaContainer(), getProject());
		assertFalse(getUser().hasPermission(node, Permission.CREATE_PERM));
		getUser().addCRUDPermissionOnRole(getFolder(), Permission.CREATE_PERM, node);
		assertTrue(getUser().hasPermission(node, Permission.CREATE_PERM));
	}

	@Test
	@Override
	public void testRead() throws IOException {
		Node node = data().getFolder("2015");
		assertEquals("folder", node.getSchema().getName());
		assertTrue(node.getSchema().isContainer());
	}

	@Test
	@Override
	public void testCreate() {
		User user = data().getUserInfo().getUser();
		Node parentNode = data().getFolder("2015");
		Node node = parentNode.create(user, data().getSchemaContainer("content"), getProject());
		long ts = System.currentTimeMillis();
		node.setCreationTimestamp(ts);
		node.setLastEditedTimestamp(ts);
		Long editedTimestamp = node.getLastEditedTimestamp();
		assertNotNull(editedTimestamp);
		assertEquals(ts, editedTimestamp.longValue());
		Long creationTimeStamp = node.getCreationTimestamp();
		assertNotNull(creationTimeStamp);
		assertEquals(ts, creationTimeStamp.longValue());
		assertEquals(user, node.getCreator());
		assertEquals(user, node.getEditor());
		Language english = data().getEnglish();
		Language german = data().getGerman();

		NodeFieldContainer englishContainer = node.getOrCreateFieldContainer(english);
		englishContainer.createString("content").setString("english content");
		englishContainer.createString("name").setString("english.html");
		assertNotNull(node.getUuid());

		List<? extends FieldContainer> allProperties = node.getFieldContainers();
		assertNotNull(allProperties);
		assertEquals(1, allProperties.size());

		NodeFieldContainer germanContainer = node.getOrCreateFieldContainer(german);
		germanContainer.createString("content").setString("german content");
		assertEquals(2, node.getFieldContainers().size());

		NodeFieldContainer container = node.getFieldContainer(english);
		assertNotNull(container);
		String text = container.getString("content").getString();
		assertNotNull(text);
		assertEquals("english content", text);

	}

	@Test
	@Override
	public void testDelete() {
		Node node = getContent();
		String uuid = node.getUuid();
		getMeshRoot().getNodeRoot().findByUuid(uuid, rh -> {
			assertNotNull(rh.result());
		});
		node.delete();
		//TODO check for attached subnodes
		getMeshRoot().getNodeRoot().findByUuid(uuid, rh -> {
			assertNull(rh.result());
		});
	}

	@Test
	@Override
	public void testUpdate() {
		Node node = getContent();
		User newUser = getMeshRoot().getUserRoot().create("newUser");
		assertEquals(getUser().getUuid(), node.getCreator().getUuid());
		node.setCreator(newUser);
		assertEquals(newUser.getUuid(), node.getCreator().getUuid());
		//TODO update other fields
	}

	@Test
	@Override
	public void testReadPermission() {
		testPermission(Permission.READ_PERM, getContent());
	}

	@Test
	@Override
	public void testDeletePermission() {
		testPermission(Permission.DELETE_PERM, getContent());
	}

	@Test
	@Override
	public void testUpdatePermission() {
		testPermission(Permission.UPDATE_PERM, getContent());
	}

	@Test
	@Override
	public void testCreatePermission() {
		testPermission(Permission.CREATE_PERM, getContent());
	}

}