package in.ajay.common;

public class AuthResponseFrame {

    private final AuthFrameResponseCode responseCode;

    public AuthResponseFrame(
            AuthFrameResponseCode responseCode) {

        this.responseCode = responseCode;
    }

    public AuthFrameResponseCode getResponseCode() {
        return this.responseCode;
    }

    @Override
    public String toString() {
        return "AuthResponseFrame{" +
                ", responseCode=" + responseCode +
                '}';
    }
}