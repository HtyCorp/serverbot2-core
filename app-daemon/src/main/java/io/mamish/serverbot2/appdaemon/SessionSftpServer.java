package io.mamish.serverbot2.appdaemon;

import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.sharedutil.ExceptionUtils;
import io.mamish.serverbot2.sharedutil.IDUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SessionSftpServer {

    // Key pair components are separately encoded and saved together in a ZIP file
    private final static File SSH_KEY_LOCAL_PATH = new File("/opt/serverbot2/daemon/sftp_ssh_keypair.bin");
    private final static String SSH_PUBLIC_KEY_ZIP_ENTRY_NAME = "public.bin";
    private final static String SSH_PRIVATE_KEY_ZIP_ENTRY_NAME = "private.bin";

    private final static String SSH_KEY_ALGORITHM = "ssh-rsa";
    private final static int RSA_KEY_SIZE = 2048;

    private final static String SFTP_USERNAME = "files";

    private final String sessionPassword;
    private final KeyPair sessionKeyPair;
    private final String sessionKeyPairFingerprint;

    public SessionSftpServer() throws IOException {

        sessionPassword = IDUtils.randomUUIDJoined();
        sessionKeyPair = getOrGenerateRsaKeyPair();
        sessionKeyPairFingerprint = generateSshKeyFingerprint((RSAPublicKey)sessionKeyPair.getPublic());

        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(NetSecConfig.APP_INSTANCE_SFTP_PORT);
        sshd.setKeyPairProvider(unusedSessionContext -> Collections.singletonList(sessionKeyPair));
        sshd.setPasswordAuthenticator((user,pass,session) -> user.equals(SFTP_USERNAME) && pass.equals(sessionPassword));

        SftpSubsystemFactory defaultSftpSubsystemFactory = new SftpSubsystemFactory();
        sshd.setSubsystemFactories(Collections.singletonList(defaultSftpSubsystemFactory));

        sshd.start();

    }

    public String getSessionUsername() {
        return SFTP_USERNAME;
    }

    public String getSessionPassword() {
        return sessionPassword;
    }

    public String getSessionKeyPairFingerprint() {
        return sessionKeyPairFingerprint;
    }

    private KeyPair getOrGenerateRsaKeyPair() {

        KeyPairGenerator rsaGenerator = ExceptionUtils.cantFail(() -> KeyPairGenerator.getInstance("RSA"));
        KeyFactory rsaFactory = ExceptionUtils.cantFail(() -> KeyFactory.getInstance("RSA"));

        // See if there's a saved key pair we can use
        Optional<KeyPair> maybeCachedKeyPair = readKeyPairFromFiles(rsaFactory);
        if (maybeCachedKeyPair.isPresent()) {
            return maybeCachedKeyPair.get();
        }

        // If not: generate a new one, save it and use it now
        KeyPair newKeyPair = generateNewRsaKeyPair(rsaGenerator);
        writeKeyPairToFiles(newKeyPair);
        return newKeyPair;

    }

    private Optional<KeyPair> readKeyPairFromFiles(KeyFactory rsaFactory) {

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(SSH_KEY_LOCAL_PATH))) {

            PublicKey publicKey = null;
            PrivateKey privateKey = null;

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                byte[] encodedKeyBytes = zis.readAllBytes();
                // Per https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/security/Key.html#getFormat(),
                // public keys uses X509 encoding while private keys using PKCS8.
                if (entry.getName().equals(SSH_PUBLIC_KEY_ZIP_ENTRY_NAME)) {
                    publicKey = rsaFactory.generatePublic(new X509EncodedKeySpec(encodedKeyBytes));
                } else if (entry.getName().equals(SSH_PRIVATE_KEY_ZIP_ENTRY_NAME)) {
                    privateKey = rsaFactory.generatePrivate(new PKCS8EncodedKeySpec(encodedKeyBytes));
                } else {
                    throw new IllegalStateException("Unexpected zip entry '"+entry.getName()+"'");
                }
            }

            if (publicKey == null || privateKey == null) {
                throw new IllegalStateException("Missing key entries from zip file");
            }

            return Optional.of(new KeyPair(publicKey, privateKey));

        } catch (FileNotFoundException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException("IOException while reading zip file:" + e.getMessage(), e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Encoded keyspec for SSH keypair component is invalid", e);
        }
    }

    private KeyPair generateNewRsaKeyPair(KeyPairGenerator rsaGenerator) {
        rsaGenerator.initialize(RSA_KEY_SIZE);
        return rsaGenerator.generateKeyPair();
    }

    private void writeKeyPairToFiles(KeyPair keyPair) {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(SSH_KEY_LOCAL_PATH))) {
            zos.putNextEntry(new ZipEntry(SSH_PUBLIC_KEY_ZIP_ENTRY_NAME));
            zos.write(keyPair.getPublic().getEncoded());
            zos.putNextEntry(new ZipEntry(SSH_PRIVATE_KEY_ZIP_ENTRY_NAME));
            zos.write(keyPair.getPrivate().getEncoded());
            zos.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write zipped SSH key file");
        }
    }

    /*
     * Useful IETF draft references:
     * SFTP URI format: https://tools.ietf.org/html/draft-ietf-secsh-scp-sftp-ssh-uri-01
     * Fingerprint encoding: https://tools.ietf.org/html/draft-ietf-secsh-fingerprint-00
     * SSH protocol (key algorithms): https://tools.ietf.org/html/draft-ietf-secsh-transport-16#page-11
     * SSH protocol arch (encodings including 'mpint'): https://www.ietf.org/rfc/rfc4251.txt
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
        byte[] exponentBytes = key.getPublicExponent().toByteArray();
        byte[] modulusBytes = key.getModulus().toByteArray();

        final int requiredBlobSizeBytes = 4 + algorithmHeaderBytes.length
                + 4 + exponentBytes.length
                + 4 + modulusBytes.length;

        ByteBuffer buf = ByteBuffer.allocate(requiredBlobSizeBytes).order(ByteOrder.BIG_ENDIAN);

        writeLengthAndBytes(buf, algorithmHeaderBytes);
        writeLengthAndBytes(buf, exponentBytes);
        writeLengthAndBytes(buf, modulusBytes);

        return buf.array();

    }

    private void writeLengthAndBytes(ByteBuffer dst, byte[] src) {
        dst.putInt(src.length);
        dst.put(src);
    }

}
