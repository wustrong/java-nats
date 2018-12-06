/*
 *   Copyright (c) 2012 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package nats.codec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import nats.NatsException;

import java.io.IOException;

/**
 * @author Mike Heath
 */
public class ConnectBody {

	private final String user;
	private final String password;
	private final boolean pedantic;
	private final boolean verbose;

	@JsonCreator
	public ConnectBody(
			@JsonProperty("user")
			String user,
			@JsonProperty("pass")
			String password,
			@JsonProperty("pedantic")
			boolean pedantic,
			@JsonProperty("verbose")
			boolean verbose) {
		this.user = user;
		this.password = password;
		this.pedantic = pedantic;
		this.verbose = verbose;
	}

	@JsonProperty("pass")
	public String getPassword() {
		return password;
	}

	public boolean isPedantic() {
		return pedantic;
	}

	public String getUser() {
		return user;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public static ConnectBody parse(String body) {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			return mapper.readValue(body, ConnectBody.class);
		} catch (IOException e) {
			throw new NatsException(e);
		}
	}

}
