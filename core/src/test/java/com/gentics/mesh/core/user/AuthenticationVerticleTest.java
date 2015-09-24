package com.gentics.mesh.core.user;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.verticle.auth.AuthenticationVerticle;
import com.gentics.mesh.rest.MeshRestClient;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;

public class AuthenticationVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private AuthenticationVerticle authenticationVerticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(authenticationVerticle);
		return list;
	}

	// Read Tests

	@Test
	public void testRestClient() throws Exception {
		User user = user();
		String username = user.getUsername();
		String uuid = user.getUuid();

		MeshRestClient client = new MeshRestClient("localhost", getPort());
		client.setLogin(username, password());
		Future<GenericMessageResponse> future = client.login();
		latchFor(future);
		assertSuccess(future);
		assertNotNull(client.getCookie());

		GenericMessageResponse loginResponse = future.result();
		assertNotNull(loginResponse);
		assertEquals("OK", loginResponse.getMessage());

		Future<UserResponse> meResponse = client.me();
		latchFor(meResponse);
		UserResponse me = meResponse.result();
		assertFalse("The request failed.", meResponse.failed());

		assertNotNull(me);
		assertEquals(uuid, me.getUuid());

		Future<GenericMessageResponse> logoutFuture = client.logout();
		latchFor(logoutFuture);
		assertSuccess(logoutFuture);

		meResponse = client.me();
		latchFor(meResponse);
		expectMessage(meResponse, HttpResponseStatus.UNAUTHORIZED, "Unauthorized");
	}

}