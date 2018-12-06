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
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.CharsetUtil;
import nats.Constants;

import java.util.List;

/**
 * @author Mike Heath
 */
abstract class AbstractFrameDecoder<T extends NatsFrame> extends ReplayingDecoder<Object> {

	private final int maxMessageSize;

	protected AbstractFrameDecoder() {
		this(Constants.DEFAULT_MAX_FRAME_SIZE);
	}

	protected AbstractFrameDecoder(int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
	}

	@Override
	protected void decode(ChannelHandlerContext context, ByteBuf in, List<Object> out) throws Exception {
		int frameLength = indexOf(in, ByteBufUtil.CRLF);
		if (frameLength >= 0) {
			if (frameLength > maxMessageSize) {
				in.skipBytes(frameLength + ByteBufUtil.CRLF.length);
				throwTooLongFrameException(context);
			} else {
				String command = in.toString(in.readerIndex(), frameLength, CharsetUtil.UTF_8);
				in.skipBytes(ByteBufUtil.CRLF.length + frameLength);
				final T decodedCommand = decodeCommand(context, command, in);
				out.add(decodedCommand);
			}
		} else {
			// Trigger a read exception on the incoming byte buffer to keep the ReplayingDecoder happy. Instead of
			// searching for CRLF, we should read all the bytes into a buffer so that the reading triggers this exception.
			in.readBytes(Integer.MAX_VALUE);
		}
	}

	protected int getMaxMessageSize() {
		return maxMessageSize;
	}

	protected abstract T decodeCommand(ChannelHandlerContext context, String command, ByteBuf in);

	protected void throwTooLongFrameException(ChannelHandlerContext ctx) {
		ctx.fireExceptionCaught(new TooLongFrameException("message size exceeds " + maxMessageSize));
	}

	/**
	 * Returns the number of bytes between the readerIndex of the haystack and
	 * the first needle found in the haystack.  -1 is returned if no needle is
	 * found in the haystack.
	 * <p/>
	 * Copied from {@link io.netty.handler.codec.DelimiterBasedFrameDecoder}.
	 */
	private int indexOf(ByteBuf haystack, byte[] needle) {
		for (int i = haystack.readerIndex(); i < haystack.writerIndex(); i++) {
			int haystackIndex = i;
			int needleIndex;
			for (needleIndex = 0; needleIndex < needle.length; needleIndex++) {
				if (haystack.getByte(haystackIndex) != needle[needleIndex]) {
					break;
				} else {
					haystackIndex++;
					if (haystackIndex == haystack.writerIndex() &&
							needleIndex != needle.length - 1) {
						return -1;
					}
				}
			}

			if (needleIndex == needle.length) {
				// Found the needle from the haystack!
				return i - haystack.readerIndex();
			}
		}
		return -1;
	}

}
