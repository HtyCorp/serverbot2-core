package io.mamish.serverbot2.appdaemon;

import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SessionSftpServer {

    private final static String SSH_KEY_ALGORITHM = "ssh-rsa";
    private final static int RSA_KEY_SIZE = 2048;

    private final String sessionUsername = "sb2";
    private final String sessionPassword = IDUtils.randomUUIDJoined();
    private final KeyPair sessionKeyPair = generateRsaKeyPair();
    private final String sessionKeyPairFingerprint = generateSshKeyFingerprint((RSAPublicKey)sessionKeyPair.getPublic());

    public SessionSftpServer() throws IOException {

        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(NetSecConfig.APP_INSTANCE_SFTP_PORT);
        sshd.setKeyPairProvider(unusedSessionContext -> Collections.singletonList(sessionKeyPair));
        sshd.setPasswordAuthenticator((user,pass,serverSession) -> user.equals(sessionUsername)
                && pass.equals(sessionPassword));

        SftpSubsystemFactory defaultSftpSubsystemFactory = new SftpSubsystemFactory();
        sshd.setSubsystemFactories(Collections.singletonList(defaultSftpSubsystemFactory));

        sshd.start();

    }

    public String getSessionUsername() {
        return sessionUsername;
    }

    public String getSessionPassword() {
        return sessionPassword;
    }

    public String getSessionKeyPairFingerprint() {
        return sessionKeyPairFingerprint;
    }

    private KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(RSA_KEY_SIZE);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to get keygen instance for mandatory algorithm!");
        }
    }

    /*
     * Useful IETF draft references:
     * SFTP URI format: https://tools.ietf.org/html/draft-ietf-secsh-scp-sftp-ssh-uri-01
     * Fingerprint encoding: https://tools.ietf.org/html/draft-ietf-secsh-fingerprint-00
     * SSH protocol (key algorithms): https://tools.ietf.org/html/draft-ietf-secsh-transport-16#page-11
     */

    private String generateSshKeyFingerprint(RSAPublicKey publicKey) {

        byte[] keyBlob = generateEncodedSshKeyBlob(publicKey);

        MessageDigest md5Digest;
        try {
            md5Digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to get digest instance for mandatory algorithm!");
        }

        byte[] md5DigestBytes = md5Digest.digest(keyBlob);

        // This isn't particularly efficient but it only happens once a session, so oh well.
        String hexFingerprint = IntStream.range(0, md5DigestBytes.length)
                .mapToObj(i -> String.format("%02x", md5DigestBytes[i]))
                .collect(Collectors.joining(":"));

        return SSH_KEY_ALGORITHM + ":" + hexFingerprint;

    }

    private byte[] generateEncodedSshKeyBlob(RSAPublicKey key) {

        byte[] algorithmHeaderBytes = SSH_KEY_ALGORITHM.getBytes(StandardCharsets.US_ASCII);
        byte[] modulusBytes = key.getModulus().toByteArray();
        byte[] exponentBytes = key.getPublicExponent().toByteArray();

        final int requiredBlobSizeBytes = 4 + algorithmHeaderBytes.length
                + 4 + modulusBytes.length
                + 4 + exponentBytes.length;
        ByteBuffer buf = ByteBuffer.allocate(requiredBlobSizeBytes).order(ByteOrder.BIG_ENDIAN);

        writeLengthAndBytes(buf, algorithmHeaderBytes);
        writeLengthAndBytes(buf, modulusBytes);
        writeLengthAndBytes(buf, exponentBytes);

        return buf.array();

    }

    private void writeLengthAndBytes(ByteBuffer dst, byte[] src) {
        dst.putInt(src.length);
        dst.put(src);
    }

}
