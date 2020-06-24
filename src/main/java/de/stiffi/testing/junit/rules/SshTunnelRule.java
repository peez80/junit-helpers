package de.stiffi.testing.junit.rules;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import de.stiffi.testing.junit.helpers.SocketHelper;
import org.junit.rules.ExternalResource;

import java.io.File;

public class SshTunnelRule extends ExternalResource {

    private int localPort;

    private String sshHost;
    private String sshUser;
    private String sshPassword;
    private int sshPort = 22;
    private String privateKeyPath;
    private String privateKeyPassphrase;
    private String remoteHost;
    private int remotePort;

    private Session session;

    public static SshTunnelRule newSshTunnelRule(String sshHost, String sshUser, String remoteHost, int remotePort) {
        SshTunnelRule me = new SshTunnelRule();
        me.withSshHost(sshHost);
        me.withSshUser(sshUser);
        me.withRemoteHost(remoteHost, remotePort);
        return me;
    }

    public SshTunnelRule withSshPort(int sshPort) {
        this.sshPort = sshPort;
        return this;
    }

    public SshTunnelRule withSshHost(String sshHost) {
        this.sshHost = sshHost;
        return this;
    }

    public SshTunnelRule withSshUser(String sshUser) {
        this.sshUser = sshUser;
        return this;
    }
    public SshTunnelRule withSshPassword(String password) {
        this.sshPassword = password;
        return this;
    }

    public SshTunnelRule withPrivateKeyAuth(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
        return this;
    }

    public SshTunnelRule withPrivateKeyPassphrase(String privateKeyPassphrase) {
        this.privateKeyPassphrase = privateKeyPassphrase;
        return this;
    }

    public SshTunnelRule withRemoteHost(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        return this;
    }

    /**
     * Default ~/.ssh/id_rsa
     * @return
     */
    public SshTunnelRule withPrivateKeyAuth() {
        String path = findDefaultPrivateKey();
        return withPrivateKeyAuth(path);
    }


    private SshTunnelRule(){}

    @Override
    protected void before() throws Throwable {
        startTunnel();
    }


    @Override
    protected void after() {
        stopTunnel();
    }

    public void startTunnel() throws JSchException {
        int port = SocketHelper.findFreePort();

        JSch jsch = new JSch();
        if (privateKeyPath != null) {
            if (privateKeyPassphrase != null) {
                jsch.addIdentity(privateKeyPath, privateKeyPassphrase);
            }else{
                jsch.addIdentity(privateKeyPath);
            }
        }


        session = jsch.getSession(sshUser, sshHost, sshPort);
        if (sshPassword != null) {
            session.setPassword(sshPassword);
        }

        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
        session.setConfig(config);

        session.connect(3000);
        localPort = session.setPortForwardingL(port, remoteHost, remotePort);

        System.out.println("Started SSH Tunnel to " + remoteHost+":"+remotePort + " on local Port " + localPort);

    }

    public void stopTunnel() {
        if (session != null) {
            session.disconnect();
            System.out.println("Stopped SSH Tunnel to " + remoteHost);
        }
    }

    public int getLocalPort() {
        return localPort;
    }


    private String findDefaultPrivateKey() {
        try {
            File f = new File("~/.ssh/id_rsa");
            if (f.exists()) {
                return f.getPath();
            }
        }catch(Exception e) {
            //Do nothing
        }

        try {
            File f = new File(System.getProperty("user.home") + "/.ssh/id_rsa");
            if (f.exists()) {
                return f.getPath();
            }
        }catch(Exception e) {
            //Do nothing
        }

        return null;
    }
}
