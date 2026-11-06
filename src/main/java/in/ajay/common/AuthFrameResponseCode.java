package in.ajay.common;

public enum AuthFrameResponseCode {
        AUTH_SUCCESSFUL(1),
        AUTH_FAILED(2),
        CLIENT_ID_NOT_ALLOWED(3),
        UNEXPECTED_FRAME(4);

        private final byte code;

    AuthFrameResponseCode(int code) {
            this.code = (byte) code;
        }

        public byte getCode() {
            return code;
        }

        public static AuthFrameResponseCode fromCode(byte code) {

            for (AuthFrameResponseCode response : values()) {
                if (response.code == code) {
                    return response;
                }
            }

            throw new IllegalArgumentException(
                    "Unknown response code: " + Byte.toUnsignedInt(code));
        }
}
