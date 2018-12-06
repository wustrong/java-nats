/*
 *   Copyright (c) 2013 Mike Heath.  All rights reserved.
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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decodes frames sent from the server.
 *
 * @author Mike Heath
 */
public class ServerFrameDecoder extends AbstractFrameDecoder<ServerFrame> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerFrameDecoder.class);

	// Regular expressions used for parsing server messages
	private static final Pattern MSG_PATTERN = Pattern.compile("^MSG\\s+(\\S+)\\s+(\\S+)\\s+((\\S+)[^\\S\\r\\n]+)?(\\d+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern OK_PATTERN = Pattern.compile("^\\+OK\\s*", Pattern.CASE_INSENSITIVE);
	private static final Pattern ERR_PATTERN = Pattern.compile("^-ERR\\s*('.+')?", Pattern.CASE_INSENSITIVE);
	private static final Pattern PING_PATTERN = Pattern.compile("^PING", Pattern.CASE_INSENSITIVE);
	private static final Pattern PONG_PATTERN = Pattern.compile("^PONG", Pattern.CASE_INSENSITIVE);
	private static final Pattern INFO_PATTERN = Pattern.compile("^INFO\\s+([^\\r\\n]+)", Pattern.CASE_INSENSITIVE);

	public ServerFrameDecoder() {
		super();
	}

	public ServerFrameDecoder(int maxMessageSize) {
		super(maxMessageSize);
	}

	@Override
	protected ServerFrame decodeCommand(ChannelHandlerContext context, String command, ByteBuf in) {
		LOGGER.trace("Decoding '{}'", command);

		Matcher matcher = MSG_PATTERN.matcher(command);
		if (matcher.matches()) {
			final String subject = matcher.group(1);
			final String id = matcher.group(2);
			final String replyTo = matcher.group(4);
			final int length = Integer.valueOf(matcher.group(5));
			if (length > getMaxMessageSize()) {
				throwTooLongFrameException(context);
			}
			final String body = in.toString(in.readerIndex(), length, CharsetUtil.UTF_8);
			in.skipBytes(length);
			in.skipBytes(ByteBufUtil.CRLF.length);
			return new ServerPublishFrame(id, subject, replyTo, body);
		}
		matcher = INFO_PATTERN.matcher(command);
		if (matcher.matches()) {
			return new ServerInfoFrame(matcher.group(1));
		}
		matcher = OK_PATTERN.matcher(command);
		if (matcher.matches()) {
			return ServerOkFrame.OK_MESSAGE;
		}
		matcher = ERR_PATTERN.matcher(command);
		if (matcher.matches()) {
			return new ServerErrorFrame(matcher.group(1));
		}
		if (PING_PATTERN.matcher(command).matches()) {
			return ServerPingFrame.PING;
		}
		if (PONG_PATTERN.matcher(command).matches()) {
			return ServerPongFrame.PONG;
		}
		throw new NatsDecodingException(command);
	}
}
