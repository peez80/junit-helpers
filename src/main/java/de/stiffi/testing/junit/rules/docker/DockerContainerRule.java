package de.stiffi.testing.junit.rules.docker;

import de.stiffi.testing.junit.helpers.ProcessHelper;
import de.stiffi.testing.junit.helpers.SocketHelper;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.util.*;

public class DockerContainerRule extends ExternalResource {

    private String dockerImage;
    private String containerName;
    private Set<Integer> mappedContainerPorts = new HashSet<>();
    private Map<String, String> environmentVariable = new HashMap<>();
    private int waitAfterStartupMs = 0;

    /**
     * container Port - Host Port
     */
    private Map<Integer, Integer> mappedPorts = new HashMap<>();

    /**
     * If != null, the ports listed here are considered as free (implementor has to take care instead of rule!)
     * Use with caution. E.g. helpful in environments where you have locally a VM with limited port forwardings.
     */
    private List<Integer> freeLocalPorts = null;


    private DockerContainerRule() {
        containerName = buildDefaultContainerName();
    }

    public static DockerContainerRule newDockerContainerRule(String image) {
        DockerContainerRule rule = new DockerContainerRule();
        rule.dockerImage = image;
        return rule;
    }

    public DockerContainerRule withContainerName(String containerName) {
        this.containerName = containerName;
        return this;
    }

    public DockerContainerRule withPortForward(int containerPort) {
        mappedContainerPorts.add(containerPort);
        return this;
    }

    /**
     * Tell DockerContainerRule to hardcoded use these ports. Use with caution! Should only be used in local environments, where e.g.
     * you are working with a VM and have not 100% control over your local ports.
     *
     * @param freeLocalPorts
     * @return
     */
    public DockerContainerRule withFreeLocalPorts(Integer... freeLocalPorts) {
        this.freeLocalPorts = Arrays.asList(freeLocalPorts);
        return this;
    }

    public DockerContainerRule withEnvironmentParameter(String key, String value) {
        environmentVariable.put(key, value);
        return this;
    }

    public DockerContainerRule withWaitAfterStartup(int waitMs) {
        this.waitAfterStartupMs = waitMs;
        return this;
    }

    private String buildDefaultContainerName() {
        return "docker-container-rule-" + System.currentTimeMillis();
    }

    private void findMappedPorts() {
        int nextLocalPortIndex = 0;
        for (Integer containerPort  : mappedContainerPorts) {
            int hostPort = SocketHelper.findFreePort();

            if (freeLocalPorts != null) {
                //evt. override with definitely free local ports
                hostPort = freeLocalPorts.get(nextLocalPortIndex);
                nextLocalPortIndex++;
            }

            mappedPorts.put(containerPort, hostPort);
        }
    }

    public int getMappedHostPort(int mappedContainerPort) {
        return mappedPorts.get(mappedContainerPort);
    }


    @Override
    protected void before() throws Throwable {
        findMappedPorts();
        String cmd = "docker run -itd --name " + containerName + " ";
        for (Integer containerPort : mappedPorts.keySet()) {
            int hostPort = getMappedHostPort(containerPort);
            cmd += "-p " + hostPort + ":" + containerPort + " ";
        }

        for (String envKey : environmentVariable.keySet()) {
            String value = environmentVariable.get(envKey);
            cmd += "-e \"" + envKey + "=" + value + "\" ";
        }


        cmd += " " + dockerImage;

        String result = ProcessHelper.execute(cmd, true);

        if (waitAfterStartupMs > 0) {
            System.out.println("Waiting " + waitAfterStartupMs + "ms after docker start");
            Thread.sleep(waitAfterStartupMs);
        }
    }

    public String getContainerName() {
        return containerName;
    }

    @Override
    protected void after() {
        try {
            ProcessHelper.execute("docker stop " + containerName, true);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        try {
            ProcessHelper.execute("docker rm -f " + containerName);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }


    private class PortMapping {
        public int hostPort;
        public int containerPort;
    }
}
