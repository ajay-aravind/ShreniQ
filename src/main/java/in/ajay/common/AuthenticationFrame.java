package in.ajay.common;

import java.util.UUID;

public class AuthenticationFrame {
    private String username;
    private String password;
    private ClientRole role;
    private UUID clientId;
    private UUID publisherSubscriberId;

    public AuthenticationFrame(String username, String password, ClientRole role, UUID clientId, UUID publisherSubscriberId) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.clientId = clientId;
        this.publisherSubscriberId = publisherSubscriberId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public ClientRole getRole() {
        return role;
    }

    public UUID getClientId() {
        return clientId;
    }

    public UUID getPublisherSubscriberId() {
        return publisherSubscriberId;
    }

    @Override
    public String toString() {
        return "AuthenticationFrame{" +
                "username='" + username + '\'' +
                ", role=" + role +
                ", clientId=" + clientId +
                ", publisherSubscriberId=" + publisherSubscriberId +
                ", password='" + '\'' +
                '}';
    }
}
