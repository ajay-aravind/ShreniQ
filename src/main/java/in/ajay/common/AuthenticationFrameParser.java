package in.ajay.common;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class AuthenticationFrameParser {

        private static final int MIN_FRAME_LENGTH = 43;

        // Replace with your protocol's assigned EOF marker.
        private static final byte EOF_MARKER = 0x7F;
        private static final int MAX_FRAME_LENGTH = 1024 * 1024;

        private AuthenticationFrameParser() {
        }

        public static AuthenticationFrame parse(byte[] data)
                throws IOException {

            if (data == null) {
                throw new IllegalArgumentException("Frame cannot be null");
            }

            ByteBuffer buffer = ByteBuffer.wrap(data)
                    .order(ByteOrder.BIG_ENDIAN);

            // 4 bytes: frame length
            if (buffer.remaining() < 4) {
                throw new ProtocolException("Incomplete frame length");
            }

            int frameLength = buffer.getInt();

            if (frameLength < MIN_FRAME_LENGTH) {
                throw new ProtocolException("Invalid frame length: " + frameLength);
            }

            if (frameLength != data.length) {
                throw new ProtocolException(
                        "Frame length mismatch. Expected: "
                                + frameLength + ", received: " + data.length);
            }

            // 1 byte: frame type
            byte frameTypeCode = buffer.get();

            FrameType frameType;

            try {
                frameType =
                        FrameType.fromCode(frameTypeCode);
            } catch (IllegalArgumentException e) {
                throw new ProtocolException(e.getMessage());
            }

            // 2 bytes + N bytes: username
            String username = readString(buffer);

            // 2 bytes + N bytes: password
            String password = readString(buffer);

            // 1 byte: publisher/subscriber
            int roleCode = Byte.toUnsignedInt(buffer.get());

            ClientRole role;

            try {
                role = ClientRole.fromCode(roleCode);
            } catch (IllegalArgumentException e) {
                throw new ProtocolException(e.getMessage());
            }

            // 16 bytes: client UUID
            UUID clientId = readUuid(buffer);

            // 16 bytes: publisher/subscriber UUID
            UUID publisherSubscriberId = readUuid(buffer);

            // 1 byte: EOF marker
            if (buffer.remaining() != 1) {
                throw new ProtocolException(
                        "Unexpected trailing bytes in authentication frame");
            }

            byte eof = buffer.get();

            if (eof != EOF_MARKER) {
                throw new ProtocolException(
                        "Invalid EOF marker: " + Byte.toUnsignedInt(eof));
            }

            return new AuthenticationFrame(
                    username,
                    password,
                    role,
                    clientId,
                    publisherSubscriberId
            );
        }

        private static UUID readUuid(ByteBuffer buffer)
                throws IOException {

            if (buffer.remaining() < 16) {
                throw new ProtocolException("Incomplete UUID");
            }

            long mostSignificantBits = buffer.getLong();
            long leastSignificantBits = buffer.getLong();

            return new UUID(
                    mostSignificantBits,
                    leastSignificantBits
            );
        }

    // ==================== ENCODING ====================

    public static byte[] encode(AuthenticationFrame frame)
            throws IOException {

        if (frame == null) {
            throw new IllegalArgumentException("Frame cannot be null");
        }

        ByteArrayOutputStream bodyBytes = new ByteArrayOutputStream();

        try (DataOutputStream body =
                     new DataOutputStream(bodyBytes)) {

            // 1 byte: frame type
            body.writeByte(FrameType.AUTHENTICATION.code());

            // 2 bytes + N bytes: username
            writeString(body, frame.getUsername());

            // 2 bytes + N bytes: password
            writeString(body, frame.getPassword());

            // 1 byte: publisher/subscriber
            body.writeByte(frame.getRole().code());

            // 16 bytes: client UUID
            writeUuid(body, frame.getClientId());

            // 16 bytes: publisher/subscriber UUID
            writeUuid(body, frame.getPublisherSubscriberId());

            // 1 byte: EOF marker
            body.writeByte(EOF_MARKER);
        }

        byte[] body = bodyBytes.toByteArray();

        // Include the 4-byte length field.
        int frameLength = 4 + body.length;

        if (frameLength > MAX_FRAME_LENGTH) {
            throw new ProtocolException(
                    "Frame exceeds maximum allowed size: " + frameLength);
        }

        ByteBuffer buffer = ByteBuffer.allocate(frameLength)
                .order(ByteOrder.BIG_ENDIAN);

        buffer.putInt(frameLength);
        buffer.put(body);

        return buffer.array();
    }

    // ==================== STREAM WRITING ====================

    public static void write(
            AuthenticationFrame frame,
            OutputStream outputStream) throws IOException {

        if (outputStream == null) {
            throw new IllegalArgumentException(
                    "OutputStream cannot be null");
        }

        byte[] encodedFrame = encode(frame);

        outputStream.write(encodedFrame);
    }

    // ==================== HELPERS ====================

    private static void writeString(
            DataOutputStream output,
            String value) throws IOException {

        if (value == null) {
            throw new IllegalArgumentException(
                    "String fields cannot be null");
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        if (bytes.length > 65535) {
            throw new ProtocolException(
                    "String exceeds maximum length of 65535 bytes");
        }

        output.writeShort(bytes.length);
        output.write(bytes);
    }

    private static void writeUuid(
            DataOutputStream output,
            UUID uuid) throws IOException {

        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }

        output.writeLong(uuid.getMostSignificantBits());
        output.writeLong(uuid.getLeastSignificantBits());
    }

    private static String readString(ByteBuffer buffer)
            throws IOException {

        if (buffer.remaining() < 2) {
            throw new ProtocolException("Missing string length");
        }

        int length = Short.toUnsignedInt(buffer.getShort());

        if (buffer.remaining() < length) {
            throw new ProtocolException("Incomplete string data");
        }

        ByteBuffer stringBuffer = buffer.slice();
        stringBuffer.limit(length);

        buffer.position(buffer.position() + length);

        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(stringBuffer)
                    .toString();
        } catch (CharacterCodingException e) {
            throw new ProtocolException("Invalid UTF-8 string");
        }
    }
}
