package com.gentics.mesh.core.verticle.role;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.core.verticle.RoleVerticle;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class RoleVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private RoleVerticle rolesVerticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return rolesVerticle;
	}

	// Create tests

	@Test
	public void testCreateRole() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");
		request.setGroupUuid(group().getUuid());

		Future<RoleResponse> future = getClient().createRole(request);
		latchFor(future);
		assertSuccess(future);

		RoleResponse restRole = future.result();
		test.assertRole(request, restRole);
	}

	@Test
	public void testCreateRoleWithConflictingName() throws Exception {

		// Create first Role
		String name = "new_role";
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName(name);
		request.setGroupUuid(group().getUuid());

		Future<RoleResponse> future = getClient().createRole(request);
		latchFor(future);
		assertSuccess(future);

		future = getClient().createRole(request);
		latchFor(future);
		expectException(future, CONFLICT, "role_conflicting_name");
	}

	@Test
	public void testCreateDeleteRole() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");
		request.setGroupUuid(group().getUuid());

		Future<RoleResponse> createFuture = getClient().createRole(request);
		latchFor(createFuture);
		assertSuccess(createFuture);

		RoleResponse restRole = createFuture.result();
		test.assertRole(request, restRole);

		Future<GenericMessageResponse> deleteFuture = getClient().deleteRole(restRole.getUuid());
		latchFor(deleteFuture);
		assertSuccess(deleteFuture);
		expectMessageResponse("role_deleted", deleteFuture, restRole.getUuid() + "/" + restRole.getName());

	}

	@Test
	public void testCreateRoleWithNoPermissionOnGroup() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");
		request.setGroupUuid(group().getUuid());

		// Add needed permission to group
		try (Trx tx = new Trx(db)) {
			role().revokePermissions(group(), CREATE_PERM);
			tx.success();
		}

		Future<RoleResponse> future = getClient().createRole(request);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", group().getUuid());
	}

	@Test
	@Ignore("We can't test this using the rest client")
	public void testCreateRoleWithBogusJson() throws Exception {
		// String requestJson = "bogus text";
		// String response = request(info, POST, "/api/v1/roles/", 400, "Bad Request", requestJson);
		// expectMessageResponse("error_parse_request_json_error", response);
	}

	@Test
	public void testCreateRoleWithNoGroupId() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");
		Future<RoleResponse> future = getClient().createRole(request);
		latchFor(future);
		expectException(future, BAD_REQUEST, "role_missing_parentgroup_field");

	}

	@Test
	public void testCreateRoleWithNoName() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setGroupUuid(group().getUuid());

		Future<RoleResponse> future = getClient().createRole(request);
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_name_must_be_set");
	}

	// Read tests

	@Test
	public void testReadOwnRoleByUUID() throws Exception {
		String uuid;
		try (Trx tx = new Trx(db)) {
			Role role = role();
			uuid = role.getUuid();
			assertNotNull("The UUID of the role must not be null.", role.getUuid());
		}

		Future<RoleResponse> future = getClient().findRoleByUuid(uuid);
		latchFor(future);
		assertSuccess(future);
		RoleResponse restRole = future.result();
		try (Trx tx = new Trx(db)) {
			test.assertRole(role(), restRole);
		}
	}

	@Test
	public void testReadExtraRoleByUUID() throws Exception {
		Role extraRole;
		try (Trx tx = new Trx(db)) {
			RoleRoot roleRoot = meshRoot().getRoleRoot();
			extraRole = roleRoot.create("extra role", group(), user());
			assertNotNull("The UUID of the role must not be null.", extraRole.getUuid());
			role().grantPermissions(extraRole, READ_PERM);
			tx.success();
		}

		Future<RoleResponse> future = getClient().findRoleByUuid(extraRole.getUuid());
		latchFor(future);
		assertSuccess(future);
		RoleResponse restRole = future.result();
		try (Trx tx = new Trx(db)) {
			test.assertRole(extraRole, restRole);
		}

	}

	@Test
	public void testReadExtraRoleByUUIDWithMissingPermission() throws Exception {
		Role extraRole;
		try (Trx tx = new Trx(db)) {
			RoleRoot roleRoot = meshRoot().getRoleRoot();
			extraRole = roleRoot.create("extra role", group(), user());
			// Revoke read permission from the role
			role().revokePermissions(extraRole, READ_PERM);
			tx.success();
		}

		assertNotNull("The UUID of the role must not be null.", extraRole.getUuid());

		Future<RoleResponse> future = getClient().findRoleByUuid(extraRole.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", extraRole.getUuid());

	}

	@Test
	public void testReadOwnRoleByUUIDWithMissingPermission() throws Exception {
		String uuid;
		try (Trx tx = new Trx(db)) {
			Role role = role();
			uuid = role.getUuid();
			assertNotNull("The UUID of the role must not be null.", role.getUuid());
			role.revokePermissions(role, READ_PERM);
			tx.success();
		}
		Future<RoleResponse> future = getClient().findRoleByUuid(uuid);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", uuid);
	}

	@Test
	public void testReadRoles() throws Exception {

		final int nRoles = 21;
		String noPermRoleName;

		try (Trx tx = new Trx(db)) {
			RoleRoot roleRoot = meshRoot().getRoleRoot();
			Role noPermRole = roleRoot.create("no_perm_role", null, user());

			role().grantPermissions(group(), READ_PERM);

			// Create and save some roles
			for (int i = 0; i < nRoles; i++) {
				Role extraRole = roleRoot.create("extra role " + i, group(), user());
				role().grantPermissions(extraRole, READ_PERM);
			}
			// Role with no permission
			group().addRole(noPermRole);

			noPermRoleName = noPermRole.getName();
			tx.success();
		}

		// Test default paging parameters
		Future<RoleListResponse> future = getClient().findRoles();
		latchFor(future);
		assertSuccess(future);
		RoleListResponse restResponse = future.result();
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(25, restResponse.getData().size());

		int perPage = 11;
		int page = 1;
		future = getClient().findRoles(new PagingInfo(page, perPage));
		latchFor(future);
		assertSuccess(future);
		restResponse = future.result();
		assertEquals("The amount of items for page {" + page + "} does not match the expected amount.", 11, restResponse.getData().size());

		// created roles + test data role
		// TODO fix this assertion. Actually we would need to add 1 since the own role must also be included in the list
		int totalRoles = nRoles + roles().size();
		int totalPages = (int) Math.ceil(totalRoles / (double) perPage);
		assertEquals("The response did not contain the correct amount of items", perPage, restResponse.getData().size());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals("The total pages could does not match. We expect {" + totalRoles + "} total roles and {" + perPage
				+ "} roles per page. Thus we expect {" + totalPages + "} pages", totalPages, restResponse.getMetainfo().getPageCount());
		assertEquals(perPage, restResponse.getMetainfo().getPerPage());
		for (RoleResponse role : restResponse.getData()) {
			System.out.println(role.getName());
		}
		assertEquals(totalRoles, restResponse.getMetainfo().getTotalCount());

		List<RoleResponse> allRoles = new ArrayList<>();
		for (page = 1; page <= totalPages; page++) {
			Future<RoleListResponse> pageFuture = getClient().findRoles(new PagingInfo(page, perPage));
			latchFor(pageFuture);
			assertSuccess(pageFuture);
			restResponse = pageFuture.result();
			allRoles.addAll(restResponse.getData());
		}
		assertEquals("Somehow not all roles were loaded when loading all pages.", totalRoles, allRoles.size());

		// Verify that extra role is not part of the response
		List<RoleResponse> filteredUserList = allRoles.parallelStream().filter(restRole -> restRole.getName().equals(noPermRoleName))
				.collect(Collectors.toList());
		assertTrue("Extra role should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

		future = getClient().findRoles(new PagingInfo(-1, perPage));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_invalid_paging_parameters");

		future = getClient().findRoles(new PagingInfo(1, 0));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_invalid_paging_parameters");

		future = getClient().findRoles(new PagingInfo(1, -1));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_invalid_paging_parameters");

		future = getClient().findRoles(new PagingInfo(4242, 25));
		latchFor(future);
		assertSuccess(future);

		assertEquals(0, future.result().getData().size());
		assertEquals(4242, future.result().getMetainfo().getCurrentPage());
		assertEquals(2, future.result().getMetainfo().getPageCount());
		assertEquals(36, future.result().getMetainfo().getTotalCount());
		assertEquals(25, future.result().getMetainfo().getPerPage());
	}

	// Update tests

	@Test
	public void testUpdateRole() throws JsonGenerationException, JsonMappingException, IOException, Exception {

		String roleUuid;
		try (Trx tx = new Trx(db)) {
			RoleRoot roleRoot = meshRoot().getRoleRoot();
			Role extraRole = roleRoot.create("extra role", group(), user());
			roleUuid = extraRole.getUuid();
			role().grantPermissions(extraRole, UPDATE_PERM);
			tx.success();
		}
		RoleUpdateRequest request = new RoleUpdateRequest();
		request.setName("renamed role");

		Future<RoleResponse> future = getClient().updateRole(roleUuid, request);
		latchFor(future);
		assertSuccess(future);
		RoleResponse restRole = future.result();
		assertEquals(request.getName(), restRole.getName());
		assertEquals(roleUuid, restRole.getUuid());

		// Check that the extra role was updated as expected
		try (Trx tx = new Trx(db)) {
			RoleRoot roleRoot = meshRoot().getRoleRoot();
			roleRoot.findByUuid(roleUuid, rh -> {
				Role reloadedRole = rh.result();
				assertEquals("The role should have been renamed", request.getName(), reloadedRole.getName());
			});
		}
	}

	@Test
	public void testUpdateOwnRole() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		String uuid;
		try (Trx tx = new Trx(db)) {
			Role role = role();
			uuid = role.getUuid();
		}

		RoleUpdateRequest restRole = new RoleUpdateRequest();
		restRole.setName("renamed role");

		Future<RoleResponse> future = getClient().updateRole(uuid, restRole);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", uuid);

		// Add the missing permission and try again
		try (Trx tx = new Trx(db)) {
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);
			tx.success();
		}

		future = getClient().updateRole(uuid, restRole);
		latchFor(future);
		assertSuccess(future);

		// Check that the role was updated
		try (Trx tx = new Trx(db)) {
			CountDownLatch latch = new CountDownLatch(1);
			boot.roleRoot().findByUuid(uuid, rh -> {
				Role reloadedRole = rh.result();
				assertEquals(restRole.getName(), reloadedRole.getName());
				latch.countDown();
			});
			failingLatch(latch);
			;
		}

	}

	// Delete tests

	@Test
	public void testDeleteRoleByUUID() throws Exception {
		String roleUuid;
		String roleName;
		try (Trx tx = new Trx(db)) {
			RoleRoot roleRoot = meshRoot().getRoleRoot();
			Role extraRole = roleRoot.create("extra role", group(), user());
			roleUuid = extraRole.getUuid();
			roleName = extraRole.getName();
			role().grantPermissions(extraRole, DELETE_PERM);
			tx.success();
		}

		Future<GenericMessageResponse> future = getClient().deleteRole(roleUuid);
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("role_deleted", future, roleUuid + "/" + roleName);

		try (Trx tx = new Trx(db)) {
			CountDownLatch latch = new CountDownLatch(1);
			RoleRoot roleRoot = meshRoot().getRoleRoot();
			roleRoot.findByUuid(roleUuid, rh -> {
				assertNull("The user should have been deleted", rh.result());
				latch.countDown();
			});
			failingLatch(latch);
		}
	}

	@Test
	public void testDeleteRoleByUUIDWithMissingPermission() throws Exception {
		Future<GenericMessageResponse> future = getClient().deleteRole(role().getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", role().getUuid());

		try (Trx tx = new Trx(db)) {
			CountDownLatch latch = new CountDownLatch(1);
			boot.roleRoot().findByUuid(role().getUuid(), rh -> {
				assertNotNull("The role should not have been deleted", rh.result());
				latch.countDown();
			});
			failingLatch(latch);
		}
	}

}
