package in.ajay.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AuthenticationResponseFrameParser {

        private static final int FRAME_LENGTH = 3;

        // Replace with your protocol's assigned EOF marker.
        private static final byte EOF_MARKER = 0x7F;

        /**
         * Converts AuthResponseFrame object into binary format.
         */
        public static byte[] encode(AuthResponseFrame frame) {

            ByteBuffer buffer = ByteBuffer.allocate(FRAME_LENGTH);

            buffer.order(ByteOrder.BIG_ENDIAN);

            buffer.putInt(FRAME_LENGTH);
            buffer.put(FrameType.AUTHENTICATION_RESPONSE.code());
            buffer.put(frame.getResponseCode().getCode());
            buffer.put(EOF_MARKER);

            return buffer.array();
        }

        /**
         * Decodes a binary byte array into AuthResponseFrame.
         */
        public static AuthResponseFrame decode(byte[] data) throws IOException {

            if (data == null || data.length < FRAME_LENGTH) {
                throw new IllegalArgumentException("Incomplete frame");
            }

            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.BIG_ENDIAN);

            byte frameTypeCode = buffer.get();

            if(frameTypeCode != FrameType.AUTHENTICATION_RESPONSE.code()){
                throw new IllegalArgumentException(
                        "Expected Authentication response frame type, but found: " +
                                frameTypeCode);
            }

            byte responseCode = buffer.get();
            byte receivedEof = buffer.get();

            if (receivedEof != EOF_MARKER) {
                throw new IllegalArgumentException(
                        "Invalid EOF marker: " +
                                Byte.toUnsignedInt(receivedEof));
            }

            AuthFrameResponseCode code = AuthFrameResponseCode.fromCode(responseCode);

            return new AuthResponseFrame(code);
        }
    }
