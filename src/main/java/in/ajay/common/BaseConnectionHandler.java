package in.ajay.common;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BaseConnectionHandler {
    public int readFrameLength(InputStream input) throws IOException {
        byte[] lengthBytes = new byte[4];
        readFully(input, lengthBytes, 0, 4);

        return ByteBuffer.wrap(lengthBytes)
                .order(ByteOrder.BIG_ENDIAN)
                .getInt();
    }

    public  byte[] readRemainingFrame(
            InputStream input,
            int frameLength) throws IOException {

        byte[] frame = new byte[frameLength];

        ByteBuffer.wrap(frame)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(frameLength);

        readFully(input, frame, 4, frameLength - 4);

        return frame;
    }
    /**
     * Helper to read the requested number of bytes.
     */
    protected void readFully(
            InputStream input,
            byte[] buffer) throws IOException {

        readFully(input, buffer, 0, buffer.length);
    }

    protected void readFully(
            InputStream input,
            byte[] buffer,
            int offset,
            int length) throws IOException {

        int totalRead = 0;

        while (totalRead < length) {

            int bytesRead = input.read(
                    buffer,
                    offset + totalRead,
                    length - totalRead);

            if (bytesRead == -1) {
                throw new EOFException(
                        "Unexpected end of stream");
            }

            totalRead += bytesRead;
        }
    }
}
